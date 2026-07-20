package org.opengroup.osdu.workflow.swagger;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Map;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfiguration {

    @Autowired
    private SwaggerConfigurationProperties configurationProperties;

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("Authorization")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");
        final String securitySchemeName = "Authorization";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        Example externalWorkflowExample = new Example().value(
            java.util.Map.of(
                "workflowName", "external_airflow_example",
                "description",
                "This is an example of creating a workflow that executes on external Airflow. "
                    + "Configuration should be provided via Secret service. "
                    + "Details in the README: https://community.opengroup.org/osdu/platform/data-flow/ingestion/ingestion-workflow/-/tree/master#external-airflow-support",
                "registrationInstructions", Map.of(
                    "dagName", "airflow_monitoring",
                    "externalAirflowSecret", "external_airflow_secret")
            )
        );

        Components components = new Components()
            .addExamples("ExternalWorkflowExample", externalWorkflowExample)
            .addSecuritySchemes(securitySchemeName, securityScheme);

        OpenAPI openAPI = new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components)
                .info(apiInfo())
                .tags(tags());

        if(configurationProperties.isApiServerFullUrlEnabled())
            return openAPI;
        return openAPI
                .servers(Arrays.asList(new Server().url(configurationProperties.getApiServerUrl())));
    }

    private List<Tag> tags() {
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag().name("workflow-run-api").description("WorkflowRun related endpoints"));
        tags.add(new Tag().name("workflow-manager-api").description("WorkflowManager related endpoints"));
        tags.add(new Tag().name("workflow-system-manager-api").description("Workflow System Manager related endpoints"));
        tags.add(new Tag().name("health").description("Health related endpoints"));
        tags.add(new Tag().name("info").description("Version info endpoint"));
        return tags;
    }

    private Info apiInfo() {
        return new Info()
                .title(configurationProperties.getApiTitle())
                .description(configurationProperties.getApiDescription())
                .version(configurationProperties.getApiVersion())
                .license(new License().name(configurationProperties.getApiLicenseName()).url(configurationProperties.getApiLicenseUrl()))
                .contact(new Contact().name(configurationProperties.getApiContactName()).email(configurationProperties.getApiContactEmail()));
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            Parameter dataPartitionId = new Parameter()
                    .name(DpsHeaders.DATA_PARTITION_ID)
                    .description("Tenant Id")
                    .in("header")
                    .required(true)
                    .schema(new StringSchema());
            return operation.addParametersItem(dataPartitionId);
        };
    }
}
