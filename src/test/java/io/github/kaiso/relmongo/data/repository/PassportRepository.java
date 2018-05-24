package io.github.kaiso.relmongo.data.repository;

import io.github.kaiso.relmongo.data.model.Passport;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PassportRepository extends MongoRepository<Passport, String> {

    Optional<Passport> findById(String id);
}
