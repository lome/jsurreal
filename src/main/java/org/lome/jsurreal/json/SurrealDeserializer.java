package org.lome.jsurreal.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import jakarta.persistence.Entity;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class SurrealDeserializer extends JsonDeserializer<Object> {

    private final JsonDeserializer<Object> defaultDeserializer;

    public SurrealDeserializer(JsonDeserializer<Object> defaultDeserializer) {
        this.defaultDeserializer = checkNotNull(defaultDeserializer);
    }

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {

        return defaultDeserializer.deserialize(jsonParser, deserializationContext);
    }

    public static class SurrealDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer deserializer) {
            return new SurrealDeserializer(deserializer);
        }
    }
}
