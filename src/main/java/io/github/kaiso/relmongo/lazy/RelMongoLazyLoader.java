package io.github.kaiso.relmongo.lazy;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.cglib.proxy.LazyLoader;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.ReflectionUtils;

import io.github.kaiso.relmongo.exception.RelMongoConfigurationException;
import io.github.kaiso.relmongo.mongo.DatabaseOperations;
import io.github.kaiso.relmongo.util.ObjectIdReaderCallback;

public class RelMongoLazyLoader implements LazyLoader {

    private Object original;
    private MongoOperations mongoOperations;

    public RelMongoLazyLoader(Object original, MongoOperations mongoOperations) {
        super();
        this.original = original;
        this.mongoOperations = mongoOperations;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object loadObject() throws Exception {
        if (!(original instanceof LazyLoadingProxy)) {
            if (Collection.class.isAssignableFrom(original.getClass())) {
                List<ObjectId> idlist = (List<ObjectId>) ((Collection) original).stream().map(this::getIdFromObject).collect(Collectors.toList());
                if (!idlist.isEmpty()) {
                    return DatabaseOperations.findByIds(mongoOperations, ((Collection) original).iterator().next().getClass(),
                            idlist.toArray(new ObjectId[idlist.size()]));
                }
            } else {
                return DatabaseOperations.findByPropertyValue(mongoOperations, original.getClass(), "_id", getIdFromObject(original));
            }
        }
        return null;
    }

    private ObjectId getIdFromObject(Object obj) {

        ObjectIdReaderCallback objectIdReaderCallback = new ObjectIdReaderCallback(obj);
        ReflectionUtils.doWithFields(obj.getClass(), objectIdReaderCallback);
        return objectIdReaderCallback.getObjectId()
                .orElseThrow(() ->  new RelMongoConfigurationException("the Id field of class [" + obj.getClass()
				+ "] must be annotated by @Id (org.springframework.data.annotation.Id)"));

    }

   

}
