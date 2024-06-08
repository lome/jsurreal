package org.lome.jsurreal.protocol.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lome.jsurreal.util.JsonMapperProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QueryResponseItem {

    final static ObjectMapper objectMapper = JsonMapperProvider.getObjectMapper();

    public enum Status {
        OK,
        ERR
    }

    private Object result;
    private Status status;
    private String time;

    public QueryResponseItem() {
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public <T> T getResultAs(Class<T> resultClass){
        if (result instanceof Iterable<?>){
            Iterator<?> iterator = ((Iterable)result).iterator();
            if (iterator.hasNext()){
                return objectMapper.convertValue(iterator.next(), resultClass);
            }
        } else {
            return objectMapper.convertValue(result, resultClass);
        }
        return null;
    }

    public <T> List<T> getResultsAs(Class<T> resultClass){
        if (result instanceof Iterable<?>){
            Iterator<?> iterator = ((Iterable)result).iterator();
            List<T> data = new ArrayList<>();
            while(iterator.hasNext()){
                data.add(objectMapper.convertValue(iterator.next(), resultClass));
            }
            return data;
        } else {
            return List.of(objectMapper.convertValue(result, resultClass));
        }
    }
}
