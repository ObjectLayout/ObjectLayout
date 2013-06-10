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
 *     the form of {@link org.ObjectLayout.UnifiedStructuredArray#get}() (for {@link int} indices) or {@link org.ObjectLayout.UnifiedStructuredArray#getL}()
 *     (for {@link long} indices). Individual element contents can then be accessed and manipulated using any and all
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
 * <p>
 *     Note: At least for some JVM implementations, {@link org.ObjectLayout.UnifiedStructuredArray#get}() access may be faster than
 *     {@link org.ObjectLayout.UnifiedStructuredArray#getL}() access.
 *
 * @param <T> type of the element occupying each array slot.
 */
public final class UnifiedStructuredArray<T> implements Iterable<T> {

    private static final int MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT = 30;
    private static final int MAX_EXTRA_PARTITION_SIZE = 1 << MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT;
    private static final int MASK = MAX_EXTRA_PARTITION_SIZE - 1;

    private final Field[] fields;
    private final boolean hasFinalFields;
    private final Class<T> elementClass;

    private final long[] lengths;

    private final long length;
    private final int numOfDimensions;

    // Separated internal arrays by type for performance reasons, to avoid casting and checkcast at runtime.
    // Wrong dimension count gets (of the wrong type for the dimension depth) will result in NPEs rather
    // than class cast exceptions.

    private final UnifiedStructuredArray<T>[][] longAddressableSubArrays; // Used to store subArrays at indexes above Integer.MAX_VALUE
    private final UnifiedStructuredArray<T>[] intAddressableSubArrays;

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
    public static <T> UnifiedStructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long length) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator = new FixedConstructorAndArgsLocator<T>(elementClass);
        long[] lengths = { length };
        return new UnifiedStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);

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
    public static <T> UnifiedStructuredArray<T> newInstance(final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                                     final long length) throws NoSuchMethodException {
        long[] lengths = { length };
        return new UnifiedStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);    }

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
    public static <T> UnifiedStructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long length,
                                                     final Class[] initArgTypes,
                                                     final Object... initArgs) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator =
                new FixedConstructorAndArgsLocator<T>(elementClass, initArgTypes, initArgs);
        long[] lengths = { length };
        return new UnifiedStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
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
    public static <T> UnifiedStructuredArray<T> newInstance(final Class elementClass,
                                                                     final Long... lengths) throws NoSuchMethodException {
        final ConstructorAndArgsLocator constructorAndArgsLocator = new FixedConstructorAndArgsLocator(elementClass);
        return new UnifiedStructuredArray<T>(lengths.length, constructorAndArgsLocator, LongArrayToPrimitiveLongArray(lengths), null);
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
    public static <T> UnifiedStructuredArray<T> newInstance(final Class<T> elementClass,
                                                                     final long[] lengths) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator = new FixedConstructorAndArgsLocator<T>(elementClass);
        return new UnifiedStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
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
    public static <T> UnifiedStructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long[] lengths,
                                                     final Class[] initArgTypes,
                                                     final Object... initArgs) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator =
                new FixedConstructorAndArgsLocator<T>(elementClass, initArgTypes, initArgs);
        return new UnifiedStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
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
    public static <T> UnifiedStructuredArray<T> newInstance(final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                                     final Long... lengths) throws NoSuchMethodException {
        return new UnifiedStructuredArray<T>(lengths.length, constructorAndArgsLocator, LongArrayToPrimitiveLongArray(lengths), null);
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
    public static <T> UnifiedStructuredArray<T> newInstance(final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                                                     final long[] lengths)
            throws NoSuchMethodException {
        return new UnifiedStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
    }

    /**
     * Copy an array of elements to a newly created array. Copying of individual elements is done by using
     * the <code>elementClass</code> copy constructor to construct the individual member elements of the new
     * array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to duplicate.
     * @throws NoSuchMethodException if the element class does not have a public copy constructor.
     */
    public static <T> UnifiedStructuredArray<T> copyInstance(final UnifiedStructuredArray<T> source)
            throws NoSuchMethodException {
        return copyInstance(source, new long[source.getNumOfDimensions()], source.getLengths());
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
    public static <T> UnifiedStructuredArray<T> copyInstance(final UnifiedStructuredArray<T> source,
                                                                      final long[] sourceOffsets,
                                                                      Long... counts) throws NoSuchMethodException {
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
    public static <T> UnifiedStructuredArray<T> copyInstance(final UnifiedStructuredArray<T> source,
                                                                      final long[] sourceOffsets,
                                                                      long[] counts) throws NoSuchMethodException {
        if (source.getNumOfDimensions() != sourceOffsets.length) {
            throw new IllegalArgumentException("source.getNumDimensions() must match sourceOffsets.length");
        }
        if (counts.length != source.getNumOfDimensions()) {
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
                 new UnifiedCopyConstructorAndArgsLocator<T>(source.getElementClass(), source, sourceOffsets, false);

        return new UnifiedStructuredArray<T>(source.getNumOfDimensions(),
                constructorAndArgsLocator, counts, null);
    }


    @SuppressWarnings("unchecked")
    private UnifiedStructuredArray(final int numOfDimensions,
                                   final ConstructorAndArgsLocator constructorAndArgsLocator,
                                   final long[] lengths,
                                   final long[] containingIndexes) throws NoSuchMethodException {
        if (numOfDimensions < 1) {
            throw new IllegalArgumentException("numOfDimensions must be at least 1");
        }

        this.numOfDimensions = numOfDimensions;
        this.lengths = lengths;
        this.length = lengths[0];

        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
        }

        if (lengths.length != numOfDimensions) {
            throw new IllegalArgumentException("number of lengths provided (" + lengths.length +
                    ") does not match numDimensions (" + numOfDimensions + ")");
        }

        this.elementClass = constructorAndArgsLocator.getElementClass();

        final Field[] fields = removeStaticFields(elementClass.getDeclaredFields());
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        this.fields = fields;
        this.hasFinalFields = containsFinalQualifiedFields(fields);

        if (numOfDimensions > 1) {
            // We have sub arrays, not elements:
            intAddressableElements = null;
            longAddressableElements = null;

            // int-addressable sub arrays:
            final int intLength = (int) Math.min(length, Integer.MAX_VALUE);
            intAddressableSubArrays = (UnifiedStructuredArray<T>[])new UnifiedStructuredArray[intLength];

            // Subsequent partitions hold long-addressable-only sub arrays:
            final long extraLength = length - intLength;
            final int numFullPartitions = (int)(extraLength >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
            final int lastPartitionSize = (int)extraLength & MASK;

            longAddressableSubArrays = (UnifiedStructuredArray<T>[][])new UnifiedStructuredArray[numFullPartitions + 1][];
            // full long-addressable-only partitions:
            for (int i = 0; i < numFullPartitions; i++) {
                longAddressableSubArrays[i] = (UnifiedStructuredArray<T>[])new Object[MAX_EXTRA_PARTITION_SIZE];
            }
            // Last partition with leftover long-addressable-only size:
            longAddressableSubArrays[numFullPartitions] = (UnifiedStructuredArray<T>[])new UnifiedStructuredArray[lastPartitionSize];

            // This is an array of arrays. Pass the constructorAndArgsLocator through to
            // a subArrayConstructorAndArgsLocator that will be used to populate the sub-array:
            final long[] subArrayLengths = new long[lengths.length - 1];
            for (int i = 0; i < subArrayLengths.length; i++) {
                subArrayLengths[i] = lengths[i+1];
            }
            final Class[] subArrayArgTypes = { Integer.TYPE, ConstructorAndArgsLocator.class,
                    long[].class, long[].class};
            final Object[] subArrayArgs = {numOfDimensions - 1, constructorAndArgsLocator,
                    subArrayLengths, null /* containingIndexes arg goes here */};
            Class targetClass = UnifiedStructuredArray.class;
            Constructor<UnifiedStructuredArray<T>> constructor =
                    targetClass.getDeclaredConstructor(subArrayArgTypes);
            ConstructorAndArgsLocator<UnifiedStructuredArray<T>> subArrayConstructorAndArgsLocator =
                    new ArrayConstructorAndArgsLocator<UnifiedStructuredArray<T>>(constructor, subArrayArgs, 3);
            populateElements(subArrayConstructorAndArgsLocator,
                    intAddressableSubArrays, longAddressableSubArrays, containingIndexes);
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
            populateElements(constructorAndArgsLocator,
                    intAddressableElements, longAddressableElements, containingIndexes);
        }
    }


    /**
     * Get the number of dimensions of the array.
     *
     * @return the number of dimensions of the array.
     */
    public int getNumOfDimensions() {
        return numOfDimensions;
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
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T getL(long[] indexes) {
        return getL(indexes, 0);
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
     * @throws NullPointerException if number of relevant indexes does not match number of dimensions in the array
     */
    public T getL(long[] indexes, int indexOffset) {
        if ((indexes.length - indexOffset) != numOfDimensions) {
            throw new NullPointerException("number of relevant elements in indexes must match numOfDimensions");
        }
        if (numOfDimensions > 1) {
            UnifiedStructuredArray<T> containedArray = getOfUnifiedStructuredArrayL(indexes[indexOffset]);
            return containedArray.getL(indexes, indexOffset + 1);

        } else {
            return getL(indexes[indexOffset]);
        }
    }

    /**
     * Get a reference to an element in the array, using a varargs long indexes.
     *
     * @param indexes The indexes (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T getL(Long... indexes) {
        return getL(LongArrayToPrimitiveLongArray(indexes));

    }

    /**
     * Get a reference to an element in the array, using a varargs int indexes.
     *
     * @param indexes The indexes (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(Integer... indexes) {
        return getL(IntegerArrayToPrimitiveLongArray(indexes));

    }

    // fast long index variants:

    /**
     * Get a reference to an element in the array, using 2 <code>long</code> indexes.
     * @param index0
     * @param index1
     * @return the element at [index0, index1]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T getL(long index0, long index1) {
        return getOfUnifiedStructuredArrayL(index0).getL(index1);
    }

    /**
     * Get a reference to an element in the array, using 3 <code>long</code> indexes.
     * @param index0
     * @param index1
     * @param index2
     * @return the element at [index0, index1, index2]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T getL(long index0, long index1, long index2) {
        UnifiedStructuredArray<T> level0element = getOfUnifiedStructuredArrayL(index0);
        UnifiedStructuredArray<T> level1element = level0element.getOfUnifiedStructuredArrayL(index1);
        return level1element.getL(index2);
    }

    /**
     * Get a reference to an element in the array, using 4 <code>long</code> indexes.
     * @param index0
     * @param index1
     * @param index2
     * @param index3
     * @return the element at [index0, index1, index2, index3]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T getL(long index0, long index1, long index2, long index3) {
        UnifiedStructuredArray<T> level0element = getOfUnifiedStructuredArrayL(index0);
        UnifiedStructuredArray<T> level1element = level0element.getOfUnifiedStructuredArrayL(index1);
        UnifiedStructuredArray<T> level2element = level1element.getOfUnifiedStructuredArrayL(index2);
        return level2element.getL(index3);
    }

    // fast int index variants:

    /**
     * Get a reference to an element in the array, using 2 <code>int</code> indexes.
     * @param index0
     * @param index1
     * @return the element at [index0, index1]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(int index0, int index1) {
        return getOfUnifiedStructuredArray(index0).getL(index1);
    }

    /**
     * Get a reference to an element in the array, using 3 <code>int</code> indexes.
     * @param index0
     * @param index1
     * @param index2
     * @return the element at [index0, index1, index2]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(int index0, int index1, int index2) {
        UnifiedStructuredArray<T> level0element = getOfUnifiedStructuredArray(index0);
        UnifiedStructuredArray<T> level1element = level0element.getOfUnifiedStructuredArray(index1);
        return level1element.get(index2);
    }

    /**
     * Get a reference to an element in the array, using 4 <code>int</code> indexes.
     * @param index0
     * @param index1
     * @param index2
     * @param index3
     * @return the element at [index0, index1, index2, index3]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(int index0, int index1, int index2, int index3) {
        UnifiedStructuredArray<T> level0element = getOfUnifiedStructuredArray(index0);
        UnifiedStructuredArray<T> level1element = level0element.getOfUnifiedStructuredArray(index1);
        UnifiedStructuredArray<T> level2element = level1element.getOfUnifiedStructuredArray(index2);
        return level2element.get(index3);
    }

    // Type specific public gets of first dimension:

    /**
     * Get a reference to a UnifiedStructuredArray element in this array, using a <code>long</code> index.
     * @param index
     * @return a reference to the UnifiedStructuredArray located in element [index] of this array
     * @throws NullPointerException if array has less than two dimensions
     */
    @SuppressWarnings("unchecked")
    public UnifiedStructuredArray<T> getOfUnifiedStructuredArrayL(final long index) {
        if (index < Integer.MAX_VALUE) {
            return getOfUnifiedStructuredArray((int) index);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & MASK;

        return longAddressableSubArrays[partitionIndex][partitionOffset];
    }

    /**
     * Get a reference to a UnifiedStructuredArray element in this array, using a <code>int</code> index.
     * @param index
     * @return a reference to the UnifiedStructuredArray located in element [index] of this array
     * @throws NullPointerException if array has less than two dimensions
     */
    @SuppressWarnings("unchecked")
    public UnifiedStructuredArray<T> getOfUnifiedStructuredArray(final int index) {
        return intAddressableSubArrays[index];
    }

    /**
     * Get a reference to an element in the array, using a <code>long</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws NullPointerException if array has more than one dimensions
     */
    public T getL(final long index) {
        if (index < Integer.MAX_VALUE) {
            return get((int) index);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & MASK;

        return longAddressableElements[partitionIndex][partitionOffset];
    }

    /**
     * Get a reference to an element in the array, using an <code>int</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws NullPointerException if array has more than one dimensions
     */
    public T get(final int index) {
        return intAddressableElements[index];
    }

    private <E> void populateElements(final ConstructorAndArgsLocator<E> constructorAndArgsLocator,
                                      E[] intAddressable,
                                      E[][] longAddressable,
                                  long[] containingIndexes) throws NoSuchMethodException {
        try {
            final long[] indexes;

            if (containingIndexes != null) {
                indexes = new long[containingIndexes.length + 1];
                for (int i = 0; i < containingIndexes.length; i++) {
                    indexes[i] = containingIndexes[i];
                }
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
        private final long[] cursors = new long[numOfDimensions];
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

            T t = getL(cursors);

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
         * Remove operation is not supported on {@link org.ObjectLayout.UnifiedStructuredArray}s.
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


    private static long[] LongArrayToPrimitiveLongArray(Long[] lengths) {
        long [] longLengths = new long[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            longLengths[i] = lengths[i];
        }
        return longLengths;
    }

    private static long[] IntegerArrayToPrimitiveLongArray(Integer[] lengths) {
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
    public static void shallowCopy(final UnifiedStructuredArray src, final long srcOffset,
                                   final UnifiedStructuredArray dst, final long dstOffset,
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
     * @throws IllegalArgumentException if final fields are discovered and all allowFinalFieldOverwrite is not true.
     * @throws IllegalArgumentException if source or destination arrays have more than one dimension.
     * @throws ArrayStoreException if the element classes in src and dst are not identical.
     */
    public static void shallowCopy(final UnifiedStructuredArray src, final long srcOffset,
                                   final UnifiedStructuredArray dst, final long dstOffset,
                                   final long count,
                                   final boolean allowFinalFieldOverwrite) {
        if (src.elementClass != dst.elementClass) {
            throw new ArrayStoreException(String.format("Only objects of the same class can be copied: %s != %s",
                    src.getClass(), dst.getClass()));
        }
        if ((src.numOfDimensions > 1) || (dst.numOfDimensions > 1)) {
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
                    reverseShallowCopy(src.getL(srcIdx), dst.getL(dstIdx), fields);
                }
            } else {
                for (long srcIdx = srcOffset, dstIdx = dstOffset, limit = srcOffset + count;
                     srcIdx < limit; srcIdx++, dstIdx++) {
                    shallowCopy(src.getL(srcIdx), dst.getL(dstIdx), fields);
                }
            }
        }
    }
}
