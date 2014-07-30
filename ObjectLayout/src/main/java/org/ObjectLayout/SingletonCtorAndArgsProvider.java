/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;

/**
 * Supports a single (cached) constructor and set of arguments for either default construction or construction
 * with a given fixed set of arguments (repeated for all indices)
 *
 * @param <T> type of the element occupying each array slot
 */
public class SingletonCtorAndArgsProvider<T> extends CtorAndArgsProvider<T> {

    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final CtorAndArgs<T> ctorAndArgs;

    /**
     * Used to apply default constructor to all elements.
     *
     * @param elementClass The element class
     * @throws NoSuchMethodException if no default constructor is found for elementClass
     */
    public SingletonCtorAndArgsProvider(final Class<T> elementClass) throws NoSuchMethodException {
        this(elementClass, EMPTY_ARG_TYPES, EMPTY_ARGS);
    }

    /**
     * Used to apply a fixed constructor with a given set of arguments to all elements.
     *
     * @param elementClass The element class
     * @param constructorArgTypes The argument types for the element constructor
     * @param args The arguments to be passed to the constructor for all elements
     * @throws NoSuchMethodException if a constructor matching constructorArgTypes
     * @throws IllegalArgumentException if constructorArgTypes and args conflict
     */
    public SingletonCtorAndArgsProvider(final Class<T> elementClass,
                                        final Class[] constructorArgTypes,
                                        final Object... args) throws NoSuchMethodException {
        super(elementClass);
        ctorAndArgs = new CtorAndArgs<T>(elementClass, constructorArgTypes, args);
    }

    /**
     * Used to apply a fixed constructor with a given set of arguments to all elements.
     *
     * @param constructor The element constructor
     * @param args The arguments to be passed to the constructor for all elements
     * @throws NoSuchMethodException if a constructor matching argTypes
     * @throws IllegalArgumentException if argTypes and args conflict
     */
    public SingletonCtorAndArgsProvider(final Constructor<T> constructor,
                                        final Object... args) throws NoSuchMethodException {
        super(constructor.getDeclaringClass());
        ctorAndArgs = new CtorAndArgs<T>(constructor, args);
    }

    /**
     * Set the constructor arguments to be indicated in this CtorAndArgsProvider. Enables recycling of
     * {@link CtorAndArgsProvider} objects to avoid re-allocation. E.g. in copy construction loops.
     *
     * @param args constructor arguments to be indicated in this {@link CtorAndArgsProvider}
     */
    public final void setArgs(final Object... args) {
        ctorAndArgs.setArgs(args);
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}.
     *
     * @param index of the element to be constructed in the target array
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @Override
    public CtorAndArgs<T> getForIndex(final long index) throws NoSuchMethodException {
        return ctorAndArgs;
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}.
     *
     * @param index of the element to be constructed in the target array (one per dimension)
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @Override
    public CtorAndArgs<T> getForIndex(final long... index) throws NoSuchMethodException {
        return ctorAndArgs;
    }
}
