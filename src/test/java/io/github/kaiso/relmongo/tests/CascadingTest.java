package io.github.kaiso.relmongo.tests;

import com.mongodb.client.FindIterable;

import io.github.kaiso.relmongo.data.model.Address;
import io.github.kaiso.relmongo.data.model.Car;
import io.github.kaiso.relmongo.data.model.Color;
import io.github.kaiso.relmongo.data.model.DrivingLicense;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.model.State;
import io.github.kaiso.relmongo.data.repository.DrivingLicenseRepository;
import io.github.kaiso.relmongo.data.repository.PersonRepository;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;
import io.github.kaiso.relmongo.util.RelMongoConstants;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CascadingTest extends AbstractBaseTest {

    @Autowired
    private PersonRepository repository;

    @Autowired
    private DrivingLicenseRepository drivingLicenseRepository;

    @Test
    public void shouldCascadeSaveOneToOneMultiLevel() {
        State state = new State();
        state.setName("Paris");
        DrivingLicense drivingLicense = new DrivingLicense();
        drivingLicense.setNumber("12345");
        drivingLicense.setState(state);

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setDrivingLicense(drivingLicense);
        repository.save(person);

        Iterator<Document> iterator = mongoOperations.getCollection("drivingLicenses").find().iterator();

        assertEquals(iterator.next().get("_id").toString(), drivingLicense.getId());
        assertFalse(iterator.hasNext());
        Document document = mongoOperations.getCollection("states").find().iterator().next();
        assertNotNull(document);
        assertEquals("Paris", document.get("name"));
    }

    @Test
    public void shouldCascadeSaveOneToManyMultiLevel() {

        State state = new State();
        ObjectId stateId = ObjectId.get();
        state.setId(stateId);
        state.setName("El Goussa");
        Address address1 = new Address();
        Address address2 = new Address();
        String location1 = "1st street";
        address1.setLocation(location1);
        address1.setState(state);
        String location2 = "2st street";
        address2.setLocation(location2);
        address2.setState(state);

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setAddresses(Arrays.asList(new Address[] { address1, address2 }));
        repository.save(person);

        Document document = mongoOperations.getCollection("people").find().iterator().next();

        Document obj = new Document();
        obj.put("_id", address1.getId());
        obj.put(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME, "addresses");
        assertEquals(2, ((Collection<?>) document.get("addresses")).size());
        assertTrue(((Collection<?>) document.get("addresses")).contains(obj));

        Iterator<Document> addresses = mongoOperations.getCollection("addresses").find().iterator();

        List<String> addressesList = new ArrayList<>();
        addressesList.add(addresses.next().getString("location"));
        addressesList.add(addresses.next().getString("location"));
        assertFalse(addresses.hasNext());
        assertTrue(addressesList.contains(location1));
        assertTrue(addressesList.contains(location2));

        Iterator<Document> stateIterator = mongoOperations.getCollection("states").find().iterator();
        assertEquals(stateId, stateIterator.next().get("_id"));
        assertFalse(stateIterator.hasNext());
    }

    @Test
    public void shouldCascadeRemoveMultiLevel() {
        Car car1 = new Car(1);
        car1.setColor(Color.BLUE);
        car1.setManufacturer("BMW");

        Car car2 = new Car(2);
        car2.setColor(Color.BLUE);
        car2.setManufacturer("BMW");

        State state1 = new State();
        String drivingLicenseId1 = "ZZ12345";
        state1.setName("El Goussa");
        DrivingLicense drivingLicense1 = new DrivingLicense(drivingLicenseId1);
        drivingLicense1.setNumber("12345");
        drivingLicense1.setState(state1);

        State state2 = new State();
        String drivingLicenseId2 = "YY12345";
        state2.setName("El Goussa");
        DrivingLicense drivingLicense2 = new DrivingLicense(drivingLicenseId2);
        drivingLicense2.setNumber("12345");
        drivingLicense2.setState(state2);

        Person person1 = new Person();
        person1.setName("Kais");
        person1.setEmail("kais.omri.int@gmail.com");
        person1.setCars(Arrays.asList(new Car[] { car1 }));
        person1.setDrivingLicense(drivingLicense1);
        repository.save(person1);

        Person person2 = new Person();
        person2.setName("Dave");
        person2.setEmail("dave@mail.com");
        person2.setCars(Arrays.asList(new Car[] { car2 }));
        person2.setDrivingLicense(drivingLicense2);
        repository.save(person2);

        assertEquals(2, drivingLicenseRepository.findAll().size());

        // 2 states
        Iterator<Document> stateIterator = mongoOperations.getCollection("states").find().iterator();
        assertNotNull(stateIterator.next());
        assertTrue(stateIterator.hasNext());

        repository.delete(person2);

        FindIterable<Document> cars = mongoOperations.getCollection("car").find();
        AtomicReference<List<Document>> carsDocuments = new AtomicReference<>(new ArrayList<>());
        Consumer<Document> filter = new Consumer<Document>() {

            @Override
            public void accept(Document t) {
                carsDocuments.get().add(t);
            }
        };
        cars.forEach(filter);
        assertEquals(1, carsDocuments.get().size());
        assertEquals(carsDocuments.get().get(0).get("_id"), car1.getId());
        assertEquals(1, drivingLicenseRepository.findAll().size());
        // it remains one state
        stateIterator = mongoOperations.getCollection("states").find().iterator();
        assertNotNull(stateIterator.next());
        assertFalse(stateIterator.hasNext());

    }

}
