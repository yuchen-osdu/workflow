package org.opengroup.osdu.azure.workflow.framework.util;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class TestResourceProvider {
  private static final String DAGS_FOLDER = "dags";
  private static final String OPERATORS_FOLDER = "operators";

  private static Configuration templateConfiguration;

  static {
    templateConfiguration = new Configuration(Configuration.VERSION_2_3_30);
    templateConfiguration.setDefaultEncoding("UTF-8");

    ClassTemplateLoader classTemplateLoader =
        new ClassTemplateLoader(TestResourceProvider.class, "/");
    templateConfiguration.setTemplateLoader(classTemplateLoader);
  }

  public static String getDAGFileContent(String dagTemplateFileName,
                                         Map<String, Object> templateContext) throws Exception  {
    return getContent(DAGS_FOLDER + "/" + dagTemplateFileName, templateContext);
  }

  public static String getOperatorFileContent(String operatorFileName) throws Exception {
    return getContent(OPERATORS_FOLDER + "/" + operatorFileName, null);
  }

  private static String getContent(String filePath, Map<String, Object> templateContext)
      throws Exception {
    if(templateContext == null) {
      templateContext = new HashMap<>();
    }
    Template template = templateConfiguration.getTemplate(filePath);
    StringWriter outputContentWriter = new StringWriter();
    template.process(templateContext, outputContentWriter);

    return outputContentWriter.toString();
  }
}
