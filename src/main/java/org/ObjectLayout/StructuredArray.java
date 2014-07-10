/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
 *     options. The CtorAndArgsProvider API provides for array creation with arbitrary, user-supplied
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
public class StructuredArray<T> implements Iterable<T> {

    static final int MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT = 30;
    static final int MAX_EXTRA_PARTITION_SIZE = 1 << MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT;
    static final int MASK = MAX_EXTRA_PARTITION_SIZE - 1;
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final Class<T> elementClass;

    private final long length;            // A cached lengths[0]
    private final long[] lengths;
    private final long totalElementCount; // A cached product of lengths[i]

    private final int dimensionCount;

    private final Field[] fields;
    private final boolean hasFinalFields;

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
        final CtorAndArgsProvider<T> ctorAndArgsProvider = new SingletonCtorAndArgsProvider<T>(elementClass);
        final long[] lengths = {length};
        return instantiate(lengths.length, ctorAndArgsProvider, lengths);
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param arrayClass of the array to create.
     * @param length of the array to create.
     * @param elementClass of each element in the array
     */
    @SuppressWarnings("unchecked")
    public static <T> StructuredArray<T> newSubclassInstance(final Class<? extends StructuredArray<T>> arrayClass,
                                                             final Class<T> elementClass,
                                                             final long length) {
        try {
            CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                    new CtorAndArgs<StructuredArray<T>>(((Class<StructuredArray<T>>)arrayClass).getConstructor(),
                            EMPTY_ARGS);
            return newSubclassInstance(arrayCtorAndArgs, elementClass, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use <code>elementClass</code>'s default constructor.
     *
     * @param arrayCtorAndArgs of the array to create.
     * @param length of the array to create.
     * @param elementClass of each element in the array
     */

    public static <T> StructuredArray<T> newSubclassInstance(final CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs,
                                                             final Class<T> elementClass,
                                                             final long length) {
        try {
            final CtorAndArgsProvider<T> ctorAndArgsProvider = new SingletonCtorAndArgsProvider<T>(elementClass);
            final long[] lengths = {length};
            return instantiate(arrayCtorAndArgs, lengths.length, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an array of <code>length</code> elements of type <code>elementClass</code>. Use constructor and
     * arguments supplied (on a potentially per element index basis) by the specified
     * <code>ctorAndArgsProvider</code> to construct and initialize each element.
     *
     * @param length of the array to create.
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                                     final long length) throws NoSuchMethodException {
        final long[] lengths = {length};
        return instantiate(lengths.length, ctorAndArgsProvider, lengths);
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Use constructor and arguments supplied (on a potentially
     * per element index basis) by the specified <code>ctorAndArgsProvider</code> to construct and initialize
     * each element.
     *
     * @param arrayClass of the array to create.
     * @param length of the array to create.
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis.
     */
    @SuppressWarnings("unchecked")
    public static <T> StructuredArray<T> newSubclassInstance(final Class<? extends StructuredArray<T>> arrayClass,
                                                             final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                                             final long length) {
        try {
            CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                    new CtorAndArgs<StructuredArray<T>>(((Class<StructuredArray<T>>)arrayClass).getConstructor(),
                            EMPTY_ARGS);
            final long[] lengths = {length};
            return instantiate(arrayCtorAndArgs, lengths.length, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use constructor and arguments
     * supplied (on a potentially per element index basis) by the specified <code>ctorAndArgsProvider</code>
     * to construct and initialize each element.
     *
     * @param arrayCtorAndArgs of the array to create.
     * @param length of the array to create.
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis.
     */
    public static <T> StructuredArray<T> newSubclassInstance(final CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs,
                                                             final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                                             final long length) {
        try {
            final long[] lengths = {length};
            return instantiate(arrayCtorAndArgs, lengths.length, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an array of <code>length</code> elements, each containing an element object of type
     * <code>elementClass</code>. Use a fixed (same for all elements) constructor identified by the argument
     * classes specified in  <code>elementConstructorArgs</code> to construct and initialize each element,
     * passing the remaining arguments to that constructor.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @param elementConstructorArgTypes for selecting the constructor to call for initialising each structure object.
     * @param elementConstructorArgs to be passed to a constructor for initialising each structure object.
     * @throws IllegalArgumentException if elementConstructorArgTypes and constructor arguments do not match in length
     * @throws NoSuchMethodException if elementConstructorArgTypes does not match a public constructor signature in elementClass

     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long length,
                                                     final Class[] elementConstructorArgTypes,
                                                     final Object... elementConstructorArgs) throws NoSuchMethodException {
        final CtorAndArgsProvider<T> ctorAndArgsProvider =
                new SingletonCtorAndArgsProvider<T>(elementClass, elementConstructorArgTypes, elementConstructorArgs);
        final long[] lengths = {length};
        return instantiate(lengths.length, ctorAndArgsProvider, lengths);
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements of type <code>elementClass</code>.
     * Use a fixed (same for all elements) constructor identified by the argument classes specified in
     * <code>elementConstructorArgs</code> to construct and initialize each element, passing the remaining
     * arguments to that constructor.
     *
     * @param arrayClass of the array to create.
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @param elementConstructorArgTypes for selecting the constructor to call for initialising each structure object.
     * @param elementConstructorArgs to be passed to a constructor for initialising each structure object.
     */
    @SuppressWarnings("unchecked")
    public static <T> StructuredArray<T> newSubclassInstance(final Class<? extends StructuredArray<T>> arrayClass,
                                                             final Class<T> elementClass,
                                                             final long length,
                                                             final Class[] elementConstructorArgTypes,
                                                             final Object... elementConstructorArgs) {
        try {
            CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                    new CtorAndArgs<StructuredArray<T>>(((Class<StructuredArray<T>>)arrayClass).getConstructor(),
                            EMPTY_ARGS);
            return newSubclassInstance(arrayCtorAndArgs,
                    elementClass, length, elementConstructorArgTypes, elementConstructorArgs);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use a fixed (same for all elements) constructor identified by
     * the argument classes specified in  <code>elementConstructorArgs</code> to construct and initialize each
     * element, passing the remaining arguments to that constructor.
     *
     * @param arrayCtorAndArgs of the array to create.
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @param elementConstructorArgTypes for selecting the constructor to call for initialising each structure object.
     * @param elementConstructorArgs to be passed to a constructor for initialising each structure object.
     */
    public static <T> StructuredArray<T> newSubclassInstance(final CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs,
                                                             final Class<T> elementClass,
                                                             final long length,
                                                             final Class[] elementConstructorArgTypes,
                                                             final Object... elementConstructorArgs) {
        try {
            final CtorAndArgsProvider<T> ctorAndArgsProvider =
                    new SingletonCtorAndArgsProvider<T>(elementClass, elementConstructorArgTypes, elementConstructorArgs);
            final long[] lengths = {length};
            return instantiate(arrayCtorAndArgs, lengths.length, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    // Multi-dimensional newInstance forms:

    /**
     * Create a multi dimensional array of elements of type <code>elementClass</code>. Each dimension of the array
     * will be of a length designated in the <code>lengths[]</code>  passed. Elements will be constructed Using the
     * <code>elementClass</code>'s default constructor.
     *
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long... lengths) throws NoSuchMethodException {
        final CtorAndArgsProvider<T> ctorAndArgsProvider = new SingletonCtorAndArgsProvider<T>(elementClass);
        return instantiate(lengths.length, ctorAndArgsProvider, lengths);
    }

    /**
     * Create a multi dimensional <code>arrayClass</code> array of elements of type <code>elementClass</code>.
     * Each dimension of the array will be of a length designated in the <code>lengths[]</code>  passed.
     * Elements will be constructed Using the <code>elementClass</code>'s default constructor.
     *
     * @param arrayClass of the array to create.
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     */
    @SuppressWarnings("unchecked")
    public static <T> StructuredArray<T> newSubclassInstance(final Class<? extends StructuredArray<T>> arrayClass,
                                                             final Class<T> elementClass,
                                                             final long... lengths) {
        try {
            CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                    new CtorAndArgs<StructuredArray<T>>(((Class<StructuredArray<T>>)arrayClass).getConstructor(),
                            EMPTY_ARGS);
            return newSubclassInstance(arrayCtorAndArgs, elementClass, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a multi dimensional <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of elements
     * of type <code>elementClass</code>. Each dimension of the array will be of a length designated in the
     * <code>lengths[]</code> passed. Elements will be constructed Using the <code>elementClass</code>'s default
     * constructor.
     *
     * @param arrayCtorAndArgs of the array to create.
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     */
    public static <T> StructuredArray<T> newSubclassInstance(final CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs,
                                                             final Class<T> elementClass,
                                                             final long... lengths) {
        try {
            final CtorAndArgsProvider<T> ctorAndArgsProvider = new SingletonCtorAndArgsProvider<T>(elementClass);
            return instantiate(arrayCtorAndArgs, lengths.length, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a multi dimensional array of elements of type <code>elementClass</code>. Each dimension of the array
     * will be of a length designated in the <code>lengths[]</code>  passed. Elements will be constructed using a
     * fixed (same for all elements) constructor identified by the argument classes specified in
     * <code>elementConstructorArgTypes</code> to construct and initialize each element, passing the
     * <code>elementConstructorArgs</code> arguments to that constructor.
     *
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     * @param elementConstructorArgTypes for selecting the constructor to call for initialising each structure object.
     * @param elementConstructorArgs to be passed to a constructor for initialising each structure object.
     * @throws IllegalArgumentException if elementConstructorArgTypes and constructor arguments do not match in length
     * @throws NoSuchMethodException if elementConstructorArgTypes does not match a public constructor signature in elementClass

     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long[] lengths,
                                                     final Class[] elementConstructorArgTypes,
                                                     final Object... elementConstructorArgs) throws NoSuchMethodException {
        final CtorAndArgsProvider<T> ctorAndArgsProvider =
                new SingletonCtorAndArgsProvider<T>(elementClass, elementConstructorArgTypes, elementConstructorArgs);
        return instantiate(lengths.length, ctorAndArgsProvider, lengths);
    }

    /**
     * Create a multi dimensional <code>arrayClass</code> array of elements of type <code>elementClass</code> Each
     * dimension of the array will be of a length designated in the <code>lengths[]</code>  passed. Elements will be
     * constructed using a fixed (same for all elements) constructor identified by the argument classes specified
     * in <code>elementConstructorArgTypes</code> to construct and initialize each element, passing the
     * <code>elementConstructorArgs</code> arguments to that constructor.
     *
     * @param arrayClass of the array to create.
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     * @param elementConstructorArgTypes for selecting the constructor to call for initialising each structure object.
     * @param elementConstructorArgs to be passed to a constructor for initialising each structure object.
     * @throws IllegalArgumentException if elementConstructorArgTypes and constructor arguments do not match in length

     */
    @SuppressWarnings("unchecked")
    public static <T> StructuredArray<T> newSubclassInstance(final Class<? extends StructuredArray<T>> arrayClass,
                                                             final Class<T> elementClass,
                                                             final long[] lengths,
                                                             final Class[] elementConstructorArgTypes,
                                                             final Object... elementConstructorArgs) {
        try {
            CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                    new CtorAndArgs<StructuredArray<T>>(((Class<StructuredArray<T>>)arrayClass).getConstructor(),
                            EMPTY_ARGS);
            return newSubclassInstance(arrayCtorAndArgs, elementClass, lengths,
                    elementConstructorArgTypes, elementConstructorArgs);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a multi dimensional <code>(arrayCtorAndArgs.getConstructor().getDeclaringClass())</code> array of
     * elements of type <code>elementClass</code>. Each dimension of the array will be of a length designated in
     * the <code>lengths[]</code>  passed. Elements will be constructed using a fixed (same for all elements)
     * constructor identified by the argument classes specified in  <code>elementConstructorArgTypes</code> to
     * construct and initialize each element, passing the <code>elementConstructorArgs</code> arguments to that
     * constructor.
     *
     * @param arrayCtorAndArgs of the array to create.
     * @param elementClass of each element in the array
     * @param lengths of the array dimensions to create.
     * @param elementConstructorArgTypes for selecting the constructor to call for initialising each structure object.
     * @param elementConstructorArgs to be passed to a constructor for initialising each structure object.
     * @throws IllegalArgumentException if elementConstructorArgTypes and constructor arguments do not match in length

     */
    public static <T> StructuredArray<T> newSubclassInstance(final CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs,
                                                             final Class<T> elementClass,
                                                             final long[] lengths,
                                                             final Class[] elementConstructorArgTypes,
                                                             final Object... elementConstructorArgs) {
        try {
            final CtorAndArgsProvider<T> ctorAndArgsProvider =
                    new SingletonCtorAndArgsProvider<T>(elementClass, elementConstructorArgTypes, elementConstructorArgs);
            return instantiate(arrayCtorAndArgs, lengths.length, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a multi dimensional array of elements of type <code>elementClass</code>. Each dimension of the array
     * will be of a length designated in the <code>lengths[]</code> passed. Elements will be constructed using the
     * constructor and arguments supplied (on a potentially per element index basis) by the specified
     * <code>ctorAndArgsProvider</code> to construct and initialize each element.
     *
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis.
     * @param lengths of the array dimensions to create.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                                     final long... lengths) throws NoSuchMethodException {
        return instantiate(lengths.length, ctorAndArgsProvider, lengths);
    }

    /**
     * Create a multi dimensional <code>arrayClass</code> array of elements of type <code>elementClass</code>.
     * Each dimension of the array will be of a length designated in the <code>lengths[]</code> passed. Elements
     * will be constructed using the constructor and arguments supplied (on a potentially per element index basis)
     * by the specified <code>ctorAndArgsProvider</code> to construct and initialize each element.
     *
     * @param arrayClass of the array to create.
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis.
     * @param lengths of the array dimensions to create.
     */
    @SuppressWarnings("unchecked")
    public static <T> StructuredArray<T> newSubclassInstance(final Class<? extends StructuredArray<T>> arrayClass,
                                                             final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                                             final long... lengths) {
        try {
            CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                    new CtorAndArgs<StructuredArray<T>>(((Class<StructuredArray<T>>)arrayClass).getConstructor(),
                            EMPTY_ARGS);
            return instantiate(arrayCtorAndArgs, lengths.length, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a multi dimensional <code>(arrayCtorAndArgs.getConstructor().getDeclaringClass())</code> array of
     * elements of type <code>elementClass</code>. Each dimension of the array will be of a length designated in
     * the <code>lengths[]</code> passed. Elements will be constructed using the constructor and arguments supplied
     * (on a potentially per element index basis) by the specified <code>ctorAndArgsProvider</code> to construct
     * and initialize each element.
     *
     * @param arrayCtorAndArgs of the array to create.
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis.
     * @param lengths of the array dimensions to create.
     */
    public static <T> StructuredArray<T> newSubclassInstance(final CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs,
                                                             final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                                             final long... lengths) {
        try {
            return instantiate(arrayCtorAndArgs, lengths.length, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
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
    @SuppressWarnings("unchecked")
    public static <T> StructuredArray<T> copyInstance(final StructuredArray<T> source,
                                                      final long[] sourceOffsets,
                                                      final long... counts) throws NoSuchMethodException {
        if (source.getDimensionCount() != sourceOffsets.length) {
            throw new IllegalArgumentException("source.getDimensionCount() must match sourceOffsets.length");
        }
        if (counts.length != source.getDimensionCount()) {
            throw new IllegalArgumentException("source.getDimensionCount() must match counts.length");
        }

        for (int i = 0; i < counts.length; i++) {
            if (source.getLengths()[i] < sourceOffsets[i] + counts[i]) {
                throw new ArrayIndexOutOfBoundsException(
                        "Dimension " + i + ": source " + source + " length of " + source.getLengths()[i] +
                                " is smaller than sourceOffset (" + sourceOffsets[i] + ") + count (" + counts[i] + ")" );
            }
        }

        final CtorAndArgsProvider<T> ctorAndArgsProvider =
                 new CopyCtorAndArgsProvider<T>(source.getElementClass(), source, sourceOffsets, false);
        CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                new CtorAndArgs<StructuredArray<T>>(((Class<StructuredArray<T>>) source.getClass()).getConstructor(),
                        EMPTY_ARGS);
        return instantiate(arrayCtorAndArgs, source.getDimensionCount(), ctorAndArgsProvider, counts);
    }

    @SuppressWarnings("unchecked")
    private static <T> StructuredArray<T> instantiate(final int dimensionCount,
                                                      final CtorAndArgsProvider ctorAndArgsProvider,
                                                      final long... lengths) throws NoSuchMethodException {
        Class arrayClass = StructuredArray.class;
        CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                new CtorAndArgs<StructuredArray<T>>(arrayClass.getConstructor(), EMPTY_ARGS);
        return instantiate(arrayCtorAndArgs, dimensionCount, ctorAndArgsProvider, lengths);
    }

    private static <T> StructuredArray<T> instantiate(CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs,
                                                      final int dimensionCount,
                                                      final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                                      final long[] lengths) throws NoSuchMethodException {
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setArrayConstructorArgs(arrayCtorAndArgs, dimensionCount, ctorAndArgsProvider, lengths, null);
        constructorMagic.setActive(true);
        try {
            Constructor<? extends StructuredArray<T>> arrayConstructor = arrayCtorAndArgs.getConstructor();
            return StructuredArrayIntrinsicSupport.instantiateStructuredArray(
                    arrayConstructor.getDeclaringClass(),
                    ctorAndArgsProvider.getElementClass(),
                    lengths,
                    arrayConstructor,
                    arrayCtorAndArgs.getArgs()
            );
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } finally {
            constructorMagic.setActive(false);
        }
    }

    @SuppressWarnings("unchecked")
    public StructuredArray() {
        checkConstructorMagic();
        ConstructorMagic constructorMagic = getConstructorMagic();

        @SuppressWarnings("unchecked")
        final CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs = constructorMagic.getArrayCtorAndArgs();
        @SuppressWarnings("unchecked")
        final CtorAndArgsProvider<T> ctorAndArgsProvider = constructorMagic.getCtorAndArgsProvider();
        final int dimensionCount = constructorMagic.getDimensionCount();
        final long[] lengths = constructorMagic.getLengths();
        final long[] containingIndex = constructorMagic.getContainingIndex();

        if (dimensionCount < 1) {
            throw new IllegalArgumentException("dimensionCount must be at least 1");
        }

        this.dimensionCount = dimensionCount;
        this.lengths = lengths;
        this.length = lengths[0];

        // Compute and cache total element count:
        long totalCount = 1;
        for (long length : lengths) {
            totalCount *= length;
        }
        totalElementCount = totalCount;

        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
        }

        if (lengths.length != dimensionCount) {
            throw new IllegalArgumentException("number of lengths provided (" + lengths.length +
                    ") does not match numDimensions (" + dimensionCount + ")");
        }

        this.elementClass = ctorAndArgsProvider.getElementClass();

        final Field[] fields = removeStaticFields(elementClass.getDeclaredFields());
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        this.fields = fields;
        this.hasFinalFields = containsFinalQualifiedFields(fields);

        if (!StructuredArrayIntrinsicSupport.StructuredArrayIsIntrinsicToJdk) {
            // Allocate internal storage:

            // Size int-addressable sub arrays:
            final int intLength = (int) Math.min(length, Integer.MAX_VALUE);
            // Size Subsequent partitions hold long-addressable-only sub arrays:
            final long extraLength = length - intLength;
            final int numFullPartitions = (int) (extraLength >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
            final int lastPartitionSize = (int) extraLength & MASK;

            if (dimensionCount > 1) {
                // We have sub arrays, not elements:
                intAddressableElements = null;
                longAddressableElements = null;

                intAddressableSubArrays = new StructuredArray[intLength];
                longAddressableSubArrays = new StructuredArray[numFullPartitions + 1][];
                // full long-addressable-only partitions:
                for (int i = 0; i < numFullPartitions; i++) {
                    longAddressableSubArrays[i] = new StructuredArray[MAX_EXTRA_PARTITION_SIZE];
                }
                // Last partition with leftover long-addressable-only size:
                longAddressableSubArrays[numFullPartitions] = new StructuredArray[lastPartitionSize];

            } else {
                // We have elements, not sub arrays:
                intAddressableSubArrays = null;
                longAddressableSubArrays = null;

                intAddressableElements = (T[]) new Object[intLength];
                longAddressableElements = (T[][]) new Object[numFullPartitions + 1][];
                // full long-addressable-only partitions:
                for (int i = 0; i < numFullPartitions; i++) {
                    longAddressableElements[i] = (T[]) new Object[MAX_EXTRA_PARTITION_SIZE];
                }
                // Last partition with leftover long-addressable-only size:
                longAddressableElements[numFullPartitions] = (T[]) new Object[lastPartitionSize];
            }
        } else {
            // No internal storage:
            intAddressableElements = null;
            longAddressableElements = null;
            intAddressableSubArrays = null;
            longAddressableSubArrays = null;
        }

        try {
            if (dimensionCount > 1) {
                // This is an array of arrays. Pass the ctorAndArgsProvider through to
                // a subArrayCtorAndArgsProvider that will be used to populate the sub-array:
                final long[] subArrayLengths = new long[lengths.length - 1];
                System.arraycopy(lengths, 1, subArrayLengths, 0, subArrayLengths.length);

                final Object[] subArrayArgs = {arrayCtorAndArgs, dimensionCount - 1, ctorAndArgsProvider,
                        subArrayLengths, null /* containingIndex arg goes here */};
                @SuppressWarnings("unchecked")
                final ArrayCtorAndArgsProvider<StructuredArray<T>> subArrayCtorAndArgsProvider =
                        new ArrayCtorAndArgsProvider(arrayCtorAndArgs.getConstructor(), subArrayArgs, 4);

                populateSubArrays(subArrayCtorAndArgsProvider, containingIndex);
            } else {
                // This is a single dimension array. Populate it:
                populateLeafElements(ctorAndArgsProvider, containingIndex);
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
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
     * Get a reference to an element in an N dimensional array, using N long indices (or a <code>long[N]</code> index array).
     *
     * @param indices The indices (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if number of indices does not match number of dimensions in the array
     */
    public T get(final long... indices) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.get(this, indices, 0);
    }

    /**
     * Get a reference to an element in an N dimensional array, using N int indices (or a <code>int[N]</code> index array).
     *
     * @param indices The indices (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if number of indices does not match number of dimensions in the array
     */
    public T get(final int... indices) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.get(this, indices, 0);
    }

    // fast long index element get variants:

    /**
     * Get a reference to an element in a single dimensional array, using a <code>long</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if array has more than one dimensions
     */
    public T get(final long index) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.get(this, index);
    }

    /**
     * Get a reference to an element in a two dimensional array, using 2 <code>long</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @return the element at [index0, index1]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final long index0, final long index1) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.get(this, index0, index1);
    }

    /**
     * Get a reference to an element in a three dimensional array, using 3 <code>long</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @param index2 the third index (in the third array dimension) of the element to retrieve
     * @return the element at [index0, index1, index2]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final long index0, final long index1, final long index2) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.get(this, index0, index1, index2);

    }

    // fast int index element get variants:

    /**
     * Get a reference to an element in a single dimensional array, using an <code>int</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws NullPointerException if array has more than one dimensions
     */
    public T get(final int index) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.get(this, index);
    }

    /**
     * Get a reference to an element in a two dimensional array, using 2 <code>int</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @return the element at [index0, index1]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final int index0, final int index1) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.get(this, index0, index1);
    }

    /**
     * Get a reference to an element in a three dimensional array, using 3 <code>int</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @param index2 the third index (in the third array dimension) of the element to retrieve
     * @return the element at [index0, index1, index2]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final int index0, final int index1, final int index2) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.get(this, index0, index1, index2);
    }

    // Type specific public gets of first dimension subarray:

    /**
     * Get a reference to a StructuredArray element in this array, using a <code>long</code> index.
     * @param index (in this array's first dimension) of the StructuredArray to retrieve
     * @return a reference to the StructuredArray located at [index] in the first dimension of this array
     * @throws IllegalArgumentException if array has less than two dimensions
     */
    public StructuredArray<T> getSubArray(final long index) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.getSubArray(this, index);
    }

    /**
     * Get a reference to a StructuredArray element in this array, using a <code>int</code> index.
     * @param index (in this array's first dimension) of the StructuredArray to retrieve
     * @return a reference to the StructuredArray located at [index] in the first dimension of this array
     * @throws IllegalArgumentException if array has less than two dimensions
     */
    public StructuredArray<T> getSubArray(final int index) throws IllegalArgumentException {
        return StructuredArrayIntrinsicSupport.getSubArray(this, index);
    }

    private void populateElement(final long index0,
                                     final Constructor<T> constructor,
                                     Object... args) {
        try {
            // Instantiate:
            T element = StructuredArrayIntrinsicSupport.constructElementAtIndex(this, index0, constructor, args);

            if (!StructuredArrayIntrinsicSupport.StructuredArrayIsIntrinsicToJdk) {
                // place in proper internal storage location:
                if (index0 < Integer.MAX_VALUE) {
                    intAddressableElements[(int) index0] = element;
                    return;
                }

                // Calculate index into long-addressable-only partitions:
                final long longIndex0 = (index0 - Integer.MAX_VALUE);
                final int partitionIndex = (int) (longIndex0 >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
                final int partitionOffset = (int) longIndex0 & MASK;

                longAddressableElements[partitionIndex][partitionOffset] = element;
            }
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void populateSubArray(final long index0,
                                 final Constructor<StructuredArray<T>> constructor,
                                 Object... args) {
        try {
            // Instantiate:
            getConstructorMagic().setArrayConstructorArgs(args);
            StructuredArray<T> subArray = StructuredArrayIntrinsicSupport.constructSubArrayAtIndex(this, index0, constructor);

            if (!StructuredArrayIntrinsicSupport.StructuredArrayIsIntrinsicToJdk) {
                // place in proper internal storage location:
                if (index0 < Integer.MAX_VALUE) {
                    intAddressableSubArrays[(int) index0] = subArray;
                    return;
                }

                // Calculate index into long-addressable-only partitions:
                final long longIndex0 = (index0 - Integer.MAX_VALUE);
                final int partitionIndex = (int) (longIndex0 >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
                final int partitionOffset = (int) longIndex0 & MASK;

                longAddressableSubArrays[partitionIndex][partitionOffset] = subArray;
            }
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void populateLeafElements(final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                      final long[] containingIndex) {
        final int thisIndex;
        final long[] index;
        if (containingIndex != null) {
            thisIndex = containingIndex.length;
            index = new long[thisIndex + 1];
            System.arraycopy(containingIndex, 0, index, 0, containingIndex.length);
        } else {
            thisIndex = 0;
            index = null;
        }

        try {
            for (long index0 = 0; index0 < length; index0++) {
                final CtorAndArgs<T> ctorAndArgs;
                if (index != null) {
                    index[thisIndex] = index0;
                    ctorAndArgs = ctorAndArgsProvider.getForIndex(index);
                } else {
                    ctorAndArgs = ctorAndArgsProvider.getForIndex(index0);
                }
                populateElement(index0, ctorAndArgs.getConstructor(), ctorAndArgs.getArgs());
                ctorAndArgsProvider.recycle(ctorAndArgs);
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void populateSubArrays(final ArrayCtorAndArgsProvider<StructuredArray<T>> arrayCtorAndArgsProvider,
                                  final long[] containingIndex) {
        final int thisIndex;
        final long[] index;
        if (containingIndex != null) {
            thisIndex = containingIndex.length;
            index = new long[thisIndex + 1];
            System.arraycopy(containingIndex, 0, index, 0, containingIndex.length);
        } else {
            thisIndex = 0;
            index = null;
        }

        try {
            for (long index0 = 0; index0 < length; index0++) {
                final CtorAndArgs<StructuredArray<T>> ctorAndArgs;
                if (index != null) {
                    index[thisIndex] = index0;
                    ctorAndArgs = arrayCtorAndArgsProvider.getForIndex(index);
                } else {
                    ctorAndArgs = arrayCtorAndArgsProvider.getForIndex(index0);
                }
                populateSubArray(index0, ctorAndArgs.getConstructor(), ctorAndArgs.getArgs());
                arrayCtorAndArgsProvider.recycle(ctorAndArgs);
            }
        } catch (NoSuchMethodException ex) {
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
    public ElementIterator iterator() {
        return new ElementIterator();
    }

    /**
     * Specialised {@link java.util.Iterator} with the ability to be {@link #reset()} enabling reuse.
     */
    public class ElementIterator implements Iterator<T> {

        private final long[] cursors = new long[dimensionCount];
        private long elementCountToCursor = 0;

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

            final T element = get(cursors);

            // Increment cursors from inner-most dimension out:
            for (int cursorDimension = cursors.length - 1; cursorDimension >= 0; cursorDimension--) {
                if ((++cursors[cursorDimension]) < lengths[cursorDimension]) {
                    break;
                }

                // This dimension wrapped. Reset to zero and continue to one dimension higher
                cursors[cursorDimension] = 0;
            }
            elementCountToCursor++;

            return element;
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
            String msg = String.format("Only objects of the same class can be copied: %s != %s",
                                       src.getClass(), dst.getClass());
            throw new ArrayStoreException(msg);
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


    // ConstructorMagic support:

    private static class ConstructorMagic {
        private boolean isActive() {
            return active;
        }

        private void setActive(boolean active) {
            this.active = active;
        }

        public void setArrayConstructorArgs(final CtorAndArgs arrayCtorAndArgs, int dimensionCount,
                                            final CtorAndArgsProvider ctorAndArgsProvider,
                                            final long[] lengths,
                                            final long[] containingIndex) {
            this.arrayCtorAndArgs = arrayCtorAndArgs;
            this.dimensionCount = dimensionCount;
            this.ctorAndArgsProvider = ctorAndArgsProvider;
            this.lengths = lengths;
            this.containingIndex = containingIndex;
        }

        public void setArrayConstructorArgs(Object... args) {
            this.arrayCtorAndArgs = (CtorAndArgs) args[0];
            this.dimensionCount = (Integer) args[1];
            this.ctorAndArgsProvider = (CtorAndArgsProvider) args[2];
            this.lengths = (long[]) args[3];
            this.containingIndex = (long[]) args[4];
        }

        public CtorAndArgs getArrayCtorAndArgs() {
            return arrayCtorAndArgs;
        }

        public int getDimensionCount() {
            return dimensionCount;
        }

        public CtorAndArgsProvider getCtorAndArgsProvider() {
            return ctorAndArgsProvider;
        }

        public long[] getLengths() {
            return lengths;
        }

        public long[] getContainingIndex() {
            return containingIndex;
        }

        private boolean active = false;

        private CtorAndArgs arrayCtorAndArgs = null;
        private int dimensionCount = 1;
        private CtorAndArgsProvider ctorAndArgsProvider = null;
        private long[] lengths = null;
        private long[] containingIndex = null;
    }

    private static final ThreadLocal<ConstructorMagic> threadLocalConstructorMagic = new ThreadLocal<ConstructorMagic>();

    @SuppressWarnings("unchecked")
    private static ConstructorMagic getConstructorMagic() {
        ConstructorMagic constructorMagic = threadLocalConstructorMagic.get();
        if (constructorMagic == null) {
            constructorMagic = new ConstructorMagic();
            threadLocalConstructorMagic.set(constructorMagic);
        }
        return constructorMagic;
    }

    @SuppressWarnings("unchecked")
    private static void checkConstructorMagic() {
        final ConstructorMagic constructorMagic = threadLocalConstructorMagic.get();
        if ((constructorMagic == null) || !constructorMagic.isActive()) {
            throw new IllegalArgumentException("StructuredArray<> must not be directly instantiated with a constructor. Use newInstance(...) instead.");
        }
    }


    public StructuredArray<T>[][] getLongAddressableSubArrays() {
        return longAddressableSubArrays;
    }

    public StructuredArray<T>[] getIntAddressableSubArrays() {
        return intAddressableSubArrays;
    }

    public T[][] getLongAddressableElements() {
        return longAddressableElements;
    }

    public T[] getIntAddressableElements() {
        return intAddressableElements;
    }
}
