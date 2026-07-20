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


import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

@RunWith(MockitoJUnitRunner.class)
public class AwsAirflowHttpClientTest  {

	private final String airflowDagURL = "airflowDagURL";
	private final String body = "body";
	private final String dagName = "dagName";


	@InjectMocks
    private AwsAirflowHttpClient client = new AwsAirflowHttpClient();

	@Mock
	private DpsHeaders originalRequestHeaders;

	@Test
	public void testMakeRequestToAirflow() throws IOException
	{
		HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);

		Mockito.when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

		InputStream is = Mockito.mock(InputStream.class);

		when(connection.getInputStream()).thenReturn(is);

		try (MockedConstruction<URL> url = Mockito.mockConstruction(URL.class, (mockUrl, context) -> {
            when(mockUrl.openConnection()).thenReturn(connection);
        })) {   
			try (MockedConstruction<DataOutputStream> writer = Mockito.mockConstruction(DataOutputStream.class, (mockWriter, context) -> {
        	})) {
				try (MockedConstruction<BufferedReader> reader = Mockito.mockConstruction(BufferedReader.class, (mockReader, context) -> {
					when(mockReader.readLine()).thenReturn(null);
        		})) {  
					client.makeRequestToAirflow(airflowDagURL, body, dagName, originalRequestHeaders);
					Mockito.verify(connection, Mockito.times(1)).getOutputStream();
				}
			}
		}
	}

	@Test (expected = AppException.class)
	public void testMakeRequestToAirflowNoResponse() throws IOException
	{
		HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);

		Mockito.when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

		InputStream is = Mockito.mock(InputStream.class);

		when(connection.getInputStream()).thenReturn(is);

		try (MockedConstruction<URL> url = Mockito.mockConstruction(URL.class, (mockUrl, context) -> {
            when(mockUrl.openConnection()).thenReturn(connection);
        })) {   
			try (MockedConstruction<DataOutputStream> writer = Mockito.mockConstruction(DataOutputStream.class, (mockWriter, context) -> {
        	})) {
				try (MockedConstruction<BufferedReader> reader = Mockito.mockConstruction(BufferedReader.class, (mockReader, context) -> {
					when(mockReader.readLine()).thenReturn(null);
        		})) {  
					client.makeRequestToAirflow(airflowDagURL, body, dagName, originalRequestHeaders);
					Mockito.verify(connection, Mockito.times(1)).getOutputStream();
				}
			}
		}
	}
}
