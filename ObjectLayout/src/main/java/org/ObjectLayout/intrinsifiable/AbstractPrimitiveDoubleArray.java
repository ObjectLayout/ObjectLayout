/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveDoubleArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveDoubleArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractPrimitiveDoubleArray extends PrimitiveArray {

    private final double[][] longAddressableElements; // Used to store elements at indexes above Integer.MAX_VALUE
    private final double[] intAddressableElements;

    protected final double[] _asArray() {
        if (getLength() > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Cannot make double[] from array with more than Integer.MAX_VALUE elements (" +
                            getLength() + ")");
        }
        return intAddressableElements;
    }

    protected double _get(final int index) {
        return intAddressableElements[index];
    }

    protected double _get(final long index) {
        if (index < Integer.MAX_VALUE) {
            return _get((int) index);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        return longAddressableElements[partitionIndex][partitionOffset];
    }

    protected void _set(final int index, final double value) {
        intAddressableElements[index] = value;
    }

    protected void _set(final long index, final double value) {
        if (index < Integer.MAX_VALUE) {
            _set((int) index, value);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        longAddressableElements[partitionIndex][partitionOffset] = value;
    }
    
    protected AbstractPrimitiveDoubleArray() {
        intAddressableElements = (double[]) createIntAddressableElements(double.class);
        longAddressableElements = (double[][]) createLongAddressableElements(double.class);
    }

    protected AbstractPrimitiveDoubleArray(AbstractPrimitiveDoubleArray sourceArray) {
        intAddressableElements = sourceArray.intAddressableElements.clone();
        int numLongAddressablePartitions = sourceArray.longAddressableElements.length;
        longAddressableElements = new double[numLongAddressablePartitions][];
        for (int i = 0; i < numLongAddressablePartitions; i++) {
            longAddressableElements[i] = sourceArray.longAddressableElements[i].clone();
        }
    }
}
