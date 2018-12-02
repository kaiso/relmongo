package io.github.kaiso.relmongo.tests.common;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.github.kaiso.relmongo.config.EnableRelMongo;

import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

@EnableRelMongo
@EnableMongoRepositories(basePackages = "io.github.kaiso.relmongo.data")
@ExtendWith(SpringExtension.class)
@ComponentScan(basePackages = { "io.github.kaiso.relmongo.tests" })
public abstract class AbstractBaseTest {
    
    private static Logger logger = LoggerFactory.getLogger(AbstractBaseTest.class);

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

        _mongo = new MongoClient(Collections.singletonList(new ServerAddress("127.0.0.1", 55777)));
        // _mongo = new MongoClient(Arrays.asList(new ServerAddress("127.0.0.1",
        // 30001),new ServerAddress("127.0.0.1", 30002),new ServerAddress("127.0.0.1",
        // 30003)));
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

    public @Bean MongoDbFactory mongoDbFactory() {
        return new SimpleMongoDbFactory(_mongo, "test");
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDbFactory mongoDbFactory) throws Exception {
        return new MongoTemplate(mongoDbFactory);
    }

}
