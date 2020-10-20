/**
*   Copyright 2018 Kais OMRI and authors.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

package io.github.kaiso.relmongo.events.callback;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.ManyToOne;
import io.github.kaiso.relmongo.annotation.Nested;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.events.processor.MappedByProcessor;
import io.github.kaiso.relmongo.exception.RelMongoConfigurationException;
import io.github.kaiso.relmongo.model.MappedByMetadata;
import io.github.kaiso.relmongo.mongo.DatabaseOperations;
import io.github.kaiso.relmongo.mongo.DocumentUtils;
import io.github.kaiso.relmongo.mongo.PersistentRelationResolver;
import io.github.kaiso.relmongo.util.AnnotationsUtils;
import io.github.kaiso.relmongo.util.ReflectionsUtil;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 
 * @author Kais OMRI
 *
 */
public class PersistentPropertyPostLoadingCallback implements FieldCallback {

    private Object source;
    private MongoOperations mongoOperations;
    private Document document;
    private static ConcurrentHashMap<UUID, Map<Object, Object>> cache = new ConcurrentHashMap<>();
    private UUID executionId;

    public PersistentPropertyPostLoadingCallback(Object source, Document document, MongoOperations mongoOperations) {
        super();
        this.source = source;
        this.mongoOperations = mongoOperations;
        this.document = document;
        this.executionId = UUID.randomUUID();
        cache.put(executionId, new HashMap<>());
    }

    public void close() {
        cache.remove(executionId);
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);

        if ( field.isAnnotationPresent(Nested.class) ) {
            Object object = field.get(source);
            if ( object != null ) {

                Document nestedDocument = (Document) ((org.bson.Document) document).get(field.getName());
                
                PersistentPropertyPostLoadingCallback callback = new PersistentPropertyPostLoadingCallback(object, nestedDocument, mongoOperations);
                ReflectionUtils.doWithFields(object.getClass(), callback);
                
            }
            return;
        }
        
        if (!(field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToOne.class))) {
            return;
        }

        Class<?> type = ReflectionsUtil.getGenericType(field);

        FetchType fetchType = AnnotationsUtils.getFetchType(field);

        if (DocumentUtils.isLoaded(document.get(field.getName()))) {
            MappedByProcessor.processChild(source, source, field, type);
            return;
        }

        List<Object> identifierList = new ArrayList<>();
        MappedByMetadata mappedByInfos = AnnotationsUtils.getMappedByInfos(field);

        if (mappedByInfos.getMappedByValue() == null) {
            String joinPropertyName = AnnotationsUtils.getJoinProperty(field);
            if (field.isAnnotationPresent(OneToMany.class) && !Collection.class.isAssignableFrom(field.getType())) {
                throw new RelMongoConfigurationException("in @OneToMany, the field must be of type collection ");
            }
            Object relations = document.get(joinPropertyName);

            if (relations == null || (relations instanceof Document && ((Document) relations).keySet().isEmpty())
                || (relations instanceof Collection && ((Collection<?>) relations).isEmpty())) {
                return;
            }
            if (relations instanceof Collection) {
                identifierList.addAll(((Collection<?>) relations).stream()
                    .map(DocumentUtils::mapIdentifier).collect(Collectors.toList()));
            } else {
                identifierList.add(DocumentUtils.mapIdentifier(relations));
            }
        } else {
            identifierList.add(DocumentUtils.mapIdentifier(document));
        }

        if (FetchType.LAZY.equals(fetchType) || mappedByInfos.getMappedByValue() != null) {
            // mappedBy fields are loaded only in lazy mode to avoid cycles in loading
            ReflectionUtils.setField(field, source, PersistentRelationResolver.lazyLoader(field.getType(), mongoOperations,
                identifierList, mappedByInfos.getMappedByJoinProperty(), type,
                field.get(source), source, field.getName()));
        } else if (FetchType.EAGER.equals(fetchType)) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                ReflectionUtils.setField(field, source,
                    DatabaseOperations.findByIds(mongoOperations, type, identifierList.toArray(new Object[identifierList.size()])));
            } else {
                ReflectionUtils.setField(field, source,
                    DatabaseOperations.findByPropertyValue(mongoOperations, type, "_id", identifierList.get(0)));
            }
            MappedByProcessor.processChild(source, source, field, type);
        }
    }

}
