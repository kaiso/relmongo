package io.github.kaiso.relmongo.tests;

import io.github.kaiso.relmongo.data.model.Address;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.model.PersonDetails;
import io.github.kaiso.relmongo.data.model.State;
import io.github.kaiso.relmongo.data.repository.PersonRepository;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class OrphanRemovalTest extends AbstractBaseTest {

    @Autowired
    private PersonRepository repository;

    @Test
    public void shouldRemoveOrphanOnOneToMany() {
        State state1 = new State();
        state1.setName("Paris");
        State state2 = new State();
        state2.setName("El Goussa");
        Address address1 = new Address();
        Address address2 = new Address();
        address1.setLocation("1st street");
        address1.setState(state1);
        address2.setLocation("2st street");
        address2.setState(state2);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setAddresses(new LinkedList<Address>(Arrays.asList(address1, address2)));
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson.get().getAddresses().size(), 2);
        person.getAddresses().remove(address1);
        repository.save(person);
        retreivedPerson = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson.get().getAddresses().size(), 1);
        List<Document> list = new ArrayList<>();
        Consumer<Document> f = new Consumer<Document>() {
            @Override
            public void accept(Document t) {
                list.add(t);
            }
        };
        mongoOperations.getCollection("addresses").find().forEach(f);
        assertEquals(list.size(), 1);
        assertEquals(list.get(0).get("location"), address2.getLocation());
        Iterator<Document> stateIterator = mongoOperations.getCollection("states").find().iterator();
        assertEquals("El Goussa", stateIterator.next().get("name"));
        assertFalse(stateIterator.hasNext());
        
    }

    @Test
    public void shouldRemoveAllOrphanOnOneToMany() {
        Address address1 = new Address();
        Address address2 = new Address();
        address1.setLocation("1st street");
        address2.setLocation("2st street");
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setAddresses(new LinkedList<Address>(Arrays.asList(address1, address2)));
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson.get().getAddresses().size(), 2);
        person.setAddresses(null);
        repository.save(person);
        retreivedPerson = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson.get().getAddresses(), null);
        List<Document> list = new ArrayList<>();
        Consumer<Document> f = new Consumer<Document>() {
            @Override
            public void accept(Document t) {
                list.add(t);
            }
        };
        mongoOperations.getCollection("addresses").find().forEach(f);
        assertEquals(list.size(), 0);
    }

    @Test
    public void shouldRemoveOrphanOnOneToOne() {
        PersonDetails dtails = new PersonDetails();
        dtails.setBank("bank");
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setPersonDetails(dtails);
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson.get().getPersonDetails().getBank(), dtails.getBank());
        person.setPersonDetails(null);
        repository.save(person);
        retreivedPerson = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson.get().getPersonDetails(), null);
        assertEquals(false, mongoOperations.getCollection("detail").find().iterator().hasNext());
    }

}
