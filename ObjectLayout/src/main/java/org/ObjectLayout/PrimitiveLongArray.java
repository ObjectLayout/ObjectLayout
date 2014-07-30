/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

/**
 * A subclassable array of longs.
 */

public class PrimitiveLongArray extends PrimitiveArray {

    private long[] array;

    public long[] getArray() {
        return array;
    }

    public long get(final int index) {
        return array[index];
    }

    public void set(final int index, final long value) {
        array[index] = value;
    }

    public static PrimitiveLongArray newInstance(final int length) {
        return PrimitiveArray.newSubclassInstance(PrimitiveLongArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final int length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        array = new long[(int) length];
    }
}
