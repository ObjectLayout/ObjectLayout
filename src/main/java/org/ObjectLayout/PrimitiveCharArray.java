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

    public static PrimitiveCharArray newInstance(final int length) {
        return PrimitiveArray.newSubclassInstance(PrimitiveCharArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final int length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        array = new char[(int) length];
    }
}
