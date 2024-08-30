package org.lome.jsurreal.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import net.sf.cglib.core.ReflectUtils;
import org.lome.jsurreal.annotation.EntityReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

public class SurrealSerializer extends JsonSerializer<Object> {

    private final static Logger logger = LoggerFactory.getLogger(SurrealSerializer.class);
    private final JsonSerializer<Object> defaultSerializer;

    public SurrealSerializer(JsonSerializer<Object> defaultSerializer) {
        this.defaultSerializer = checkNotNull(defaultSerializer);
    }

    @Override
    public void serialize(Object target, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // Do we really need custom serialization ?
        if (target == null || JsonUtil.isPrimitive(target)) {
            defaultSerializer.serialize(target, gen, provider);
        } else {
            if (JsonUtil.isEntity(target)){
                serializeEntity(target, gen, provider);
            } else {
                defaultSerializer.serialize(target, gen, provider);
            }
        }
    }

    private void serializeEntity(Object target, JsonGenerator gen, SerializerProvider provider) throws IOException {
        System.out.println(target.getClass().getAnnotation(EntityReference.class));
        gen.writeStartObject();
        AtomicReference<Object> id = new AtomicReference<>();
        Map<String,Object> fieldValues = new HashMap<>();
        Arrays.asList(ReflectUtils.getBeanProperties(target.getClass())).forEach(propertyDescriptor -> {
            try {
                Field field = target.getClass().getDeclaredField(propertyDescriptor.getName());
                if (JsonUtil.isTransient(field)) return;

                Object value = propertyDescriptor.getReadMethod().invoke(target);
                if (value == null){
                    logger.trace("Ignoring property {}, null",propertyDescriptor.getName());
                    return;
                }

                if (JsonUtil.isId(field)){
                    if (id.get() != null){
                        throw new RuntimeException("Double id!");
                    }
                    logger.trace("Found id value for {}",propertyDescriptor.getName());
                    id.set(value);
                } else {
                    fieldValues.put(JsonUtil.getFieldName(field), value);
                }

            } catch (NoSuchFieldException e) {
                logger.debug("Ignoring property {}, field not defined",propertyDescriptor.getName());
            } catch (InvocationTargetException e) {
                logger.debug("Ignoring property {}",propertyDescriptor.getName(),e);
            } catch (IllegalAccessException e) {
                logger.debug("Ignoring property {}",propertyDescriptor.getName(),e);
            }
        });
        if (id.get() != null){
            gen.writeObjectField("id", String.format("%s:%s", JsonUtil.getTableName(target.getClass()),id.get().toString()));
        }
        fieldValues.forEach((name,value) -> {
            try {
                gen.writeObjectField(name, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        gen.writeEndObject();
    }




    public static class SurrealSerializerModifier extends BeanSerializerModifier{
        @Override
        public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer serializer) {
            return new SurrealSerializer(serializer);
        }
    }
}
