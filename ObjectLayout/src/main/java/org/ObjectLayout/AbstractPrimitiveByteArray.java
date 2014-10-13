/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

/**
 * This class contains the intrinsifiable portions of PrimitiveByteArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveByteArray are expected to replace the implementation of this
 * base class.
 */

abstract class AbstractPrimitiveByteArray extends AbstractPrimitiveArray {

    private final byte[][] longAddressableElements; // Used to store elements at indexes above Integer.MAX_VALUE
    private final byte[] intAddressableElements;

    final byte[] _asArray() {
        if (_getLength() > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Cannot make byte[] from array with more than Integer.MAX_VALUE elements (" +
                            _getLength() + ")");
        }
        return intAddressableElements;
    }

    byte _get(final int index) {
        return intAddressableElements[index];
    }

    byte _get(final long index) {
        if (index < Integer.MAX_VALUE) {
            return _get((int) index);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        return longAddressableElements[partitionIndex][partitionOffset];
    }

    void _set(final int index, final byte value) {
        intAddressableElements[index] = value;
    }

    void _set(final long index, final byte value) {
        if (index < Integer.MAX_VALUE) {
            _set((int) index, value);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        longAddressableElements[partitionIndex][partitionOffset] = value;
    }
    
    AbstractPrimitiveByteArray() {
        intAddressableElements = (byte[]) createIntAddressableElements(byte.class);
        longAddressableElements = (byte[][]) createLongAddressableElements(byte.class);
    }

    AbstractPrimitiveByteArray(AbstractPrimitiveByteArray sourceArray) {
        intAddressableElements = sourceArray.intAddressableElements.clone();
        int numLongAddressablePartitions = sourceArray.longAddressableElements.length;
        longAddressableElements = new byte[numLongAddressablePartitions][];
        for (int i = 0; i < numLongAddressablePartitions; i++) {
            longAddressableElements[i] = sourceArray.longAddressableElements[i].clone();
        }
    }
}
