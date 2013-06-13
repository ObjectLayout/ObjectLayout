/*
 * Copyright 2013 Gil Tene
 * Copyright 2012, 2013 Martin Thompson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class MultiDimensionalCopyCtorAndArgsProvider<T> extends CtorAndArgsProvider<T> {

    private final Constructor<T> copyCtor;
    private final MultiDimensionalStructuredArray<T> source;
    private final long[] sourceOffsets;
    private final boolean keepInternalCachingThreadSafe;
    private CtorAndArgs<T> nonThreadSafeCachedCtorAndArgs = null;
    private long[] nonThreadSafeCachedTargetIndexes = null;
    private final AtomicReference<CtorAndArgs<T>> cachedConstructorAndArgs = new AtomicReference<CtorAndArgs<T>>();
    private final AtomicReference<long[]> cachedTargetIndexes = new AtomicReference<long[]>();

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source MultiDimensionalStructuredArray to copy from
     * @throws NoSuchMethodException
     */
    public MultiDimensionalCopyCtorAndArgsProvider(final Class<T> elementClass,
                                                   final MultiDimensionalStructuredArray<T> source) throws NoSuchMethodException {
        this(elementClass, source, null, true);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source MultiDimensionalStructuredArray to copy from
     * @param sourceOffsets The beginning index in the source from which to start copying
     * @throws NoSuchMethodException if a copy constructor is not found in element class
     */
    public MultiDimensionalCopyCtorAndArgsProvider(final Class<T> elementClass,
                                                   final MultiDimensionalStructuredArray<T> source,
                                                   final long[] sourceOffsets) throws NoSuchMethodException {
        this(elementClass, source, sourceOffsets, true);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source MultiDimensionalStructuredArray to copy from
     * @param sourceOffsets The beginning index in the source from which to start copying
     * @param keepInternalCachingThreadSafe Control whether or not internal caching is kept thread-safe
     * @throws NoSuchMethodException NoSuchMethodException if a copy constructor is not found in element class
     */
    public MultiDimensionalCopyCtorAndArgsProvider(final Class<T> elementClass,
                                                   final MultiDimensionalStructuredArray<T> source,
                                                   final long[] sourceOffsets,
                                                   final boolean keepInternalCachingThreadSafe) throws NoSuchMethodException {
        super(elementClass);
        copyCtor = elementClass.getConstructor(elementClass);
        this.source = source;
        if (sourceOffsets != null) {
            this.sourceOffsets = sourceOffsets;
        } else {
            this.sourceOffsets = new long[source.getNumOfDimensions()];
        }
        if (this.sourceOffsets.length != source.getNumOfDimensions()) {
            throw new IllegalArgumentException("number of sourceOffsets elements must match source.getDimensionCount()");
        }
        this.keepInternalCachingThreadSafe = keepInternalCachingThreadSafe;
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link MultiDimensionalStructuredArray}.                           .
     *
     * @param indices The indexes of the element to be constructed in the target array
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @SuppressWarnings("unchecked")
    public CtorAndArgs<T> getForIndices(final long[] indices) throws NoSuchMethodException {
        CtorAndArgs<T> ctorAndArgs;
        long[] targetIndexes;

        // Try (but not too hard) to use a cached, previously allocated ctorAndArgs object:
        if (keepInternalCachingThreadSafe) {
            ctorAndArgs = cachedConstructorAndArgs.getAndSet(null);
            targetIndexes = cachedTargetIndexes.getAndSet(null);
        } else {
            ctorAndArgs = nonThreadSafeCachedCtorAndArgs;
            nonThreadSafeCachedCtorAndArgs = null;
            targetIndexes = nonThreadSafeCachedTargetIndexes;
            nonThreadSafeCachedTargetIndexes = null;
        }

        if (ctorAndArgs == null) {
            // We have nothing cached that's not being used. A bit of allocation in contended cases won't kill us:
            ctorAndArgs = new CtorAndArgs<T>(copyCtor, new Object[1]);
        }
        if (targetIndexes == null) {
            targetIndexes = new long[sourceOffsets.length];
        }

        for (int i = 0; i < indices.length; i++) {
            targetIndexes[i] = indices[i] + sourceOffsets[i];
        }

        // Set the source object for the copy constructor:
        ctorAndArgs.getArgs()[0] = source.getL(targetIndexes);

        return ctorAndArgs;
    }


    /**
     * Recycle an {@link CtorAndArgs} instance (place it back in the internal cache if
     * desired). This is [very] useful for avoiding a re-allocation of a new {@link CtorAndArgs}
     * and an associated args array for {@link #getForIndices(long[])} invocation in cases such as this (where the
     * returned {@link CtorAndArgs} is not constant across indices).
     * Recycling is optional, and is not guaranteed to occur.
     *
     * @param ctorAndArgs the {@link CtorAndArgs} instance to recycle
     */
    @SuppressWarnings("unchecked")
    public void recycle(final CtorAndArgs ctorAndArgs) {
        // Only recycle ctorAndArgs if ctorAndArgs is compatible with our state:
        if (ctorAndArgs.getConstructor() != copyCtor ||
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
