/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

/**
 * This class contains the intrinsifiable portions of PrimitiveShortArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveShortArray are expected to replace the implementation of this
 * base class.
 */

public abstract class AbstractPrimitiveShortArray extends PrimitiveArray {

    private final short[][] longAddressableElements; // Used to store elements at indexes above Integer.MAX_VALUE
    private final short[] intAddressableElements;

    protected final short[] _asArray() {
        if (getLength() > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Cannot make short[] from array with more than Integer.MAX_VALUE elements (" +
                            getLength() + ")");
        }
        return intAddressableElements;
    }

    protected short _get(final int index) {
        return intAddressableElements[index];
    }

    protected short _get(final long index) {
        if (index < Integer.MAX_VALUE) {
            return _get((int) index);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        return longAddressableElements[partitionIndex][partitionOffset];
    }

    protected void _set(final int index, final short value) {
        intAddressableElements[index] = value;
    }

    protected void _set(final long index, final short value) {
        if (index < Integer.MAX_VALUE) {
            _set((int) index, value);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        longAddressableElements[partitionIndex][partitionOffset] = value;
    }
    
    protected AbstractPrimitiveShortArray() {
        intAddressableElements = (short[]) createIntAddressableElements(short.class);
        longAddressableElements = (short[][]) createLongAddressableElements(short.class);
    }

    protected AbstractPrimitiveShortArray(AbstractPrimitiveShortArray sourceArray) {
        intAddressableElements = sourceArray.intAddressableElements.clone();
        int numLongAddressablePartitions = sourceArray.longAddressableElements.length;
        longAddressableElements = new short[numLongAddressablePartitions][];
        for (int i = 0; i < numLongAddressablePartitions; i++) {
            longAddressableElements[i] = sourceArray.longAddressableElements[i].clone();
        }
    }
}
