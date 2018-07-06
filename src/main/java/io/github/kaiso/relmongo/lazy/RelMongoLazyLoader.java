package io.github.kaiso.relmongo.lazy;

import io.github.kaiso.relmongo.mongo.DatabaseOperations;

import org.bson.types.ObjectId;
import org.springframework.cglib.proxy.LazyLoader;
import org.springframework.data.mongodb.core.MongoOperations;

import java.util.Collection;
import java.util.List;

public class RelMongoLazyLoader implements LazyLoader {

    private List<Object> ids;
    private MongoOperations mongoOperations;
    private Class<?> targetClass;
    private Class<?> fieldType;
    private Object original;

    public RelMongoLazyLoader(List<Object> ids, MongoOperations mongoOperations, Class<?> targetClass, Class<?> fieldType, Object original) {
        super();
        this.ids = ids;
        this.mongoOperations = mongoOperations;
        this.targetClass = targetClass;
        this.fieldType = fieldType;
        this.original = original;
    }

    @Override
    public Object loadObject() throws Exception {
        Object result = null;
        if (!(original instanceof LazyLoadingProxy) && !ids.isEmpty()) {
            if (Collection.class.isAssignableFrom(fieldType)) {
                result = DatabaseOperations.findByIds(mongoOperations, targetClass, ids.toArray(new ObjectId[ids.size()]));
            } else {
                result = DatabaseOperations.findByPropertyValue(mongoOperations, targetClass, "_id", ids.get(0));
            }
        }
        return result != null ? result : original;
    }

}
