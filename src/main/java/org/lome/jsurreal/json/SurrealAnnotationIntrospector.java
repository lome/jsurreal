package org.lome.jsurreal.json;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import net.sf.cglib.core.ReflectUtils;
import org.lome.jsurreal.annotation.EntityReference;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.util.*;

public class SurrealAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        return findFromId(a)
                .or(() -> findFromColumn(a))
                .orElse(super.findNameForSerialization(a));
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        return findFromId(a)
                .or(() -> findFromColumn(a))
                .orElse(super.findNameForDeserialization(a));
    }

    @Override
    public boolean isAnnotationBundle(Annotation ann) {
        return super.isAnnotationBundle(ann);
    }

    @Override
    public Object findSerializer(Annotated a) {
        return findFromId(a)
                .map((p) -> idSerializer(a))
                .orElse(customSerializer(a));
    }

    @Override
    public Object findKeySerializer(Annotated a) {
        return entityReferenceKeySerializer(super.findKeySerializer(a));
    }

    @Override
    public Object findContentSerializer(Annotated a) {
        if (a instanceof AnnotatedField){
            AnnotatedField annotatedField = (AnnotatedField) a;
            Field field = annotatedField.getAnnotated();
            AnnotatedType annotatedType = field.getAnnotatedType();
            if (annotatedType instanceof AnnotatedParameterizedType){
                AnnotatedParameterizedType annotatedParameterizedType = (AnnotatedParameterizedType)annotatedType;
                if (Arrays.asList(annotatedParameterizedType.getAnnotatedActualTypeArguments())
                        .stream()
                        .filter(aa -> aa.isAnnotationPresent(EntityReference.class))
                        .count() > 0){
                    return entityReferenceSerializer();
                } else {
                    return super.findContentSerializer(a);
                }
            }
        }
        return super.findContentSerializer(a);
    }

    private Object customSerializer(Annotated a) {
        if (isEntityReference(a)){
            return entityReferenceSerializer();
        }
        return super.findContentSerializer(a);
    }

    private Object entityReferenceKeySerializer(Object a) {
        return new JsonSerializer<Object>() {
            @Override
            public void serialize(Object object, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                if (object != null && object.getClass().isAnnotationPresent(Entity.class)) {
                    Object idValue = JsonUtil.extractId(object);
                    if (idValue != null) {
                        jsonGenerator.writeFieldName(String.format("%s:%s", JsonUtil.getTableName(object.getClass()), idValue));
                    } else {
                        jsonGenerator.writeNull();
                    }
                } else {
                    jsonGenerator.writeFieldName(object.toString());
                }
            }
        };
    }

    private Object entityReferenceSerializer() {
        return new JsonSerializer<Object>() {
            @Override
            public void serialize(Object object, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                if (object != null && object.getClass().isAnnotationPresent(Entity.class)) {
                    Object idValue = JsonUtil.extractId(object);
                    if (idValue != null) {
                        jsonGenerator.writeObject(String.format("%s:%s", JsonUtil.getTableName(object.getClass()), idValue));
                    } else {
                        jsonGenerator.writeNull();
                    }
                } else {
                    serializerProvider.findValueSerializer(object.getClass()).serialize(object, jsonGenerator, serializerProvider);
                }
            }
        };
    }

    private boolean isEntityReference(Annotated a) {
        return a.hasAnnotation(EntityReference.class);
    }

    private Object idSerializer(final Annotated a){
        final Class<?> entityClass = ((AnnotatedMember) a).getDeclaringClass();
        return new JsonSerializer<Object>() {
            @Override
            public void serialize(Object object, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                if (object != null) {
                    jsonGenerator.writeObject(String.format("%s:%s", JsonUtil.getTableName(entityClass), object));
                } else {
                    jsonGenerator.writeNull();
                }
            }
        };
    }

    private Optional<PropertyName> findFromColumn(Annotated a){
        return Optional.ofNullable(a.getAnnotation(Column.class))
                .map(c -> c.name())
                .filter(Objects::nonNull)
                .map(name -> PropertyName.construct(name));
    }

    private Optional<PropertyName> findFromId(Annotated a){
        return Optional.ofNullable(a.getAnnotation(Id.class))
                .map(name -> PropertyName.construct("id"));
    }

    @Override
    public Object findDeserializer(Annotated a) {
        return null;
    }

}
