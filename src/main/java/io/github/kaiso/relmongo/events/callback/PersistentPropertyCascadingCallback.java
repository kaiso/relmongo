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
import io.github.kaiso.relmongo.mongo.Operation;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

public class PersistentPropertyCascadingCallback implements FieldCallback {

	private Object source;
	private MongoOperations mongoOperations;
	private Operation operation;

	public PersistentPropertyCascadingCallback(Object source, MongoOperations mongoOperations, Operation operation) {
		super();
		this.source = source;
		this.mongoOperations = mongoOperations;
		this.operation = operation;
	}

	public void doWith(Field field) throws IllegalAccessException {
		ReflectionUtils.makeAccessible(field);
		if (field.isAnnotationPresent(OneToMany.class)) {
			doCascade(field, field.getAnnotation(OneToMany.class).cascade(), OneToMany.class);
		} else if (field.isAnnotationPresent(OneToOne.class)) {
			doCascade(field, field.getAnnotation(OneToOne.class).cascade(), OneToOne.class);
		}

	}

	private void doCascade(Field field, CascadeType cascadeType, Class<?> annotation) throws IllegalAccessException {
		Object child = field.get(source);
		if (child != null) {
			if (Collection.class.isAssignableFrom(child.getClass())) {
				if (Arrays.asList(CascadeType.PERSIST, CascadeType.ALL).contains(cascadeType)
						&& Operation.PERSIST.equals(operation)) {
					((Collection<?>) child).parallelStream().forEach(mongoOperations::save);
				}
			} else {
				if (Arrays.asList(CascadeType.PERSIST, CascadeType.ALL).contains(cascadeType)
						&& Operation.PERSIST.equals(operation)) {
					mongoOperations.save(child);
				}
			}
		}

	}

}
