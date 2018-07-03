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
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import io.github.kaiso.relmongo.annotation.CascadeType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.exception.RelMongoConfigurationException;
import io.github.kaiso.relmongo.exception.RelMongoProcessingException;
import io.github.kaiso.relmongo.util.ReflectionsUtil;
import io.github.kaiso.relmongo.util.RelMongoConstants;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.stream.Collectors;

public class PersistentPropertySavingCallback implements FieldCallback {

    private Object source;

    public PersistentPropertySavingCallback(Object source) {
        super();
        this.source = source;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);
        if (field.isAnnotationPresent(OneToMany.class)) {
            saveAssociation(field, field.getAnnotation(OneToMany.class).cascade());
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            saveAssociation(field, field.getAnnotation(OneToOne.class).cascade());
        }

    }

    private void saveAssociation(Field field, CascadeType cascadeType) {
        String name = "";
        try {
            name = field.getAnnotation(JoinProperty.class).name();
        } catch (Exception e) {
            throw new RelMongoConfigurationException("Missing or misconfigured @JoinProperty annotation", e);
        }
        Object reference = null;
        reference = ((BasicDBObject) source).get(field.getName());
        String collection = getCollectionName(field);
        if (reference instanceof BasicDBList) {
            BasicDBList list = new BasicDBList();
            list.addAll(((BasicDBList) reference).stream().map(dbObject -> this.keepOnlyIdentifier(dbObject, collection, cascadeType))
                    .collect(Collectors.toList()));
            ((BasicDBObject) source).remove(field.getName());
            ((BasicDBObject) source).put(name, list);
        } else if (reference instanceof BasicDBObject) {
            ((BasicDBObject) source).remove(field.getName());
            ((BasicDBObject) source).put(name, this.keepOnlyIdentifier(reference, collection, cascadeType));
        }
    }

    private BasicDBObject keepOnlyIdentifier(Object obj, String collection, CascadeType cascadeType) {
        Object objectId = ((DBObject) obj).get("_id");
        if (objectId == null) {
            if (cascadeType != CascadeType.PERSIST) {
                throw new RelMongoProcessingException("ObjectId must not be null when persisting without cascade PERSIST ");
            } else {
                objectId = ObjectId.get();
            }
        }
        return new BasicDBObject().append("_id", objectId).append(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME, collection);
    }

    private String getCollectionName(Field field) {
        String collection = ReflectionsUtil.getGenericType(field).getAnnotation(Document.class).collection();
        if (collection == null || "".equals(collection)) {
            collection = ReflectionsUtil.getGenericType(field).getSimpleName().toLowerCase();
        }
        return collection;
    }

}
