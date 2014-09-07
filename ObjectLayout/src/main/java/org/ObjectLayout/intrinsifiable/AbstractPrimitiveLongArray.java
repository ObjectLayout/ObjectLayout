/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveLongArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveLongArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractPrimitiveLongArray extends PrimitiveArray {

    private final long[] array;

    protected final long[] _getArray() {
        return array;
    }

    protected AbstractPrimitiveLongArray() {
        array = new long[getLength()];
    }

    protected AbstractPrimitiveLongArray(AbstractPrimitiveLongArray sourceArray) {
        array = sourceArray._getArray().clone();
    }
}
