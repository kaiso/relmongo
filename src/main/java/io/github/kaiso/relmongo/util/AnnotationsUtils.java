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
package io.github.kaiso.relmongo.util;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.ManyToOne;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.exception.RelMongoConfigurationException;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Map.Entry;

/**
 * 
 * @author Kais OMRI
 *
 */
public class AnnotationsUtils {

    private AnnotationsUtils() {
        super();
    }

    public static String getJoinProperty(Field field) {
        try {
            return field.getAnnotation(JoinProperty.class).name();
        } catch (Exception e) {
            throw new RelMongoConfigurationException("Missing or misconfigured @JoinProperty annotation on Field "
                    + field.getName() + " from Class " + field.getDeclaringClass());
        }
    }

    public static Entry<FetchType, String> getMappedByAndFetchType(Field field) {
        FetchType fetchType = null;
        String mappedBy = null;
        if (field.isAnnotationPresent(OneToMany.class)) {
            fetchType = field.getAnnotation(OneToMany.class).fetch();
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            fetchType = field.getAnnotation(OneToOne.class).fetch();
            mappedBy = field.getAnnotation(OneToOne.class).mappedBy();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            mappedBy = field.getAnnotation(ManyToOne.class).mappedBy();
            fetchType = field.getAnnotation(ManyToOne.class).fetch();
        }
        return new AbstractMap.SimpleEntry<>(fetchType, mappedBy);
    }
}
