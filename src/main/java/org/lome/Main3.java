package org.lome;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.lome.jsurreal.annotation.Query;
import org.lome.jsurreal.annotation.Variable;
import org.lome.jsurreal.jpa.SurrealRepository;
import org.lome.jsurreal.jpa.SurrealRepositoryFactory;
import org.lome.jsurreal.protocol.SurrealDBClient;
import org.lome.jsurreal.protocol.command.QueryRequest;
import org.lome.jsurreal.protocol.command.SignIn;
import org.lome.jsurreal.protocol.exception.RequestLimitExceededException;
import org.lome.jsurreal.protocol.exception.SurrealCallException;
import org.lome.jsurreal.util.JsonMapperProvider;

import java.io.IOException;
import java.util.List;


public class Main3 {

    final static ObjectMapper objectMapper = JsonMapperProvider.getObjectMapper();

    public static void main(String[] args) throws RequestLimitExceededException, SurrealCallException {
        try (var client = new SurrealDBClient("localhost", 8000)){
            client.connect();
            client.signIn(new SignIn("root","root"));
            printJson(client.query(new QueryRequest("DEFINE NAMESPACE IF NOT EXISTS example;")));
            printJson(client.query(new QueryRequest("USE NS example; DEFINE DATABASE IF NOT EXISTS example_db;")));
            client.use("example","test");

            PersonSurrealRepository repo = SurrealRepositoryFactory.surrealRepository(client, PersonSurrealRepository.class, Person.class);
            Person alice = repo.save(new Person("alice"));
            printJson(alice);

            printJson(repo.findAlice());
            printJson(repo.findByName("alice"));

            Person alice2 = repo.save(new Person("alice"));

            repo.findAllByName("alice").forEach(found -> {
                System.out.println("FOUND!! ");
                printJson(found);
            });

            repo.deleteAll();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static interface PersonSurrealRepository extends SurrealRepository<Person>{
        @Query("SELECT * FROM persons WHERE name = 'alice'")
        public Person findAlice();

        @Query("SELECT * FROM persons WHERE name = $name")
        public Person findByName(@Variable("name") String name);

        @Query("SELECT * FROM persons WHERE name = $name")
        public List<Person> findAllByName(@Variable("name") String name);
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