/**
*   Copyright 2018 Kais OMRI.
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
package io.github.kaiso.relmongo.events.processor;

import io.github.kaiso.relmongo.exception.RelMongoProcessingException;
import io.github.kaiso.relmongo.model.MappedByMetadata;
import io.github.kaiso.relmongo.util.AnnotationsUtils;
import io.github.kaiso.relmongo.util.ReflectionsUtil;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * 
 * @author Kais OMRI
 *
 */
public final class MappedByProcessor {

    private MappedByProcessor() {
        super();
    }

    public static void processChild(Object parent, Object value, Field targetField, Class<?> fieldType) {
        ReflectionUtils.makeAccessible(targetField);
        ReflectionUtils.doWithFields(fieldType, new FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalAccessException {
                ReflectionUtils.makeAccessible(field);
                if (!ReflectionsUtil.getGenericType(field).equals(parent.getClass())) {
                    return;
                }

                MappedByMetadata mappedByInfos = AnnotationsUtils.getMappedByInfos(field);

                Object target = targetField.get(parent);
                if (mappedByInfos.getMappedByValue() != null && mappedByInfos.getMappedByValue().equals(targetField.getName())
                    && target != null) {
                    if (Collection.class.isAssignableFrom(targetField.getType())) {
                        ((Collection<?>) target).forEach(element -> {
                            try {
                                ReflectionUtils.setField(field, element, value);
                            } catch (IllegalArgumentException e) {
                                throw new RelMongoProcessingException("unable to set mappedBy child object "
                                    + e.getMessage());
                            }
                        });
                    } else {
                        ReflectionUtils.setField(field, target, value);
                    }
                }
            }
        });
    }

}
