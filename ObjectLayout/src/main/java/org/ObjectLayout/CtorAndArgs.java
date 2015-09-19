/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

/**
 * Captures a specific constructor and arguments to be passed to it. Useful for providing
 * fully customizable, per-element construction behavior for iterative constructors (such as those
 * used for elements in {@link StructuredArray})
 *
 * @param <T> type of the element occupying each array slot.
 */
public class CtorAndArgs<T> {
    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];
    private static final MethodHandles.Lookup noLookup = null;

    private Constructor<T> constructor;
    private Object[] args;
    private Object contextCookie;

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
     * @param instanceClass The class which the constructor in this {@link CtorAndArgs} will instantiate
     * @param constructorArgTypes The argument types expected by the constructor
     * @param args constructor arguments to be indicated in this {@link CtorAndArgs}
     */
    public CtorAndArgs(
            final Class<T> instanceClass,
            final Class[] constructorArgTypes,
            final Object... args) {
        this(noLookup, instanceClass, constructorArgTypes, args);
    }

    /**
     * Create a {@link CtorAndArgs} instance for the given instanceClass, with a constructor
     * identified by the constructorArgTypes, and using the args provides. The presumption is
     * that types in args match those expected by constructor. Obviously exceptions may be
     * generated at construction time if this is not the case.
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param instanceClass The class which the constructor in this {@link CtorAndArgs} will instantiate
     * @param constructorArgTypes The argument types expected by the constructor
     * @param args constructor arguments to be indicated in this {@link CtorAndArgs}
     */
    public CtorAndArgs(
            MethodHandles.Lookup lookup,
            final Class<T> instanceClass,
            final Class[] constructorArgTypes,
            final Object... args) {
        try {
            if (constructorArgTypes.length != args.length) {
                throw new IllegalArgumentException("argument types and values must be the same length");
            }
            @SuppressWarnings("unchecked")
            Constructor<T> ctor = instanceClass.getDeclaredConstructor(constructorArgTypes);
            if (lookup != null) {
                if (!belongsToThisPackage(ctor.getDeclaringClass())) {
                    lookup.unreflectConstructor(ctor); // May throw IllegalAccessException.
                    // The fact the the provided lookup Proves we may setAccessible.
                    ctor.setAccessible(true);
                }
            }
            setConstructor(ctor);
            setArgs(args);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a {@link CtorAndArgs} instance for the given instanceClass, using the default constructor (and no args)
     *
     * @param instanceClass The class which the constructor in this {@link CtorAndArgs} will instantiate
     */
    public CtorAndArgs(final Class<T> instanceClass) {
        this(instanceClass, EMPTY_ARG_TYPES, EMPTY_ARGS);
    }

    /**
     * Create a {@link CtorAndArgs} instance for the given instanceClass, using the default constructor (and no args)
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param instanceClass The class which the constructor in this {@link CtorAndArgs} will instantiate
     */
    public CtorAndArgs(
            MethodHandles.Lookup lookup,
            final Class<T> instanceClass) {
        this(lookup, instanceClass, EMPTY_ARG_TYPES, EMPTY_ARGS);
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
     * @return the {@link CtorAndArgs} instance operated on
     */
    public final CtorAndArgs<T> setConstructor(final Constructor<T> constructor) {
        if (null == constructor) {
            throw new NullPointerException("constructor cannot be null");
        }
        this.constructor = constructor;
        return this;
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
     * @return the {@link CtorAndArgs} instance operated on
     */
    public final CtorAndArgs<T> setArgs(final Object... args) {
        this.args = args;
        return this;
    }

    /**
     * Get the value of the opaque contextCookie object
     * @return opaque contextCookie object (may be null)
     */
    public Object getContextCookie() {
        return contextCookie;
    }

    /**
     * Set the value of the opaque contextCookie object
     * @param contextCookie carries an opaque object
     */
    public void setContextCookie(final Object contextCookie) {
        this.contextCookie = contextCookie;
    }


    private static final ClassLoader thisClassLoader =
            CtorAndArgs.class.getClassLoader();
    private static final int indexOfLastPeriodInThisPackageName =
            CtorAndArgs.class.getName().lastIndexOf('.');
    private static final String thisPackageName =
            CtorAndArgs.class.getName().substring(0, indexOfLastPeriodInThisPackageName);

    static boolean belongsToThisPackage(Class<?> cls) {
        if (cls.getClassLoader() != thisClassLoader) {
            return false;
        }
        if (cls.isArray()) {
            return false; // Arrays are not in this class.
        }
        String className = cls.getName();
        // See if length of package name differs:
        if (className.lastIndexOf('.') != indexOfLastPeriodInThisPackageName) {
            return false;
        }
        return className.startsWith(thisPackageName);
    }
}
