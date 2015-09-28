package org.ObjectLayout;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A model that describes the structure of a AbstractStructuredArray
 *
 * @param <S> The class of the AbstractStructuredArray modeled by the model
 * @param <T> The class of the elements in the StructuredArray modeled by the model
 */
abstract class AbstractStructuredArrayModel<S extends AbstractStructuredArrayBase<T>, T> {
    private final Class<S> arrayClass;
    private final long length;
    private final StructuredArrayModel structuredSubArrayModel;
    private final PrimitiveArrayModel primitiveSubArrayModel;
    private final Class<T> elementClass;

    /**
     * Create a model of a StructuredArray instance with terminal (non StructuredArray) elements
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param elementClass The class of the elements in the StructuredArray modeled by the model
     * @param length The length of the StructuredArray modeled by the model
     */
    AbstractStructuredArrayModel(final Class<S> arrayClass,
                                        final Class<T> elementClass,
                                        final long length) {
        this.arrayClass = arrayClass != null ? arrayClass : deriveArrayTypeParameter();
        this.length = length;
        this.structuredSubArrayModel = null;
        this.primitiveSubArrayModel = null;
        this.elementClass = elementClass != null ? elementClass : deriveElementTypeParameter();
    }

    /**
     * Create a model of a StructuredArray instance with elements that are PrimitiveArrays
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param primitiveSubArrayModel The model describing the structure of the elements of the array being modeled
     * @param length The length of the StructuredArray modeled by the model
     */
    @SuppressWarnings("unchecked")
    AbstractStructuredArrayModel(final Class<S> arrayClass,
                                        final PrimitiveArrayModel primitiveSubArrayModel,
                                        final long length) {
        this.arrayClass = arrayClass != null ? arrayClass : deriveArrayTypeParameter();
        this.length = length;
        this.structuredSubArrayModel = null;
        this.primitiveSubArrayModel = primitiveSubArrayModel;
        this.elementClass = (Class<T>) primitiveSubArrayModel._getArrayClass();
    }

    /**
     * Create a model of a StructuredArray instance with elements that are themselves StructuredArrays
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param structuredSubArrayModel The model describing the structure of the elements of the array being modeled
     * @param length The length of the StructuredArray modeled by the model
     */
    @SuppressWarnings("unchecked")
    AbstractStructuredArrayModel(final Class<S> arrayClass,
                                        final StructuredArrayModel structuredSubArrayModel,
                                        final long length) {
        this.arrayClass = arrayClass != null ? arrayClass : deriveArrayTypeParameter();
        this.length = length;
        this.structuredSubArrayModel = structuredSubArrayModel;
        this.primitiveSubArrayModel = null;
        this.elementClass = (Class<T>) structuredSubArrayModel._getArrayClass();
    }

    /**
     * Get the class of the array modeled by this model
     * @return the class of the StructuredArray modeled by this model
     */
    final Class<S> _getArrayClass() {
        return arrayClass;
    }

    /**
     * Get the length of the array modeled by the model
     * @return The length of the StructuredArray modeled by the model
     */
    final long _getLength() {
        return length;
    }


    /**
     * Get the model describing the structure of the elements of the array being modeled (when those
     * elements are structured arrays).
     * @return the model describing the structure of the elements of the array being modeled
     */
    final StructuredArrayModel _getStructuredSubArrayModel() {
        return structuredSubArrayModel;
    }

    /**
     * Get the model describing the structure of the elements of the array being modeled (when those
     * elements are primitive arrays).
     * @return the model describing the structure of the elements of the array being modeled
     */
    final PrimitiveArrayModel _getPrimitiveSubArrayModel() {
        return primitiveSubArrayModel;
    }

    /**
     * Get the class of the elements in the StructuredArray modeled by the model
     * @return the class of the elements in the StructuredArray modeled by the model
     */
    final Class<T> _getElementClass() {
        return elementClass;
    }

    private Class<T> deriveElementTypeParameter() {
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        @SuppressWarnings("unchecked")
        Class<T> derivedType = typeToClass(genericSuperclass.getActualTypeArguments()[1]);
        return derivedType;
    }

    private Class<S> deriveArrayTypeParameter() {
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        @SuppressWarnings("unchecked")
        Class<S> derivedType = typeToClass(genericSuperclass.getActualTypeArguments()[0]);
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
