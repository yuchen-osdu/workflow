package org.opengroup.osdu.workflow.util;

import java.util.Map;

public abstract class TestBase {

    protected HTTPClient client;
    protected Map<String, String> headers;

    public abstract void setup() throws Exception;
    public abstract void tearDown() throws Exception;
}
