/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.workflow.provider.ibm.model;

import java.util.Map;

import org.opengroup.osdu.workflow.model.WorkflowMetadata;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * @author BhushanRade
 *
 */
@Getter
@Setter
@NonNull
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class WorkflowMetadataDoc {
	private String _id;
	private String _rev;
	private String workflowId;
	private String workflowName;
	private String description;
	private String createdBy;
	private Long creationTimestamp;
	private Long version;
	private Map<String, Object> registrationInstructions;

	public WorkflowMetadataDoc(WorkflowMetadata workflowMetadata) {
		super();
		this._id = workflowMetadata.getWorkflowId();
		this.workflowName = workflowMetadata.getWorkflowName();
		this.description = workflowMetadata.getDescription();
		this.createdBy = workflowMetadata.getCreatedBy();
		this.creationTimestamp = workflowMetadata.getCreationTimestamp();
		this.version = workflowMetadata.getVersion();
		this.registrationInstructions = workflowMetadata.getRegistrationInstructions();
	}

	public WorkflowMetadata getWorkflowMetadata() {
		return WorkflowMetadata.builder().workflowId(this._id)
		                          .workflowName(this.workflowName)
		                          .description(this.description)
		                          .createdBy(this.createdBy)
		                          .creationTimestamp(this.creationTimestamp)
		                          .version(this.version)
		                          .registrationInstructions(this.registrationInstructions)
		                          .build();
	}

}
