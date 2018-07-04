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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.lazy.LazyLoadingProxy;
import io.github.kaiso.relmongo.lazy.RelMongoLazyLoader;
import io.github.kaiso.relmongo.model.LoadableObjectsMetadata;
import io.github.kaiso.relmongo.util.RelMongoConstants;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.objenesis.ObjenesisStd;

import java.util.List;
import java.util.stream.Collectors;

public final class PersistentRelationResolver {

    private static ObjenesisStd objenesisStd = new ObjenesisStd(true);

    private PersistentRelationResolver() {
        super();
    }

    public static void resolveOnLoading(MongoOperations mongoOperations, List<LoadableObjectsMetadata> loadableObjects, DBObject source) {
        for (LoadableObjectsMetadata relation : loadableObjects) {
            String collection = relation.getTargetAssociationClass().getAnnotation(Document.class).collection();
            if (collection == null || "".equals(collection)) {
                collection = relation.getTargetAssociationClass().getSimpleName().toLowerCase();
            }
            if (relation.getObjectIds() instanceof BasicDBList && hasToLoad((BasicDBList) relation.getObjectIds())) {
                if (FetchType.EAGER.equals(relation.getFetchType())) {
                    List<Object> identifierList = ((BasicDBList) relation.getObjectIds()).stream().map(PersistentRelationResolver::mapIdentifier)
                            .collect(Collectors.toList());

                    source.put(relation.getFieldName(), DatabaseOperations.getDocumentsById(mongoOperations, identifierList, collection));
                } else {
                    source.put(relation.getFieldName(), relation.getObjectIds());
                }
            } else if (relation.getObjectIds() instanceof BasicDBObject && hasToLoad((BasicDBObject) relation.getObjectIds())) {
                if (FetchType.EAGER.equals(relation.getFetchType())) {
                    source.put(relation.getFieldName(), DatabaseOperations.getDocumentByPropertyValue(mongoOperations, mapIdentifier(relation.getObjectIds()),
                            relation.getReferencedPropertyName(), collection));
                } else {
                    source.put(relation.getFieldName(), relation.getObjectIds());
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

    private static boolean hasToLoad(BasicDBList objects) {
        // the last contition verifies if the objects were already laoded by another
        // query
        if (objects.isEmpty())
            return false;
        BasicDBObject basicDBObject = (BasicDBObject) objects.get(0);
        return hasToLoad(basicDBObject);
    }

    /**
     * checks if an object is already populated to be loaded anther time or not
     * 
     * @param basicDBObject
     * @return
     */
    private static boolean hasToLoad(BasicDBObject basicDBObject) {
        int propscount = basicDBObject.get(RelMongoConstants.RELMONGOTARGET_PROPERTY_NAME) == null ? 1 : 2;
        return basicDBObject.size() <= propscount;
    }

    public static Object mapIdentifier(Object object) {
        return ((BasicDBObject) object).getObjectId("_id");
    }

}
