/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveDoubleArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveDoubleArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractPrimitiveDoubleArray extends PrimitiveArray {

    private final double[] array;

    protected final double[] _getArray() {
        return array;
    }

    protected AbstractPrimitiveDoubleArray() {
        array = new double[getLength()];
    }

    protected AbstractPrimitiveDoubleArray(AbstractPrimitiveDoubleArray sourceArray) {
        array = sourceArray._getArray().clone();
    }
}
