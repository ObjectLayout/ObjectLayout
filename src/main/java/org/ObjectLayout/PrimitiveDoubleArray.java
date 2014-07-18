/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

public class PrimitiveDoubleArray extends PrimitiveArray {

    private double[] array;

    public double[] getArray() {
        return array;
    }

    public double get(final int index) {
        return array[index];
    }

    public void set(final int index, final double value) {
        array[index] = value;
    }

    public double get(final long index) {
        checkBounds(index);
        return array[(int) index];
    }

    public void set(final long index, final double value) {
        checkBounds(index);
        array[(int) index] = value;
    }

    public static PrimitiveDoubleArray newInstance(final long length) {
        return (PrimitiveDoubleArray) PrimitiveArray.newSubclassInstance(PrimitiveDoubleArray.class, length);
    }

    @Override
    final void initializePrimitiveArray(final long length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot instantiate array with more than Integer.MAX_VALUE elements");
        }
        array = new double[(int) length];
    }

    void checkBounds(final long index) {
        if (index > array.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
