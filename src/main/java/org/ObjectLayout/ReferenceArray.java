/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;


public class ReferenceArray<T> extends PrimitiveArray {

    private T[] array;

    public T[] getArray() {
        return array;
    }

    public T get(final int index) {
        return array[index];
    }

    public void set(final int index, final T value) {
        array[index] = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> ReferenceArray<T> newInstance(final int length) {
        return PrimitiveArray.newSubclassInstance(ReferenceArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final int length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        @SuppressWarnings("unchecked")
        T[] myArray = (T[]) new Object[(int) length];
        array = myArray;
    }
}
