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

package io.github.kaiso.relmongo.events.callback;

import io.github.kaiso.relmongo.annotation.CascadeType;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Kais OMRI
 *
 */
public class PersistentPropertyPostSavingCallback implements FieldCallback {

    private Object source;
    private MongoOperations mongoOperations;
    private Class<?> sourceClass;
    private Set<Object> cache = new HashSet<>();

    public PersistentPropertyPostSavingCallback(Object source, Class<?> sourceClass, MongoOperations mongoOperations) {
        super();
        this.source = source;
        this.mongoOperations = mongoOperations;
        this.sourceClass = sourceClass;
    }

    public void apply() {
        ReflectionUtils.doWithFields(sourceClass, this);
        cache.stream().forEach(mongoOperations::save);
        cache.clear();
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);
        if (field.isAnnotationPresent(OneToMany.class)) {
            doProcessing(field, field.getAnnotation(OneToMany.class).cascade(), field.getAnnotation(OneToMany.class).orphanRemoval());
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            doProcessing(field, field.getAnnotation(OneToOne.class).cascade(), field.getAnnotation(OneToOne.class).orphanRemoval());
        }

    }

    private void doProcessing(Field field, CascadeType cascadeType, boolean orphanRemoval) throws IllegalAccessException {
        Object child = field.get(source);
        if (child != null) {
            if (Collection.class.isAssignableFrom(child.getClass())) {
                cascadeCollection(cascadeType, (Collection<?>) child);
                removeOrphans(orphanRemoval, (Collection<?>) child);
            } else {
                cascadeItem(cascadeType, child);
                removeOrphans(orphanRemoval, child);

            }
        }

    }

    private void cascadeItem(CascadeType cascadeType, Object child) {
        if (Arrays.asList(CascadeType.PERSIST, CascadeType.ALL).contains(cascadeType)) {
            cache.add(child);
        }
    }

    private void cascadeCollection(CascadeType cascadeType, Collection<?> child) {
        if (Arrays.asList(CascadeType.PERSIST, CascadeType.ALL).contains(cascadeType)) {
            cache.addAll(child);
        }
    }

    private void removeOrphans(Boolean orphanRemoval, Object child) {
        if (Boolean.TRUE.equals(orphanRemoval)) {

        }
    }

    private void removeOrphans(Boolean orphanRemoval, Collection<?> child) {
        if (Boolean.TRUE.equals(orphanRemoval)) {

        }
    }

}
