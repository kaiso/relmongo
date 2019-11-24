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

package io.github.kaiso.relmongo.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Kais OMRI
 *
 */
public final class DatabaseOperations {

    private DatabaseOperations() {
        super();
    }

    @SuppressWarnings({ "rawtypes" })
    public static BasicDBList getDocumentsById(MongoOperations mongoOperations, List<Object> ids, String collection) {
        Assert.notNull(ids, "Ids must not be null!");
        Assert.hasText(collection, "Collection must not be null or empty!");
        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$in", ids));
        FindIterable<Document> result = mongoOperations.getCollection(collection).find(query);
        BasicDBList list = new BasicDBList();
        for (Iterator iterator = result.iterator(); iterator.hasNext();) {
            list.add(iterator.next());

        }
        return list;
    }

    public static Document getDocumentByPropertyValue(MongoOperations mongoOperations, Object objectId, String propertyName, String collection) {
        Assert.notNull(objectId, "objectId must not be null!");
        Assert.hasText(collection, "Collection must not be null or empty!");
        BasicDBObject query = new BasicDBObject(propertyName, new BasicDBObject("$eq", objectId));
        FindIterable<Document> result = mongoOperations.getCollection(collection).find(query);
        return result.iterator().hasNext() ? result.iterator().next() : null;
    }

    public static <T> Collection<T> findByIds(MongoOperations mongoOperations, Class<T> clazz, ObjectId... id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(Arrays.asList(id)));
        return mongoOperations.find(query, clazz);
    }

    public static <T> T findByPropertyValue(MongoOperations mongoOperations, Class<T> clazz, String propertyName, Object value) {
        BasicDBObject query = new BasicDBObject(propertyName, new BasicDBObject("$eq", value));
        FindIterable<Document> result = mongoOperations.getCollection(mongoOperations.getCollectionName(clazz)).find(query).limit(1);
        return result.iterator().hasNext() ? mongoOperations.getConverter().read(clazz,result.iterator().next()) : null;
    }

    public static void saveObjects(MongoOperations mongoOperations, Object obj) {
        mongoOperations.save(obj);
    }

    public static void removeObjectsByIds(MongoOperations mongoOperations, String collectionName, List<ObjectId> objectsIds) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(objectsIds));
        mongoOperations.remove(query, collectionName);
    }

}
