package org.lome.jsurreal.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.time.ZoneId;
import java.util.TimeZone;

public class JsonMapperProvider {
    
    private final static ObjectMapper objectMapper = build();

    private static ObjectMapper build() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // StdDateFormat is ISO8601 since jackson 2.9
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        mapper.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
        return mapper;
    }
    
    public static ObjectMapper getObjectMapper(){
        return objectMapper;
    }


}
