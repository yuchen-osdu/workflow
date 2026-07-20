package org.opengroup.osdu.workflow.util;

import static org.opengroup.osdu.workflow.consts.DefaultVariable.DEFAULT_DATA_PARTITION_ID_TENANT1;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.consts.TestConstants.HEADER_CORRELATION_ID;
import static org.opengroup.osdu.workflow.consts.TestConstants.HEADER_DATA_PARTITION_ID;
import static org.opengroup.osdu.workflow.consts.TestConstants.HEADER_USER;

import com.google.api.client.util.Strings;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import lombok.ToString;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

@Log
@ToString
public class HTTPClient {

	private OpenIDTokenProvider openIDTokenProvider;
	public static final String INTEGRATION_TESTER_TOKEN = "PRIVILEGED_USER_TOKEN";
	public static final String INTEGRATION_TESTER_NO_ACCESS_TOKEN = "NO_ACCESS_USER_TOKEN";

	protected static String token = null;
	protected static String noAccessToken = null;

	public HTTPClient() {
		token = System.getProperty(INTEGRATION_TESTER_TOKEN, System.getenv(INTEGRATION_TESTER_TOKEN));
		noAccessToken = System.getProperty(INTEGRATION_TESTER_NO_ACCESS_TOKEN,
				System.getenv(INTEGRATION_TESTER_NO_ACCESS_TOKEN));

		if (Strings.isNullOrEmpty(token) || Strings.isNullOrEmpty(noAccessToken)) {
			openIDTokenProvider = new OpenIDTokenProvider();

			if (Strings.isNullOrEmpty(token))
				token = openIDTokenProvider.getToken();

			if (Strings.isNullOrEmpty(noAccessToken))
				noAccessToken = openIDTokenProvider.getNoAccessToken();
		}
	}

	public String getAccessToken() {
		return "Bearer " + token;
	}

	public String getNoDataAccessToken() {
		return "Bearer " + noAccessToken;
	}

	private static Client getClient() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception ignored) {
		}
		return Client.create();
	}

	public ClientResponse send(String httpMethod, String url, String payLoad, Map<String, String> headers,
			String token) {
		ClientResponse response;
		try {
			if (StringUtils.isBlank(headers.get(HEADER_CORRELATION_ID))) {
				String correlationId = java.util.UUID.randomUUID().toString();
				headers.put(HEADER_CORRELATION_ID, correlationId);
			}
			log.info(String.format("Request correlation id: %s", headers.get(HEADER_CORRELATION_ID)));
			Client client = getClient();
			client.setReadTimeout(180000);
			client.setConnectTimeout(10000);
			WebResource webResource = client.resource(url);
			log.info("[%s] URL = %s".formatted(httpMethod, url));
			response = this.getClientResponse(httpMethod, payLoad, webResource, headers, token);
		} catch (Exception e) {
			throw new AssertionError("Error: Send request error", e);
		}
		log.info("waiting on response");
		return response;
	}

	private ClientResponse getClientResponse(String httpMethod, String requestBody, WebResource webResource,
			Map<String, String> headers, String token) {
		final WebResource.Builder builder = webResource.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON).header("Authorization", token);
		headers.forEach(builder::header);
		log.info("making request to workflow api");
		return builder.method(httpMethod, ClientResponse.class, requestBody);
	}

	public Map<String, String> getCommonHeader() {
		Map<String, String> headers = new HashMap<>();
		headers.put(HEADER_DATA_PARTITION_ID, getEnvironmentVariableOrDefaultKey(DEFAULT_DATA_PARTITION_ID_TENANT1));
		headers.put(HEADER_USER, "testUser");
		return headers;
	}

	public static Map<String, String> overrideHeader(Map<String, String> currentHeaders, String... partitions) {
		String value = String.join(",", partitions);
		currentHeaders.put(HEADER_DATA_PARTITION_ID, value);
		return currentHeaders;
	}

	public Map<String, String> getCommonHeaderWithoutPartition() {
		Map<String, String> headers = new HashMap<>();
		headers.put(HEADER_DATA_PARTITION_ID, "");
		headers.put(HEADER_USER, "testUser");
		return headers;
	}
}
