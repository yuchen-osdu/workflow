package org.opengroup.osdu.workflow.provider.azure.consts;

public class CacheConstants {
  // Number of seconds the information for active number of dag runs will be retained in the local cache
  public static final int ACTIVE_DAG_RUNS_LOCAL_CACHE_EXPIRATION_SECONDS = 20;
  // Maximum number of entries the active dag runs local cache will contain
  public static final int ACTIVE_DAG_RUNS_LOCAL_CACHE_MAXIMUM_SIZE = 1000;
  // Key corresponding to which the count of active dag runs is stored in the cache
  public static final String ACTIVE_DAG_RUNS_COUNT_CACHE_KEY = "active-dag-runs-count";
  // Number of seconds the workflow metadata will be retained in the local cache
  public static final int WORKFLOW_METADATA_LOCAL_CACHE_EXPIRATION_SECONDS = 600;
  // Maximum number of entries the workflow metadata local cache will contain
  public static final int WORKFLOW_METADATA_LOCAL_CACHE_MAXIMUM_SIZE = 1000;
}
