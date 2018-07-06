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

package io.github.kaiso.relmongo.mongo;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.lazy.LazyLoadingProxy;
import io.github.kaiso.relmongo.lazy.RelMongoLazyLoader;
import io.github.kaiso.relmongo.model.LoadableObjectsMetadata;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.objenesis.ObjenesisStd;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class PersistentRelationResolver {

    private static ObjenesisStd objenesisStd = new ObjenesisStd(true);

    private PersistentRelationResolver() {
        super();
    }

    public static void resolveOnLoading(MongoOperations mongoOperations, List<LoadableObjectsMetadata> loadableObjects, org.bson.Document document) {
        for (LoadableObjectsMetadata relation : loadableObjects) {
            String collection = relation.getTargetAssociationClass().getAnnotation(Document.class).collection();
            if (collection == null || "".equals(collection)) {
                collection = relation.getTargetAssociationClass().getSimpleName().toLowerCase();
            }
            if (FetchType.EAGER.equals(relation.getFetchType())) {
                if (relation.getObjectIds() instanceof Collection) {
                    List<Object> identifierList = ((Collection<?>) relation.getObjectIds()).stream().map(DocumentUtils::mapIdentifier)
                            .collect(Collectors.toList());
                    document.put(relation.getFieldName(), DatabaseOperations.getDocumentsById(mongoOperations, identifierList, collection));
                } else if (relation.getObjectIds() instanceof org.bson.Document) {
                    document.put(relation.getFieldName(), DatabaseOperations.getDocumentByPropertyValue(mongoOperations,
                            DocumentUtils.mapIdentifier(relation.getObjectIds()), relation.getReferencedPropertyName(), collection));
                }
            }
        }

    }

    public static Object lazyLoader(Class<?> type, MongoOperations mongoOperations, List<Object> ids, Class<?> targetClass, Object original) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        RelMongoLazyLoader lazyLoader = new RelMongoLazyLoader(ids, mongoOperations, targetClass, type, original);
        enhancer.setCallback(lazyLoader);
        enhancer.setInterfaces(new Class[] { LazyLoadingProxy.class, type.isInterface() ? type : NoOp.class });
        enhancer.create();
        @SuppressWarnings("unchecked")
        Factory factory = (Factory) objenesisStd.newInstance(enhancer.createClass());
        return factory.newInstance(lazyLoader);
    }

}
