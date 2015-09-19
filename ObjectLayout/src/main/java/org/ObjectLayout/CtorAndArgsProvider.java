/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

/**
 * A functional interface for providing a constructor and arguments given a construction context (index, etc.).
 * Used for providing element construction information in StructuredArrays.
 *
 * @param <T> type of the element occupying each array slot.
 */
public interface CtorAndArgsProvider<T> {

    /**
     * Get a {@link CtorAndArgs} instance to be used in constructing a given element index in
     * a {@link StructuredArray}
     *
     * @param context The construction context (index, containing array, etc.) of the element to be constructed
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    public CtorAndArgs<T> getForContext(final ConstructionContext<T> context) throws NoSuchMethodException;

}
