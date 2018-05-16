package io.github.kaiso.relmongo.data.repository;

import io.github.kaiso.relmongo.data.model.Car;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CarRepository extends MongoRepository<Car, String>{

    
    Optional<Car> findById(String id);
}
