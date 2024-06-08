package org.lome.jsurreal.jpa;

import org.lome.jsurreal.protocol.SurrealDBClient;
import org.lome.jsurreal.protocol.exception.SurrealCallException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface SurrealRepository<T> {

    private EntityMapper<T> getEntityMapper(){
        throw new RuntimeException("Not implemented!");
    }

    private SurrealDBClient getClient(){
        throw new RuntimeException("Not implemented!");
    }

    private Class<T> getEntityClass(){
        return getEntityMapper().getEntityClass();
    }

    private T unwrap(Object proxy){
        return getEntityMapper().fromProxy(proxy);
    }

    private Object wrap(T element){
        return getEntityMapper().toProxy(element);
    }

    private String getTableName(){
        return getEntityMapper().getTableName();
    }

    private String getEntityId(T element){
        return getEntityMapper().getEntityId(element);
    }

    /**************** Repository default Methods ****************/

    default Optional<T> findById(String id) throws SurrealCallException {
        return Optional.ofNullable(getClient().select(id, getEntityClass()))
                .map(this::unwrap);
    }

    default Iterable<T> findAll() throws SurrealCallException {
        return getClient().selectTable(getTableName(), getEntityClass());
    }

    default T save(T element) throws SurrealCallException {
        return getClient().insert(getTableName(), List.of(element)).getFirst();
    }

    default T update(T element) throws SurrealCallException {
        String entityId = getEntityId(element);
        if (entityId == null) throw new RuntimeException("Null entity id");
        return getClient().update(entityId, element);
    }

    default void deleteAll() throws SurrealCallException {
        getClient().deleteTable(getTableName(), getEntityClass());
    }

    default void deleteById(String id) throws SurrealCallException {
        getClient().delete(id, getEntityClass());
    }

    default void delete(T element) throws SurrealCallException {
        String entityId = getEntityId(element);
        if (entityId == null) throw new RuntimeException("Null entity id");
        deleteById(entityId);
    }

    default void deleteAll(Iterable<T> elements) throws SurrealCallException {
        for (T element : elements){
            delete(element);
        }
    }

    default Iterable<T> saveAll(Iterable<T> elements) throws SurrealCallException {
        return StreamSupport.stream(elements.spliterator(), false)
                .map(e -> {
                    try {
                        return save(e);
                    } catch (SurrealCallException ex) {
                        throw new RuntimeException(ex);
                    }
                }).collect(Collectors.toList());
    }


}
