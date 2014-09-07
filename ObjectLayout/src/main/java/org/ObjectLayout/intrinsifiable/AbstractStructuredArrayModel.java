package org.ObjectLayout.intrinsifiable;

/**
 * A model that describes the structure of a StructuredArrayIntrinsifiableBase
 *
 * @param <S> The class of the StructuredArrayIntrinsifiableBase modeled by the model
 * @param <T> The class of the elements in the StructuredArray modeled by the model
 */
public class AbstractStructuredArrayModel<S extends AbstractStructuredArray<T>, T>  extends
        AbstractArrayModel<S> {
    private final AbstractArrayModel subArrayModel;
    private final Class<T> elementClass;

    /**
     * Create a model of a StructuredArray instance with terminal (non StructuredArray) elements
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param elementClass The class of the elements in the StructuredArray modeled by the model
     * @param length The length of the StructuredArray modeled by the model
     */
    public AbstractStructuredArrayModel(final Class<S> arrayClass,
                                        final Class<T> elementClass,
                                        final long length) {
        super(arrayClass, length);
        this.subArrayModel = null;
        this.elementClass = elementClass;
    }

    /**
     * Create a model of a StructuredArray instance with elements that are themselves StructuredArrays
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param subArrayModel The model describing the structure of the elements of the array being modeled
     * @param length The length of the StructuredArray modeled by the model
     */
    @SuppressWarnings("unchecked")
    public AbstractStructuredArrayModel(final Class<S> arrayClass,
                                        final AbstractArrayModel subArrayModel,
                                        final long length) {
        super(arrayClass, length);
        this.subArrayModel = subArrayModel;
        this.elementClass = (Class<T>) subArrayModel._getArrayClass();
    }

    /**
     * Get the model describing the structure of the elements of the array being modeled (when those
     * elements are arrays).
     * @return the model describing the structure of the elements of the array being modeled
     */
    protected final AbstractArrayModel _getSubArrayModel() {
        return subArrayModel;
    }

    /**
     * Get the class of the elements in the StructuredArray modeled by the model
     * @return the class of the elements in the StructuredArray modeled by the model
     */
    protected final Class<T> _getElementClass() {
        return elementClass;
    }
}
