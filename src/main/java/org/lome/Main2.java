package org.lome;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.lome.jsurreal.jpa.SurrealRepository;
import org.lome.jsurreal.jpa.SurrealRepositoryFactory;
import org.lome.jsurreal.protocol.SurrealDBClient;
import org.lome.jsurreal.protocol.command.QueryRequest;
import org.lome.jsurreal.protocol.command.SignIn;
import org.lome.jsurreal.protocol.exception.RequestLimitExceededException;
import org.lome.jsurreal.protocol.exception.SurrealCallException;
import org.lome.jsurreal.util.JsonMapperProvider;

import java.io.IOException;
import java.util.Map;


public class Main2 {

    final static ObjectMapper objectMapper = JsonMapperProvider.getObjectMapper();

    public static void main(String[] args) throws RequestLimitExceededException, SurrealCallException {
        try (var client = new SurrealDBClient("localhost", 8000)){
            client.connect();
            client.signIn(new SignIn("root","root"));
            printJson(client.query(new QueryRequest("DEFINE NAMESPACE IF NOT EXISTS example;")));
            printJson(client.query(new QueryRequest("USE NS example; DEFINE DATABASE IF NOT EXISTS example_db;")));
            client.use("example","test");

            SurrealRepository<Person> repo = SurrealRepositoryFactory.surrealRepository(client, Person.class);
            Person alice = repo.save(new Person("alice"));
            printJson(alice);
            printJson(repo.count());
            printJson(repo.findAll());
            printJson(repo.findById(alice.getId()));
            alice.setName("bob");
            alice = repo.update(alice);
            printJson(alice);
            repo.deleteAll();
            printJson(repo.findAll());

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

    @Entity
    public static class Person{
        @Id
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