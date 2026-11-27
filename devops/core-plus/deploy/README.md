<!--- Deploy -->

# Deploy helm chart

## Introduction

This chart bootstraps the Workflow service on a [Kubernetes](https://kubernetes.io) cluster using [Helm](https://helm.sh) package manager.

## Prerequisites

The code was tested on **Kubernetes cluster** with **Istio** and the **Kubernetes Gateway API** CRDs installed (the CRDs are only needed when `httpRoute.enabled` is `true`).
> It is possible to use other versions, but it hasn't been tested

### Packages

Packages are only needed for installation from a local computer.

- **HELM** (version: v3.7.1 or higher) [helm](https://helm.sh/docs/intro/install/)
- **Kubectl** (version: v1.21.0 or higher) [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Installation

First you need to set variables in **values.yaml** file using any code editor. Some of the values are prefilled, but you need to specify some values as well. You can find more information about them below.

### Global variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**global.domain** | your domain, ex `example.com`. Not used by the chart templates (the `VirtualService` that used it was removed). Use `global.hostname` for the route host | string | `""` | no
**global.hostname** | host the `HTTPRoute` attaches to when `httpRoute.hostnames` is empty, ex `osdu.example.com` | string | `""` | only if `httpRoute.enabled` is true and `httpRoute.hostnames` is empty
**global.limitsEnabled** | whether CPU and memory limits are enabled | boolean | `true` | yes
**global.dataPartitionId** | data partition id, also used as `SHARED_TENANT_NAME` | string | `""` | yes
**global.onPremEnabled** | whether on-prem is enabled | boolean | - | no

### Configmap variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**data.logLevel** | logging level | string | `ERROR` | yes
**data.partitionHost** | partition service host address | string | `http://partition` | yes
**data.entitlementsHost** | entitlements service host address | string | `http://entitlements` | yes
**data.schemaHost** | schema service host address | string | `http://schema` | yes
**data.legalHost** | legal service host address | string | `http://legal` | yes
**data.workflowHost** | workflow service host address | string | `http://workflow` | yes
**data.osduAirflowUrl** | Airflow 2 URL | string | `http://airflow:8080` | yes
**data.airflowVersion3Enabled** | use the Airflow 3 engine (`OSDU_AIRFLOW_VERSION=airflow3`) | boolean | `false` | no
**data.osduAirflow3Url** | Airflow 3 URL, used when `data.airflowVersion3Enabled` is true | string | `""` | only if Airflow 3 is enabled
**data.sharedTenantName** | not used by the chart templates (`SHARED_TENANT_NAME` comes from `global.dataPartitionId`) | string | `""` | no
**data.dagNames** | DAG names registered by the bootstrap job | list | see `values.yaml` | yes

Airflow 3 credentials are not values. They come from the secret named by `conf.workflowAirflowSecretName`, which must contain `OSDU_AIRFLOW_AIRFLOW3_USERNAME` and `OSDU_AIRFLOW_AIRFLOW3_PASSWORD` when Airflow 3 is enabled.

### Deployment variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**data.requestsCpu** | amount of requested CPU | string | `5m` | yes
**data.requestsMemory** | amount of requested memory | string | `650Mi` | yes
**data.limitsCpu** | CPU limit | string | `1` | only if `global.limitsEnabled` is true
**data.limitsMemory** | memory limit | string | `3G` | only if `global.limitsEnabled` is true
**data.serviceAccountName** | name of your service account | string | `workflow` | yes
**data.bootstrapServiceAccountName** | service account of the bootstrap job | string | `""` | yes
**data.imagePullPolicy** | when to pull image | string | `IfNotPresent` | yes
**data.image** | service image | string | `""` | yes
**data.bootstrapImage** | bootstrap image | string | `""` | yes

### Config variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**conf.appName** | name of the app, also the Service name | string | `workflow` | yes
**conf.configmap** | configmap to be used | string | `workflow-config` | yes
**conf.replicas** | number of replicas | integer | `1` | yes
**conf.workflowKeycloakSecretName** | Keycloak secret name | string | `workflow-keycloak-secret` | yes
**conf.workflowAirflowSecretName** | Airflow secret name | string | `workflow-airflow-secret` | yes
**conf.workflowPostgresSecretName** | PostgreSQL secret name | string | `workflow-postgres-secret` | yes
**conf.rabbitmqSecretName** | RabbitMQ secret name | string | `rabbitmq-secret` | yes
**conf.bootstrapSecretName** | bootstrap secret name | string | `datafier-secret` | yes

### Routing (HTTPRoute)

The chart owns the routing for the service. It renders a Gateway API `HTTPRoute`, disabled by default. The chart no longer ships an Istio `VirtualService`.

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**httpRoute.enabled** | render the `HTTPRoute` | boolean | `false` | yes
**httpRoute.name** | route name. When empty the name is `<release>-route`, which differs from any route of the same name defined outside this chart | string | `""` | no
**httpRoute.parentRefs** | gateways the route attaches to | list | `cimpl-gateway` in `istio-system` | yes
**httpRoute.hostnames** | hosts the route serves. When empty, `global.hostname` is used | list | `[]` | one of `httpRoute.hostnames` or `global.hostname`
**httpRoute.routes** | one rule per entry, see below | list | `/api/workflow/` | yes

The render fails when `httpRoute.enabled` is true and both `httpRoute.hostnames` and `global.hostname` are empty, so the route is never exposed on all hosts.

Each entry of `httpRoute.routes` accepts:

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**path** | path to match | string | - | yes
**pathType** | match type, `PathPrefix` or `Exact` | string | `PathPrefix` | no
**serviceName** | backend Service name | string | `conf.appName` | no
**port** | backend Service port | integer | `80` | no
**filters** | Gateway API filters, for example `URLRewrite` | list | - | no

Example, a second route with a rewrite and another port:

```yaml
httpRoute:
  enabled: true
  hostnames:
    - osdu.example.com
  routes:
    - path: /api/workflow/
    - path: /other/
      port: 9002
      filters:
        - type: URLRewrite
          urlRewrite:
            path:
              type: ReplacePrefixMatch
              replacePrefixMatch: /
```

### Istio variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**istio.proxyCPU** | CPU request for Envoy sidecars | string | `5m` | yes
**istio.proxyCPULimit** | CPU limit for Envoy sidecars | string | `200m` | yes
**istio.proxyMemory** | memory request for Envoy sidecars | string | `50Mi` | yes
**istio.proxyMemoryLimit** | memory limit for Envoy sidecars | string | `256Mi` | yes
**istio.bootstrapProxyCPU** | CPU request for the bootstrap job sidecar | string | `5m` | yes
**istio.bootstrapProxyCPULimit** | CPU limit for the bootstrap job sidecar | string | `50m` | yes

### Install the helm chart

Run this command from within this directory:

```console
helm install core-plus-workflow-deploy .
```

To expose the service through the Gateway API:

```console
helm install core-plus-workflow-deploy . --set httpRoute.enabled=true --set global.hostname=osdu.example.com
```

To check the rendered route without installing:

```console
helm template core-plus-workflow-deploy . --set httpRoute.enabled=true --set global.hostname=osdu.example.com -s templates/httproute.yaml
```
