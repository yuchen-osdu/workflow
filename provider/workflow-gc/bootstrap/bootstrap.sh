#!/usr/bin/env bash

#Variable for disabling EDS ingest dag bootstrap
USE_EDS_DAG="${USE_EDS_DAG:-false}"

set -ex

source ./validate-env.sh "WORKFLOW_HOST"
source ./validate-env.sh "PARTITION_HOST"
source ./validate-env.sh "LEGAL_HOST"
source ./validate-env.sh "SCHEMA_HOST"

bootstrap_workflow_onprem() {

    # Getting ID_TOKEN from OpenID provider
    ID_TOKEN="$(curl --location --silent --globoff --request POST "${OPENID_PROVIDER_URL}/protocol/openid-connect/token" \
        --header "data-partition-id: ${DATA_PARTITION_ID}" \
        --header "Content-Type: application/x-www-form-urlencoded" \
        --data-urlencode "grant_type=client_credentials" \
        --data-urlencode "scope=openid" \
        --data-urlencode "client_id=${OPENID_PROVIDER_CLIENT_ID}" \
        --data-urlencode "client_secret=${OPENID_PROVIDER_CLIENT_SECRET}" | jq -r ".id_token")"
    export ID_TOKEN

    # Iterating over dag names
    IFS=","
    read -ra DAG_LIST <<<"$DAG_NAMES"

    # Create system workflow for each dag
    for DAG_NAME in "${DAG_LIST[@]}"; do

        status_code=$(curl --location --globoff --request POST "${WORKFLOW_HOST}/api/workflow/v1/workflow/system" \
            --write-out "%{http_code}" --silent --output "output.txt" \
            --header 'Content-Type: application/json' \
            --header "Authorization: Bearer ${ID_TOKEN}" \
            --data-binary '{ "workflowName": "'"${DAG_NAME}"'", "registrationInstructions": { "dagName": "'"${DAG_NAME}"'", "etc": "string" }, "description": "'"${DAG_NAME}"'" }')

        # Checking result code
        # 200 - Created, 409 - Already exists, Other - error
        if [ "$status_code" == 200 ]; then
            echo "Successfully registered workflow ${DAG_NAME}"
        elif [ "$status_code" == 409 ]; then
            cat /opt/output.txt | jq -r '.message'
        else
            cat /opt/output.txt | jq -r '.message'
            exit 1
        fi
        rm /opt/output.txt
    done
if [ "${USE_EDS_DAG}" == "true" ]; then
    # EDS ingest dag bootstrap

    # Check Schema bootstrap
    status_code_schema=$(curl --location --globoff --request GET "${SCHEMA_HOST}/api/schema-service/v1/schema/osdu:wks:reference-data--WorkflowUsageType:1.0.0" \
        --write-out "%{http_code}" --silent --output "output_schema.txt" \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer ${ID_TOKEN}" \
        --header "data-partition-id: ${DATA_PARTITION_ID}")

    # Checking result code
    if [ "$status_code_schema" == 200 ]; then
        echo "Schema bootstrap successfully finished!"
    else
        echo "Schema bootstrap has not finished yet!"
        cat /opt/output_schema.txt | jq -r '.message'
        exit 1
    fi
    rm /opt/output_schema.txt

    # Create Legal tag for EDS
    echo '{
            "name": "demo-legaltag",
            "description": "tag-for-eds",
            "properties": {
                "countryOfOrigin": [
                    "US"
                ],
                "contractId": "AE12345",
                "expirationDate": "2025-12-25",
                "originator": "Schlumberger",
                "dataType": "Third Party Data",
                "securityClassification": "Public",
                "personalData": "No Personal Data",
                "exportClassification": "EAR99"
                }
            }' >legal_tag.json

    status_code_legal=$(curl --location --globoff --request POST "${LEGAL_HOST}/api/legal/v1/legaltags" \
        --write-out "%{http_code}" --silent --output "output_legal.txt" \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer ${ID_TOKEN}" \
        --header "data-partition-id: ${DATA_PARTITION_ID}" \
        --data-binary @legal_tag.json)

    # Checking result code
    # 200 - Created, 409 - Already exists, Other - error
    if [ "$status_code_legal" == 201 ]; then
        echo "Successfully create a legaltag!"
    elif [ "$status_code_legal" == 409 ]; then
        cat /opt/output_legal.txt | jq -r ".message"
    else
        cat /opt/output_legal.txt | jq -r '.message'
        exit 1
    fi
    rm /opt/output_legal.txt

    for file in eds_ingest_data/*.json; do
        jq --arg dp "$DATA_PARTITION_ID" 'walk(if type == "string" then gsub("{{data_partition_id}}"; $dp) else . end)' $file >$(basename $file)

        status_code=$(curl --location --globoff --request POST "${WORKFLOW_HOST}/api/workflow/v1/workflow/Osdu_ingest/workflowRun" \
            --write-out "%{http_code}" --silent --output "output.txt" \
            --header 'Content-Type: application/json' \
            --header "Authorization: Bearer ${ID_TOKEN}" \
            --header "data-partition-id: ${DATA_PARTITION_ID}" \
            --data-binary @$(basename $file))

        # Checking result code
        if [ "$status_code" == 200 ]; then
            echo "Successfully registered dag ${file}"
        else
            cat /opt/output.txt | jq -r '.message'
            exit 1
        fi
        rm /opt/output.txt
        sleep 45
    done
fi
}

bootstrap_workflow_gc() {

    # Getting IDENTITY_TOKEN from context
    IDENTITY_TOKEN="$(gcloud auth print-identity-token)"
    export IDENTITY_TOKEN

    # Iterating over dag names
    IFS=","
    read -ra DAG_LIST <<<"$DAG_NAMES"

    # Create system workflow for each dag
    for DAG_NAME in "${DAG_LIST[@]}"; do

        status_code=$(curl --location --globoff --request POST "${WORKFLOW_HOST}/api/workflow/v1/workflow/system" \
            --write-out "%{http_code}" --silent --output "output.txt" \
            --header 'Content-Type: application/json' \
            --header "Authorization: Bearer ${IDENTITY_TOKEN}" \
            --data-binary '{ "workflowName": "'"${DAG_NAME}"'", "registrationInstructions": { "dagName": "'"${DAG_NAME}"'", "etc": "string" }, "description": "'"${DAG_NAME}"'" }')

        # Checking result code
        # 200 - Created, 409 - Already exists, Other - error
        if [ "$status_code" == 200 ]; then
            echo "Successfully registered workflow ${DAG_NAME}"
        elif [ "$status_code" == 409 ]; then
            cat /opt/output.txt | jq -r '.message'
        else
            cat /opt/output.txt | jq -r '.message'
            exit 1
        fi
        rm /opt/output.txt
    done

if [ "${USE_EDS_DAG}" == "true" ]; then
    # EDS ingest dag bootstrap

    # Check Schema bootstrap
    status_code_schema=$(curl --location --globoff --request GET "${SCHEMA_HOST}/api/schema-service/v1/schema/osdu:wks:reference-data--WorkflowUsageType:1.0.0" \
        --write-out "%{http_code}" --silent --output "output_schema.txt" \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer ${IDENTITY_TOKEN}" \
        --header "data-partition-id: ${DATA_PARTITION_ID}")

    # Checking result code
    if [ "$status_code_schema" == 200 ]; then
        echo "Schema bootstrap successfully finished!"
    else
        echo "Schema bootstrap has not finished yet!"
        cat /opt/output_schema.txt | jq -r '.message'
        exit 1
    fi
    rm /opt/output_schema.txt

    # Create Legal tag for EDS
    echo '{
            "name": "demo-legaltag",
            "description": "tag-for-eds",
            "properties": {
                "countryOfOrigin": [
                    "US"
                ],
                "contractId": "AE12345",
                "expirationDate": "2025-12-25",
                "originator": "Schlumberger",
                "dataType": "Third Party Data",
                "securityClassification": "Public",
                "personalData": "No Personal Data",
                "exportClassification": "EAR99"
                }
            }' >legal_tag.json

    status_code_legal=$(curl --location --globoff --request POST "${LEGAL_HOST}/api/legal/v1/legaltags" \
        --write-out "%{http_code}" --silent --output "output_legal.txt" \
        --header 'Content-Type: application/json' \
        --header "Authorization: Bearer ${IDENTITY_TOKEN}" \
        --header "data-partition-id: ${DATA_PARTITION_ID}" \
        --data-binary @legal_tag.json)

    # Checking result code
    # 200 - Created, 409 - Already exists, Other - error
    if [ "$status_code_legal" == 201 ]; then
        echo "Successfully create a legaltag!"
    elif [ "$status_code_legal" == 409 ]; then
        cat /opt/output_legal.txt | jq -r '.message'
    else
        cat /opt/output_legal.txt | jq -r '.message'
        exit 1
    fi
    rm /opt/output_legal.txt

    for file in eds_ingest_data/*.json; do
        jq --arg dp "$DATA_PARTITION_ID" 'walk(if type == "string" then gsub("{{data_partition_id}}"; $dp) else . end)' $file >$(basename $file)

        status_code=$(curl --location --globoff --request POST "${WORKFLOW_HOST}/api/workflow/v1/workflow/Osdu_ingest/workflowRun" \
            --write-out "%{http_code}" --silent --output "output.txt" \
            --header 'Content-Type: application/json' \
            --header "Authorization: Bearer ${IDENTITY_TOKEN}" \
            --header "data-partition-id: ${DATA_PARTITION_ID}" \
            --data-binary @$(basename $file))

        # Checking result code
        if [ "$status_code" == 200 ]; then
            echo "Successfully registered dag ${file}"
        else
            cat /opt/output.txt | jq -r '.message'
            exit 1
        fi
        rm /opt/output.txt
        sleep 45
    done
fi
}

if [ "${ONPREM_ENABLED}" == "true" ]; then
    source ./validate-env.sh "DATA_PARTITION_ID"
    source ./validate-env.sh "OPENID_PROVIDER_URL"
    source ./validate-env.sh "OPENID_PROVIDER_CLIENT_ID"
    source ./validate-env.sh "OPENID_PROVIDER_CLIENT_SECRET"
    bootstrap_workflow_onprem
else
    # Specifying "system" partition for GC installation
    export DATA_PARTITION_ID="system"
    bootstrap_workflow_gc
fi

touch /tmp/bootstrap_ready
