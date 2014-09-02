/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

/**
 * A functional interface for providing a constructor and arguments given an index.
 * Used for providing element construction information in StructuredArrays.
 * <p>
 * Implementations that wish to recycle the {@link org.ObjectLayout.CtorAndArgs} values they
 * provide in an effort to avoid allocating per-element instances of {@link org.ObjectLayout.CtorAndArgs}
 * should extend {@link org.ObjectLayout.AbstractCtorAndArgsProvider} rather than directly implement
 * this interface. {@link org.ObjectLayout.AbstractCtorAndArgsProvider} implementations can override
 * the {@link org.ObjectLayout.AbstractCtorAndArgsProvider#recycle(CtorAndArgs)} method, which would be
 * called on each {@link org.ObjectLayout.CtorAndArgs} after it is used for construction and is
 * not longer needed. An example of this recycling pattern can be found in the implementation of
 * {@link org.ObjectLayout.CopyCtorAndArgsProvider}.
 * </p>
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
