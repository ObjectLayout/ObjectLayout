package org.ObjectLayout;

import org.ObjectLayout.intrinsifiable.StructuredArrayIntrinsifiableBase;

import java.lang.reflect.Constructor;

/**
 * A model that describes the structure of a StructuredArray
 *
 * @param <S> The class of the StructuredArray modeled by the model
 * @param <T> The class of the elements in the StructuredArray modeled by the model
 */
public class StructuredArrayModel<S extends StructuredArray<T>, T> {
    private final Class<S> arrayClass;
    private final Class<T> elementClass;
    private final StructuredArrayModel subArrayModel;
    private final long length;

    /**
     * Create a model of a StructuredArray instance with terminal (non StructuredArray) elements
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param elementClass The class of the elements in the StructuredArray modeled by the model
     * @param length The length of the StructuredArray modeled by the model
     */
    public StructuredArrayModel(final Class<S> arrayClass,
                                final Class<T> elementClass,
                                final long length) {
        this.arrayClass = arrayClass;
        this.elementClass = elementClass;
        this.subArrayModel = null;
        this.length = length;
    }

    /**
     * Create a model of a StructuredArray instance with elements that are themselves StructuredArrays
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param subArrayModel The model describing the structure of the elements of the array being modeled
     * @param length The length of the StructuredArray modeled by the model
     * @param <A2> The class of the StructuredArray modeled by the subArrayModel
     * @param <T2> The class of the elements in the StructuredArray modeled by the subArrayModel
     */
    @SuppressWarnings("unchecked")
    public <A2 extends StructuredArray<T2>, T2>
    StructuredArrayModel(final Class<S> arrayClass,
                         final StructuredArrayModel<A2, T2> subArrayModel,
                         final long length) {
        this.arrayClass = arrayClass;
        this.elementClass = (Class<T>) subArrayModel.arrayClass;
        this.subArrayModel = subArrayModel;
        this.length = length;
    }

    /**
     * Determine if this model is equal to another object. If the other object is not a model, they are
     * not equal. If the other object is a model, the two are equal if all details, (arrayClass, elementClass,
     * length, and any subArrayModel hierarchy details) are identical.
     *
     * @param other the other object
     * @return true is the other object is a model that is equal to this one, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StructuredArrayModel)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        StructuredArrayModel<S, T> otherArray = (StructuredArrayModel<S, T>) other;
        if ((arrayClass != otherArray.arrayClass) ||
                (elementClass != otherArray.elementClass) ||
                (length != otherArray.length)) {
            return false;
        }
        if ((subArrayModel == null) && (otherArray.subArrayModel == null)) {
            return true;
        }
        // if either subArrayModel is null, they are not equal. Otherwise, compare subArrays:
        return (subArrayModel != null) &&
                (otherArray.subArrayModel != null) &&
                subArrayModel.equals(otherArray.subArrayModel);
    }

    /**
     * Get the class of the StructuredArray modeled by this model
     * @return the class of the StructuredArray modeled by this model
     */
    public Class<S> getArrayClass() {
        return arrayClass;
    }

    /**
     * Get the class of the elements in the StructuredArray modeled by the model
     * @return the class of the elements in the StructuredArray modeled by the model
     */
    public Class<T> getElementClass() {
        return elementClass;
    }

    /**
     * Get the model describing the structure of the elements of the array being modeled
     * @return the model describing the structure of the elements of the array being modeled
     */
    public StructuredArrayModel getSubArrayModel() {
        return subArrayModel;
    }

    /**
     * Get the length of the StructuredArray modeled by the model
     * @return The length of the StructuredArray modeled by the model
     */
    public long getLength() {
        return length;
    }
}
