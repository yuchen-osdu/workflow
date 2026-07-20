# Workflow Bootstrap

The following script is responsible for creating workflows, required for DAGs to run.
It creates workflow for all DAGs on either Google Cloud or Reference (On-Prem) environments.
Below you can find required variables to bootstrap Workflow Service on Google Cloud and on-prem.
They should be passed as environment variables.

## Common variables

| Name                  | Description                                                                            | Example                      | Required |
| --------------------- | -------------------------------------------------------------------------------------- | ---------------------------- | -------- |
| **DATA_PARTITION_ID** | ID of data partition                                                                   | `osdu`                       | yes      |
| **WORKFLOW_HOST**     | Workflow host URL                                                                      | `http://workflow`            | yes      |
| **DAG_NAMES**         | Name of dags delimited by `,`                                                          | `Osdu-ingestion,segy_to_vds` | yes      |
| **ONPREM_EANABLED**   | Used to switch between Baremetal (On-Prem) bootstrap (`true`) and Google Cloud bootstrap (`false`) | `true`                       | yes      |

## On-prem variables

| Name                              | Description                          | Example           | Required |
| --------------------------------- | ------------------------------------ | ----------------- | -------- |
| **OPENID_PROVIDER_URL**           | Keycloak host URL                    | `http://keycloak` | yes      |
| **OPENID_PROVIDER_CLIENT_ID**     | Client id for keycloak authorization | `bootstrap_user`  | yes      |
| **OPENID_PROVIDER_CLIENT_SECRET** | Client id for keycloak authorization | `p@assw0rd`       | yes      |
