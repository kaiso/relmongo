package io.github.kaiso.relmongo.tests.common;

import io.github.kaiso.relmongo.config.MongoSpringConfiguration;
import io.github.kaiso.relmongo.config.TestContextConfiguration;
import io.github.kaiso.relmongo.data.model.Address;
import io.github.kaiso.relmongo.data.model.Car;
import io.github.kaiso.relmongo.data.model.DrivingLicense;
import io.github.kaiso.relmongo.data.model.House;
import io.github.kaiso.relmongo.data.model.Passport;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.model.PersonDetails;
import io.github.kaiso.relmongo.data.model.State;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = { TestContextConfiguration.class, MongoSpringConfiguration.class })
@ExtendWith(SpringExtension.class)
public abstract class AbstractBaseTest {

    @Autowired
    protected MongoOperations mongoOperations;

    @AfterEach
    public void afterEach() {
        mongoOperations.remove(DrivingLicense.class).all();
        mongoOperations.remove(Passport.class).all();
        mongoOperations.remove(Person.class).all();
        mongoOperations.remove(PersonDetails.class).all();
        mongoOperations.remove(House.class).all();
        mongoOperations.remove(Car.class).all();
        mongoOperations.remove(State.class).all();
        mongoOperations.remove(Address.class).all();
    }

}
