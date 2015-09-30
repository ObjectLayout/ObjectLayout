/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;

/**
 *     An array of non-replaceable objects.
 * <p>
 *     A structured array contains array element objects of a fixed (at creation time, per array instance) class,
 *     and can support elements of any class that provides accessible constructors. The elements in a StructuredArray
 *     are all allocated and constructed at array creation time, and individual elements cannot be removed or
 *     replaced after array creation. Array elements can be accessed using an index-based accessor methods in
 *     the form of {@link AbstractStructuredArray#get}() using either int or long indices. Individual element contents
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
abstract class AbstractStructuredArray<T> extends AbstractStructuredArrayBase<T> {

    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];
    private static final MethodHandles.Lookup noLookup = null;

    final Field[] fields;
    final boolean hasFinalFields;

    private final StructuredArrayModel<? extends AbstractStructuredArray<T>, T> arrayModel;

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
    static <T> AbstractStructuredArray<T> _newInstance(
            final Class<T> elementClass,
            final long length) {
        @SuppressWarnings("unchecked")
        AbstractStructuredArray<T> instance = _newInstance(AbstractStructuredArray.class, elementClass, length);
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
    static <T> AbstractStructuredArray<T> _newInstance(
            MethodHandles.Lookup lookup,
            final Class<T> elementClass,
            final long length) {
        @SuppressWarnings("unchecked")
        AbstractStructuredArray<T> instance = _newInstance(lookup, AbstractStructuredArray.class, elementClass, length);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length) {
        return _newInstance(noLookup, arrayClass, elementClass, length);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            MethodHandles.Lookup lookup,
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length) {
            CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<>(lookup, arrayClass, EMPTY_ARG_TYPES, EMPTY_ARGS);
            return _newInstance(lookup, arrayCtorAndArgs, elementClass, length);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length) {
        return _newInstance(noLookup, arrayCtorAndArgs, elementClass, length);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            MethodHandles.Lookup lookup,
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length) {
        AbstractStructuredArrayBuilder<S, T> arrayBuilder = new AbstractStructuredArrayBuilder<>(
                lookup,
                arrayCtorAndArgs.getConstructor().getDeclaringClass(),
                elementClass,
                length).
                arrayCtorAndArgs(arrayCtorAndArgs).
                resolve();
        return instantiate(arrayBuilder);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final CtorAndArgs<T> elementCtorAndArgs,
            final long length) {
        AbstractStructuredArrayBuilder<S, T> arrayBuilder = new AbstractStructuredArrayBuilder<>(
                arrayCtorAndArgs.getConstructor().getDeclaringClass(),
                elementCtorAndArgs.getConstructor().getDeclaringClass(),
                length).
                arrayCtorAndArgs(arrayCtorAndArgs).
                elementCtorAndArgs(elementCtorAndArgs).
                resolve();
        return instantiate(arrayBuilder);
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
    static <T> AbstractStructuredArray<T> _newInstance(
            final Class<T> elementClass,
            final CtorAndArgsProvider<T> ctorAndArgsProvider,
            final long length) {
            return instantiate(elementClass, length, ctorAndArgsProvider);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length,
            final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        return _newInstance(noLookup, arrayClass, elementClass, length, ctorAndArgsProvider);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            MethodHandles.Lookup lookup,
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final long length,
            final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        CtorAndArgs<S> arrayCtorAndArgs = new CtorAndArgs<>(lookup, arrayClass, EMPTY_ARG_TYPES, EMPTY_ARGS);
        return instantiate(arrayCtorAndArgs, elementClass, length, ctorAndArgsProvider);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length,
            final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        return instantiate(arrayCtorAndArgs, elementClass, length, ctorAndArgsProvider);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final Collection<T> sourceCollection) {
        return _newInstance(noLookup, arrayClass, elementClass, sourceCollection);
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
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            MethodHandles.Lookup lookup,
            final Class<S> arrayClass,
            final Class<T> elementClass,
            final Collection<T> sourceCollection) {

        long length = sourceCollection.size();
        for (T element : sourceCollection) {
            if (element.getClass() != elementClass) {
                throw new IllegalArgumentException(
                        "Collection contains elements of type other than elementClass " + elementClass.getName());
            }
        }

        final CtorAndArgs<T> copyCtorAndArgs;
        final Object[] args = new Object[1];
        try {
            copyCtorAndArgs = new CtorAndArgs<>(lookup, elementClass, new Class[] {elementClass}, args);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Failed to locate copy constructor and args for elementClass " +
                    elementClass.getName() + ".", ex);
        }

        final Iterator<T> sourceIterator = sourceCollection.iterator();

        AbstractStructuredArrayBuilder<S, T> arrayBuilder = new AbstractStructuredArrayBuilder<>(
                lookup,
                arrayClass,
                elementClass,
                length).
                elementCtorAndArgsProvider(
                        new CtorAndArgsProvider<T>() {
                            @Override
                            public CtorAndArgs<T> getForContext(ConstructionContext<T> context) {
                                args[0] = sourceIterator.next();
                                return copyCtorAndArgs.setArgs(args);
                            }
                        }
                );

        return instantiate(arrayBuilder);
    }

    /**
     * Create a &lt;S extends StructuredArray&lt;T&gt;&gt; array instance according to the details provided in the
     * arrayBuilder.
     * @param arrayBuilder provides details for building the array
     * @param <S> The class of the array to be created
     * @param <T> The class of the array elements
     * @return The newly created array
     */
    static <S extends AbstractStructuredArray<T>, T> S _newInstance(
            final AbstractStructuredArrayBuilder<S, T> arrayBuilder) {
        return instantiate(arrayBuilder);
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
    static <S extends AbstractStructuredArray<T>, T> S _copyInstance(
            final S source) {
        return _copyInstance(noLookup, source);
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
    static <S extends AbstractStructuredArray<T>, T> S _copyInstance(
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
    static <S extends AbstractStructuredArray<T>, T> S _copyInstance(
            final S source,
            final long sourceOffset,
            final long count) {
        return _copyInstance(noLookup, source, sourceOffset, count);
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
    static <S extends AbstractStructuredArray<T>, T> S _copyInstance(
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
    static <S extends AbstractStructuredArray<T>, T> S _copyInstance(
            final S source,
            final long[] sourceOffsets,
            final long[] counts) {
        return _copyInstance(noLookup, source, sourceOffsets, counts);
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
    static <S extends AbstractStructuredArray<T>, T> S _copyInstance(
            MethodHandles.Lookup lookup,
            final S source,
            final long[] sourceOffsets,
            final long[] counts) {
        if (sourceOffsets.length != counts.length) {
            throw new IllegalArgumentException("sourceOffsets.length must match counts.length");
        }

        // Verify source ranges fit in model:
        int depth = 0;
        StructuredArrayModel arrayModel = source.getArrayModel();
        while((depth < counts.length) && (arrayModel != null) && StructuredArrayModel.class.isInstance(arrayModel)) {
            if (arrayModel.getLength() < sourceOffsets[depth] + counts[depth]) {
                throw new ArrayIndexOutOfBoundsException(
                        "At nesting depth " + depth + ", source length (" + arrayModel.getLength() +
                                ") is smaller than sourceOffset (" + sourceOffsets[depth] +
                                ") + count (" + counts[depth] + ")" );
            }
            arrayModel = (StructuredArrayModel) arrayModel.getStructuredSubArrayModel();
            depth++;
        }

        // If we run out of model depth before we run out of sourceOffsets and counts, throw:
        if (depth < counts.length) {
            throw new IllegalArgumentException("sourceOffsets.length and counts.length (" + counts.length +
                    ") must not exceed StructuredArray nesting depth (" + depth + ")");
        }

        final StructuredArrayModel<S, T> sourceArrayModel = (StructuredArrayModel<S, T>) source.getArrayModel();
        final Class<S> sourceArrayClass = sourceArrayModel.getArrayClass();
        CtorAndArgs<S> arrayCtorAndArgs =
                new CtorAndArgs<>(lookup, sourceArrayClass, new Class[] {sourceArrayClass}, source);

        final AbstractStructuredArrayBuilder<S, T> arrayBuilder =
                createCopyingArrayBuilder(lookup, sourceArrayModel, sourceOffsets, 0, counts, 0).
                        arrayCtorAndArgs(arrayCtorAndArgs).
                        contextCookie(source);

        return instantiate(arrayBuilder);
    }

    private static <S extends AbstractStructuredArray<T>, T> AbstractStructuredArrayBuilder<S, T> createCopyingArrayBuilder(
            MethodHandles.Lookup lookup,
            final StructuredArrayModel<S, T> sourceArrayModel,
            final long[] sourceOffsets, final int offsetsIndex,
            final long[] counts, final int countsIndex) {
        final Class<S> sourceArrayClass = sourceArrayModel.getArrayClass();
        final Class<T> elementClass = sourceArrayModel.getElementClass();

        long sourceOffset = (offsetsIndex < sourceOffsets.length) ? sourceOffsets[offsetsIndex] : 0;
        long count = (countsIndex < counts.length) ? counts[countsIndex] : sourceArrayModel.getLength();

        final CtorAndArgsProvider<T> elementCopyCtorAndArgsProvider =
                new CopyCtorAndArgsProvider<>(lookup, elementClass, sourceOffset);

        if (sourceArrayModel.getStructuredSubArrayModel() != null) {
            // This array contains another array:
            AbstractStructuredArrayBuilder subArrayBuilder =
                    createCopyingArrayBuilder(lookup,
                            (StructuredArrayModel)sourceArrayModel.getStructuredSubArrayModel(),
                            sourceOffsets, offsetsIndex + 1,
                            counts, countsIndex + 1);
            @SuppressWarnings("unchecked")
            AbstractStructuredArrayBuilder<S, T> builder =
                    new AbstractStructuredArrayBuilder<>(lookup, sourceArrayClass, subArrayBuilder, count).
                            elementCtorAndArgsProvider(elementCopyCtorAndArgsProvider).
                            resolve();
            return builder;
        } else if (sourceArrayModel.getPrimitiveSubArrayModel() != null) {
            // This array contains elements that are PrimitiveArrays:
            PrimitiveArrayModel model = (PrimitiveArrayModel) sourceArrayModel.getPrimitiveSubArrayModel();
            @SuppressWarnings("unchecked")
            PrimitiveArrayBuilder subArrayBuilder =
                    new PrimitiveArrayBuilder(model.getArrayClass(), model.getLength());
            @SuppressWarnings("unchecked")
            AbstractStructuredArrayBuilder<S, T> builder =
                    new AbstractStructuredArrayBuilder<>(lookup, sourceArrayClass, subArrayBuilder, count).
                    elementCtorAndArgsProvider(elementCopyCtorAndArgsProvider).
                    resolve();
            return builder;

        } else {
            // This is a leaf array (it's elements are regular objects):
            return new AbstractStructuredArrayBuilder<>(lookup, sourceArrayClass, elementClass, count).
                    elementCtorAndArgsProvider(elementCopyCtorAndArgsProvider).
                    resolve();
        }
    }

    private static <T> AbstractStructuredArray<T> instantiate(
            final Class<T> elementClass,
            final long length,
            final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        @SuppressWarnings("unchecked")
        AbstractStructuredArrayBuilder<AbstractStructuredArray<T>, T> arrayBuilder =
                new AbstractStructuredArrayBuilder(AbstractStructuredArray.class, elementClass, length).
                        elementCtorAndArgsProvider(ctorAndArgsProvider).resolve();
        return instantiate(arrayBuilder);
    }

    private static <S extends AbstractStructuredArray<T>, T> S instantiate(
            final CtorAndArgs<S> arrayCtorAndArgs,
            final Class<T> elementClass,
            final long length,
            final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        @SuppressWarnings("unchecked")
        AbstractStructuredArrayBuilder<S, T> arrayBuilder =
                new AbstractStructuredArrayBuilder(arrayCtorAndArgs.getConstructor().getDeclaringClass(), elementClass, length).
                        arrayCtorAndArgs(arrayCtorAndArgs).
                        elementCtorAndArgsProvider(ctorAndArgsProvider).
                        resolve();
        return instantiate(arrayBuilder);
    }

    private static <S extends AbstractStructuredArray<T>, T> S instantiate(
            final AbstractStructuredArrayBuilder<S, T> arrayBuilder) {
        ConstructionContext<T> context = new ConstructionContext<>(arrayBuilder.getContextCookie());
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(arrayBuilder, context);
        try {
            arrayBuilder.resolve();
            constructorMagic.setActive(true);
            StructuredArrayModel<S, T> arrayModel = arrayBuilder.getArrayModel();
            Constructor<S> constructor = arrayBuilder.getArrayCtorAndArgs().getConstructor();
            Object[] args = arrayBuilder.getArrayCtorAndArgs().getArgs();
            return AbstractStructuredArrayBase.instantiateStructuredArray(arrayModel, constructor, args);
        } finally {
            constructorMagic.setActive(false);
        }
    }

    protected AbstractStructuredArray() {
        checkConstructorMagic();
        // Extract locally needed args from constructor magic:
        ConstructorMagic constructorMagic = getConstructorMagic();
        @SuppressWarnings("unchecked")
        final ConstructionContext<T> context = constructorMagic.getContext();
        @SuppressWarnings("unchecked")
        final AbstractStructuredArrayBuilder<AbstractStructuredArray<T>, T> arrayBuilder = constructorMagic.getArrayBuilder();
        final CtorAndArgsProvider<T> ctorAndArgsProvider = arrayBuilder.getElementCtorAndArgsProvider();

        // Finish consuming constructMagic arguments:
        constructorMagic.setActive(false);

        context.setArray(this);
        this.arrayModel = arrayBuilder.getArrayModel();

        final Field[] fields = removeStaticFields(getElementClass().getDeclaredFields());
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        this.fields = fields;
        this.hasFinalFields = containsFinalQualifiedFields(fields);

        AbstractStructuredArrayBuilder structuredSubArrayBuilder = arrayBuilder.getStructuredSubArrayBuilder();
        PrimitiveArrayBuilder primitiveSubArrayBuilder = arrayBuilder.getPrimitiveSubArrayBuilder();

        if (structuredSubArrayBuilder != null) {
            populateStructuredSubArrays(ctorAndArgsProvider, structuredSubArrayBuilder, context);
        } else if (primitiveSubArrayBuilder != null) {
            populatePrimitiveSubArrays(ctorAndArgsProvider, primitiveSubArrayBuilder, context);
        } else {
            // This is a single dimension array. Populate it:
            populateLeafElements(ctorAndArgsProvider, context);
        }
    }

    protected AbstractStructuredArray(AbstractStructuredArray<T> sourceArray) {
        // Support copy constructor. When we get here, everything is already set up for the regular
        // (default) construction path to perform the required copy.
        // Copying will actually be done according to the CtorAndArgsProvider and context already supplied,
        // with top-most source array being (already) passed as the contextCookie in the builder's
        // construction context, and copying proceeding using the context indices and the supplied
        // contextCookie provided by each CtorAndArgsProvider to figure out individual sources.
        this();
    }

    /**
     * Get the length (number of elements) of the array.
     *
     * @return the number of elements in the array.
     */
    long getLength() {
        return super.getLength();
    }


    /**
     * Get the {@link Class} of elements stored in the array.
     *
     * @return the {@link Class} of elements stored in the array.
     */
    Class<T> getElementClass() {
        return super.getElementClass();
    }

    /**
     * Get the array model
     * @return a model of this array
     */
    StructuredArrayModel<? extends AbstractStructuredArray<T>, T> getArrayModel() {
        return arrayModel;
    }

    /**
     * Get a reference to an element in a single dimensional array, using a <code>long</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    T get(final long index) throws IllegalArgumentException {
        return super.get(index);
    }

    /**
     * Get a reference to an element in a single dimensional array, using an <code>int</code> index.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    T get(final int index) throws IllegalArgumentException {
        return super.get(index);
    }

    //
    //
    // Populating array elements:
    //
    //

    private void populateLeafElement(final long index,
                                     CtorAndArgs<T> ctorAndArgs) {
        // Instantiate:
        constructElementAtIndex(index, ctorAndArgs.getConstructor(), ctorAndArgs.getArgs());
    }

    private void populatePrimitiveSubArray(final long index,
                                           PrimitiveArrayBuilder subArrayBuilder,
                                           final CtorAndArgs<T> subArrayCtorAndArgs) {
        // Instantiate:
        constructPrimitiveSubArrayAtIndex(
                index,
                subArrayBuilder.getArrayModel(),
                subArrayCtorAndArgs.getConstructor(),
                subArrayCtorAndArgs.getArgs());
    }

    private void populateStructuredSubArray(final ConstructionContext<T> context,
                                            AbstractStructuredArrayBuilder subArrayBuilder,
                                            final CtorAndArgs<T> subArrayCtorAndArgs) {
        ConstructionContext<T> subArrayContext = new ConstructionContext<>(subArrayCtorAndArgs.getContextCookie());
        subArrayContext.setContainingContext(context);
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(subArrayBuilder, subArrayContext);
        try {
            constructorMagic.setActive(true);
            // Instantiate:
            constructSubArrayAtIndex(
                    context.getIndex(),
                    subArrayBuilder.getArrayModel(),
                    subArrayCtorAndArgs.getConstructor(),
                    subArrayCtorAndArgs.getArgs());
        } finally {
            constructorMagic.setActive(false);
        }
    }

    private void populateLeafElements(final CtorAndArgsProvider<T> ctorAndArgsProvider,
                                      final ConstructionContext<T> context) {
        long length = getLength();

        try {
            for (long index = 0; index < length; index++) {
                final CtorAndArgs<T> ctorAndArgs;

                context.setIndex(index);
                ctorAndArgs = ctorAndArgsProvider.getForContext(context);

                if (ctorAndArgs.getConstructor().getDeclaringClass() != getElementClass()) {
                    throw new IllegalArgumentException("ElementClass (" + getElementClass() +
                            ") does not match ctorAndArgs.getConstructor().getDeclaringClass() (" +
                            ctorAndArgs.getConstructor().getDeclaringClass() + ")");
                }

                populateLeafElement(index, ctorAndArgs);
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void populatePrimitiveSubArrays(final CtorAndArgsProvider<T> subArrayCtorAndArgsProvider,
                                             final PrimitiveArrayBuilder subArrayBuilder,
                                             final ConstructionContext<T> context) {
        long length = getLength();

        try {
            for (long index = 0; index < length; index++) {
                final CtorAndArgs<T> ctorAndArgs;

                context.setIndex(index);
                ctorAndArgs = subArrayCtorAndArgsProvider.getForContext(context);

                if (ctorAndArgs.getConstructor().getDeclaringClass() != getElementClass()) {
                    throw new IllegalArgumentException("ElementClass (" + getElementClass() +
                            ") does not match ctorAndArgs.getConstructor().getDeclaringClass() (" +
                            ctorAndArgs.getConstructor().getDeclaringClass() + ")");
                }

                populatePrimitiveSubArray(index, subArrayBuilder, ctorAndArgs);
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void populateStructuredSubArrays(final CtorAndArgsProvider<T> subArrayCtorAndArgsProvider,
                                   final AbstractStructuredArrayBuilder subArrayBuilder,
                                   final ConstructionContext<T> context) {
        long length = getLength();

        try {
            for (long index = 0; index < length; index++) {
                final CtorAndArgs<T> ctorAndArgs;

                context.setIndex(index);
                ctorAndArgs = subArrayCtorAndArgsProvider.getForContext(context);

                if (ctorAndArgs.getConstructor().getDeclaringClass() != getElementClass()) {
                    throw new IllegalArgumentException("ElementClass (" + getElementClass() +
                            ") does not match ctorAndArgs.getConstructor().getDeclaringClass() (" +
                            ctorAndArgs.getConstructor().getDeclaringClass() + ")");
                }

                populateStructuredSubArray(context, subArrayBuilder, ctorAndArgs);
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * create a fresh StructuredArray intended to occupy a a given intrinsic field in the containing object,
     * at the field described by the supplied intrinsicObjectModel, using the supplied constructor and arguments.
     */
    static <T, A extends AbstractStructuredArray<T>> A constructStructuredArrayWithin(
            final Object containingObject,
            final AbstractIntrinsicObjectModel<A> intrinsicObjectModel,
            AbstractStructuredArrayBuilder<A, T> arrayBuilder)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        ConstructionContext context = new ConstructionContext(arrayBuilder.getContextCookie());
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(arrayBuilder, context);
        try {
            constructorMagic.setActive(true);
            return AbstractStructuredArray.constructStructuredArrayWithin(
                    containingObject,
                    intrinsicObjectModel,
                    arrayBuilder.getArrayModel(),
                    arrayBuilder.getArrayCtorAndArgs().getConstructor(),
                    arrayBuilder.getArrayCtorAndArgs().getArgs());
        } finally {
            constructorMagic.setActive(false);
        }
    }

    //
    //
    // Shallow copy support:
    //
    //

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
    static <S extends AbstractStructuredArray<T>, T> void _shallowCopy(
            final S src,
            final long srcOffset,
            final S dst,
            final long dstOffset,
            final long count) {
        _shallowCopy(src, srcOffset, dst, dstOffset, count, false);
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
    static <S extends AbstractStructuredArray<T>, T> void _shallowCopy(
            final S src,
            final long srcOffset,
            final S dst,
            final long dstOffset,
            final long count,
            final boolean allowFinalFieldOverwrite) {
        if (src.getElementClass() != dst.getElementClass()) {
            String msg = String.format("Only objects of the same class can be copied: %s != %s",
                    src.getClass(), dst.getClass());
            throw new IllegalArgumentException(msg);
        }

        if ((AbstractStructuredArray.class.isAssignableFrom(src.getElementClass()) ||
                (AbstractStructuredArray.class.isAssignableFrom(dst.getElementClass())))) {
            throw new IllegalArgumentException("shallowCopy only supported for single dimension arrays (with no nested StructuredArrays)");
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

    private static class ConstructorMagic {
        private boolean isActive() {
            return active;
        }

        private void setActive(boolean active) {
            this.active = active;
        }

        private void setConstructionArgs(final AbstractStructuredArrayBuilder arrayBuilder, final ConstructionContext context) {
            this.arrayBuilder = arrayBuilder;
            this.context = context;
        }

        private AbstractStructuredArrayBuilder getArrayBuilder() {
            return arrayBuilder;
        }

        private ConstructionContext getContext() {
            return context;
        }

        private boolean active = false;

        private AbstractStructuredArrayBuilder arrayBuilder;
        private ConstructionContext context;
    }

    private static final ThreadLocal<ConstructorMagic> threadLocalConstructorMagic =
            new ThreadLocal<ConstructorMagic>() {
                @Override protected ConstructorMagic initialValue() {
                    return new ConstructorMagic();
                }
            };

    private static ConstructorMagic getConstructorMagic() {
        return threadLocalConstructorMagic.get();
    }

    private static void checkConstructorMagic() {
        if (!getConstructorMagic().isActive()) {
            throw new IllegalArgumentException(
                    "StructuredArray<> must not be directly instantiated with a constructor." +
                            " Use newInstance(...) or a builder instead.");
        }
    }
}
