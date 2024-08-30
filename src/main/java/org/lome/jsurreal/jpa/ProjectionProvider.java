package org.lome.jsurreal.jpa;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Primitives;
import jakarta.persistence.Entity;
import net.sf.cglib.core.ReflectUtils;
import org.lome.jsurreal.jpa.factory.Projection;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class ProjectionProvider {

    private final static LoadingCache<Class<?>, Class<Projection<?>>> cache =
            CacheBuilder.newBuilder().build(new CacheLoader<>() {
                @Override
                public Class<Projection<?>> load(Class<?> key) throws Exception {
                    return null;
                }
            });

    private static Class<Projection<?>> project(Class<?> clazz){
        try {
            return cache.get(clazz);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Object project(T item){
        if (item == null) return null;
        if (isPrimitive(item.getClass())) return item;
        if (isCollection(item.getClass())) return projectCollection((Collection)item);
        return ((Projection<T>)ReflectUtils.newInstance(project(item.getClass())))
                .fill(item);
    }

    private static boolean isCollection(Class<?> item) {
        return item != null && Collection.class.isAssignableFrom(item);
    }

    private static <T> Collection<Object> projectCollection(Collection<T> items) {
        Collection<Object> targetCollection = (Collection<Object>) ReflectUtils.newInstance(items.getClass());
        items.stream()
                .map(ProjectionProvider::project)
                .forEach(targetCollection::add);
        return targetCollection;
    }

    public static <T> Object projectBack(T item){
        if (item == null) return null;
        if (item instanceof Projection<?>) return ((Projection<?>)item).original();
        return item;
    }

    protected static boolean isEntity(Class<?> item){
        return item != null && item.getAnnotation(Entity.class) != null;
    }

    protected static boolean isEntity(Object item){
        return item != null && isEntity(item.getClass());
    }

    protected static boolean isPrimitive(Class<?> item){
        return Primitives.isWrapperType(item) || item.isPrimitive() || item.isAssignableFrom(String.class);
    }

}
