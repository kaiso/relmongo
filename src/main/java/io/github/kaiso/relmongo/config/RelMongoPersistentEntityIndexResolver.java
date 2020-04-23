package io.github.kaiso.relmongo.config;

import io.github.kaiso.relmongo.annotation.RelMongoAnnotation;
import io.github.kaiso.relmongo.config.RelMongoPersistentEntityIndexResolver.CycleGuard.Path;
import io.github.kaiso.relmongo.config.RelMongoPersistentEntityIndexResolver.TextIndexIncludeOptions.IncludeStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.HashIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.index.TextIndexDefinition.TextIndexDefinitionBuilder;
import org.springframework.data.mongodb.core.index.TextIndexDefinition.TextIndexedFieldSpec;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * inspired from MongoPersistentEntityIndexResolver
 * see copyright notice on
 * {@link org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver}
 * 
 * @author Kais OMRI (kaiso)
 *
 */
public class RelMongoPersistentEntityIndexResolver extends MongoPersistentEntityIndexResolver {

    private static final Logger logger = LoggerFactory.getLogger(RelMongoPersistentEntityIndexResolver.class);

    private MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext;

    public RelMongoPersistentEntityIndexResolver(MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(mappingContext);
        this.mappingContext = mappingContext;
    }

    @Override
    public List<IndexDefinitionHolder> resolveIndexForEntity(MongoPersistentEntity<?> root) {

        Assert.notNull(root, "MongoPersistentEntity must not be null!");
        Document document = root.findAnnotation(Document.class);
        Assert.notNull(document, () -> String
            .format("Entity %s is not a collection root. Make sure to annotate it with @Document!", root.getName()));

        List<IndexDefinitionHolder> indexInformation = new ArrayList<>();
        String collection = root.getCollection();
        indexInformation.addAll(potentiallyCreateCompoundIndexDefinitions("", collection, root));
        indexInformation.addAll(potentiallyCreateTextIndexDefinition(root, collection));

        root.doWithProperties((PropertyHandler<MongoPersistentProperty>) property -> this
            .potentiallyAddIndexForProperty(root, property, indexInformation, new CycleGuard()));

        indexInformation.addAll(resolveIndexesForDbrefs("", collection, root));

        return indexInformation;
    }

    private void potentiallyAddIndexForProperty(MongoPersistentEntity<?> root, MongoPersistentProperty persistentProperty,
        List<IndexDefinitionHolder> indexes, CycleGuard guard) {

        try {
            if (persistentProperty.isAnnotationPresent(RelMongoAnnotation.class)) {
                return;
            }
            if (persistentProperty.isEntity()) {
                indexes.addAll(resolveIndexForClass(persistentProperty.getTypeInformation().getActualType(),
                    persistentProperty.getFieldName(), Path.of(persistentProperty), root.getCollection(), guard));
            }

            List<IndexDefinitionHolder> indexDefinitions = createIndexDefinitionHolderForProperty(
                persistentProperty.getFieldName(), root.getCollection(), persistentProperty);
            if (!indexDefinitions.isEmpty()) {
                indexes.addAll(indexDefinitions);
            }
        } catch (CyclicPropertyReferenceException e) {
            logger.info(e.getMessage());
        }
    }

    private List<IndexDefinitionHolder> createIndexDefinitionHolderForProperty(String dotPath, String collection,
        MongoPersistentProperty persistentProperty) {

        List<IndexDefinitionHolder> indices = new ArrayList<>(2);

        if (persistentProperty.isAnnotationPresent(Indexed.class)) {
            indices.add(createIndexDefinition(dotPath, collection, persistentProperty));
        } else if (persistentProperty.isAnnotationPresent(GeoSpatialIndexed.class)) {
            indices.add(createGeoSpatialIndexDefinition(dotPath, collection, persistentProperty));
        }

        if (persistentProperty.isAnnotationPresent(HashIndexed.class)) {
            indices.add(createHashedIndexDefinition(dotPath, collection, persistentProperty));
        }

        return indices;
    }

    private List<IndexDefinitionHolder> resolveIndexForClass(final TypeInformation<?> type, final String dotPath,
        final Path path, final String collection, final CycleGuard guard) {

        MongoPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(type);

        final List<IndexDefinitionHolder> indexInformation = new ArrayList<>();
        indexInformation.addAll(potentiallyCreateCompoundIndexDefinitions(dotPath, collection, entity));

        entity.doWithProperties((PropertyHandler<MongoPersistentProperty>) property -> this
            .guardAndPotentiallyAddIndexForProperty(property, dotPath, path, collection, indexInformation, guard));

        indexInformation.addAll(resolveIndexesForDbrefs(dotPath, collection, entity));

        return indexInformation;
    }

    private void guardAndPotentiallyAddIndexForProperty(MongoPersistentProperty persistentProperty, String dotPath,
        Path path, String collection, List<IndexDefinitionHolder> indexes, CycleGuard guard) {

        String propertyDotPath = (StringUtils.hasText(dotPath) ? dotPath + "." : "") + persistentProperty.getFieldName();

        Path propertyPath = path.append(persistentProperty);
        guard.protect(persistentProperty, propertyPath);

        if (persistentProperty.isEntity()) {
            try {
                indexes.addAll(resolveIndexForClass(persistentProperty.getTypeInformation().getActualType(), propertyDotPath,
                    propertyPath, collection, guard));
            } catch (CyclicPropertyReferenceException e) {
                logger.info(e.getMessage());
            }
        }

        List<IndexDefinitionHolder> indexDefinitions = createIndexDefinitionHolderForProperty(propertyDotPath, collection,
            persistentProperty);

        if (!indexDefinitions.isEmpty()) {
            indexes.addAll(indexDefinitions);
        }
    }

    private List<IndexDefinitionHolder> resolveIndexesForDbrefs(final String path, final String collection,
        MongoPersistentEntity<?> entity) {

        final List<IndexDefinitionHolder> indexes = new ArrayList<>(0);
        entity.doWithAssociations((AssociationHandler<MongoPersistentProperty>) association -> this
            .resolveAndAddIndexesForAssociation(association, indexes, path, collection));
        return indexes;
    }

    private void resolveAndAddIndexesForAssociation(Association<MongoPersistentProperty> association,
        List<IndexDefinitionHolder> indexes, String path, String collection) {

        MongoPersistentProperty property = association.getInverse();

        String propertyDotPath = (StringUtils.hasText(path) ? path + "." : "") + property.getFieldName();

        if (property.isAnnotationPresent(GeoSpatialIndexed.class) || property.isAnnotationPresent(TextIndexed.class)) {
            throw new MappingException(
                String.format("Cannot create geospatial-/text- index on DBRef in collection '%s' for path '%s'.", collection,
                    propertyDotPath));
        }

        List<IndexDefinitionHolder> indexDefinitions = createIndexDefinitionHolderForProperty(propertyDotPath, collection,
            property);

        if (!indexDefinitions.isEmpty()) {
            indexes.addAll(indexDefinitions);
        }
    }

    private Collection<? extends IndexDefinitionHolder> potentiallyCreateTextIndexDefinition(
        MongoPersistentEntity<?> root, String collection) {

        String name = root.getType().getSimpleName() + "_TextIndex";
        if (name.getBytes().length > 127) {
            String[] args = ClassUtils.getShortNameAsProperty(root.getType()).split("\\.");
            name = "";
            Iterator<String> it = Arrays.asList(args).iterator();
            while (it.hasNext()) {

                if (!it.hasNext()) {
                    name += it.next() + "_TextIndex";
                } else {
                    name += (it.next().charAt(0) + ".");
                }
            }

        }
        TextIndexDefinitionBuilder indexDefinitionBuilder = new TextIndexDefinitionBuilder().named(name);

        if (StringUtils.hasText(root.getLanguage())) {
            indexDefinitionBuilder.withDefaultLanguage(root.getLanguage());
        }

        try {
            appendTextIndexInformation("", Path.empty(), indexDefinitionBuilder, root,
                new TextIndexIncludeOptions(IncludeStrategy.DEFAULT), new CycleGuard());
        } catch (CyclicPropertyReferenceException e) {
            logger.info(e.getMessage());
        }

        if (root.hasCollation()) {
            indexDefinitionBuilder.withSimpleCollation();
        }

        TextIndexDefinition indexDefinition = indexDefinitionBuilder.build();

        if (!indexDefinition.hasFieldSpec()) {
            return Collections.emptyList();
        }

        IndexDefinitionHolder holder = new IndexDefinitionHolder("", indexDefinition, collection);
        return Collections.singletonList(holder);

    }

    private void appendTextIndexInformation(final String dotPath, final Path path,
        final TextIndexDefinitionBuilder indexDefinitionBuilder, final MongoPersistentEntity<?> entity,
        final TextIndexIncludeOptions includeOptions, final CycleGuard guard) {

        entity.doWithProperties(new PropertyHandler<MongoPersistentProperty>() {

            @Override
            public void doWithPersistentProperty(MongoPersistentProperty persistentProperty) {

                guard.protect(persistentProperty, path);

                if (persistentProperty.isExplicitLanguageProperty() && !StringUtils.hasText(dotPath)) {
                    indexDefinitionBuilder.withLanguageOverride(persistentProperty.getFieldName());
                }

                TextIndexed indexed = persistentProperty.findAnnotation(TextIndexed.class);

                if (includeOptions.isForce() || indexed != null || persistentProperty.isEntity()) {

                    String propertyDotPath = (StringUtils.hasText(dotPath) ? dotPath + "." : "")
                        + persistentProperty.getFieldName();

                    Path propertyPath = path.append(persistentProperty);

                    TextIndexedFieldSpec parentFieldSpec = includeOptions.getParentFieldSpec();
                    Float weight = indexed != null ? indexed.weight()
                        : (parentFieldSpec != null ? parentFieldSpec.getWeight() : 1.0F);

                    if (persistentProperty.isEntity()) {

                        TextIndexIncludeOptions optionsForNestedType = includeOptions;
                        if (!IncludeStrategy.FORCE.equals(includeOptions.getStrategy()) && indexed != null) {
                            optionsForNestedType = new TextIndexIncludeOptions(IncludeStrategy.FORCE,
                                new TextIndexedFieldSpec(propertyDotPath, weight));
                        }

                        try {
                            appendTextIndexInformation(propertyDotPath, propertyPath, indexDefinitionBuilder,
                                mappingContext.getPersistentEntity(persistentProperty.getActualType()), optionsForNestedType, guard);
                        } catch (CyclicPropertyReferenceException e) {
                            logger.info(e.getMessage());
                        } catch (InvalidDataAccessApiUsageException e) {
                            logger.info(String.format("Potentially invalid index structure discovered. Breaking operation for %s.",
                                entity.getName()), e);
                        }
                    } else if (includeOptions.isForce() || indexed != null) {
                        indexDefinitionBuilder.onField(propertyDotPath, weight);
                    }
                }

            }
        });

    }

    private List<IndexDefinitionHolder> potentiallyCreateCompoundIndexDefinitions(String dotPath, String collection,
        MongoPersistentEntity<?> entity) {

        if (entity.findAnnotation(CompoundIndexes.class) == null && entity.findAnnotation(CompoundIndex.class) == null) {
            return Collections.emptyList();
        }

        return createCompoundIndexDefinitions(dotPath, collection, entity);
    }

    /**
     * {@link CycleGuard} holds information about properties and the paths for
     * accessing those. This information is used
     * to detect potential cycles within the references.
     *
     * @author Christoph Strobl
     * @author Mark Paluch
     */
    static class CycleGuard {

        private final Set<String> seenProperties = new HashSet<>();

        /**
         * Detect a cycle in a property path if the property was seen at least once.
         *
         * @param property
         *            The property to inspect
         * @param path
         *            The type path under which the property can be reached.
         * @throws CyclicPropertyReferenceException
         *             in case a potential cycle is detected.
         * @see Path#isCycle()
         */
        void protect(MongoPersistentProperty property, Path path) throws CyclicPropertyReferenceException {

            String propertyTypeKey = createMapKey(property);
            if (!seenProperties.add(propertyTypeKey)) {

                if (path.isCycle()) {
                    throw new CyclicPropertyReferenceException(property.getFieldName(), property.getOwner().getType(),
                        path.toCyclePath());
                }
            }
        }

        private String createMapKey(MongoPersistentProperty property) {
            return ClassUtils.getShortName(property.getOwner().getType()) + ":" + property.getFieldName();
        }

        /**
         * Path defines the full property path from the document root. <br />
         * A {@link Path} with {@literal spring.data.mongodb} would be created for the
         * property {@code Three.mongodb}.
         *
         * <pre>
         * <code>
         * &#64;Document
         * class One {
         *   Two spring;
         * }
         *
         * class Two {
         *   Three data;
         * }
         *
         * class Three {
         *   String mongodb;
         * }
         * </code>
         * </pre>
         *
         * @author Christoph Strobl
         * @author Mark Paluch
         */
        static class Path {

            private static final Path EMPTY = new Path(Collections.emptyList(), false);

            private final List<PersistentProperty<?>> elements;
            private final boolean cycle;

            public Path(List<PersistentProperty<?>> emptyList, boolean b) {
                this.elements = emptyList;
                this.cycle = b;
            }

            /**
             * @return an empty {@link Path}.
             * @since 1.10.8
             */
            static Path empty() {
                return EMPTY;
            }

            /**
             * Creates a new {@link Path} from the initial {@link PersistentProperty}.
             *
             * @param initial
             *            must not be {@literal null}.
             * @return the new {@link Path}.
             * @since 1.10.8
             */
            static Path of(PersistentProperty<?> initial) {
                return new Path(Collections.singletonList(initial), false);
            }

            /**
             * Creates a new {@link Path} by appending a {@link PersistentProperty
             * breadcrumb} to the path.
             *
             * @param breadcrumb
             *            must not be {@literal null}.
             * @return the new {@link Path}.
             * @since 1.10.8
             */
            Path append(PersistentProperty<?> breadcrumb) {

                List<PersistentProperty<?>> elements = new ArrayList<>(this.elements.size() + 1);
                elements.addAll(this.elements);
                elements.add(breadcrumb);

                return new Path(elements, this.elements.contains(breadcrumb));
            }

            /**
             * @return {@literal true} if a cycle was detected.
             * @since 1.10.8
             */
            public boolean isCycle() {
                return cycle;
            }

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Object#toString()
             */
            @Override
            public String toString() {
                return this.elements.isEmpty() ? "(empty)" : toPath(this.elements.iterator());
            }

            /**
             * Returns the cycle path truncated to the first discovered cycle. The result
             * for the path
             * {@literal foo.bar.baz.bar} is {@literal bar -> baz -> bar}.
             *
             * @return the cycle path truncated to the first discovered cycle.
             * @since 1.10.8
             */
            String toCyclePath() {

                if (!cycle) {
                    return "";
                }

                for (int i = 0; i < this.elements.size(); i++) {

                    int index = indexOf(this.elements, this.elements.get(i), i + 1);

                    if (index != -1) {
                        return toPath(this.elements.subList(i, index + 1).iterator());
                    }
                }

                return toString();
            }

            private static <T> int indexOf(List<T> haystack, T needle, int offset) {

                for (int i = offset; i < haystack.size(); i++) {
                    if (haystack.get(i).equals(needle)) {
                        return i;
                    }
                }

                return -1;
            }

            private static String toPath(Iterator<PersistentProperty<?>> iterator) {

                StringBuilder builder = new StringBuilder();
                while (iterator.hasNext()) {

                    builder.append(iterator.next().getName());
                    if (iterator.hasNext()) {
                        builder.append(" -> ");
                    }
                }

                return builder.toString();
            }
        }
    }

    static class TextIndexIncludeOptions {

        enum IncludeStrategy {
            FORCE, DEFAULT;
        }

        private final IncludeStrategy strategy;

        private final @Nullable TextIndexedFieldSpec parentFieldSpec;

        public TextIndexIncludeOptions(IncludeStrategy strategy, @Nullable TextIndexedFieldSpec parentFieldSpec) {
            this.strategy = strategy;
            this.parentFieldSpec = parentFieldSpec;
        }

        public TextIndexIncludeOptions(IncludeStrategy strategy) {
            this(strategy, null);
        }

        public IncludeStrategy getStrategy() {
            return strategy;
        }

        @Nullable
        public TextIndexedFieldSpec getParentFieldSpec() {
            return parentFieldSpec;
        }

        public boolean isForce() {
            return IncludeStrategy.FORCE.equals(strategy);
        }

    }

}
