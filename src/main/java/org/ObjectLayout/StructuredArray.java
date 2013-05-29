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

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;

/**
 * <p>
 *     An array of (potentially) mutable but non-replaceable objects.
 * <p>
 *     A structured array contains array element objects of a fixed (at creation time, per array instance) class,
 *     and can support elements of any class that provides public constructors. The elements in a StructuredArray
 *     are all allocated and constructed at array creation time, and individual elements cannot be removed or
 *     replaced after array creation. Array elements can be accessed using an index-based accessor methods in
 *     the form of {@link StructuredArray#get}() (for {@link int} indices) or {@link StructuredArray#getL}()
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
 *     Note: At least for some JVM implementations, {@link StructuredArray#get}() access may be faster than
 *     {@link StructuredArray#getL}() access.
 *
 * @param <T> type of the element occupying each array slot.
 */
public final class StructuredArray<T> implements Iterable<T> {

    private static final int MAX_PARTITION_SIZE_POW2_EXPONENT = 30;
    private static final int MAX_PARTITION_SIZE = 1 << MAX_PARTITION_SIZE_POW2_EXPONENT;
    private static final int MASK = MAX_PARTITION_SIZE  - 1;

    private final Field[] fields;
    private final boolean hasFinalFields;
    private final Class<T> elementClass;

    private final long length;
    private final T[][] elements;
    private final T[] elements0;

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @throws NoSuchMethodException if the element class does not have a public default constructor
     */
    public static <T> StructuredArray<T> newInstance(final long length,
                                                     final Class<T> elementClass) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator = new FixedConstructorAndArgsLocator<T>(elementClass);

        return new StructuredArray<T>(length, elementClass, constructorAndArgsLocator);
    }

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Use constructor and arguments supplied (on a potentially
     * per element index basis) by the specified <code>constructorAndArgsLocator</code> to construct and initialize
     * each element.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @param constructorAndArgsLocator produces element constructors [potentially] on a per element basis.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final long length,
                                                     final Class<T> elementClass,
                                                     final ConstructorAndArgsLocator<T> constructorAndArgsLocator)
            throws NoSuchMethodException {
        return new StructuredArray<T>(length, elementClass, constructorAndArgsLocator);
    }

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
    public static <T> StructuredArray<T> newInstance(final long length,
                                                     final Class<T> elementClass,
                                                     final Class[] initArgTypes,
                                                     final Object... initArgs) throws NoSuchMethodException {
        final ConstructorAndArgsLocator<T> constructorAndArgsLocator =
                new FixedConstructorAndArgsLocator<T>(elementClass, initArgTypes, initArgs);

        return new StructuredArray<T>(length, elementClass, constructorAndArgsLocator);
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
        return copyInstance(source, 0, source.getLength());
    }

    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to duplicate.
     * @param sourceOffset offset index in source where the region to be copied begins.
     * @param count of elements to copy.
     * @throws NoSuchMethodException if the element class does not have a public copy constructor.
     */
    public static <T> StructuredArray<T> copyInstance(final StructuredArray<T> source,
                                                      final long sourceOffset,
                                                      final long count) throws NoSuchMethodException {
        if (source.getLength() < sourceOffset + count) {
            throw new ArrayIndexOutOfBoundsException(
                    "source " + source + " length of " + source.getLength() +
                    " is smaller than sourceOffset (" + sourceOffset + ") + count (" + count + ")" );
        }

        final ConstructorAndArgsLocator<T> constructorAndArgsLocator =
                 new CopyConstructorAndArgsLocator<T>(source.getElementClass(), source, sourceOffset);

        return new StructuredArray<T>(count, source.getElementClass(), constructorAndArgsLocator);
    }

    @SuppressWarnings("unchecked")
    private StructuredArray(final long length,
                            final Class<T> elementClass,
                            final ConstructorAndArgsLocator<T> constructorAndArgsLocator) throws NoSuchMethodException {
        if (null == elementClass) {
            throw new NullPointerException("elementClass cannot be null");
        }

        if (elementClass != constructorAndArgsLocator.getElementClass()) {
            throw new IllegalArgumentException("elementClass and constructorAndArgsLocator's generatedClass must match");
        }

        this.elementClass = elementClass;

        final Field[] fields = removeStaticFields(elementClass.getDeclaredFields());
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        this.fields = fields;
        this.hasFinalFields = containsFinalQualifiedFields(fields);

        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
        }
        this.length = length;

        final int numFullPartitions = (int)(length >>> MAX_PARTITION_SIZE_POW2_EXPONENT);
        final int lastPartitionSize = (int)length & MASK;

        elements = (T[][])new Object[numFullPartitions + 1][];
        for (int i = 0; i < numFullPartitions; i++) {
            elements[i] = (T[])new Object[MAX_PARTITION_SIZE];
        }
        elements[numFullPartitions] = (T[])new Object[lastPartitionSize];

        elements0 = elements[0];

        populateElements(elements, constructorAndArgsLocator);
    }

    /**
     * Get the length (number of elements) of the array.
     *
     * @return the number of elements in the array.
     */
    public long getLength() {
        return length;
    }

    /**
     * Get a reference to an element in the array, using a <code>long</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public T getL(final long index) {
        final int partitionIndex = (int)(index >>> MAX_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)index & MASK;

        return elements[partitionIndex][partitionOffset];
    }

    /**
     * Get a reference to an element in the array, using an <code>int</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public T get(final int index) {
        return elements0[index];
    }

    private static <E> void populateElements(final E[][] elements,
                                             final ConstructorAndArgsLocator<E> constructorAndArgsLocator)
            throws NoSuchMethodException {
        try {
            long index = 0;
            for (final E[] partition : elements) {
                for (int i = 0, size = partition.length; i < size; i++, index++) {
                    final ConstructorAndArgs<E> constructorAndArgs = constructorAndArgsLocator.getForIndex(index);
                    partition[i] = constructorAndArgs.getConstructor().newInstance(constructorAndArgs.getConstructorArgs());
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
     * @throws IllegalStateException if final fields are discovered and all allowFinalFieldOverwrite is not true.
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

        final Field[] fields = src.fields;
        if (!allowFinalFieldOverwrite && dst.hasFinalFields) {
            throw new IllegalArgumentException("Cannot shallow copy onto final fields");
        }

        if (((srcOffset + count) < MAX_PARTITION_SIZE) && ((dstOffset + count) < MAX_PARTITION_SIZE)) {
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

    /**
     * {@inheritDoc}
     */
    public StructureIterator iterator() {
        return new StructureIterator();
    }

    /**
     * Specialised {@link Iterator} with the ability to be {@link #reset()} enabling reuse.
     */
    public class StructureIterator implements Iterator<T> {
        private long cursor = 0;

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return cursor < length;
        }

        /**
         * {@inheritDoc}
         */
        public T next() {
            if (cursor >= length) {
                throw new NoSuchElementException();
            }

            if (cursor > MAX_PARTITION_SIZE) {
                return getL(cursor++);
            }

            return get((int)(cursor++));
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
            cursor = 0;
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
}
