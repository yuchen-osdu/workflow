/*
  Copyright 2021 Google LLC
  Copyright 2021 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package org.opengroup.osdu.workflow.logging;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.util.IpAddressUtil;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequiredArgsConstructor
@RequestScope
public class AuditLogger {
	private final JaxRsDpsLog logger;
	private final DpsHeaders headers;
	private final HttpServletRequest httpServletRequest;

  private AuditEvents events = null;

  private AuditEvents getAuditEvents() {
    if (this.events == null) {
      String userIpAddress = IpAddressUtil.getClientIpAddress(httpServletRequest);
      String userAgent = httpServletRequest.getHeader("user-agent");
      String userAuthorizedGroupName = headers.getUserAuthorizedGroupName();
      this.events = new AuditEvents(this.headers.getUserEmail(), userIpAddress, userAgent, userAuthorizedGroupName);
    }
    return this.events;
  }

  public void workflowCreateEvent(List<String> resources) {
    this.writeLog(this.getAuditEvents().getWorkflowCreateSuccessEvent(resources));
  }

  public void workflowUpdateEvent(List<String> resources) {
    this.writeLog(this.getAuditEvents().getWorkflowUpdateSuccessEvent(resources));
  }

  public void workflowDeleteEvent(List<String> resources) {
    this.writeLog(this.getAuditEvents().getWorkflowDeleteSuccessEvent(resources));
  }

  public void workflowRunEvent(List<String> resources) {
    this.writeLog(this.getAuditEvents().getWorkflowRunSuccessEvent(resources));
  }

  private void writeLog(AuditPayload log) {
    this.logger.audit(log);
  }
}
