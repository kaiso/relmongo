package io.github.kaiso.relmongo.tests;

import com.mongodb.DBObject;

import io.github.kaiso.relmongo.data.model.Car;
import io.github.kaiso.relmongo.data.model.Color;
import io.github.kaiso.relmongo.data.model.DrivingLicense;
import io.github.kaiso.relmongo.data.model.House;
import io.github.kaiso.relmongo.data.model.Passport;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.repository.CarRepository;
import io.github.kaiso.relmongo.data.repository.DrivingLicenseRepository;
import io.github.kaiso.relmongo.data.repository.HouseRepository;
import io.github.kaiso.relmongo.data.repository.PassportRepository;
import io.github.kaiso.relmongo.data.repository.PersonRepository;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = { PersonRepositoryTest.class })
public class PersonRepositoryTest extends AbstractBaseTest {

    @Autowired
    private PersonRepository repository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private PassportRepository passportRepository;

    @Autowired
    private DrivingLicenseRepository drivingLicenseRepository;

    @Autowired
    private MongoOperations mongoOperations;

    @BeforeEach
    public void beforeEach() {
        mongoOperations.dropCollection(DrivingLicense.class);
        mongoOperations.dropCollection(Passport.class);
        mongoOperations.dropCollection(Person.class);
        mongoOperations.dropCollection(House.class);
        mongoOperations.dropCollection(Car.class);
    }

    @Test
    public void shouldSaveAndRetreive() {
        Person employee = new Person();
        employee.setName("Dave");
        employee.setEmail("dave@mail.com");
        repository.save(employee);
        assertNotNull(employee.getId());
        Optional<Person> retreivedEmployee = repository.findById(employee.getId().toString());
        assertTrue(retreivedEmployee.isPresent());
        assertEquals(retreivedEmployee.get().getId(), employee.getId());
    }

    @Test
    public void shouldPersistOnlyIdOnOneToManyRelation() {
        Car car = new Car();
        car.setColor(Color.BLUE);
        car.setManufacturer("BMW");
        carRepository.save(car);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] { car }));
        repository.save(person);
        DBObject personNoRelational = mongoOperations.getCollection("people").find().next();
        assertNull(personNoRelational.get("cars"));
    }

    @Test
    public void shouldPersistOnlyReferencedPropertyOnOneToOneRelation() {
        Passport passport = new Passport();
        passport.setNumber("12345");
        passportRepository.save(passport);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setPassport(passport);
        repository.save(person);
        DBObject personNoRelational = mongoOperations.getCollection("people").find().next();
        assertNull(personNoRelational.get("passport"));
        System.out.println("db id" + personNoRelational.get("passportId"));
        System.out.println("relational id" + passport.getId());
        assertEquals(personNoRelational.get("passportId"), passport.getId());
    }

    @Test
    public void shouldfetchOneToOneRelation() {
        Passport passport = new Passport();
        passport.setNumber("12345");
        passportRepository.save(passport);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setPassport(passport);
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertNotNull(retreivedPerson.get().getPassport());
        assertEquals(retreivedPerson.get().getPassport().getNumber(), "12345");
    }

    @Test
    public void shouldFetchOneToManyRelation() {
        Car car = new Car();
        car.setColor(Color.BLUE);
        String manufacturer = "BMW";
        car.setManufacturer(manufacturer);
        carRepository.save(car);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] { car }));
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertFalse(retreivedPerson.get().getCars().isEmpty());
        assertTrue(retreivedPerson.get().getCars().get(0).getColor().equals(Color.BLUE));
        assertTrue(retreivedPerson.get().getCars().get(0).getManufacturer().equals(manufacturer));
    }

    @Test
    public void shouldLazyLoadCollection() {
        House house = new House();
        house.setAddress("Paris");
        houseRepository.save(house);
        Person employee = new Person();
        employee.setName("Dave");
        employee.setEmail("dave@mail.com");
        employee.setHouses(Arrays.asList(new House[] { house }));
        repository.save(employee);
        Optional<Person> retreivedEmployee = repository.findById(employee.getId().toString());
        assertFalse(retreivedEmployee.get().getHouses().isEmpty());
        assertTrue(retreivedEmployee.get().getHouses().get(0).getAddress().equals("Paris"));
    }

    @Test
    public void shouldLazyLoadObject() {
        DrivingLicense drivingLicense = new DrivingLicense();
        drivingLicense.setNumber("12345");
        drivingLicenseRepository.save(drivingLicense);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setDrivingLicense(drivingLicense);
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertNotNull(retreivedPerson.get().getDrivingLicense());
        assertEquals(retreivedPerson.get().getDrivingLicense().getNumber(), "12345");
    }

    @Test
    public void shouldReplaceOneToOneRelation() {
        Passport passport = new Passport();
        passport.setNumber("12345");
        passportRepository.save(passport);
        Passport passport1 = new Passport();
        passport1.setNumber("77777");
        passportRepository.save(passport1);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setPassport(passport);
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson.get().getPassport().getNumber(), "12345");

        person.setPassport(passport1);
        repository.save(person);
        Optional<Person> retreivedPerson1 = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson1.get().getPassport().getNumber(), "77777");
    }

    @Test
    public void shouldReplaceOneToManyRelation() {
        Car car = new Car();
        car.setColor(Color.BLUE);
        String manufacturer = "BMW";
        car.setManufacturer(manufacturer);
        carRepository.save(car);

        Car car1 = new Car();
        car1.setColor(Color.RED);
        String manufacturer1 = "JAGUAR";
        car1.setManufacturer(manufacturer1);
        carRepository.save(car1);

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] { car }));
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertFalse(retreivedPerson.get().getCars().isEmpty());
        assertTrue(retreivedPerson.get().getCars().get(0).getColor().equals(Color.BLUE));
        assertTrue(retreivedPerson.get().getCars().get(0).getManufacturer().equals(manufacturer));

        person.setCars(Arrays.asList(new Car[] { car1 }));
        repository.save(person);
        Optional<Person> retreivedPerson1 = repository.findById(person.getId().toString());
        assertFalse(retreivedPerson1.get().getCars().isEmpty());
        assertTrue(retreivedPerson1.get().getCars().get(0).getColor().equals(Color.RED));
        assertTrue(retreivedPerson1.get().getCars().get(0).getManufacturer().equals(manufacturer1));
    }

    @Test
    public void shouldRemoveOneToOneRelation() {
        Passport passport = new Passport();
        passport.setNumber("12345");
        passportRepository.save(passport);

        DrivingLicense drivingLicense = new DrivingLicense();
        drivingLicense.setNumber("12345");
        drivingLicenseRepository.save(drivingLicense);

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setPassport(passport);
        person.setDrivingLicense(drivingLicense);
        repository.save(person);

        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertEquals(retreivedPerson.get().getPassport().getNumber(), "12345");

        person.setPassport(null);
        person.setDrivingLicense(null);
        repository.save(person);
        Optional<Person> retreivedPerson1 = repository.findById(person.getId().toString());
        assertNull(retreivedPerson1.get().getPassport());
        assertNull(retreivedPerson1.get().getDrivingLicense());
    }

    @Test
    public void shouldRemoveElementFromOneToManyRelation() {
        Car car = new Car();
        car.setColor(Color.BLUE);
        String manufacturer = "BMW";
        car.setManufacturer(manufacturer);
        carRepository.save(car);

        Car car1 = new Car();
        car1.setColor(Color.RED);
        String manufacturer1 = "JAGUAR";
        car1.setManufacturer(manufacturer1);
        carRepository.save(car1);

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] { car, car1 }));
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertTrue(retreivedPerson.get().getCars().size() == 2);
        assertTrue(retreivedPerson.get().getCars().get(0).getColor().equals(Color.BLUE));
        assertTrue(retreivedPerson.get().getCars().get(0).getManufacturer().equals(manufacturer));

        retreivedPerson.get().getCars().remove(0);
        repository.save(retreivedPerson.get());
        Optional<Person> retreivedPerson1 = repository.findById(person.getId().toString());
        assertTrue(retreivedPerson.get().getCars().size() == 1);
        assertTrue(retreivedPerson1.get().getCars().get(0).getColor().equals(Color.RED));
        assertTrue(retreivedPerson1.get().getCars().get(0).getManufacturer().equals(manufacturer1));
    }

}
