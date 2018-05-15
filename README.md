# RelMongo
RelMongo (Relational Mongo) allows to use relations between mongodb collections in a JPA way <br>
RelMongo is based on Spring Data Mongo framework.

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
```
    @OneToMany(fetch=FetchType.EAGER)
    @JoinProperty(name="cars")
    private List<Car> cars;
```
and on your Spring App config class simply add @EnableRelationalMongo annotation:
``` ... Other Annotations
    @EnableRelationalMongo
    public Class AppConfig
```
# Strengths
- [x] Based on [Spring framework and derivatives](https://spring.io/)
- [x] Simple to use
- [x] The lazy loading is done in a bulk way so no N+1 problems
# Notes
- RelMongo may be an alternative for DBREF.
- [MongoDB](https://www.mongodb.com/) is a document oriented database and is not suitable for relations, if you are using relations massively you may have
a design or technical choice problems.


# LICENSE

   Copyright 2018 Kais OMRI and authors.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
