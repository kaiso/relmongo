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

package io.github.kaiso.relmongo.events.processor;

import io.github.kaiso.relmongo.events.callback.PersistentPropertyCascadingRemoveCallback;
import io.github.kaiso.relmongo.events.callback.PersistentPropertyConvertingCallback;
import io.github.kaiso.relmongo.events.callback.PersistentPropertyPostLoadingCallback;
import io.github.kaiso.relmongo.events.callback.PersistentPropertyPostSavingCallback;
import io.github.kaiso.relmongo.events.callback.PersistentPropertySavingCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.MongoMappingEvent;
import org.springframework.util.ReflectionUtils;

/**
 * 
 * @author Kais OMRI
 * @see AbstractMongoEventListener
 *
 */
public class RelMongoProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RelMongoProcessor.class);

    @SuppressWarnings("unchecked")
    public void onApplicationEvent(MongoMappingEvent<?> event, MongoTemplate mongoTemplate) {
        try {

            if (event instanceof AfterLoadEvent) {

                onAfterLoad((AfterLoadEvent<Object>) event, mongoTemplate);

                return;
            }

            if (event instanceof AbstractDeleteEvent) {

                Class<?> eventDomainType = ((AbstractDeleteEvent<?>) event).getType();

                if (eventDomainType != null) {
                    if (event instanceof BeforeDeleteEvent) {
                        onBeforeDelete((BeforeDeleteEvent<Object>) event, mongoTemplate);
                    }
                    /*
                     * if (event instanceof AfterDeleteEvent) {
                     * onAfterDelete((AfterDeleteEvent<Object>) event);
                     * }
                     */
                }

                return;

            }

            if (event instanceof BeforeConvertEvent) {
                onBeforeConvert((BeforeConvertEvent<Object>) event, mongoTemplate);
            } else if (event instanceof BeforeSaveEvent) {
                onBeforeSave((BeforeSaveEvent<Object>) event, mongoTemplate);
            } else if (event instanceof AfterSaveEvent) {
                onAfterSave((AfterSaveEvent<Object>) event, mongoTemplate);
            } else if (event instanceof AfterConvertEvent) {
                onAfterConvert((AfterConvertEvent<Object>) event, mongoTemplate);
            }

        } catch (Exception e) {
            logger.warn("Failed to process MappingEvent " + event.getClass().getSimpleName(), e);
        }
    }

    public void onAfterLoad(AfterLoadEvent<Object> event, MongoTemplate template) {
        // if (event.getType().isAnnotationPresent(Document.class)) {
        // PersistentPropertyLoadingCallback callback = new
        // PersistentPropertyLoadingCallback(event.getSource());
        // ReflectionUtils.doWithFields(event.getType(), callback);
        // List<LoadableObjectsMetadata> loadableObjects =
        // callback.getLoadableObjects();
        // if (!loadableObjects.isEmpty()) {
        // PersistentRelationResolver.resolveOnLoading(mongoOperations, loadableObjects,
        // event.getSource());
        // }
        // }
    }

    public void onBeforeSave(BeforeSaveEvent<Object> event, MongoTemplate mongoTemplate) {
        if (event.getSource().getClass().isAnnotationPresent(Document.class)) {
            PersistentPropertySavingCallback callback = new PersistentPropertySavingCallback(event.getDocument(), event.getCollectionName(), mongoTemplate);
            ReflectionUtils.doWithFields(event.getSource().getClass(), callback);
        }
    }

    public void onBeforeConvert(BeforeConvertEvent<Object> event, MongoTemplate mongoTemplate) {
        if (event.getSource().getClass().isAnnotationPresent(Document.class)) {
            PersistentPropertyConvertingCallback callback = new PersistentPropertyConvertingCallback(event.getSource());
            ReflectionUtils.doWithFields(event.getSource().getClass(), callback);
        }
    }

    public void onAfterConvert(AfterConvertEvent<Object> event, MongoTemplate mongoTemplate) {
        if (event.getSource().getClass().isAnnotationPresent(Document.class)) {
            PersistentPropertyPostLoadingCallback callback = new PersistentPropertyPostLoadingCallback(event.getSource(), event.getDocument(), mongoTemplate);
            ReflectionUtils.doWithFields(event.getSource().getClass(), callback);
        }
    }

    public void onAfterSave(AfterSaveEvent<Object> event, MongoTemplate mongoTemplate) {
        if (event.getSource().getClass().isAnnotationPresent(Document.class)) {
            new PersistentPropertyPostSavingCallback(event.getSource(), event.getSource().getClass(), mongoTemplate)
                .apply();

        }

    }

    public void onBeforeDelete(BeforeDeleteEvent<Object> event, MongoTemplate mongoTemplate) {
        if (event.getType().isAnnotationPresent(Document.class) && !event.getSource().isEmpty()) {
            PersistentPropertyCascadingRemoveCallback callback = new PersistentPropertyCascadingRemoveCallback(event.getDocument(), mongoTemplate,
                event.getType(), event.getCollectionName());
            callback.doProcessing();
        }
    }

}
