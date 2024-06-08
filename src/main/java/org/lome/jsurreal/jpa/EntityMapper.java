package org.lome.jsurreal.jpa;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.sf.cglib.core.ReflectUtils;
import org.atteo.evo.inflector.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.*;

import jakarta.persistence.Id;

public class EntityMapper<T> {

    final static Logger logger = LoggerFactory.getLogger(EntityMapper.class);

    final Class<T> entityClass;
    final Class<?> proxyClass;
    final String tableName;
    private PropertyDescriptor idField;


    public EntityMapper(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.proxyClass = proxyClass();
        this.tableName = tableName();
        logger.debug("Table for {} is {}", entityClass, this.tableName);
        if (entityClass.getAnnotation(Entity.class) == null) throw new RuntimeException("Missing @Entity annotation");
    }

    private Class<?> proxyClass() {

        DynamicType.Builder<Object> proxyBuilder = new ByteBuddy()
                .subclass(Object.class)
                .annotateType(AnnotationDescription.Builder.ofType(JsonInclude.class)
                        .define("value", JsonInclude.Include.NON_NULL)
                        .build())
                .annotateType(AnnotationDescription.Builder.ofType(JsonIgnoreProperties.class)
                        .define("ignoreUnknown", false)
                        .build());

        for (PropertyDescriptor propertyDescriptor : ReflectUtils.getBeanProperties(entityClass)) {
            try {
                Field field = entityClass.getDeclaredField(propertyDescriptor.getName());

                Id idAnnotation = field.getAnnotation(Id.class);
                if (idAnnotation != null) {
                    if (!field.getType().equals(String.class)){
                        throw new RuntimeException("@Id must a String field");
                    }
                    idField = propertyDescriptor;

                    proxyBuilder = proxyBuilder.defineField(field.getName(), field.getType(), Member.PUBLIC)
                            .annotateField(AnnotationDescription.Builder.ofType(JsonProperty.class)
                            .define("value", "id")
                            .build());
                    continue;
                }

                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null) {
                    String column = columnAnnotation.name();
                    if (StringUtil.isNullOrEmpty(column)) {
                        column = field.getName();
                    } else {
                        proxyBuilder = proxyBuilder.defineField(field.getName(), field.getType(), Member.PUBLIC)
                                .annotateField(AnnotationDescription.Builder.ofType(JsonProperty.class)
                                .define("value", column)
                                .build());
                    }
                    continue;
                }

                Transient transientAnnotation = field.getAnnotation(Transient.class);
                if (transientAnnotation != null) {
                    proxyBuilder = proxyBuilder.defineField(field.getName(), field.getType(), Member.PUBLIC)
                            .annotateField(AnnotationDescription.Builder.ofType(JsonIgnore.class)
                            .build());
                    continue;
                }

                proxyBuilder = proxyBuilder.defineField(field.getName(), field.getType(), Member.PUBLIC)
                        .annotateField(AnnotationDescription.Builder.ofType(JsonProperty.class)
                                .define("value", field.getName())
                                .build());

            } catch (NoSuchFieldException e) {
                logger.error("Error loading declared field: {}", propertyDescriptor.getName());
            }
        }
        return proxyBuilder.make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }

    protected T fromProxy(Object proxy) {
        T mapped = (T) ReflectUtils.newInstance(entityClass);
        Map<String,PropertyDescriptor> mappedMap = beanProps(entityClass);
        Arrays.asList(proxyClass.getDeclaredFields()).forEach(field -> {
            PropertyDescriptor descriptor = mappedMap.get(field.getName());
            try {
                field.setAccessible(true);
                Object value = field.get(proxy);
                if (value != null){
                    descriptor.getWriteMethod().invoke(mapped,value);
                }
            } catch (Exception e) {
                logger.error("Error remapping field {}", field.getName(), e);
            }
        });
        return mapped;
    }

    private Map<String,PropertyDescriptor> beanProps(Class<?> beanClass){
        Map<String,PropertyDescriptor> map = new HashMap<>();
        Arrays.asList(ReflectUtils.getBeanProperties(beanClass))
                .forEach(propertyDescriptor -> map.put(propertyDescriptor.getName(), propertyDescriptor));
        return map;
    }

    protected Object toProxy(T element) {
        Object proxied = (T) ReflectUtils.newInstance(proxyClass);
        Map<String,PropertyDescriptor> mappedMap = beanProps(entityClass);
        mappedMap.forEach((k,v) -> {
            try {
                Object value = v.getReadMethod().invoke(element);
                if (value != null) {
                    Field field = proxyClass.getDeclaredField(k);
                    field.setAccessible(true);
                    field.set(proxied, value);
                }
            } catch (Exception e) {
                logger.error("Error remapping field {}", k, e);
            }
        });
        return proxied;
    }

    protected String getEntityId(T element){
        try {
            return (String)idField.getReadMethod().invoke(element);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private String tableName() {
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation == null) throw new RuntimeException("Not an entity, please annotate "+entityClass.getName()+" with @Entity");
        String name = entityAnnotation.name();
        if (StringUtil.isNullOrEmpty(name)) return fromClassName(entityClass);
        return name;
    }

    private String fromClassName(Class<T> entityClass) {
        String plural = English.plural(entityClass.getSimpleName());
        return normalizeName(plural);
    }

    private String normalizeName(String name){
        return name.trim().replaceAll("([A-Z]+)","_$1")
                .replaceAll("^_+","")
                .replaceAll("_+$","").toLowerCase(Locale.ROOT);
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

}
