/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveByteArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveByteArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractPrimitiveByteArray extends PrimitiveArray {

    private final byte[] array;

    protected final byte[] _getArray() {
        return array;
    }

    protected AbstractPrimitiveByteArray() {
        array = new byte[getLength()];
    }

    protected AbstractPrimitiveByteArray(AbstractPrimitiveByteArray sourceArray) {
        array = sourceArray._getArray().clone();
    }
}
