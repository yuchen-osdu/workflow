# workflow-gc

The OSDU R3 Workflow service is designed to start business processes in the system. In the OSDU R3
prototype phase, the service allows you to work with workflow metadata, supporting CRUD operations
and also trigger workflow in airflow, get, delete and change the status of process startup records.

The Workflow service provides a wrapper functionality around the Apache Airflow functions and is
designed to carry out preliminary work with files before running the Airflow Directed Acyclic Graphs
(DAGs) that will perform actual ingestion of OSDU data.

## Running Locally

### Requirements

In order to run this service locally, you will need the following:

- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)
- Infrastructure dependencies, deployable through the relevant [infrastructure template](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-gc-provisioning)

## Service Configuration

### Baremetal

[Baremetal service configuration](docs/baremetal/README.md)

### Google Cloud

[Google cloud service configuration](docs/gcp/README.md)

### Test the application

After the service has started it should be accessible via a web browser by visiting [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). If the request does not fail, you can then run the integration tests.

### Baremetal test configuration

[Baremetal service configuration](docs/baremetal/README.md)

### Google Cloud test configuration

[Google Cloud service configuration](docs/gcp/README.md)

### Run Locally

Check that maven is installed:

```bash
$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_212, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/jdk8u212-b04/jre
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. This file should live within `~/.mvn/community-maven.settings.xml`:

```bash
$ cat ~/.m2/settings.xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>community-maven-via-private-token</id>
            <!-- Treat this auth token like a password. Do not share it with anyone, including Microsoft support. -->
            <!-- The generated token expires on or before 11/14/2019 -->
             <configuration>
              <httpHeaders>
                  <property>
                      <name>Private-Token</name>
                      <value>${env.COMMUNITY_MAVEN_TOKEN}</value>
                  </property>
              </httpHeaders>
             </configuration>
        </server>
    </servers>
</settings>
```

- Update the Google cloud SDK to the latest version:

```bash
gcloud components update
```

- Set Google Project Id:

```bash
gcloud config set project <YOUR-PROJECT-ID>
```

- Perform a basic authentication in the selected project:

```bash
gcloud auth application-default login
```

## Testing

- Navigate to workflow service's root folder and run:

```bash
mvn clean install
```

- If you wish to see the coverage report then go to testing/target/site/jacoco-aggregate and open index.html

- If you wish to build the project without running tests

```bash
mvn clean install -DskipTests
```

### Running

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*

```bash
cd provider/workflow-gc-datastore/ && mvn spring-boot:run
```

## Deployment

Workflow Service is compatible with App Engine Flexible Environment and Cloud Run.

- To deploy into Cloud run, please, use this documentation:
  <https://cloud.google.com/run/docs/quickstarts/build-and-deploy>

- To deploy into App Engine, please, use this documentation:
  <https://cloud.google.com/appengine/docs/flexible/java/quickstart>

## License

Copyright 2020 Google LLC
Copyright 2020 EPAM Systems, Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
