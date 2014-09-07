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

public abstract class AbstractPrimitiveShortArray extends PrimitiveArray {

    private final short[] array;

    protected final short[] _getArray() {
        return array;
    }

    protected AbstractPrimitiveShortArray() {
        array = new short[getLength()];
    }

    protected AbstractPrimitiveShortArray(AbstractPrimitiveShortArray sourceArray) {
        array = sourceArray._getArray().clone();
    }
}
