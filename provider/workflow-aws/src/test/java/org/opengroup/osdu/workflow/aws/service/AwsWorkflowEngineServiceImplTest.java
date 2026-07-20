/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.workflow.aws.service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.ClientResponse;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.aws.service.airflow.sqs.WorkflowRequestBodyFactory;
import org.opengroup.osdu.workflow.aws.service.airflow.sqs.WorkflowSqsClient;
import org.opengroup.osdu.workflow.aws.service.s3.WorkflowS3Client;
import org.springframework.boot.test.context.SpringBootTest;
import org.opengroup.osdu.workflow.aws.config.AwsAirflowApiMode;
import org.opengroup.osdu.workflow.aws.config.AwsServiceConfig;
import org.opengroup.osdu.workflow.aws.repository.AwsWorkflowRunRepository;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes={WorkflowS3Client.class})
public class AwsWorkflowEngineServiceImplTest {
    @InjectMocks
    AwsWorkflowEngineServiceImpl CUT = new AwsWorkflowEngineServiceImpl();

    @Mock
    private AwsServiceConfig config;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    AwsServiceConfig awsConfig;

    @Mock
    private AirflowConfig airflowConfig;

    @Mock
    private Client restClient;

    @Mock
    DpsHeaders headers;

    @Mock
    WorkflowSqsClient sqsClient;

    @Mock
    WorkflowS3Client s3Client;

    @Mock
    private WorkflowRequestBodyFactory workflowRequestBodyFactory;

    @Mock
    AwsWorkflowRunRepository awsWorkflowRunRepository;

    @Test
    public void saveSQSMode()
    {
        // Arrange
        String runId = "test-run-id";
        String dagName = "test-dag-name";
        String serializedData = "{some-serialized-data}";
        String partitionId = "test-partition";
        String ref = "test-ref";


        WorkflowEngineRequest request = WorkflowEngineRequest.builder().runId(runId).workflowId("workflowId").dagName(dagName).workflowName(dagName).workflowEngineExecutionDate("date")
          .isSystemWorkflow(false).isDeployedThroughWorkflowService(false).build();

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("execution_context", new HashMap<>());

        // to mock
        Mockito.when(awsWorkflowRunRepository.runExists(runId))
            .thenReturn(false);

        Mockito.when(awsConfig.getAirflowApiMode())
            .thenReturn(AwsAirflowApiMode.SQS);

        Mockito.when(workflowRequestBodyFactory.getSerializedWorkflowRequest(Mockito.anyMap(), Mockito.eq(dagName),
            Mockito.eq(runId), Mockito.any(DpsHeaders.class), Mockito.anyBoolean()))
            .thenReturn(serializedData);

        Mockito.when(dpsHeaders.getPartitionId())
            .thenReturn(partitionId);

        Mockito.when(s3Client.save(Mockito.eq(runId), Mockito.eq(serializedData), Mockito.eq(partitionId)))
            .thenReturn(ref);

        Mockito.doNothing().when(sqsClient).sendMessageToWorkflowQueue(ref);

        // Act
        CUT.triggerWorkflow(request, inputData);

        // Assert
        Mockito.verify(awsWorkflowRunRepository, Mockito.times(1)).runExists(runId);

        Mockito.verify(workflowRequestBodyFactory, Mockito.times(1)).getSerializedWorkflowRequest(Mockito.anyMap(), Mockito.eq(dagName),
            Mockito.eq(runId), Mockito.any(DpsHeaders.class), Mockito.anyBoolean());

        Mockito.verify(s3Client, Mockito.times(1)).save(Mockito.eq(runId), Mockito.eq(serializedData), Mockito.eq(partitionId));

        Mockito.verify(sqsClient, Mockito.times(1)).sendMessageToWorkflowQueue(ref);
    }


    @Test
    public void saveHTTPMode() throws JSONException
    {
        // Arrange
        String runId = "test-run-id";
        String dagName = "test-dag-name";

        WorkflowEngineRequest request = WorkflowEngineRequest.builder().runId(runId).workflowId("workflowId").dagName(dagName).workflowName(dagName).workflowEngineExecutionDate("date")
          .isSystemWorkflow(false).isDeployedThroughWorkflowService(false).build();

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("execution_context", new HashMap<>());

        Map<String, String> value = new HashMap<String, String>();

        value.put("a", "b");

        inputData.put("key", value);

        // to mock
        Mockito.when(awsWorkflowRunRepository.runExists(runId))
            .thenReturn(false);

        Mockito.when(awsConfig.getAirflowApiMode())
            .thenReturn(AwsAirflowApiMode.HTTP);

        WebResource webResource = Mockito.mock(WebResource.class);

        Builder builder = Mockito.mock(Builder.class);

        ClientResponse response = Mockito.mock(ClientResponse.class);

        Mockito.when(response.getStatus()).thenReturn(200);

        Mockito.when(response.getEntity(String.class)).thenReturn("{}");

        Mockito.when(builder.method(Mockito.anyString(), Mockito.eq(ClientResponse.class), Mockito.any())).thenReturn(response);

        Mockito.when(builder.header(Mockito.anyString(), Mockito.any())).thenReturn(builder);

        Mockito.when(webResource.type(Mockito.anyString())).thenReturn(builder);

        Mockito.when(restClient.resource(Mockito.anyString())).thenReturn(webResource);

        // Act
        TriggerWorkflowResponse result = CUT.triggerWorkflow(request, inputData);

        // Assert
        Assert.assertNotNull(result);
    }


    @Test (expected = AppException.class)
    public void saveHTTPModeInvalidResponse() throws JSONException
    {
        // Arrange
        String runId = "test-run-id";
        String dagName = "test-dag-name";

        WorkflowEngineRequest request = WorkflowEngineRequest.builder().runId(runId).workflowId("workflowId").dagName(dagName).workflowName(dagName).workflowEngineExecutionDate("date")
          .isSystemWorkflow(false).isDeployedThroughWorkflowService(false).build();

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("execution_context", new HashMap<>());

        Map<String, String> value = new HashMap<String, String>();

        value.put("a", "b");

        inputData.put("key", value);

        // to mock
        Mockito.when(awsWorkflowRunRepository.runExists(runId))
            .thenReturn(false);

        Mockito.when(awsConfig.getAirflowApiMode())
            .thenReturn(AwsAirflowApiMode.HTTP);

        WebResource webResource = Mockito.mock(WebResource.class);

        Builder builder = Mockito.mock(Builder.class);

        ClientResponse response = Mockito.mock(ClientResponse.class);

        Mockito.when(response.getStatus()).thenReturn(200);

        Mockito.when(builder.method(Mockito.anyString(), Mockito.eq(ClientResponse.class), Mockito.any())).thenReturn(response);

        Mockito.when(builder.header(Mockito.anyString(), Mockito.any())).thenReturn(builder);

        Mockito.when(webResource.type(Mockito.anyString())).thenReturn(builder);

        Mockito.when(restClient.resource(Mockito.anyString())).thenReturn(webResource);

        // Act
        CUT.triggerWorkflow(request, inputData);
    }

    @Test (expected = AppException.class)
    public void saveHTTPModeNon200Code() throws JSONException
    {
        // Arrange
        String runId = "test-run-id";
        String dagName = "test-dag-name";

        WorkflowEngineRequest request = WorkflowEngineRequest.builder().runId(runId).workflowId("workflowId").dagName(dagName).workflowName(dagName).workflowEngineExecutionDate("date")
          .isSystemWorkflow(false).isDeployedThroughWorkflowService(false).build();

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("execution_context", new HashMap<>());

        Map<String, String> value = new HashMap<String, String>();

        value.put("a", "b");

        inputData.put("key", value);

        // to mock
        Mockito.when(awsWorkflowRunRepository.runExists(runId))
            .thenReturn(false);

        Mockito.when(awsConfig.getAirflowApiMode())
            .thenReturn(AwsAirflowApiMode.HTTP);

        WebResource webResource = Mockito.mock(WebResource.class);

        Builder builder = Mockito.mock(Builder.class);

        ClientResponse response = Mockito.mock(ClientResponse.class);

        Mockito.when(response.getStatus()).thenReturn(400);

        Mockito.when(builder.method(Mockito.anyString(), Mockito.eq(ClientResponse.class), Mockito.any())).thenReturn(response);

        Mockito.when(builder.header(Mockito.anyString(), Mockito.any())).thenReturn(builder);

        Mockito.when(webResource.type(Mockito.anyString())).thenReturn(builder);

        Mockito.when(restClient.resource(Mockito.anyString())).thenReturn(webResource);

        // Act
        CUT.triggerWorkflow(request, inputData);

    }
}
