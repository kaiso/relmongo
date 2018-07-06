package io.github.kaiso.relmongo.data.repository.impl;

import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.repository.PersonRepositoryCustom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;

import java.util.List;

public class PersonRepositoryImpl implements PersonRepositoryCustom {

    private MongoTemplate mongoTemplate;

    @Autowired
    public PersonRepositoryImpl(MongoTemplate mongoTemplate) {
        super();
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Person> findAll() {
        LookupOperation lookup = LookupOperation.newLookup().from("cars").localField("carsrefs._id").foreignField("_id").as("cars");

        LookupOperation lookupHouses = LookupOperation.newLookup().from("houses").localField("housesrefs._id").foreignField("_id").as("houses");

        AggregationResults<Person> result = mongoTemplate.aggregate(Aggregation.newAggregation(lookup, lookupHouses), "people", Person.class);

        return result.getMappedResults();
    }

}
