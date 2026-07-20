/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.workflow.provider.ibm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@Getter
@Setter
public class IBMCouchDBConfig {
	@Value("${ibm.db.url}")
	private String dbUrl;
	
	@Value("${ibm.db.user:#{null}}")
	private String dbUser;
	
	@Value("${ibm.db.password:#{null}}")
	private String dbPassword;

	@Value("${ibm.env.prefix:local-dev}")
	private String dbNamePrefix;
	
}
