package org.lome.jsurreal.jpa.factory;

import jakarta.persistence.Entity;

public class EntityProjection<T> extends Projection<T>{

    protected EntityProjection(Class<T> entityClass){
        super(entityClass);
        assert entityClass.getAnnotation(Entity.class) != null;
    }

    public Object id(){
        throw new RuntimeException(new IllegalAccessException());
    }

}
