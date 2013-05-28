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

/**
 * Supports the construction of a new array's individual elements using a copy constructor to copy a source
 * array's corresponding elements
 * @param <T> type of the element occupying each array slot
 */
public class ElementCopyConstructorGenerator<T> extends ElementConstructorGenerator {
    final Constructor<T> copyConstructor;
    final AbstractStructuredArray<T> source;
    final long sourceOffset;

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source StructuredArray to copy from
     * @throws NoSuchMethodException
     */
    public ElementCopyConstructorGenerator(final Class<T> elementClass,
                                           AbstractStructuredArray<T> source) throws NoSuchMethodException {
        this(elementClass, source, 0);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param elementClass The class of the elements to be constructed
     * @param source The source StructuredArray to copy from
     * @param sourceOffset The beginning index in the source from which to start copying
     * @throws NoSuchMethodException if a copy constructor is not found in element class
     */
    @SuppressWarnings("unchecked")
    public ElementCopyConstructorGenerator(final Class<T> elementClass, AbstractStructuredArray<T> source,
                                           long sourceOffset) throws NoSuchMethodException {
        super(elementClass);
        copyConstructor = elementClass.getConstructor(elementClass);
        this.source = source;
        this.sourceOffset = sourceOffset;
        cachedConstructorAndArgsObject.set(new ConstructorAndArgs<T>(copyConstructor, new Object[1]));
    }

    /**
     * Get a {@link ConstructorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}
     * @param index The index of the element to be constructed in the target array
     * @return {@link ConstructorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @SuppressWarnings("unchecked")
    public ConstructorAndArgs<T> getElementConstructorAndArgsForIndex(final long index) throws NoSuchMethodException {
        // Try (but not too hard) to use a cached, previously allocated constructorAndArgs object:
        ConstructorAndArgs<T> constructorAndArgs =
                (ConstructorAndArgs<T>) cachedConstructorAndArgsObject.getAndSet(null);
        if (constructorAndArgs == null) {
            // Someone is using the previously cached instance. A bit of allocation in contended cases won't kill us:
            constructorAndArgs = new ConstructorAndArgs<T>(copyConstructor, new Object[1]);
        }
        // Set the source object for the copy constructor:
        constructorAndArgs.getConstructorArgs()[0] = source.get(index + sourceOffset);

        return constructorAndArgs;
    }

    /**
     * Recycle a ConstructorAndArgs instance (place it back in the internal cache if desired). This is [very]
     * useful for avoiding a re-allocation of a new ConstructorAndArgs and an associated args array for
     * getElementConstructorAndArgsForIndex invocation in cases where the returned ConstructorAndArgs is not constant.
     * Recycling is optional, and is not guaranteed to occur. It will only be done if it is helpful.
     * @param constructorAndArgs the {@link ConstructorAndArgs} instance to recycle
     */
    @SuppressWarnings("unchecked")
    public void recycleElementConstructorAndArgs(ConstructorAndArgs constructorAndArgs) {
        // Only recycle constructorAndArgs if constructorAndArgs is compatible with our state:
        if ((constructorAndArgs.getConstructor() != copyConstructor) ||
                (constructorAndArgs.getConstructorArgs().length != 1)) {
            return;
        }
        cachedConstructorAndArgsObject.set(constructorAndArgs);
    }
}
