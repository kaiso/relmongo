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
package io.github.kaiso.relmongo.util;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * @author Kais OMRI
 */
public class ObjectIdReaderCallback implements FieldCallback {

    private ObjectId objectId;
    private Field idField;
    private Object source;

    public ObjectIdReaderCallback(Object source) {
        this.source = source;
    }

    @Override
    public void doWith(Field field) throws IllegalAccessException {
        if (field.isAnnotationPresent(Id.class)) {
            ReflectionUtils.makeAccessible(field);
            try {
                Object value = field.get(source);
                if (value instanceof String) {
                    objectId = new ObjectId((String) value);
                } else {
                    objectId = (ObjectId) value;
                }
                this.idField = field;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException("unable to access the @Id field", e);
            } catch (ClassCastException e) {
                throw new IllegalStateException("the @Id field must be of type ObjectId or String", e);
            }
        }

    }

    public Optional<ObjectId> getObjectId() {
        return Optional.ofNullable(objectId);
    }

    public Field getIdField() {
        return idField;
    }

}
