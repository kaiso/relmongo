package io.github.kaiso.relmongo.data.repository.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import io.github.kaiso.relmongo.data.model.Color;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.repository.PersonRepositoryCustom;

public class PersonRepositoryCustomImpl implements PersonRepositoryCustom {

	private MongoTemplate mongoTemplate;

	@Autowired
	public PersonRepositoryCustomImpl(MongoTemplate mongoTemplate) {
		super();
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public List<Person> findAll() {
		LookupOperation lookup = LookupOperation.newLookup().from("cars").localField("carsrefs._id").foreignField("_id")
				.as("cars");

		AggregationResults<Person> result = mongoTemplate.aggregate(
				Aggregation.newAggregation(lookup), "people",
				Person.class);

		return result.getMappedResults();
	}

}
