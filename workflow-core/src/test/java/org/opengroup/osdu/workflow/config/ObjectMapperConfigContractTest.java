package org.opengroup.osdu.workflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.workflow.exception.handler.ApiError;
import org.opengroup.osdu.workflow.exception.handler.ConflictApiError;
import org.opengroup.osdu.workflow.model.CreateWorkflowRequest;
import org.opengroup.osdu.workflow.model.TriggerWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowRunResponse;
import org.opengroup.osdu.workflow.model.WorkflowRunsPage;
import org.opengroup.osdu.workflow.model.WorkflowStatus;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;
import org.opengroup.osdu.workflow.model.UpdateWorkflowRunRequest;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the HTTP JSON contract of the Jackson 3 mapper to what the Jackson 2 mapper produced and
 * accepted before the Spring Boot 4 migration.
 */
class ObjectMapperConfigContractTest {

  private final JsonMapper jackson3 = new ObjectMapperConfig().objectMapper();
  private final com.fasterxml.jackson.databind.ObjectMapper jackson2 = previousJackson2Mapper();

  static Stream<Arguments> responses() {
    Map<String, Object> instructions = new LinkedHashMap<>();
    instructions.put("zeta", 1);
    instructions.put("alpha", Map.of("dagName", "my-dag"));
    instructions.put("nothing", null);
    WorkflowMetadata metadata = WorkflowMetadata.builder()
        .workflowId("wf-id").workflowName("wf").description("desc").createdBy("user")
        .creationTimestamp(1600000000000L).version(3L).registrationInstructions(instructions)
        .engineVersion("airflow3").build();
    metadata.setIsSystemWorkflow(true);
    metadata.setIsDeployedThroughWorkflowService(true);
    WorkflowRun run = WorkflowRun.builder().workflowId("wf-id").workflowName("wf").runId("run")
        .startTimeStamp(1L).status(WorkflowStatusType.SUCCESS).submittedBy("user")
        .workflowEngineExecutionDate("2024-01-01T00:00:00").engineVersion("airflow2").build();
    return Stream.of(
        Arguments.of(metadata),
        Arguments.of(WorkflowMetadata.builder().workflowName("only-name").build()),
        Arguments.of(run),
        Arguments.of(new WorkflowRunsPage(List.of(run), "cursor")),
        Arguments.of(WorkflowRunResponse.builder().workflowId("wf-id").runId("run")
            .startTimeStamp(1L).endTimeStamp(2L).status(WorkflowStatusType.RUNNING)
            .submittedBy("user").build()),
        Arguments.of(WorkflowStatus.builder().workflowId("wf-id").airflowRunId("run")
            .workflowStatusType(WorkflowStatusType.FINISHED).submittedAt(new Date(1600000000123L))
            .submittedBy("user").build()),
        Arguments.of(new UpdateWorkflowRunRequest(WorkflowStatusType.FAILED)),
        Arguments.of(new ApiError(HttpStatus.BAD_REQUEST, "Validation failed", List.of("e1"))),
        Arguments.of(ConflictApiError.ConflictErrorBuilder().message("exists").errors(List.of("e"))
            .conflictId("wf-id").build()),
        Arguments.of(new AppError(409, "Conflict", "already exists")),
        Arguments.of(Map.of("message", "x")));
  }

  @ParameterizedTest
  @MethodSource("responses")
  void serializationShouldMatchJackson2Mapper(Object value) throws Exception {
    assertThat(jackson3.writeValueAsString(value)).isEqualTo(jackson2.writeValueAsString(value));
  }

  static Stream<Arguments> requests() {
    return Stream.of(
        Arguments.of(CreateWorkflowRequest.class,
            "{\"workflowName\":\"wf\",\"description\":\"d\",\"registrationInstructions\":{\"a\":1}}"),
        Arguments.of(TriggerWorkflowRequest.class,
            "{\"runId\":\"r\",\"executionContext\":{\"id\":7,\"nested\":{\"k\":[1,\"v\"]}}}"),
        Arguments.of(TriggerWorkflowRequest.class, "{\"executionContext\":{}}"),
        Arguments.of(UpdateWorkflowRunRequest.class, "{\"status\":\"finished\"}"),
        Arguments.of(WorkflowMetadata.class,
            "{\"workflowName\":\"wf\",\"isSystemWorkflow\":true,\"version\":2,"
                + "\"isDeployedThroughWorkflowService\":null}"));
  }

  @ParameterizedTest
  @MethodSource("requests")
  void deserializationShouldMatchJackson2Mapper(Class<?> type, String json) throws Exception {
    Object fromJackson3 = jackson3.readValue(json, type);
    Object fromJackson2 = jackson2.readValue(json, type);
    assertThat(fromJackson3).usingRecursiveComparison().isEqualTo(fromJackson2);
  }

  static Stream<Arguments> rejectedRequests() {
    return Stream.of(
        Arguments.of(CreateWorkflowRequest.class, "{\"workflowName\":1}"),
        Arguments.of(CreateWorkflowRequest.class, "{\"workflowName\":\"wf\",\"unknown\":1}"),
        Arguments.of(TriggerWorkflowRequest.class, "{\"runId\":42}"),
        Arguments.of(UpdateWorkflowRunRequest.class, "{\"status\":0}"),
        Arguments.of(UpdateWorkflowRunRequest.class, "{\"status\":\"FINISHED\"}"));
  }

  @ParameterizedTest
  @MethodSource("rejectedRequests")
  void shouldRejectWhatJackson2MapperRejected(Class<?> type, String json) {
    assertThatThrownBy(() -> jackson2.readValue(json, type)).isInstanceOf(IOException.class);
    assertThatThrownBy(() -> jackson3.readValue(json, type)).isInstanceOf(JacksonException.class);
  }

  @Test
  void intForStringErrorMessageShouldMatchJackson2Mapper() {
    String json = "{\"workflowName\":1}";
    Throwable fromJackson2 = catchThrowable(() -> jackson2.readValue(json, CreateWorkflowRequest.class));
    Throwable fromJackson3 = catchThrowable(() -> jackson3.readValue(json, CreateWorkflowRequest.class));
    assertThat(((JacksonException) fromJackson3).getOriginalMessage())
        .isEqualTo(((com.fasterxml.jackson.core.JacksonException) fromJackson2).getOriginalMessage());
  }

  @Test
  void shouldKeepWritingEnumsAndDatesTheSameWay() throws Exception {
    assertThat(jackson3.writeValueAsString(WorkflowStatusType.SUBMITTED)).isEqualTo("\"submitted\"");
    assertThat(jackson3.writeValueAsString(new Date(5L))).isEqualTo("5");
  }

  private static com.fasterxml.jackson.databind.ObjectMapper previousJackson2Mapper() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new JsonDeserializer<String>() {
      @Override
      public String deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
          throw ctxt.wrongTokenException(parser, String.class, JsonToken.VALUE_STRING,
              "Attempted to parse int to string but this is forbidden");
        }
        return parser.getValueAsString();
      }
    });
    return new com.fasterxml.jackson.databind.ObjectMapper()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(module)
        .findAndRegisterModules();
  }
}
