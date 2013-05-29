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

/**
 * Captures a specific constructor and arguments to be passed to it. Useful for providing
 * fully customizable, per-element construction behavior for iterative constructors (such as those
 * used for elements in {@link StructuredArray})
 *
 * @param <T> type of the element occupying each array slot.
 */
public class ConstructorAndArgs<T> {

    private Constructor<T> constructor;
    private Object[] constructorArgs;

    /**
     * Create a {@link ConstructorAndArgs} instance. The presumption is that types in constructorArguments
     * match those expected by constructor. Obviously exceptions may be generated at construction time if
     * this is not the case.
     *
     * @param constructor Constructor to be indicated in this {@link ConstructorAndArgs}
     * @param constructorArguments constructor arguments to be indicated in this {@link ConstructorAndArgs}
     */
    public ConstructorAndArgs(final Constructor<T> constructor, final Object[] constructorArguments) {
        setConstructor(constructor);
        setConstructorArgs(constructorArguments);
    }

    /**
     * @return the Constructor indicated in this ConstructorAndArgs
     */
    public final Constructor<T> getConstructor() {
        return constructor;
    }

    /**
     * Set the constructor to be indicated in this {@link ConstructorAndArgs}. Enables recycling of
     * {@link ConstructorAndArgs} objects to avoid re-allocation. E.g. in copy construction loops.
     *
     * @param constructor Constructor to be indicated in this ConstructorAndArgs
     */
    public final void setConstructor(final Constructor<T> constructor) {
        if (null == constructor) {
            throw new NullPointerException("constructor cannot be null");
        }
        this.constructor = constructor;
    }

    /**
     * @return the constructor arguments indicated in this ConstructorAndArgs
     */
    public final Object[] getConstructorArgs() {
        return constructorArgs;
    }

    /**
     * Set the constructor arguments to be indicated in this ConstructorAndArgs. Enables recycling of
     * {@link ConstructorAndArgs} objects to avoid re-allocation. E.g. in copy construction loops.
     *
     * @param constructorArgs constructor arguments to be indicated in this {@link ConstructorAndArgs}
     */
    public final void setConstructorArgs(final Object[] constructorArgs) {
        if (null == constructorArgs) {
            throw new NullPointerException("constructorArgs cannot be null");
        }
        this.constructorArgs = constructorArgs;
    }
}
