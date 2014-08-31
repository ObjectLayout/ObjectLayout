package org.ObjectLayout;

import java.lang.reflect.Constructor;

/**
 * A model for instantiating a structured array.
 */
public class StructuredArrayBuilder<A extends StructuredArray<T>, T> {
    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final StructuredArrayModel<A, T> arrayModel;

    private final StructuredArrayBuilder subArrayBuilder;

    private CtorAndArgs<A> arrayCtorAndArgs;
    private CtorAndArgsProvider<T> elementCtorAndArgsProvider;
    private Object contextCookie;

    public StructuredArrayBuilder(final Class<A> arrayClass,
                                  final Class<T> elementClass,
                                  final long length) {
        this.arrayModel = new StructuredArrayModel<A, T>(arrayClass, elementClass, length);
        this.subArrayBuilder = null;
    }


    @SuppressWarnings("unchecked")
    public <A2 extends StructuredArray<T2>, T2>
    StructuredArrayBuilder(final Class<A> arrayClass,
                           final StructuredArrayBuilder<A2, T2> subArrayBuilder,
                           final long length) {
        this.arrayModel = new StructuredArrayModel<A, T>(arrayClass, subArrayBuilder.getArrayModel(), length);
        this.subArrayBuilder = subArrayBuilder;
    }

    public StructuredArrayBuilder<A, T> elementCtorAndArgsProvider(final CtorAndArgsProvider<T> ctorAndArgsProvider) {
        this.elementCtorAndArgsProvider = ctorAndArgsProvider;
        return this;
    }

    public StructuredArrayBuilder<A, T> elementCtorAndArgs(final CtorAndArgs<T> ctorAndArgs) {
        if (subArrayBuilder != null) {
            throw new IllegalArgumentException(
                    "ctoAndArgs for constructing subArray elements should be supplied in subArrayBuilder");
        }
        return elementCtorAndArgsProvider(
                new SingletonCtorAndArgsProvider<T>(ctorAndArgs.getConstructor(), ctorAndArgs.getArgs()));
    }

    public StructuredArrayBuilder<A, T> elementCtorAndArgs(final Constructor<T> constructor, final Object... args) {
        return elementCtorAndArgs(new CtorAndArgs<T>(constructor, args));
    }

    public StructuredArrayBuilder<A, T> arrayCtorAndArgs(final CtorAndArgs<A> arrayCtorAndArgs) {
        this.arrayCtorAndArgs = arrayCtorAndArgs;
        return this;
    }

    public StructuredArrayBuilder<A, T> arrayCtorAndArgs(final Constructor<A> constructor, final Object... args) {
        this.arrayCtorAndArgs = new CtorAndArgs<A>(constructor, args);
        return this;
    }

    public StructuredArrayBuilder<A, T> contextCookie(final Object contextCookie) {
        this.contextCookie = contextCookie;
        return this;
    }

    public StructuredArrayBuilder<A, T> resolve(boolean resolveArrayCtorAndArgs) throws NoSuchMethodException {
        if (arrayCtorAndArgs == null) {
            arrayCtorAndArgs = new CtorAndArgs<A>(arrayModel.getArrayClass(), EMPTY_ARG_TYPES, EMPTY_ARGS);
        }

        if (elementCtorAndArgsProvider == null) {
            if ((subArrayBuilder != null) && (subArrayBuilder.arrayCtorAndArgs != null)) {
                // Use the CtorAndArgs provided for subArray elements:
                @SuppressWarnings("unchecked")
                AbstractCtorAndArgsProvider<T> subArrayCtorAndArgsProvider =
                        new SingletonCtorAndArgsProvider<T>(
                                (Constructor<T>)subArrayBuilder.arrayCtorAndArgs.getConstructor(),
                                subArrayBuilder.arrayCtorAndArgs.getArgs());
                elementCtorAndArgsProvider = subArrayCtorAndArgsProvider;
            } else {
                // Use the default constructor:
                elementCtorAndArgsProvider = new SingletonCtorAndArgsProvider<T>(arrayModel.getElementClass());
            }
        }

        if (subArrayBuilder != null) {
            // recurse through subArray builders and resolve them too:
            subArrayBuilder.resolve(false);
        }

        return this;
    }

    public StructuredArrayBuilder<A, T> resolve() throws NoSuchMethodException {
        return resolve(true);
    }

    public A build() throws NoSuchMethodException {
        resolve();
        return StructuredArray.newInstance(this);
    }


    public StructuredArrayModel<A, T> getArrayModel() {
        return arrayModel;
    }

    public StructuredArrayBuilder getSubArrayBuilder() {
        return subArrayBuilder;
    }

    public CtorAndArgs<A> getArrayCtorAndArgs() {
        return arrayCtorAndArgs;
    }

    public CtorAndArgsProvider<T> getElementCtorAndArgsProvider() {
        return elementCtorAndArgsProvider;
    }

    public Object getContextCookie() {
        return contextCookie;
    }
}
