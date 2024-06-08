package org.lome.jsurreal.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lome.jsurreal.util.JsonMapperProvider;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public class SurrealResponse {

    final static ObjectMapper objectMapper = JsonMapperProvider.getObjectMapper();

    private String id;
    private Object result;
    private SurrealError error;

    public SurrealResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getResult() {
        return result;
    }

    public <T> T getResultAs(Class<T> convertClass) {
        T parsed = objectMapper.convertValue(result, convertClass);
        return parsed;
    }

    public <T> List<T> getResultAsList(Class<T> itemClass) {
        JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, itemClass);
        List<T> parsed = objectMapper.convertValue(result, javaType);
        return parsed;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public SurrealError getError() {
        return error;
    }

    public void setError(SurrealError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "SurrealResponse{" +
                "id='" + id + '\'' +
                ", result=" + result +
                ", error=" + error +
                '}';
    }
}
