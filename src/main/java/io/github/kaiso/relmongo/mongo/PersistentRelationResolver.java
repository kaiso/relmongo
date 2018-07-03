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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.objenesis.ObjenesisStd;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.lazy.LazyLoadingProxy;
import io.github.kaiso.relmongo.lazy.RelMongoLazyLoader;
import io.github.kaiso.relmongo.model.LoadableObjectsMetadata;
import io.github.kaiso.relmongo.util.RelMongoConstants;

public final class PersistentRelationResolver {

    private static ObjenesisStd objenesisStd = new ObjenesisStd(true);

    private PersistentRelationResolver() {
        super();
    }

    public static void resolveOnLoading(MongoOperations mongoOperations, List<LoadableObjectsMetadata> loadableObjects, org.bson.Document document) {
        for (LoadableObjectsMetadata relation : loadableObjects) {
            String collection = relation.getTargetAssociationClass().getAnnotation(Document.class).collection();
            if(collection == null || "".equals(collection)) {
            	   collection = relation.getTargetAssociationClass().getSimpleName().toLowerCase();
            }
            if (relation.getObjectIds() instanceof Collection && hasToLoad((Collection<?>) relation.getObjectIds())) {
                if (FetchType.EAGER.equals(relation.getFetchType())) {
                    List<Object> identifierList = ((Collection<?>) relation.getObjectIds()).stream().map(PersistentRelationResolver::mapIdentifier)
                            .collect(Collectors.toList());
                    document.put(relation.getFieldName(), DatabaseOperations.getDocumentsById(mongoOperations, identifierList, collection));
                } else {
                    document.put(relation.getFieldName(), relation.getObjectIds());
                }
            } else if (relation.getObjectIds() instanceof org.bson.Document && hasToLoad((org.bson.Document) relation.getObjectIds())) {
                if (FetchType.EAGER.equals(relation.getFetchType())) {
                    document.put(relation.getFieldName(), DatabaseOperations.getDocumentByPropertyValue(mongoOperations, mapIdentifier(relation.getObjectIds()),
                            relation.getReferencedPropertyName(), collection));
                } else {
                    document.put(relation.getFieldName(), relation.getObjectIds());
                }
            }
        }

    }

    public static Object lazyLoader(Class<?> type, Object original, MongoOperations mongoOperations) {
        if (original == null)
            return null;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(original.getClass());
        RelMongoLazyLoader lazyLoader = new RelMongoLazyLoader(original, mongoOperations);
        enhancer.setCallback(lazyLoader);
        enhancer.setInterfaces(new Class[] { LazyLoadingProxy.class, type.isInterface() ? type : NoOp.class });
        enhancer.create();
        @SuppressWarnings("unchecked")
        Factory factory = (Factory) objenesisStd.newInstance(enhancer.createClass());
        return factory.newInstance(lazyLoader);
    }

    private static boolean hasToLoad(Collection<?> objects) {
        // the last contition verifies if the objects were already laoded by another
        // query
        if (objects.isEmpty())
            return false;
        org.bson.Document document = (org.bson.Document) objects.iterator().next();
        return hasToLoad(document);
    }

    /**
     * checks if an object is already populated to be loaded anther time or not
     * 
     * @param document
     * @return
     */
    private static boolean hasToLoad(org.bson.Document document) {
        int propscount = document.get(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME) == null ? 1 : 2;
        return document.size() <= propscount;
    }

    public static Object mapIdentifier(Object object) {
        return ((org.bson.Document) object).getObjectId("_id");
    }

}
