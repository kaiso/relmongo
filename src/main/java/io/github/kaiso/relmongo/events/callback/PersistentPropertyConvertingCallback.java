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

import io.github.kaiso.relmongo.annotation.CascadeType;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.events.processor.MappedByProcessor;
import io.github.kaiso.relmongo.exception.RelMongoConfigurationException;
import io.github.kaiso.relmongo.exception.RelMongoInvalidApiUsageException;
import io.github.kaiso.relmongo.exception.RelMongoProcessingException;
import io.github.kaiso.relmongo.util.AnnotationsUtils;
import io.github.kaiso.relmongo.util.ObjectIdReaderCallback;
import io.github.kaiso.relmongo.util.ReflectionsUtil;

import org.bson.types.ObjectId;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * 
 * @author Kais OMRI
 *
 */
public class PersistentPropertyConvertingCallback implements FieldCallback {

    private Object source;

    public PersistentPropertyConvertingCallback(Object source) {
        super();
        this.source = source;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);

        if (AnnotationsUtils.isMappedBy(field)) {
            ReflectionUtils.setField(field, source, null);
            return;
        }


        if (field.isAnnotationPresent(OneToMany.class)) {
            fillIdentifiers(field, field.getAnnotation(OneToMany.class).cascade());
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            fillIdentifiers(field, field.getAnnotation(OneToOne.class).cascade());
        }
        
        MappedByProcessor.processChild(source, null, field, ReflectionsUtil.getGenericType(field));

    }

    private void fillIdentifiers(Field field, CascadeType cascadeType) throws IllegalAccessException {
        Object reference = field.get(source);
        if (reference == null) {
            return;
        }
        if (Arrays.asList(CascadeType.PERSIST, CascadeType.ALL).contains(cascadeType)) {
            if (Collection.class.isAssignableFrom(reference.getClass())) {
                ((Collection<?>) reference).stream().forEach(this::checkIdentifier);
            } else {
                checkIdentifier(reference);
            }
        }

    }

    private void checkIdentifier(Object obj) {

        try {
            ObjectIdReaderCallback objectIdReaderCallback = new ObjectIdReaderCallback(obj);
            ReflectionUtils.doWithFields(obj.getClass(), objectIdReaderCallback);
            Field idField = objectIdReaderCallback.getIdField();
            if (idField == null) {
                throw new RelMongoConfigurationException("the Id field of class [" + obj.getClass()
                    + "] must be annotated by @Id (org.springframework.data.annotation.Id)");
            }
            if (idField.get(obj) == null) {
                ReflectionUtils.setField(idField, obj, generateId(idField));
            }

        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RelMongoProcessingException(e);
        }

    }

    private Object generateId(Field idField) {
        Object id;
        if (idField.getType().equals(BigInteger.class)) {
            id = new BigInteger(source.toString(), 16);
        } else if (idField.getType().equals(ObjectId.class)) {
            id = ObjectId.get();
        } else if (idField.getType().equals(String.class)) {
            id = ObjectId.get().toString();
        } else if (idField.getType().equals(Long.class)) {
            id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        } else {
            throw new RelMongoInvalidApiUsageException(
                String.format("Cannot autogenerate id of type %s for entity of type %s!", idField.getType(),
                    idField.getDeclaringClass().getName()));
        }
        return id;
    }

}
