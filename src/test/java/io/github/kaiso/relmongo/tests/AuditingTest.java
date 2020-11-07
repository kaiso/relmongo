package io.github.kaiso.relmongo.tests;

import io.github.kaiso.relmongo.data.model.Address;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.repository.AddressRepository;
import io.github.kaiso.relmongo.data.repository.PersonRepository;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuditingTest extends AbstractBaseTest {

    @Autowired
    private PersonRepository repository;

    @Autowired
    private AddressRepository addressRepository;

    @Test
    public void shouldPopulateAuditingOnCascade() {
        Address address1 = new Address();
        Address address2 = new Address();
        address1.setLocation("1st street");
        address2.setLocation("2st street");
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setAddresses(new LinkedList<Address>(Arrays.asList(address1, address2)));
        repository.save(person);
        Consumer<Document> f = new Consumer<Document>() {
            @Override
            public void accept(Document t) {
                assertNotNull(t.get("lastModifiedDate"));
            }
        };
        mongoOperations.getCollection("addresses").find().forEach(f);
    }
    
    @Test
    public void shouldRemoveByCriteria() {
        Address address1 = new Address();
        Address address2 = new Address();
        address1.setLocation("1st street");
        address2.setLocation("2st street");
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setAddresses(new LinkedList<Address>(Arrays.asList(address1, address2)));
        repository.save(person);
        Consumer<Document> f = new Consumer<Document>() {
            @Override
            public void accept(Document t) {
                assertNotNull(t.get("lastModifiedDate"));
            }
        };
        
        
        mongoOperations.getCollection("addresses").find().forEach(f);
        
        Criteria expression = Criteria.where("creationDate").lt(LocalDateTime.now());
        Query query = new Query().addCriteria(expression);
        mongoOperations.remove(query, Address.class).wasAcknowledged();
    }

    @Test
    public void shouldPopulateAuditingOnSave() {
        Address address1 = new Address(123l);
        address1.setLocation("1st street");
        addressRepository.save(address1);
        Consumer<Document> f = new Consumer<Document>() {
            @Override
            public void accept(Document t) {
                assertNotNull(t.get("lastModifiedDate"));
            }
        };
        mongoOperations.getCollection("addresses").find().forEach(f);
    }
    
    

}
