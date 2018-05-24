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

import com.mongodb.BasicDBObject;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.model.LoadableObjectsMetadata;
import io.github.kaiso.relmongo.util.ReflectionsUtil;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PersistentPropertyLoadingCallback implements FieldCallback {

    private List<LoadableObjectsMetadata> loadableObjects = new ArrayList<>();
    private Object source;

    public PersistentPropertyLoadingCallback(Object source) {
        super();
        this.source = source;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);
        FetchType fetchType = null;
        if (field.isAnnotationPresent(OneToMany.class)) {
            fetchType = field.getAnnotation(OneToMany.class).fetch();
            loadAssociation(field, fetchType);
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            fetchType = field.getAnnotation(OneToOne.class).fetch();
            loadAssociation(field, fetchType);
        }

    }

    private void loadAssociation(Field field, FetchType fetchType) {
        String name = "";
        try {
            name = field.getAnnotation(JoinProperty.class).name();
        } catch (Exception e) {
            throw new IllegalArgumentException("Missing or misconfigured @JoinProperty annotation", e);
        }
        Object ids = null;
        try {
            ids = ((BasicDBObject) source).get(name);
        } catch (Exception e) {
            throw new IllegalArgumentException("Property defined in @JoinProperty annotation is not present", e);
        }

        loadableObjects.add(new LoadableObjectsMetadata(field.getName(), name, "_id", ReflectionsUtil.getGenericType(field), fetchType, ids));
    }

    public List<LoadableObjectsMetadata> getLoadableObjects() {
        return loadableObjects;
    }

}
