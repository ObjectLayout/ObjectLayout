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

import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Constructor;

/**
 * <p>
 * Supports a general element construction API for {@link StructuredArray} by providing specific constructor
 * and arguments generator to used for constructing individual array elements during
 * array creation.
 * Subclasses can be created that would provide a fixed constructor (as in {@link ElementFixedConstructorGenerator}),
 * or to provide arguments and constructor values that would take the array index of the constructed element
 * into account. A good example of this latter pattern can be found in the implementation of
 * {@link ElementCopyConstructorGenerator}.
 * </p>
 * @param <T> type of the element occupying each array slot.
 */
abstract public class ElementConstructorGenerator<T> {

    private final Class<T> elementClass;

    final AtomicReference<ConstructorAndArgs<T>> cachedConstructorAndArgsObject =
            new AtomicReference<ConstructorAndArgs<T>>();

    /**
     * Used to apply a fixed constructor with a given set of arguments to all element.
     * @param elementClass The element class
     * @throws NoSuchMethodException if a constructor matching defaultArgTypes
     * @throws IllegalArgumentException if argTypes and args conflict
     */
    public ElementConstructorGenerator(final Class<T> elementClass) throws NoSuchMethodException {
        this.elementClass = elementClass;
    }

    /**
     * Get a {@link ConstructorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}
     * @param index The index of the element to be constructed in the target array
     * @return {@link ConstructorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    abstract public ConstructorAndArgs<T> getElementConstructorAndArgsForIndex(final long index) throws NoSuchMethodException;

    /**
     * Recycle a ConstructorAndArgs instance (place it back in the internal cache if desired). This is [very]
     * useful for avoiding a re-allocation of a new ConstructorAndArgs and an associated args array for
     * getElementConstructorAndArgsForIndex invocation in cases where the returned ConstructorAndArgs is not constant.
     * Recycling is optional, and is not guaranteed to occur. It will only be done if it is helpful.
     * @param constructorAndArgs the {@link ConstructorAndArgs} instance to recycle
     */
    abstract public void recycleElementConstructorAndArgs(ConstructorAndArgs constructorAndArgs);

    /**
     * Get the {@link Class} of the elements to be constructed
     * @return {@link Class} of elements to be constructed
     */
    public Class<T> getElementClass() {
        return elementClass;
    }
}
