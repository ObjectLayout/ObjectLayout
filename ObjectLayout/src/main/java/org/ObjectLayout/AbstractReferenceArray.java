/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

/**
 * This class contains the intrinsifiable portions of PrimitiveShortArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveShortArray are expected to replace the implementation of this
 * base class.
 */

abstract class AbstractReferenceArray<T> extends AbstractPrimitiveArray {

    private final T[][] longAddressableElements; // Used to store elements at indexes above Integer.MAX_VALUE
    private final T[] intAddressableElements;

    final T[] _asArray() {
        if (_getLength() > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Cannot make T[] from array with more than Integer.MAX_VALUE elements (" +
                            _getLength() + ")");
        }
        return intAddressableElements;
    }

    T _get(final int index) {
        return intAddressableElements[index];
    }

    T _get(final long index) {
        if (index < Integer.MAX_VALUE) {
            return _get((int) index);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        return longAddressableElements[partitionIndex][partitionOffset];
    }

    void _set(final int index, final T value) {
        intAddressableElements[index] = value;
    }

    void _set(final long index, final T value) {
        if (index < Integer.MAX_VALUE) {
            _set((int) index, value);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        longAddressableElements[partitionIndex][partitionOffset] = value;
    }
    @SuppressWarnings("unchecked")
    AbstractReferenceArray() {
        intAddressableElements = (T[]) createIntAddressableElements(Object.class);
        longAddressableElements = (T[][]) createLongAddressableElements(Object.class);
    }

    @SuppressWarnings("unchecked")
    AbstractReferenceArray(AbstractReferenceArray sourceArray) {
        intAddressableElements = (T[]) sourceArray.intAddressableElements.clone();
        int numLongAddressablePartitions = sourceArray.longAddressableElements.length;
        longAddressableElements = (T[][]) new Object[numLongAddressablePartitions][];
        for (int i = 0; i < numLongAddressablePartitions; i++) {
            longAddressableElements[i] = (T[]) sourceArray.longAddressableElements[i].clone();
        }
    }
}
