### Workflow acceptance tests

End-to-end tests for the OSDU Workflow service. Tests use **[os-core-test](https://community.opengroup.org/osdu/platform/system/lib/core/os-core-test) `0.2.13`** for service URLs, authentication, typed HTTP clients, `/info` assertions, and entitlements role verification.

### Prerequisites

Export the variables below, or place them in a `.env` file loaded by `os-core-test` (see the [os-core-test README](https://community.opengroup.org/osdu/platform/system/lib/core/os-core-test/-/blob/main/README.md)).

Configuration is resolved in this order (first match wins):

1. `.env` in the project working directory
2. `src/test/resources/.env` on the test classpath
3. JVM system properties (`-Dname=value`)
4. OS environment variables

| name | value | description | required |
|------|-------|-------------|----------|
| `HOST` | ex `https://osdu.example.com` | OSDU API host; `os-core-test` appends service paths (Workflow: `/api/workflow/v1`, Entitlements: `/api/entitlements/v2`) | yes |

`BaseWorkflowAcceptanceTest` configures `WORKFLOW_V1` and `ENTITLEMENTS_V2` from `HOST`. `GetServiceInfoIntegrationTest` uses `BaseGetInfoAcceptanceTests` with `ServiceType.WORKFLOW_V1`.

Authentication can be provided as OIDC config:

| name | value | description | sensitive? |
|------|-------|-------------|------------|
| `TEST_OPENID_PROVIDER_URL` | ex `https://keycloak.example.com/auth/realms/osdu` | OpenID provider URL | yes |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID` | `********` | Privileged User client ID | yes |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********` | Privileged User client secret | yes |
| `PRIVILEGED_USER_OPENID_PROVIDER_SCOPE` | ex `api://my-app/.default` | OAuth2 scope (optional, defaults to `openid`) | no |
| `NO_ACCESS_USER_OPENID_PROVIDER_CLIENT_ID` | `********` | No-access User client ID | yes |
| `NO_ACCESS_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********` | No-access User client secret | yes |
| `NO_ACCESS_USER_OPENID_PROVIDER_SCOPE` | ex `api://my-app/.default` | OAuth2 scope (optional, defaults to `openid`) | no |

Or tokens can be used directly (`{USER_TYPE}_TOKEN`):

| name | value | description | sensitive? |
|------|-------|-------------|------------|
| `PRIVILEGED_USER_TOKEN` | `********` | Privileged User token | yes |
| `NO_ACCESS_USER_TOKEN` | `********` | No-access User token | yes |

### Test configuration

| name | value | description | required |
|------|-------|-------------|----------|
| `DATA_PARTITION_ID` | ex `opendes` | Data partition value included in workflow run payloads. The shared workflow client handles workflow request headers. Defaults to an empty string. | no |
| `TEST_DAG_NAME` | ex `airflow_monitoring` | DAG/workflow name used by standard workflow tests. Defaults to `airflow_monitoring`. | no |
| `EXTERNAL_AIRFLOW_TESTS_ENABLED` | `true` OR `false` | Run tests annotated with `@TestExternalAirflow`. Defaults to `false`. | no |
| `WORKFLOW_NAME_EXTERNAL_AIRFLOW` | ex `external-airflow-accept-test` | Workflow name prefix used by external Airflow tests. Defaults to `external-airflow-accept-test`. | no |
| `TEST_DAG_NAME_EXTERNAL_AIRFLOW` | ex `airflow_monitoring` | DAG name used by external Airflow tests. Defaults to `TEST_DAG_NAME`. | no |
| `EXTERNAL_AIRFLOW_SECRET` | ex `airflow-workflow-tests` | Secret name containing external Airflow connection parameters. Defaults to `airflow-workflow-tests`. | no |
| `AIRFLOW3_TESTS_ENABLED` | `true` OR `false` | Run tests annotated with `@TestAirflow3`, which assert Airflow 3-specific behaviour (e.g. `/info` reports the Airflow 3 engine version). Set to `true` only when the Workflow Service under test targets Airflow 3. Defaults to `false`. | no |
| `AIRFLOW3_EXPECTED_VERSION_PREFIX` | ex `3` or `3.2.2` | Expected Airflow 3 version prefix asserted by the `@TestAirflow3` `/info` test. Defaults to `3`. | no |

### Entitlements roles

Integration accounts must have the roles listed in `src/test/resources/required-roles.json`. `os-core-test` verifies every user type in that file against entitlements during test initialization.

| PRIVILEGED_USER | NO_ACCESS_USER |
|-----------------|----------------|
| users | users |
| service.entitlements.user | |
| service.workflow.system-admin | |
| service.workflow.admin | |
| service.workflow.creator | |
| service.workflow.viewer | |
| service.legal.admin | |
| service.legal.editor | |

### Test suites

Each row is a JUnit test class run by `mvn test`.

| Test class | Coverage |
|------------|----------|
| `GetServiceInfoIntegrationTest` | Workflow `/info` endpoint using shared `BaseGetInfoAcceptanceTests`. |
| `WorkflowV3IntegrationTests` | Create, list, get, and delete workflow APIs. |
| `WorkflowRunV3IntegrationTests` | Create, list, get, update, duplicate-run, impersonation, and authorization workflow-run APIs. |
| `GetWorkflowRunLatestTaskInfoTest` | Latest task info endpoint for workflow runs. |
| `PostCreateSystemWorkflowV3IntegrationTests` | System workflow create API. |
| `DeleteSystemWorkflowV3IntegrationTests` | System workflow delete API. |

External Airflow scenarios are disabled by default and run only when `EXTERNAL_AIRFLOW_TESTS_ENABLED=true`.

### Test framework notes

- **Base class:** `BaseWorkflowAcceptanceTest` extends `os-core-test` `BaseAcceptanceTests` and provides `WorkflowClient` and `EntitlementsClient`.
- **HTTP client:** `org.opengroup.osdu.core.test.client.WorkflowClient` - non-2xx responses throw `ClientException`.
- **Unauthenticated requests:** Tests use a dedicated `WorkflowClient` constructed with authentication disabled.
- **Cleanup:** Created workflow runs are finished during teardown before created workflows are deleted. `WorkflowClient.teardown()` also deletes resources tracked by the typed client.
- **Payloads:** Workflow and workflow-run payloads use `os-core-test` request model objects instead of raw JSON strings.

### Run tests

```bash
# Export variables above, or use a .env file in this directory.
cd workflow-acceptance-test && mvn clean test
```

Run a single test class:

```bash
mvn test -Dtest=WorkflowRunV3IntegrationTests
```

## License

Copyright © 2026 Google LLC

Copyright © 2026 EPAM Systems

Copyright © 2026 ExxonMobil

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
