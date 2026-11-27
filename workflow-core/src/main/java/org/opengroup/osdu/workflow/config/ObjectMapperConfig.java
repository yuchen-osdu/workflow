/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.workflow.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.opengroup.osdu.workflow.model.jackson.ForceStringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class ObjectMapperConfig {

  /**
   * Jackson 3 mapper used for HTTP message conversion. Declared as {@link JsonMapper} so it replaces
   * Spring Boot's and os-core-common's mappers. The Jackson 3 defaults that differ from the Jackson 2
   * mapper this service used before are pinned so the JSON API contract does not change.
   */
  @Bean
  @Primary
  public JsonMapper objectMapper() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new ForceStringDeserializer());
    return JsonMapper.builder()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
        .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        .changeDefaultPropertyInclusion(incl -> incl
            .withValueInclusion(JsonInclude.Include.NON_NULL)
            .withContentInclusion(JsonInclude.Include.NON_NULL))
        // Jackson 2 defaults the old mapper relied on
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        // Jackson 3 detects constructor parameter names; keep field declaration order in responses
        .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
        .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
        .disable(EnumFeature.READ_ENUMS_USING_TO_STRING)
        .addModule(module)
        .findAndAddModules()
        .build();
  }

}
