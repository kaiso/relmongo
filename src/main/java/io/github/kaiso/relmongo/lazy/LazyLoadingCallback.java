package io.github.kaiso.relmongo.lazy;

import io.github.kaiso.relmongo.mongo.DatabaseLoader;

import org.bson.types.ObjectId;
import org.springframework.cglib.proxy.LazyLoader;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LazyLoadingCallback implements LazyLoader {

    private Object original;
    private MongoOperations mongoOperations;

    public LazyLoadingCallback(Object original, MongoOperations mongoOperations) {
        super();
        this.original = original;
        this.mongoOperations = mongoOperations;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object loadObject() throws Exception {

        if (Collection.class.isAssignableFrom(original.getClass()) && !(original instanceof LazyLoadingProxy)) {
            List<ObjectId> idlist = (List<ObjectId>) ((Collection) original).stream().map(this::getIdFromObject)
                    .collect(Collectors.toList());
            if (!idlist.isEmpty()) {
                return DatabaseLoader.findByIds(mongoOperations, ((Collection) original).iterator().next().getClass(),
                        idlist.toArray(new ObjectId[idlist.size()]));
            }
        }
        return Collections.emptyList();
    }

    private ObjectId getIdFromObject(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                ReflectionUtils.makeAccessible(field);
                try {
                    Object value = field.get(obj);
                    ObjectId id;
                    if (value instanceof String) {
                        id = new ObjectId((String) value);
                    } else {
                        id = (ObjectId) value;
                    }
                    return id;
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException("unable to access the @Id field", e);
                } catch (ClassCastException e) {
                    throw new IllegalStateException("the @Id field must be of type ObjectId or String", e);
                }
            }
        }
        throw new IllegalStateException("can not find the @Id field");

    }

}
