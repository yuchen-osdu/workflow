// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.azure.workflow.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.opengroup.osdu.workflow.consts.TestConstants;
import org.opengroup.osdu.workflow.util.PayloadBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_ACTIVE;
import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_CONCURRENT_TASK_RUN;
import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_CONCURRENT_WORKFLOW_RUN;
import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_DESCRIPTION;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;


public class AzurePayLoadBuilder {

  private static final String DAG_CONTENT = "test-dag-content";

  public static String buildInvalidWorkflowIdPayload(String workflowId){
    Map<String, Object> payload = new HashMap<>();

    payload.put("Workflow", workflowId);

    return new Gson().toJson(payload);
  }

  public static String buildStartWorkflow(Map<String, Object> context, String type){
    Map<String, Object> payload = new HashMap<>();

    payload.put("WorkflowType", type);
    payload.put("DataType", "opaqueo");
    payload.put("Context", context);

    return new Gson().toJson(payload);
  }

  public static String getValidWorkflowPayload(){
    return PayloadBuilder.buildStartWorkflow(buildContext(), TestConstants.WORKFLOW_TYPE_INGEST);
  }

  public static String getInValidWorkflowPayload(){
    return AzurePayLoadBuilder.buildStartWorkflow(buildContext(), TestConstants.WORKFLOW_TYPE_INGEST);
  }

  public static String buildCreateWorkflowValidPayloadWithDagContent() {
    Map<String, String> registrationInstructions = new HashMap<String, String>();
    registrationInstructions.put("dagContent", DAG_CONTENT);
    Map<String, Object> payload = new HashMap<>();
    payload.put("workflowName", CREATE_WORKFLOW_WORKFLOW_NAME);
    payload.put("registrationInstructions", registrationInstructions);
    payload.put("description", "Test workflow record for integration tests.");
    return new Gson().toJson(payload);
  }

  public static String buildCreateWorkflowValidPayloadWithGivenDescription(String description) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("workflowName", CREATE_WORKFLOW_WORKFLOW_NAME);
    payload.put("registrationInstructions", new HashMap<String, String>());
    payload.put("description", description);
    return new Gson().toJson(payload);
  }

  public static String buildCreateWorkflowValidPayloadWithGivenWorkflowName(String workflowName) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("workflowName", workflowName);
    payload.put("registrationInstructions", new HashMap<String, String>());
    payload.put("description", "Test workflow record for integration tests.");

    return new Gson().toJson(payload);
  }

  public static Map<String, Object> buildCreateWorkflowRequestWithRegistrationInstructions(String workflowName) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("workflowName", workflowName);

    Map<String, Object> registrationInstructions = new HashMap<>();
    registrationInstructions.put("dagName", workflowName);
    registrationInstructions.put("concurrentWorkflowRun", WORKFLOW_CONCURRENT_WORKFLOW_RUN);
    registrationInstructions.put("concurrentTaskRun", WORKFLOW_CONCURRENT_TASK_RUN);
    registrationInstructions.put("active", WORKFLOW_ACTIVE);

    payload.put("registrationInstructions", registrationInstructions);
    payload.put("description",WORKFLOW_DESCRIPTION);

    return payload;
  }

  public static Map<String, Object> buildContext() {
    Map<String, Object> context = new HashMap<>();

    Map<String, Object> conf = new HashMap<>();
   conf.put("test","test") ;
   context.put("conf", conf);

    return context;
  }
  public static JsonObject getWellLogCtxObj()
  {
    JsonObject main= new JsonObject();
    JsonObject item= new JsonObject();
    item.addProperty("data-partition-id","opendes");
    JsonObject legalTagitem= new JsonObject();
    JsonArray legal = new JsonArray();
    legal.add("opendes-dps-integration-test-valid2-legal-tag");
    JsonArray otherRelevantDataCountries = new JsonArray();
    otherRelevantDataCountries.add("US");
    legalTagitem.add("legaltags",legal);
    legalTagitem.add("otherRelevantDataCountries",otherRelevantDataCountries);
    legalTagitem.addProperty("status","complaint");

    //acl
    JsonObject acl= new JsonObject();
    JsonArray viewers = new JsonArray();
    viewers.add("data.default.viewer@opendes.contoso.com");
    JsonArray owners = new JsonArray();
    owners.add("data.default.owner@opendes.contoso.com");
    acl.add("viewers",viewers);
    acl.add("owners",owners);
    item.add("legal-tags",legalTagitem);
    item.add("acl",acl);
    JsonObject workproduct= new JsonObject();
    workproduct.addProperty("ResourceTypeID","srn:type:work-product/WellLog:");
    workproduct.addProperty("ResourceSecurityClassification","srn:reference-data/ResourceSecurityClassification:RESTRICTED:");
    JsonObject groupTypeProperties= new JsonObject();
    JsonArray Components = new JsonArray();
    JsonArray Artefacts1 = new JsonArray();
    JsonObject GroupTypeProperties = new JsonObject();
    GroupTypeProperties.add("Components",Components);
    groupTypeProperties.add("GroupTypeProperties", GroupTypeProperties);
    JsonObject data= new JsonObject();
    JsonObject individualTypeProperties= new JsonObject();
    individualTypeProperties.addProperty("Name","AKM-11 LOG");
    individualTypeProperties.addProperty("Description","Well Log");
    groupTypeProperties.add("IndividualTypeProperties",individualTypeProperties );
    JsonObject extensionProperties= new JsonObject();
    groupTypeProperties.add("ExtensionProperties",extensionProperties);
    JsonArray componentsAssociativeIDs = new JsonArray();
    componentsAssociativeIDs.add("wpc-1");
    groupTypeProperties.add("ComponentsAssociativeIDs", componentsAssociativeIDs);


    JsonArray workproductcomponents= new JsonArray();
    JsonObject resourceTypeID= new JsonObject();
    resourceTypeID.addProperty("ResourceTypeID", "srn:type:work-product-component/WellLog:");
    resourceTypeID.addProperty("ResourceSecurityClassification", "srn:reference-data/ResourceSecurityClassification:RESTRICTED:");
    workproductcomponents.add(resourceTypeID);
    JsonObject dataa= new JsonObject();
    JsonObject groupTypePropertiess= new JsonObject();
    JsonArray files = new JsonArray();
    JsonArray Artefacts = new JsonArray();
    JsonObject GroupTypeProperties1 = new JsonObject();
    GroupTypeProperties1.add("Files",files);
    GroupTypeProperties1.add("Artefacts",Artefacts);
    groupTypePropertiess.add("GroupTypeProperties", GroupTypeProperties1);
    JsonObject individualTypePropertie= new JsonObject();
    individualTypePropertie.addProperty("Name","AKM-11 LOG");
    individualTypePropertie.addProperty("Description","Well Log");
    individualTypePropertie.addProperty("WellboreID","srn:master-data/Wellbore:1013:");
    groupTypePropertiess.add("IndividualTypeProperties", individualTypePropertie);
    JsonObject topMeasuredDepth= new JsonObject();
    topMeasuredDepth.addProperty("Depth", 2182.0004);
    topMeasuredDepth.addProperty("UnitOfMeasure", "srn:reference-data/UnitOfMeasure:M:");
    groupTypePropertiess.add("TopMeasuredDepth", topMeasuredDepth);
    JsonObject bottomMeasuredDepth= new JsonObject();
    bottomMeasuredDepth.addProperty("Depth",  2481.0);
    bottomMeasuredDepth.addProperty("UnitOfMeasure", "srn:reference-data/UnitOfMeasure:M:");
    groupTypePropertiess.add("BottomMeasuredDepth", bottomMeasuredDepth);
    JsonArray curves = new JsonArray();
    JsonObject curvesObj= new JsonObject();
    curvesObj.addProperty("Mnemonic","DEPT");
    curvesObj.addProperty("TopDepth",2182.0);
    curvesObj.addProperty("BaseDepth",2481.0);
    curvesObj.addProperty("DepthUnit","srn:reference-data/UnitOfMeasure:M:");
    curvesObj.addProperty("CurveUnit","srn:reference-data/UnitOfMeasure:M:");
    JsonObject curvesObj2= new JsonObject();
    curvesObj2.addProperty("Mnemonic","GR");
    curvesObj2.addProperty("TopDepth",2182.0);
    curvesObj2.addProperty("BaseDepth",2481.0);
    curvesObj2.addProperty("DepthUnit","srn:reference-data/UnitOfMeasure:M:");
    curvesObj2.addProperty("CurveUnit","srn:reference-data/UnitOfMeasure:GAPI:");
    JsonObject curvesObj3= new JsonObject();
    curvesObj3.addProperty("Mnemonic","DT");
    curvesObj3.addProperty("TopDepth",2182.0);
    curvesObj3.addProperty("BaseDepth",2481.0);
    curvesObj3.addProperty("DepthUnit","srn:reference-data/UnitOfMeasure:M:");
    curvesObj3.addProperty("CurveUnit","srn:reference-data/UnitOfMeasure:US/F:");
    JsonObject curvesObj4= new JsonObject();
    curvesObj4.addProperty("Mnemonic","RHOB");
    curvesObj4.addProperty("TopDepth",2182.0);
    curvesObj4.addProperty("BaseDepth",2481.0);
    curvesObj4.addProperty("DepthUnit","srn:reference-data/UnitOfMeasure:M:");
    curvesObj4.addProperty("CurveUnit","srn:reference-data/UnitOfMeasure:G/C3:");
    JsonObject curvesObj5= new JsonObject();
    curvesObj5.addProperty("Mnemonic","DRHO");
    curvesObj5.addProperty("TopDepth",2182.0);
    curvesObj5.addProperty("BaseDepth",2481.0);
    curvesObj5.addProperty("DepthUnit","srn:reference-data/UnitOfMeasure:M:");
    curvesObj5.addProperty("CurveUnit","srn:reference-data/UnitOfMeasure:G/C3:");
    JsonObject curvesObj6= new JsonObject();
    curvesObj6.addProperty("Mnemonic","NPHI");
    curvesObj6.addProperty("TopDepth",2182.0);
    curvesObj6.addProperty("BaseDepth",2481.0);
    curvesObj6.addProperty("DepthUnit","srn:reference-data/UnitOfMeasure:M:");
    curvesObj6.addProperty("CurveUnit","srn:reference-data/UnitOfMeasure:V/V:");
    curves.add(curvesObj);
    curves.add(curvesObj2);
    curves.add(curvesObj3);
    curves.add(curvesObj4);
    curves.add(curvesObj5);
    curves.add(curvesObj6);
    groupTypePropertiess.add("Curves", curves);
    JsonObject emptyObj= new JsonObject();
    groupTypePropertiess.add("ExtensionProperties",emptyObj);
    resourceTypeID.addProperty("AssociativeID", "wpc-1");
    JsonArray fileAssociativeIDs = new JsonArray();
    fileAssociativeIDs.add("f-1");
    groupTypePropertiess.add("FileAssociativeIDs", fileAssociativeIDs);

    JsonArray filesData= new JsonArray();

    JsonObject resourceType= new JsonObject();
    resourceType.addProperty("ResourceTypeID", "srn:type:file/las2:");
    resourceType.addProperty("ResourceSecurityClassification", "srn:reference-data/ResourceSecurityClassification:RESTRICTED:");
    resourceType.addProperty("AssociativeID", "f-1");
    JsonObject gTypeProperties= new JsonObject();
    gTypeProperties.addProperty("FileSource", "");
    gTypeProperties.addProperty("PreLoadFilePath", "https://azglobalosdulake.blob.core.windows.net/data/well-logs/1013_akm11_1978_comp.las");

    JsonObject DData= new JsonObject();
    JsonObject Grp= new JsonObject();
    Grp.add("GroupTypeProperties", gTypeProperties);
    Grp.add("IndividualTypeProperties",emptyObj);
    Grp.add("ExtensionProperties",emptyObj);
    DData.add("Data", Grp);
    filesData.add(resourceType);
    filesData.add(DData);
    data.add("Files", filesData);
    dataa.add("Data",groupTypePropertiess);
    workproductcomponents.add(dataa);
    workproduct.add("Data", groupTypeProperties);
    data.add("Workproduct", workproduct);
    data.add("WorkProductComponents", workproductcomponents);
    item.add("data", data);
    main.add("Context", item);
    return item;

  }
}
