package io.github.kaiso.relmongo.config;

import com.mongodb.MongoException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.MappingContextEvent;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexOperationsProvider;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver.IndexDefinitionHolder;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.util.MongoDbErrorCodes;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component that inspects {@link MongoPersistentEntity} instances contained in
 * the given {@link MongoMappingContext}
 * for indexing metadata and ensures the indexes to be available.
 * this class is inspired from spring framework
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Philipp Schneider
 * @author Johno Crawford
 * @author Laurent Canet
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author kaiso
 */
public class RelMongoPersistentEntityIndexCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger("io.github.kaiso.relmongo.RelMongoIndexEngine");
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final Map<Class<?>, Boolean> classesSeen = new ConcurrentHashMap<Class<?>, Boolean>();
    private final IndexOperationsProvider indexOperationsProvider;
    private final RelMongoMappingContext mappingContext;
    private final IndexResolver indexResolver;

    /**
     * Creates a new {@link RelMongoPersistentEntityIndexCreator} for the given
     * {@link MongoTemplate}
     *
     * @param mongoTemplate
     *            must not be {@literal null}.
     */
    public RelMongoPersistentEntityIndexCreator(MongoTemplate mongoTemplate) {

        Assert.notNull(mongoTemplate, "MongoTemplate must not be null!");

        this.indexOperationsProvider = mongoTemplate;

        MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
        if (mappingContext instanceof RelMongoMappingContext) {
            this.mappingContext = (RelMongoMappingContext) mappingContext;
            this.indexResolver = new RelMongoPersistentEntityIndexResolver(this.mappingContext);
            if (this.mappingContext.hasAutoIndexEnabled()) {
                LOGGER.warn(
                    "Index auto creation is deprecated and will be disabled by default in the next releases {} applications should manually set up indexes to avoid compatibility problems and performance issues",
                    LINE_SEPARATOR);
                ApplicationEventPublisher eventPublisher = new RelMongoMappingContextEventPublisher(this);
                this.mappingContext.setApplicationEventPublisher(eventPublisher);
                if (classesSeen.isEmpty()) {
                    // the events were published before index creator was created
                    this.mappingContext.getPersistentEntities().forEach(this::checkForIndexes);
                }
            }
        } else {
            if (mappingContext instanceof MongoMappingContext && ((MongoMappingContext) mappingContext).isAutoIndexCreation()) {
                LOGGER.warn(
                    "Index auto creation is enabled on defautl MongoMappingContext, this will cause index creation problems on associations {} Consider using io.github.kaiso.relmongo.config.RelMongoMappingContext instead of spring default MongoMappingContext or disable index auto creation",
                    LINE_SEPARATOR);
            }
            this.mappingContext = null;
            this.indexResolver = null;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.
     * springframework.context.ApplicationEvent)
     */
    public void onApplicationEvent(MappingContextEvent<?, ?> event) {

        if (mappingContext == null || !mappingContext.hasAutoIndexEnabled() || !event.wasEmittedBy(mappingContext)) {
            return;
        }

        PersistentEntity<?, ?> entity = event.getPersistentEntity();

        // Double check type as Spring infrastructure does not consider nested generics
        if (entity instanceof MongoPersistentEntity) {

            checkForIndexes((MongoPersistentEntity<?>) entity);
        }
    }

    private void checkForIndexes(final MongoPersistentEntity<?> entity) {

        Class<?> type = entity.getType();

        if (!classesSeen.containsKey(type)) {

            this.classesSeen.put(type, Boolean.TRUE);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Analyzing class " + type + " for index information.");
            }

            checkForAndCreateIndexes(entity);
        }
    }

    private void checkForAndCreateIndexes(MongoPersistentEntity<?> entity) {

        if (entity.isAnnotationPresent(Document.class)) {

            String collection = entity.getCollection();

            for (IndexDefinition indexDefinition : indexResolver.resolveIndexFor(entity.getTypeInformation())) {

                IndexDefinitionHolder indexToCreate = indexDefinition instanceof IndexDefinitionHolder
                    ? (IndexDefinitionHolder) indexDefinition
                    : new IndexDefinitionHolder("", indexDefinition, collection);

                createIndex(indexToCreate);
            }
        }
    }

    void createIndex(IndexDefinitionHolder indexDefinition) {

        try {

            IndexOperations indexOperations = indexOperationsProvider.indexOps(indexDefinition.getCollection());
            indexOperations.ensureIndex(indexDefinition);

        } catch (UncategorizedMongoDbException ex) {

            if (ex.getCause() instanceof MongoException
                && MongoDbErrorCodes.isDataIntegrityViolationCode(((MongoException) ex.getCause()).getCode())) {

                IndexInfo existingIndex = fetchIndexInformation(indexDefinition);
                String message = "Cannot create index for '%s' in collection '%s' with keys '%s' and options '%s'.";

                if (existingIndex != null) {
                    message += " Index already defined as '%s'.";
                }

                throw new DataIntegrityViolationException(
                    String.format(message, indexDefinition.getPath(), indexDefinition.getCollection(),
                        indexDefinition.getIndexKeys(), indexDefinition.getIndexOptions(), existingIndex),
                    ex.getCause());
            }

            throw ex;
        }
    }

    /**
     * Returns whether the current index creator was registered for the given
     * {@link MappingContext}.
     *
     * @param context
     *            the mapping context
     * @return whether the creator is mapped to this context
     */
    public boolean isIndexCreatorFor(MappingContext<?, ?> context) {
        return this.mappingContext.equals(context);
    }

    @Nullable
    private IndexInfo fetchIndexInformation(@Nullable IndexDefinitionHolder indexDefinition) {

        if (indexDefinition == null) {
            return null;
        }

        try {

            IndexOperations indexOperations = indexOperationsProvider.indexOps(indexDefinition.getCollection());
            Object indexNameToLookUp = indexDefinition.getIndexOptions().get("name");

            List<IndexInfo> existingIndexes = indexOperations.getIndexInfo();

            return existingIndexes.stream().//
                filter(indexInfo -> ObjectUtils.nullSafeEquals(indexNameToLookUp, indexInfo.getName())).//
                findFirst().//
                orElse(null);

        } catch (Exception e) {
            LOGGER.debug(
                String.format("Failed to load index information for collection '%s'.", indexDefinition.getCollection()), e);
        }

        return null;
    }

    class RelMongoMappingContextEventPublisher implements ApplicationEventPublisher {

        private final RelMongoPersistentEntityIndexCreator indexCreator;

        RelMongoMappingContextEventPublisher(RelMongoPersistentEntityIndexCreator indexCreator) {
            this.indexCreator = indexCreator;
        }

        @Override
        public void publishEvent(Object event) {
            // do nothing

        }

        @SuppressWarnings("unchecked")
        public void publishEvent(ApplicationEvent event) {
            if (event instanceof MappingContextEvent) {
                this.indexCreator.onApplicationEvent((MappingContextEvent<MongoPersistentEntity<?>, MongoPersistentProperty>) event);
            }
        }
    };

}
