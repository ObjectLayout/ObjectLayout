package org.ObjectLayout;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A model that describes the structure of a StructuredArray or PrimitiveArray
 *
 * @param <A> The class of the array modeled by the model
 */
abstract class AbstractPrimitiveArrayModel<A extends AbstractPrimitiveArray> {
    private final Class<A> arrayClass;
    private final long length;

    /**
     * Create a model of an array instance with terminal (non StructuredArray) elements
     *
     * @param arrayClass The class of the array modeled by the model
     * @param length The length of the StructuredArray modeled by the model
     */
    AbstractPrimitiveArrayModel(final Class<A> arrayClass,
                                       final long length) {
        this.arrayClass = arrayClass != null ? arrayClass : deriveArrayTypeParameter();
        this.length = length;
    }

    /**
     * Get the class of the array modeled by this model
     * @return the class of the StructuredArray modeled by this model
     */
    final Class<A> _getArrayClass() {
        return arrayClass;
    }

    /**
     * Get the length of the array modeled by the model
     * @return The length of the StructuredArray modeled by the model
     */
    final long _getLength() {
        return length;
    }

    private Class<A> deriveArrayTypeParameter() {
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        @SuppressWarnings("unchecked")
        Class<A> derivedType = typeToClass(genericSuperclass.getActualTypeArguments()[0]);
        return derivedType;
    }

    static Class typeToClass(Type t) {
        if (t instanceof Class) {
            return (Class) t;
        } else if (t instanceof ParameterizedType) {
            return (Class) ((ParameterizedType)t).getRawType();
        } else {
            throw new IllegalArgumentException();
        }
    }
}
