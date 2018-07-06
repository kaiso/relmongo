package io.github.kaiso.relmongo.data.repository;

import java.util.List;

import io.github.kaiso.relmongo.data.model.Person;

public interface PersonRepositoryCustom {

     List<Person> findAll();
    
}