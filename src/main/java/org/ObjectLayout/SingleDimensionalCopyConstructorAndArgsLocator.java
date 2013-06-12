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
public class SingleDimensionalCopyConstructorAndArgsLocator<T> extends ConstructorAndArgsLocator<T> {

    final Constructor<T> copyConstructor;
    final SingleDimensionalStructuredArray<T> source;
    final long sourceOffset;
    final boolean keepInternalCachingThreadSafe;
    ConstructorAndArgs<T> nonThreadSafeCachedConstructorAndArgs = null;
    final AtomicReference<ConstructorAndArgs<T>> cachedConstructorAndArgs = new AtomicReference<ConstructorAndArgs<T>>();

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source SingleDimensionalStructuredArray to copy from
     * @throws NoSuchMethodException
     */
    public SingleDimensionalCopyConstructorAndArgsLocator(final Class<T> elementClass,
                                                          final SingleDimensionalStructuredArray<T> source) throws NoSuchMethodException {
        this(elementClass, source, 0);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source SingleDimensionalStructuredArray to copy from
     * @param sourceOffset The beginning index in the source from which to start copying
     * @throws NoSuchMethodException if a copy constructor is not found in element class
     */
    public SingleDimensionalCopyConstructorAndArgsLocator(final Class<T> elementClass,
                                                          final SingleDimensionalStructuredArray<T> source,
                                                          final long sourceOffset) throws NoSuchMethodException {
        this(elementClass, source, sourceOffset, true);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source SingleDimensionalStructuredArray to copy from
     * @param sourceOffset The beginning index in the source from which to start copying
     * @param keepInternalCachingThreadSafe Control whether or not internal caching is kept thread-safe
     * @throws NoSuchMethodException NoSuchMethodException if a copy constructor is not found in element class
     */
    public SingleDimensionalCopyConstructorAndArgsLocator(final Class<T> elementClass,
                                                          final SingleDimensionalStructuredArray<T> source,
                                                          final long sourceOffset,
                                                          final boolean keepInternalCachingThreadSafe) throws NoSuchMethodException {
        super(elementClass);

        copyConstructor = elementClass.getConstructor(elementClass);
        this.source = source;
        this.sourceOffset = sourceOffset;
        this.keepInternalCachingThreadSafe = keepInternalCachingThreadSafe;
    }


    @SuppressWarnings("unchecked")
    private ConstructorAndArgs<T> getForIndex(final long index) throws NoSuchMethodException {
        ConstructorAndArgs<T> constructorAndArgs;

        // Try (but not too hard) to use a cached, previously allocated constructorAndArgs object:
        if (keepInternalCachingThreadSafe) {
            constructorAndArgs = cachedConstructorAndArgs.getAndSet(null);
        } else {
            constructorAndArgs = nonThreadSafeCachedConstructorAndArgs;
            nonThreadSafeCachedConstructorAndArgs = null;
        }

        if (constructorAndArgs == null) {
            // We have nothing cached that's not being used. A bit of allocation in contended cases won't kill us:
            constructorAndArgs = new ConstructorAndArgs<T>(copyConstructor, new Object[1]);
        }

        // Set the source object for the copy constructor:
        constructorAndArgs.getConstructorArgs()[0] = source.getL(index + sourceOffset);

        return constructorAndArgs;
    }

    /**
     * Get a {@link ConstructorAndArgs} instance to be used in constructing a given element index in
     * a {@link SingleDimensionalStructuredArray}. (supports only 1 dimensional array copies).                           .
     *
     * @param indices The index of the element to be constructed in the target array (supports only 1 dimensional copies)
     * @return {@link ConstructorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public ConstructorAndArgs<T> getForIndices(final long[] indices) throws NoSuchMethodException {
        if (indices.length != 1) {
            throw new IllegalArgumentException("Not supported for multi-dimensional copies");
        }
        return getForIndex(indices[0]);
    }


    /**
     * Recycle an {@link ConstructorAndArgs} instance (place it back in the internal cache if desired). This is [very]
     * useful for avoiding a re-allocation of a new {@link ConstructorAndArgs} and an associated args array for
     * {@link #getForIndex(long)} invocation in cases such as this (where the returned {@link ConstructorAndArgs}
     * is not constant across indices).
     * Recycling is optional, and is not guaranteed to occur.
     *
     * @param constructorAndArgs the {@link ConstructorAndArgs} instance to recycle
     */
    @SuppressWarnings("unchecked")
    public void recycle(final ConstructorAndArgs constructorAndArgs) {
        // Only recycle constructorAndArgs if constructorAndArgs is compatible with our state:
        if (constructorAndArgs.getConstructor() != copyConstructor ||
            constructorAndArgs.getConstructorArgs().length != 1) {
            return;
        }

        if (keepInternalCachingThreadSafe) {
            cachedConstructorAndArgs.lazySet(constructorAndArgs);
        } else {
            nonThreadSafeCachedConstructorAndArgs = constructorAndArgs;
        }
    }
}
