/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/


package org.opengroup.osdu.workflow.provider.ibm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = "org.opengroup.osdu")
@PropertySource("classpath:swagger.properties")
public class WorkflowIBMApplication {

  public static void main(String[] args) {
    SpringApplication.run(WorkflowIBMApplication.class, args);
  }

}
