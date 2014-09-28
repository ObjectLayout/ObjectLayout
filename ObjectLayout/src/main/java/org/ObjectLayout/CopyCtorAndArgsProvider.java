/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

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
class CopyCtorAndArgsProvider<T> extends AbstractCtorAndArgsProvider<T> {

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
        this.copyConstructor = elementClass.getDeclaredConstructor(elementClass);
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
        StructuredArray<T> sourceArray = (StructuredArray<T>) context.getContextCookie();

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
     * Recycle a {@link CtorAndArgs} instance (place it back in the internal cache if
     * it's model is appropriate). This is [very] useful for avoiding a re-allocation of a
     * new {@link CtorAndArgs} and an associated args array for each {@link #getForContext(ConstructionContext)}
     * invocation in cases such as this, where the returned {@link CtorAndArgs} is not constant across indices,
     * but where a cached instance can be mutated to fit the purpose. Recycling is not guaranteed to occur.
     *
     * @param ctorAndArgs the {@link CtorAndArgs} instance to recycle
     */
    @Override
    public void recycle(final CtorAndArgs<T> ctorAndArgs) {
        // Only recycle ctorAndArgs if ctorAndArgs is compatible with our state:
        Object[] args = ctorAndArgs.getArgs();
        if ((ctorAndArgs.getConstructor() != copyConstructor) || (args == null) || (args.length != 1)) {
            return;
        }

        cachedConstructorAndArgs.lazySet(ctorAndArgs);
    }
}
