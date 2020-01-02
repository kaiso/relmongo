<p align="center">
  <img src="https://raw.githubusercontent.com/kaiso/relmongo/master/docs/assets/images/logo.png">
</p>

***
## Java relationship-enabled domain model persistence framework for MongoDB
***

[![][license img]][license]
[![][maven img]][maven]
[![][build img]][build]
[![][coverage img]][coverage]
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/kaiso/relmongo)](https://github.com/kaiso/relmongo/releases)
[![Join the chat at https://gitter.im/relmongo/general](https://badges.gitter.im/relmongo/general.svg)](https://gitter.im/relmongo/general?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
<br>
The RelMongo Java framework provides relationships mapping of MongoDB domain model objects. The framework provides annotations like OneToMany, OneToOne and ManyToOne as an alternative for DBRef and allows cascade operations and lazy loading. RelMongo uses Spring data mongodb and manual references which make lookup aggregations work properly and overcomes DBRef limitations. [Learn more...](https://kaiso.github.io/relmongo/)


# Documentation
 For documentation, wiki and examples please visit the project [home page](https://kaiso.github.io/relmongo/).
# LICENSE

   © Copyright 2020 Kais OMRI.

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
[coverage img]:https://coveralls.io/repos/github/kaiso/relmongo/badge.svg?branch=master
[coverage]:https://coveralls.io/github/kaiso/relmongo?branch=master

