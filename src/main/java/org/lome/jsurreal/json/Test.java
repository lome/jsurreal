package org.lome.jsurreal.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.lome.jsurreal.jpa.SampleEntity;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;

public class Test {

    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new SimpleModule()
                        .setSerializerModifier(new SurrealSerializer.SurrealSerializerModifier())
                        .setDeserializerModifier(new SurrealDeserializer.SurrealDeserializerModifier()));
        mapper.registerModule(new Jdk8Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // StdDateFormat is ISO8601 since jackson 2.9
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        mapper.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));


        SampleEntity mario = new SampleEntity();
        mario.setFuck("Mario");
        mario.setId("mario");

        SampleEntity maria = new SampleEntity();
        maria.setFuck("Maria");
        maria.setId("maria");

        mario.setWife(maria);

        SampleEntity marietto = new SampleEntity();
        marietto.setFuck("Marietto");
        marietto.setId("marietto");

        SampleEntity mariuccia = new SampleEntity();
        mariuccia.setFuck("Mariuccia");
        mariuccia.setId("mariuccia");

        mario.setChildrens(Arrays.asList(marietto, mariuccia));

        System.out.println(mapper.writeValueAsString(mario));

    }



}
