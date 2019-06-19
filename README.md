<p align="center">
  <img src="https://raw.githubusercontent.com/kaiso/relmongo/master/docs/images/logo.png">
</p>
  
[![][license img]][license]
[![][maven img]][maven]
[![][build img]][build]
[![][coverage img]][coverage]
[![Join the chat at https://gitter.im/relmongo/general](https://badges.gitter.im/relmongo/general.svg)](https://gitter.im/relmongo/general?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
<br>

RelMongo allows to implement two-way relations and associations between MongoDB collections in a simple way <br>
RelMongo is built in top of the [Spring Data MongoDB](https://projects.spring.io/spring-data-mongodb/) framework.
# Features
 RelMongo provides :
 - @EnableRelMongo to enable RelMongo engine
 - @OneToMany annotation to address 1..N relations
 - @OneToOne annotation to address 1..1 relations
 - Two fetching methods ( LAZY and EAGER)
 - Cascading operations
 - Bidirectional mapping using the `mappedBy` attribute
 
To get more details please see the [release notes](https://github.com/kaiso/relmongo/releases).
# Wiki
 [Take a tour in the RelMongo wiki](https://github.com/kaiso/relmongo/wiki)
# Binaries
- Maven
  ```xml
   <dependency>
      <groupId>io.github.kaiso.relmongo</groupId>
      <artifactId>relmongo</artifactId>
      <version>x.y.z</version>
   </dependency>
  ```
# Compatibility Matrix

[See compatibility on wiki](https://github.com/kaiso/relmongo/wiki/Compatibility-Matrix)

# Usage
RelMongo is very simple to use.<br>
given two concepts with "one to *" relation<br><br>

      __________________                         __________________
     |    Person        |                       |    Car           |
     |__________________| 1                  *  |__________________|
     |  name (string)   |---------------------->|   ....           |
     |  cars (list )    |                       |                  |
     |                  |                       |                  |
     |__________________|                       |__________________|

on your Person mongo entity simply add the following annotations from RelMongo :
```java
    @OneToMany(fetch=FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinProperty(name="cars")
    private List<Car> cars;
```
and on your Spring App config class simply add @EnableRelMongo annotation:
```java
    ... Other Annotations
    @EnableRelMongo
    public Class AppConfig
    
```
test your code :
```java
        Car car = new Car();
        car.setColor(Color.BLUE);
        String manufacturer = "BMW";
        car.setManufacturer(manufacturer);
        Person person = new Person();
        person.setName("person");
        person.setEmail("person@mail.com");
        person.setCars(Arrays.asList(new Car[] {car}));
        repository.save(person);
        Optional<Person> retreivedPerson = repository.findById(person.getId().toString());
        assertFalse(retreivedPerson.get().getCars().isEmpty());
        assertTrue(retreivedPerson.get().getCars().get(0).getColor().equals(Color.BLUE));
        
```

database layout when executing this test :
- cars collection :
```javascript 
{
    _id : ObjectId(5afaff0e2557db3a140d0f85),
    manufacturer : BMW,
    color : BLUE
}
``` 
- persons collection
```javascript 
  {
    _id : ObjectId(5afaff0e2557db3a140d0f86),
    name : person,
    email : person@mail.com,
    cars : [ 
        {
            _id : ObjectId(5afaff0e2557db3a140d0f85)
            _relmongo_target: cars
        }
    ]
}
``` 
# Strengths
- [x] Based on [Spring framework and derivatives](https://spring.io/)
- [x] Simple to use
- [x] Ready to use on existing database with few changes and in many cases with no changes
- [x] Bidirectional mapping
# Notes
- RelMongo may be an alternative for DBREF which allow to use $lookup querries in mongodb while it is not possible with DBREF.
- [MongoDB](https://www.mongodb.com/) is a document oriented database and is not suitable for relations, if you are using relations massively you may have
a design or technical choice problems.
- RelMongo does not garantee integrity in the database since it is not implemented by MongoDB


# LICENSE

   © Copyright 2018 Kais OMRI.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

[license]:LICENSE-2.0.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg
[maven]:http://search.maven.org/#search|gav|1|g:"io.github.kaiso.relmongo"%20AND%20a:"relmongo"
[maven img]:https://maven-badges.herokuapp.com/maven-central/io.github.kaiso.relmongo/relmongo/badge.svg
[build]:https://travis-ci.org/kaiso/relmongo
[build img]:https://travis-ci.org/kaiso/relmongo.svg?branch=master
[coverage]:https://coveralls.io/repos/github/kaiso/relmongo/badge.svg?branch=master
[coverage img]:https://coveralls.io/github/kaiso/relmongo?branch=master

