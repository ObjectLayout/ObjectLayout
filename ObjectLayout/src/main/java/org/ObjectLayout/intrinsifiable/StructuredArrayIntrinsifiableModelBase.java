package org.ObjectLayout.intrinsifiable;

/**
 * A model that describes the structure of a StructuredArray
 *
 * @param <S> The class of the StructuredArray modeled by the model
 * @param <T> The class of the elements in the StructuredArray modeled by the model
 */
public class StructuredArrayIntrinsifiableModelBase<S extends StructuredArrayIntrinsifiableBase<T>, T> {
    private final Class<S> arrayClass;
    private final Class<T> elementClass;
    private final StructuredArrayIntrinsifiableModelBase subArrayModel;
    private final long length;

    /**
     * Create a model of a StructuredArray instance with terminal (non StructuredArray) elements
     *
     * @param arrayClass The class of the StructuredArray modeled by the model
     * @param elementClass The class of the elements in the StructuredArray modeled by the model
     * @param length The length of the StructuredArray modeled by the model
     */
    public StructuredArrayIntrinsifiableModelBase(final Class<S> arrayClass,
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
    public <A2 extends StructuredArrayIntrinsifiableBase<T2>, T2>
    StructuredArrayIntrinsifiableModelBase(final Class<S> arrayClass,
                                           final StructuredArrayIntrinsifiableModelBase<A2, T2> subArrayModel,
                                           final long length) {
        this.arrayClass = arrayClass;
        this.elementClass = (Class<T>) subArrayModel.arrayClass;
        this.subArrayModel = subArrayModel;
        this.length = length;
    }

    /**
     * Get the class of the StructuredArray modeled by this model
     * @return the class of the StructuredArray modeled by this model
     */
    protected final Class<S> _getArrayClass() {
        return arrayClass;
    }

    /**
     * Get the class of the elements in the StructuredArray modeled by the model
     * @return the class of the elements in the StructuredArray modeled by the model
     */
    protected final Class<T> _getElementClass() {
        return elementClass;
    }

    /**
     * Get the model describing the structure of the elements of the array being modeled
     * @return the model describing the structure of the elements of the array being modeled
     */
    protected final StructuredArrayIntrinsifiableModelBase _getSubArrayModel() {
        return subArrayModel;
    }

    /**
     * Get the length of the StructuredArray modeled by the model
     * @return The length of the StructuredArray modeled by the model
     */
    protected final long _getLength() {
        return length;
    }
}
