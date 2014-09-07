package org.ObjectLayout.intrinsifiable;

/**
 * A model that describes the structure of a StructuredArray or PrimitiveArray
 *
 * @param <A> The class of the array modeled by the model
 */
public abstract class AbstractArrayModel<A extends AbstractArray> {
    private final Class<A> arrayClass;
    private final long length;

    /**
     * Create a model of an array instance with terminal (non StructuredArray) elements
     *
     * @param arrayClass The class of the array modeled by the model
     * @param length The length of the StructuredArray modeled by the model
     */
    public AbstractArrayModel(final Class<A> arrayClass,
                              final long length) {
        this.arrayClass = arrayClass;
        this.length = length;
    }

    /**
     * Get the class of the array modeled by this model
     * @return the class of the StructuredArray modeled by this model
     */
    protected final Class<A> _getArrayClass() {
        return arrayClass;
    }

    /**
     * Get the length of the array modeled by the model
     * @return The length of the StructuredArray modeled by the model
     */
    protected final long _getLength() {
        return length;
    }
}
