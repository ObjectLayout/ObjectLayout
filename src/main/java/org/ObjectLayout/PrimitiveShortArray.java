/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

public class PrimitiveShortArray extends PrimitiveArray {

    private short[] array;

    public short[] getArray() {
        return array;
    }

    public short get(final int index) {
        return array[index];
    }

    public void set(final int index, final short value) {
        array[index] = value;
    }

    public short get(final long index) {
        checkBounds(index);
        return get((int) index);
    }

    public void set(final long index, final short value) {
        checkBounds(index);
        set((int) index, value);
    }

    public static PrimitiveShortArray newInstance(final long length) {
        return (PrimitiveShortArray) PrimitiveArray.newSubclassInstance(PrimitiveShortArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final long length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        array = new short[(int) length];
    }

    void checkBounds(final long index) {
        if (index > array.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
