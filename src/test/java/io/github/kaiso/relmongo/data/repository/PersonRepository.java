package io.github.kaiso.relmongo.data.repository;

import io.github.kaiso.relmongo.data.model.Person;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface PersonRepository extends MongoRepository<Person, String>, PersonRepositoryCustom {

	Optional<Person> findById(String id);

	@Query(value = "{ 'passport._id': ?0 }")
	Optional<Person> findByPassport(String id); 
}
