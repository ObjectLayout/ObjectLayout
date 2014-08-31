/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Supports the construction of a new array's individual elements using a copy constructor to copy a source
 * array's corresponding elements.
 *
 * @param <T> type of the element occupying each array slot
 */
public class CopyCtorAndArgsProvider<T> extends AbstractCtorAndArgsProvider<T> {

    private final long sourceOffset;
    private final Constructor<T> copyConstructor;
    private final AtomicReference<CtorAndArgs<T>> cachedConstructorAndArgs = new AtomicReference<CtorAndArgs<T>>();

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array
     *
     * @throws NoSuchMethodException
     */
    public CopyCtorAndArgsProvider(final Class<T> elementClass) throws NoSuchMethodException {
        this(elementClass, 0);
    }

    /**
     * Used to apply a copy constructor to a target array's elements, copying corresponding elements from a
     * source array, starting at a given offset
     *
     * @param sourceOffset The beginning index in the source from which to start copying
     * @throws NoSuchMethodException if a copy constructor is not found in element class
     */
    public CopyCtorAndArgsProvider(final Class<T> elementClass, final long sourceOffset) throws NoSuchMethodException {
        this.sourceOffset = sourceOffset;
        this.copyConstructor = elementClass.getConstructor(elementClass);
    }

    /**
     * Get a {@link CtorAndArgs} instance to be used in copy-constructing a given element index in
     * a {@link StructuredArray}.                           .
     *
     * @param context The construction context (index, containing array, etc.) of the element to be constructed.
     * @return {@link CtorAndArgs} instance to used in element construction
     * @throws NoSuchMethodException if expected constructor is not found in element class
     */
    @Override
    public CtorAndArgs<T> getForContext(final ConstructionContext context) throws NoSuchMethodException {

        // Find source array for this context:
        @SuppressWarnings("unchecked")
        StructuredArray<T> sourceArray = (StructuredArray<T>) context.getCookie();

        long index = context.getIndex() + sourceOffset;

        T sourceElement = sourceArray.get(index);

        // Try (but not too hard) to use a cached, previously allocated ctorAndArgs object:
        CtorAndArgs<T> ctorAndArgs = cachedConstructorAndArgs.getAndSet(null);
        if (ctorAndArgs == null) {
            // We have nothing cached that's not being used. A bit of allocation in contended cases won't kill us:
            ctorAndArgs = new CtorAndArgs<T>(copyConstructor, new Object[1]);
        }

        ctorAndArgs.setConstructor(copyConstructor);
        // Set the source object for the copy constructor:
        ctorAndArgs.getArgs()[0] = sourceElement;

        // Set the cookie object to the source element. Will be passed on as the cookie object in the
        // next context level if the sourceElement is a StructuredArray.
        ctorAndArgs.setContextCookie(sourceElement);

        return ctorAndArgs;
    }

    /**
     * Recycle an {@link CtorAndArgs} instance (place it back in the internal cache if
     * desired). This is [very] useful for avoiding a re-allocation of a new {@link CtorAndArgs}
     * and an associated args array for {@link #getForContext(ConstructionContext)} invocation in
     * cases such as this (where the returned {@link CtorAndArgs} is not constant across indices).
     * Recycling is optional, and is not guaranteed to occur.
     *
     * @param ctorAndArgs the {@link CtorAndArgs} instance to recycle
     */
    @Override
    public void recycle(final CtorAndArgs<T> ctorAndArgs) {
        // Only recycle ctorAndArgs if ctorAndArgs is compatible with our state:
        if (ctorAndArgs.getArgs().length != 1) {
            return;
        }

        cachedConstructorAndArgs.lazySet(ctorAndArgs);
    }
}
