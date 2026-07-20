### Running Acceptance Tests

You will need to have the following environment variables defined.

| name                                | value                                                                           | description                                                                             | sensitive?                              | source |
|-------------------------------------|---------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|-----------------------------------------|--------|
| `WORKFLOW_HOST`                     | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com/api/workflow/`        | -                                                                                       | no                                     | -      |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | eg `osdu`                                                                       | Partition Id used for testing                                                           | no                                     | -      |
| `DOMAIN`                            | eg `group`                                                                      |                                                                                         | no                                     | -      |
| `ENTITLEMENT_V2_URL`                | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com/api/entitlements/v2/` |                                                                                         | no                                     | -      |
| `LEGAL_TAG`                         | eg `osdu-demo-legaltag`                                                         |                                                                                         | no                                     | -      |
| `OTHER_RELEVANT_DATA_COUNTRIES`     | eg `US`                                                                         |                                                                                         | no                                     | -      |
| `TEST_DAG_NAME`                     | eg `airflow_monitoring`                                                         |                                                                                         | no                                     | -      |
| `EXTERNAL_AIRFLOW_TESTS_ENABLED`    | eg `true`                                                                       | Defines whether external Airflow tests are enabled. If not set, it defaults to `false`. | no                                     | -      |
| `TEST_DAG_NAME_EXTERNAL_AIRFLOW`    | eg `airflow_monitoring`                                                         | DAG name in external Airflow.                                                           | no                                     | -      |
| `EXTERNAL_AIRFLOW_SECRET`           | eg `external_airflow_for_acceptance_tests`                                      | Name of the secret containing JSON with external Airflow connection parameters.         | no                                     | -      |
| `WORKFLOW_NAME_EXTERNAL_AIRFLOW`    | `test_workflow_external_airflow` (default value)                                | Workflow name that will be used during tests.                                           | no                                     | -      |

Authentication can be provided as OIDC config:

| name                                            | value                                   | description                   | sensitive? | source |
|-------------------------------------------------|-----------------------------------------|-------------------------------|------------|--------|
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | PRIVILEGED_USER Client Id     | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | PRIVILEGED_USER Client secret | yes        | -      |
| `NO_ACCESS_USER_OPENID_PROVIDER_CLIENT_ID`      | `********`                              | NO_ACCESS_USER Client Id      | yes        | -      |
| `NO_ACCESS_USER_OPENID_PROVIDER_CLIENT_SECRET`  | `********`                              | NO_ACCESS_USER Client secret  | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | `https://keycloak.com/auth/realms/osdu` | OpenID provider url           | yes        | -      |

Or tokens can be used directly from env variables:

| name                    | value      | description           | sensitive? | source |
|-------------------------|------------|-----------------------|------------|--------|
| `PRIVILEGED_USER_TOKEN` | `********` | PRIVILEGED_USER_TOKEN Token | yes        | -      |
| `NO_ACCESS_USER_TOKEN`  | `********` | NO_ACCESS_USER_TOKEN Token  | yes        | -      |

**Entitlements configuration for integration accounts**

| PRIVILEGED_USER                      | NO_ACCESS_USER        |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------|
|  users<br/>service.workflow.system-admin<br/>service.entitlements.user<br/>service.workflow.admin<br/>service.workflow.creator<br/>service.workflow.viewer<br/>service.legal.admin<br/>service.legal.editor<br/>data.test1<br/>data.integration.test<br/> |users|


Execute following command to build code and run all the integration tests:

```bash
# Note: this assumes that the environment variables for integration tests as outlined
#       above are already exported in your environment.
$ (cd workflow-acceptance-test && mvn clean test)
```


## License

Copyright © Google LLC

Copyright © EPAM Systems

Copyright © ExxonMobil

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
