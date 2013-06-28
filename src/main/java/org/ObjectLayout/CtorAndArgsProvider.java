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

/**
 * Supports a general element construction API for {@link StructuredArray} by providing specific constructor
 * and arguments factory to used for constructing individual array elements during array creation.
 * <p>
 * Subclasses can be created that would provide a fixed constructor (as in {@link SingletonCtorAndArgsProvider}),
 * or to provide arguments and constructor values that would take the array index of the constructed element into account.
 * An example of this latter pattern can be found in the implementation of a {@link CopyCtorAndArgsProvider}.
 *
 * @param <T> type of the element occupying each array slot.
 */
public abstract class CtorAndArgsProvider<T> {

    private final Class<T> elementClass;

    /**
     * Used to apply a fixed constructor with a given set of arguments to all element.
     *
     * @param elementClass The element class
     * @throws NoSuchMethodException if a constructor matching defaultArgTypes
     * @throws IllegalArgumentException if argTypes and args conflict
     */
    public CtorAndArgsProvider(final Class<T> elementClass) throws NoSuchMethodException {
        if (null == elementClass) {
            throw new IllegalArgumentException("elementClass cannot be null");
        }
        this.elementClass = elementClass;
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}
     *
     * @param indices The indices of the element to be constructed in the target array
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public CtorAndArgs<T> getForIndices(final long[] indices) throws NoSuchMethodException {
        throw new IllegalArgumentException("No support for getForIndices()");
    }

    /**
     * Recycle a {@link CtorAndArgs} instance (place it back in the internal cache if desired).
     * This is [very] useful for avoiding a re-allocation of a new {@link CtorAndArgs} and an
     * associated args array for {@link #getForIndices(long[])} invocation in cases where
     * the returned {@link CtorAndArgs} is not constant.
     * <p>
     * Recycling is optional, and is not guaranteed to occur. It will only be done if it is helpful.
     * Overriding this method is optional for subclasses. See example in {@link CopyCtorAndArgsProvider}
     *
     * @param ctorAndArgs the {@link CtorAndArgs} instance to recycle
     */
    public void recycle(final CtorAndArgs ctorAndArgs) {
    }

    /**
     * Get the {@link Class} of the elements to be constructed
     *
     * @return {@link Class} of elements to be constructed
     */
    public Class<T> getElementClass() {
        return elementClass;
    }
}
