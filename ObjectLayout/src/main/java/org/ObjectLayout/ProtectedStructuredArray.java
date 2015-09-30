/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 *     An array of non-replaceable objects. This protected variant keeps access to the subclassed array protected
 *     such that inheriting classes can avoid unintentional exposure of the array and its contents to their users.
 * <p>
 *     A structured array contains array element objects of a fixed (at creation time, per array instance) class,
 *     and can support elements of any class that provides accessible constructors. The elements in a StructuredArray
 *     are all allocated and constructed at array creation time, and individual elements cannot be removed or
 *     replaced after array creation. Array elements can be accessed using an index-based accessor methods in
 *     the form of {@link ProtectedStructuredArray#get}() using either int or long indices. Individual element contents
 *     can then be accessed and manipulated using any and all operations supported by the member element's class.
 * <p>
 *     While simple creation of default-constructed elements and fixed constructor parameters are available through
 *     the newInstance factory methods, supporting arbitrary member types requires a wider range of construction
 *     options. The {@link CtorAndArgsProvider} API provides for array creation with arbitrary, user-supplied
 *     constructors and arguments, the selection of which can take the element index and construction context
 *     into account.
 * <p>
 *     StructuredArray is designed with semantics specifically chosen and restricted such that a "flat" memory
 *     layout of the implemented data structure would be possible on optimizing JVMs. Doing so provides for the
 *     possibility of matching access speed benefits that exist in data structures with similar semantics that
 *     are supported in other languages (e.g. an array of structs in C-like languages). While fully functional
 *     on all JVM implementation (of Java SE 7 and above), the semantics are such that a JVM may transparently
 *     optimise the implementation to provide a compact contiguous layout that facilitates consistent stride
 *     based memory access and dead-reckoning (as opposed to de-referenced) access to elements
 *
 * @param <T> The class of the array elements
 */
public class ProtectedStructuredArray<T> extends AbstractStructuredArray<T> {

    // Single-dimensional newInstance forms:

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <T> ProtectedStructuredArray<T> newInstance(
            final Class<T> elementClass,
            final long length) {
        @SuppressWarnings("unchecked")
        ProtectedStructuredArray<T> instance = _newInstance(ProtectedStructuredArray.class, elementClass, length);
        return instance;
    }

    /**
     * Create an array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <T> ProtectedStructuredArray<T> newInstance(
            MethodHandles.Lookup lookup,
            final Class<T> elementClass,
            final long length) {
        @SuppressWarnings("unchecked")
        ProtectedStructuredArray<T> instance = _newInstance(lookup, ProtectedStructuredArray.class, elementClass, length);
        return instance;
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param arrayClass of the array to create
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length) {
        return _newInstance(arrayClass, elementClass, length);
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements of
     * type <code>elementClass</code>. Using the <code>elementClass</code>'s default constructor.
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param arrayClass of the array to create
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            MethodHandles.Lookup lookup,
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length) {
        return _newInstance(lookup, arrayClass, elementClass, length);
    }

    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use <code>elementClass</code>'s default constructor.
     *
     * @param arrayCtorAndArgs for creating the array
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length) {
        return _newInstance(arrayCtorAndArgs, elementClass, length);
    }

    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use <code>elementClass</code>'s default constructor.
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param arrayCtorAndArgs for creating the array
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            MethodHandles.Lookup lookup,
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length) {
        return _newInstance(lookup, arrayCtorAndArgs, elementClass, length);
    }

    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use <code>elementClass</code>'s default constructor.
     *
     * @param arrayCtorAndArgs for creating the array
     * @param elementCtorAndArgs of each element in the array
     * @param length of the array to create
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final CtorAndArgs<T> elementCtorAndArgs,
            final long length) {
        return _newInstance(arrayCtorAndArgs, elementCtorAndArgs, length);
    }

    /**
     * Create an array of <code>length</code> elements of type <code>elementClass</code>. Use constructor and
     * arguments supplied (on a potentially per element index basis) by the specified
     * <code>ctorAndArgsProvider</code> to construct and initialize each element.
     *
     * @param elementClass of each element in the array
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis
     * @param length of the array to create
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <T> ProtectedStructuredArray<T> newInstance(
            final Class<T> elementClass,
            final CtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) {
        @SuppressWarnings("unchecked")
        ProtectedStructuredArray<T> instance = _newInstance(ProtectedStructuredArray.class, elementClass, length, ctorAndArgsProvider);
        return instance;
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Use constructor and arguments supplied (on a potentially
     * per element index basis) by the specified <code>ctorAndArgsProvider</code> to construct and initialize
     * each element.
     *
     * @param arrayClass of the array to create.
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length,
            final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        return _newInstance(arrayClass, elementClass, length, ctorAndArgsProvider);
    }

    /**
     * Create an <code>arrayClass</code> array of <code>length</code> elements, each containing an element object of
     * type <code>elementClass</code>. Use constructor and arguments supplied (on a potentially
     * per element index basis) by the specified <code>ctorAndArgsProvider</code> to construct and initialize
     * each element.
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param arrayClass of the array to create.
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            MethodHandles.Lookup lookup,
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length,
            final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        return _newInstance(lookup, arrayClass, elementClass, length, ctorAndArgsProvider);
    }

    /**
     * Create an <code>arrayCtorAndArgs.getConstructor().getDeclaringClass()</code> array of <code>length</code>
     * elements of type <code>elementClass</code>. Use constructor and arguments
     * supplied (on a potentially per element index basis) by the specified <code>ctorAndArgsProvider</code>
     * to construct and initialize each element.
     *
     * @param arrayCtorAndArgs of the array to create
     * @param elementClass of each element in the array
     * @param length of the array to create
     * @param ctorAndArgsProvider produces element constructors [potentially] on a per element basis
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length,
            final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        return _newInstance(arrayCtorAndArgs, elementClass, length, ctorAndArgsProvider);
    }

    /**
     * Create a &lt;S extends StructuredArray&lt;T&gt;&gt; array instance with elements copied from a source
     * collection.
     * @param arrayClass of the array to create.
     * @param elementClass of each element in the array
     * @param sourceCollection provides details for building the array
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final Collection<T> sourceCollection) {
        return _newInstance(arrayClass, elementClass, sourceCollection);
    }

    /**
     * Create a &lt;S extends StructuredArray&lt;T&gt;&gt; array instance with elements copied from a source
     * collection.
     * @param lookup The lookup object to use when resolving constructors
     * @param arrayClass of the array to create.
     * @param elementClass of each element in the array
     * @param sourceCollection provides details for building the array
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S newInstance(
            MethodHandles.Lookup lookup,
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final Collection<T> sourceCollection) {
        return _newInstance(lookup, arrayClass, elementClass, sourceCollection);
    }

    /**
     * Create a &lt;S extends StructuredArray&lt;T&gt;&gt; array instance according to the details provided in the
     * arrayBuilder.
     * @param arrayBuilder provides details for building the array
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends StructuredArray<T>, T> S newInstance(
            final StructuredArrayBuilder<S, T> arrayBuilder) {
        return _newInstance(arrayBuilder);
    }

    /**
     * Copy a given array of elements to a newly created array. Copying of individual elements is done by using
     * the <code>elementClass</code> copy constructor to construct the individual member elements of the new
     * array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to duplicate
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S copyInstance(
            final S source) {
        return _copyInstance(source);
    }

    /**
     * Copy a given array of elements to a newly created array. Copying of individual elements is done by using
     * the <code>elementClass</code> copy constructor to construct the individual member elements of the new
     * array based on the corresponding elements of the <code>source</code> array.
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param source The array to duplicate
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S copyInstance(
            MethodHandles.Lookup lookup,
            final S source) {
        return _copyInstance(lookup, source, 0, source.getLength());
    }

    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     *
     * @param source The array to copy from
     * @param sourceOffset offset index, indicating where the source region to be copied begins
     * @param count the number of elements to copy
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S copyInstance(
            final S source,
            final long sourceOffset,
            final long count) {
        return _copyInstance(source, sourceOffset, count);
    }

    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param source The array to copy from
     * @param sourceOffset offset index, indicating where the source region to be copied begins
     * @param count the number of elements to copy
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    protected static <S extends ProtectedStructuredArray<T>, T> S copyInstance(
            MethodHandles.Lookup lookup,
            final S source,
            final long sourceOffset,
            final long count) {
        return _copyInstance(lookup, source, new long[]{sourceOffset}, new long[]{count});
    }

    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     * <p>
     * This form is useful [only] for copying partial ranges from nested StructuredArrays.
     * </p>
     * @param source The array to copy from
     * @param sourceOffsets offset indexes, indicating where the source region to be copied begins at each
     *                      StructuredArray nesting depth
     * @param counts the number of elements to copy at each StructuredArray nesting depth
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    @SuppressWarnings("unchecked")
    protected static <S extends ProtectedStructuredArray<T>, T> S copyInstance(
            final S source,
            final long[] sourceOffsets,
            final long[] counts) {
        return _copyInstance(source, sourceOffsets, counts);
    }

    /**
     * Copy a range from an array of elements to a newly created array. Copying of individual elements is done
     * by using the <code>elementClass</code> copy constructor to construct the individual member elements of
     * the new array based on the corresponding elements of the <code>source</code> array.
     * <p>
     * This form is useful [only] for copying partial ranges from nested StructuredArrays.
     * </p>
     * @param lookup The lookup object to use when resolving constructors
     * @param source The array to copy from
     * @param sourceOffsets offset indexes, indicating where the source region to be copied begins at each
     *                      StructuredArray nesting depth
     * @param counts the number of elements to copy at each StructuredArray nesting depth
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    @SuppressWarnings("unchecked")
    protected static <S extends ProtectedStructuredArray<T>, T> S copyInstance(
            MethodHandles.Lookup lookup,
            final S source,
            final long[] sourceOffsets,
            final long[] counts) {
        return _copyInstance(lookup, source, sourceOffsets, counts);
    }

    protected ProtectedStructuredArray() {
    }

    protected ProtectedStructuredArray(ProtectedStructuredArray<T> sourceArray) {
        // Support copy constructor. When we get here, everything is already set up for the regular
        // (default) construction path to perform the required copy.
        // Copying will actually be done according to the CtorAndArgsProvider and context already supplied,
        // with top-most source array being (already) passed as the contextCookie in the builder's
        // construction context, and copying proceeding using the context indices and the supplied
        // contextCookie provided by each CtorAndArgsProvider to figure out individual sources.
        super(sourceArray);
    }

    /**
     * Get the length (number of elements) of the array.
     *
     * @return the number of elements in the array.
     */
    protected long getLength() {
        return super.getLength();
    }


    /**
     * Get the {@link Class} of elements stored in the array.
     *
     * @return the {@link Class} of elements stored in the array.
     */
    protected Class<T> getElementClass() {
        return super.getElementClass();
    }

    /**
     * Get the array model
     * @return a model of this array
     */
    protected StructuredArrayModel<? extends StructuredArray<T>, T> getArrayModel() {
        @SuppressWarnings("unchecked")
        StructuredArrayModel<? extends StructuredArray<T>, T> model =
                (StructuredArrayModel<? extends StructuredArray<T>, T>) super.getArrayModel();
        return model;
    }

    /**
     * Get a reference to an element in a single dimensional array, using a <code>long</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    protected T get(final long index) throws IllegalArgumentException {
        return super.get(index);
    }

    /**
     * Get a reference to an element in a single dimensional array, using an <code>int</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    protected T get(final int index) throws IllegalArgumentException {
        return super.get(index);
    }
    
    //
    //
    // Shallow copy support:
    //
    //

    /**
     * Shallow copy a region of element object contents from one array to the other.
     * <p>
     * shallowCopy will copy all fields from each of the source elements to the corresponding fields in each
     * of the corresponding destination elements. If the same array is both the src and dst then the copy will
     * happen as if a temporary intermediate array was used.
     *
     * @param src array to copy
     * @param srcOffset offset index in src where the region begins
     * @param dst array into which the copy should occur
     * @param dstOffset offset index in the dst where the region begins
     * @param count of structure elements to copy
     * @param <S> The class of the arrays
     * @param <T> The class of the array elements
     * @throws IllegalArgumentException if the source and destination array element types are not identical, or if
     * the source or destination arrays have nested StructuredArrays within them, or if final fields are discovered
     * and all allowFinalFieldOverwrite is not true.
     */
    protected static <S extends ProtectedStructuredArray<T>, T> void shallowCopy(
            final S src,
            final long srcOffset,
            final S dst,
            final long dstOffset,
            final long count) {
        _shallowCopy(src, srcOffset, dst, dstOffset, count);
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
     * @param src array to copy
     * @param srcOffset offset index in src where the region begins
     * @param dst array into which the copy should occur
     * @param dstOffset offset index in the dst where the region begins
     * @param count of structure elements to copy.
     * @param allowFinalFieldOverwrite allow final fields to be overwritten during a copy operation.
     * @param <S> The class of the arrays
     * @param <T> The class of the array elements
     * @throws IllegalArgumentException if the source and destination array element types are not identical, or if
     * the source or destination arrays have nested StructuredArrays within them, or if final fields are discovered
     * and all allowFinalFieldOverwrite is not true.
     */
    protected static <S extends ProtectedStructuredArray<T>, T> void shallowCopy(
            final S src,
            final long srcOffset,
            final S dst,
            final long dstOffset,
            final long count,
            final boolean allowFinalFieldOverwrite) {
        _shallowCopy(src, srcOffset, dst, dstOffset, count, allowFinalFieldOverwrite);
    }
}
