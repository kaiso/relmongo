package io.github.kaiso.relmongo.tests;

import io.github.kaiso.relmongo.data.model.Car;
import io.github.kaiso.relmongo.data.model.DrivingLicense;
import io.github.kaiso.relmongo.data.model.House;
import io.github.kaiso.relmongo.data.model.Passport;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.model.State;
import io.github.kaiso.relmongo.data.repository.PersonRepository;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionException;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration(classes = { TransactionsTest.class })
public class TransactionsTest extends AbstractBaseTest {

    @Autowired
    private TransactionalBean transactionalBean;

    @Service
    public static class TransactionalBean {

        @Autowired
        private PersonRepository repository;

        @Autowired
        private MongoOperations mongoOperations;

        public void before() {

            mongoOperations.dropCollection(DrivingLicense.class);
            mongoOperations.dropCollection(Passport.class);
            mongoOperations.dropCollection(Person.class);
            mongoOperations.dropCollection(House.class);
            mongoOperations.dropCollection(Car.class);
            mongoOperations.dropCollection(State.class);

            // create collection
            mongoOperations.createCollection(Person.class);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void savePerson(Person person) {
            repository.save(person);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void savePersonAndDropCollection(Person person) {
            repository.save(person);
            repository.count();
            mongoOperations.dropCollection(Person.class);
        }

    }

    @Configuration
    @EnableTransactionManagement
    public static class TransactionConfig {

        @Bean
        MongoTransactionManager transactionManager(MongoDbFactory dbFactory) {
            return new MongoTransactionManager(dbFactory);
        }

    }

    @BeforeEach
    public void beforeEach() {
        transactionalBean.before();
    }

    @Test
    @Disabled("this test works only on replicaset environment")
    public void shouldSaveInTransaction() {

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        transactionalBean.savePerson(person);

    }

    @Test
    @Disabled("this test works only on replicaset environment")
    public void shouldSaveInTransaction_excepttion() {
        Assertions.assertThrows(MongoTransactionException.class, () -> {
            Person person = new Person();
            person.setName("Dave");
            person.setEmail("dave@mail.com");
            transactionalBean.savePersonAndDropCollection(person);
        });

    }

}
