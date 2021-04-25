package io.github.kaiso.relmongo.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableRelMongo(mongoTemplateRef = "mongoTemplate")
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = { "io.github.kaiso.relmongo.data.repository.impl", "io.github.kaiso.relmongo.data.repository" })
@ComponentScan(basePackages = { "io.github.kaiso.relmongo.tests" })
public class MongoSpringConfiguration {

}
