/*
 * Copyright 2013 Gil Tene
 * Copyright 2012, 2013 Real Logic Ltd.
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

/**
 * <p>
 *      An array of objects with semantics restricted to be consistent with layouts of an array of structures
 *      in C-like languages.
 *
 *      The length and indexes of this array are 64 bit signed longs.
 * </p>
 * <p>
 *      A JVM may optimise the implementation with intrinsics to provide a compact contiguous layout
 *      that facilitates consistent stride based memory access and dead-reckoning (as opposed to de-referenced)
 *      access to elements
 * </p>
 * @param <T>
 */
public final class LongStructuredArray<T> extends AbstractStructuredArray<T> {
    private static final int MAX_PARTITION_SIZE_POW2_EXPONENT = 30;
    private static final int MAX_PARTITION_SIZE = 1 << MAX_PARTITION_SIZE_POW2_EXPONENT;
    private static final int MASK = MAX_PARTITION_SIZE  - 1;

    private final long length;
    private final T[][] partitions;

    /**
     * Create an array of types to be laid out like a contiguous array of structures.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     */
    public static <T> LongStructuredArray<T> newInstance(final long length, final Class<T> elementClass) throws NoSuchMethodException {
        final ElementConstructorGenerator<T> constructorGenerator =
                new ElementFixedConstructorGenerator<T>(elementClass);
        return new LongStructuredArray<T>(length, elementClass, constructorGenerator);
    }

    /**
     * Create an array of types to be laid out like a contiguous array of structures.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @param constructorGenerator produces element constructors [potentially] on a per element basis.
     */
    public static <T> LongStructuredArray<T> newInstance(final long length,
                                                     final Class<T> elementClass,
                                                     final ElementConstructorGenerator<T> constructorGenerator) {
        return new LongStructuredArray<T>(length, elementClass, constructorGenerator);
    }

    /**
     * Create an array of types to be laid out like a contiguous array of structures and provide
     * a list of arguments to be passed to a constructor for each element.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @param initArgTypes for selecting the constructor to call for initialising each structure object.
     * @param initArgs to be passed to a constructor for initialising each structure object.
     * @throws IllegalArgumentException if the constructor arguments do not match the signature.
     */
    public static <T> LongStructuredArray<T> newInstance(final long length,
                                                     final Class<T> elementClass,
                                                     final Class[] initArgTypes,
                                                     final Object... initArgs) throws NoSuchMethodException {
        final ElementConstructorGenerator<T> constructorGenerator =
                new ElementFixedConstructorGenerator<T>(elementClass, initArgTypes, initArgs);
        return new LongStructuredArray<T>(length, elementClass, constructorGenerator);
    }

    /**
     * Copy an array of elements using the elements' copy constructor
     *
     * @param source The array to duplicate.
     * @throws NoSuchMethodException if the element class does not have a copy constructor.
     */
    public static <T> LongStructuredArray<T> copyInstance(StructuredArray<T> source) throws NoSuchMethodException {
        return copyInstance(source, 0, source.getLength());
    }

    /**
     * Copy an array of elements using the elements' copy constructor
     *
     * @param source The array to duplicate.
     * @param sourceOffset offset index in source where the region to be copied begins.
     * @param count of elements to copy.
     * @throws NoSuchMethodException if the element class does not have a copy constructor.
     */
    public static <T> LongStructuredArray<T> copyInstance(StructuredArray<T> source, long sourceOffset, long count) throws NoSuchMethodException {
        if (source.getLength() < sourceOffset + count) {
            throw new ArrayIndexOutOfBoundsException(
                    "source " + source + " length of " + source.getLength() +
                            " is smaller than sourceOffset (" + sourceOffset + ") + count (" + count + ")" );
        }
        @SuppressWarnings("unchecked")
        final ElementConstructorGenerator<T> copyConstructorGenerator =
                (ElementConstructorGenerator<T>) new ElementCopyConstructorGenerator<T>(source.getElementClass(), source, sourceOffset);
        return new LongStructuredArray<T>(source.getLength(), source.getElementClass(), copyConstructorGenerator);
    }

    @SuppressWarnings("unchecked")
    private LongStructuredArray(final long length,
                                final Class<T> elementClass,
                                final ElementConstructorGenerator<T> elementConstructorGenerator) {
        super(elementClass, elementConstructorGenerator);
        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
        }
        this.length = length;

        final int numFullPartitions = (int)(length >>> MAX_PARTITION_SIZE_POW2_EXPONENT);
        final int lastPartitionSize = (int)length & MASK;

        partitions = (T[][])new Object[numFullPartitions + 1][];
        for (int i = 0; i < numFullPartitions; i++) {
            partitions[i] = (T[])new Object[MAX_PARTITION_SIZE];
        }
        partitions[numFullPartitions] = (T[])new Object[lastPartitionSize];

        populatePartitions(partitions, elementConstructorGenerator);
    }

    private static <E> void populatePartitions(final E[][] partitions,
                                               final ElementConstructorGenerator<E> constructorGenerator) {
        try {
            long index = 0;
            for (final E[] partition : partitions) {
                for (int i = 0, size = partition.length; i < size; i++, index++) {
                    ConstructorAndArgs<E> constructorAndArgs = constructorGenerator.getElementConstructorAndArgsForIndex(index);
                    partition[i] = constructorAndArgs.getConstructor().newInstance(constructorAndArgs.getConstructorArgs());
                    constructorGenerator.recycleElementConstructorAndArgs(constructorAndArgs);
                }
            }
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the length of the array by number of elements.
     *
     * @return the number of elements in the array.
     */
    public long getLength() {
        return length;
    }

    long internalGetLengthAsLong() {
        return length;
    }

    /**
     * Get a reference to an element in the array.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public T get(final long index) {
        final int partitionIndex = (int)(index >>> MAX_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)index & MASK;

        return partitions[partitionIndex][partitionOffset];
    }
}
