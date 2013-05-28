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

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;

/**
 * <p>
 *      An array of objects with semantics restricted to be consistent with layouts of an array of structures
 *      in C-like languages.
 * <p>
 *      A JVM may optimise the implementation with intrinsics to provide a compact contiguous layout
 *      that facilitates consistent stride based memory access and dead-reckoning (as opposed to de-referenced)
 *      access to elements
 * </p>
 * @param <T> type of the element occupying each array slot.
 */
public final class StructuredArray<T> extends AbstractStructuredArray<T> {
    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final int length;
    private final T[] elements;

    /**
     * Create an array of types to be laid out like a contiguous array of structures.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     */
    public static <T> StructuredArray<T> newInstance(final int length, final Class<T> elementClass) throws NoSuchMethodException {
        final ElementConstructorGenerator<T> constructorGenerator =
                new ElementFixedConstructorGenerator<T>(elementClass);
        return new StructuredArray<T>(length, elementClass, constructorGenerator);
    }

    /**
     * Create an array of types to be laid out like a contiguous array of structures.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @param constructorGenerator produces element constructors [potentially] on a per element basis.
     */
    public static <T> StructuredArray<T> newInstance(final int length,
                                                     final Class<T> elementClass,
                                                     final ElementConstructorGenerator<T> constructorGenerator) {
        return new StructuredArray<T>(length, elementClass, constructorGenerator);
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
    public static <T> StructuredArray<T> newInstance(final int length,
                                                     final Class<T> elementClass,
                                                     final Class[] initArgTypes,
                                                     final Object... initArgs) throws NoSuchMethodException {
        final ElementConstructorGenerator<T> constructorGenerator =
                new ElementFixedConstructorGenerator<T>(elementClass, initArgTypes, initArgs);
        return new StructuredArray<T>(length, elementClass, constructorGenerator);
    }

    /**
     * Copy an array of elements using the element class copy constructor
     *
     * @param source The array to duplicate.
     * @throws NoSuchMethodException if the element class does not have a copy constructor.
     */
    public static <T> StructuredArray<T> copyInstance(StructuredArray<T> source) throws NoSuchMethodException {
        return copyInstance(source, 0, source.getLength());
    }

    /**
     * Copy an array of elements using the element class copy constructor
     *
     * @param source The array to duplicate.
     * @param sourceOffset offset index in source where the region to be copied begins.
     * @param count of elements to copy.
     * @throws NoSuchMethodException if the element class does not have a copy constructor.
     */
    public static <T> StructuredArray<T> copyInstance(StructuredArray<T> source, int sourceOffset, int count) throws NoSuchMethodException {
        if (source.getLength() < sourceOffset + count) {
            throw new ArrayIndexOutOfBoundsException(
                    "source " + source + " length of " + source.getLength() +
                            " is smaller than sourceOffset (" + sourceOffset + ") + count (" + count + ")" );
        }
        @SuppressWarnings("unchecked")
        final ElementConstructorGenerator<T> copyConstructorGenerator =
                (ElementConstructorGenerator<T>) new ElementCopyConstructorGenerator<T>(source.getElementClass(), source, sourceOffset);
        return new StructuredArray<T>(source.getLength(), source.getElementClass(), copyConstructorGenerator);
    }

    @SuppressWarnings("unchecked")
    private StructuredArray(final int length,
                              final Class<T> elementClass,
                              final ElementConstructorGenerator<T> elementConstructorGenerator) {
        super(elementClass, elementConstructorGenerator);
        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
        }
        this.length = length;
        elements = (T[]) new Object[length];
        populateElements(elements, elementConstructorGenerator);
    }

    /**
     * Get the length of the array by number of elements.
     *
     * @return the number of elements in the array.
     */
    public int getLength() {
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
        int intIndex = (int) index;
        if (index != intIndex) {
            throw new ArrayIndexOutOfBoundsException("index " + index + "out of bounds");
        }
        return get(intIndex);
    }

    /**
     * Get a reference to an element in the array.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public T get(final int index) {
        return elements[index];
    }


    private static <E> void populateElements(E[] elements, final ElementConstructorGenerator<E> constructorGenerator) {
        try {
            for (int i = 0, size = elements.length; i < size; i++) {
                ConstructorAndArgs<E> constructorAndArgs = constructorGenerator.getElementConstructorAndArgsForIndex(i);
                elements[i] = constructorAndArgs.getConstructor().newInstance(constructorAndArgs.getConstructorArgs());
                constructorGenerator.recycleElementConstructorAndArgs(constructorAndArgs);
            }
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}