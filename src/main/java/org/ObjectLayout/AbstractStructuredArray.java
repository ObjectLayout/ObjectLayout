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

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.lang.reflect.Modifier.*;

/**
 * <p>
 *      An array of objects with semantics restricted to be consistent with layouts of an array of structures
 *      in C-like languages.
 * </p>
 * <p>
 *      A JVM may optimise the implementation with intrinsics to provide a compact contiguous layout
 *      that facilitates consistent stride based memory access and dead-reckoning (as opposed to de-referenced)
 *      access to elements
 * </p>
 * @param <T> type of the element occupying each array slot.
 */
abstract class AbstractStructuredArray<T> implements Iterable<T>
{
    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final Field[] fields;
    private final boolean hasFinalFields;
    private final Class<T> elementClass;

    @SuppressWarnings("unchecked")
    protected AbstractStructuredArray(final Class<T> elementClass,
                                      final ElementConstructorGenerator<T> elementConstructorGenerator) {
        if (null == elementClass) {
            throw new NullPointerException("elementClass cannot be null");
        }

        if (elementClass.getClass() != elementConstructorGenerator.getElementClass()) {
            throw new IllegalArgumentException("elementClass and elementConstructorGenerator's generatedClass must match");
        }

        this.elementClass = elementClass;

        final Field[] fields = removeStaticFields(elementClass.getDeclaredFields());
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        this.fields = fields;
        this.hasFinalFields = containsFinalQualifiedFields(fields);
    }

    /**
     * Get the {@link Class} of elements stored as elements of the array.
     *
     * @return the {@link Class} of elements stored as elements of the array.
     */
    public Class<T> getElementClass() {
        return elementClass;
    }

    /**
     * Get a reference to an element in the array.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    abstract public T get(final long index);

    abstract long internalGetLengthAsLong();

    /**
     * Shallow copy a region of structures from one array to the other.  If the same array is both the src
     * and dst then the copy will happen as if a temporary intermediate array was used.
     *
     * @param src array to copy.
     * @param srcOffset offset index in src where the region begins.
     * @param dst array into which the copy should occur.
     * @param dstOffset offset index in the dst where the region begins.
     * @param count of structure elements to copy.
     * @throws IllegalStateException if final fields are discovered.
     * @throws ArrayStoreException if the element classes in src and dst are not identical.
     */
    public static void shallowCopy(final AbstractStructuredArray src, final long srcOffset,
                                   final AbstractStructuredArray dst, final long dstOffset,
                                   final long count) {
        shallowCopy(src, srcOffset, dst, dstOffset, count, false);

    }

    /**
     * Shallow copy a region of structures from one array to the other.  If the same array is both the src
     * and dst then the copy will happen as if a temporary intermediate array was used.
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
    public static void shallowCopy(final AbstractStructuredArray src, final long srcOffset,
                                   final AbstractStructuredArray dst, final long dstOffset,
                                   final long count, final boolean allowFinalFieldOverwrite) {
        if (src.elementClass != dst.elementClass) {
            final String msg = String.format("Only objects of the same class can be copied: %s != %s",
                    src.getClass(), dst.getClass());

            throw new ArrayStoreException(msg);
        }

        final Field[] fields = src.fields;
        if (!allowFinalFieldOverwrite && dst.hasFinalFields) {
            throw new IllegalStateException("cannot shallow copy onto final fields");
        }

        if (dst == src && (dstOffset >= srcOffset && (dstOffset + count) >= srcOffset)) {
            for (long srcIdx = srcOffset + count, dstIdx = dstOffset + count, limit = srcOffset - 1;
                 srcIdx > limit;
                 srcIdx--, dstIdx--) {
                reverseShallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
            }
        } else {
            for (long srcIdx = srcOffset, dstIdx = dstOffset, limit = srcOffset + count;
                 srcIdx < limit;
                 srcIdx++, dstIdx++) {
                shallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
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
            return cursor < internalGetLengthAsLong();
        }

        /**
         * {@inheritDoc}
         */
        public T next() {
            if (cursor >= internalGetLengthAsLong()) {
                throw new NoSuchElementException();
            }

            return get(cursor++);
        }

        /**
         * Remove operations are not supported on {@link AbstractStructuredArray}s.
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

    private static Field[] removeStaticFields(final Field[] declaredFields)
    {
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

    private static void shallowCopy(final Object src, final Object dst, final Field[] fields)
    {
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