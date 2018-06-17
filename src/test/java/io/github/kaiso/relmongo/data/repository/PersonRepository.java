package io.github.kaiso.relmongo.data.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import io.github.kaiso.relmongo.data.model.Person;

public interface PersonRepository extends MongoRepository<Person, String>, PersonRepositoryCustom {

	Optional<Person> findById(String id);

}
