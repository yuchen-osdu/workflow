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

// import org.junit.Assert;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.Mockito;
// import org.mockito.runners.MockitoJUnitRunner;
// import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelper;
// import org.opengroup.osdu.core.common.model.WorkflowType;
// import org.opengroup.osdu.workflow.aws.WorkflowAwsApplication;
// import org.opengroup.osdu.workflow.aws.util.dynamodb.converters.IngestionStrategyDoc;
// import org.opengroup.osdu.workflow.model.IngestionStrategy;
// import org.springframework.boot.test.context.SpringBootTest;

// import static org.mockito.MockitoAnnotations.initMocks;

// @RunWith(MockitoJUnitRunner.class)
// @SpringBootTest(classes={WorkflowAwsApplication.class})
// public class IngestionStrategyRepositoryImplTest {

//     @InjectMocks
//     IngestionStrategyRepositoryImpl CUT = new IngestionStrategyRepositoryImpl();

//     @Mock
//     private DynamoDBQueryHelper queryHelper;

//     @Before
//     public void setUp() {
//         initMocks(this);
//     }

//     @Test
//     public void findByWorkflowTypeAndDataTypeAndUserId()
//     {
//         // Arrange
//         WorkflowType workflowType = WorkflowType.INGEST;
//         String dataType = "Opaque";
//         String userId = "TestUserId";
//         String compositeKey = "INGEST:Opaque:TestUserId";

//         IngestionStrategyDoc expectedDoc = new IngestionStrategyDoc();
//         expectedDoc.setWorkflowType(workflowType.toString());
//         expectedDoc.setDataType(dataType);
//         expectedDoc.setUserId(userId);
//         expectedDoc.setDagName("TestDagName");

//         IngestionStrategy expected = new IngestionStrategy();
//         expected.setWorkflowType(WorkflowType.valueOf(expectedDoc.getWorkflowType()));
//         expected.setDataType(expectedDoc.getDataType());
//         expected.setUserId(expectedDoc.getUserId());
//         expected.setDagName(expectedDoc.getDagName());

//         Mockito.when(queryHelper.loadByPrimaryKey(IngestionStrategyDoc.class, compositeKey)).thenReturn(expectedDoc);

//         // Act
//         IngestionStrategy actual = CUT.findByWorkflowTypeAndDataTypeAndUserId(workflowType, dataType, userId);

//         // Assert
//         Mockito.verify(queryHelper, Mockito.times(1)).loadByPrimaryKey(IngestionStrategyDoc.class, compositeKey);
//         Assert.assertEquals(actual, expected);
//     }
// }
