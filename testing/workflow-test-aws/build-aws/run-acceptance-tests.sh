# Copyright © 2020 Amazon Web Services
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

SCRIPT_SOURCE_DIR=$(dirname "$0")
echo "Script source location"
echo "$SCRIPT_SOURCE_DIR"

# Required variables
export WORKFLOW_HOST=$WORKFLOW_URL
export DEFAULT_DATA_PARTITION_ID_TENANT1=osdu
export DOMAIN=example.com
export ENTITLEMENT_V2_URL=$ENTITLEMENT_V2_URL
export LEGAL_TAG=osdu
export OTHER_RELEVANT_DATA_COUNTRIES=US
export TEST_DAG_NAME=my_first_dag

export AWS_COGNITO_AUTH_FLOW="USER_PASSWORD_AUTH"
export PRIVILEGED_USER_TOKEN=$(aws cognito-idp initiate-auth --region ${AWS_REGION} --auth-flow ${AWS_COGNITO_AUTH_FLOW} --client-id ${AWS_COGNITO_CLIENT_ID} --auth-parameters "{\"USERNAME\":\"${ADMIN_USER}\",\"PASSWORD\":\"${ADMIN_PASSWORD}\"}" --query AuthenticationResult.AccessToken --output text)
export NO_ACCESS_USER_TOKEN=$(aws cognito-idp initiate-auth --region ${AWS_REGION} --auth-flow ${AWS_COGNITO_AUTH_FLOW} --client-id ${AWS_COGNITO_CLIENT_ID} --auth-parameters "{\"USERNAME\":\"${USER_NO_ACCESS}\",\"PASSWORD\":\"${USER_NO_ACCESS_PASSWORD}\"}" --query AuthenticationResult.AccessToken --output text)

mvn clean test -f "$SCRIPT_SOURCE_DIR"/pom.xml
TEST_EXIT_CODE=$?

if [ -n "$1" ]
  then
    mkdir -p "$1"
    cp -R "$SCRIPT_SOURCE_DIR"/target/surefire-reports "$1"
fi

exit $TEST_EXIT_CODE
