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

package org.opengroup.osdu.workflow.model.jackson;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

@JacksonComponent
public class ForceStringDeserializer extends ValueDeserializer<String> {

  @Override
  public String deserialize(JsonParser parser, DeserializationContext ctxt) {
    if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
      // Same message as the Jackson 2 wrongTokenException, which Jackson 3 formats differently
      return ctxt.reportInputMismatch(String.class,
          "Unexpected token (VALUE_NUMBER_INT), expected VALUE_STRING: "
              + "Attempted to parse int to string but this is forbidden");
    }
    return parser.getValueAsString();
  }
}
