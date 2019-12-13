package io.github.kaiso.relmongo.tests;

import io.github.kaiso.relmongo.data.model.House;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.repository.PersonRepository;
import io.github.kaiso.relmongo.lazy.LazyLoadingProxy;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class AggregationTest extends AbstractBaseTest {

    @Autowired
    private PersonRepository repository;

    @Test
    public void shouldFetchThroughAggregation() {
        House house = new House("H1");
        house.setAddress("Paris");

        House house1 = new House("H2");
        house.setAddress("Bir El Hafey");

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");

        person.setHouses(Arrays.asList(new House[] { house, house1 }));
        repository.save(person);

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.lookup("houses", "houses._id", "_id", "houses"));
        AggregationResults<Person> result = mongoOperations.aggregate(aggregation, "people", Person.class);
        Assertions.assertFalse(result.getMappedResults().isEmpty());
        Assertions.assertEquals(result.getMappedResults().get(0).getHouses().size(), 2);
        assertFalse(result.getMappedResults().get(0).getHouses() instanceof LazyLoadingProxy);

    }

}
