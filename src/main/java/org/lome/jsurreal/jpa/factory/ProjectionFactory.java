package org.lome.jsurreal.jpa.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.sf.cglib.core.ReflectUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

public class ProjectionFactory {

    public static <Y> Class<Projection<Y>> createProjectionType(Y item){
        assert item != null;
        assert item.getClass().getAnnotation(Entity.class) == null;

        Class<Y> itemClass = (Class<Y>) item.getClass();
        DynamicType.Builder<?> projectionBuilder = new ByteBuddy()
                .subclass(TypeDescription.Generic.Builder.parameterizedType(Projection.class, itemClass).build())
                .annotateType(AnnotationDescription.Builder.ofType(JsonInclude.class)
                        .define("value", JsonInclude.Include.NON_NULL)
                        .build())
                .annotateType(AnnotationDescription.Builder.ofType(JsonIgnoreProperties.class)
                        .define("ignoreUnknown", false)
                        .build());

        for (PropertyDescriptor propertyDescriptor : ReflectUtils.getBeanProperties(itemClass)) {
            try {
                Field field = itemClass.getDeclaredField(propertyDescriptor.getName());

                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null) {
                    String column = columnAnnotation.name();
                    if (StringUtil.isNullOrEmpty(column)) {
                        column = field.getName();
                    } else {
                        projectionBuilder = projectionBuilder.defineField(field.getName(), field.getType(), Member.PUBLIC)
                                .annotateField(AnnotationDescription.Builder.ofType(JsonProperty.class)
                                        .define("value", column)
                                        .build());
                    }
                    continue;
                }

                Transient transientAnnotation = field.getAnnotation(Transient.class);
                if (transientAnnotation != null) {
                    projectionBuilder = projectionBuilder.defineField(field.getName(), field.getType(), Member.PUBLIC)
                            .annotateField(AnnotationDescription.Builder.ofType(JsonIgnore.class)
                                    .build());
                    continue;
                }

                projectionBuilder = projectionBuilder.defineField(field.getName(), field.getType(), Member.PUBLIC)
                        .annotateField(AnnotationDescription.Builder.ofType(JsonProperty.class)
                                .define("value", field.getName())
                                .build());

            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Error loading declared field: "+propertyDescriptor.getName());
            }
        }
        return (Class<Projection<Y>>) projectionBuilder.make()
                .load(item.getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }

}
