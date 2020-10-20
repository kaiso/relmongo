/**
*   Copyright 2018 Kais OMRI [kais.omri.int@gmail.com] and authors.
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

import io.github.kaiso.relmongo.annotation.CascadeType;
import io.github.kaiso.relmongo.annotation.Nested;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.mongo.DatabaseOperations;
import io.github.kaiso.relmongo.mongo.DocumentUtils;
import io.github.kaiso.relmongo.util.AnnotationsUtils;
import io.github.kaiso.relmongo.util.ReflectionsUtil;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 
 * @author Kais OMRI
 *
 */
public class PersistentPropertyCascadingRemoveCallback implements FieldCallback {

    private Document source;
    private MongoOperations mongoOperations;
    // private Object entity;
    private Class<?> entityType;
    private String collectionName;

    public PersistentPropertyCascadingRemoveCallback(Document source, MongoOperations mongoOperations, Class<?> entityType, String collectionName) {
        super();
        this.source = source;
        this.mongoOperations = mongoOperations;
        this.entityType = entityType;
        this.collectionName = collectionName;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);
        if (field.isAnnotationPresent(OneToMany.class)) {
            doCascade(field, field.getAnnotation(OneToMany.class).cascade());
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            doCascade(field, field.getAnnotation(OneToOne.class).cascade());
        } else if ( field.isAnnotationPresent(Nested.class)) {

            Document childDocument = (Document) source.get(field.getName());
            
            if ( childDocument != null ) {
                
                Class<?> childEntityType = ReflectionsUtil.getGenericType(field);
                
                PersistentPropertyCascadingRemoveCallback callback = new PersistentPropertyCascadingRemoveCallback(childDocument, mongoOperations,
                        childEntityType, collectionName);
                    callback.doProcessing();
            }
            
            
        }

    }

    private void doCascade(Field field, CascadeType cascadeType) {
        if (!Arrays.asList(CascadeType.REMOVE, CascadeType.ALL).contains(cascadeType)) {
            return;
        }
        Object child = this.source.get(AnnotationsUtils.getJoinProperty(field));
        if (child != null) {
            if (Collection.class.isAssignableFrom(child.getClass())) {
                cascadeRemove(ReflectionsUtil.getGenericType(field), (Collection<?>) child);
            } else {
                cascadeRemove(ReflectionsUtil.getGenericType(field), Collections.singletonList(child));
            }
        }

    }

    private void cascadeRemove(Class<?> entityClass, Collection<?> children) {
        DatabaseOperations.removeObjectsByIds(mongoOperations, entityClass, children.stream()
            .filter(p -> p instanceof Document)
            .filter(p -> ((Document) p).get("_id") != null)
            .map(DocumentUtils::mapIdentifier).collect(Collectors.toList()));
    }

    public void doProcessing() {
        loadEntity();
        ReflectionUtils.doWithFields(entityType, this);
    }

    private void loadEntity() {
        if (!DocumentUtils.isLoaded(source)) {
            Object sourceId = this.source.get("_id");
            if (sourceId != null) {
                this.source = DatabaseOperations.getDocumentByPropertyValue(mongoOperations, sourceId, "_id", collectionName);
            }
        }
    }

}
