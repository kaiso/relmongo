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

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.OneToMany;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.Collection;

public class PersistentPropertyLazyLoadingCallback implements FieldCallback {

    private Object source;
    private MongoOperations mongoOperations;

    public PersistentPropertyLazyLoadingCallback(Object source, MongoOperations mongoOperations) {
        super();
        this.source = source;
        this.mongoOperations = mongoOperations;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);

        if (field.isAnnotationPresent(OneToMany.class)&& FetchType.LAZY.equals(field.getAnnotation(OneToMany.class).fetch())) {
            String referencedPropertyName = "";
            try {
                referencedPropertyName = field.getAnnotation(JoinProperty.class).referencedPropertyName();
            } catch (Exception e) {
                throw new IllegalArgumentException("Missing or misconfigured @JoinProperty annotation", e);
            }
            if (!"_id".equals(referencedPropertyName)) {
                throw new IllegalArgumentException("in @OneToMany, referencedPropertyName must be allways _id ");
            }
            if (!Collection.class.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException("in @OneToMany, the field must be of type collection "); 
            }
            
          field.set(source, PersistentRelationResolver.lazyLoader(field.getType(), field.get(source), mongoOperations));

        }

    }

}
