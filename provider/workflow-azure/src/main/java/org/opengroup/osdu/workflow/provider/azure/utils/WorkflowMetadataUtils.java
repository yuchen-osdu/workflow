package org.opengroup.osdu.workflow.provider.azure.utils;

import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.azure.config.AzureWorkflowEngineConfig;
import org.opengroup.osdu.workflow.provider.azure.model.WorkflowMetadataDoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowMetadataUtils {
  protected static final String KEY_DAG_CONTENT = "dagContent";

  public static WorkflowMetadataDoc buildWorkflowMetadataDoc(AzureWorkflowEngineConfig workflowEngineConfig, final WorkflowMetadata workflowMetadata) {
    // If we need to save multiple versions of workflow, then choose id as guid and get becomes a query.
    // This is to avoid conflicts. Only one combination of Id and partition key should exist.
    Map<String, Object> registrationInstructionForMetadata =
        new HashMap<>(workflowMetadata.getRegistrationInstructions());
    String dagContent =
        (String) registrationInstructionForMetadata.remove(KEY_DAG_CONTENT);
    if (workflowEngineConfig.getIgnoreDagContent()) {
      dagContent = "";
    }

    return WorkflowMetadataDoc.builder()
        .id(workflowMetadata.getWorkflowName())
        .partitionKey(workflowMetadata.getWorkflowName())
        .workflowName(workflowMetadata.getWorkflowName())
        .description(workflowMetadata.getDescription())
        .createdBy(workflowMetadata.getCreatedBy())
        .creationTimestamp(workflowMetadata.getCreationTimestamp())
        .version(workflowMetadata.getVersion())
        .isRegisteredByWorkflowService(
            dagContent != null && !dagContent.isEmpty())
        .registrationInstructions(registrationInstructionForMetadata).build();
  }

  public static WorkflowMetadata buildWorkflowMetadata(final WorkflowMetadataDoc workflowMetadataDoc) {
    return WorkflowMetadata.builder()
        .workflowId(workflowMetadataDoc.getId())
        .workflowName(workflowMetadataDoc.getWorkflowName())
        .description(workflowMetadataDoc.getDescription())
        .createdBy(workflowMetadataDoc.getCreatedBy())
        .creationTimestamp(workflowMetadataDoc.getCreationTimestamp())
        .version(workflowMetadataDoc.getVersion())
        .isDeployedThroughWorkflowService(workflowMetadataDoc.getIsRegisteredByWorkflowService())
        .registrationInstructions(workflowMetadataDoc.getRegistrationInstructions()).build();
  }

  public static SqlQuerySpec buildSqlQuerySpecForGetAllWorkflow(String prefix) {
    SqlQuerySpec sqlQuerySpec;
    if(prefix != null && !(prefix.isEmpty())) {
      SqlParameter prefixParameter = new SqlParameter("@prefix", prefix);
      sqlQuerySpec = new SqlQuerySpec("SELECT * FROM c " +
          "where STARTSWITH(c.workflowName, @prefix, true) " +
          "ORDER BY c._ts DESC", prefixParameter);
    }
    else {
      sqlQuerySpec = new SqlQuerySpec("SELECT * FROM c " +
          "ORDER BY c._ts DESC");
    }
    return sqlQuerySpec;
  }

  public static List<WorkflowMetadata> convertWorkflowMetadataDocsToWorkflowMetadataList(List<WorkflowMetadataDoc> workflowMetadataDocs) {
    List<WorkflowMetadata> workflowMetadataList = new ArrayList<>();
    for (WorkflowMetadataDoc workflowMetadataDoc: workflowMetadataDocs) {
      workflowMetadataList.add(buildWorkflowMetadata(workflowMetadataDoc));
    }
    return workflowMetadataList;
  }
}
