package org.ObjectLayout;

/**
 * A model that describes the structure of a PrimitiveArray
 *
 * @param <S> The class of the PrimitiveArray modeled by the model
 */
public class PrimitiveArrayModel<S extends AbstractPrimitiveArray> extends AbstractPrimitiveArrayModel<S> {

    /**
     * Create a model of a PrimitiveArray instance
     *
     * @param arrayClass The class of the PrimitiveArray modeled by the model
     * @param length The length of the PrimitiveArray modeled by the model
     */
    public PrimitiveArrayModel(final Class<S> arrayClass,
                               final long length) {
        super(arrayClass, length);
    }

    /**
     * Determine if this model is equal to another object. If the other object is not a model, they are
     * not equal. If the other object is a model, the two are equal if all details, (arrayClass, elementClass,
     * length) are identical.
     *
     * @param other the other object
     * @return true is the other object is a model that is equal to this one, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PrimitiveArrayModel)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        PrimitiveArrayModel<S> otherArray = (PrimitiveArrayModel<S>) other;
        if ((getArrayClass() != otherArray.getArrayClass()) ||
                (getLength() != otherArray.getLength())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getArrayClass().hashCode() ^ ((int)(getLength()^(getLength()>>>32)));
    }

    /**
     * Get the class of the PrimitiveArray modeled by this model
     * @return the class of the PrimitiveArray modeled by this model
     */
    public final Class<S> getArrayClass() {
        return super._getArrayClass();
    }

    /**
     * Get the length of the PrimitiveArray modeled by the model
     * @return The length of the PrimitiveArray modeled by the model
     */
    public final  long getLength() {
        return super._getLength();
    }
}
