package org.lome;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lome.jsurreal.protocol.command.InfoResponse;
import org.lome.jsurreal.protocol.command.QueryRequest;
import org.lome.jsurreal.protocol.command.SignIn;
import org.lome.jsurreal.protocol.exception.RequestLimitExceededException;
import org.lome.jsurreal.protocol.SurrealDBClient;
import org.lome.jsurreal.protocol.exception.SurrealCallException;

import java.io.IOException;
import java.util.Map;


public class Main {

    final static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws RequestLimitExceededException, SurrealCallException {
        try (var client = new SurrealDBClient("localhost", 8000)){
            client.connect();
            client.signIn(new SignIn("root","root"));
            printJson(client.query(new QueryRequest("DEFINE NAMESPACE IF NOT EXISTS example;")));
            printJson(client.query(new QueryRequest("USE NS example; DEFINE DATABASE IF NOT EXISTS example_db;")));
            client.use("example", "example_db");
            printJson((client.info()));

            Person alice = new Person("Alice");
            alice = client.create("persons", alice).getFirst();

            printJson(client.select(alice.getId(), alice.getClass()));
            printJson(client.selectTable("persons", alice.getClass()));

            printJson(client.merge(alice.getId(), alice.getClass(), Map.of("foo","bar")));
            printJson(client.mergeTable("persons", alice.getClass(), Map.of("bar","baz")));

            alice.setName("Bob");
            printJson(client.update(alice.getId(), alice));
            printJson(client.updateTable("persons", alice));

            printJson(client.delete(alice.getId(), alice.getClass()));
            printJson(client.deleteTable("persons", alice.getClass()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printJson(Object object) {
        try {
            System.out.println(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Person{
        private String id;
        private String name;

        public Person(){}
        public Person(String name){
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}