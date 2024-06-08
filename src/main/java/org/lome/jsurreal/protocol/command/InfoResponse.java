package org.lome.jsurreal.protocol.command;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class InfoResponse {

    private String id;
    private String name;
    private Map<String,Object> additionalData;

    public InfoResponse(){
        this.additionalData = new HashMap<>();
    }

    @JsonAnySetter
    public void additionalData(String name, Object value){
        this.additionalData.put(name, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> additionalData() {
        return additionalData;
    }

    @Override
    public String toString() {
        return "InfoResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", additionalData=" + additionalData +
                '}';
    }
}
