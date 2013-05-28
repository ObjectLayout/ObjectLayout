/*
 * Copyright 2013 Gil Tene
 * Copyright 2012, 2013 Real Logic Ltd.
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
 * <p>
 * Supports a fixed (cached) constructor and set of arguments for either default construction or construction
 * with a given fixed set of arguments (repeated for all indexes)
 * </p>
 * @param <T> type of the element occupying each array slot
 */
public class ElementFixedConstructorGenerator<T> extends ElementConstructorGenerator<T> {
    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    final AtomicReference<ConstructorAndArgs<T>> cachedConstructorAndArgsObject =
            new AtomicReference<ConstructorAndArgs<T>>();

    /**
     * Used to apply default constructor to all elements
     * @param elementClass The element class
     * @throws NoSuchMethodException if no default constructor is found for elementClass
     */
    public ElementFixedConstructorGenerator(final Class<T> elementClass) throws NoSuchMethodException {
        this(elementClass, EMPTY_ARG_TYPES, EMPTY_ARGS);
    }

    /**
     * Used to apply a fixed constructor with a given set of arguments to all elements
     * @param elementClass The element class
     * @param argTypes The argument types for the element constructor
     * @param args The arguments to be passed to the constructor for all elements
     * @throws NoSuchMethodException if a constructor matching argTypes
     * @throws IllegalArgumentException if argTypes and args conflict
     */
    public ElementFixedConstructorGenerator(final Class<T> elementClass,
                                            final Class[] argTypes, final Object[] args) throws NoSuchMethodException {
        super(elementClass);
        if (argTypes.length != args.length)
        {
            throw new IllegalArgumentException("argument types and values must be the same length");
        }

        Constructor<T> constructor = elementClass.getConstructor(argTypes);
        cachedConstructorAndArgsObject.set(new ConstructorAndArgs<T>(constructor, args));
    }

    /**
     * Get a {@link org.ObjectLayout.ConstructorAndArgs} instance to be used in constructing a given element index in
     * a {@link org.ObjectLayout.StructuredArray}
     * @param index The index of the element to be constructed in the target array
     * @return {@link org.ObjectLayout.ConstructorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public ConstructorAndArgs<T> getElementConstructorAndArgsForIndex(final long index) throws NoSuchMethodException {
        return cachedConstructorAndArgsObject.get();
    }

    /**
     * Recycle a ConstructorAndArgs instance (place it back in the internal cache if desired). This is [very]
     * useful for avoiding a re-allocation of a new ConstructorAndArgs and an associated args array for
     * getElementConstructorAndArgsForIndex invocation in cases where the returned ConstructorAndArgs is not constant.
     * Recycling is optional, and is not guaranteed to occur. It will only be done if it is helpful.
     * @param constructorAndArgs the {@link org.ObjectLayout.ConstructorAndArgs} instance to recycle
     */
    public void recycleElementConstructorAndArgs(ConstructorAndArgs constructorAndArgs) {
        // This class has a fixed-at-construction-time constructorAndArgs, so do nothing here.
    }
}
