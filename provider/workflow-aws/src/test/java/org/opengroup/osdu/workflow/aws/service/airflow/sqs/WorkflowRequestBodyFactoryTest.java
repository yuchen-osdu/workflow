/*
 * Copyright © 2021 Amazon Web Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.workflow.aws.service.airflow.sqs;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.OsduRuntimeException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@RunWith(MockitoJUnitRunner.class)
public class WorkflowRequestBodyFactoryTest {

	private final String dagName = "dagName";
	private final String runId = "runId";

	@InjectMocks
    private WorkflowRequestBodyFactory factory = new WorkflowRequestBodyFactory();

	@Mock
	private DpsHeaders originalRequestHeaders;

	@Test
	public void testGetSerializedWorkflowRequest()
	{
		Map<String, Object> inputParams = new HashMap<String, Object>();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("authorization", "authorization");
		headers.put("AppKey", "AppKey");

		Mockito.when(originalRequestHeaders.getHeaders()).thenReturn(headers);

		try (MockedConstruction<ObjectMapper> mapper = Mockito.mockConstruction(ObjectMapper.class, (mockMapper, context) -> {
            when(mockMapper.writeValueAsString(Mockito.any())).thenReturn(null);
        })) {  
		
			String result = factory.getSerializedWorkflowRequest(inputParams, dagName, runId, originalRequestHeaders, false);

			Assert.assertNotNull(result);
		}
	}

	@Test (expected = OsduRuntimeException.class)
	public void testGetSerializedWorkflowRequestException()
	{
		Map<String, Object> inputParams = new HashMap<String, Object>();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("authorization", "authorization");
		headers.put("AppKey", "AppKey");

		Mockito.when(originalRequestHeaders.getHeaders()).thenReturn(headers);

		try (MockedConstruction<ObjectMapper> mapper = Mockito.mockConstruction(ObjectMapper.class, (mockMapper, context) -> {
            when(mockMapper.writeValueAsString(Mockito.any())).thenThrow(Mockito.mock(JsonProcessingException.class));
        })) {  
		
			factory.getSerializedWorkflowRequest(inputParams, dagName, runId, originalRequestHeaders, false);
		}
	}
  
}
