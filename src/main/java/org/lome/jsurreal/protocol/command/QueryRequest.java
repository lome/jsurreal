package org.lome.jsurreal.protocol.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class QueryRequest extends BaseRequest{

    final String query;
    final Map<String,Object> variables;

    public QueryRequest(String query){
        this.query = query;
        this.variables = new HashMap<>();
    }

    public QueryRequest withVariable(String name, Object value){
        this.variables.put(name, value);
        return this;
    }

    public QueryRequest withVariables(Map<String,Object> variables){
        this.variables.putAll(variables);
        return this;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public Object[] toRequestParameters() {
        return new Object[]{this.query, this.variables};
    }
}
