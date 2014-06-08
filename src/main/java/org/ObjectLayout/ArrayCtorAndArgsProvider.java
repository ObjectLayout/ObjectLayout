/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Supports the construction of a new array's individual elements using a copy constructor to copy a source
 * array's corresponding elements.
 *
 * @param <T> type of the element occupying each array slot
 */
public class ArrayCtorAndArgsProvider<T> extends CtorAndArgsProvider<T> {

    private final Constructor<T> constructor;
    private final Object[] originalArgs;
    private final int containingIndexesIndexInArgs;

    private final boolean keepInternalCachingThreadSafe;
    private CtorAndArgs<T> nonThreadSafeCachedCtorAndArgs = null;
    private Object[] nonThreadSafeCachedArgs = null;
    private long[] nonThreadSafeCachedContainingIndexes = null;
    private final AtomicReference<CtorAndArgs<T>> cachedConstructorAndArgs = new AtomicReference<CtorAndArgs<T>>();
    private final AtomicReference<Object[]> cachedArgs = new AtomicReference<Object[]>();
    private final AtomicReference<long[]> cachedContainingIndexes = new AtomicReference<long[]>();


    /**
     * Used to apply a fixed constructor with a given set of arguments to all elements.
     *
     * @param constructor The element constructor
     * @param args The arguments to be passed to the constructor for all elements
     * @throws NoSuchMethodException if a constructor matching argTypes
     * @throws IllegalArgumentException if argTypes and args conflict
     */
    public ArrayCtorAndArgsProvider(final Constructor<T> constructor,
                                    final Object[] args,
                                    final int containingIndexesIndexInArgs) throws NoSuchMethodException {
        this(constructor, args, containingIndexesIndexInArgs, true);
    }

    /**
     * Used to apply a fixed constructor with a given set of arguments to all elements.
     *
     * @param constructor The element constructor
     * @param args The arguments to be passed to the constructor for all elements
     * @param keepInternalCachingThreadSafe Control whether or not internal caching is kept thread-safe
     * @throws NoSuchMethodException if a constructor matching argTypes
     * @throws IllegalArgumentException if argTypes and args conflict
     */
    public ArrayCtorAndArgsProvider(final Constructor<T> constructor,
                                    final Object[] args,
                                    final int containingIndexesIndexInArgs,
                                    final boolean keepInternalCachingThreadSafe) throws NoSuchMethodException {
        super(constructor.getDeclaringClass());
        this.constructor = constructor;
        this.originalArgs = args;
        this.containingIndexesIndexInArgs = containingIndexesIndexInArgs;
        this.keepInternalCachingThreadSafe = keepInternalCachingThreadSafe;
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}.                           .
     *
     * @param index The index of the element to be constructed in the target array
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @SuppressWarnings("unchecked")
    public CtorAndArgs<T> getForIndex(final long index) throws NoSuchMethodException {
        throw new IllegalArgumentException("getForIndex not supported");
    }


    public CtorAndArgs<T> getForIndices(final long[] indices) throws NoSuchMethodException {
        CtorAndArgs<T> ctorAndArgs;
        Object[] args;
        long[] containingIndexes;

        // Try (but not too hard) to use a cached, previously allocated ctorAndArgs object:
        if (keepInternalCachingThreadSafe) {
            ctorAndArgs = cachedConstructorAndArgs.getAndSet(null);
            args = cachedArgs.getAndSet(null);
            containingIndexes = cachedContainingIndexes.getAndSet(null);
        } else {
            ctorAndArgs = nonThreadSafeCachedCtorAndArgs;
            nonThreadSafeCachedCtorAndArgs = null;
            args = nonThreadSafeCachedArgs;
            nonThreadSafeCachedArgs = null;
            containingIndexes = nonThreadSafeCachedContainingIndexes;
            nonThreadSafeCachedContainingIndexes = null;
        }

        if ((containingIndexes == null) || (containingIndexes.length != indices.length))  {
            containingIndexes = new long[indices.length];
        }
        System.arraycopy(indices, 0, containingIndexes, 0, indices.length);

        if (args == null) {
            args = Arrays.copyOf(originalArgs, originalArgs.length);
        }
        args[containingIndexesIndexInArgs] = containingIndexes;

        if (ctorAndArgs == null) {
            // We have nothing cached that's not being used. A bit of allocation in contended cases won't kill us:
            ctorAndArgs = new CtorAndArgs<T>(constructor, args);
        }
        ctorAndArgs.setArgs(args);

        return ctorAndArgs;
    }


    /**
     * Recycle an {@link CtorAndArgs} instance (place it back in the internal cache if desired). This is [very]
     * useful for avoiding a re-allocation of a new {@link CtorAndArgs} and an associated args array for
     * {@link #getForIndex(long)} invocation in cases such as this (where the returned {@link CtorAndArgs}
     * is not constant across indices).
     * Recycling is optional, and is not guaranteed to occur.
     *
     * @param ctorAndArgs the {@link CtorAndArgs} instance to recycle
     */
    @SuppressWarnings("unchecked")
    public void recycle(final CtorAndArgs ctorAndArgs) {
        // Only recycle ctorAndArgs if ctorAndArgs is compatible with our state:
        if ((ctorAndArgs == null) || (ctorAndArgs.getConstructor() != constructor)) {
            return;
        }
        Object[] args = ctorAndArgs.getArgs();
        if ((args == null) || (args.length != originalArgs.length)) {
            return;
        }
        long[] containingIndexes = (long []) args[containingIndexesIndexInArgs];

        if (keepInternalCachingThreadSafe) {
            cachedConstructorAndArgs.lazySet(ctorAndArgs);
            cachedArgs.lazySet(args);
            cachedContainingIndexes.lazySet(containingIndexes);
        } else {
            nonThreadSafeCachedCtorAndArgs = ctorAndArgs;
            nonThreadSafeCachedArgs = args;
            nonThreadSafeCachedContainingIndexes = containingIndexes;
        }
    }
}
