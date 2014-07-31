/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

/**
 * A concrete implementation of both SingleDimensionalCtorAndArgsProvider and
 * MultiDimensionalCtorAndArgsProvider. Supports a general element construction API
 * for {@link org.ObjectLayout.StructuredArray} by providing specific constructor
 * and arguments factory to be used for constructing individual array elements
 * during array creation.
 * <p>
 * Subclasses can be created that would provide a fixed constructor
 * (as in {@link org.ObjectLayout.SingletonCtorAndArgsProvider}), or to provide
 * arguments and constructor values that would take the array index of the constructed
 * element into account. An example of this latter pattern can be found in the
 * implementation of a {@link org.ObjectLayout.CopyCtorAndArgsProvider}.
 *
 * @param <T> type of the element occupying each array slot.
 */
public abstract class CtorAndArgsProvider<T>
        implements SingleDimensionalCtorAndArgsProvider<T>, MultiDimensionalCtorAndArgsProvider<T> {

    /**
     * Get a {@link org.ObjectLayout.CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link org.ObjectLayout.StructuredArray}
     *
     * @param index The index of the element to be constructed in the target array.
     * @return {@link org.ObjectLayout.CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public CtorAndArgs<T> getForIndex(final long index) throws NoSuchMethodException {
        return getForIndex(new long[] { index });
    }

    /**
     * Get a {@link org.ObjectLayout.CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link org.ObjectLayout.StructuredArray}
     *
     * @param index The index of the element to be constructed in the target array (one value per dimension).
     * @return {@link org.ObjectLayout.CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public CtorAndArgs<T> getForIndex(final long... index) throws NoSuchMethodException {
        if (index.length == 1) {
            throw new IllegalArgumentException("No support for getForIndex()");
        }
        throw new IllegalArgumentException("No support for multi dimensional getForIndex()");
    }

    /**
     * Recycle a {@link org.ObjectLayout.CtorAndArgs} instance (place it back in the internal cache if desired).
     * This is [very] useful for avoiding a re-allocation of a new {@link org.ObjectLayout.CtorAndArgs} and an
     * associated args array for {@link #getForIndex(long[])} invocation in cases where
     * the returned {@link org.ObjectLayout.CtorAndArgs} is not constant.
     * <p>
     * Recycling is optional, and is not guaranteed to occur. It will only be done if it is helpful.
     * Overriding this method is optional for subclasses. See example in {@link org.ObjectLayout.CopyCtorAndArgsProvider}
     *
     * @param ctorAndArgs the {@link org.ObjectLayout.CtorAndArgs} instance to recycle
     */
    public void recycle(final CtorAndArgs<T> ctorAndArgs) {
    }
}
