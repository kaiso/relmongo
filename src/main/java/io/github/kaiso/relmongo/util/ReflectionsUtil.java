package io.github.kaiso.relmongo.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;

public final class ReflectionsUtil {

    private ReflectionsUtil() {
        super();
    }

    public static Class<?> getGenericType(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        }
        return field.getType();
    }

}
