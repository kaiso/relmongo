package io.github.kaiso.relmongo.data.repository;

import io.github.kaiso.relmongo.data.model.Person;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PersonRepository extends MongoRepository<Person, String>{

    
    Optional<Person> findById(String id);
}
