package io.github.kaiso.relmongo.data.repository;

import io.github.kaiso.relmongo.data.model.DrivingLicense;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DrivingLicenseRepository extends MongoRepository<DrivingLicense, String> {

    Optional<DrivingLicense> findById(String id);
}
