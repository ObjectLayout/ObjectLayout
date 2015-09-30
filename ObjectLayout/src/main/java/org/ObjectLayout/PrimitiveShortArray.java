/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

/**
 * A subclassable array of shorts.
 *
 * {@link PrimitiveShortArray} is designed with semantics specifically chosen and restricted such that a "flat" memory
 * layout of the implemented data structure would be possible on optimizing JVMs. While fully functional
 * on all JVM implementation (of Java SE 7 and above), the semantics are such that a JVM may transparently
 * optimise the implementation to provide a compact contiguous layout that facilitates dead-reckoning (as
 * opposed to de-referenced) access to elements
 */

public class PrimitiveShortArray extends AbstractPrimitiveShortArray {

    /**
     * Get a reference to a short[] that represents the contents of this array. Will throw an
     * exception if array is too long to represent as a short[].
     *
     * @return a reference to a short[] that represents the contents of this array
     * @throws IllegalStateException if array is too long to represent as a short[]
     */
    public short[] asArray() throws IllegalStateException {
        return _asArray();
    }

    /**
     * Get the length of the array
     *
     * @return the length of the array
     */
    public long getLength() {
        return _getLength();
    }

    /**
     * Get the value of an element in the array.
     *
     * @param index the index of the element
     * @return the value of the element at the given index
     */
    public short get(final int index) {
        return _get(index);
    }


    /**
     * Get the value of an element in the array.
     *
     * @param index the index of the element
     * @return the value of the element at the given index
     */
    public short get(final long index) {
        return _get(index);
    }

    /**
     * set the value of an element in the array.
     *
     * @param index the index of the element to set
     * @param value the value to assign to the element
     */
    public void set(final int index, final short value) {
        _set(index, value);
    }

    /**
     * set the value of an element in the array.
     *
     * @param index the index of the element to set
     * @param value the value to assign to the element
     */
    public void set(final long index, final short value) {
        _set(index, value);
    }

    /**
     * Default constructor
     */
    public PrimitiveShortArray() {
        super();
    }

    /**
     * Copying constructor
     *
     * @param sourceArray the array to copy
     */
    public PrimitiveShortArray(PrimitiveShortArray sourceArray) {
        super(sourceArray);
    }

    /**
     * Create a new instance of {@link PrimitiveShortArray} with a given length.
     *
     * @param length the length of the array.
     * @return a new instance of {@link PrimitiveShortArray} with the given length
     */
    public static PrimitiveShortArray newInstance(final long length) {
        return AbstractPrimitiveArray._newInstance(noLookup, PrimitiveShortArray.class, length);
    }

    /**
     * Create a new instance of &lt;A extends {@link PrimitiveShortArray}&gt; with a given length.
     *
     * @param arrayClass The class of the array to be created (extends PrimitiveShortArray)
     * @param length the length of the array.
     * @param <A> The class of the array to be created (extends PrimitiveShortArray)
     * @return a new instance of &lt;A extends {@link PrimitiveShortArray}&gt; with a given length
     */
    public static <A extends PrimitiveShortArray> A newInstance(
            final Class<A> arrayClass,
            final long length) {
        return AbstractPrimitiveArray._newInstance(noLookup, arrayClass, length);
    }

    /**
     * Create a new instance of &lt;A extends {@link PrimitiveShortArray}&gt; with a given length.
     *
     * @param lookup The lookup object to use for accessing the array's constructor
     * @param arrayClass The class of the array to be created (extends PrimitiveShortArray)
     * @param length the length of the array.
     * @param <A> The class of the array to be created (extends PrimitiveShortArray)
     * @return a new instance of &lt;A extends {@link PrimitiveShortArray}&gt; with a given length
     */
    public static <A extends PrimitiveShortArray> A newInstance(
            MethodHandles.Lookup lookup,
            final Class<A> arrayClass,
            final long length) {
        return AbstractPrimitiveArray._newInstance(lookup, arrayClass, length);
    }

    /**
     * Create a new instance of &lt;A extends {@link PrimitiveShortArray}&gt; with a given length,
     * array constructor, and array constructor arguments.
     *
     * @param length The length of the array.
     * @param arrayCtorAndArgs for creating the array
     * @param <A> The class of the array to be created (extends PrimitiveShortArray)
     * @return a new instance of &lt;A extends {@link PrimitiveShortArray}&gt;
     */
    public static <A extends PrimitiveShortArray> A newInstance(
            final CtorAndArgs<A> arrayCtorAndArgs,
            final long length) {
        return AbstractPrimitiveArray._newInstance(arrayCtorAndArgs, length);
    }

    /**
     * Create a new instance of &lt;A extends {@link PrimitiveShortArray}&gt; with a given length,
     * array constructor, and array constructor arguments.
     *
     * @param length The length of the array.
     * @param arrayConstructor The array constructor to use
     * @param arrayConstructorArgs The arguments to pass to the array constructor
     * @param <A> The class of the array to be created (extends PrimitiveShortArray)
     * @return a new instance of &lt;A extends {@link PrimitiveShortArray}&gt;
     */
    public static <A extends PrimitiveShortArray> A newInstance(
            final long length,
            final Constructor<A> arrayConstructor,
            final Object... arrayConstructorArgs) {
        return AbstractPrimitiveArray._newInstance(length, arrayConstructor, arrayConstructorArgs);
    }

    /**
     * Create a new &lt;A extends {@link PrimitiveShortArray}&gt; instance, using a copy constructor to
     * replicate the contents of the given source array
     * @param source The array to replicate
     * @param <A> The class of the array to be created (extends PrimitiveShortArray)
     * @return a new instance of &lt;A extends {@link PrimitiveShortArray}&gt; copied from the source array
     * @throws NoSuchMethodException if &lt;A&gt; does not have a copy constructor
     */
    public static <A extends PrimitiveShortArray> A copyInstance(A source) throws NoSuchMethodException {
        return AbstractPrimitiveArray._copyInstance(noLookup, source);
    }

    /**
     * Create a new &lt;A extends {@link PrimitiveShortArray}&gt; instance, using a copy constructor to
     * replicate the contents of the given source array
     * @param lookup The lookup object to use for accessing the array's constructor
     * @param source The array to replicate
     * @param <A> The class of the array to be created (extends PrimitiveShortArray)
     * @return a new instance of &lt;A extends {@link PrimitiveShortArray}&gt; copied from the source array
     * @throws NoSuchMethodException if &lt;A&gt; does not have a copy constructor
     */
    public static <A extends PrimitiveShortArray> A copyInstance(
            MethodHandles.Lookup lookup,
            A source) throws NoSuchMethodException {
        return AbstractPrimitiveArray._copyInstance(lookup, source);
    }
}
