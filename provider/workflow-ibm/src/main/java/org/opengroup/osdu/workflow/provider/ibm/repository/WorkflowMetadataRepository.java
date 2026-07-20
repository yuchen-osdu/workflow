/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.workflow.provider.ibm.repository;

import static com.cloudant.client.api.query.Expression.eq;
import static com.cloudant.client.api.query.Expression.regex;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.ibm.config.IBMCouchDB;
import org.opengroup.osdu.workflow.provider.ibm.model.WorkflowMetadataDoc;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.query.Expression;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.org.lightcouch.DocumentConflictException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowMetadataRepository implements IWorkflowMetadataRepository {

	@Autowired
	IBMCouchDB ibmCouchDB;

	final private String COLLECTION_NAME="WorkflowMetadata";

	@Inject
	TenantInfo tenantInfo;

	@Override
	public WorkflowMetadata createWorkflow(WorkflowMetadata workflowMetadata) {
		Response response = null;
		Database db = getDatabase();
		WorkflowMetadataDoc workflowMetadataDoc = new WorkflowMetadataDoc(workflowMetadata);
		QueryResult<WorkflowMetadataDoc> result = db.query(new QueryBuilder(eq("workflowName", workflowMetadata.getWorkflowName())).build(), WorkflowMetadataDoc.class);
		if(result.getDocs().size()>0) {
			throw new AppException(HttpStatus.SC_CONFLICT, "Conflict", String.format("Workflow with name %s already exists", workflowMetadata.getWorkflowName()));
		}
		try {
			response = db.save(workflowMetadataDoc);
		} catch (DocumentConflictException e) {
			throw new AppException(HttpStatus.SC_CONFLICT, "Conflict", String.format("Workflow with name %s already exists", workflowMetadata.getWorkflowName()));
		} catch (Exception e) {
			log.error("Workflow creation failed", e);
			e.printStackTrace();
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed", "Workflow creation failed", e);
		}
		if (response.getStatusCode() == 201)
			return workflowMetadata;
		else
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed..", "Workflow creation failed");

	}

	private Database getDatabase() {
	return ibmCouchDB.getDatabase(tenantInfo.getDataPartitionId(), COLLECTION_NAME);
}

	@Override
	public WorkflowMetadata getWorkflow(String workflowName) {
		Database db = getDatabase();
		QueryResult<WorkflowMetadataDoc> result = null;
		try {
			result = db.query(new QueryBuilder(eq("workflowName", workflowName)).build(), WorkflowMetadataDoc.class);
		} catch (Exception e) {
			log.error(String.format("Workflow: %s doesn't exist", workflowName), e);
			e.printStackTrace();
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unexpected error", "Workflow doesn't exist", e);
		}
		if(result != null && result.getDocs().isEmpty()) {
			final String errorMessage = String.format("Workflow: %s doesn't exist", workflowName);
			log.error(errorMessage);
			throw new WorkflowNotFoundException(errorMessage);
		} else
			return result.getDocs().get(0).getWorkflowMetadata();
	}

	@Override
	public void deleteWorkflow(String workflowName) {
		Database db = getDatabase();
		QueryResult<WorkflowMetadataDoc> result = null;
		try {
			result = db.query(new QueryBuilder(eq("workflowName", workflowName)).build(), WorkflowMetadataDoc.class);
			if(result!=null && result.getDocs().size()>0) {
				WorkflowMetadataDoc workflowMetadataDoc = result.getDocs().get(0);
				db.remove(workflowMetadataDoc);
			}
			else {
				throw new AppException(HttpStatus.SC_NOT_FOUND, String.format("Workflow: %s doesn't exist", workflowName), "Document deletion failed");
			}
		}  catch (Exception e) {
			log.error(String.format("Workflow: %s doesn't exist", workflowName), e);
			e.printStackTrace();
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unexpected error", "Workflow doesn't exist", e);
		}


	}

	@Override
	public List<WorkflowMetadata> getAllWorkflowForTenant(String prefix) {
		Database db = getDatabase();
		QueryResult<WorkflowMetadataDoc> result = db.query(new QueryBuilder(Expression.regex("workflowName", prefix!=null?prefix:"")).build(), WorkflowMetadataDoc.class);
		List<WorkflowMetadata> workflowMetadataList = result.getDocs().stream().map(i -> i.getWorkflowMetadata()).collect(Collectors.toList());
		return workflowMetadataList;
	}

}
