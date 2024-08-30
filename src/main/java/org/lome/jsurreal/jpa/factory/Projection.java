package org.lome.jsurreal.jpa.factory;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import net.sf.cglib.core.ReflectUtils;
import org.lome.jsurreal.jpa.ProjectionProvider;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Projection<T> {

    final Map<String,Object> unmappedProperties;
    final Class<T> originalClass;

    protected Projection(Class<T> originalClass){
        this.originalClass = originalClass;
        this.unmappedProperties = new HashMap<>();
    }

    public Optional<Object> get(String name){
        return Optional.ofNullable(unmappedProperties.get(name));
    }

    @JsonAnySetter
    public void set(String name, Object value){
        unmappedProperties.put(name, value);
    }

    protected Map<String, Object> unmappedProperties(){
        return this.unmappedProperties;
    }

    public T original() {
        T mapped = (T) ReflectUtils.newInstance(originalClass);
        Map<String,PropertyDescriptor> mappedMap = beanProps(originalClass);
        Arrays.asList(getClass().getDeclaredFields()).forEach(field -> {
            PropertyDescriptor descriptor = mappedMap.get(field.getName());
            if (descriptor == null) return;
            try {
                field.setAccessible(true);
                Object value = field.get(this);
                if (value != null){
                    descriptor.getWriteMethod().invoke(mapped,
                            projectBack(value));
                }
            } catch (Exception e) {
                // ignore
                e.printStackTrace();
            }
        });
        return mapped;
    }

    private Object projectBack(Object value) {
        if (value instanceof Projection<?>){
            return ((Projection<?>)value).original();
        } else {
            return value;
        }
    }

    private Object project(Object value) {
        return ProjectionProvider.project(value);
    }

    private Map<String,PropertyDescriptor> beanProps(Class<?> beanClass){
        Map<String,PropertyDescriptor> map = new HashMap<>();
        Arrays.asList(ReflectUtils.getBeanProperties(beanClass))
                .forEach(propertyDescriptor -> map.put(propertyDescriptor.getName(), propertyDescriptor));
        return map;
    }

    public Projection<T> fill(T element) {
        Map<String,PropertyDescriptor> mappedMap = beanProps(originalClass);
        mappedMap.forEach((k,v) -> {
            try {
                Object value = v.getReadMethod().invoke(element);
                if (value != null) {
                    Field field = getClass().getDeclaredField(k);
                    if (field == null) throw new RuntimeException("Missing proxy field: "+k);
                    field.setAccessible(true);
                    field.set(this, project(value));
                }
            } catch (Exception e) {
                // ignore
                e.printStackTrace();
            }
        });
        return this;
    }




}
