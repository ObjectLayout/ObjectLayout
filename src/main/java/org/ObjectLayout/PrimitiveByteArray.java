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

    public static PrimitiveByteArray newInstance(final int length) {
        return PrimitiveArray.newSubclassInstance(PrimitiveByteArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final int length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        array = new byte[(int) length];
    }
}
