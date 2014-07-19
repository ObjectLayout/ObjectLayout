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

    public T get(final long index) {
        checkBounds(index);
        return array[(int) index];
    }

    public void set(final long index, final T value) {
        checkBounds(index);
        array[(int) index] = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> ReferenceArray<T> newInstance(final long length) {
        return PrimitiveArray.newSubclassInstance(ReferenceArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final long length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        @SuppressWarnings("unchecked")
        T[] myArray = (T[]) new Object[(int) length];
        array = myArray;
    }

    void checkBounds(final long index) {
        if (index > array.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
    
    public static <T> void shallowCopy(
            final ReferenceArray<T> src, final long srcOffset, 
            final ReferenceArray<T> dst, final long dstOffset, 
            final long count) {
        
        int length = (int) count;
        int srcOff = (int) srcOffset;
        int dstOff = (int) dstOffset;
        
        System.arraycopy(src.array, srcOff, dst.array, dstOff, length);
    }
}
