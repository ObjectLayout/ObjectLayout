/*
 * Copyright 2013 Gil Tene
 * Copyright 2012, 2013 Martin Thompson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;

/**
 *     An array of (potentially) mutable but non-replaceable objects.
 * <p>
 *     A structured array contains array element objects of a fixed (at creation time, per array instance) class,
 *     and can support elements of any class that provides public constructors. The elements in a StructuredArray
 *     are all allocated and constructed at array creation time, and individual elements cannot be removed or
 *     replaced after array creation. Array elements can be accessed using an index-based accessor methods in
 *     the form of {@link StructuredArray#get}() using either {@link int} or
 *     {@link long} indices. Individual element contents can then be accessed and manipulated using any and all
 *     operations supported by the member element's class.
 * <p>
 *     While simple creation of default-constructed elements and fixed constructor parameters are available through
 *     the newInstance factory methods, supporting arbitrary member types requires a wider range of construction
 *     options. The ConstructorAndArgsLocator API provides for array creation with arbitrary, user-supplied
 *     constructors and arguments, either of which can take the element index into account.
 * <p>
 *     StructuredArray is designed with semantics specifically restricted to be consistent with layouts of an
 *     array of structures in C-like languages. While fully functional on all JVM implementation (of Java SE 5
 *     and above), the semantics are such that a JVM may transparently optimise the implementation to provide a
 *     compact contiguous layout that facilitates consistent stride based memory access and dead-reckoning
 *     (as opposed to de-referenced) access to elements
 *
 * @param <T> type of the element occupying each array slot.
 */
public final class StructuredArray<T> implements Iterable<T> {

    private static final int MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT = 30;
    private static final int MAX_EXTRA_PARTITION_SIZE = 1 << MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT;
    private static final int MASK = MAX_EXTRA_PARTITION_SIZE - 1;

    private final Field[] fields;
    private final boolean hasFinalFields;
    private final Class<T> elementClass;

    private final long[] lengths;
    private final long length;      // A cached lengths[0]

    private final int dimensionCount;

    // Separated internal storage arrays by type for performance reasons, to avoid casting and checkcast at runtime.
    // Wrong dimension count gets (of the wrong type for the dimension depth) will result in NPEs rather
    // than class cast exceptions.

    private final StructuredArray<T>[][] longAddressableSubArrays; // Used to store subArrays at indexes above Integer.MAX_VALUE
    private final StructuredArray<T>[] intAddressableSubArrays;

    private final T[][] longAddressableElements; // Used to store elements at indexes above Integer.MAX_VALUE
    private final T[] intAddressableElements;

    // Single-dimensional newInstance forms:

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @throws NoSuchMethodException if the element class does not have a public default constructor
     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long length) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator = new FixedConstructorAndArgsLocator<T>(elementClass);
        final long[] lengths = {length};
        return new StructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);

    }

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Use constructor and arguments supplied (on a potentially
     * per element index basis) by the specified <code>constructorAndArgsLocator</code> to construct and initialize
     * each element.
     *
     * @param length of the array to create.
     * @param constructorAndArgsLocator produces element constructors [potentially] on a per element basis.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                                     final long length) throws NoSuchMethodException {
        final long[] lengths = {length};
        return new StructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);    }

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Use a fixed (same for all elements) constructor identified by the argument
     * classes specified in  <code>initArgs</code> to construct and initialize each element, passing the remaining
     * arguments to that constructor.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @param initArgTypes for selecting the constructor to call for initialising each structure object.
     * @param initArgs to be passed to a constructor for initialising each structure object.
     * @throws IllegalArgumentException if initArgTypes and constructor arguments do not match in length
     * @throws NoSuchMethodException if initArgTypes does not match a public constructor signature in elementClass

     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long length,
                                                     final Class[] initArgTypes,
                                                     final Object... initArgs) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator =
                new FixedConstructorAndArgsLocator<T>(elementClass, initArgTypes, initArgs);
        final long[] lengths = {length};
        return new StructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
    }

    // Multi-dimensional newInstance forms:

    /**
     * Create a multi dimensional array of elements. Each dimension of the array will be of a length designated
     * in the <code>lengths</code> parameters passed. Each element of the array will consist of
     * an object of type <code>elementClass</code>. Elements will be constructed Using the
     * <code>elementClass</code>'s default constructor.
     *
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final Long... lengths) throws NoSuchMethodException {
        final ConstructorAndArgsLocator constructorAndArgsLocator = new FixedConstructorAndArgsLocator<T>(elementClass);
        return new StructuredArray<T>(lengths.length, constructorAndArgsLocator, LongArrayToPrimitiveLongArray(lengths), null);
    }

    /**
     * Create a multi dimensional array of elements. Each dimension of the array will be of a length designated
     * in the <code>lengths[]</code>  passed. Each element of the array will consist of
     * an object of type <code>elementClass</code>. Elements will be constructed Using the
     * <code>elementClass</code>'s default constructor.
     *
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long[] lengths) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator = new FixedConstructorAndArgsLocator<T>(elementClass);
        return new StructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
    }

    /**
     * Create a multi dimensional array of elements. Each dimension of the array will be of a length designated
     * in the <code>lengths[]</code>  passed. Each element of the array will consist of
     * an object of type <code>elementClass</code>. Elements will be constructed using a fixed (same for all elements)
     * constructor identified by the argument classes specified in  <code>initArgTypes</code> to construct and
     * initialize each element, passing the <code>initArgs</code> arguments to that constructor.
     *
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     * @param initArgTypes for selecting the constructor to call for initialising each structure object.
     * @param initArgs to be passed to a constructor for initialising each structure object.
     * @throws IllegalArgumentException if initArgTypes and constructor arguments do not match in length
     * @throws NoSuchMethodException if initArgTypes does not match a public constructor signature in elementClass

     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long[] lengths,
                                                     final Class[] initArgTypes,
                                                     final Object... initArgs) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator =
                new FixedConstructorAndArgsLocator<T>(elementClass, initArgTypes, initArgs);
        return new StructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
    }

    /**
     * Create a multi dimensional array of elements. Each dimension of the array will be of a length designated
     * in the <code>lengths</code> arguments passed. Each element of the array will consist of
     * an object of type <code>elementClass</code>. Elements will be constructed using the constructor and arguments
     * supplied (on a potentially per element index basis) by the specified <code>constructorAndArgsLocator</code>.
     * to construct and initialize each element.
     *
     * @param constructorAndArgsLocator produces element constructors [potentially] on a per element basis.
     * @param lengths of the array dimensions to create.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                                     final Long... lengths) throws NoSuchMethodException {
        return new StructuredArray<T>(lengths.length, constructorAndArgsLocator, LongArrayToPrimitiveLongArray(lengths), null);
    }

    /**
     * Create a multi dimensional array of elements. Each dimension of the array will be of a length designated
     * in the <code>lengths[]</code> passed. Each element of the array will consist of
     * an object of type <code>elementClass</code>. Elements will be constructed using the constructor and arguments
     * supplied (on a potentially per element index basis) by the specified <code>constructorAndArgsLocator</code>.
     * to construct and initialize each element.
     *
     * @param constructorAndArgsLocator produces element constructors [potentially] on a per element basis.
     * @param lengths of the array dimensions to create.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                                     final long[] lengths) throws NoSuchMethodException {
        return new StructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
    }

    /**
     * Copy an array of elements to a newly created array. Copying of individual elements is done by using
     * the <code>elementClass</code> copy constructor to construct the individual member elements of the new
     * array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to duplicate.
     * @throws NoSuchMethodException if the element class does not have a public copy constructor.
     */
    public static <T> StructuredArray<T> copyInstance(final StructuredArray<T> source) throws NoSuchMethodException {
        return copyInstance(source, new long[source.getDimensionCount()], source.getLengths());
    }

    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to copy from.
     * @param sourceOffsets offset indexes for each dimension, indicating where the source region to be copied begins.
     * @param counts the number of elements in each dimension to copy.
     * @throws NoSuchMethodException if the element class does not have a public copy constructor.
     */
    public static <T> StructuredArray<T> copyInstance(final StructuredArray<T> source,
                                                      final long[] sourceOffsets,
                                                      final Long... counts) throws NoSuchMethodException {
        return copyInstance(source, sourceOffsets, LongArrayToPrimitiveLongArray(counts));
    }
    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to copy from.
     * @param sourceOffsets offset indexes for each dimension, indicating where the source region to be copied begins.
     * @param counts the number of elements in each dimension to copy.
     * @throws NoSuchMethodException if the element class does not have a public copy constructor.
     */
    public static <T> StructuredArray<T> copyInstance(final StructuredArray<T> source,
                                                      final long[] sourceOffsets,
                                                      final long[] counts) throws NoSuchMethodException {
        if (source.getDimensionCount() != sourceOffsets.length) {
            throw new IllegalArgumentException("source.getNumDimensions() must match sourceOffsets.length");
        }
        if (counts.length != source.getDimensionCount()) {
            throw new IllegalArgumentException("source.getNumDimensions() must match counts.length");
        }
        for (int i = 0; i < counts.length; i++) {
            if (source.getLengths()[i] < sourceOffsets[i] + counts[i]) {
                throw new ArrayIndexOutOfBoundsException(
                        "Dimension " + i + ": source " + source + " length of " + source.getLengths()[i] +
                                " is smaller than sourceOffset (" + sourceOffsets[i] + ") + count (" + counts[i] + ")" );
            }
        }

        final ConstructorAndArgsLocator<T> constructorAndArgsLocator =
                 new CopyConstructorAndArgsLocator<T>(source.getElementClass(), source, sourceOffsets, false);

        return new StructuredArray<T>(source.getDimensionCount(),
                constructorAndArgsLocator, counts, null);
    }


    @SuppressWarnings("unchecked")
    private StructuredArray(final int dimensionCount,
                            final ConstructorAndArgsLocator constructorAndArgsLocator,
                            final long[] lengths,
                            final long[] containingIndexes) throws NoSuchMethodException {
        if (dimensionCount < 1) {
            throw new IllegalArgumentException("dimensionCount must be at least 1");
        }

        this.dimensionCount = dimensionCount;
        this.lengths = lengths;
        this.length = lengths[0];

        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
        }

        if (lengths.length != dimensionCount) {
            throw new IllegalArgumentException("number of lengths provided (" + lengths.length +
                    ") does not match numDimensions (" + dimensionCount + ")");
        }

        this.elementClass = constructorAndArgsLocator.getElementClass();

        final Field[] fields = removeStaticFields(elementClass.getDeclaredFields());
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        this.fields = fields;
        this.hasFinalFields = containsFinalQualifiedFields(fields);

        if (dimensionCount > 1) {
            // We have sub arrays, not elements:
            intAddressableElements = null;
            longAddressableElements = null;

            // int-addressable sub arrays:
            final int intLength = (int) Math.min(length, Integer.MAX_VALUE);
            intAddressableSubArrays = new StructuredArray[intLength];

            // Subsequent partitions hold long-addressable-only sub arrays:
            final long extraLength = length - intLength;
            final int numFullPartitions = (int)(extraLength >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
            final int lastPartitionSize = (int)extraLength & MASK;

            longAddressableSubArrays = new StructuredArray[numFullPartitions + 1][];
            // full long-addressable-only partitions:
            for (int i = 0; i < numFullPartitions; i++) {
                longAddressableSubArrays[i] = (StructuredArray<T>[])new Object[MAX_EXTRA_PARTITION_SIZE];
            }
            // Last partition with leftover long-addressable-only size:
            longAddressableSubArrays[numFullPartitions] = new StructuredArray[lastPartitionSize];

            // This is an array of arrays. Pass the constructorAndArgsLocator through to
            // a subArrayConstructorAndArgsLocator that will be used to populate the sub-array:
            final long[] subArrayLengths = new long[lengths.length - 1];
            System.arraycopy(lengths, 1, subArrayLengths, 0, subArrayLengths.length);

            final Class[] subArrayArgTypes = {Integer.TYPE, ConstructorAndArgsLocator.class,
                                              long[].class, long[].class};
            final Object[] subArrayArgs = {dimensionCount - 1, constructorAndArgsLocator,
                                           subArrayLengths, null /* containingIndexes arg goes here */};
            final Class targetClass = StructuredArray.class;
            final Constructor<StructuredArray<T>> constructor = targetClass.getDeclaredConstructor(subArrayArgTypes);
            final ConstructorAndArgsLocator<StructuredArray<T>> subArrayConstructorAndArgsLocator =
                    new ArrayConstructorAndArgsLocator<StructuredArray<T>>(constructor, subArrayArgs, 3);

            populateElements(subArrayConstructorAndArgsLocator, intAddressableSubArrays,
                             longAddressableSubArrays, containingIndexes);
        } else {
            // We have elements, no sub arrays:
            intAddressableSubArrays = null;
            longAddressableSubArrays = null;

            // int-addressable elements:
            final int intLength = (int) Math.min(length, Integer.MAX_VALUE);
            intAddressableElements = (T[])new Object[intLength];

            // Subsequent partitions hold long-addressable-only elements:
            final long extraLength = length - intLength;
            final int numFullPartitions = (int)(extraLength >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
            final int lastPartitionSize = (int)extraLength & MASK;

            longAddressableElements = (T[][])new Object[numFullPartitions + 1][];
            // full long-addressable-only partitions:
            for (int i = 0; i < numFullPartitions; i++) {
                longAddressableElements[i] = (T[])new Object[MAX_EXTRA_PARTITION_SIZE];
            }
            // Last partition with leftover long-addressable-only size:
            longAddressableElements[numFullPartitions] = (T[])new Object[lastPartitionSize];

            // This is a single dimension array. Populate it:
            populateElements(constructorAndArgsLocator, intAddressableElements,
                             longAddressableElements, containingIndexes);
        }
    }

    /**
     * Get the number of dimensions of the array.
     *
     * @return the number of dimensions of the array.
     */
    public int getDimensionCount() {
        return dimensionCount;
    }

    /**
     * Get the lengths (number of elements per dimension) of the array.
     *
     * @return the number of elements in each dimension in the array.
     */
    public long[] getLengths() {
        return lengths;
    }

    /**
     * Get the total number of elements (in all dimensions combined) in this multi-dimensional array.
     *
     * @return the total number of elements (in all dimensions combined) in the array.
     */
    public long getTotalElementCount() {
        long totalElementCount = 1;
        for (long length : lengths) {
            totalElementCount *= length;
        }

        return totalElementCount;
    }

    /**
     * Get the length (number of elements) of the array.
     *
     * @return the number of elements in the array.
     */
    public long getLength() {
        return length;
    }

    // Variations on get():

    /**
     * Get a reference to an element in the array, using a <code>long[]</code> index array.
     *
     * @param indexes The indexes (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if number of indexes does not match number of dimensions in the array
     */
    public T get(final long[] indexes) throws IllegalArgumentException {
        return get(indexes, 0);
    }

    /**
     * Get a reference to an element in the array, using <code>long</code> indexes supplied in an array.
     * indexOffset indicates the starting point in the array at which the first index should be found.
     * This form is useful when passing index arrays through multiple levels to avoid construction of
     * temporary varargs containers or construction of new shorter index arrays.
     *
     * @param indexes The indexes (at each dimension) of the element to retrieve.
     * @param indexOffset The beginning offset in the indexes array related to this arrays contents.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if number of relevant indexes does not match number of dimensions in the array
     */
    public T get(final long[] indexes, final int indexOffset) throws IllegalArgumentException {
        if ((indexes.length - indexOffset) != dimensionCount) {
            throw new IllegalArgumentException("number of relevant elements in indexes must match array dimension count");
        }
        if (dimensionCount > 1) {
            StructuredArray<T> containedArray = getSubArray(indexes[indexOffset]);
            return containedArray.get(indexes, indexOffset + 1);
        } else {
            return get(indexes[indexOffset]);
        }
    }

    /**
     * Get a reference to an element in the array, using a varargs long indexes.
     *
     * @param indexes The indexes (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if number of indexes does not match number of dimensions in the array
     */
    public T get(final Long... indexes) throws IllegalArgumentException {
        return get(LongArrayToPrimitiveLongArray(indexes));
    }

    /**
     * Get a reference to an element in the array, using a varargs int indexes.
     *
     * @param indexes The indexes (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if number of indexes does not match number of dimensions in the array
     */
    public T get(final Integer... indexes) throws IllegalArgumentException {
        return get(IntegerArrayToPrimitiveLongArray(indexes));
    }

    // fast long index element get variants:

    /**
     * Get a reference to an element in the array, using a <code>long</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if array has more than one dimensions
     */
    public T get(final long index) throws IllegalArgumentException {
        if (index < Integer.MAX_VALUE) {
            return get((int) index);
        }
        if (dimensionCount != 1) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & MASK;

        return longAddressableElements[partitionIndex][partitionOffset];
    }

    /**
     * Get a reference to an element in the array, using 2 <code>long</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @return the element at [index0, index1]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final long index0, final long index1) throws IllegalArgumentException {
        if (dimensionCount != 2) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }
        return getSubArray(index0).get(index1);
    }

    /**
     * Get a reference to an element in the array, using 3 <code>long</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @param index2 the third index (in the third array dimension) of the element to retrieve
     * @return the element at [index0, index1, index2]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final long index0, final long index1, final long index2) throws IllegalArgumentException {
        if (dimensionCount != 3) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }
        return getSubArray(index0).getSubArray(index1).get(index2);
    }

    /**
     * Get a reference to an element in the array, using 4 <code>long</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @param index2 the third index (in the third array dimension) of the element to retrieve
     * @param index3 the fourth index (in the fourth array dimension) of the element to retrieve
     * @return the element at [index0, index1, index2, index3]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final long index0, final long index1, final long index2, final long index3) throws IllegalArgumentException {
        if (dimensionCount != 4) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }
        return getSubArray(index0).getSubArray(index1).getSubArray(index2).get(index3);
    }

    // fast int index element get variants:


    /**
     * Get a reference to an element in the array, using an <code>int</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws NullPointerException if array has more than one dimensions
     */
    public T get(final int index) throws IllegalArgumentException {
        if (dimensionCount != 1) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }
        return intAddressableElements[index];
    }

    /**
     * Get a reference to an element in the array, using 2 <code>int</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @return the element at [index0, index1]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final int index0, final int index1) throws IllegalArgumentException {
        if (dimensionCount != 2) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }
        return getSubArray(index0).get(index1);
    }

    /**
     * Get a reference to an element in the array, using 3 <code>int</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @param index2 the third index (in the third array dimension) of the element to retrieve
     * @return the element at [index0, index1, index2]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final int index0, final int index1, final int index2) throws IllegalArgumentException {
        if (dimensionCount != 3) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }
        return getSubArray(index0).getSubArray(index1).get(index2);
    }

    /**
     * Get a reference to an element in the array, using 4 <code>int</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @param index2 the third index (in the third array dimension) of the element to retrieve
     * @param index3 the fourth index (in the fourth array dimension) of the element to retrieve
     * @return the element at [index0, index1, index2, index3]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final int index0, final int index1, final int index2, final int index3) throws IllegalArgumentException {
        if (dimensionCount != 4) {
            throw new IllegalArgumentException("number of index parameters to get() must match array dimension count");
        }
        return getSubArray(index0).getSubArray(index1).getSubArray(index2).get(index3);
    }

    // Type specific public gets of first dimension:

    /**
     * Get a reference to a StructuredArray element in this array, using a <code>long</code> index.
     * @param index (in this array's first dimension) of the StructuredArray to retrieve
     * @return a reference to the StructuredArray located at [index] in the first dimension of this array
     * @throws IllegalArgumentException if array has less than two dimensions
     */
    @SuppressWarnings("unchecked")
    public StructuredArray<T> getSubArray(final long index) throws IllegalArgumentException {
        if (index < Integer.MAX_VALUE) {
            return getSubArray((int) index);
        }

        if (dimensionCount < 2) {
            throw new IllegalArgumentException("cannot call getSubArrayL() on single dimensional array");
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & MASK;

        return longAddressableSubArrays[partitionIndex][partitionOffset];
    }

    /**
     * Get a reference to a StructuredArray element in this array, using a <code>int</code> index.
     * @param index (in this array's first dimension) of the StructuredArray to retrieve
     * @return a reference to the StructuredArray located at [index] in the first dimension of this array
     * @throws IllegalArgumentException if array has less than two dimensions
     */
    @SuppressWarnings("unchecked")
    public StructuredArray<T> getSubArray(final int index) throws IllegalArgumentException {
        if (dimensionCount < 2) {
            throw new IllegalArgumentException("cannot call getSubArray() on single dimensional array");
        }
        return intAddressableSubArrays[index];
    }

    private <E> void populateElements(final ConstructorAndArgsLocator<E> constructorAndArgsLocator,
                                      final E[] intAddressable,
                                      final E[][] longAddressable,
                                      long[] containingIndexes) throws NoSuchMethodException {
        try {
            final long[] indexes;

            if (containingIndexes != null) {
                indexes = new long[containingIndexes.length + 1];
                System.arraycopy(containingIndexes, 0, indexes, 0, containingIndexes.length);
            } else {
                indexes = new long[1];
            }

            final int thisIndex = indexes.length - 1;

            long index = 0;

            for (int i = 0; i < intAddressable.length; i++, index++) {
                indexes[thisIndex] = index;
                final ConstructorAndArgs<E> constructorAndArgs = constructorAndArgsLocator.getForIndexes(indexes);
                final Constructor<E> constructor = constructorAndArgs.getConstructor();
                intAddressable[i] = constructor.newInstance(constructorAndArgs.getConstructorArgs());
                constructorAndArgsLocator.recycle(constructorAndArgs);
            }

            for (final E[] partition : longAddressable) {
                indexes[thisIndex] = index;
                for (int i = 0, size = partition.length; i < size; i++, index++) {
                    final ConstructorAndArgs<E> constructorAndArgs = constructorAndArgsLocator.getForIndexes(indexes);
                    final Constructor<E> constructor = constructorAndArgs.getConstructor();
                    partition[i] = constructor.newInstance(constructorAndArgs.getConstructorArgs());
                    constructorAndArgsLocator.recycle(constructorAndArgs);
                }
            }
        } catch (final NoSuchMethodException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the {@link Class} of elements stored in the array.
     *
     * @return the {@link Class} of elements stored in the array.
     */
    public Class<T> getElementClass() {
        return elementClass;
    }

    /**
     * {@inheritDoc}
     */
    public StructureIterator iterator() {
        return new StructureIterator();
    }

    /**
     * Specialised {@link java.util.Iterator} with the ability to be {@link #reset()} enabling reuse.
     */
    public class StructureIterator implements Iterator<T> {

        private final long[] cursors = new long[dimensionCount];
        private long elementCountToCursor = 0;
        private final long totalElementCount = getTotalElementCount();

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return elementCountToCursor < totalElementCount;
        }

        /**
         * {@inheritDoc}
         */
        public T next() {
            if (elementCountToCursor >= totalElementCount) {
                throw new NoSuchElementException();
            }

            T t = get(cursors);

            // Increment cursors from inner-most dimension out:
            for (int cursorDimension = cursors.length - 1; cursorDimension >= 0; cursorDimension--) {
                if ((++cursors[cursorDimension]) < lengths[cursorDimension])
                    break;
                // This dimension wrapped. Reset to zero and continue to one dimension higher
                cursors[cursorDimension] = 0;
            }
            elementCountToCursor++;

            return t;
        }

        /**
         * Remove operation is not supported on {@link StructuredArray}s.
         *
         * @throws UnsupportedOperationException if called.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Reset to the beginning of the collection enabling reuse of the iterator.
         */
        public void reset() {
            for (int i = 0; i < cursors.length; i++) {
                cursors[i] = 0;
            }
            elementCountToCursor = 0;
        }

        public long[] getCursors() {
            return Arrays.copyOf(cursors, cursors.length);
        }
    }

    private static Field[] removeStaticFields(final Field[] declaredFields) {
        int staticFieldCount = 0;
        for (final Field field : declaredFields) {
            if (isStatic(field.getModifiers())) {
                staticFieldCount++;
            }
        }

        final Field[] instanceFields = new Field[declaredFields.length - staticFieldCount];
        int i = 0;
        for (final Field field : declaredFields) {
            if (!isStatic(field.getModifiers())) {
                instanceFields[i++] = field;
            }
        }

        return instanceFields;
    }

    private boolean containsFinalQualifiedFields(final Field[] fields) {
        for (final Field field : fields) {
            if (isFinal(field.getModifiers())) {
                return true;
            }
        }

        return false;
    }

    private static void shallowCopy(final Object src, final Object dst, final Field[] fields) {
        try {
            for (final Field field : fields) {
                field.set(dst, field.get(src));
            }
        } catch (final IllegalAccessException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    private static void reverseShallowCopy(final Object src, final Object dst, final Field[] fields) {
        try {
            for (int i = fields.length - 1; i >= 0; i--) {
                final Field field = fields[i];
                field.set(dst, field.get(src));
            }
        } catch (final IllegalAccessException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }


    private static long[] LongArrayToPrimitiveLongArray(final Long[] lengths) {
        long [] longLengths = new long[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            longLengths[i] = lengths[i];
        }
        return longLengths;
    }

    private static long[] IntegerArrayToPrimitiveLongArray(final Integer[] lengths) {
        long [] longLengths = new long[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            longLengths[i] = lengths[i];
        }
        return longLengths;
    }

    /**
     * Shallow copy a region of element object contents from one array to the other.
     * <p>
     * shallowCopy will copy all fields from each of the source elements to the corresponding fields in each
     * of the corresponding destination elements. If the same array is both the src and dst then the copy will
     * happen as if a temporary intermediate array was used.
     *
     * @param src array to copy.
     * @param srcOffset offset index in src where the region begins.
     * @param dst array into which the copy should occur.
     * @param dstOffset offset index in the dst where the region begins.
     * @param count of structure elements to copy.
     * @throws IllegalStateException if final fields are discovered.
     * @throws ArrayStoreException if the element classes in src and dst are not identical.
     */
    public static void shallowCopy(final StructuredArray src, final long srcOffset,
                                   final StructuredArray dst, final long dstOffset,
                                   final long count) {
        shallowCopy(src, srcOffset, dst, dstOffset, count, false);
    }

    /**
     * Shallow copy a region of element object contents from one array to the other.
     * <p>
     * shallowCopy will copy all fields from each of the source elements to the corresponding fields in each
     * of the corresponding destination elements. If the same array is both the src and dst then the copy will
     * happen as if a temporary intermediate array was used.
     *
     * If <code>allowFinalFieldOverwrite</code> is specified as <code>true</code>, even final fields will be copied.
     *
     * @param src array to copy.
     * @param srcOffset offset index in src where the region begins.
     * @param dst array into which the copy should occur.
     * @param dstOffset offset index in the dst where the region begins.
     * @param count of structure elements to copy.
     * @param allowFinalFieldOverwrite allow final fields to be overwritten during a copy operation.
     * @throws IllegalArgumentException if source or destination arrays have more than one dimension, or
     * if final fields are discovered and all allowFinalFieldOverwrite is not true.
     * @throws ArrayStoreException if the element classes in src and dst are not identical.
     */
    public static void shallowCopy(final StructuredArray src, final long srcOffset,
                                   final StructuredArray dst, final long dstOffset,
                                   final long count,
                                   final boolean allowFinalFieldOverwrite) {
        if (src.elementClass != dst.elementClass) {
            throw new ArrayStoreException(String.format("Only objects of the same class can be copied: %s != %s",
                    src.getClass(), dst.getClass()));
        }
        if ((src.dimensionCount > 1) || (dst.dimensionCount > 1)) {
            throw new IllegalArgumentException("shallowCopy only supported for single dimension arrays");
        }

        final Field[] fields = src.fields;
        if (!allowFinalFieldOverwrite && dst.hasFinalFields) {
            throw new IllegalArgumentException("Cannot shallow copy onto final fields");
        }

        if (((srcOffset + count) < Integer.MAX_VALUE) && ((dstOffset + count) < Integer.MAX_VALUE)) {
            // use the (faster) int based get
            if (dst == src && (dstOffset >= srcOffset && (dstOffset + count) >= srcOffset)) {
                for (int srcIdx = (int)(srcOffset + count), dstIdx = (int)(dstOffset + count), limit = (int)(srcOffset - 1);
                     srcIdx > limit; srcIdx--, dstIdx--) {
                    reverseShallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
                }
            } else {
                for (int srcIdx = (int)srcOffset, dstIdx = (int)dstOffset, limit = (int)(srcOffset + count);
                     srcIdx < limit; srcIdx++, dstIdx++) {
                    shallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
                }
            }
        } else {
            // use the (slower) long based getL
            if (dst == src && (dstOffset >= srcOffset && (dstOffset + count) >= srcOffset)) {
                for (long srcIdx = srcOffset + count, dstIdx = dstOffset + count, limit = srcOffset - 1;
                     srcIdx > limit; srcIdx--, dstIdx--) {
                    reverseShallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
                }
            } else {
                for (long srcIdx = srcOffset, dstIdx = dstOffset, limit = srcOffset + count;
                     srcIdx < limit; srcIdx++, dstIdx++) {
                    shallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
                }
            }
        }
    }
}
