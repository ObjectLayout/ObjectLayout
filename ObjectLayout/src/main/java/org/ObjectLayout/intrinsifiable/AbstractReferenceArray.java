/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveShortArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveShortArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractReferenceArray<T> extends PrimitiveArray {

    private final T[] array;

    protected final T[] _getArray() {
        return array;
    }

    protected AbstractReferenceArray() {
        @SuppressWarnings("unchecked")
        T[] a = (T[]) new Object[(int) getLength()];
        array = a;
    }

    protected AbstractReferenceArray(AbstractReferenceArray<T> sourceArray) {
        array = sourceArray._getArray().clone();
    }
}
