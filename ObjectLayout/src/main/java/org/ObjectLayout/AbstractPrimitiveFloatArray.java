/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

/**
 * This class contains the intrinsifiable portions of PrimitiveFloatArray behavior. JDK implementations
 * that choose to intrinsify PrimitiveFloatArray are expected to replace the implementation of this
 * base class.
 */

abstract class AbstractPrimitiveFloatArray extends AbstractPrimitiveArray {

    private final float[][] longAddressableElements; // Used to store elements at indexes above Integer.MAX_VALUE
    private final float[] intAddressableElements;

    final float[] _asArray() {
        if (_getLength() > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Cannot make float[] from array with more than Integer.MAX_VALUE elements (" +
                            _getLength() + ")");
        }
        return intAddressableElements;
    }

    float _get(final int index) {
        return intAddressableElements[index];
    }

    float _get(final long index) {
        if (index < Integer.MAX_VALUE) {
            return _get((int) index);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        return longAddressableElements[partitionIndex][partitionOffset];
    }

    void _set(final int index, final float value) {
        intAddressableElements[index] = value;
    }

    void _set(final long index, final float value) {
        if (index < Integer.MAX_VALUE) {
            _set((int) index, value);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        longAddressableElements[partitionIndex][partitionOffset] = value;
    }

    AbstractPrimitiveFloatArray() {
        intAddressableElements = (float[]) createIntAddressableElements(float.class);
        longAddressableElements = (float[][]) createLongAddressableElements(float.class);
    }

    AbstractPrimitiveFloatArray(AbstractPrimitiveFloatArray sourceArray) {
        intAddressableElements = sourceArray.intAddressableElements.clone();
        int numLongAddressablePartitions = sourceArray.longAddressableElements.length;
        longAddressableElements = new float[numLongAddressablePartitions][];
        for (int i = 0; i < numLongAddressablePartitions; i++) {
            longAddressableElements[i] = sourceArray.longAddressableElements[i].clone();
        }
    }
}
