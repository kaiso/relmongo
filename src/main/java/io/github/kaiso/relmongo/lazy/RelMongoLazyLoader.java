package io.github.kaiso.relmongo.lazy;

import io.github.kaiso.relmongo.events.processor.MappedByProcessor;
import io.github.kaiso.relmongo.mongo.DatabaseOperations;

import org.bson.types.ObjectId;
import org.springframework.cglib.proxy.LazyLoader;
import org.springframework.data.mongodb.core.MongoOperations;

import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Kais OMRI
 *
 */
public class RelMongoLazyLoader implements LazyLoader {

    private List<Object> ids;
    private MongoOperations mongoOperations;
    private Class<?> targetClass;
    private String fieldName;
    private Class<?> fieldType;
    private Object original;
    private Object parent;
    private String property;

    public RelMongoLazyLoader(List<Object> ids, String property, MongoOperations mongoOperations, Class<?> targetClass,
            Class<?> fieldType, String fieldName, Object original, Object parent) {
        super();
        this.ids = ids;
        this.property = property;
        this.mongoOperations = mongoOperations;
        this.targetClass = targetClass;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.original = original;
        this.parent = parent;
    }

    @Override
    public Object loadObject() throws Exception {
        Object result = null;
        if (!(original instanceof LazyLoadingProxy) && !ids.isEmpty()) {
            if (Collection.class.isAssignableFrom(fieldType)) {
                result = DatabaseOperations.findByIds(mongoOperations, targetClass, ids.toArray(new ObjectId[ids.size()]));
            } else {
                if (property == null) {
                    result = DatabaseOperations.findByPropertyValue(mongoOperations, targetClass, "_id", ids.get(0));
                } else {
                    result = DatabaseOperations.findByPropertyValue(mongoOperations, targetClass, property + "._id", ids.get(0));
                    MappedByProcessor.processChild(parent, parent, parent.getClass().getDeclaredField(fieldName), fieldType);
                }
            }
        }
        return result != null ? result : original;
    }

}
