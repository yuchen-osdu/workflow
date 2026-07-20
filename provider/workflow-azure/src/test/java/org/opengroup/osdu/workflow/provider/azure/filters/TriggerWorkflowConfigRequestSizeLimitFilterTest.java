package org.opengroup.osdu.workflow.provider.azure.filters;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.workflow.provider.azure.config.TriggerWorkflowConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TriggerWorkflowConfigRequestSizeLimitFilterTest {


   private MockHttpServletResponse response;

   @Mock
   private FilterChain filterchain;


   @Mock
   private  TriggerWorkflowConfig triggerWorkflowConfig;

   @InjectMocks
   private TriggerWorkflowConfigRequestSizeLimitFilter triggerWorkflowConfigRequestSizeLimitFilter;

   private static final int VALID_CONTENT_SIZE_TRIGGER_WORKFLOW_REQUEST = 10;
   private static final int INVALID_CONTENT_SIZE_TRIGGER_WORKFLOW_REQUEST = 40;
   private static final int MAX_CONTENT_SIZE_TRIGGER_WORKFLOW_REQUEST = 20;
   private static final String URI = "api/workflow/v1/workflow/dummy/workflowRun";
   private static final String INVALID_URI = "api/workflow/v1/workflow/dummy/workflowRun";

   @BeforeEach
   public void init() {
     response = new MockHttpServletResponse();
   }

  @Test
    public void doFilterInternalWithInvalidPOSTTriggerWorkflowRequestTest() throws ServletException, IOException {

      byte[] content = new byte[INVALID_CONTENT_SIZE_TRIGGER_WORKFLOW_REQUEST];
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRequestURI(URI);
      request.setMethod(HttpMethod.POST);
      request.setContent(content);
      request.setContentType("application/json");
      when(triggerWorkflowConfig.getMaxRequestSizeInBytes()).thenReturn(MAX_CONTENT_SIZE_TRIGGER_WORKFLOW_REQUEST);
      triggerWorkflowConfigRequestSizeLimitFilter.doFilterInternal(request,response,filterchain);
      assertEquals(HttpStatus.SC_REQUEST_TOO_LONG,response.getStatus());
  }

  @Test
  public void doFilterInternalWithValidPOSTTriggerWorkflowRequestRequestTest() throws ServletException, IOException {

    byte[] content = new byte[VALID_CONTENT_SIZE_TRIGGER_WORKFLOW_REQUEST];
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(URI);
    request.setMethod(HttpMethod.POST);
    request.setContent(content);
    request.setContentType(MediaType.APPLICATION_JSON);
    when(triggerWorkflowConfig.getMaxRequestSizeInBytes()).thenReturn(MAX_CONTENT_SIZE_TRIGGER_WORKFLOW_REQUEST);
    triggerWorkflowConfigRequestSizeLimitFilter.doFilterInternal(request,response,filterchain);
  }
  @Test
  public void doFilterInternalWithAnyOtherRequestTest() throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(INVALID_URI);
    request.setMethod(HttpMethod.GET);
    triggerWorkflowConfigRequestSizeLimitFilter.doFilterInternal(request,response,filterchain);
  }
}
