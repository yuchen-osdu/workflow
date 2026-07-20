package org.opengroup.osdu.workflow.consts;

import static java.util.Objects.isNull;

public enum DefaultVariable {
	WORKFLOW_HOST(""),
	DEFAULT_DATA_PARTITION_ID_TENANT1(""),
	DOMAIN(""),

	INTEGRATION_TESTER(""),
	NO_DATA_ACCESS_TESTER(""),
	LEGAL_TAG(""),
	OTHER_RELEVANT_DATA_COUNTRIES(""),

	FINISHED_WORKFLOW_ID(""),

	TEST_DAG_NAME(""),
  DEFAULT_DATA_PARTITION_ID_TENANT("");


	public static String getEnvironmentVariableOrDefaultKey(DefaultVariable key){
		String variable = getEnvironmentVariable(key.name());

		return isNull(variable) ? key.getDefaultValue() : variable;
	}

	private static String getEnvironmentVariable(String variableName){
		return System.getProperty(variableName, System.getenv(variableName));
	}

	private String defaultValue;

	DefaultVariable(String defaultValue){
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getVariableKey(DefaultVariable var) {
		return var.name();
	}
}

