package io.github.kaiso.relmongo.tests;

import com.mongodb.client.FindIterable;

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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class CascadingTest extends AbstractBaseTest {

    @Autowired
    private PersonRepository repository;

    @Autowired
    private DrivingLicenseRepository drivingLicenseRepository;


    @Test
    public void shouldCascadeSaveOneToOneObject() {
        State state = new State();
        ObjectId drivingLicenseId = ObjectId.get();
        state.setName("Paris");
        DrivingLicense drivingLicense = new DrivingLicense();
        drivingLicense.setNumber("12345");
        drivingLicense.setState(state);
        drivingLicense.setId(drivingLicenseId);
        drivingLicenseRepository.save(drivingLicense);

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setDrivingLicense(drivingLicense);
        repository.save(person);

        FindIterable<Document> drivingLicenses = mongoOperations.getCollection("drivingLicenses").find();
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<Document> drvDoc = new AtomicReference<Document>(null);
        Consumer<Document> filter = new Consumer<Document>() {

            @Override
            public void accept(Document t) {
                count.incrementAndGet();
                drvDoc.set(t);
            }
        };
        drivingLicenses.forEach(filter);
        assertTrue(count.get() == 1);
        assertEquals(drvDoc.get().get("_id"), drivingLicenseId);
        Document document = mongoOperations.getCollection("states").find().iterator().next();
        assertNotNull(document);
        assertEquals("Paris", document.get("name"));
    }

    @Test
    public void shouldCascadeSaveOneToManyObjects() {
        Car car1 = new Car(1);
        car1.setColor(Color.BLUE);
        car1.setManufacturer("BMW");

        Car car2 = new Car(2);
        car2.setColor(Color.BLUE);
        car2.setManufacturer("BMW");

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] { car1, car2 }));
        repository.save(person);

        Document document = mongoOperations.getCollection("people").find().iterator().next();
        assertNull(document.get("cars"));
        Document obj = new Document();
        obj.put("_id", car1.getId());
        obj.put(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME, "car");
        assertTrue(((Collection<?>) document.get("carsrefs")).size() == 2);
        assertTrue(((Collection<?>) document.get("carsrefs")).contains(obj));

        FindIterable<Document> cars = mongoOperations.getCollection("car").find();
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<List<Document>> carsDocuments = new AtomicReference<>(new ArrayList<>());
        Consumer<Document> filter = new Consumer<Document>() {

            @Override
            public void accept(Document t) {
                count.incrementAndGet();
                carsDocuments.get().add(t);
            }
        };
        cars.forEach(filter);
        assertTrue(count.get() == 2);
        assertTrue(carsDocuments.get().stream().map((item) -> {
            return item.get("_id");
        }).collect(Collectors.toList()).contains(car1.getId()));

    }

    @Test
    public void shouldCascadeRemoveCollection() {
        Car car1 = new Car(1);
        car1.setColor(Color.BLUE);
        car1.setManufacturer("BMW");

        Car car2 = new Car(2);
        car2.setColor(Color.BLUE);
        car2.setManufacturer("BMW");

        Person person1 = new Person();
        person1.setName("Kais");
        person1.setEmail("kais.omri.int@gmail.com");
        person1.setCars(Arrays.asList(new Car[] { car1 }));
        repository.save(person1);

        Person person2 = new Person();
        person2.setName("Dave");
        person2.setEmail("dave@mail.com");
        person2.setCars(Arrays.asList(new Car[] { car2 }));
        repository.save(person2);

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
        assertTrue(carsDocuments.get().size() == 1);

        assertTrue(carsDocuments.get().get(0).get("_id").equals(car1.getId()));

    }

}
