## Service Configuration for Anthos

## Environment variables

Define the following environment variables.

Must have:

| name                                      | value                                      | description                                                                                                                                                                                                                               | sensitive? | source |
|-------------------------------------------|--------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|--------|
| `SPRING_PROFILES_ACTIVE`                  | ex `anthos`                                | Spring profile that activate default configuration for Baremetal environment                                                                                                                                                              | false      | -      |
| `OSDU_AIRFLOW_USERNAME`                   | `******`                                   | Airflow username, need to be defined if `AIRFLOW_IAAP_MODE`=`false`                                                                                                                                                                       | yes        | -      |
| `OSDU_AIRFLOW_PASSWORD`                   | `******`                                   | Airflow password, need to be defined if `AIRFLOW_IAAP_MODE`=`false`                                                                                                                                                                       | yes        | -      |
| `GCP_AIRFLOW_URL`                         | ex `https://********-tp.appspot.com`       | Airflow endpoint                                                                                                                                                                                                                          | yes        | -      |
| `<POSTGRES_PASSWORD_ENV_VARIABLE_NAME>`   | ex `POSTGRES_PASS_OSDU`                    | Postgres password env name, name of that variable not defined at the service level, the name will be received through partition service. Each tenant can have its own ENV name value, and it must be present in ENV of Workflow service   | yes        | -      |
| `<AMQP_PASSWORD_ENV_VARIABLE_NAME>`       | ex `AMQP_PASS_OSDU`                        | Amqp password env name, name of that variable not defined at the service level, the name will be received through partition service. Each tenant can have its own ENV name value, and it must be present in ENV of Workflow service       | yes        | -      |
| `<AMQP_ADMIN_PASSWORD_ENV_VARIABLE_NAME>` | ex `AMQP_ADMIN_PASS_OSDU`                  | Amqp admin password env name, name of that variable not defined at the service level, the name will be received through partition service. Each tenant can have its own ENV name value, and it must be present in ENV of Workflow service | yes        | -      |
| `SHARED_TENANT_NAME`                      | ex `osdu`                                  | Shared account id                                                                                                                                                                                                                         | no         | -      |
| `OPENID_PROVIDER_CLIENT_ID`               | `*****`                                    | Client id that represents this service and serves to request tokens, example `workload-identity-legal`                                                                                                                                    | yes        | -      |
| `OPENID_PROVIDER_CLIENT_SECRET`           | `*****`                                    | This client secret that serves to request tokens                                                                                                                                                                                          | yes        | -      |
| `OPENID_PROVIDER_URL`                     | `https://keycloack.com/auth/realms/master` | URL of OpenID Connect provider, it will be used as `<OpenID URL> + /.well-known/openid-configuration` to auto configure endpoint for token request                                                                                        | no         | -      |

Defined in default application property file but possible to override:

| name                               | value                                         | description                                                                                                                                            | sensitive? | source                              |
|------------------------------------|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|------------|-------------------------------------|
| `LOG_PREFIX`                       | `workflow`                                    | Logging prefix                                                                                                                                         | no         | -                                   |
| `AUTHORIZE_API`                    | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint                                                                                                                              | no         | output of infrastructure deployment |
| `PARTITION_API`                    | ex `http://localhost:8081/api/partition/v1`   | Partition service endpoint                                                                                                                             | no         | -                                   |
| `STATUS_CHANGED_MESSAGING_ENABLED` | `true` OR `false`                             | Allows configuring message publishing about schemas changes to Pub/Sub                                                                                 | no         | -                                   |
| `STATUS_CHANGED_TOPIC_NAME`        | ex `status-changed`                           | Allows to subscribe a specific Pub/Sub topic                                                                                                           | no         | -                                   |
| `OSDU_AIRFLOW_VERSION2`            | `true` OR `false`                             | Allows to configure Airflow API used by Workflow service, choose `true` to use `stable` API, `false` to use `experimental` API, by default used `true` | no         | -                                   |
| `COMPOSER_CLIENT`                  | `IAAP` OR `V2` OR `NONE`                      | Allows to configure authentication method used by Workflow to authenticate its requests to Airflow, by default `NONE` is used                          | no         | -                                   |
| `MANAGEMENT_ENDPOINTS_WEB_BASE`    | ex `/`                                        | Web base for Actuator                                                                                                                                  | no         | -                                   |
| `MANAGEMENT_SERVER_PORT`           | ex `8081`                                     | Port for Actuator                                                                                                                                      | no         | -                                   |
| `SECRET_API`                       | ex `http://secret/api/secret/v2`              | Secret service API endpoint                                                                                                                            | no         | output of infrastructure deployment |
| `GROUP_ID`                         | ex `group`                                    | The id of the groups is created. The default (and recommended for `jdbc`) value is `group`                                                             | no         | -                                   |


These variables define service behavior, and are used to switch between `Reference` or `Google Cloud` environments, their overriding
and usage in the mixed mode were not tested. Usage of spring profiles is preferred.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `PARTITION_AUTH_ENABLED` | ex `true` or `false` | Disable or enable auth token provisioning for requests to Partition service | no | - |
| `OQMDRIVER` | `rabbitmq` or `pubsub` | OQM driver mode that defines which message broker will be used | no | - |
| `OSMDRIVER` | `postgres` OR `datastore` | OSM driver mode that defines which storage will be used | no | - |
| `SYSTEM_WORKFLOW_NAMESPACE` | ex `system_workflow_namespace` | Namespace for System Workflows | no | output of infrastructure deployment |

### Properties set in Partition service

Note that properties can be set in Partition as `sensitive` in that case, property `value` should be present **not value itself**, but **ENV variable name**.
This variable should be present in the environment of service that needs that variable.

Example:

```
    "elasticsearch.port": {
      "sensitive": false, <- value not sensitive
      "value": "9243"  <- will be used as is.
    },
      "elasticsearch.password": {
      "sensitive": true, <- value is sensitive
      "value": "ELASTIC_SEARCH_PASSWORD_OSDU" <- service consumer should have env variable ELASTIC_SEARCH_PASSWORD_OSDU with elastic search password
    }
```

## Postgres configuration

### Properties set in Partition service

**prefix:** `osm.postgres`

It can be overridden by:

- through the Spring Boot property `osm.postgres.partition-properties-prefix`
- environment variable `OSM_POSTGRES_PARTITION_PROPERTIES_PREFIX`

**Property set:**

| Property | Description |
| --- | --- |
| osm.postgres.datasource.url | server URL |
| osm.postgres.datasource.username | username |
| osm.postgres.datasource.password | password |

<details><summary>Example of a definition for a single tenant</summary>

```

curl -L -X PATCH 'http://partition.com/api/partition/v1/partitions/opendes' -H 'data-partition-id: opendes' -H 'Authorization: Bearer ...' -H 'Content-Type: application/json' --data-raw '{
  "properties": {
    "osm.postgres.datasource.url": {
      "sensitive": false,
      "value": "jdbc:postgresql://127.0.0.1:5432/postgres"
    },
    "osm.postgres.datasource.username": {
      "sensitive": false,
      "value": "postgres"
    },
    "osm.postgres.datasource.password": {
      "sensitive": true,
      "value": "<POSTGRES_PASSWORD_ENV_VARIABLE_NAME>" <- (Not actual value, just name of env variable)
    }
  }
}'

```

</details>

### Persistence layer

### Database structure for OSMDRIVER=postgres

```
DROP TABLE IF EXISTS anthos.workflow_osm;
CREATE TABLE IF NOT EXISTS anthos.workflow_osm
(
 id text COLLATE pg_catalog."default" NOT NULL,
 pk bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 data jsonb NOT NULL,
 CONSTRAINT workflow_id UNIQUE (id)
)
TABLESPACE pg_default;
ALTER TABLE anthos.workflow_osm
    OWNER to postgres;


DROP TABLE IF EXISTS anthos.workflow_run_osm;
CREATE TABLE IF NOT EXISTS anthos.workflow_run_osm
(
 id text COLLATE pg_catalog."default" NOT NULL,
 pk bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 data jsonb NOT NULL,
 CONSTRAINT workflow_run_id UNIQUE (id)
)
TABLESPACE pg_default;
ALTER TABLE anthos.workflow_run_osm
    OWNER to postgres;

DROP TABLE IF EXISTS system_workflow_namespace.system_workflow_osm;
CREATE TABLE IF NOT EXISTS system_workflow_namespace.system_workflow_osm
(
 id text COLLATE pg_catalog."default" NOT NULL,
 pk bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 data jsonb NOT NULL,
 CONSTRAINT workflow_run_id UNIQUE (id)
)
TABLESPACE pg_default;
ALTER TABLE system_workflow_namespace.system_workflow_osm
    OWNER to postgres;
```

## RabbitMQ configuration

### Properties set in Partition service

**prefix:** `oqm.rabbitmq`

It can be overridden by:

- through the Spring Boot property `oqm.rabbitmq.partition-properties-prefix`
- environment variable `OQM_RABBITMQ_PARTITION_PROPERTIES_PREFIX`

**Property Set** (for two types of connection: messaging and admin operations):

| Property | Description |
| --- | --- |
| oqm.rabbitmq.amqp.host | messaging hostname or IP |
| oqm.rabbitmq.amqp.port | - port |
| oqm.rabbitmq.amqp.path | - path |
| oqm.rabbitmq.amqp.username | - username |
| oqm.rabbitmq.amqp.password | - password |
| oqm.rabbitmq.admin.schema | admin host schema |
| oqm.rabbitmq.admin.host | - host name |
| oqm.rabbitmq.admin.port | - port |
| oqm.rabbitmq.admin.path | - path |
| oqm.rabbitmq.admin.username | - username |
| oqm.rabbitmq.admin.password | - password |

<details><summary>Example of a single tenant definition</summary>

```

curl -L -X PATCH 'https://dev.osdu.club/api/partition/v1/partitions/opendes' -H 'data-partition-id: opendes' -H 'Authorization: Bearer ...' -H 'Content-Type: application/json' --data-raw '{
  "properties": {
    "oqm.rabbitmq.amqp.host": {
      "sensitive": false,
      "value": "localhost"
    },
    "oqm.rabbitmq.amqp.port": {
      "sensitive": false,
      "value": "5672"
    },
    "oqm.rabbitmq.amqp.path": {
      "sensitive": false,
      "value": ""
    },
    "oqm.rabbitmq.amqp.username": {
      "sensitive": false,
      "value": "guest"
    },
    "oqm.rabbitmq.amqp.password": {
      "sensitive": true,
      "value": "<AMQP_PASSWORD_ENV_VARIABLE_NAME>" <- (Not actual value, just name of env variable)
    },

     "oqm.rabbitmq.admin.schema": {
      "sensitive": false,
      "value": "http"
    },
     "oqm.rabbitmq.admin.host": {
      "sensitive": false,
      "value": "localhost"
    },
    "oqm.rabbitmq.admin.port": {
      "sensitive": false,
      "value": "9002"
    },
    "oqm.rabbitmq.admin.path": {
      "sensitive": false,
      "value": "/api"
    },
    "oqm.rabbitmq.admin.username": {
      "sensitive": false,
      "value": "guest"
    },
    "oqm.rabbitmq.admin.password": {
      "sensitive": true,
      "value": "<AMQP_ADMIN_PASSWORD_ENV_VARIABLE_NAME>" <- (Not actual value, just name of env variable)
    }
  }
}'

```

</details>

### Exchanges & queues configuration

At RabbitMq should be created exchange with the name:

**name:** `status-changed`

It can be overridden by:

- through the Spring Boot property `gcp.status-changed.topicName`
- environment variable `STATUS_CHANGED_TOPIC_NAME`

![Screenshot](./pics/rabbit.PNG)

### Running E2E Tests

You will need to have the following environment variables defined.

| name                                           | value                                                         | description                                   | sensitive?                                        | source        |
|------------------------------------------------|---------------------------------------------------------------|-----------------------------------------------|---------------------------------------------------|---------------|
| `DOMAIN`                                       | ex `contoso.com`                                              | OSDU R2 to run tests under                    | no                                                | -             |
| `LEGAL_TAG`                                    | `********`                                                    | Demo legal tag used to pass test              | yes                                               | Legal service |
| `WORKFLOW_HOST`                                | ex `https://os-workflow-dot-opendes.appspot.com/api/workflow` | Endpoint of workflow service                  | no                                                | -             |
| `DEFAULT_DATA_PARTITION_ID_TENANT1`            | ex `opendes`                                                  | OSDU tenant used for testing                  | no                                                | -             |
| `OTHER_RELEVANT_DATA_COUNTRIES`                | `US`                                                          | -                                             | no                                                | -             |
| `FINISHED_WORKFLOW_ID`                         | `********`                                                    | Workflow ID with finished status              | yes                                               | -             |
| `TEST_DAG_NAME`                                | `********`                                                    | Name of test DAG                              | yes                                               | -             |
| `TEST_OPENID_PROVIDER_CLIENT_ID`               | `********`                                                    | Client Id for `$INTEGRATION_TESTER`           | yes                                               | --            |
| `TEST_OPENID_PROVIDER_CLIENT_SECRET`           | `********`                                                    |                                               | Client secret for `$INTEGRATION_TESTER`           | --            |
| `TEST_NO_ACCESS_OPENID_PROVIDER_CLIENT_ID`     | `********`                                                    | Client Id for `$NO_ACCESS_INTEGRATION_TESTER` | yes                                               | --            |
| `TEST_NO_ACCESS_OPENID_PROVIDER_CLIENT_SECRET` | `********`                                                    |                                               | Client secret for `$NO_ACCESS_INTEGRATION_TESTER` | --            |
| `TEST_OPENID_PROVIDER_URL`                     | `https://keycloak.com/auth/realms/osdu`                       | OpenID provider URL                           | yes                                               | --            |
| `ENTITLEMENT_V2_URL`                           | ex `http://localhost:8080/api/entitlements/v2/`               | Entitlements V2 Host                          | no                                                | --            |

**Entitlements configuration for integration accounts**

user impersonatetestmember@test.com  doesn't need to exist in any identity provider(Keycloak), just configure at Entitlement Service

| INTEGRATION_TESTER | NO_DATA_ACCESS_TESTER | impersonatetestmember@test.com |
| ---  | ---   | ---   |
| service.workflow.system-admin<br/>users<br/>service.entitlements.user<br/>service.workflow.admin<br/>service.workflow.creator<br/>service.workflow.viewer<br/>service.legal.admin<br/>service.legal.editor<br/>data.test1<br/>data.integration.test | users | service.workflow.creator<br/>data.default.owners<br/>data.wke.viewers<br/>users<br/>service.entitlements.user<br/>users.datalake.impersonation<br/>data.wke.owners<br/>data.default.viewers<br/>service.reservoir-dms.viewers<br/>data.ihs.viewers<br/> |


```bash
# build + install integration test core
$ (cd testing/workflow-test-core/ && mvn clean install)

# build + run baremetal integration tests.
#
# Note: this assumes that the environment variables for integration tests as outlined
#       above are already exported in your environment.
$ (cd testing/workflow-test-baremetal/ && mvn clean test)
```
