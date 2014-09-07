/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import org.ObjectLayout.intrinsifiable.PrimitiveArray;
import org.ObjectLayout.intrinsifiable.AbstractPrimitiveShortArray;

/**
 * A subclassable array of shorts.
 *
 * PrimitiveShortArray is designed with semantics specifically chosen and restricted such that a "flat" memory
 * layout of the implemented data structure would be possible on optimizing JVMs. While fully functional
 * on all JVM implementation (of Java SE 6 and above), the semantics are such that a JVM may transparently
 * optimise the implementation to provide a compact contiguous layout that facilitates dead-reckoning (as
 * opposed to de-referenced) access to elements
 */

public class PrimitiveShortArray extends AbstractPrimitiveShortArray {

    /**
     * Get a reference to the internal {@link short[]} representation of the array.
     *
     * @return a reference to the internal {@link short[]} representation of the array
     */
    public short[] getArray() {
        return _getArray();
    }

    /**
     * Get the value of an element in the array.
     *
     * @param index the index of the element
     * @return the value of the element at the given index
     */
    public short get(final int index) {
        return _getArray()[index];
    }

    /**
     * set the value of an element in the array.
     *
     * @param index the index of the element to set
     * @param value the value to assign to the element
     */
    public void set(final int index, final short value) {
        _getArray()[index] = value;
    }

    public static PrimitiveShortArray newInstance(final int length) {
        return PrimitiveArray.newInstance(PrimitiveShortArray.class, length);
    }

    /**
     * Default constructor
     */
    public PrimitiveShortArray() {
        super();
    }

    /**
     * Copying constructor
     *
     * @param sourceArray the array to copy
     */
    public PrimitiveShortArray(PrimitiveShortArray sourceArray) {
        super(sourceArray);
    }
}
