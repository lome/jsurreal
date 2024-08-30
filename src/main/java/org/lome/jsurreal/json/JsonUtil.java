package org.lome.jsurreal.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Primitives;
import io.netty.util.internal.StringUtil;
import jakarta.persistence.*;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.sf.cglib.core.ReflectUtils;
import org.atteo.evo.inflector.English;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Locale;

public class JsonUtil {

    static boolean isCollection(Class<?> item) {
        return item != null && Collection.class.isAssignableFrom(item);
    }

    static boolean isEntity(Class<?> item){
        return item != null && item.getAnnotation(Entity.class) != null;
    }

    static boolean isEntity(Object item){
        return item != null && isEntity(item.getClass());
    }

    static boolean isPrimitive(Class<?> item){
        return Primitives.isWrapperType(item) || item.isPrimitive() || item.isAssignableFrom(String.class);
    }


    public static boolean isPrimitive(Object target) {
        return target == null ? true : isPrimitive(target.getClass());
    }

    public static boolean isId(Field field) {
        return field.getAnnotation(Id.class) != null;
    }

    public static boolean isTransient(Field field) {
        return field.getAnnotation(Transient.class) != null;
    }

    public static String getFieldName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null){
            if (column.name() != null) return column.name();
        }
        return field.getName();
    }

    public static String getTableName(Class<?> item) {
        if (!isEntity(item)) throw new RuntimeException("Not an entity!");
        Entity entity = item.getAnnotation(Entity.class);
        Table table = item.getAnnotation(Table.class);
        if (table != null && !StringUtil.isNullOrEmpty(table.name())){
            return table.name();
        }
        if (StringUtil.isNullOrEmpty(entity.name())){
            return entity.name();
        }
        return fromClassName(item);
    }

    private static String fromClassName(Class<?> entityClass) {
        //String plural = English.plural(entityClass.getSimpleName());
        return normalizeName(entityClass.getSimpleName());
    }

    private static String normalizeName(String name){
        return name.trim().replaceAll("([A-Z]+)","_$1")
                .replaceAll("^_+","")
                .replaceAll("_+$","").toLowerCase(Locale.ROOT);
    }

    public static Object extractId(Object entity){
        if (entity == null) return null;
        Class<?> entityClass = entity.getClass();
        if (entityClass.getAnnotation(Entity.class) == null) throw new RuntimeException("Missing @Entity annotation");
        for (PropertyDescriptor propertyDescriptor : ReflectUtils.getBeanProperties(entityClass)) {
            try {
                Field field = entityClass.getDeclaredField(propertyDescriptor.getName());
                Id idAnnotation = field.getAnnotation(Id.class);
                if (idAnnotation != null) {
                    return propertyDescriptor.getReadMethod().invoke(entity);
                }
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
