/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;

/**
 * Captures a specific constructor and arguments to be passed to it. Useful for providing
 * fully customizable, per-element construction behavior for iterative constructors (such as those
 * used for elements in {@link StructuredArray})
 *
 * @param <T> type of the element occupying each array slot.
 */
public class CtorAndArgs<T> {

    private Constructor<T> constructor;
    private Object[] args;

    /**
     * Create a {@link CtorAndArgs} instance. The presumption is that types in args
     * match those expected by constructor. Obviously exceptions may be generated at construction time if
     * this is not the case.
     *
     * @param constructor Constructor to be indicated in this {@link CtorAndArgs}
     * @param args constructor arguments to be indicated in this {@link CtorAndArgs}
     */
    public CtorAndArgs(final Constructor<T> constructor, final Object... args) {
        setConstructor(constructor);
        setArgs(args);
    }

    /**
     * Create a {@link CtorAndArgs} instance for the given instanceClass, with a constructor
     * identified by the constructorArgTypes, and using the args provides. The presumption is
     * that types in args match those expected by constructor. Obviously exceptions may be
     * generated at construction time if this is not the case.
     *
     * @param instanceClass
     * @param constructorArgTypes
     * @param args
     * @throws NoSuchMethodException
     */
    public CtorAndArgs(Class<T> instanceClass, final Class[] constructorArgTypes, final Object... args)
            throws NoSuchMethodException {
        if (constructorArgTypes.length != args.length) {
            throw new IllegalArgumentException("argument types and values must be the same length");
        }
        setConstructor(instanceClass.getConstructor(constructorArgTypes));
        setArgs(args);
    }

    /**
     * @return the Constructor indicated in this CtorAndArgs
     */
    public final Constructor<T> getConstructor() {
        return constructor;
    }

    /**
     * Set the constructor to be indicated in this {@link CtorAndArgs}. Enables recycling of
     * {@link CtorAndArgs} objects to avoid re-allocation. E.g. in copy construction loops.
     *
     * @param constructor Constructor to be indicated in this CtorAndArgs
     */
    public final void setConstructor(final Constructor<T> constructor) {
        if (null == constructor) {
            throw new NullPointerException("constructor cannot be null");
        }
        this.constructor = constructor;
    }

    /**
     * @return the constructor arguments indicated in this CtorAndArgs
     */
    public final Object[] getArgs() {
        return args;
    }

    /**
     * Set the constructor arguments to be indicated in this CtorAndArgs. Enables recycling of
     * {@link CtorAndArgs} objects to avoid re-allocation. E.g. in copy construction loops.
     *
     * @param args constructor arguments to be indicated in this {@link CtorAndArgs}
     */
    public final void setArgs(final Object... args) {
        if (null == args) {
            throw new NullPointerException("args cannot be null");
        }
        this.args = args;
    }
}
