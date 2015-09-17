/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

/**
 * Supports the construction of a {@link org.ObjectLayout.StructuredArray}'s individual elements using a
 * copy constructor, copying a source array's individual elements.
 * <p>
 * Expects the source {@link org.ObjectLayout.StructuredArray} to be provided as an opaque (Object) cookie
 * in the {@link ConstructionContext} parameter passed to the
 * {@link org.ObjectLayout.CopyCtorAndArgsProvider#getForContext(ConstructionContext)} method.
 * <p>
 * If the source element being copied is itself an instance of {@link org.ObjectLayout.StructuredArray}, the
 * source element will be passed through as a contextCookie in the outgoing {@link org.ObjectLayout.CtorAndArgs},
 * and will become the source array (the context cookie) in the next {@link org.ObjectLayout.StructuredArray}
 * nesting level and it's calls to {@link CopyCtorAndArgsProvider#getForContext(ConstructionContext)}.
 * <p>
 * {@link CopyCtorAndArgsProvider} will attempt to efficiently recycle {@link org.ObjectLayout.CtorAndArgs}
 * instances and to avoid per-element allocation of new {@link org.ObjectLayout.CtorAndArgs} instances by caching
 * and recycling discarded instances.
 *
 * @param <T> type of the element occupying each array slot
 */
class CopyCtorAndArgsProvider<T> implements CtorAndArgsProvider<T> {
    private static final MethodHandles.Lookup noLookup = null;

    private final long sourceOffset;
    private final Constructor<T> copyConstructor;
    private final CtorAndArgs<T> ctorAndArgs;

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array
     */
    public CopyCtorAndArgsProvider(
            final Class<T> elementClass) throws NoSuchMethodException {
        this(noLookup, elementClass);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array
     */
    public CopyCtorAndArgsProvider(
            MethodHandles.Lookup lookup,
            final Class<T> elementClass) {
        this(lookup, elementClass, 0);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param sourceOffset The beginning index in the source from which to start copying
     */
    public CopyCtorAndArgsProvider(
            final Class<T> elementClass,
            final long sourceOffset) {
        this(noLookup, elementClass, sourceOffset);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param lookup The lookup object to use when resolving constructors
     * @param elementClass The element class to which the copy constructor belongs
     * @param sourceOffset The beginning index in the source from which to start copying
     */
    public CopyCtorAndArgsProvider(
            MethodHandles.Lookup lookup,
            final Class<T> elementClass,
            final long sourceOffset) {
            this.sourceOffset = sourceOffset;
            this.ctorAndArgs = new CtorAndArgs<>(lookup, elementClass, new Class[] {elementClass}, new Object[1]);
            this.copyConstructor = ctorAndArgs.getConstructor(); // Remember it so we can overwrite back each time.
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in copy-constructing a given element index in
     * a {@link StructuredArray}.                           .
     *
     * @param context The construction context (index, containing array, etc.) of the element to be constructed.
     * @return {@link CtorAndArgs} instance to used in element construction
     */
    @Override
    public CtorAndArgs<T> getForContext(final ConstructionContext context) {

        // Find source array for this context:
        @SuppressWarnings("unchecked")
        StructuredArray<T> sourceArray = (StructuredArray<T>) context.getContextCookie();

        long index = context.getIndex() + sourceOffset;

        T sourceElement = sourceArray.get(index);

        ctorAndArgs.setConstructor(copyConstructor);
        // Set the source object for the copy constructor:
        ctorAndArgs.getArgs()[0] = sourceElement;

        // Set the cookie object to the source element. Will be passed on as the cookie object in the
        // next context level if the sourceElement is a StructuredArray.
        ctorAndArgs.setContextCookie(sourceElement);

        return ctorAndArgs;
    }
}
