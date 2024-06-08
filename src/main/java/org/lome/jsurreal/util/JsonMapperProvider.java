package org.lome.jsurreal.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class JsonMapperProvider {
    
    private final static ObjectMapper objectMapper = build();

    private static ObjectMapper build() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        return mapper;
    }
    
    public static ObjectMapper getObjectMapper(){
        return objectMapper;
    }


}
