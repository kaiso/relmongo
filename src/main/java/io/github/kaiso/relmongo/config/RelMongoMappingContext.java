package io.github.kaiso.relmongo.config;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.CachingMongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.MongoSimpleTypes;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

import java.util.AbstractMap;

/**
 * Default implementation of a {@link MappingContext} for MongoDB using
 * {@link BasicMongoPersistentEntity} and
 * {@link BasicMongoPersistentProperty} as primary abstractions.
 * this class is inspired from spring framework ( see mappingcontext)
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author kaiso
 */
public class RelMongoMappingContext extends MongoMappingContext {

    private static final FieldNamingStrategy DEFAULT_NAMING_STRATEGY = PropertyNameFieldNamingStrategy.INSTANCE;

    private FieldNamingStrategy fieldNamingStrategy = DEFAULT_NAMING_STRATEGY;

    private boolean autoIndexCreation = true;

    /**
     * Creates a new {@link MongoMappingContext}.
     */
    public RelMongoMappingContext() {
        setSimpleTypeHolder(MongoSimpleTypes.HOLDER);
    }

    /**
     * Configures the {@link FieldNamingStrategy} to be used to determine the field
     * name if no manual mapping is applied.
     * Defaults to a strategy using the plain property name.
     *
     * @param fieldNamingStrategy
     *            the {@link FieldNamingStrategy} to be used to determine the field
     *            name if no manual
     *            mapping is applied.
     */
    public void setFieldNamingStrategy(@Nullable FieldNamingStrategy fieldNamingStrategy) {
        this.fieldNamingStrategy = fieldNamingStrategy == null ? DEFAULT_NAMING_STRATEGY : fieldNamingStrategy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.mapping.context.AbstractMappingContext#
     * shouldCreatePersistentEntityFor(org.springframework.data.util.
     * TypeInformation)
     */
    @Override
    protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
        return !MongoSimpleTypes.HOLDER.isSimpleType(type.getType()) && !AbstractMap.class.isAssignableFrom(type.getType());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.mapping.AbstractMappingContext#
     * createPersistentProperty(java.lang.reflect.Field,
     * java.beans.PropertyDescriptor,
     * org.springframework.data.mapping.MutablePersistentEntity,
     * org.springframework.data.mapping.SimpleTypeHolder)
     */
    @Override
    public MongoPersistentProperty createPersistentProperty(Property property, BasicMongoPersistentEntity<?> owner,
        SimpleTypeHolder simpleTypeHolder) {
        return new CachingMongoPersistentProperty(property, owner, simpleTypeHolder, fieldNamingStrategy);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.mapping.BasicMappingContext#createPersistentEntity(
     * org.springframework.data.util.TypeInformation,
     * org.springframework.data.mapping.model.MappingContext)
     */
    @Override
    protected <T> BasicMongoPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
        return new BasicMongoPersistentEntity<T>(typeInformation);
    }

    /**
     * Returns whether auto-index creation is enabled or disabled.
     * <strong>NOTE:</strong>Index creation should happen at a well-defined time
     * that is ideally controlled by the
     * application itself.
     *
     * @return {@literal true} when auto-index creation is enabled; {@literal false}
     *         otherwise.
     * @since 2.2
     * @see org.springframework.data.mongodb.core.index.Indexed
     */
    public boolean isAutoIndexCreation() {
        return false;
    }

    public boolean hasAutoIndexEnabled() {
        return autoIndexCreation;
    }

    /**
     * Enables/disables auto-index creation.
     * <strong>NOTE:</strong>Index creation should happen at a well-defined time
     * that is ideally controlled by the
     * application itself.
     *
     * @param autoCreateIndexes
     *            set to {@literal false} to disable auto-index creation.
     * @since 2.2
     * @see org.springframework.data.mongodb.core.index.Indexed
     */
    public void setAutoIndexCreation(boolean autoCreateIndexes) {
        this.autoIndexCreation = autoCreateIndexes;
    }
}
