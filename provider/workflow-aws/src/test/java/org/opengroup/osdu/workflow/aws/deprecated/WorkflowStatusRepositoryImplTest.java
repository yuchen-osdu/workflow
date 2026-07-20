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
// import org.opengroup.osdu.workflow.aws.WorkflowAwsApplication;
// import org.opengroup.osdu.workflow.aws.util.dynamodb.converters.WorkflowStatusDoc;
// import org.opengroup.osdu.workflow.model.WorkflowStatus;
// import org.opengroup.osdu.workflow.model.WorkflowStatusType;
// import org.springframework.boot.test.context.SpringBootTest;
// import java.util.Date;

// import static org.mockito.MockitoAnnotations.initMocks;

// @RunWith(MockitoJUnitRunner.class)
// @SpringBootTest(classes={WorkflowAwsApplication.class})
// public class WorkflowStatusRepositoryImplTest {

//     @InjectMocks
//     WorkflowStatusRepositoryImpl CUT = new WorkflowStatusRepositoryImpl();

//     @Mock
//     private DynamoDBQueryHelper queryHelper;

//     @Before
//     public void setUp() throws Exception {
//         initMocks(this);
//     }

//     @Test
//     public void findWorkflowStatus()
//     {
//         // Arrange
//         String workflowId = "TestWorkflowId";
//         String airflowRunId = "TestAirflowRunId";
//         WorkflowStatusType workflowStatusType = WorkflowStatusType.SUBMITTED;
//         Date testDate = new Date();
//         Date submittedAt = testDate;
//         String submittedBy = "TestSubmittedBy";

//         WorkflowStatus expected = new WorkflowStatus();
//         expected.setWorkflowId(workflowId);
//         expected.setAirflowRunId(airflowRunId);
//         expected.setWorkflowStatusType(workflowStatusType);
//         expected.setSubmittedAt(submittedAt);
//         expected.setSubmittedBy(submittedBy);

//         WorkflowStatusDoc expectedDoc = new WorkflowStatusDoc();
//         expectedDoc.setWorkflowId(expected.getWorkflowId());
//         expectedDoc.setAirflowRunId(expected.getAirflowRunId());
//         expectedDoc.setWorkflowStatusType(expected.getWorkflowStatusType().toString());
//         expectedDoc.setSubmittedAt(testDate);
//         expectedDoc.setSubmittedBy(expected.getSubmittedBy());

//         Mockito.when(queryHelper.loadByPrimaryKey(WorkflowStatusDoc.class, workflowId)).thenReturn(expectedDoc);

//         // Act
//         WorkflowStatus actual = CUT.findWorkflowStatus(workflowId);

//         // Assert
//         Mockito.verify(queryHelper, Mockito.times(1)).loadByPrimaryKey(WorkflowStatusDoc.class, workflowId);
//         Assert.assertEquals(actual, expected);
//     }

//     @Test
//     public void saveWorkflowStatus()
//     {
//         // Arrange
//         Date d = new Date();
//         WorkflowStatus expected = new WorkflowStatus();
//         expected.setWorkflowId("TestWorkflowId");
//         expected.setAirflowRunId("TestAirflowRunId");
//         expected.setWorkflowStatusType(WorkflowStatusType.SUBMITTED);
//         expected.setSubmittedAt(d);

//         WorkflowStatusDoc expectedDoc = new WorkflowStatusDoc();
//         expectedDoc.setWorkflowId(expected.getWorkflowId());
//         expectedDoc.setAirflowRunId(expected.getAirflowRunId());
//         expectedDoc.setWorkflowStatusType(expected.getWorkflowStatusType().toString());
//         expectedDoc.setSubmittedBy(expected.getSubmittedBy());
//         expectedDoc.setSubmittedAt(d);

//         // Act
//         WorkflowStatus actual = CUT.saveWorkflowStatus(expected);

//         // Assert
//         Mockito.verify(queryHelper, Mockito.times(1)).save(expectedDoc);
//         Assert.assertEquals(actual, expected);
//     }

//   @Test
//   public void updateWorkflowStatus()
//   {
//     String workflowId = "6893fab0-38eb-4aed-96e9-c667f1e771c8";
//     WorkflowStatusType updatedWorkflowStatusType = WorkflowStatusType.FINISHED;
//     Date testDate = new Date();

//     WorkflowStatus original = new WorkflowStatus();
//     original.setWorkflowId(workflowId);
//     original.setWorkflowStatusType(WorkflowStatusType.SUBMITTED);
//     original.setAirflowRunId("Test AirflowRunId");
//     original.setSubmittedBy("Test Submitted By");
//     original.setSubmittedAt(testDate);

//     WorkflowStatusDoc originalDoc = new WorkflowStatusDoc();
//     originalDoc.setWorkflowId(original.getWorkflowId());
//     originalDoc.setWorkflowStatusType(original.getWorkflowStatusType().toString());
//     originalDoc.setAirflowRunId(original.getAirflowRunId());
//     originalDoc.setSubmittedBy(original.getSubmittedBy());
//     originalDoc.setSubmittedAt(original.getSubmittedAt());

//     WorkflowStatus expected = new WorkflowStatus();
//     expected.setWorkflowId(workflowId);
//     expected.setWorkflowStatusType(updatedWorkflowStatusType);
//     expected.setAirflowRunId("Test AirflowRunId");
//     expected.setSubmittedBy("Test Submitted By");
//     expected.setSubmittedAt(testDate);

//     WorkflowStatusDoc expectedDoc = new WorkflowStatusDoc();
//     expectedDoc.setWorkflowId(expected.getWorkflowId());
//     expectedDoc.setWorkflowStatusType(expected.getWorkflowStatusType().toString());
//     expectedDoc.setAirflowRunId(expected.getAirflowRunId());
//     expectedDoc.setSubmittedBy(expected.getSubmittedBy());
//     expectedDoc.setSubmittedAt(testDate);

//     Mockito.when(queryHelper.loadByPrimaryKey(WorkflowStatusDoc.class, workflowId)).thenReturn(originalDoc);

//     // Act
//     WorkflowStatus actual = CUT.updateWorkflowStatus(workflowId, updatedWorkflowStatusType);

//     // Assert
//     Mockito.verify(queryHelper, Mockito.times(1)).save(expectedDoc);
//     Assert.assertEquals(actual, expected);
//   }
// }
