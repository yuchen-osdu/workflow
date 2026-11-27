package org.opengroup.osdu.workflow.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class WebConfigurationTest {

  private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
      .addFilters(new WebConfiguration().trailingSlashFilter().getFilter())
      .build();

  @ParameterizedTest
  @ValueSource(strings = {"/v1/workflow/wf/workflowRun", "/v1/workflow/wf/workflowRun/"})
  void shouldPreserveBodyAndQueryWithEitherTrailingSlash(String path) throws Exception {
    mvc.perform(post(path).queryParam("prefix", "p1")
            .contentType(MediaType.APPLICATION_JSON).content("{\"runId\":\"r1\"}"))
        .andExpect(status().isOk())
        .andExpect(content().string("wf:p1:{\"runId\":\"r1\"}"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/v1/workflow", "/v1/workflow/"})
  void shouldMatchRootMappingWithEitherTrailingSlash(String path) throws Exception {
    mvc.perform(get(path).queryParam("prefix", "Hello"))
        .andExpect(status().isOk())
        .andExpect(content().string("Hello"));
  }

  // Non-static so component scanning in other tests does not register it as a controller.
  @RestController
  class ProbeController {
    @PostMapping("/v1/workflow/{name}/workflowRun")
    public String trigger(@PathVariable("name") String name, @RequestParam("prefix") String prefix,
        @RequestBody String body) {
      return name + ":" + prefix + ":" + body;
    }

    @GetMapping("/v1/workflow")
    public String list(@RequestParam("prefix") String prefix) {
      return prefix;
    }
  }
}
