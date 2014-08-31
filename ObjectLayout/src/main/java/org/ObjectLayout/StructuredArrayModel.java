package org.ObjectLayout;

import org.ObjectLayout.intrinsifiable.StructuredArrayIntrinsifiableBase;

import java.lang.reflect.Constructor;

/**
 * A model for instantiating a structured array.
 */
public class StructuredArrayModel<A extends StructuredArray<T>, T> {
    private final Class<A> arrayClass;
    private final Class<T> elementClass;
    private final StructuredArrayModel subArrayModel;
    private final long length;

    public StructuredArrayModel(final Class<A> arrayClass,
                                final Class<T> elementClass,
                                final long length) {
        this.arrayClass = arrayClass;
        this.elementClass = elementClass;
        this.subArrayModel = null;
        this.length = length;
    }


    @SuppressWarnings("unchecked")
    public <A2 extends StructuredArray<T2>, T2>
    StructuredArrayModel(final Class<A> arrayClass,
                         final StructuredArrayModel<A2, T2> subArrayModel,
                         final long length) {
        this.arrayClass = arrayClass;
        this.elementClass = (Class<T>) subArrayModel.arrayClass;
        this.subArrayModel = subArrayModel;
        this.length = length;
    }

    @Override
    public boolean equals(Object other) {
        // TODO: check equality
        return false;
    }

    public Class<A> getArrayClass() {
        return arrayClass;
    }

    public Class<T> getElementClass() {
        return elementClass;
    }

    public StructuredArrayModel getSubArrayModel() {
        return subArrayModel;
    }

    public long getLength() {
        return length;
    }
}
