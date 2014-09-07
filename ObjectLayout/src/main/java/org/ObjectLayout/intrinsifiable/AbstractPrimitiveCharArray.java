/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveCharArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveCharArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractPrimitiveCharArray extends PrimitiveArray {

    private final char[] array;

    protected final char[] _getArray() {
        return array;
    }

    protected AbstractPrimitiveCharArray() {
        array = new char[getLength()];
    }

    protected AbstractPrimitiveCharArray(AbstractPrimitiveCharArray sourceArray) {
        array = sourceArray._getArray().clone();
    }
}
