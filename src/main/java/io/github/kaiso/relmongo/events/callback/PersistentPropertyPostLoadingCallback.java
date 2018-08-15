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
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.events.processor.MappedByProcessor;
import io.github.kaiso.relmongo.exception.RelMongoConfigurationException;
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
import java.util.List;
import java.util.Map.Entry;
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

    public PersistentPropertyPostLoadingCallback(Object source, Document document, MongoOperations mongoOperations) {
        super();
        this.source = source;
        this.mongoOperations = mongoOperations;
        this.document = document;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);
        Class<?> type = ReflectionsUtil.getGenericType(field);

        String mappedByProperty = null;
        Entry<FetchType, String> result = AnnotationsUtils.getMappedByAndFetchType(field);
        String mappedBy = result.getValue();
        FetchType fetchType = result.getKey();

        if (DocumentUtils.isLoaded(document.get(field.getName()))) {
            MappedByProcessor.processChild(source, source, field, type);
            return;
        }

        if (mappedBy != null && !"".equals(mappedBy)) {
            Field targetProp;
            try {
                targetProp = type.getDeclaredField(mappedBy);
                ReflectionUtils.makeAccessible(targetProp);
            } catch (NoSuchFieldException | SecurityException ex) {
                throw new RelMongoConfigurationException("unable to find field with name " + mappedBy +
                        " in the type " + type.getCanonicalName(), ex);
            }
            mappedByProperty = AnnotationsUtils.getJoinProperty(targetProp);
        }

        if (FetchType.LAZY.equals(fetchType)) {
            List<Object> identifierList = new ArrayList<>();
            if (mappedByProperty == null) {
                String joinPropertyName = AnnotationsUtils.getJoinProperty(field);
                if (field.isAnnotationPresent(OneToMany.class) && !Collection.class.isAssignableFrom(field.getType())) {
                    throw new RelMongoConfigurationException("in @OneToMany, the field must be of type collection ");
                }
                Object relations = document.get(joinPropertyName);

                if (relations == null || (relations instanceof Document && ((Document) relations).keySet().isEmpty())) {
                    return;
                }
                if (relations instanceof Collection) {
                    identifierList.addAll(((Collection<?>) relations).stream()
                            .map(DocumentUtils::mapIdentifier).collect(Collectors.toList()));
                } else {
                    identifierList.add(DocumentUtils.mapIdentifier(relations));
                }
            } else {
                identifierList.add(document.getObjectId("_id"));
            }
            field.set(source, PersistentRelationResolver.lazyLoader(field.getType(), mongoOperations,
                    identifierList, mappedByProperty, type,
                    field.get(source), source, field.getName()));
        } else if (mappedByProperty != null) {
            field.set(source, DatabaseOperations.findByPropertyValue(mongoOperations, type,
                    mappedByProperty + "._id", document.getObjectId("_id")));
        }

    }

}
