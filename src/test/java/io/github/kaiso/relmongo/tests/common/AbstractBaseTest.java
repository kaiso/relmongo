package io.github.kaiso.relmongo.tests.common;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.github.kaiso.relmongo.config.EnableRelationalMongo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@EnableRelationalMongo
@EnableMongoRepositories(basePackages = "io.github.kaiso.relmongo.data")
@ExtendWith(SpringExtension.class)
public abstract class AbstractBaseTest {

    /**
     * please store Starter or RuntimeConfig in a static final field
     * if you want to use artifact store caching (or else disable caching)
     */
    private static final MongodStarter starter = MongodStarter.getDefaultInstance();

    private static MongodExecutable _mongodExe;
    private static MongodProcess _mongod;

    private static MongoClient _mongo;

    @BeforeAll
    protected static void setUp() throws Exception {

        _mongodExe = starter.prepare(new MongodConfigBuilder().version(Version.Main.PRODUCTION)
                .net(new Net("localhost", 55777, Network.localhostIsIPv6())).build());
        _mongod = _mongodExe.start();

        _mongo = new MongoClient("localhost", 55777);
    }

    @AfterAll
    protected static void tearDown() throws Exception {

        _mongod.stop();
        _mongodExe.stop();
    }

    @Bean
    public Mongo mongo() {
        return _mongo;
    }

    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongo(), "test");
    }

}
