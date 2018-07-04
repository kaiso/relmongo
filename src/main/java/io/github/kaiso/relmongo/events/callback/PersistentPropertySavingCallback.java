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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import com.mongodb.BasicDBList;

import io.github.kaiso.relmongo.annotation.CascadeType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;
import io.github.kaiso.relmongo.exception.RelMongoConfigurationException;
import io.github.kaiso.relmongo.exception.RelMongoProcessingException;
import io.github.kaiso.relmongo.util.ReflectionsUtil;
import io.github.kaiso.relmongo.util.RelMongoConstants;

public class PersistentPropertySavingCallback implements FieldCallback {

	private Object source;

	public PersistentPropertySavingCallback(Object source) {
		super();
		this.source = source;
	}

	public void doWith(Field field) throws IllegalAccessException {
		ReflectionUtils.makeAccessible(field);
		if (field.isAnnotationPresent(OneToMany.class)) {
			saveAssociation(field, field.getAnnotation(OneToMany.class).cascade());
		} else if (field.isAnnotationPresent(OneToOne.class)) {
			saveAssociation(field, field.getAnnotation(OneToOne.class).cascade());
		}

	}

	private void saveAssociation(Field field, CascadeType cascadeType) {
		String name = "";
		try {
			name = field.getAnnotation(JoinProperty.class).name();
		} catch (Exception e) {
			throw new RelMongoConfigurationException("Missing or misconfigured @JoinProperty annotation", e);
		}
		Object reference = null;
		reference = ((org.bson.Document) source).get(field.getName());
		String collection = getCollectionName(field);
		if (reference instanceof BasicDBList) {
			BasicDBList list = new BasicDBList();
			list.addAll(((BasicDBList) reference).stream()
					.map(dbObject -> this.keepOnlyIdentifier(dbObject, collection, cascadeType))
					.collect(Collectors.toList()));
			((org.bson.Document) source).remove(field.getName());
			((org.bson.Document) source).put(name, list);
		} else if (reference instanceof org.bson.Document) {
			((org.bson.Document) source).remove(field.getName());
			((org.bson.Document) source).put(name, this.keepOnlyIdentifier(reference, collection, cascadeType));
		}
	}

	private org.bson.Document keepOnlyIdentifier(Object obj, String collection, CascadeType cascadeType) {
		Object objectId = ((org.bson.Document) obj).get("_id");
		if (objectId == null && !Arrays.asList(CascadeType.PERSIST, CascadeType.ALL).contains(cascadeType)) {
			throw new RelMongoProcessingException(
					"ObjectId must not be null when persisting without cascade ALL or PERSIST ");
		}
		return new org.bson.Document().append("_id", objectId).append(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME,
				collection);
	}

	private String getCollectionName(Field field) {
		String collection = ReflectionsUtil.getGenericType(field).getAnnotation(Document.class).collection();
		if (collection == null || "".equals(collection)) {
			collection = ReflectionsUtil.getGenericType(field).getSimpleName().toLowerCase();
		}
		return collection;
	}

}
