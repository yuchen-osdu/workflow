/*
 *  Copyright 2020-2025 Google LLC
 *  Copyright 2020-2025 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.workflow.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalAirflowConfig {
  String airflowVersion;
  String airflowApiClientType;
  Map<String, Object> configMap;

  public String getStringValue(String key) {
    if (!configMap.containsKey(key)) {
      throw new AppException(
          HttpStatus.SC_BAD_REQUEST,
          "Config value not found",
          "Config value %s not found".formatted(key));
    }
    return (String) configMap.get(key);
  }
}
