/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.workflow.provider.ibm.repository;

import static com.cloudant.client.api.query.Expression.eq;
import static com.cloudant.client.api.query.Expression.gte;
import static com.cloudant.client.api.query.Operation.and;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowRunsPage;
import org.opengroup.osdu.workflow.provider.ibm.config.IBMCouchDB;
import org.opengroup.osdu.workflow.provider.ibm.model.WorkflowRunDoc;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.org.lightcouch.DocumentConflictException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowRunRepository implements IWorkflowRunRepository {


	private static final int WORKFLOW_RUN_LIMIT = 100;

	@Autowired
	IBMCouchDB ibmCouchDB;

	final private String COLLECTION_NAME="WorkflowRun";

	@Inject
	TenantInfo tenantInfo;

	@Override
	public WorkflowRun saveWorkflowRun(WorkflowRun workflowRun) {
		Database db = getDatabase();
		WorkflowRunDoc workflowRunDoc = new WorkflowRunDoc(workflowRun);
		try {
			db.save(workflowRunDoc);
			return workflowRun;
		} catch (DocumentConflictException e) {
			log.error("Conflict", e);
			throw new AppException(e.getStatusCode(), "Conflict", "Workflow exists with workflowId"+workflowRun.getWorkflowId());
		} catch (Exception e) {
			log.error("Save operation failed", e);
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "data save operation failed", "Workflow details failed to persist in database", e);
		}
	}

	private Database getDatabase() {
	return ibmCouchDB.getDatabase(tenantInfo.getDataPartitionId(), COLLECTION_NAME);
}

	@Override
	public WorkflowRun getWorkflowRun(String workflowName, String runId) {
		Database db = getDatabase();
		QueryResult<WorkflowRunDoc> results = db.query(new QueryBuilder(and(eq("workflowName", workflowName), eq("_id", runId))).build(), WorkflowRunDoc.class);
		if(results!=null && results.getDocs().size()>0)
			return results.getDocs().get(0).getWorkflowRun();
		else
			throw new AppException(HttpStatus.SC_NOT_FOUND, "Not Found", String.format("WorkflowRun: %s for Workflow: %s doesn't exist", runId, workflowName));
	}

	@Override
	public WorkflowRunsPage getWorkflowRunsByWorkflowName(String workflowName, Integer limit, String cursor) {
		Database db = getDatabase();
		int numRecords = 0;
		String initialId = validateCursor(cursor, db);

        if (limit != null) {
            numRecords = limit > 0 ? limit : WORKFLOW_RUN_LIMIT;
        }
        //_id = runid
		QueryResult<WorkflowRunDoc> results = db.query(new QueryBuilder(
				and(eq("workflowName", workflowName), gte("_id", initialId)))
				.limit(numRecords+1)
				.sort(Sort.asc("_id"))
				.build(), WorkflowRunDoc.class);
		WorkflowRunsPage page = new WorkflowRunsPage();
		page.setCursor("");
		List<WorkflowRunDoc> workflowRunDocList = results.getDocs();
		List<WorkflowRun> workflowRunList = workflowRunDocList.stream().map(wrd -> wrd.getWorkflowRun()).collect(Collectors.toList());

		if ((results.getDocs().size()) - 1 < numRecords) {
			page.setCursor(null);
			page.setItems(workflowRunList);
		} else {
			page.setCursor(workflowRunDocList.get(workflowRunDocList.size() - 1).get_id());
			workflowRunList.remove(numRecords);
			page.setItems(workflowRunList);
		}
		//page.setCursor(workflowRunDocList.get(workflowRunDocList.size() - 1).get_id());
		//page.setItems(workflowRunList);
		return page;
	}

	private String validateCursor(String cursor, Database db) {
		if (cursor != null && !cursor.isEmpty()) {
			if (db.contains(cursor)) {
				return cursor;
			} else {
				throw new AppException(HttpStatus.SC_BAD_REQUEST, "Cursor invalid",
						"The requested cursor does not exist or is invalid");
			}
		} else {
			return "0";
		}
	}

	@Override
	public void deleteWorkflowRuns(String workflowName, List<String> runIds) {
		Database db = getDatabase();
		QueryResult<WorkflowRunDoc> results = db.query(new QueryBuilder(
				eq("workflowName", workflowName))
				.build(), WorkflowRunDoc.class);
		if(results.getDocs().isEmpty()) {
			throw new AppException(HttpStatus.SC_NOT_FOUND, "NOT_FOUND", String.format("WorkflowRun %s does not exists", workflowName));
		}

		for(WorkflowRunDoc workflowRunDoc:results.getDocs()) {
			db.remove(workflowRunDoc);
		}

	}

	@Override
	public WorkflowRun updateWorkflowRun(WorkflowRun workflowRun) {
		Database db = getDatabase();
		QueryResult<WorkflowRunDoc> results = db.query(new QueryBuilder(
				eq("_id", workflowRun.getRunId())).
				build(), WorkflowRunDoc.class);
		if(results.getDocs().isEmpty()) {
			throw new AppException(HttpStatus.SC_NOT_FOUND, "NOT_FOUND", String.format("Deletion failed!!! WorkflowRun %s does not exists", workflowRun.getWorkflowName()));
		}
		String rev = results.getDocs().get(0).get_rev();
		WorkflowRunDoc workflowRunDoc2 = new WorkflowRunDoc(workflowRun);
		workflowRunDoc2.set_rev(rev);
		try {
			db.update(workflowRunDoc2);
		} catch (DocumentConflictException e) {
			log.error("Updation failed! Document conflicts",e);
			throw new AppException(HttpStatus.SC_CONFLICT, "Document conflicts" , "Could not update workflow :"+workflowRun.getWorkflowName(), e);
		}
		return workflowRun;
	}

	@Override
	public List<WorkflowRun> getAllRunInstancesOfWorkflow(String workflowName, Map<String, Object> params) {
		Database db = getDatabase();
		QueryResult<WorkflowRunDoc> results = db.query(new QueryBuilder(
				eq("workflowName", workflowName)).
				build(), WorkflowRunDoc.class);
		List<WorkflowRun> workflowRunList = results.getDocs().stream().map(wrd -> wrd.getWorkflowRun()).collect(Collectors.toList());
		return workflowRunList;
	}

}
