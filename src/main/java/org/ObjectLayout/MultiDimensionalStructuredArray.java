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
 *     and can support elements of any class that provides public constructors. The elements in a MultiDimensionalStructuredArray
 *     are all allocated and constructed at array creation time, and individual elements cannot be removed or
 *     replaced after array creation. Array elements can be accessed using an index-based accessor methods in
 *     the form of {@link MultiDimensionalStructuredArray#get}() (for {@link int} indices) or {@link MultiDimensionalStructuredArray#getL}()
 *     (for {@link long} indices). Individual element contents can then be accessed and manipulated using any and all
 *     operations supported by the member element's class.
 * <p>
 *     While simple creation of default-constructed elements and fixed constructor parameters are available through
 *     the newInstance factory methods, supporting arbitrary member types requires a wider range of construction
 *     options. The ConstructorAndArgsLocator API provides for array creation with arbitrary, user-supplied
 *     constructors and arguments, either of which can take the element index into account.
 * <p>
 *     MultiDimensionalStructuredArray is designed with semantics specifically restricted to be consistent with layouts of an
 *     array of structures in C-like languages. While fully functional on all JVM implementation (of Java SE 5
 *     and above), the semantics are such that a JVM may transparently optimise the implementation to provide a
 *     compact contiguous layout that facilitates consistent stride based memory access and dead-reckoning
 *     (as opposed to de-referenced) access to elements
 * <p>
 *     Note: At least for some JVM implementations, {@link MultiDimensionalStructuredArray#get}() access may be faster than
 *     {@link MultiDimensionalStructuredArray#getL}() access.
 *
 * @param <T> type of the element occupying each array slot.
 */
public final class MultiDimensionalStructuredArray<T> implements Iterable<T> {

    private static final int MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT = 30;
    private static final int MAX_EXTRA_PARTITION_SIZE = 1 << MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT;
    private static final int MASK = MAX_EXTRA_PARTITION_SIZE - 1;

    private final Class<T> elementClass;

    private final long[] lengths;

    private final long length;
    private final int numOfDimensions;
    private final Object[][] longAddressableElements; // Used to store elements at indexes above Integer.MAX_VALUE
    private final Object[] intAddressableElements;

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
    public static <T> MultiDimensionalStructuredArray<T> newInstance(final Class<T> elementClass,
                                                                     final Long... lengths) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator = new FixedConstructorAndArgsLocator<T>(elementClass);
        return new MultiDimensionalStructuredArray<T>(lengths.length, constructorAndArgsLocator,
                                                      LongArrayToPrimitiveLongArray(lengths), null);
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
    public static <T> MultiDimensionalStructuredArray<T> newInstance(final Class<T> elementClass,
                                                                     final long[] lengths) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator = new FixedConstructorAndArgsLocator<T>(elementClass);
        return new MultiDimensionalStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
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
    public static <T> MultiDimensionalStructuredArray<T> newInstance(final Class<T> elementClass,
                                                                     final long[] lengths,
                                                                     final Class[] initArgTypes,
                                                                     final Object... initArgs) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator =
                new FixedConstructorAndArgsLocator<T>(elementClass, initArgTypes, initArgs);
        return new MultiDimensionalStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
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
    public static <T> MultiDimensionalStructuredArray<T> newInstance(final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                                                     final Long... lengths) throws NoSuchMethodException {
        return new MultiDimensionalStructuredArray<T>(lengths.length, constructorAndArgsLocator,
                                                      LongArrayToPrimitiveLongArray(lengths), null);
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
    public static <T> MultiDimensionalStructuredArray<T> newInstance(final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                                                     final long[] lengths)
            throws NoSuchMethodException {
        return new MultiDimensionalStructuredArray<T>(lengths.length, constructorAndArgsLocator, lengths, null);
    }

    /**
     * Copy an array of elements to a newly created array. Copying of individual elements is done by using
     * the <code>elementClass</code> copy constructor to construct the individual member elements of the new
     * array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to duplicate.
     * @throws NoSuchMethodException if the element class does not have a public copy constructor.
     */
    public static <T> MultiDimensionalStructuredArray<T> copyInstance(final MultiDimensionalStructuredArray<T> source)
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
    public static <T> MultiDimensionalStructuredArray<T> copyInstance(final MultiDimensionalStructuredArray<T> source,
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
    public static <T> MultiDimensionalStructuredArray<T> copyInstance(final MultiDimensionalStructuredArray<T> source,
                                                                      final long[] sourceOffsets,
                                                                      final long[] counts) throws NoSuchMethodException {
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
                 new MultiDimensionalCopyConstructorAndArgsLocator<T>(source.getElementClass(), source, sourceOffsets, false);

        return new MultiDimensionalStructuredArray<T>(source.getNumOfDimensions(), constructorAndArgsLocator, counts, null);
    }


    @SuppressWarnings("unchecked")
    private MultiDimensionalStructuredArray(final int numOfDimensions,
                                            final ConstructorAndArgsLocator<T> constructorAndArgsLocator,
                                            final long[] lengths,
                                            final long[] containingIndexes) throws NoSuchMethodException {
        if (numOfDimensions < 2) {
            throw new IllegalArgumentException("numOfDimensions must be at least 2");
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

        if (numOfDimensions > 2) {
            final long[] subArrayLengths = new long[lengths.length - 1];
            System.arraycopy(lengths, 1, subArrayLengths, 0, subArrayLengths.length);
            final Class[] subArrayArgTypes = {Integer.TYPE, ConstructorAndArgsLocator.class,
                                              long[].class, long[].class};
            final Object[] subArrayArgs = {numOfDimensions - 1, constructorAndArgsLocator,
                                           subArrayLengths, null /* containingIndexes arg goes here */};
            final Class targetClass = MultiDimensionalStructuredArray.class;
            final Constructor<MultiDimensionalStructuredArray<T>> constructor =
                    targetClass.getDeclaredConstructor(subArrayArgTypes);
            final ConstructorAndArgsLocator<MultiDimensionalStructuredArray<T>> subArrayConstructorAndArgsLocator =
                    new ArrayConstructorAndArgsLocator<MultiDimensionalStructuredArray<T>>(constructor, subArrayArgs, 3);
            populateElements(subArrayConstructorAndArgsLocator, containingIndexes);
        } else {
            final long subArrayLength = lengths[1];
            final Class[] subArrayArgTypes = {ConstructorAndArgsLocator.class, Long.TYPE, long[].class};
            final Object[] subArrayArgs = {constructorAndArgsLocator, subArrayLength, null /* containingIndexes arg goes here */};
            final Class targetClass = SingleDimensionalStructuredArray.class;
            final Constructor<SingleDimensionalStructuredArray<T>> constructor =
                    targetClass.getDeclaredConstructor(subArrayArgTypes);
            final ConstructorAndArgsLocator<SingleDimensionalStructuredArray<T>> subArrayConstructorAndArgsLocator =
                    new ArrayConstructorAndArgsLocator<SingleDimensionalStructuredArray<T>>(constructor, subArrayArgs, 2);
            populateElements(subArrayConstructorAndArgsLocator, containingIndexes);
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
     */
    public T getL(long[] indexes, int indexOffset) {
        if ((indexes.length - indexOffset) != numOfDimensions) {
            throw new IllegalArgumentException("number of relevant elements in indexes must match numOfDimensions");
        }
        if (numOfDimensions > 2) {
            MultiDimensionalStructuredArray<T> containedArray = getOfMultiDimensionalStructuredArrayL(indexes[indexOffset]);
            return containedArray.getL(indexes, indexOffset + 1);

        } else {
            SingleDimensionalStructuredArray<T> containedArray = getOfStructuredArrayL(indexes[indexOffset]);
            return containedArray.getL(indexes[indexOffset + 1]);
        }
    }

    /**
     * Get a reference to an element in the array, using a varargs long indexes.
     *
     * @param indexes The indexes (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public T getL(Long... indexes) {
        return getL(LongArrayToPrimitiveLongArray(indexes));
    }

    /**
     * Get a reference to an element in the array, using a varargs int indexes.
     *
     * @param indexes The indexes (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
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
     * @throws ClassCastException if number of indexes does not match number of dimensions in the array
     */
    public T getL(long index0, long index1) throws ClassCastException {
        return getOfStructuredArrayL(index0).getL(index1);
    }

    /**
     * Get a reference to an element in the array, using 3 <code>long</code> indexes.
     * @param index0
     * @param index1
     * @param index2
     * @return the element at [index0, index1, index2]
     * @throws ClassCastException if number of indexes does not match number of dimensions in the array
     */
    public T getL(long index0, long index1, long index2) throws ClassCastException {
        MultiDimensionalStructuredArray<T> level0element = getOfMultiDimensionalStructuredArrayL(index0);
        SingleDimensionalStructuredArray<T> singleDimensionalStructuredArray = level0element.getOfStructuredArrayL(index1);
        return singleDimensionalStructuredArray.getL(index2);
    }

    /**
     * Get a reference to an element in the array, using 4 <code>long</code> indexes.
     * @param index0
     * @param index1
     * @param index2
     * @param index3
     * @return the element at [index0, index1, index2, index3]
     * @throws ClassCastException if number of indexes does not match number of dimensions in the array
     */
    public T getL(long index0, long index1, long index2, long index3) throws ClassCastException {
        MultiDimensionalStructuredArray<T> level0element = getOfMultiDimensionalStructuredArrayL(index0);
        MultiDimensionalStructuredArray<T> level1element = level0element.getOfMultiDimensionalStructuredArrayL(index1);
        SingleDimensionalStructuredArray<T> singleDimensionalStructuredArray = level1element.getOfStructuredArrayL(index2);
        return singleDimensionalStructuredArray.getL(index3);
    }

    // fast int index variants:

    /**
     * Get a reference to an element in the array, using 2 <code>int</code> indexes.
     * @param index0
     * @param index1
     * @return the element at [index0, index1]
     * @throws ClassCastException if number of indexes does not match number of dimensions in the array
     */
    public T get(int index0, int index1) throws ClassCastException {
        return getOfStructuredArrayL(index0).getL(index1);
    }

    /**
     * Get a reference to an element in the array, using 3 <code>int</code> indexes.
     * @param index0
     * @param index1
     * @param index2
     * @return the element at [index0, index1, index2]
     * @throws ClassCastException if number of indexes does not match number of dimensions in the array
     */
    public T get(int index0, int index1, int index2) throws ClassCastException {
        MultiDimensionalStructuredArray<T> level0element = getOfMultiDimensionalStructuredArray(index0);
        SingleDimensionalStructuredArray<T> singleDimensionalStructuredArray = level0element.getOfStructuredArray(index1);
        return singleDimensionalStructuredArray.get(index2);
    }

    /**
     * Get a reference to an element in the array, using 4 <code>int</code> indexes.
     * @param index0
     * @param index1
     * @param index2
     * @param index3
     * @return the element at [index0, index1, index2, index3]
     * @throws ClassCastException if number of indexes does not match number of dimensions in the array
     */
    public T get(int index0, int index1, int index2, int index3) throws ClassCastException {
        MultiDimensionalStructuredArray<T> level0element = getOfMultiDimensionalStructuredArray(index0);
        MultiDimensionalStructuredArray<T> level1element = level0element.getOfMultiDimensionalStructuredArray(index1);
        SingleDimensionalStructuredArray<T> singleDimensionalStructuredArray = level1element.getOfStructuredArray(index2);
        return singleDimensionalStructuredArray.get(index3);
    }

    // Type specific public gets of first dimension:

    /**
     * Get a reference to a SingleDimensionalStructuredArray element in this array, using a <code>long</code> index.
     * @param index
     * @return a reference to the SingleDimensionalStructuredArray located in element [index] of this array
     * @throws ClassCastException if array has more than two dimensions
     */
    @SuppressWarnings("unchecked")
    public SingleDimensionalStructuredArray<T> getOfStructuredArrayL(final long index) throws ClassCastException {
        // Note that there is no explicit numOfDimensions check here. Type casting failure will trigger if dimensions are wrong.
        return (SingleDimensionalStructuredArray<T>) getOfUnknownTypeL(index);
    }

    /**
     * Get a reference to a SingleDimensionalStructuredArray element in this array, using a <code>long</code> index.
     * @param index
     * @return a reference to the SingleDimensionalStructuredArray located in element [index] of this array
     * @throws ClassCastException if array has only two dimensions
     */
    @SuppressWarnings("unchecked")
    public MultiDimensionalStructuredArray<T> getOfMultiDimensionalStructuredArrayL(final long index) throws ClassCastException {
        // Note that there is no explicit numOfDimensions check here. Type casting failure will trigger if dimensions are wrong.
        return (MultiDimensionalStructuredArray<T>) getOfUnknownTypeL(index);
    }

    /**
     * Get a reference to a MultiDimensionalStructuredArray element in this array, using a <code>int</code> index.
     * @param index
     * @return a reference to the SingleDimensionalStructuredArray located in element [index] of this array
     * @throws ClassCastException if array has more than two dimensions
     */
    @SuppressWarnings("unchecked")
    public SingleDimensionalStructuredArray<T> getOfStructuredArray(final int index) throws ClassCastException {
        // Note that there is no explicit numOfDimensions check here. Type casting failure will trigger if dimensions are wrong.
        return (SingleDimensionalStructuredArray<T>) getOfUnknownTypeL(index);
    }

    /**
     * Get a reference to a MultiDimensionalStructuredArray element in this array, using a <code>int</code> index.
     * @param index
     * @return a reference to the SingleDimensionalStructuredArray located in element [index] of this array
     * @throws ClassCastException if array has only two dimensions
     */
    @SuppressWarnings("unchecked")
    public MultiDimensionalStructuredArray<T> getOfMultiDimensionalStructuredArray(final int index) throws ClassCastException {
        // Note that there is no explicit numOfDimensions check here. Type casting failure will trigger if dimensions are wrong.
        return (MultiDimensionalStructuredArray<T>) getOfUnknownTypeL(index);
    }

    // Type-unknown gets:

    /**
     * Get a reference to an element in the array, using a <code>long</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    private Object getOfUnknownTypeL(final long index) {
        if (index < Integer.MAX_VALUE) {
            return getOfUnknownType((int) index);
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
     */
    private Object getOfUnknownType(final int index) {
        return intAddressableElements[index];
    }

    private <E> void populateElements(final ConstructorAndArgsLocator<E> constructorAndArgsLocator,
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

            for (int i = 0; i < intAddressableElements.length; i++, index++) {
                indexes[thisIndex] = index;
                final ConstructorAndArgs<E> constructorAndArgs = constructorAndArgsLocator.getForIndices(indexes);
                final Constructor<E> constructor = constructorAndArgs.getConstructor();
                intAddressableElements[i] = constructor.newInstance(constructorAndArgs.getConstructorArgs());
                constructorAndArgsLocator.recycle(constructorAndArgs);
            }

            for (final Object[] partition : longAddressableElements) {
                indexes[thisIndex] = index;
                for (int i = 0, size = partition.length; i < size; i++, index++) {
                    final ConstructorAndArgs<E> constructorAndArgs = constructorAndArgsLocator.getForIndices(indexes);
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

            final T t = getL(cursors);

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
         * Remove operation is not supported on {@link MultiDimensionalStructuredArray}s.
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
}
