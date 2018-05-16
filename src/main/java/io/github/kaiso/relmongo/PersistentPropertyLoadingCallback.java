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

package io.github.kaiso.relmongo;

import com.mongodb.BasicDBObject;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.util.ReflectionsUtil;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PersistentPropertyLoadingCallback implements FieldCallback {

    private Map<Entry<Class<?>, FetchType>, Entry<Object, String>> loadableObjects = new HashMap<>();
    private Object source;

    public PersistentPropertyLoadingCallback(Object source) {
        super();
        this.source = source;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);

        if (field.isAnnotationPresent(OneToMany.class)) {
            String name = "";
            String referencedPropertyName = "";
            try {
                name = field.getAnnotation(JoinProperty.class).name();
                referencedPropertyName = field.getAnnotation(JoinProperty.class).referencedPropertyName();
            } catch (Exception e) {
                throw new IllegalArgumentException("Missing or misconfigured @JoinProperty annotation", e);
            }
            if (!"_id".equals(referencedPropertyName)) {
                throw new IllegalArgumentException("in @OneToMany, referencedPropertyName must be allways _id ");
            }
            Object ids = null;
            try {
                ids = ((BasicDBObject) source).get(name);
            } catch (Exception e) {
                throw new IllegalArgumentException("Property defined in @JoinProperty annotation is not present", e);
            }

            loadableObjects.put(
                    new AbstractMap.SimpleImmutableEntry<Class<?>, FetchType>(ReflectionsUtil.getGenericType(field),
                            field.getAnnotation(OneToMany.class).fetch()),
                    new AbstractMap.SimpleImmutableEntry<Object, String>(ids, name));

        }

    }

    public Map<Entry<Class<?>, FetchType>, Entry<Object, String>> getLoadableObjects() {
        return loadableObjects;
    }

}
