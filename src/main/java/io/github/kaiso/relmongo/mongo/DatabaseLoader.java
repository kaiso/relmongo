package io.github.kaiso.relmongo.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class DatabaseLoader {

    private DatabaseLoader() {
        super();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static BasicDBList getDocumentsById(MongoOperations mongoOperations, List<Object> ids, String collection) {
        Assert.notNull(ids, "Ids must not be null!");
        Assert.hasText(collection, "Collection must not be null or empty!");
        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$in", ids));
        List documents = mongoOperations.getCollection(collection).find(query).toArray();
        BasicDBList list = new BasicDBList();
        list.addAll(documents);
        return list;
    }

    public static DBObject getDocumentByPropertyValue(MongoOperations mongoOperations, Object objectId, String propertyName, String collection) {
        Assert.notNull(objectId, "objectId must not be null!");
        Assert.hasText(collection, "Collection must not be null or empty!");
        BasicDBObject query = new BasicDBObject(propertyName, new BasicDBObject("$eq", objectId));
        return mongoOperations.getCollection(collection).findOne(query);
    }

    public static <T> Collection<T> findByIds(MongoOperations mongoOperations, Class<T> clazz, ObjectId... id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(Arrays.asList(id)));
        return mongoOperations.find(query, clazz);
    }

    public static <T> T findByPropertyValue(MongoOperations mongoOperations, Class<T> clazz, String propertyName, ObjectId value) {
        Query query = new Query();
        query.addCriteria(Criteria.where(propertyName).is(value));
        return mongoOperations.findOne(query, clazz);
    }

}
