/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
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
     * @param index The index of the element to be constructed in the target array.
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public CtorAndArgs<T> getForIndex(final long index) throws NoSuchMethodException {
        return getForIndex(new long[] { index });
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}
     *
     * @param index The index of the element to be constructed in the target array (one value per dimension).
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public CtorAndArgs<T> getForIndex(final long... index) throws NoSuchMethodException {
        if (index.length == 1) {
            throw new IllegalArgumentException("No support for getForIndex()");
        }
        throw new IllegalArgumentException("No support for multi dimensional getForIndex()");
    }

    /**
     * Recycle a {@link CtorAndArgs} instance (place it back in the internal cache if desired).
     * This is [very] useful for avoiding a re-allocation of a new {@link CtorAndArgs} and an
     * associated args array for {@link #getForIndex(long[])} invocation in cases where
     * the returned {@link CtorAndArgs} is not constant.
     * <p>
     * Recycling is optional, and is not guaranteed to occur. It will only be done if it is helpful.
     * Overriding this method is optional for subclasses. See example in {@link CopyCtorAndArgsProvider}
     *
     * @param ctorAndArgs the {@link CtorAndArgs} instance to recycle
     */
    public void recycle(final CtorAndArgs<T> ctorAndArgs) {
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
