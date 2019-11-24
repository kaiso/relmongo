package io.github.kaiso.relmongo.data.repository;

import io.github.kaiso.relmongo.data.model.Address;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AddressRepository extends MongoRepository<Address, String>{

    
    Optional<Address> findById(String id);
}
