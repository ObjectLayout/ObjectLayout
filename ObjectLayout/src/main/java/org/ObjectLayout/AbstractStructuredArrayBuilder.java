package org.ObjectLayout;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

/**
 * A builder used for instantiating a {@link StructuredArray}&lt;T&gt;
 * <p>
 * {@link AbstractStructuredArrayBuilder} follows the commonly used builder pattern, and is useful for
 * capturing the instantiation parameters of {@link StructuredArray}s.
 * </p>
 * <p>
 * {@link AbstractStructuredArrayBuilder}s can be created for "flat" and "nested" {@link StructuredArray}
 * constructs, and can range from the simplest forms used when default constructors are employed, to forms that
 * supply customized per-element construction arguments that can take construction context (such as index,
 * containing array, and arbitrary data passed in a contextCookie) into account.
 * </p>
 * A simple example of using a {@link AbstractStructuredArrayBuilder} to instantiate a StructuredArray is:
 * <blockquote><pre>
 * StructuredArray&lt;MyElementClass&gt; array =
 *      new StructuredArrayBuilder(StructuredArray.class, MyElementClass.class, length).
 *          build();
 * </pre></blockquote>
 * <p>
 * An example of passing specific (but identical) construction arguments to element constructors is:
 * <blockquote><pre>
 * Constructor&lt;MyElementClass&gt; constructor = MyElementClass.class.getConstructor(int.Class, int.Class);
 *
 * StructuredArray&lt;MyElementClass&gt; array =
 *      new StructuredArrayBuilder(
 *          StructuredArray.class,
 *          MyElementClass.class,
 *          length).
 *          elementCtorAndArgs(constructor, initArg1, initArg2).
 *          build();
 * </pre></blockquote>
 * Some examples of providing per-element construction parameters that depend on construction context include:
 * <blockquote><pre>
 * Constructor&lt;MyElementClass&gt; constructor = MyElementClass.class.getConstructor(long.Class, long.Class);
 *
 * // Using a pre-constructed elementCtorAndArgsProvider:
 * StructuredArray&lt;MyElementClass&gt; array =
 *      new StructuredArrayBuilder(
 *          StructuredArray.class,
 *          MyElementClass.class,
 *          length).
 *          elementCtorAndArgsProvider(myCtorAndArgsProvider).
 *          build();
 *
 * // Using a Lambda expression for elementCtorAndArgsProvider:
 * StructuredArray&lt;MyElementClass&gt; array2 =
 *      new StructuredArrayBuilder(
 *          StructuredArray.class,
 *          MyElementClass.class,
 *          length).
 *          elementCtorAndArgsProvider(
 *              context -&gt; new CtorAndArgs&lt;MyElementClass&gt;(
 *                  constructor, context.getIndex(), context.getIndex() * 2)
 *          ).
 *          build();
 *
 * // Using an anonymous class for elementCtorAndArgsProvider:
 * StructuredArray&lt;MyElementClass&gt; array3 =
 *      new StructuredArrayBuilder(
 *          StructuredArray.class,
 *          MyElementClass.class,
 *          length).
 *          elementCtorAndArgsProvider(
 *              new CtorAndArgsProvider() {
 *                  {@literal @}Override
 *                  public CtorAndArgs getForContext(ConstructionContext context) throws NoSuchMethodException {
 *                      return new CtorAndArgs(constructor, context.getIndex(), context.getIndex() * 2);
 *                  }
 *              }
 *          ).
 *          build();
 * </pre></blockquote>
 *
 * @param <S> The class of the StructuredArray that is to be instantiated by the builder
 * @param <T> The class of the elements in the StructuredArray that is to be instantiated the builder
 */
class AbstractStructuredArrayBuilder<S extends AbstractStructuredArray<T>, T> {
    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    private static final MethodHandles.Lookup noLookup = null;

    private MethodHandles.Lookup lookup = null;

    private CtorAndArgs<S> arrayCtorAndArgs;
    private final StructuredArrayModel<S, T> arrayModel;

    private final AbstractStructuredArrayBuilder structuredSubArrayBuilder;
    private final PrimitiveArrayBuilder primitiveSubArrayBuilder;

    private CtorAndArgsProvider<T> elementCtorAndArgsProvider;
    private Object contextCookie;

    /**
     * Constructs a new {@link AbstractStructuredArrayBuilder} object for creating arrays of type S with
     * elements of type T, and the given length.
     *
     * Note: This constructor form cannot be used with an element type T that extends StructuredArray. Use the
     * constructor form {@link AbstractStructuredArrayBuilder#AbstractStructuredArrayBuilder(Class, AbstractStructuredArrayBuilder, long)}
     * to create builders that instantiate nested StructuredArrays.
     *
     * @param arrayClass The class of the array to be built by this builder
     * @param elementClass The class of elements in the array to be built by this builder
     * @param length The length of the array to be build by this builder
     */
    public AbstractStructuredArrayBuilder(final Class<S> arrayClass,
                                          final Class<T> elementClass,
                                          final long length) {
        this(noLookup, arrayClass, elementClass, length);
    }

    /**
     * Constructs a new {@link AbstractStructuredArrayBuilder} object for creating arrays of type S with
     * elements of type T, and the given length.
     *
     * Note: This constructor form cannot be used with an element type T that extends StructuredArray. Use the
     * constructor form {@link AbstractStructuredArrayBuilder#AbstractStructuredArrayBuilder(Class, AbstractStructuredArrayBuilder, long)}
     * to create builders that instantiate nested StructuredArrays.
     *
     * @param lookup The lookup object to use for accessing constructors when resolving this builder
     * @param arrayClass The class of the array to be built by this builder
     * @param elementClass The class of elements in the array to be built by this builder
     * @param length The length of the array to be build by this builder
     */
    public AbstractStructuredArrayBuilder(MethodHandles.Lookup lookup,
                                          final Class<S> arrayClass,
                                          final Class<T> elementClass,
                                          final long length) {
        if (AbstractStructuredArray.class.isAssignableFrom(elementClass) ||
                AbstractPrimitiveArray.class.isAssignableFrom(elementClass)) {
            throw new IllegalArgumentException("Cannot use this constructor form for nested " +
                    "StructuredArrays or PrimitiveArrays. " +
                    "Use the StructuredArrayBuilder(arrayClass, subArrayBuilder, length) form instead.");
        }
        this.lookup = lookup;
        this.arrayModel = new StructuredArrayModel<S, T>(arrayClass, elementClass, length){};
        this.structuredSubArrayBuilder = null;
        this.primitiveSubArrayBuilder = null;
    }

    /**
     * Constructs a new {@link AbstractStructuredArrayBuilder} object for creating arrays of the given array model.
     *
     * @param arrayModel The model of the array to be built by this builder
     */
    public AbstractStructuredArrayBuilder(final StructuredArrayModel<S, T> arrayModel) {
        this(noLookup, arrayModel);
    }

    /**
     * Constructs a new {@link AbstractStructuredArrayBuilder} object for creating arrays of the given array model.
     *
     * @param lookup The lookup object to use for accessing constructors when resolving this builder
     * @param arrayModel The model of the array to be built by this builder
     */
    public AbstractStructuredArrayBuilder(MethodHandles.Lookup lookup,
                                          final StructuredArrayModel<S, T> arrayModel) {
        this.lookup = lookup;
        this.arrayModel = arrayModel;
        this.structuredSubArrayBuilder = null;
        this.primitiveSubArrayBuilder = null;
    }

    /**
     * Constructs a new {@link AbstractStructuredArrayBuilder} object for creating an array of type S with
     * elements of type T, and the given length. Used when T extends StructuredArray, such that the
     * arrays instantiated by this builder would include nested StructuredArrays.
     *
     * @param arrayClass The class of the array to be built by this builder
     * @param subArrayBuilder The builder used for creating individual array elements (which are themselves
     *                        StructuredArrays)
     * @param length The length of the array to be build by this builder
     * @param <A> The class or the subArray (should match T for the StructuredArrayBuilder)
     * @param <E> The element class in the subArray.
     */
    public <A extends AbstractStructuredArray<E>, E> AbstractStructuredArrayBuilder(final Class<S> arrayClass,
                                                                                    final AbstractStructuredArrayBuilder<A,
                                                                                                                                                                            E> subArrayBuilder,
                                                                                    final long length) {
        this(noLookup, arrayClass, subArrayBuilder, length);
    }

    /**
     * Constructs a new {@link AbstractStructuredArrayBuilder} object for creating an array of type S with
     * elements of type T, and the given length. Used when T extends StructuredArray, such that the
     * arrays instantiated by this builder would include nested StructuredArrays.
     *
     * @param lookup The lookup object to use for accessing constructors when resolving this builder
     * @param arrayClass The class of the array to be built by this builder
     * @param subArrayBuilder The builder used for creating individual array elements (which are themselves
     *                        StructuredArrays)
     * @param length The length of the array to be build by this builder
     * @param <A> The class or the subArray (should match T for the StructuredArrayBuilder)
     * @param <E> The element class in the subArray.
     */
    public <A extends AbstractStructuredArray<E>, E> AbstractStructuredArrayBuilder(
            MethodHandles.Lookup lookup,
            final Class<S> arrayClass,
            final AbstractStructuredArrayBuilder<A, E> subArrayBuilder,
            final long length) {
        this.lookup = lookup;
        this.arrayModel =
                new StructuredArrayModel<>(
                        arrayClass,
                        subArrayBuilder.getArrayModel(),
                        length
                );
        this.structuredSubArrayBuilder = subArrayBuilder;
        this.primitiveSubArrayBuilder = null;
    }

    /**
     * Constructs a new {@link AbstractStructuredArrayBuilder} object for creating an array of type S with
     * elements of type T, and the given length. Used when T extends AbstractPrimitiveArray, such that the
     * arrays instantiated by this builder would include elements that are PrimitiveArrays.
     *
     * @param arrayClass The class of the array to be built by this builder
     * @param subArrayBuilder The builder used for creating individual array elements (which are themselves
     *                        subclassable PrimitiveArrays)
     * @param length The length of the array to be build by this builder
     * @param <A> The class in the subArray (should match T for the StructuredArrayBuilder)
     */
    public <A extends AbstractPrimitiveArray> AbstractStructuredArrayBuilder(final Class<S> arrayClass,
                                                                             final PrimitiveArrayBuilder<A>
                                                                                     subArrayBuilder,
                                                                             final long length) {
        this(noLookup, arrayClass, subArrayBuilder, length);
    }

    /**
     * Constructs a new {@link AbstractStructuredArrayBuilder} object for creating an array of type S with
     * elements of type T, and the given length. Used when T extends AbstractPrimitiveArray, such that the
     * arrays instantiated by this builder would include elements that are PrimitiveArrays.
     *
     * @param lookup The lookup object to use for accessing constructors when resolving this builder
     * @param arrayClass The class of the array to be built by this builder
     * @param subArrayBuilder The builder used for creating individual array elements (which are themselves
     *                        subclassable PrimitiveArrays)
     * @param length The length of the array to be build by this builder
     * @param <A> The class in the subArray (should match T for the StructuredArrayBuilder)
     */
    public <A extends AbstractPrimitiveArray> AbstractStructuredArrayBuilder(
            MethodHandles.Lookup lookup,
            final Class<S> arrayClass,
            final PrimitiveArrayBuilder<A> subArrayBuilder,
            final long length) {
        this.lookup = lookup;
        this.arrayModel =
                new StructuredArrayModel<>(
                        arrayClass,
                        subArrayBuilder.getArrayModel(),
                        length
                );
        this.structuredSubArrayBuilder = null;
        this.primitiveSubArrayBuilder = subArrayBuilder;
    }

    /**
     * Set the {@link CtorAndArgsProvider} used for determining the constructor and construction
     * arguments used for each element in the array. The provider may use the specific element's
     * construction context in determining the construction parameters.
     *
     * Note: This method overlaps in purpose with the alternate methods for controlling element
     * construction. Namely {@link AbstractStructuredArrayBuilder#elementCtorAndArgs(CtorAndArgs)} and
     * {@link AbstractStructuredArrayBuilder#elementCtorAndArgs(Constructor, Object...)}.
     *
     * @param ctorAndArgsProvider The provider used for determining the constructor and construction
     *                            arguments used for each element in instantiated arrays
     * @return The builder
     */
    public AbstractStructuredArrayBuilder<S, T> elementCtorAndArgsProvider(final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        this.elementCtorAndArgsProvider = ctorAndArgsProvider;
        return this;
    }

    /**
     * Set the {@link CtorAndArgs} to be used for all elements in the array. All elements will be constructed with
     * the same constructor and the same arguments.
     *
     * Note: This method overlaps in purpose with the alternate methods for controlling element
     * construction. Namely {@link AbstractStructuredArrayBuilder#elementCtorAndArgsProvider(CtorAndArgsProvider)} and
     * {@link AbstractStructuredArrayBuilder#elementCtorAndArgs(Constructor, Object...)}.
     *
     * @param ctorAndArgs The constructor and arguments used for all elements in arrays
     * @return The builder
     */
    public AbstractStructuredArrayBuilder<S, T> elementCtorAndArgs(final CtorAndArgs<T> ctorAndArgs) {
        if ((structuredSubArrayBuilder != null) || (primitiveSubArrayBuilder != null)) {
            throw new IllegalArgumentException(
                    "ctoAndArgs for constructing subArray elements should be supplied in subArrayBuilder");
        }
        return elementCtorAndArgsProvider(
                new CtorAndArgsProvider<T>() {
                    @Override
                    public CtorAndArgs<T> getForContext(ConstructionContext<T> context) throws NoSuchMethodException {
                        return ctorAndArgs;
                    }
                }
        );
    }

    /**
     * Set the {@link Constructor} and construction arguments to be used for all elements in the array.
     * All elements will be constructed with the same constructor and the same arguments.
     *
     * Note: This method overlaps in purpose with the alternate methods for controlling element
     * construction. Namely {@link AbstractStructuredArrayBuilder#elementCtorAndArgsProvider(CtorAndArgsProvider)} and
     * {@link AbstractStructuredArrayBuilder#elementCtorAndArgs(CtorAndArgs)}.
     *
     * @param constructor The constructor used for all elements in arrays
     * @param args The construction arguments supplied for the constructor, used for all elements in the array
     * @return The builder
     */
    public AbstractStructuredArrayBuilder<S, T> elementCtorAndArgs(final Constructor<T> constructor, final Object... args) {
        return elementCtorAndArgs(new CtorAndArgs<>(constructor, args));
    }

    /**
     * Set the {@link CtorAndArgs} to be used in constructing arrays.
     * Setting the means for array construction is Required if the array class (S) does not support a
     * default constructor, or if a non-default construction of the array instance is needed.
     *
     * @param arrayCtorAndArgs The constructor and arguments used for constructing arrays
     * @return The builder
     */
    public AbstractStructuredArrayBuilder<S, T> arrayCtorAndArgs(final CtorAndArgs<S> arrayCtorAndArgs) {
        this.arrayCtorAndArgs = arrayCtorAndArgs;
        return this;
    }

    /**
     * Set the {@link Constructor} and construction arguments to be used in constructing arrays.
     * Setting the means for array construction is Required if the array class (S) does not support a
     * default constructor, or if a non-default construction of the array instance is needed.
     *
     * @param constructor The constructor used for constructing arrays
     * @param args The construction arguments supplied for the constructor
     * @return The builder
     */
    public AbstractStructuredArrayBuilder<S, T> arrayCtorAndArgs(final Constructor<S> constructor, final Object... args) {
        this.arrayCtorAndArgs = new CtorAndArgs<>(constructor, args);
        return this;
    }

    /**
     * Set the (opaque) contextCookie object associated with this builder. This contextCookie object will be
     * set in {@link ConstructionContext} object passed to the element
     * {@link CtorAndArgsProvider} provider for each element in instantiated arrays.
     *
     * @param contextCookie the contextCookie object
     * @return The builder
     */
    public AbstractStructuredArrayBuilder<S, T> contextCookie(final Object contextCookie) {
        this.contextCookie = contextCookie;
        return this;
    }

    private void resolve(boolean resolveArrayCtorAndArgs) {
        if ((arrayCtorAndArgs == null) && resolveArrayCtorAndArgs) {
            this.arrayCtorAndArgs =
                    new CtorAndArgs<>(lookup, arrayModel.getArrayClass(), EMPTY_ARG_TYPES, EMPTY_ARGS);
        }

        if (elementCtorAndArgsProvider == null) {
            if ((structuredSubArrayBuilder != null) &&
                    (structuredSubArrayBuilder.arrayCtorAndArgs != null)) {
                // Use the CtorAndArgs provided for subArray elements:
                @SuppressWarnings("unchecked")
                CtorAndArgsProvider<T> subArrayCtorAndArgsProvider =
                        new CtorAndArgsProvider<T>() {
                            @Override
                            public CtorAndArgs<T> getForContext(
                                    ConstructionContext<T> context) throws NoSuchMethodException {
                                return structuredSubArrayBuilder.arrayCtorAndArgs;
                            }
                        };
                elementCtorAndArgsProvider = subArrayCtorAndArgsProvider;
            } else if ((primitiveSubArrayBuilder != null) &&
                    (primitiveSubArrayBuilder.getArrayCtorAndArgs() != null)) {
                // Use the CtorAndArgs provided for subArray elements:
                @SuppressWarnings("unchecked")
                CtorAndArgsProvider<T> subArrayCtorAndArgsProvider =
                        new CtorAndArgsProvider<T>() {
                            @Override
                            public CtorAndArgs<T> getForContext(
                                    ConstructionContext<T> context) throws NoSuchMethodException {
                                return primitiveSubArrayBuilder.getArrayCtorAndArgs();
                            }
                        };
                elementCtorAndArgsProvider = subArrayCtorAndArgsProvider;
            } else {
                // Use the default constructor:
                final CtorAndArgs<T> constantCtorAndArgs = new CtorAndArgs<>(lookup, arrayModel.getElementClass());
                elementCtorAndArgsProvider =
                        new CtorAndArgsProvider<T>() {
                            @Override
                            public CtorAndArgs<T> getForContext(
                                    ConstructionContext<T> context) throws NoSuchMethodException {
                                return constantCtorAndArgs;
                            }
                        };
            }
        }

        if (structuredSubArrayBuilder != null) {
            // recurse through subArray builders and resolve them too:
            structuredSubArrayBuilder.resolve(false);
        }
    }

    /**
     * Resolve any not-yet-resolved constructor information needed by this builder. Calling resolve() is not
     * necessary ahead of building, but it is useful for ensuring resolution works ahead of actual building
     * attempts.
     *
     * @return This builder
     * @throws IllegalArgumentException if the array constructor or element constructor fail to resolve given
     * the current information in the builder
     */
    public AbstractStructuredArrayBuilder<S, T> resolve() {
        resolve(true);
        return this;
    }

    /**
     * Build a {@link StructuredArray} according to the information captured in this builder
     * @return A newly instantiated {@link StructuredArray}
     *
     * @throws IllegalArgumentException if the array constructor or element constructor fail to resolve given
     * the current information in the builder
     */
    public S build() throws IllegalArgumentException {
        resolve();
        return AbstractStructuredArray._newInstance(this);
    }

    /**
     * Get the {@link StructuredArrayModel} that describes the arrays built by this builder
     *
     * @return The {@link StructuredArrayModel} that describes the arrays built by this builder
     */
    public StructuredArrayModel<S, T> getArrayModel() {
        return arrayModel;
    }

    /**
     * Get the {@link AbstractStructuredArrayBuilder} for the elements of the arrays built by
     * this builder. Null
     *
     * @return The {@link AbstractStructuredArrayBuilder} for the elements of the arrays built by
     * this builder. Null if the array element type T does not extend {@link StructuredArray}.
     */
    public AbstractStructuredArrayBuilder getStructuredSubArrayBuilder() {
        return structuredSubArrayBuilder;
    }

    /**
     * Get the {@link AbstractStructuredArrayBuilder} for the elements of the arrays built by
     * this builder. Null
     *
     * @return The {@link AbstractStructuredArrayBuilder} for the elements of the arrays built by
     * this builder. Null if the array element type T does not extend {@link StructuredArray}.
     */
    public PrimitiveArrayBuilder getPrimitiveSubArrayBuilder() {
        return primitiveSubArrayBuilder;
    }

    /**
     * Get the {@link CtorAndArgs} describing the constructor and arguments used to instantiate arrays with
     * this builder. May be null if non of {@link AbstractStructuredArrayBuilder#arrayCtorAndArgs},
     * {@link AbstractStructuredArrayBuilder#resolve()} or
     * {@link AbstractStructuredArrayBuilder#build()} have been called yet.
     * @return The {@link CtorAndArgs} describing the constructor and arguments used to instantiate arrays with
     * this builder.
     */
    public CtorAndArgs<S> getArrayCtorAndArgs() {
        return arrayCtorAndArgs;
    }

    /**
     * Get the {@link CtorAndArgsProvider} which provides the constructor and arguments used to construct
     * individual elements of arrays instantiated with this builder. May be null if non of
     * {@link AbstractStructuredArrayBuilder#elementCtorAndArgsProvider},
     * {@link AbstractStructuredArrayBuilder#elementCtorAndArgs},
     * {@link AbstractStructuredArrayBuilder#resolve} or
     * {@link AbstractStructuredArrayBuilder#build} have been called yet.
     * @return The {@link CtorAndArgsProvider} which provides the constructor and arguments used to construct
     * individual elements of arrays instantiated with this builder
     */
    public CtorAndArgsProvider<T> getElementCtorAndArgsProvider() {
        return elementCtorAndArgsProvider;
    }

    /**
     * Get the (opaque) contextCookie object associated with this builder. This contextCookie object will be
     * set in {@link ConstructionContext} object passed to the element
     * {@link CtorAndArgsProvider} provider for each element in instantiated arrays.
     * @return The (opaque) contextCookie object associated with this builder
     */
    public Object getContextCookie() {
        return contextCookie;
    }
}
