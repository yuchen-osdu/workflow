package org.opengroup.osdu.workflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

class SharedAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
      .withUserConfiguration(SharedAutoConfiguration.class);

  @Test
  void sharedJacksonPropertiesShouldBindToJackson3Mapper() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      JsonMapper jsonMapper = context.getBean(JsonMapper.class);
      assertThat(jsonMapper.isEnabled(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)).isTrue();
      assertThat(jsonMapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isFalse();
    });
  }
}
