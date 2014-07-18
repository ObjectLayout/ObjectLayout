/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

public class PrimitiveCharArray extends PrimitiveArray {

    private char[] array;

    public char[] getArray() {
        return array;
    }

    public char get(final int index) {
        return array[index];
    }

    public void set(final int index, final char value) {
        array[index] = value;
    }

    public char get(final long index) {
        checkBounds(index);
        return array[(int) index];
    }

    public void set(final long index, final char value) {
        checkBounds(index);
        array[(int) index] = value;
    }

    public static PrimitiveCharArray newInstance(final long length) {
        return PrimitiveArray.newSubclassInstance(PrimitiveCharArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final long length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        array = new char[(int) length];
    }

    void checkBounds(final long index) {
        if (index > array.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
