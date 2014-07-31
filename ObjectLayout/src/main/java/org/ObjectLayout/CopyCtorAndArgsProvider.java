/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Supports the construction of a new array's individual elements using a copy constructor to copy a source
 * array's corresponding elements.
 *
 * @param <T> type of the element occupying each array slot
 */
public class CopyCtorAndArgsProvider<T> extends CtorAndArgsProvider<T> {

    private final Constructor<T> copyConstructor;
    private final StructuredArray<T> source;
    private final long[] sourceOffsets;
    private final boolean keepInternalCachingThreadSafe;
    private CtorAndArgs<T> nonThreadSafeCachedCtorAndArgs = null;
    private long[] nonThreadSafeCachedTargetIndex = null;
    private final AtomicReference<CtorAndArgs<T>> cachedConstructorAndArgs = new AtomicReference<CtorAndArgs<T>>();
    private final AtomicReference<long[]> cachedTargetIndex = new AtomicReference<long[]>();

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source StructuredArray to copy from
     * @throws NoSuchMethodException
     */
    public CopyCtorAndArgsProvider(final Class<T> elementClass,
                                   final StructuredArray<T> source) throws NoSuchMethodException {
        this(elementClass, source, null, true);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source StructuredArray to copy from
     * @param sourceOffsets The beginning index in the source from which to start copying
     * @throws NoSuchMethodException if a copy constructor is not found in element class
     */
    public CopyCtorAndArgsProvider(final Class<T> elementClass,
                                   final StructuredArray<T> source,
                                   final long... sourceOffsets) throws NoSuchMethodException {
        this(elementClass, source, sourceOffsets, true);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source StructuredArray to copy from
     * @param sourceOffsets The beginning index in the source from which to start copying
     * @param keepInternalCachingThreadSafe Control whether or not internal caching is kept thread-safe
     * @throws NoSuchMethodException NoSuchMethodException if a copy constructor is not found in element class
     */
    public CopyCtorAndArgsProvider(final Class<T> elementClass,
                                   final StructuredArray<T> source,
                                   final long[] sourceOffsets,
                                   final boolean keepInternalCachingThreadSafe) throws NoSuchMethodException {
        copyConstructor = elementClass.getConstructor(elementClass);
        this.source = source;
        if (sourceOffsets != null) {
            this.sourceOffsets = sourceOffsets;
        } else {
            this.sourceOffsets = new long[source.getDimensionCount()];
        }
        if (this.sourceOffsets.length != source.getDimensionCount()) {
            throw new IllegalArgumentException("number of sourceOffsets elements must match source.getDimensionCount()");
        }
        this.keepInternalCachingThreadSafe = keepInternalCachingThreadSafe;
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in copy-constructing a given element index in
     * a {@link StructuredArray}.                           .
     *
     * @param index The index of the element to be constructed in the target array
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @Override
    @SuppressWarnings("unchecked")
    public CtorAndArgs<T> getForIndex(long index) throws NoSuchMethodException {
        CtorAndArgs<T> ctorAndArgs;

        // Try (but not too hard) to use a cached, previously allocated ctorAndArgs object:
        if (keepInternalCachingThreadSafe) {
            ctorAndArgs = cachedConstructorAndArgs.getAndSet(null);
        } else {
            ctorAndArgs = nonThreadSafeCachedCtorAndArgs;
            nonThreadSafeCachedCtorAndArgs = null;
        }

        if (ctorAndArgs == null) {
            // We have nothing cached that's not being used. A bit of allocation in contended cases won't kill us:
            ctorAndArgs = new CtorAndArgs<T>(copyConstructor, new Object[1]);
        }

        long targetIndex = index + sourceOffsets[0];

        // Set the source object for the copy constructor:
        ctorAndArgs.getArgs()[0] = source.get(targetIndex);

        return ctorAndArgs;
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in copy-constructing a given element index in
     * a {@link StructuredArray}.                           .
     *
     * @param index The index of the element to be constructed in the target array (one value per dimension)
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @Override
    @SuppressWarnings("unchecked")
    public CtorAndArgs<T> getForIndex(long... index) throws NoSuchMethodException {
        CtorAndArgs<T> ctorAndArgs;
        long[] targetIndex;

        // Try (but not too hard) to use a cached, previously allocated ctorAndArgs object:
        if (keepInternalCachingThreadSafe) {
            ctorAndArgs = cachedConstructorAndArgs.getAndSet(null);
            targetIndex = cachedTargetIndex.getAndSet(null);
        } else {
            ctorAndArgs = nonThreadSafeCachedCtorAndArgs;
            nonThreadSafeCachedCtorAndArgs = null;
            targetIndex = nonThreadSafeCachedTargetIndex;
            nonThreadSafeCachedTargetIndex = null;
        }

        if (ctorAndArgs == null) {
            // We have nothing cached that's not being used. A bit of allocation in contended cases won't kill us:
            ctorAndArgs = new CtorAndArgs<T>(copyConstructor, new Object[1]);
        }
        if (targetIndex == null) {
            targetIndex = new long[sourceOffsets.length];
        }

        for (int i = 0; i < index.length; i++) {
            targetIndex[i] = index[i] + sourceOffsets[i];
        }

        // Set the source object for the copy constructor:
        ctorAndArgs.getArgs()[0] = source.get(targetIndex);

        return ctorAndArgs;
    }


    /**
     * Recycle an {@link CtorAndArgs} instance (place it back in the internal cache if
     * desired). This is [very] useful for avoiding a re-allocation of a new {@link CtorAndArgs}
     * and an associated args array for {@link #getForIndex(long[])} invocation in cases such as this (where the
     * returned {@link CtorAndArgs} is not constant across indices).
     * Recycling is optional, and is not guaranteed to occur.
     *
     * @param ctorAndArgs the {@link CtorAndArgs} instance to recycle
     */
    @Override
    @SuppressWarnings("unchecked")
    public void recycle(final CtorAndArgs<T> ctorAndArgs) {
        // Only recycle ctorAndArgs if ctorAndArgs is compatible with our state:
        if (ctorAndArgs.getConstructor() != copyConstructor ||
            ctorAndArgs.getArgs().length != 1) {
            return;
        }

        if (keepInternalCachingThreadSafe) {
            cachedConstructorAndArgs.lazySet(ctorAndArgs);
        } else {
            nonThreadSafeCachedCtorAndArgs = ctorAndArgs;
        }
    }
}
