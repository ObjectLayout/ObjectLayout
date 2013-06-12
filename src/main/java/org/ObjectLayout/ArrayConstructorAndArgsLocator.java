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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Supports the construction of a new array's individual elements using a copy constructor to copy a source
 * array's corresponding elements.
 *
 * @param <T> type of the element occupying each array slot
 */
public class ArrayConstructorAndArgsLocator<T> extends ConstructorAndArgsLocator<T> {

    final Constructor<T> constructor;
    final Object[] originalArgs;
    final int containingIndexesIndexInArgs;

    final boolean keepInternalCachingThreadSafe;
    ConstructorAndArgs<T> nonThreadSafeCachedConstructorAndArgs = null;
    Object[] nonThreadSafeCachedArgs = null;
    long[] nonThreadSafeCachedContainingIndexes = null;
    final AtomicReference<ConstructorAndArgs<T>> cachedConstructorAndArgs = new AtomicReference<ConstructorAndArgs<T>>();
    final AtomicReference<Object[]> cachedArgs = new AtomicReference<Object[]>();
    final AtomicReference<long[]> cachedContainingIndexes = new AtomicReference<long[]>();


    /**
     * Used to apply a fixed constructor with a given set of arguments to all elements.
     *
     * @param constructor The element constructor
     * @param args The arguments to be passed to the constructor for all elements
     * @throws NoSuchMethodException if a constructor matching argTypes
     * @throws IllegalArgumentException if argTypes and args conflict
     */
    public ArrayConstructorAndArgsLocator(final Constructor<T> constructor,
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
    public ArrayConstructorAndArgsLocator(final Constructor<T> constructor,
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
     * Get a {@link org.ObjectLayout.ConstructorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}.                           .
     *
     * @param index The index of the element to be constructed in the target array
     * @return {@link org.ObjectLayout.ConstructorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @SuppressWarnings("unchecked")
    public ConstructorAndArgs<T> getForIndex(final long index) throws NoSuchMethodException {
        throw new IllegalArgumentException("getForIndex not supported");
    }


    public ConstructorAndArgs<T> getForIndexes(final long[] indexes) throws NoSuchMethodException {
        ConstructorAndArgs<T> constructorAndArgs;
        Object[] args;
        long[] containingIndexes;

        // Try (but not too hard) to use a cached, previously allocated constructorAndArgs object:
        if (keepInternalCachingThreadSafe) {
            constructorAndArgs = cachedConstructorAndArgs.getAndSet(null);
            args = cachedArgs.getAndSet(null);
            containingIndexes = cachedContainingIndexes.getAndSet(null);
        } else {
            constructorAndArgs = nonThreadSafeCachedConstructorAndArgs;
            nonThreadSafeCachedConstructorAndArgs = null;
            args = nonThreadSafeCachedArgs;
            nonThreadSafeCachedArgs = null;
            containingIndexes = nonThreadSafeCachedContainingIndexes;
            nonThreadSafeCachedContainingIndexes = null;
        }

        if ((containingIndexes == null) || (containingIndexes.length != indexes.length))  {
            containingIndexes = new long[indexes.length];
        }
        System.arraycopy(indexes, 0, containingIndexes, 0, indexes.length);

        if (args == null) {
            args = Arrays.copyOf(originalArgs, originalArgs.length);
        }
        args[containingIndexesIndexInArgs] = containingIndexes;

        if (constructorAndArgs == null) {
            // We have nothing cached that's not being used. A bit of allocation in contended cases won't kill us:
            constructorAndArgs = new ConstructorAndArgs<T>(constructor, args);
        }
        constructorAndArgs.setConstructorArgs(args);

        return constructorAndArgs;
    }


    /**
     * Recycle an {@link org.ObjectLayout.ConstructorAndArgs} instance (place it back in the internal cache if desired). This is [very]
     * useful for avoiding a re-allocation of a new {@link org.ObjectLayout.ConstructorAndArgs} and an associated args array for
     * {@link #getForIndex(long)} invocation in cases such as this (where the returned {@link org.ObjectLayout.ConstructorAndArgs}
     * is not constant across indices).
     * Recycling is optional, and is not guaranteed to occur.
     *
     * @param constructorAndArgs the {@link org.ObjectLayout.ConstructorAndArgs} instance to recycle
     */
    @SuppressWarnings("unchecked")
    public void recycle(final ConstructorAndArgs constructorAndArgs) {
        // Only recycle constructorAndArgs if constructorAndArgs is compatible with our state:
        if ((constructorAndArgs == null) || (constructorAndArgs.getConstructor() != constructor)) {
            return;
        }
        Object[] args = constructorAndArgs.getConstructorArgs();
        if ((args == null) || (args.length != originalArgs.length)) {
            return;
        }
        long[] containingIndexes = (long []) args[containingIndexesIndexInArgs];

        if (keepInternalCachingThreadSafe) {
            cachedConstructorAndArgs.lazySet(constructorAndArgs);
            cachedArgs.lazySet(args);
            cachedContainingIndexes.lazySet(containingIndexes);
        } else {
            nonThreadSafeCachedConstructorAndArgs = constructorAndArgs;
            nonThreadSafeCachedArgs = args;
            nonThreadSafeCachedContainingIndexes = containingIndexes;
        }
    }
}
