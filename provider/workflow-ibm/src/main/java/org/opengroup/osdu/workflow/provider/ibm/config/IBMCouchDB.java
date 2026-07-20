/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.workflow.provider.ibm.config;

import java.net.MalformedURLException;

import jakarta.annotation.PostConstruct;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.ibm.auth.ServiceCredentials;
import org.opengroup.osdu.core.ibm.cloudant.IBMCloudantClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloudant.client.api.Database;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IBMCouchDB {

	@Autowired
	IBMCouchDBConfig ibmCouchDBConfig;

	IBMCloudantClientFactory cloudantFactory;

	VmCache<String, Database> databaseRepoCache = new VmCache<>(5 * 30, 1000);

	@PostConstruct
	public void init() {
		cloudantFactory = new IBMCloudantClientFactory(new ServiceCredentials(ibmCouchDBConfig.getDbUrl(),
				ibmCouchDBConfig.getDbUser(), ibmCouchDBConfig.getDbPassword()));
		log.info("IBM Cloudant factory created ");
	}

	public Database getDatabase(String tenant, String collectionName) {
		String fullyColletionName = ibmCouchDBConfig.getDbNamePrefix() + "-" + tenant + "-" + collectionName;
		if (databaseRepoCache.get(fullyColletionName) != null) {
			log.info(String.format("Cache hit, Database %s configuration found in cache", fullyColletionName));
			return databaseRepoCache.get(fullyColletionName);
		}
		Database db = null;
		try {
			db = cloudantFactory.getDatabase(ibmCouchDBConfig.getDbNamePrefix() + "-" + tenant, collectionName);
			databaseRepoCache.put(fullyColletionName, db);
			log.info(String.format("Cache miss, Database %s configured and added in cache", fullyColletionName));
			return db;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "MalformedURLException", "DB operation failed");
		}
	}
}
