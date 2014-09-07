/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import org.ObjectLayout.intrinsifiable.PrimitiveArray;
import org.ObjectLayout.intrinsifiable.AbstractPrimitiveCharArray;

/**
 * A subclassable array of chars.
 *
 * PrimitiveCharArray is designed with semantics specifically chosen and restricted such that a "flat" memory
 * layout of the implemented data structure would be possible on optimizing JVMs. While fully functional
 * on all JVM implementation (of Java SE 6 and above), the semantics are such that a JVM may transparently
 * optimise the implementation to provide a compact contiguous layout that facilitates dead-reckoning (as
 * opposed to de-referenced) access to elements
 */

public class PrimitiveCharArray extends AbstractPrimitiveCharArray {

    /**
     * Get a reference to the internal {@link char[]} representation of the array.
     *
     * @return a reference to the internal {@link char[]} representation of the array
     */
    public char[] getArray() {
        return _getArray();
    }

    /**
     * Get the value of an element in the array.
     *
     * @param index the index of the element
     * @return the value of the element at the given index
     */
    public char get(final int index) {
        return _getArray()[index];
    }

    /**
     * set the value of an element in the array.
     *
     * @param index the index of the element to set
     * @param value the value to assign to the element
     */
    public void set(final int index, final char value) {
        _getArray()[index] = value;
    }

    public static PrimitiveCharArray newInstance(final int length) {
        return PrimitiveArray.newInstance(PrimitiveCharArray.class, length);
    }

    /**
     * Default constructor
     */
    public PrimitiveCharArray() {
        super();
    }

    /**
     * Copying constructor
     *
     * @param sourceArray the array to copy
     */
    public PrimitiveCharArray(PrimitiveCharArray sourceArray) {
        super(sourceArray);
    }
}
