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

package org.opengroup.osdu.workflow.aws.gsm;


import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.v2.sns.AmazonSNSConfig;
import org.opengroup.osdu.core.aws.v2.sns.PublishRequestBuilder;
import org.opengroup.osdu.core.aws.v2.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.common.exception.CoreException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.status.Message;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;


@RunWith(MockitoJUnitRunner.class)
public class AwsWorkflowStatusPublisherTest {

    private final String amazonSnsTopic = "amazonSnsTopic";

    @InjectMocks
    private AwsWorkflowStatusPublisher publisher = new AwsWorkflowStatusPublisher();

    @Mock
    SnsClient snsClient;

    @Test
    public void testPublish()
    {
        Message[] messages = new Message[1];
        messages[0] = Mockito.mock(Message.class);

        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("key", "value");
        attributeMap.put(DpsHeaders.DATA_PARTITION_ID, "partition");
        attributeMap.put(DpsHeaders.CORRELATION_ID, "correlation");

        PublishRequest publishRequest = Mockito.mock(PublishRequest.class);

        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
            when(mockProvider.getParameterAsStringOrDefault(Mockito.anyString(), Mockito.any())).thenReturn(amazonSnsTopic);
        })) {
            try (MockedConstruction<AmazonSNSConfig> snsConfig = Mockito.mockConstruction(AmazonSNSConfig.class, (mockSnsConfig, context) -> {
                when(mockSnsConfig.AmazonSNS()).thenReturn(snsClient);
            })) {
                try (MockedConstruction<PublishRequestBuilder> builder= Mockito.mockConstruction(PublishRequestBuilder.class, (mockBuilder, context) -> {
                when(mockBuilder.generatePublishRequest(Mockito.anyString(),Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(publishRequest);
                })) {
                    publisher.init();

                    publisher.publish(messages, attributeMap);

                    Mockito.verify(snsClient, Mockito.times(1)).publish(publishRequest);
                }
            }
        }
    }


    @Test (expected = CoreException.class)
    public void testPublishNoMsg()
    {
        Message[] messages = new Message[0];

        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("key", "value");

        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
            when(mockProvider.getParameterAsStringOrDefault(Mockito.anyString(), Mockito.any())).thenReturn(amazonSnsTopic);
        })) {
            try (MockedConstruction<AmazonSNSConfig> snsConfig = Mockito.mockConstruction(AmazonSNSConfig.class, (mockSnsConfig, context) -> {
                when(mockSnsConfig.AmazonSNS()).thenReturn(snsClient);
            })) {
                publisher.init();

                publisher.publish(messages, attributeMap);
            }
        }
    }

    @Test (expected = CoreException.class)
    public void testPublishEmptyMap()
    {
        Message[] messages = new Message[1];
        messages[0] = Mockito.mock(Message.class);

        Map<String, String> attributeMap = new HashMap<String, String>();

        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
            when(mockProvider.getParameterAsStringOrDefault(Mockito.anyString(), Mockito.any())).thenReturn(amazonSnsTopic);
        })) {
            try (MockedConstruction<AmazonSNSConfig> snsConfig = Mockito.mockConstruction(AmazonSNSConfig.class, (mockSnsConfig, context) -> {
                when(mockSnsConfig.AmazonSNS()).thenReturn(snsClient);
            })) {
                publisher.init();

                publisher.publish(messages, attributeMap);
            }
        }
    }

    @Test (expected = CoreException.class)
    public void testPublishNoPartition()
    {
        Message[] messages = new Message[1];
        messages[0] = Mockito.mock(Message.class);

        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("key", "value");

        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
            when(mockProvider.getParameterAsStringOrDefault(Mockito.anyString(), Mockito.any())).thenReturn(amazonSnsTopic);
        })) {
            try (MockedConstruction<AmazonSNSConfig> snsConfig = Mockito.mockConstruction(AmazonSNSConfig.class, (mockSnsConfig, context) -> {
                when(mockSnsConfig.AmazonSNS()).thenReturn(snsClient);
            })) {
                publisher.init();

                publisher.publish(messages, attributeMap);
            }
        }
    }

    @Test (expected = CoreException.class)
    public void testPublishNoCorrelation()
    {
        Message[] messages = new Message[1];
        messages[0] = Mockito.mock(Message.class);

        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("key", "value");
        attributeMap.put(DpsHeaders.DATA_PARTITION_ID, "partition");

        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
            when(mockProvider.getParameterAsStringOrDefault(Mockito.anyString(), Mockito.any())).thenReturn(amazonSnsTopic);
        })) {
            try (MockedConstruction<AmazonSNSConfig> snsConfig = Mockito.mockConstruction(AmazonSNSConfig.class, (mockSnsConfig, context) -> {
                when(mockSnsConfig.AmazonSNS()).thenReturn(snsClient);
            })) {
                publisher.init();

                publisher.publish(messages, attributeMap);
            }
        }
    }
}