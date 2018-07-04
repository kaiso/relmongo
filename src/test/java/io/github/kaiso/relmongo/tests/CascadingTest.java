package io.github.kaiso.relmongo.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;

import com.mongodb.client.FindIterable;

import io.github.kaiso.relmongo.data.model.Car;
import io.github.kaiso.relmongo.data.model.Color;
import io.github.kaiso.relmongo.data.model.DrivingLicense;
import io.github.kaiso.relmongo.data.model.House;
import io.github.kaiso.relmongo.data.model.Passport;
import io.github.kaiso.relmongo.data.model.Person;
import io.github.kaiso.relmongo.data.model.State;
import io.github.kaiso.relmongo.data.repository.DrivingLicenseRepository;
import io.github.kaiso.relmongo.data.repository.PersonRepository;
import io.github.kaiso.relmongo.tests.common.AbstractBaseTest;
import io.github.kaiso.relmongo.util.RelMongoConstants;

@ContextConfiguration(classes = { CascadingTest.class })
public class CascadingTest extends AbstractBaseTest {

	@Autowired
	private PersonRepository repository;

	@Autowired
	private DrivingLicenseRepository drivingLicenseRepository;

	@Autowired
	private MongoOperations mongoOperations;

	@BeforeEach
	public void beforeEach() {
		mongoOperations.dropCollection(DrivingLicense.class);
		mongoOperations.dropCollection(Passport.class);
		mongoOperations.dropCollection(Person.class);
		mongoOperations.dropCollection(House.class);
		mongoOperations.dropCollection(Car.class);
		mongoOperations.dropCollection(State.class);
	}

	@Test
	public void shouldCascadeSaveOneToOneObject() {
		State state = new State();
		ObjectId drivingLicenseId = ObjectId.get();
		state.setName("Paris");
		DrivingLicense drivingLicense = new DrivingLicense();
		drivingLicense.setNumber("12345");
		drivingLicense.setState(state);
		drivingLicense.setId(drivingLicenseId);
		drivingLicenseRepository.save(drivingLicense);

		Person person = new Person();
		person.setName("Dave");
		person.setEmail("dave@mail.com");
		person.setDrivingLicense(drivingLicense);
		repository.save(person);

		FindIterable<Document> drivingLicenses = mongoOperations.getCollection("drivingLicenses").find();
		AtomicInteger count = new AtomicInteger(0);
		AtomicReference<Document> drvDoc = new AtomicReference<Document>(null);
		Consumer<Document> filter = new Consumer<Document>() {

			@Override
			public void accept(Document t) {
				count.incrementAndGet();
				drvDoc.set(t);
			}
		};
		drivingLicenses.forEach(filter);
		assertTrue(count.get() == 1);
		assertEquals(drvDoc.get().get("_id"), drivingLicenseId);
		Document document = mongoOperations.getCollection("states").find().iterator().next();
		assertNotNull(document);
		assertEquals("Paris", document.get("name"));
	}

	@Test
	public void shouldCascadeSaveOneToManyObjects() {
		Car car1 = new Car();
		car1.setColor(Color.BLUE);
		car1.setManufacturer("BMW");

		Car car2 = new Car();
		car2.setColor(Color.BLUE);
		car2.setManufacturer("BMW");

		Person person = new Person();
		person.setName("Dave");
		person.setEmail("dave@mail.com");
		person.setCars(Arrays.asList(new Car[] { car1, car2 }));
		repository.save(person);

		Document document = mongoOperations.getCollection("people").find().iterator().next();
		assertNull(document.get("cars"));
		Document obj = new Document();
		obj.put("_id", car1.getId());
		obj.put(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME, "cars");
		assertTrue(((Collection<?>) document.get("carsrefs")).size() == 2);
		assertTrue(((Collection<?>) document.get("carsrefs")).contains(obj));

		FindIterable<Document> cars = mongoOperations.getCollection("cars").find();
		AtomicInteger count = new AtomicInteger(0);
		AtomicReference<List<Document>> carsDocuments = new AtomicReference<>(new ArrayList<>());
		Consumer<Document> filter = new Consumer<Document>() {

			@Override
			public void accept(Document t) {
				count.incrementAndGet();
				carsDocuments.get().add(t);
			}
		};
		cars.forEach(filter);
		assertTrue(count.get() == 2);
		assertTrue(carsDocuments.get().stream().map((item) -> {
			return item.get("_id");
		}).collect(Collectors.toList()).contains(car1.getId()));

	}

}
