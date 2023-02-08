package io.github.kaiso.relmongo.tests;

import com.mongodb.client.MongoClient;

import io.github.kaiso.relmongo.config.EnableRelMongo;
import io.github.kaiso.relmongo.config.RelMongoMappingContext;
import io.github.kaiso.relmongo.config.TestContextConfiguration;
import io.github.kaiso.relmongo.config.TestContextConfiguration.RMLocalDateTimeToDateConverter;
import io.github.kaiso.relmongo.config.TestContextConfiguration.RelMongoEntityScanner;
import io.github.kaiso.relmongo.data.model.Address;
import io.github.kaiso.relmongo.data.model.Car;
import io.github.kaiso.relmongo.data.model.Color;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.model.State;
import io.github.kaiso.relmongo.data.repository.AddressRepository;
import io.github.kaiso.relmongo.data.repository.CarRepository;
import io.github.kaiso.relmongo.data.repository.PersonRepository;
import io.github.kaiso.relmongo.tests.MultipleMongoTemplateRefTest.FirstTemplateRefTestConfiguration;
import io.github.kaiso.relmongo.tests.MultipleMongoTemplateRefTest.SecondTemplateRefTestConfiguration;
import io.github.kaiso.relmongo.util.RelMongoConstants;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = { FirstTemplateRefTestConfiguration.class, SecondTemplateRefTestConfiguration.class, TestContextConfiguration.class })
@ExtendWith(SpringExtension.class)
public class MultipleMongoTemplateRefTest {

    @EnableMongoAuditing
    @EnableMongoRepositories(basePackages = { "io.github.kaiso.relmongo.data.repository.impl",
        "io.github.kaiso.relmongo.data.repository" }, excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            AddressRepository.class
        }))
    @ComponentScan(basePackages = { "io.github.kaiso.relmongo.tests" })
    public static final class FirstTemplateRefTestConfiguration {

    }

    @EnableMongoAuditing
    @EnableRelMongo(mongoTemplateRef = "secondMongoTemplate")
    @EnableMongoRepositories(basePackages = { "io.github.kaiso.relmongo.data.repository.impl",
        "io.github.kaiso.relmongo.data.repository" }, includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            AddressRepository.class
        }), mongoTemplateRef = "secondMongoTemplate")
    @ComponentScan(basePackages = { "io.github.kaiso.relmongo.tests" })
    public static final class SecondTemplateRefTestConfiguration {

        @Bean
        public MongoTemplate secondMongoTemplate(MongoClient mongoClient, ApplicationContext applicationContext) throws Exception {

            MongoDbFactory mongoDbFactory = new SimpleMongoClientDbFactory(mongoClient, "secondTest");
            DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
            MongoCustomConversions conversions = new MongoCustomConversions(Collections.singletonList(new RMLocalDateTimeToDateConverter()));

            RelMongoMappingContext context = new RelMongoMappingContext();
            context.setInitialEntitySet(new RelMongoEntityScanner(applicationContext)
                .scan(Collections.singleton("io.github.kaiso.relmongo.data"), Document.class, Persistent.class));
            context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());

            MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);
            converter.setCustomConversions(conversions);
            converter.afterPropertiesSet();

            return new MongoTemplate(mongoDbFactory, converter);
        }
    }

    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private PersonRepository personRepository;

    @Autowired
    protected MongoOperations secondMongoTemplate;
    @Autowired
    protected MongoOperations mongoTemplate;

    @Test
    public void shouldStoreReferenceOnEnabledRelMongo() {
        State state = new State();
        ObjectId stateId = ObjectId.get();
        state.setId(stateId);
        state.setName("El Goussa");
        Address address1 = new Address();
        String location1 = "1st street";
        address1.setLocation(location1);
        address1.setState(state);
        address1.setId(Long.valueOf("1"));

        addressRepository.save(address1);

        org.bson.Document document = secondMongoTemplate.getCollection("addresses").find().iterator().next();

        org.bson.Document obj = new org.bson.Document();
        obj.put("_id", stateId);
        obj.put(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME, "states");
        assertEquals(2, ((org.bson.Document) document.get("state")).size());
        assertTrue(((org.bson.Document) document.get("state")).equals(obj));

    }

    @Test
    public void shouldNotStoreReferenceOnDisabledRelMongo() {
        Car car1 = new Car(1);
        car1.setColor(Color.BLUE);
        car1.setManufacturer("BMW");
        Car car2 = new Car(2);
        car2.setColor(Color.RED);
        car2.setManufacturer("BMW");

        Person person = new Person();
        person.setName("Dave");
        person.setEmail("dave@mail.com");
        person.setCars(Arrays.asList(new Car[] { car1, car2 }));
        personRepository.save(person);

        org.bson.Document document = mongoTemplate.getCollection("people").find().iterator().next();

        assertNotNull(document.get("cars"));
        assertNull(document.get("carsrefs"));
        assertTrue(carRepository.findAll().isEmpty());
        assertFalse(secondMongoTemplate.getCollection("people").find().iterator().hasNext());
    }

}
