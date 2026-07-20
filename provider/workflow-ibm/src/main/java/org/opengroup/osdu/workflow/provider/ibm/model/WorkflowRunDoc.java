/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.workflow.provider.ibm.model;

import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkflowRunDoc {
	private String _id;
	private String _rev;
	private String workflowName;
	//private String runId;
	private Long startTimeStamp;
	private Long endTimeStamp;
	private WorkflowStatusType status;
	private String submittedBy;
	private String workflowEngineExecutionDate;
	
	public WorkflowRunDoc(WorkflowRun workflowRun) {
		super();
		this._id = workflowRun.getRunId();
		//this.runId= workflowRun.getRunId();
		this.workflowName = workflowRun.getWorkflowName();
		this.startTimeStamp = workflowRun.getStartTimeStamp();
		this.endTimeStamp = workflowRun.getEndTimeStamp();
		this.status = workflowRun.getStatus();
		this.submittedBy = workflowRun.getSubmittedBy();
		this.workflowEngineExecutionDate = workflowRun.getWorkflowEngineExecutionDate();
	}
	
	public WorkflowRun getWorkflowRun() {
		return WorkflowRun.builder()
				//.workflowId(this.get_id())
				.workflowName(this.getWorkflowName())
				.runId(this.get_id())
				.startTimeStamp(getStartTimeStamp())
				.endTimeStamp(getEndTimeStamp())
				.status(getStatus())
				.submittedBy(getSubmittedBy())
				.workflowEngineExecutionDate(getWorkflowEngineExecutionDate())
				.build();			
	}

}
