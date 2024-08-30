package org.lome.jsurreal.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.lome.jsurreal.jpa.SampleEntity;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;

public class Test2 {

    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .setDateFormat(new StdDateFormat().withColonInTimeZone(true))
                .setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")))
                .setAnnotationIntrospector(new SurrealAnnotationIntrospector());


        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        mapper.setVisibility(mapper.getDeserializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.ANY));




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
        mario.addCousin(maria);

        String serialized = mapper.writeValueAsString(mario);
        System.out.println(serialized);

        mario = mapper.readValue(serialized, SampleEntity.class);
        System.out.println(mario);

    }



}
