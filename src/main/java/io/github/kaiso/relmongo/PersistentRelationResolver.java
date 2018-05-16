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

package io.github.kaiso.relmongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.lazy.LazyLoadingCallback;
import io.github.kaiso.relmongo.lazy.LazyLoadingProxy;
import io.github.kaiso.relmongo.mongo.DatabaseLoader;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.objenesis.ObjenesisStd;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public final class PersistentRelationResolver {

    private static ObjenesisStd objenesisStd = new ObjenesisStd(true);

    private PersistentRelationResolver() {
        super();
    }

    public static void resolveOnLoading(MongoOperations mongoOperations,
            Map<Entry<Class<?>, FetchType>, Entry<Object, String>> loadableObjects, DBObject source) {
        for (Entry<Entry<Class<?>, FetchType>, Entry<Object, String>> relation : loadableObjects.entrySet()) {
            Object ids = relation.getValue().getKey();
            if (ids instanceof BasicDBList && hasToLoad((BasicDBList) ids, relation.getKey().getValue())) {
                List<Object> identifierList = ((BasicDBList) ids).stream()
                        .map(PersistentRelationResolver::mapIdentifier).collect(Collectors.toList());
                String collection = relation.getKey().getKey().getAnnotation(Document.class).collection();
                source.put(relation.getValue().getValue(),
                        DatabaseLoader.getDocumentsById(mongoOperations, identifierList, collection));
            }
        }

    }

    public static Object lazyLoader(Class<?> type, Object original, MongoOperations mongoOperations) {
        if (original == null)
            return null;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(original.getClass());
        LazyLoadingCallback lazyLoader = new LazyLoadingCallback(original, mongoOperations);
        enhancer.setCallback(lazyLoader);
        enhancer.setInterfaces(new Class[] { LazyLoadingProxy.class, type.isInterface() ? type : NoOp.class });
        enhancer.create();
        @SuppressWarnings("unchecked")
        Factory factory = (Factory) objenesisStd.newInstance(enhancer.createClass());
        return factory.newInstance(lazyLoader);
    }

    private static boolean hasToLoad(BasicDBList objects, FetchType fetchType) {
        // the last contition verifies if the objects were already laoded by another
        // query
        return FetchType.EAGER.equals(fetchType) && !objects.isEmpty() && ((BasicDBObject) objects.get(0)).size() <= 1;
    }

    public static Object mapIdentifier(Object object) {
        return ((BasicDBObject) object).getObjectId("_id");
    }

}
