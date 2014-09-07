/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;


import org.ObjectLayout.intrinsifiable.AbstractReferenceArray;


/**
 * A subclassable array of object references.
 *
 * ReferenceArray is designed with semantics specifically chosen and restricted such that a "flat" memory
 * layout of the implemented data structure would be possible on optimizing JVMs. While fully functional
 * on all JVM implementation (of Java SE 6 and above), the semantics are such that a JVM may transparently
 * optimise the implementation to provide a compact contiguous layout that facilitates dead-reckoning (as
 * opposed to de-referenced) access to elements
 *
 * @param <T> The reference type
 */

public class ReferenceArray<T> extends AbstractReferenceArray<T> {

    /**
     * Get a reference to the internal array of references.
     *
     * @return The T[] array of references
     */
    public T[] getArray() {
        return _getArray();
    }

    /**
     * Get the reference element at the given index in the array.
     *
     * @param index the index in the array
     * @return the reference element at the given index in the array
     */
    public T get(final int index) {
        return _getArray()[index];
    }

    /**
     * Set reference element at the given index in the array to the given value.
     *
     * @param index the index in the array
     * @param value the value to set the element to
     */
    public void set(final int index, final T value) {
        _getArray()[index] = value;
    }

    /**
     * Create a new instance of ReferenceArray<T> with a given length.
     *
     * @param length the length of the array
     * @param <T> the reference type
     * @return A newly created ReferenceArray<T>
     */
    public static <T> ReferenceArray<T> newInstance(final int length) {
        @SuppressWarnings("unchecked")
        ReferenceArray<T> referenceArray = newInstance(ReferenceArray.class, length);
        return referenceArray;
    }

    /**
     * Default constructor
     */
    public ReferenceArray() {
        super();
    }

    /**
     * Copy constructor
     *
     * @param sourceArray the array to copy
     */
    public ReferenceArray(ReferenceArray<T> sourceArray) {
        super(sourceArray);
    }
}
