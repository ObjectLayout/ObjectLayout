/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class StructuredArrayIntrinsicSupport {
    static final boolean StructuredArrayIsIntrinsicToJdk;

    static {
        StructuredArrayIsIntrinsicToJdk = false;
    }

    /**
     * Instantiate a StructuredArray of arrayClass with member elements of elementClass, and the
     * set of lengths (one length per dimension in the lengths[] array), using the given constructor
     * and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with one that
     * allocates room for the entire StructuredArray and all it's elements.
     */
    static <T> StructuredArray<T> instantiateStructuredArray(Class<? extends StructuredArray<T>> arrayClass,
                                                      Class<T> elementClass,
                                                      long[] lengths,
                                                      final Constructor<? extends StructuredArray<T>> constructor,
                                                      Object... args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return constructor.newInstance(args);
    }

    /**
     * Construct a fresh element intended to occupy a given index in the given array, using the
     * supplied constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    static <T> T constructElementAtIndex(final StructuredArray<T> structuredArray,
                                            final long index0,
                                            final Constructor<T> constructor,
                                            Object... args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return constructor.newInstance(args);
    }

    /**
     * Construct a fresh sub-array intended to occupy a given index in the given array, using the
     * supplied constructor.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    static <T> StructuredArray<T> constructSubArrayAtIndex(final StructuredArray<T> structuredArray,
                                             long index0,
                                             final Constructor<StructuredArray<T>> constructor)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return constructor.newInstance();
    }

    /**
     * Get an element at a supplied [index] in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    static <T> T get(final StructuredArray<T> structuredArray, final int index)
            throws IllegalArgumentException {
        if (structuredArray.getDimensionCount() != 1) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }

        return structuredArray.getIntAddressableElements()[index];
    }

    /**
     * Get an element at a supplied [index0, index1] in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    static <T> T get(final StructuredArray<T> structuredArray, final int index0, final int index1)
            throws IllegalArgumentException {
        if (structuredArray.getDimensionCount() != 2) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }

        return getSubArray(structuredArray, index0).get(index1);
    }

    /**
     * Get an element at a supplied [index0, index1, index2] in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    static <T> T get(final StructuredArray<T> structuredArray, final int index0, final int index1, final int index2)
            throws IllegalArgumentException {
        if (structuredArray.getDimensionCount() != 3) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }

        return getSubArray(structuredArray, index0).getSubArray(index1).get(index2);
    }

    /**
     * Get an element at a supplied [index] in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    static <T> T get(final StructuredArray<T> structuredArray, final long index)
            throws IllegalArgumentException {
        if (index < Integer.MAX_VALUE) {
            return get(structuredArray, (int) index);
        }
        if (structuredArray.getDimensionCount() != 1) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> StructuredArray.MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)(longIndex & StructuredArray.MASK);

        return structuredArray.getLongAddressableElements()[partitionIndex][partitionOffset];
    }

    /**
     * Get an element at a supplied [index0, index1] in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    static <T> T get(final StructuredArray<T> structuredArray, final long index0, final long index1)
            throws IllegalArgumentException {
        if (structuredArray.getDimensionCount() != 2) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }

        return getSubArray(structuredArray, index0).get(index1);
    }

    /**
     * Get an element at a supplied [index0, index1, index2] in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    static <T> T get(final StructuredArray<T> structuredArray, final long index0, final long index1, final long index2)
            throws IllegalArgumentException {
        if (structuredArray.getDimensionCount() != 3) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }

        return getSubArray(structuredArray, index0).getSubArray(index1).get(index2);
    }

    /**
     * Get a reference to an element in an N dimensional array, using indices supplied in a
     * <code>long[N + indexOffset]</code> array.
     * indexOffset indicates the starting point in the array at which the first index should be found.
     * This form is useful when passing index arrays through multiple levels to avoid construction of
     * temporary varargs containers or construction of new shorter index arrays.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    static <T> T get(final StructuredArray<T> structuredArray, final long[] indices, final int indexOffset) throws IllegalArgumentException {
        int dimensionCount = structuredArray.getDimensionCount();
        if ((indices.length - indexOffset) != dimensionCount) {
            throw new IllegalArgumentException("number of relevant elements in indices must match array dimension count");
        }

        if (dimensionCount == 1) {
            return get(structuredArray, indices[indexOffset]);
        } else {
            StructuredArray<T> subArray = getSubArray(structuredArray, indices[indexOffset]);
            return get(subArray, indices, indexOffset + 1);
        }
    }

    /**
     * Get a reference to an element in an N dimensional array, using indices supplied in a
     * <code>int[N + indexOffset]</code> array.
     * indexOffset indicates the starting point in the array at which the first index should be found.
     * This form is useful when passing index arrays through multiple levels to avoid construction of
     * temporary varargs containers or construction of new shorter index arrays.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    static <T> T get(final StructuredArray<T> structuredArray, final int[] indices, final int indexOffset) throws IllegalArgumentException {
        int dimensionCount = structuredArray.getDimensionCount();
        if ((indices.length - indexOffset) != dimensionCount) {
            throw new IllegalArgumentException("number of relevant elements in indices must match array dimension count");
        }

        if (dimensionCount == 1) {
            return get(structuredArray, indices[indexOffset]);
        } else {
            StructuredArray<T> subArray = getSubArray(structuredArray, indices[indexOffset]);
            return get(subArray, indices, indexOffset + 1);
        }
    }

    /**
     * Get a StructuredArray Sub array at a supplied index in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    @SuppressWarnings("unchecked")
    static <T> StructuredArray<T> getSubArray(final StructuredArray<T> structuredArray,
                                              final long index) throws IllegalArgumentException {
        if (index < Integer.MAX_VALUE) {
            return getSubArray(structuredArray, (int) index);
        }

        if (structuredArray.getDimensionCount() < 2) {
            throw new IllegalArgumentException("cannot call getSubArrayL() on single dimensional array");
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> StructuredArray.MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & StructuredArray.MASK;

        return structuredArray.getLongAddressableSubArrays()[partitionIndex][partitionOffset];
    }

    /**
     * Get a StructuredArray Sub array at a supplied index in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     */
    @SuppressWarnings("unchecked")
    static <T> StructuredArray<T> getSubArray(final StructuredArray<T> structuredArray,
                                              final int index) throws IllegalArgumentException {
        if (structuredArray.getDimensionCount() < 2) {
            throw new IllegalArgumentException("cannot call getSubArray() on single dimensional array");
        }

        return structuredArray.getIntAddressableSubArrays()[index];
    }
 }
