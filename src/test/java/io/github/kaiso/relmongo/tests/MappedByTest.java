package io.github.kaiso.relmongo.tests;

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
import io.github.kaiso.relmongo.lazy.LazyLoadingProxy;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MappedByTest extends AbstractBaseTest {

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



    @Test
    public void shouldFetchManyToOneMappedBy() {
        Car car = new Car(0);
        car.setColor(Color.BLUE);
        String manufacturer = "BMW";
        car.setManufacturer(manufacturer);
        Car savedCar = carRepository.save(car);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] { car }));
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertFalse(retreivedPerson.get().getCars().isEmpty());

        Optional<Car> result = carRepository.findById(savedCar.getId().toString());
        carRepository.save(result.get());
        result = carRepository.findById(savedCar.getId().toString());
        assertTrue(result.isPresent());
        assertEquals(result.get().getOwner().getId(), retreivedPerson.get().getId());
    }
    
    @Test
    public void shouldFetchManyToOneMappedBySecondLevel() {
        Car car = new Car(0);
        car.setColor(Color.BLUE);
        String manufacturer = "BMW";
        car.setManufacturer(manufacturer);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] { car }));
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertFalse(retreivedPerson.get().getCars().isEmpty());
        assertEquals(carRepository.findAll().get(0).getId(), retreivedPerson.get().getCars().get(0).getId());
        assertEquals(retreivedPerson.get().getCars().get(0).getOwner().getId(), retreivedPerson.get().getId());
    }
    
    
    @Test
    public void shouldFetchLazyManyToOneMappedBy() {
        House house = new House("H3");
        house.setAddress("Paris");
        house = houseRepository.save(house);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setHouses(Arrays.asList(new House[] { house }));
        person = repository.save(person);
        

        Optional<House> result = houseRepository.findById(house.getId().toString());
        assertTrue(result.isPresent());
        assertEquals(result.get().getOwner().getId(), person.getId());
        assertTrue(result.get().getOwner() instanceof LazyLoadingProxy);
    }


    @Test
    public void shouldfetchOneToOneMappedBy() {
        Passport passport = new Passport();
        passport.setNumber("12345");
        passport = passportRepository.save(passport);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setPassport(passport);
        person = repository.save(person);
        Optional<Passport> retreivedPassport = passportRepository.findById(passport.getId().toString());
        // to test save retrieved one
        passportRepository.save(retreivedPassport.get());
        retreivedPassport = passportRepository.findById(passport.getId().toString());
        assertEquals(person.getId(), retreivedPassport.get().getOwner().getId());
    }
    
    @Test
    public void shouldfetchOneToOneMappedBySecondLevel() {
        Passport passport = new Passport();
        passport.setNumber("12345");
        passport = passportRepository.save(passport);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setPassport(passport);
        person = repository.save(person);
        Optional<Person> retreivedPassport = repository.findById(person.getId().toString());
        assertEquals(person.getId(), retreivedPassport.get().getPassport().getOwner().getId());
    }

    @Test
    public void shouldfetchLazyOneToOneMappedBy() {
        DrivingLicense drivingLicense = new DrivingLicense("036227777");
        drivingLicense.setNumber("12345");
        drivingLicense = drivingLicenseRepository.save(drivingLicense);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setDrivingLicense(drivingLicense);
        person = repository.save(person);
        Optional<DrivingLicense> result = drivingLicenseRepository.findById(drivingLicense.getId());
        assertTrue(result.get().getOwner() instanceof LazyLoadingProxy);
        assertEquals(result.get().getOwner().getId(), person.getId());
    }

}
