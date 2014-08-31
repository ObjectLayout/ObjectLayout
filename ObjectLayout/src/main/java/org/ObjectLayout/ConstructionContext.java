package org.ObjectLayout;

/**
 * A model for instantiating a structured array.
 */
public class ConstructionContext<T> {
    private long index;
    private StructuredArray<T> array;
    private ConstructionContext containingContext;
    private Object cookie;

    public ConstructionContext(final Object cookie) {
        this.cookie = cookie;
    }

    // Getters (public):

    public long getIndex() {
        return index;
    }

    public StructuredArray<T> getArray() {
        return array;
    }

    public ConstructionContext<StructuredArray<T>> getContainingContext() {
        return containingContext;
    }

    public Object getCookie() {
        return cookie;
    }

    // Setters (package local):

    void setIndex(long index) {
        this.index = index;
    }

    void setArray(StructuredArray<T> array) {
        this.array = array;
    }

    void setContainingContext(ConstructionContext containingContext) {
        this.containingContext = containingContext;
    }

    void setCookie(Object cookie) {
        this.cookie = cookie;
    }
}
