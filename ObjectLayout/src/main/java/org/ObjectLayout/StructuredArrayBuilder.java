package org.ObjectLayout;

import java.lang.reflect.Constructor;

/**
 * A builder used for instantiating a StructuredArray&ltT&gt.
 *
 * @param <S> The class of the StructuredArray that is to be instantiated by the builder
 * @param <T> The class of the elements in the StructuredArray that is to be instantiated the builder
 */
public class StructuredArrayBuilder<S extends StructuredArray<T>, T> {
    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final StructuredArrayModel<S, T> arrayModel;

    private final StructuredArrayBuilder subArrayBuilder;

    private CtorAndArgs<S> arrayCtorAndArgs;
    private CtorAndArgsProvider<T> elementCtorAndArgsProvider;
    private Object contextCookie;

    public StructuredArrayBuilder(final Class<S> arrayClass,
                                  final Class<T> elementClass,
                                  final long length) {
        this.arrayModel = new StructuredArrayModel<S, T>(arrayClass, elementClass, length);
        this.subArrayBuilder = null;
    }


    @SuppressWarnings("unchecked")
    public <A2 extends StructuredArray<T2>, T2>
    StructuredArrayBuilder(final Class<S> arrayClass,
                           final StructuredArrayBuilder<A2, T2> subArrayBuilder,
                           final long length) {
        this.arrayModel = new StructuredArrayModel<S, T>(arrayClass, subArrayBuilder.getArrayModel(), length);
        this.subArrayBuilder = subArrayBuilder;
    }

    public StructuredArrayBuilder<S, T> elementCtorAndArgsProvider(final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        this.elementCtorAndArgsProvider = ctorAndArgsProvider;
        return this;
    }

    public StructuredArrayBuilder<S, T> elementCtorAndArgs(final CtorAndArgs<T> ctorAndArgs) {
        if (subArrayBuilder != null) {
            throw new IllegalArgumentException(
                    "ctoAndArgs for constructing subArray elements should be supplied in subArrayBuilder");
        }
        return elementCtorAndArgsProvider(
                new ConstantCtorAndArgsProvider<T>(ctorAndArgs.getConstructor(), ctorAndArgs.getArgs()));
    }

    public StructuredArrayBuilder<S, T> elementCtorAndArgs(final Constructor<T> constructor, final Object... args) {
        return elementCtorAndArgs(new CtorAndArgs<T>(constructor, args));
    }

    public StructuredArrayBuilder<S, T> arrayCtorAndArgs(final CtorAndArgs<S> arrayCtorAndArgs) {
        this.arrayCtorAndArgs = arrayCtorAndArgs;
        return this;
    }

    public StructuredArrayBuilder<S, T> arrayCtorAndArgs(final Constructor<S> constructor, final Object... args) {
        this.arrayCtorAndArgs = new CtorAndArgs<S>(constructor, args);
        return this;
    }

    public StructuredArrayBuilder<S, T> contextCookie(final Object contextCookie) {
        this.contextCookie = contextCookie;
        return this;
    }

    public StructuredArrayBuilder<S, T> resolve(boolean resolveArrayCtorAndArgs) throws NoSuchMethodException {
        if (arrayCtorAndArgs == null) {
            arrayCtorAndArgs = new CtorAndArgs<S>(arrayModel.getArrayClass(), EMPTY_ARG_TYPES, EMPTY_ARGS);
        }

        if (elementCtorAndArgsProvider == null) {
            if ((subArrayBuilder != null) && (subArrayBuilder.arrayCtorAndArgs != null)) {
                // Use the CtorAndArgs provided for subArray elements:
                @SuppressWarnings("unchecked")
                AbstractCtorAndArgsProvider<T> subArrayCtorAndArgsProvider =
                        new ConstantCtorAndArgsProvider<T>(
                                (Constructor<T>)subArrayBuilder.arrayCtorAndArgs.getConstructor(),
                                subArrayBuilder.arrayCtorAndArgs.getArgs());
                elementCtorAndArgsProvider = subArrayCtorAndArgsProvider;
            } else {
                // Use the default constructor:
                elementCtorAndArgsProvider = new ConstantCtorAndArgsProvider<T>(arrayModel.getElementClass());
            }
        }

        if (subArrayBuilder != null) {
            // recurse through subArray builders and resolve them too:
            subArrayBuilder.resolve(false);
        }

        return this;
    }

    public StructuredArrayBuilder<S, T> resolve() throws NoSuchMethodException {
        return resolve(true);
    }

    public S build() throws NoSuchMethodException {
        resolve();
        return StructuredArray.newInstance(this);
    }


    public StructuredArrayModel<S, T> getArrayModel() {
        return arrayModel;
    }

    public StructuredArrayBuilder getSubArrayBuilder() {
        return subArrayBuilder;
    }

    public CtorAndArgs<S> getArrayCtorAndArgs() {
        return arrayCtorAndArgs;
    }

    public CtorAndArgsProvider<T> getElementCtorAndArgsProvider() {
        return elementCtorAndArgsProvider;
    }

    public Object getContextCookie() {
        return contextCookie;
    }
}
