package org.lome.jsurreal.protocol.command;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class BaseRequest {

    Map<String,Object> additionalVariables;

    public BaseRequest(){
        this.additionalVariables = new HashMap<>();
    }

    public Optional<Object> get(String name){
        return Optional.ofNullable(additionalVariables.get(name));
    }

    @JsonAnySetter
    public void set(String name, Object value){
        additionalVariables.put(name, value);
    }

    protected Map<String, Object> additionalVariables(){
        return this.additionalVariables;
    }

    public Object[] toRequestParameters() {
        return new Object[]{this};
    }

}
