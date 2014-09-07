/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveFloatArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveFloatArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractPrimitiveFloatArray extends PrimitiveArray {

    private final float[] array;

    protected final float[] _getArray() {
        return array;
    }

    protected AbstractPrimitiveFloatArray() {
        array = new float[getLength()];
    }

    protected AbstractPrimitiveFloatArray(AbstractPrimitiveFloatArray sourceArray) {
        array = sourceArray._getArray().clone();
    }
}
