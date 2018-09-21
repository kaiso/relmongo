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

import com.mongodb.BasicDBList;

import io.github.kaiso.relmongo.annotation.CascadeType;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.exception.RelMongoProcessingException;
import io.github.kaiso.relmongo.mongo.DatabaseOperations;
import io.github.kaiso.relmongo.util.AnnotationsUtils;
import io.github.kaiso.relmongo.util.ReflectionsUtil;
import io.github.kaiso.relmongo.util.RelMongoConstants;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author Kais OMRI
 *
 */
public class PersistentPropertySavingCallback implements FieldCallback {

    private Object source;
    private MongoOperations mongoOperations;
    private String collectionName;

    public PersistentPropertySavingCallback(Object source, String collectionName, MongoOperations mongoOperations) {
        super();
        this.source = source;
        this.mongoOperations = mongoOperations;
        this.collectionName = collectionName;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);
        if (field.isAnnotationPresent(OneToMany.class)) {
            saveAssociation(field, field.getAnnotation(OneToMany.class).cascade(), field.getAnnotation(OneToMany.class).orphanRemoval());
        } else if (field.isAnnotationPresent(OneToOne.class)
                && StringUtils.isEmpty(field.getAnnotation(OneToOne.class).mappedBy())) {
            saveAssociation(field, field.getAnnotation(OneToOne.class).cascade(), field.getAnnotation(OneToOne.class).orphanRemoval());
        }

    }

    private void saveAssociation(Field field, CascadeType cascadeType, Boolean orphanRemoval) {
        String name = AnnotationsUtils.getJoinProperty(field);
        Object reference = null;
        reference = ((org.bson.Document) source).get(field.getName());
        String childCollectionName = getCollectionName(field);
        if (reference instanceof BasicDBList) {
            BasicDBList list = new BasicDBList();
            list.addAll(((BasicDBList) reference).stream()
                    .map(dbObject -> this.keepOnlyIdentifier(dbObject, childCollectionName, cascadeType))
                    .collect(Collectors.toList()));
            ((org.bson.Document) source).remove(field.getName());
            ((org.bson.Document) source).put(name, list);
            if (Boolean.TRUE.equals(orphanRemoval)) {
                removeOrphans((ObjectId) ((org.bson.Document) source).get("_id"),
                        list.parallelStream().map(o -> (ObjectId) ((org.bson.Document) o).get("_id")).collect(Collectors.toList()),
                        name,
                        childCollectionName);
            }
        } else if (reference instanceof org.bson.Document) {
            ((org.bson.Document) source).remove(field.getName());
            org.bson.Document child = this.keepOnlyIdentifier(reference, childCollectionName, cascadeType);
            ((org.bson.Document) source).put(name, child);
            if (Boolean.TRUE.equals(orphanRemoval)) {
                removeOrphans((ObjectId) ((org.bson.Document) source).get("_id"), Arrays.asList(child.get("_id")), name, childCollectionName);
            }
        } else if (reference == null && Boolean.TRUE.equals(orphanRemoval)) {
            removeOrphans((ObjectId) ((org.bson.Document) source).get("_id"), Collections.emptyList(), name, childCollectionName);
        }

    }

    private org.bson.Document keepOnlyIdentifier(Object obj, String collection, CascadeType cascadeType) {
        Object objectId = ((org.bson.Document) obj).get("_id");
        if (objectId == null && !Arrays.asList(CascadeType.PERSIST, CascadeType.ALL).contains(cascadeType)) {
            throw new RelMongoProcessingException(
                    "ObjectId must not be null when persisting without cascade ALL or PERSIST ");
        }
        return new org.bson.Document().append("_id", objectId).append(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME,
                collection);
    }

    private String getCollectionName(Field field) {
        String collection = ReflectionsUtil.getGenericType(field).getAnnotation(Document.class).collection();
        if (collection == null || "".equals(collection)) {
            collection = ReflectionsUtil.getGenericType(field).getSimpleName().toLowerCase();
        }
        return collection;
    }

    @SuppressWarnings({ "unchecked" })
    private void removeOrphans(ObjectId parentId, List<Object> child, String propertyName, String childCollectionName) {
        BasicDBList result = DatabaseOperations.getDocumentsById(mongoOperations, Arrays.asList(parentId), collectionName);
        if (result == null || result.isEmpty()) {
            return;
        }
        org.bson.Document currentDocument = (org.bson.Document) result.get(0);
        if (currentDocument != null) {
            Object currentChild = currentDocument.get(propertyName);
            List<ObjectId> objectsToRemove = null;
            if (currentChild instanceof ArrayList) {
                objectsToRemove = new ArrayList<>();
                ArrayList<org.bson.Document> childList = (ArrayList<org.bson.Document>) currentChild;
                childList.removeIf(o -> child.contains(o.get("_id")));
                objectsToRemove.addAll(childList.parallelStream().map(o -> (ObjectId) o.get("_id"))
                        .collect(Collectors.toList()));
            } else if (currentChild instanceof org.bson.Document) {
                ObjectId currentChildId = (ObjectId) ((org.bson.Document) currentChild).get("_id");
                if (child.isEmpty() || !currentChildId.equals(child.get(0))) {
                    objectsToRemove = new ArrayList<>();
                    objectsToRemove.add(currentChildId);
                }
            }

            if (objectsToRemove != null && !objectsToRemove.isEmpty()) {
                DatabaseOperations.removeObjectsByIds(mongoOperations, childCollectionName, objectsToRemove);
            }
        }
    }

}
