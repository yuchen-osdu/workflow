#!/usr/bin/env bash

USE_EDS_DAG="${USE_EDS_DAG:-false}"

set -euo pipefail

log() {
  echo "[$(date -Iseconds)] $*"
}

fail() {
  echo "[$(date -Iseconds)] ERROR: $*" >&2
  exit 1
}

wait_for_workflow() {
  log "Waiting for Workflow service to become reachable..."

  local max_retries=60
  local delay=3

  for ((i=1; i<=max_retries; i++)); do
    if curl -sf \
      --connect-timeout 5 --max-time 15 \
      "${WORKFLOW_HOST}/api/workflow/v1/info" >/dev/null; then

      log "Workflow endpoint reachable"
      sleep 5
      return 0
    fi

    log "Workflow not reachable yet ($i/$max_retries)..."
    sleep $delay
  done

  fail "Workflow did not become reachable in time"
}

get_access_token() {
  log "Requesting access token from Keycloak..." >&2

  local token
  token=$(curl -s --location \
    --connect-timeout 5 --max-time 15 \
    "${OPENID_PROVIDER_URL}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=client_credentials" \
    --data-urlencode "scope=openid" \
    --data-urlencode "client_id=${OPENID_PROVIDER_CLIENT_ID}" \
    --data-urlencode "client_secret=${OPENID_PROVIDER_CLIENT_SECRET}" \
    | jq -r ".access_token")

  [[ -z "$token" || "$token" == "null" ]] && fail "Failed to obtain access token"

  echo "$token"
}

bootstrap_workflow() {
  IFS=","
  read -ra DAG_LIST <<<"$DAG_NAMES"

  for DAG_NAME in "${DAG_LIST[@]}"; do
    log "Registering workflow ${DAG_NAME}..."

    status_code=$(curl --location --globoff --request POST "${WORKFLOW_HOST}/api/workflow/v1/workflow/system" \
        --write-out "%{http_code}" --silent --output /tmp/output.txt \
        --connect-timeout 5 --max-time 15 \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer ${ACCESS_TOKEN}" \
        --data-binary '{ "workflowName": "'"${DAG_NAME}"'", "registrationInstructions": { "dagName": "'"${DAG_NAME}"'", "etc": "string" }, "description": "'"${DAG_NAME}"'" }')

    if [ "$status_code" == 200 ]; then
      log "Successfully registered workflow ${DAG_NAME}"
    elif [ "$status_code" == 409 ]; then
      log "Workflow ${DAG_NAME} already exists: $(jq -r '.message' /tmp/output.txt)"
    else
      fail "Failed to register workflow ${DAG_NAME} (HTTP ${status_code}): $(jq -r '.message' /tmp/output.txt)"
    fi
    rm -f /tmp/output.txt
  done
}

bootstrap_eds() {
  log "Checking schema bootstrap..."

  status_code_schema=$(curl --location --globoff --request GET "${SCHEMA_HOST}/api/schema-service/v1/schema/osdu:wks:reference-data--WorkflowUsageType:1.0.0" \
      --write-out "%{http_code}" --silent --output /tmp/output_schema.txt \
      --connect-timeout 5 --max-time 15 \
      --header 'Content-Type: application/json' \
      --header "Authorization: Bearer ${ACCESS_TOKEN}" \
      --header "data-partition-id: ${DATA_PARTITION_ID}")

  if [ "$status_code_schema" == 200 ]; then
    log "Schema bootstrap successfully finished!"
  else
    fail "Schema bootstrap has not finished yet: $(jq -r '.message' /tmp/output_schema.txt)"
  fi
  rm -f /tmp/output_schema.txt

  log "Creating legal tag..."
  cat <<'EOF' >/tmp/legal_tag.json
{
  "name": "demo-legaltag",
  "description": "tag-for-eds",
  "properties": {
    "countryOfOrigin": ["US"],
    "contractId": "AE12345",
    "expirationDate": "2025-12-25",
    "originator": "Schlumberger",
    "dataType": "Third Party Data",
    "securityClassification": "Public",
    "personalData": "No Personal Data",
    "exportClassification": "EAR99"
  }
}
EOF

  status_code_legal=$(curl --location --globoff --request POST "${LEGAL_HOST}/api/legal/v1/legaltags" \
      --write-out "%{http_code}" --silent --output /tmp/output_legal.txt \
      --connect-timeout 5 --max-time 15 \
      --header 'Content-Type: application/json' \
      --header "Authorization: Bearer ${ACCESS_TOKEN}" \
      --header "data-partition-id: ${DATA_PARTITION_ID}" \
      --data-binary @/tmp/legal_tag.json)

  if [ "$status_code_legal" == 201 ]; then
    log "Successfully created legal tag!"
  elif [ "$status_code_legal" == 409 ]; then
    log "Legal tag already exists: $(jq -r '.message' /tmp/output_legal.txt)"
  else
    fail "Failed to create legal tag (HTTP ${status_code_legal}): $(jq -r '.message' /tmp/output_legal.txt)"
  fi
  rm -f /tmp/output_legal.txt

  for file in eds_ingest_data/*.json; do
    log "Ingesting ${file}..."
    jq --arg dp "$DATA_PARTITION_ID" 'walk(if type == "string" then gsub("{{data_partition_id}}"; $dp) else . end)' "$file" >/tmp/$(basename "$file")

    status_code=$(curl --location --globoff --request POST "${WORKFLOW_HOST}/api/workflow/v1/workflow/Osdu_ingest/workflowRun" \
        --write-out "%{http_code}" --silent --output /tmp/output.txt \
        --connect-timeout 5 --max-time 15 \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer ${ACCESS_TOKEN}" \
        --header "data-partition-id: ${DATA_PARTITION_ID}" \
        --data-binary @/tmp/$(basename "$file"))

    if [ "$status_code" == 200 ]; then
      log "Successfully triggered ingest for ${file}"
    else
      fail "Failed to trigger ingest for ${file} (HTTP ${status_code}): $(jq -r '.message' /tmp/output.txt)"
    fi
    rm -f /tmp/output.txt
    sleep 45
  done
}

# --- MAIN ---

: "${WORKFLOW_HOST:?missing}"
: "${PARTITION_HOST:?missing}"
: "${LEGAL_HOST:?missing}"
: "${SCHEMA_HOST:?missing}"
: "${DATA_PARTITION_ID:?missing}"
: "${OPENID_PROVIDER_URL:?missing}"
: "${OPENID_PROVIDER_CLIENT_ID:?missing}"
: "${OPENID_PROVIDER_CLIENT_SECRET:?missing}"

log "Starting workflow bootstrap..."

wait_for_workflow

ACCESS_TOKEN=$(get_access_token)
export ACCESS_TOKEN

bootstrap_workflow

if [ "${USE_EDS_DAG}" == "true" ]; then
  bootstrap_eds
fi

touch /tmp/bootstrap_ready
log "Bootstrap finished successfully"

sleep infinity
