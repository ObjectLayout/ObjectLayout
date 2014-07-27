/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

public class PrimitiveIntArray extends PrimitiveArray {

    private int[] array;

    public int[] getArray() {
        return array;
    }

    public int get(final int index) {
        return array[index];
    }

    public void set(final int index, final int value) {
        array[index] = value;
    }

    public static PrimitiveIntArray newInstance(final int length) {
        return PrimitiveArray.newSubclassInstance(PrimitiveIntArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final int length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        array = new int[(int) length];
    }
}
