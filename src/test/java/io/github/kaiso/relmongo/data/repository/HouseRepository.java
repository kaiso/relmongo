package io.github.kaiso.relmongo.data.repository;

import io.github.kaiso.relmongo.data.model.House;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface HouseRepository extends MongoRepository<House, String>{

    
    Optional<House> findById(String id);
}
