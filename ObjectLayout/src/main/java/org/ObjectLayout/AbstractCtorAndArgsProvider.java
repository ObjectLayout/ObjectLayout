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
public interface AbstractCtorAndArgsProvider<T> {

}
