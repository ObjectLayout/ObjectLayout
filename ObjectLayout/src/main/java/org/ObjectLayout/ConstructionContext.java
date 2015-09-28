package org.ObjectLayout;

/**
 * The construction context for object instantiated within a StructuredArray
 *
 *
 */
public class ConstructionContext<T> {
    private long index;
    private AbstractStructuredArray<T> array;
    private ConstructionContext containingContext;
    private Object contextCookie;

    public ConstructionContext(final Object cookie) {
        this.contextCookie = cookie;
    }

    // Getters (public):

    /**
     * Get the index (of the element being constructed) in the immediately containing StructuredArray.
     *
     * @return The index in the immediately containing StructuredArray
     */
    public long getIndex() {
        return index;
    }

    /**
     * Get the immediately containing StructuredArray.
     *
     * @return the immediately containing StructuredArray
     */
    public AbstractStructuredArray<T> getArray() {
        return array;
    }

    /**
     * Get the containing context. The containing context will be non-null if the array this context
     * relates to is nested in another StructuredArray.
     *
     * @return the containing context
     */
    public ConstructionContext<AbstractStructuredArray<T>> getContainingContext() {
        @SuppressWarnings("unchecked")
        ConstructionContext<AbstractStructuredArray<T>> c = containingContext;
        return c;
    }

    /**
     * Get the construction context cookie object.The construction context cookie of the outermost
     * array is taken from the (optionally supplied) StructuredArrayBuilder used in instantiating
     * the array (via {@link org.ObjectLayout.StructuredArrayBuilder#getContextCookie()}). The
     * construction context cookie for nested arrays (if those exist) is taken from the {@link CtorAndArgs}
     * used to construct the nested array, such that a {@link CtorAndArgsProvider} can receive
     * and propagate an appropriate contextCookie. A good example of context cookie in use can be found in the
     * implementation of {@link CopyCtorAndArgsProvider}.
     *
     * @return the construction context cookie
     */
    public Object getContextCookie() {
        return contextCookie;
    }

    // Setters (package local):

    void setIndex(long index) {
        this.index = index;
    }

    void setArray(AbstractStructuredArray<T> array) {
        this.array = array;
    }

    void setContainingContext(ConstructionContext containingContext) {
        this.containingContext = containingContext;
    }

    void setContextCookie(Object contextCookie) {
        this.contextCookie = contextCookie;
    }
}
