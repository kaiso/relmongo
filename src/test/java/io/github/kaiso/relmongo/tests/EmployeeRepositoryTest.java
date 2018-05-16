package io.github.kaiso.relmongo.tests;

import io.github.kaiso.relmongo.data.model.Car;
import io.github.kaiso.relmongo.data.model.Color;
import io.github.kaiso.relmongo.data.model.House;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.model.PersonNoRelational;
import io.github.kaiso.relmongo.data.repository.CarRepository;
import io.github.kaiso.relmongo.data.repository.EmployeeRepository;
import io.github.kaiso.relmongo.data.repository.HouseRepository;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;

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

@ContextConfiguration(classes = { EmployeeRepositoryTest.class })
public class EmployeeRepositoryTest extends AbstractBaseTest {

    @Autowired
    private EmployeeRepository repository;
    
    @Autowired
    private CarRepository carRepository;
    
    @Autowired
    private HouseRepository houseRepository;
    
    @Autowired 
    private MongoOperations mongoOperations;

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
        person.setCars(Arrays.asList(new Car[] {car}));
        repository.save(person);
        PersonNoRelational personNoRelational = mongoOperations.findById(person.getId(), PersonNoRelational.class);
        assertNull(personNoRelational.getCars().get(0).getColor());
    }
    
    
    @Test
    public void shouldFetchObjectsOnOneToManyRelation() {
        Car car = new Car();
        car.setColor(Color.BLUE);
        String manufacturer = "BMW";
        car.setManufacturer(manufacturer);
        carRepository.save(car);
        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] {car}));
        repository.save(person);
        Optional<Person> retreivedEmployee = repository.findById(person.getId().toString());
        assertFalse(retreivedEmployee.get().getCars().isEmpty());
        assertTrue(retreivedEmployee.get().getCars().get(0).getColor().equals(Color.BLUE));
        assertTrue(retreivedEmployee.get().getCars().get(0).getManufacturer().equals(manufacturer));
    }
    
    
    @Test
    public void shouldLazyLoadCollection() {
        House house = new House();
        house.setAddress("Paris");
        houseRepository.save(house);
        Person employee = new Person();
        employee.setName("Dave");
        employee.setEmail("dave@mail.com");
        employee.setHouses(Arrays.asList(new House[] {house}));
        repository.save(employee);
        
        Optional<Person> retreivedEmployee = repository.findById(employee.getId().toString());
        assertFalse(retreivedEmployee.get().getHouses().isEmpty());
        assertTrue(retreivedEmployee.get().getHouses().get(0).getAddress().equals("Paris"));
    }

}
