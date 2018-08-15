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

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.exception.RelMongoProcessingException;
import io.github.kaiso.relmongo.util.AnnotationsUtils;
import io.github.kaiso.relmongo.util.ReflectionsUtil;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map.Entry;

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
        ReflectionUtils.doWithFields(fieldType, new FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                ReflectionUtils.makeAccessible(field);
                if (!ReflectionsUtil.getGenericType(field).equals(parent.getClass())) {
                    return;
                }
                
                Entry<FetchType, String> result = AnnotationsUtils.getMappedByAndFetchType(field);
                
                if (!StringUtils.isEmpty(result.getValue()) && result.getValue().equals(targetField.getName())
                        && targetField.get(parent) != null) {
                    if (Collection.class.isAssignableFrom(targetField.getType())) {
                        ((Collection<?>) targetField.get(parent)).forEach(element -> {
                            try {
                                field.set(element, value);
                            } catch (IllegalArgumentException | IllegalAccessException e) {
                                throw new RelMongoProcessingException("unable to set mappedBy child object "
                                        + e.getMessage());
                            }
                        });

                    } else {
                        field.set(targetField.get(parent), value);
                    }
                }
            }
        });
    }

}
