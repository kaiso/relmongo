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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.ManyToOne;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.exception.RelMongoConfigurationException;
import io.github.kaiso.relmongo.model.MappedByMetadata;

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

	public static FetchType getFetchType(Field field) {
		FetchType fetchType = null;
		if (field.isAnnotationPresent(OneToMany.class)) {
			fetchType = field.getAnnotation(OneToMany.class).fetch();
		} else if (field.isAnnotationPresent(OneToOne.class)) {
			fetchType = field.getAnnotation(OneToOne.class).fetch();
		} else if (field.isAnnotationPresent(ManyToOne.class)) {
			fetchType = field.getAnnotation(ManyToOne.class).fetch();
		}
		return fetchType;
	}

	public static Boolean isMappedBy(Field field) {
		MappedByMetadata data = new MappedByMetadata();
		if (field.isAnnotationPresent(OneToOne.class)) {
			data.setMappedByValue(field.getAnnotation(OneToOne.class).mappedBy());
		} else if (field.isAnnotationPresent(ManyToOne.class)) {
			data.setMappedByValue(field.getAnnotation(ManyToOne.class).mappedBy());
		}
		return (data.getMappedByValue() != null);
	}

	public static MappedByMetadata getMappedByInfos(Field field) {
		MappedByMetadata data = new MappedByMetadata();
		Class<? extends Annotation> targetAnnotation = null;
		if (field.isAnnotationPresent(OneToOne.class)) {
			data.setMappedByValue(field.getAnnotation(OneToOne.class).mappedBy());
			if (data.getMappedByValue() != null) {
				targetAnnotation = OneToOne.class;
			}
		} else if (field.isAnnotationPresent(ManyToOne.class)) {
			data.setMappedByValue(field.getAnnotation(ManyToOne.class).mappedBy());
			if (data.getMappedByValue() != null) {
				targetAnnotation = OneToMany.class;
			}
		}

		if (targetAnnotation == null) {
			return data;
		}

		if (field.isAnnotationPresent(JoinProperty.class)) {
			throw new RelMongoConfigurationException("can not use mappedBy and @JoinProperty on the same field "
					+ field.getName() + " of class " + field.getDeclaringClass().getName());
		}

		Field targetProp;
		Class<?> type = ReflectionsUtil.getGenericType(field);
		try {
			targetProp = type.getDeclaredField(data.getMappedByValue());
			ReflectionUtils.makeAccessible(targetProp);
			if (!targetProp.isAnnotationPresent(targetAnnotation)) {
				throw new RelMongoConfigurationException("misconfigured bidirectional mapping, the field \""
						+ field.getName() + "\" declared in " + field.getDeclaringClass().getName()
						+ " declares a mappedBy property linked to  the field \"" + data.getMappedByValue()
						+ "\"  of class " + type.getName()
						+ " so you must use compatible RelMongo annotations on the these fields");
			}

			data.setMappedByJoinProperty(getJoinProperty(targetProp));

		} catch (NoSuchFieldException | SecurityException ex) {
			throw new RelMongoConfigurationException("unable to find field with name " + data.getMappedByValue()
					+ " in the type " + type.getCanonicalName(), ex);
		}

		return data;
	}
}
