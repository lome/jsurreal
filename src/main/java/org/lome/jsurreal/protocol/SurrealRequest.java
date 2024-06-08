package org.lome.jsurreal.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SurrealRequest {

    private final String id;
    private final String method;
    private final Object[] params;

    public SurrealRequest(String id, String method, Object[] params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getParams() {
        return params.length == 0 ? null : params;
    }
}
