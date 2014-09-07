/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveIntArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveIntArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractPrimitiveIntArray extends PrimitiveArray {

    private final int[] array;

    protected final int[] _getArray() {
        return array;
    }

    protected AbstractPrimitiveIntArray() {
        array = new int[getLength()];
    }

    protected AbstractPrimitiveIntArray(AbstractPrimitiveIntArray sourceArray) {
        array = sourceArray._getArray().clone();
    }
}
