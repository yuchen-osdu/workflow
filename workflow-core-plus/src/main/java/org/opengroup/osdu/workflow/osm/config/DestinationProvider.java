/*
 *  Copyright 2020-2021 Google LLC
 *  Copyright 2020-2021 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.osm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.osm.core.model.Destination;
import org.opengroup.osdu.core.osm.core.model.Kind;
import org.opengroup.osdu.core.osm.core.model.Namespace;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DestinationProvider implements IDestinationProvider {

  private final ITenantFactory tenantFactory;

  @Override
  public Destination getDestination(String tenantName, String kindName) {
    TenantInfo tenantInfo = tenantFactory.getTenantInfo(tenantName);
    return getDestination(tenantInfo, kindName);
  }

  @Override
  public Destination getDestination(TenantInfo tenantInfo, String kindName) {
    log.debug("Providing a destination for the tenant: " + tenantInfo.getName());
    return this.buildDestination(tenantInfo, kindName);
  }

  private Destination buildDestination(TenantInfo tenantInfo, String kindName) {
    Destination destination = Destination.builder()
        .partitionId(tenantInfo.getDataPartitionId())
        .namespace(new Namespace(tenantInfo.getName()))
        .kind(new Kind(kindName))
        .build();
    log.debug("Result destination: " + destination);
    return destination;
  }
}
