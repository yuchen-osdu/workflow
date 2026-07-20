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

package org.opengroup.osdu.workflow.aws.service.s3;


import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.s3.IS3ClientFactory;
import org.opengroup.osdu.core.aws.v2.s3.S3ClientWithBucket;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class WorkflowS3ClientTest {

   private static final String WORKFLOWBUCKETNAME = "workflow-bucket-name";

   private WorkflowS3Client CUT;

   @Mock
   private IS3ClientFactory s3ClientFactory;

   @BeforeEach
   void setup() {
      CUT = new WorkflowS3Client(s3ClientFactory, WORKFLOWBUCKETNAME);
   }

   @Test
   void save() {
      // Arrange
      S3Client s3 = mock(S3Client.class);
      S3ClientWithBucket s3ClientWithBucket = mock(S3ClientWithBucket.class);
      when(s3ClientWithBucket.getS3Client())
           .thenReturn(s3);
      when(s3ClientWithBucket.getBucketName())
           .thenReturn(WORKFLOWBUCKETNAME);
      when(s3ClientFactory.getS3ClientForPartition(anyString(), anyString()))
           .thenReturn(s3ClientWithBucket);

      // Act
      CUT.save("runId", "content", "data-partition");

      // Assert
      verify(s3, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
   }
}
