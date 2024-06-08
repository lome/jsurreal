package org.lome.jsurreal.jpa;

import io.netty.util.internal.StringUtil;
import jakarta.persistence.Entity;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.sf.cglib.core.CodeGenerationException;
import net.sf.cglib.core.ReflectUtils;
import org.lome.jsurreal.annotation.Query;
import org.lome.jsurreal.annotation.Variable;
import org.lome.jsurreal.protocol.SurrealDBClient;
import org.lome.jsurreal.protocol.command.QueryRequest;
import org.lome.jsurreal.protocol.command.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;

import static net.bytebuddy.implementation.MethodDelegation.to;

public class SurrealRepositoryFactory {

    final static Logger logger = LoggerFactory.getLogger(SurrealRepositoryFactory.class);

    public static <T> SurrealRepository<T> surrealRepository(SurrealDBClient client, Class<T> entityClass){
        try {
            return new ByteBuddy()
                    .subclass(SurrealRepository.class)
                    .method(ElementMatchers.named("getClient")).intercept(FixedValue.value(client))
                    .method(ElementMatchers.named("getEntityMapper")).intercept(FixedValue.value(new EntityMapper<>(entityClass)))
                    .make()
                    .load(client.getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded()
                    .newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T, S extends SurrealRepository<T>> S surrealRepository(SurrealDBClient client,
                                                                                             Class<S> repositoryClass,
                                                                                             Class<T> entityClass){
        try {
            final EntityMapper<T> entityMapper = new EntityMapper<>(entityClass);
            DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<S> builder = new ByteBuddy()
                    .subclass(repositoryClass)
                    .method(ElementMatchers.named("getClient")).intercept(FixedValue.value(client))
                    .method(ElementMatchers.named("getEntityMapper")).intercept(FixedValue.value(entityMapper));

            for (Method method : repositoryClass.getDeclaredMethods()){
                if (method.getAnnotation(Query.class) != null){
                    builder = addQueryMethod(method, builder, client);
                }
            }

            return builder.make()
                    .load(repositoryClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded()
                    .newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static <S> DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<S> addQueryMethod(Method method,
                                                                                                                 DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<S> builder,
                                                                                                                 SurrealDBClient client) {
        ElementMatcher.Junction matcher = ElementMatchers.named(method.getName()).and(
                ElementMatchers.takesArguments(method.getParameterTypes())
        );

        String query = method.getAnnotation(Query.class).value();
        if (StringUtil.isNullOrEmpty(query)){
            logger.warn("Invalid empty @Query on {} : {}",method.getDeclaringClass().getName(),method.getName());
            return builder;
        }

        Map<String,Integer> variablesMap = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int idx = 0; idx < parameters.length; idx++){
            Parameter parameter = parameters[idx];
            Variable variable = parameter.getAnnotation(Variable.class);
            if (variable != null){
                String name = variable.value();
                if (StringUtil.isNullOrEmpty(name)){
                    logger.error("Empty @Variable name at {} : {}",method.getDeclaringClass().getName(),method.getName());
                } else {
                    variablesMap.put(name, idx);
                }
            }
        }

        Class<?> returnType = method.getReturnType();
        Function<QueryResponse, ?> mapper = null;
        if (Collection.class.isAssignableFrom(returnType)){
            Class<?> returnClass = (Class<?>)((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[0];
            Collection newCollection = null;
            try {
                newCollection = (Collection) ReflectUtils.newInstance(returnType);
            }catch(CodeGenerationException e){
                if (List.class.equals(returnType)){
                    newCollection = new ArrayList();
                } else {
                    if (Set.class.equals(returnType)){
                        newCollection = new HashSet();
                    } else {
                        throw new RuntimeException("Unsupported collection: "+returnType);
                    }
                }
            }
            Collection collection = newCollection;

            if (returnClass.getAnnotation(Entity.class) != null){
                EntityMapper<?> entityMapper = new EntityMapper<>(returnClass);
                mapper = queryResponseItems -> {
                    Optional.ofNullable(queryResponseItems.getFirst())
                            .map(queryResponseItem -> queryResponseItem.getResultsAs(entityMapper.proxyClass))
                            .orElse(List.of())
                            .stream()
                            .map(entityMapper::fromProxy)
                                    .forEach(collection::add);
                    return collection;
                };

            } else {
                mapper = queryResponseItems -> {
                    Optional.ofNullable(queryResponseItems.getFirst())
                            .map(queryResponseItem -> queryResponseItem.getResultsAs(returnClass))
                            .orElse(List.of())
                            .stream()
                            .forEach(collection::add);
                    return collection;
                };
            }

        } else {
            if (returnType.getAnnotation(Entity.class) != null){
                EntityMapper<?> entityMapper = new EntityMapper<>(returnType);
                mapper = queryResponseItems -> Optional.ofNullable(queryResponseItems.getFirst())
                        .map(queryResponseItem -> entityMapper.fromProxy(queryResponseItem.getResultAs(entityMapper.proxyClass)))
                        .orElse(null);
            } else {
                mapper = queryResponseItems -> Optional.ofNullable(queryResponseItems.getFirst())
                        .map(queryResponseItem -> queryResponseItem.getResultAs(returnType))
                        .orElse(null);
            }

        }

        return builder.method(matcher)
                .intercept(to(new QueryInterceptor(client, mapper, query, variablesMap)));

    }

    public static class QueryInterceptor {
        final SurrealDBClient client;
        final Function<QueryResponse, ?> mapper;
        final String query;
        final Map<String,Integer> variablesMap;

        public QueryInterceptor(SurrealDBClient client, Function<QueryResponse, ?> mapper, String query, Map<String, Integer> variablesMap) {
            this.client = client;
            this.mapper = mapper;
            this.query = query;
            this.variablesMap = variablesMap;
        }

        @RuntimeType
        public Object delegate(@AllArguments Object... args) throws Exception {
            QueryRequest request = new QueryRequest(query);
            for (Map.Entry<String,Integer> var : variablesMap.entrySet()){
                if ((args.length-1) >= var.getValue()){
                    Object value = args[var.getValue()];
                    if (value != null){
                        request = request.withVariable(var.getKey(), value);
                    }
                }else{
                    logger.warn("Missing variable value {}:{}", var.getValue(),var.getKey());
                }
            }
            return mapper.apply(client.query(request));
        }
    }

}
