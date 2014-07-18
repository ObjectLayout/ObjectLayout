/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

public class PrimitiveByteArray extends PrimitiveArray {

    private byte[] array;

    public byte[] getArray() {
        return array;
    }

    public byte get(final int index) {
        return array[index];
    }

    public void set(final int index, final byte value) {
        array[index] = value;
    }

    public byte get(final long index) {
        checkBounds(index);
        return get((int) index);
    }

    public void set(final long index, final byte value) {
        checkBounds(index);
        set((int) index, value);
    }

    public static PrimitiveByteArray newInstance(final long length) {
        return (PrimitiveByteArray) PrimitiveArray.newSubclassInstance(PrimitiveByteArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final long length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        array = new byte[(int) length];
    }

    void checkBounds(final long index) {
        if (index > array.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
