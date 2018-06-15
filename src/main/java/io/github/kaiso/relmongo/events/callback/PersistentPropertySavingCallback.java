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

import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.util.ReflectionsUtil;
import io.github.kaiso.relmongo.util.RelMongoConstants;

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
        if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(OneToOne.class)) {
            saveAssociation(field);
        }

    }

    private void saveAssociation(Field field) {
        String name = "";
        try {
            name = field.getAnnotation(JoinProperty.class).name();
        } catch (Exception e) {
            throw new IllegalArgumentException("Missing or misconfigured @JoinProperty annotation", e);
        }
        Object reference = null;
        try {
            reference = ((org.bson.Document) source).get(field.getName());
            String collection = getCollectionName(field);
            if (reference instanceof BasicDBList) {
                BasicDBList list = new BasicDBList();
                list.addAll(((BasicDBList) reference).stream().map(dbObject -> this.keepOnlyIdentifier(dbObject, collection)).collect(Collectors.toList()));
                ((org.bson.Document) source).remove(field.getName());
                ((org.bson.Document) source).put(name, list);
            } else if (reference instanceof org.bson.Document) {
                ((org.bson.Document) source).remove(field.getName());
                ((org.bson.Document) source).put(name, this.keepOnlyIdentifier(reference, collection));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Property defined in @JoinProperty annotation is not present", e);
        }
    }

    private org.bson.Document keepOnlyIdentifier(Object obj, String collection) {
        return new org.bson.Document().append("_id", ((org.bson.Document) obj).get("_id")).append(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME, collection);
    }

    private String getCollectionName(Field field) {
        return ReflectionsUtil.getGenericType(field).getAnnotation(Document.class).collection();
    }

}
