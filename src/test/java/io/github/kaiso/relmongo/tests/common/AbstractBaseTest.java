package io.github.kaiso.relmongo.tests.common;

import io.github.kaiso.relmongo.config.TestContextConfiguration;
import io.github.kaiso.relmongo.data.model.Address;
import io.github.kaiso.relmongo.data.model.Car;
import io.github.kaiso.relmongo.data.model.DrivingLicense;
import io.github.kaiso.relmongo.data.model.House;
import io.github.kaiso.relmongo.data.model.Passport;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.model.PersonDetails;
import io.github.kaiso.relmongo.data.model.State;

import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@ContextConfiguration(classes = { TestContextConfiguration.class })
@ExtendWith(SpringExtension.class)
public abstract class AbstractBaseTest {

    private static Logger logger = LoggerFactory.getLogger(AbstractBaseTest.class);

    @Autowired
    protected MongoOperations mongoOperations;

    static {
        ConfigurationSource configurationSource;
        try {
            configurationSource = new ConfigurationSource(
                new FileInputStream(new File(System.getProperty("user.dir") + "/src/test/resources/log4j2-test.yaml")));
            Configurator.initialize(ConfigurationBuilderFactory.newConfigurationBuilder().setConfigurationSource(configurationSource).build(true));
            logger.info("Logger initialized !");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void afterEach() {
//         mongoOperations.dropCollection(DrivingLicense.class);
//         mongoOperations.dropCollection(Passport.class);
//         mongoOperations.dropCollection(Person.class);
//         mongoOperations.dropCollection(House.class);
//         mongoOperations.dropCollection(Car.class);
//         mongoOperations.dropCollection(State.class);
//         mongoOperations.dropCollection(Address.class);
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
