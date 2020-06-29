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

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.util.ReflectionUtils;

/**
 * 
 * @author Kais OMRI
 *
 */
public class RelMongoProcessor extends AbstractMongoEventListener<Object> {

    private MongoOperations mongoOperations;

    public RelMongoProcessor(MongoOperations mongoOperations) {
        super();
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterLoad(AfterLoadEvent<Object> event) {
//        if (event.getType().isAnnotationPresent(Document.class)) {
//            PersistentPropertyLoadingCallback callback = new PersistentPropertyLoadingCallback(event.getSource());
//            ReflectionUtils.doWithFields(event.getType(), callback);
//            List<LoadableObjectsMetadata> loadableObjects = callback.getLoadableObjects();
//            if (!loadableObjects.isEmpty()) {
//                PersistentRelationResolver.resolveOnLoading(mongoOperations, loadableObjects, event.getSource());
//            }
//        }
        super.onAfterLoad(event);
    }

    @Override
    public void onBeforeSave(BeforeSaveEvent<Object> event) {
        if (event.getSource().getClass().isAnnotationPresent(Document.class)) {
            PersistentPropertySavingCallback callback = new PersistentPropertySavingCallback(event.getDocument(), event.getCollectionName(), mongoOperations);
            ReflectionUtils.doWithFields(event.getSource().getClass(), callback);
        }
        super.onBeforeSave(event);
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        if (event.getSource().getClass().isAnnotationPresent(Document.class)) {
            PersistentPropertyConvertingCallback callback = new PersistentPropertyConvertingCallback(event.getSource());
            ReflectionUtils.doWithFields(event.getSource().getClass(), callback);
        }
        super.onBeforeConvert(event);
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Object> event) {
        if (event.getSource().getClass().isAnnotationPresent(Document.class)) {
            PersistentPropertyPostLoadingCallback callback = new PersistentPropertyPostLoadingCallback(event.getSource(), event.getDocument(), mongoOperations);
            ReflectionUtils.doWithFields(event.getSource().getClass(), callback);
        }
        super.onAfterConvert(event);
    }

    @Override
    public void onAfterSave(AfterSaveEvent<Object> event) {
        super.onAfterSave(event);
        if (event.getSource().getClass().isAnnotationPresent(Document.class)) {
            new PersistentPropertyPostSavingCallback(event.getSource(), event.getSource().getClass(), mongoOperations)
                .apply();

        }

    }

    @Override
    public void onBeforeDelete(BeforeDeleteEvent<Object> event) {
        super.onBeforeDelete(event);
        if (event.getType().isAnnotationPresent(Document.class) && !event.getSource().isEmpty()) {
            PersistentPropertyCascadingRemoveCallback callback = new PersistentPropertyCascadingRemoveCallback(event.getDocument(), mongoOperations,
                event.getType(), event.getCollectionName());
            callback.doProcessing();
        }
    }

}
