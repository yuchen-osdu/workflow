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

// package org.opengroup.osdu.workflow.aws.repository;


// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.Mockito;
// import org.mockito.internal.util.reflection.Whitebox;
// import org.mockito.runners.MockitoJUnitRunner;
// import org.opengroup.osdu.workflow.aws.WorkflowAwsApplication;
// import org.opengroup.osdu.workflow.aws.service.AirflowClient;
// import org.opengroup.osdu.workflow.aws.service.SubmitIngestServiceImpl;
// import org.opengroup.osdu.workflow.aws.util.dynamodb.converters.WorkflowStatusDoc;
// import org.springframework.boot.test.context.SpringBootTest;

// import java.io.IOException;
// import java.util.HashMap;
// import java.util.Map;

// @RunWith(MockitoJUnitRunner.class)
// @SpringBootTest(classes={WorkflowAwsApplication.class})
// public class SubmitIngestServiceImplTest {

//     @InjectMocks
//     SubmitIngestServiceImpl repo;

//     @Mock
//     AirflowClient airflowClient;

//     @Test
//     public void testSubmitIngest() throws IOException {
//         // Arrange
//         String dagName = "dagTest";
//         String airflowBaseUrl = "test url";
//         Map<String, Object> data = new HashMap<>();
//         data.put("test", "testing");

//         Whitebox.setInternalState(repo, "airflowBaseUrl", airflowBaseUrl);

//         Mockito.doNothing().when(airflowClient).makeRequestToAirflow(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

//         // Act
//         repo.submitIngest(dagName, data);

//         // Assert
//         Mockito.verify(airflowClient, Mockito.times(1))
//                 .makeRequestToAirflow("test url/api/experimental/dags/dagTest/dag_runs", "{\"test\":\"testing\"}", dagName);
//     }
// }
