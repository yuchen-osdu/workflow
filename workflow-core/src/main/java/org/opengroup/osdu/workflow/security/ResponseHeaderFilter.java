package org.opengroup.osdu.workflow.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.api.client.util.Strings;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.ResponseHeadersFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResponseHeaderFilter implements Filter {

  @Autowired
  private DpsHeaders dpsHeaders;

  private ResponseHeadersFactory responseHeadersFactory = new ResponseHeadersFactory();

  // defaults to * for any front-end, string must be comma-delimited if more than one domain
  @Value("${ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS:*}")
  private String ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS;

  @Inject
  private JaxRsDpsLog logger;

  private static final String OPTIONS_STRING = "OPTIONS";
  private static final String FOR_HEADER_NAME = "frame-of-reference";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = getHttpServletRequest(request);
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    long startTime = getStartTime(httpRequest);

    if(!isExemptedPath(httpRequest)) {
      addFrameOfReferenceToDpsHeaders(httpRequest);
      this.dpsHeaders.addCorrelationIdIfMissing();
      addRequiredResponseHeaders(httpResponse);

      // This block handles the OPTIONS preflight requests performed by Swagger. We
      // are also enforcing requests coming from other origins to be rejected.
      if (httpRequest.getMethod().equalsIgnoreCase(OPTIONS_STRING)) {
        httpResponse.setStatus(HttpStatus.SC_OK);
      }
      chain.doFilter(request, response);

      logger.request(Request.builder()
          .requestMethod(httpRequest.getMethod())
          .latency(Duration.ofMillis(System.currentTimeMillis() - startTime))
          .requestUrl(httpRequest.getRequestURI().toLowerCase())
          .Status(httpResponse.getStatus())
          .ip(httpRequest.getRemoteAddr())
          .build());
    }
  }

  private HttpServletRequest getHttpServletRequest(ServletRequest request) throws ServletException {
    if (request instanceof HttpServletRequest) {
      return (HttpServletRequest)request;
    } else {
      throw new ServletException("Request is not HttpServletRequest");
    }
  }

  private Long getStartTime(HttpServletRequest httpRequest) {
    Object property = httpRequest.getAttribute("starttime");

    if(property == null) {
      return System.currentTimeMillis();
    } else {
      return (long)property;
    }
  }

  private boolean isExemptedPath(HttpServletRequest httpRequest) {
    String path = httpRequest.getServletPath();
    return path.endsWith("/liveness_check") || path.endsWith("/readiness_check");
  }

  private void addFrameOfReferenceToDpsHeaders(HttpServletRequest httpRequest) {
    String FORHeader = httpRequest.getHeader(FOR_HEADER_NAME);
    if (!Strings.isNullOrEmpty(FORHeader)) {
      this.dpsHeaders.put(FOR_HEADER_NAME, FORHeader);
    }
  }

  private void addRequiredResponseHeaders(HttpServletResponse httpResponse) {
    Map<String, String> responseHeaders = responseHeadersFactory
        .getResponseHeaders(ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS);
    for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
      httpResponse.addHeader(header.getKey(), header.getValue());
    }
    httpResponse.addHeader(DpsHeaders.CORRELATION_ID, this.dpsHeaders.getCorrelationId());
  }

  @Override
  public void destroy() {
  }
}
