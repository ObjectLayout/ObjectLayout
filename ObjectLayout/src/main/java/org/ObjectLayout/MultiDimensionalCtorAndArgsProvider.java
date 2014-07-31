/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

/**
 * A functional interface for providing a constructor and arguments, given a
 * (multi-dimensional) index. Used for providing element construction information
 * in (multi-dimensional) StructuredArrays.
 *
 * @param <T> type of the element occupying each array slot.
 */
public interface MultiDimensionalCtorAndArgsProvider<T> extends AbstractCtorAndArgsProvider<T> {

    /**
     * Get a {@link CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}
     *
     * @param index The index of the element to be constructed in the target array (one value per dimension).
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public CtorAndArgs<T> getForIndex(final long... index) throws NoSuchMethodException;

}
