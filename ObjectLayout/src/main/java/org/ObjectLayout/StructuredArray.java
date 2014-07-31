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
 *     the form of {@link StructuredArray#get}() using either int or
 *     long indices. Individual element contents can then be accessed and manipulated using any and all
 *     operations supported by the member element's class.
 * <p>
 *     While simple creation of default-constructed elements and fixed constructor parameters are available through
 *     the newInstance factory methods, supporting arbitrary member types requires a wider range of construction
 *     options. The CtorAndArgsProvider API provides for array creation with arbitrary, user-supplied
 *     constructors and arguments, either of which can take the element index into account.
 * <p>
 *     StructuredArray is designed with semantics specifically restricted to be consistent with layouts of an
 *     array of structures in C-like languages. While fully functional on all JVM implementation (of Java SE 6
 *     and above), the semantics are such that a JVM may transparently optimise the implementation to provide a
 *     compact contiguous layout that facilitates consistent stride based memory access and dead-reckoning
 *     (as opposed to de-referenced) access to elements
 *
 * @param <T> type of the element occupying each array slot.
 */
public class StructuredArray<T> extends StructuredArrayIntrinsifiableBase<T> implements Iterable<T> {


    private static final Object[] EMPTY_ARGS = new Object[0];

    private final long totalElementCount; // A cached product of lengths[i]

    private final Field[] fields;
    private final boolean hasFinalFields;

    // Single-dimensional newInstance forms:

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param length of the array to create.
     * @param elementClass of each element in the array
     * @throws NoSuchMethodException if the element class does not have a public default constructor
     */

    public static <T> StructuredArray<T> newInstance(
            final Class<T> elementClass,
            final long length) throws NoSuchMethodException {
        final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider = new SingletonCtorAndArgsProvider<T>(elementClass);
        final long[] lengths = {length};
        return instantiate(elementClass, ctorAndArgsProvider, lengths);
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param arrayClass of the array to create.
     * @param length of the array to create.
     * @param elementClass of each element in the array
     */
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length) {
        try {
            Constructor<S> ctor = arrayClass.getConstructor();
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<S>(ctor, EMPTY_ARGS);
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

    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length) {
        try {
            final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider =
                    new SingletonCtorAndArgsProvider<T>(elementClass);
            final long[] lengths = {length};
            return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create an array of <code>length</code> elements of type <code>elementClass</code>. Use constructor and
     * arguments supplied (on a potentially per element index basis) by the specified
     * <code>ctorAndArgsProvider</code> to construct and initialize each element.
     *
     * @param elementClass of each element in the array
     * @param length of the array to create.
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final SingleDimensionalCtorAndArgsProvider<T> ctorAndArgsProvider,
                                                     final long length) throws NoSuchMethodException {
        final long[] lengths = {length};
        return instantiate(elementClass, ctorAndArgsProvider, lengths);
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
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final SingleDimensionalCtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) {
        try {
            Constructor<S> ctor = arrayClass.getConstructor();
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<S>(ctor, EMPTY_ARGS);
            final long[] lengths = {length};
            return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
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
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final SingleDimensionalCtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) {
        final long[] lengths = {length};
        return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
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
    @SuppressWarnings("rawtypes")
    public static <T> StructuredArray<T> newInstance(
            final Class<T> elementClass,
            final long length,
            final Class[] elementConstructorArgTypes,
            final Object... elementConstructorArgs) throws NoSuchMethodException {
        final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider =
                new SingletonCtorAndArgsProvider<T>(elementClass, elementConstructorArgTypes, elementConstructorArgs);
        final long[] lengths = {length};
        return instantiate(elementClass, ctorAndArgsProvider, lengths);
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
    @SuppressWarnings("rawtypes")
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length,
            final Class[] elementConstructorArgTypes,
            final Object... elementConstructorArgs) {
        try {
            Constructor<S> ctor = arrayClass.getConstructor();
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<S>(ctor, EMPTY_ARGS);
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
    @SuppressWarnings("rawtypes")
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length,
            final Class[] elementConstructorArgTypes,
            final Object... elementConstructorArgs) {
        try {
            final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider =
                    new SingletonCtorAndArgsProvider<T>(elementClass, elementConstructorArgTypes, elementConstructorArgs);
            final long[] lengths = {length};
            return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
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
        final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider = new SingletonCtorAndArgsProvider<T>(elementClass);
        return instantiate(elementClass, ctorAndArgsProvider, lengths);
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
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long... lengths) {
        try {
            Constructor<S> ctor = arrayClass.getConstructor();
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<S>(ctor, EMPTY_ARGS);
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
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass, 
            final long... lengths) {
        try {
            final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider = new SingletonCtorAndArgsProvider<T>(elementClass);
            return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
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
    @SuppressWarnings("rawtypes")
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final long[] lengths,
                                                     final Class[] elementConstructorArgTypes,
                                                     final Object... elementConstructorArgs) throws NoSuchMethodException {
        final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider =
                new SingletonCtorAndArgsProvider<T>(elementClass, elementConstructorArgTypes, elementConstructorArgs);
        return instantiate(elementClass, ctorAndArgsProvider, lengths);
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
    @SuppressWarnings("rawtypes")
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long[] lengths,
            final Class[] elementConstructorArgTypes,
            final Object... elementConstructorArgs) {
        try {
            Constructor<S> ctor = arrayClass.getConstructor();
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<S>(ctor, EMPTY_ARGS);
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
    @SuppressWarnings("rawtypes")
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass, 
            final long[] lengths,
            final Class[] elementConstructorArgTypes,
            final Object... elementConstructorArgs) {
        try {
            final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider =
                    new SingletonCtorAndArgsProvider<T>(elementClass, elementConstructorArgTypes, elementConstructorArgs);
            return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
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
     * @param elementClass of each element in the array
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis.
     * @param lengths of the array dimensions to create.
     * @throws NoSuchMethodException if the element class does not not support a supplied constructor
     */
    public static <T> StructuredArray<T> newInstance(final Class<T> elementClass,
                                                     final MultiDimensionalCtorAndArgsProvider<T> ctorAndArgsProvider,
                                                     final long... lengths) throws NoSuchMethodException {
        return instantiate(elementClass, ctorAndArgsProvider, lengths);
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
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final MultiDimensionalCtorAndArgsProvider<T> ctorAndArgsProvider,
            final long... lengths) {
        try {
            Constructor<S> ctor = arrayClass.getConstructor();
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<S>(ctor, EMPTY_ARGS);
            return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
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
    public static <T, S extends StructuredArray<T>> S newSubclassInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final MultiDimensionalCtorAndArgsProvider<T> ctorAndArgsProvider,
            final long... lengths) {
        return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
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

        final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider =
                new CopyCtorAndArgsProvider<T>(source.getElementClass(), source, sourceOffsets, false);
        CtorAndArgs<StructuredArray<T>> arrayCtorAndArgs =
                new CtorAndArgs<StructuredArray<T>>(((Class<StructuredArray<T>>) source.getClass()).getConstructor(),
                        EMPTY_ARGS);
        return instantiate(arrayCtorAndArgs, source.getElementClass(), ctorAndArgsProvider, counts);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T, S extends StructuredArray<T>> S instantiate(
            final Class<T> elementClass,
            final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider,
            final long... lengths) throws NoSuchMethodException {
        Class arrayClass = StructuredArray.class;
        CtorAndArgs<S> arrayCtorAndArgs =
                new CtorAndArgs<S>(arrayClass.getConstructor(), EMPTY_ARGS);
        return instantiate(arrayCtorAndArgs, elementClass, ctorAndArgsProvider, lengths);
    }

    private static <T, S extends StructuredArray<T>> S instantiate(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider,
            final long[] lengths) {
        if (!(((lengths.length == 1) && (ctorAndArgsProvider instanceof SingleDimensionalCtorAndArgsProvider)) ||
                (ctorAndArgsProvider instanceof MultiDimensionalCtorAndArgsProvider))) {
            throw new IllegalArgumentException("arrayCtorAndArgs must be and instance of" +
                    ((lengths.length == 1) ? "either SingleDimensionalCtorAndArgsProvider or" : "") +
                    "MultiDimensionalCtorAndArgsProvider");
        }
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(arrayCtorAndArgs, ctorAndArgsProvider, null);
        try {
            constructorMagic.setActive(true);
            return StructuredArrayIntrinsifiableBase.instantiateStructuredArray(elementClass, arrayCtorAndArgs,
                    ctorAndArgsProvider, lengths);
        } finally {
            constructorMagic.setActive(false);
        }
    }

    @SuppressWarnings("rawtypes")
    public StructuredArray() {
        checkConstructorMagic();
        // Extract locally needed args from constructor magic:
        ConstructorMagic constructorMagic = getConstructorMagic();
        @SuppressWarnings("unchecked")
        final CtorAndArgs<? extends StructuredArray<T>> arrayCtorAndArgs = constructorMagic.getArrayCtorAndArgs();
        @SuppressWarnings("unchecked")
        final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider = constructorMagic.getCtorAndArgsProvider();
        final long[] containingIndex = constructorMagic.getContainingIndex();

        // Finish consuming constructMagic arguments:
        constructorMagic.setActive(false);

        // Compute and cache total element count:
        long totalCount = 1;
        for (long length : getLengths()) {
            totalCount *= length;
        }
        totalElementCount = totalCount;

        final Field[] fields = removeStaticFields(getElementClass().getDeclaredFields());
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        this.fields = fields;
        this.hasFinalFields = containsFinalQualifiedFields(fields);

        long[] lengths = getLengths();
        int dimensionCount = getDimensionCount();

        try {
            if (dimensionCount > 1) {
                // This is an array of arrays. Pass the ctorAndArgsProvider through to
                // a subArrayCtorAndArgsProvider that will be used to populate the sub-array:
                final long[] subArrayLengths = new long[lengths.length - 1];
                System.arraycopy(lengths, 1, subArrayLengths, 0, subArrayLengths.length);

                final ArrayConstructionArgs subArrayArgs =
                        new ArrayConstructionArgs(arrayCtorAndArgs, getElementClass(),
                                ctorAndArgsProvider, subArrayLengths, null);
                @SuppressWarnings("unchecked")
                final ArrayCtorAndArgsProvider<StructuredArray<T>> subArrayCtorAndArgsProvider =
                        new ArrayCtorAndArgsProvider(arrayCtorAndArgs.getConstructor(), subArrayArgs);

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
        return super.getDimensionCount();
    }

    /**
     * Get the lengths (number of elements per dimension) of the array.
     *
     * @return the number of elements in each dimension in the array.
     */
    public long[] getLengths() {
        return super.getLengths();
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
        return super.getLength();
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
        return super.get(indices, 0);
    }

    /**
     * Get a reference to an element in an N dimensional array, using N int indices (or a <code>int[N]</code> index array).
     *
     * @param indices The indices (at each dimension) of the element to retrieve.
     * @return a reference to the indexed element.
     * @throws IllegalArgumentException if number of indices does not match number of dimensions in the array
     */
    public T get(final int... indices) throws IllegalArgumentException {
        return super.get(indices, 0);
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
        return super.get(index);
    }

    /**
     * Get a reference to an element in a two dimensional array, using 2 <code>long</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @return the element at [index0, index1]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final long index0, final long index1) throws IllegalArgumentException {
        return super.get(index0, index1);
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
        return super.get(index0, index1, index2);

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
        return super.get(index);
    }

    /**
     * Get a reference to an element in a two dimensional array, using 2 <code>int</code> indexes.
     * @param index0 the first index (in the first array dimension) of the element to retrieve
     * @param index1 the second index (in the second array dimension) of the element to retrieve
     * @return the element at [index0, index1]
     * @throws NullPointerException if number of indexes does not match number of dimensions in the array
     */
    public T get(final int index0, final int index1) throws IllegalArgumentException {
        return super.get(index0, index1);
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
        return super.get(index0, index1, index2);
    }

    // Type specific public gets of first dimension subarray:

    /**
     * Get a reference to a StructuredArray element in this array, using a <code>long</code> index.
     * @param index (in this array's first dimension) of the StructuredArray to retrieve
     * @return a reference to the StructuredArray located at [index] in the first dimension of this array
     * @throws IllegalArgumentException if array has less than two dimensions
     */
    public StructuredArray<T> getSubArray(final long index) throws IllegalArgumentException {
        return super.getSubArray(index);
    }

    /**
     * Get a reference to a StructuredArray element in this array, using a <code>int</code> index.
     * @param index (in this array's first dimension) of the StructuredArray to retrieve
     * @return a reference to the StructuredArray located at [index] in the first dimension of this array
     * @throws IllegalArgumentException if array has less than two dimensions
     */
    public StructuredArray<T> getSubArray(final int index) throws IllegalArgumentException {
        return super.getSubArray(index);
    }

    private void populateElement(final long index0,
                                 final Constructor<T> constructor,
                                 Object... args) {
        try {
            // Instantiate:
            constructElementAtIndex(index0, constructor, args);
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
                                  ArrayConstructionArgs args) {
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(
                args.arrayCtorAndArgs, args.ctorAndArgsProvider, args.containingIndex);
        try {
            constructorMagic.setActive(true);
            // Instantiate:
            constructSubArrayAtIndex(index0, constructor, args);
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

    private void populateLeafElements(final AbstractCtorAndArgsProvider<T> ctorAndArgsProvider,
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

        long length = getLength();

        try {
            for (long index0 = 0; index0 < length; index0++) {
                final CtorAndArgs<T> ctorAndArgs;
                if (index != null) {
                    index[thisIndex] = index0;
                    ctorAndArgs = ((MultiDimensionalCtorAndArgsProvider<T>) ctorAndArgsProvider).getForIndex(index);
                } else {
                    if (ctorAndArgsProvider instanceof SingleDimensionalCtorAndArgsProvider)
                        ctorAndArgs = ((SingleDimensionalCtorAndArgsProvider<T>) ctorAndArgsProvider).getForIndex(index0);
                    else
                        ctorAndArgs = ((MultiDimensionalCtorAndArgsProvider<T>) ctorAndArgsProvider).getForIndex(index0);
                }
                if (ctorAndArgs.getConstructor().getDeclaringClass() != getElementClass()) {
                    throw new IllegalArgumentException("ElementClass (" + getElementClass() +
                            ") does not match ctorAndArgs.getConstructor().getDeclaringClass() (" +
                            ctorAndArgs.getConstructor().getDeclaringClass() + ")");
                }
                populateElement(index0, ctorAndArgs.getConstructor(), ctorAndArgs.getArgs());
                if (ctorAndArgsProvider instanceof CtorAndArgsProvider) {
                    ((CtorAndArgsProvider<T>) ctorAndArgsProvider).recycle(ctorAndArgs);
                }
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

        long length = getLength();

        try {
            for (long index0 = 0; index0 < length; index0++) {
                final CtorAndArgs<StructuredArray<T>> ctorAndArgs;
                if (index != null) {
                    index[thisIndex] = index0;
                    ctorAndArgs = arrayCtorAndArgsProvider.getForIndex(index);
                } else {
                    ctorAndArgs = arrayCtorAndArgsProvider.getForIndex(index0);
                }
                populateSubArray(index0, ctorAndArgs.getConstructor(), (ArrayConstructionArgs) ctorAndArgs.getArgs()[0]);
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
        return super.getElementClass();
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

        private final long[] cursors = new long[getDimensionCount()];
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

            long lengths[] = getLengths();

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
    @SuppressWarnings("rawtypes")
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
    @SuppressWarnings("rawtypes")
    public static void shallowCopy(final StructuredArray src, final long srcOffset,
                                   final StructuredArray dst, final long dstOffset,
                                   final long count,
                                   final boolean allowFinalFieldOverwrite) {
        if (src.getElementClass() != dst.getElementClass()) {
            String msg = String.format("Only objects of the same class can be copied: %s != %s",
                    src.getClass(), dst.getClass());
            throw new ArrayStoreException(msg);
        }
        if ((src.getDimensionCount() > 1) || (dst.getDimensionCount() > 1)) {
            throw new IllegalArgumentException("shallowCopy only supported for single dimension arrays");
        }

        final Field[] fields = src.fields;
        if (!allowFinalFieldOverwrite && dst.hasFinalFields) {
            throw new IllegalArgumentException("Cannot shallow copy onto final fields");
        }

        if (((srcOffset + count) < Integer.MAX_VALUE) && ((dstOffset + count) < Integer.MAX_VALUE)) {
            // use the (faster) int based get
            if (dst == src && (dstOffset >= srcOffset && (dstOffset + count) >= srcOffset)) {
                int srcIdx = (int)(srcOffset + count) - 1;
                int dstIdx = (int)(dstOffset + count) - 1;
                int limit = (int)(srcOffset - 1);
                for (; srcIdx > limit; srcIdx--, dstIdx--) {
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

    //
    //
    // ConstructorMagic support:
    //
    //

    /**
     * OPTIMIZATION NOTE: The ConstructorMagic will likely not need to be modified in any way even for
     * optimized JDK implementations. It resides in this class for scoping reasons.
     */

    static class ConstructorMagic {
        private boolean isActive() {
            return active;
        }

        private void setActive(boolean active) {
            this.active = active;
        }

        public void setConstructionArgs(CtorAndArgs arrayCtorAndArgs,
                                        AbstractCtorAndArgsProvider ctorAndArgsProvider,
                                        long[] containingIndex) {
            this.arrayCtorAndArgs = arrayCtorAndArgs;
            this.ctorAndArgsProvider = ctorAndArgsProvider;
            this.containingIndex = containingIndex;
        }

        public CtorAndArgs getArrayCtorAndArgs() {
            return arrayCtorAndArgs;
        }

        public AbstractCtorAndArgsProvider getCtorAndArgsProvider() {
            return ctorAndArgsProvider;
        }

        public long[] getContainingIndex() {
            return containingIndex;
        }

        private boolean active = false;

        private CtorAndArgs arrayCtorAndArgs = null;
        private AbstractCtorAndArgsProvider ctorAndArgsProvider = null;
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
}
