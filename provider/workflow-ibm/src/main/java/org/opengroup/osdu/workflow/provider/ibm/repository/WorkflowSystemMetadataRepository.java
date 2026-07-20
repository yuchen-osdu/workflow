package org.opengroup.osdu.workflow.provider.ibm.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.ibm.config.IBMCouchDB;
import org.opengroup.osdu.workflow.provider.ibm.model.WorkflowMetadataDoc;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowSystemMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.query.Expression;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.org.lightcouch.DocumentConflictException;

import static com.cloudant.client.api.query.Expression.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

@Component
@Slf4j
public class WorkflowSystemMetadataRepository implements IWorkflowSystemMetadataRepository {
	/**
	 * Returns workflow metadata based on workflowName
	 *
	 * @param workflowName Name of the workflow for which metadata should be retrieved.
	 * @return Workflow metadata
	 */

	@Autowired
	IBMCouchDB ibmCouchDB;

	final private String COLLECTION_NAME="WorkflowSystemMetadata";


	@Override
	public WorkflowMetadata getSystemWorkflow(String workflowName) {
		//throw new AppException(HttpStatus.SC_NOT_FOUND, "workflow not found", String.format("Workflow: %s doesn't exist", workflowName));
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
			throw new AppException(HttpStatus.SC_NOT_FOUND, "workflow not found", String.format("Workflow: %s doesn't exist", workflowName));
		} else {
			log.info("System workflow found :"+workflowName);
			return result.getDocs().get(0).getWorkflowMetadata();
		}

	}

	/**
	 * Creates workflow metadata record in persistence store.
	 *
	 * @param workflowMetadata Workflow metadata object to save in persistence store.
	 * @return Workflow metadata
	 */
	@Override
	public WorkflowMetadata createSystemWorkflow(WorkflowMetadata workflowMetadata) {
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
			log.error(String.format("System Workflow %s creation failed",workflowMetadata.getWorkflowName()), e);
			e.printStackTrace();
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed", "Workflow creation failed", e);
		}
		if (response.getStatusCode() == 201)
			return workflowMetadata;
		else
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed..", "Workflow creation failed");

	}

	/**
	 * Deletes workflow metadata based on workflowName
	 *
	 * @param workflowName Name of the workflow for which metadata should be deleted.
	 */
	@Override
	public void deleteSystemWorkflow(String workflowName) {
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

	/**
	 * Get all system workflows metadata based on prefix
	 *
	 * @param prefix Name of the system workflow for which metadata should be deleted.
	 */


	@Override
	public List<WorkflowMetadata> getAllSystemWorkflow(String prefix) {
		Database db = getDatabase();
		QueryResult<WorkflowMetadataDoc> result = db.query(new QueryBuilder(Expression.regex("workflowName",prefix!=null?prefix:"")).build(), WorkflowMetadataDoc.class);
		List<WorkflowMetadata> workflowMetadataList = result.getDocs().stream().map(i -> i.getWorkflowMetadata()).collect(Collectors.toList());
		return workflowMetadataList;
	}

	private Database getDatabase() {
		return ibmCouchDB.getDatabase("Shared", COLLECTION_NAME);
	}
}
