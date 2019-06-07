package io.github.kaiso.relmongo.tests.common;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EnableRelMongo
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "io.github.kaiso.relmongo.data")
@ExtendWith(SpringExtension.class)
@ComponentScan(basePackages = { "io.github.kaiso.relmongo.tests" })
public abstract class AbstractBaseTest extends AbstractMongoClientConfiguration {

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
    @Autowired
    private ApplicationContext applicationContext;
    private static MongoClient _mongo;

    @BeforeAll
    protected static void setUp() throws Exception {

        _mongodExe = starter.prepare(new MongodConfigBuilder().version(Version.Main.PRODUCTION)
                .net(new Net("localhost", 55777, Network.localhostIsIPv6())).build());
        _mongod = _mongodExe.start();

        _mongo = MongoClients.create("mongodb://localhost:55777");
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
    @Override
    public MongoClient mongoClient() {
        return _mongo;
    }

    @Bean
    @Primary
    @Override
    public MongoMappingContext mongoMappingContext() throws ClassNotFoundException {
        MongoMappingContext context = new MongoMappingContext();
        context.setInitialEntitySet(new EntityScanner(applicationContext)
                .scan(Document.class, Persistent.class));
        context.setSimpleTypeHolder(customConversions().getSimpleTypeHolder());
        context.afterPropertiesSet();
        return context;
    }

    @Bean
    @Primary
    @Override
    public CustomConversions customConversions() {
        return new MongoCustomConversions(Collections.singletonList(new RMLocalDateTimeToDateConverter()));
    }
    
    @Bean
    @Override
    public MappingMongoConverter mappingMongoConverter() throws Exception {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext());
        converter.setCustomConversions(customConversions());
        converter.afterPropertiesSet();
        return converter;
    }

    @Override
    public String getDatabaseName() {
        return "test";
    }

    @Override
    public Collection<String> getMappingBasePackages() {
        return Collections.singleton("io.github.kaiso.relmongo.data");
    }

    public class EntityScanner {

        private final ApplicationContext context;

        /**
         * Create a new {@link EntityScanner} instance.
         * 
         * @param context
         *            the source application context
         */
        public EntityScanner(ApplicationContext context) {
            Assert.notNull(context, "Context must not be null");
            this.context = context;
        }

        /**
         * Scan for entities with the specified annotations.
         * 
         * @param annotationTypes
         *            the annotation types used on the entities
         * @return a set of entity classes
         * @throws ClassNotFoundException
         *             if an entity class cannot be loaded
         */
        @SafeVarargs
        public final Set<Class<?>> scan(Class<? extends Annotation>... annotationTypes)
                throws ClassNotFoundException {
            List<String> packages = getPackages();
            if (packages.isEmpty()) {
                return Collections.emptySet();
            }
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                    false);
            scanner.setEnvironment(this.context.getEnvironment());
            scanner.setResourceLoader(this.context);
            for (Class<? extends Annotation> annotationType : annotationTypes) {
                scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
            }
            Set<Class<?>> entitySet = new HashSet<>();
            for (String basePackage : packages) {
                if (StringUtils.hasText(basePackage)) {
                    for (BeanDefinition candidate : scanner
                            .findCandidateComponents(basePackage)) {
                        entitySet.add(ClassUtils.forName(candidate.getBeanClassName(),
                                this.context.getClassLoader()));
                    }
                }
            }
            return entitySet;
        }

        private List<String> getPackages() {
            return Collections.singletonList("io.github.kaiso.relmongo.data");
        }

    }

    public class RMLocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
        public Date convert(LocalDateTime source) {
            return Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
        }
    }

}
