/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This class contains the intrinsifiable portions of StructuredArray behavior. JDK implementations
 * that choose to intrinsify StructuredArray are expected to replace the implementation of this
 * base class.
 *
 * @param <T> the element type in the array
 */
abstract class AbstractStructuredArrayBase<T> {

    //
    //
    // Constructor:
    //
    //

    /**
     * Optimization NOTE: Optimized JDK implementations may choose to replace the logic of this
     * constructor, along with the way some of the internal fields are represented (see note
     * in "Internal fields" section farther below.
     */

    AbstractStructuredArrayBase() {
        checkConstructorMagic();
        ConstructorMagic constructorMagic = getConstructorMagic();

        @SuppressWarnings("unchecked")
        final AbstractStructuredArrayModel<AbstractStructuredArrayBase<T>, T> arrayModel =
                constructorMagic.getArrayModel();
        final Class<T> elementClass = arrayModel._getElementClass();
        final long length = arrayModel._getLength();

        // Finish consuming constructMagic arguments:
        constructorMagic.setActive(false);

        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
        }

        this.length = length;
        this.elementClass = elementClass;

        allocateInternalStorage(length);
    }

    /**
     * Instantiate a StructuredArray of arrayClass with the given array model, and the supplied
     * array constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with one that
     * allocates room for the entire StructuredArray and all it's elements.
     */
    static <S extends AbstractStructuredArrayBase<T>, T> S instantiateStructuredArray(
            AbstractStructuredArrayModel<S, T> arrayModel, Constructor<S> arrayConstructor, Object... args) {

        // For implementations that need the array class and the element class,
        // this is how
        // how to get them:
        //
        // Class<? extends StructuredArray<T>> arrayClass =
        // arrayConstructor.getDeclaringClass();
        // Class<T> elementClass = arrayModel.getElementClass();

        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(arrayModel);

        try {
            constructorMagic.setActive(true);
            return arrayConstructor.newInstance(args);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } finally {
            constructorMagic.setActive(false);
        }
    }

    /**
     * Construct a fresh element intended to occupy a given index in the given array, using the
     * supplied constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    void constructElementAtIndex(
            final long index,
            final Constructor<T> constructor,
            final Object... args) {
        try {
            T element = constructor.newInstance(args);
            storeElementInLocalStorageAtIndex(element, index);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Construct a fresh primitive sub-array intended to occupy a given index in the given array, using the
     * supplied constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    void constructPrimitiveSubArrayAtIndex(
            final long index,
            AbstractPrimitiveArrayModel primitiveSubArrayModel,
            final Constructor<T> constructor,
            final Object... args) {
        int length = (int) primitiveSubArrayModel._getLength();
        @SuppressWarnings("unchecked")
        Constructor<? extends AbstractPrimitiveArray> c = (Constructor<? extends AbstractPrimitiveArray>) constructor;
        @SuppressWarnings("unchecked")
        T element = (T) AbstractPrimitiveArray._newInstance(length, c, args);
        storeElementInLocalStorageAtIndex(element, index);
    }

    /**
     * Construct a fresh sub-array intended to occupy a given index in the given array, using the
     * supplied constructor.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    void constructSubArrayAtIndex(
            long index,
            AbstractStructuredArrayModel subArrayModel,
            final Constructor<T> subArrayConstructor,
            final Object... args) {
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(subArrayModel);
        try {
            constructorMagic.setActive(true);
            T subArray = subArrayConstructor.newInstance(args);
            storeElementInLocalStorageAtIndex(subArray, index);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } finally {
            constructorMagic.setActive(false);
        }
    }

    /**
     * Construct a fresh StructuredArray intended to occupy a a given intrinsic field in the containing object,
     * at the field described by the supplied intrinsicObjectModel, using the supplied constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    static <T> T constructStructuredArrayWithin(
            final Object containingObject,
            final AbstractIntrinsicObjectModel<T> intrinsicObjectModel,
            AbstractStructuredArrayModel subArrayModel,
            final Constructor<T> subArrayConstructor,
            final Object... args) {
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setConstructionArgs(subArrayModel);
        try {
            constructorMagic.setActive(true);
            T array = subArrayConstructor.newInstance(args);
            intrinsicObjectModel.directlyInitializeTargetField(containingObject, array);
            return array;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } finally {
            constructorMagic.setActive(false);
        }
    }

    /**
     * Get an element at a supplied [index] in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     * */
    T get(final int index) {
        return intAddressableElements[index];
    }

    /**
     * Get an element at a supplied [index] in a StructuredArray
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * faster access form (e.g. they may be able to derive the element reference directly from the
     * structuredArray reference without requiring a de-reference).
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    T get(final long index) {
        if (index < Integer.MAX_VALUE) {
            return get((int) index);
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex = (index - Integer.MAX_VALUE);
        final int partitionIndex = (int)(longIndex >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int)longIndex & PARTITION_MASK;

        return longAddressableElements[partitionIndex][partitionOffset];
    }

    //
    //
    // Accessor methods for instance state:
    //
    //

    Class<T> getElementClass() {
        return elementClass;
    }

    long getLength() {
        return length;
    }

    //
    //
    // Internal fields:
    //
    //

    /**
     * OPTIMIZATION NOTE: Optimized JDK implementations may choose to hide these fields in non-Java-accessible
     * internal instance fields (much like array.length is hidden), in order to ensure that no uncontrolled
     * modification of the fields can be made by any Java program (not even via reflection getting at private
     * fields and bypassing their finality). This is an important security concern because optimization
     * may need to make strong assumptions about the true finality of some of these fields.
     */

    private final Class<T> elementClass;

    private final long length;

    //
    //
    // Internal Storage support:
    //
    //

    /**
     * OPTIMIZATION NOTE: Optimized JDK implementations may choose to replace this vanilla-Java internal
     * storage representation with one that is more intrinsically understood by the JDK and JVM.
     */

    static final int MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT = 30;
    static final int MAX_EXTRA_PARTITION_SIZE = 1 << MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT;
    static final int PARTITION_MASK = MAX_EXTRA_PARTITION_SIZE - 1;

    private T[][] longAddressableElements; // Used to store elements at indexes above Integer.MAX_VALUE
    private T[] intAddressableElements;


    @SuppressWarnings("unchecked")
    private void allocateInternalStorage(final long length) {
        // Allocate internal storage:

        // Size int-addressable sub arrays:
        final int intLength = (int) Math.min(length, Integer.MAX_VALUE);
        // Size Subsequent partitions hold long-addressable-only sub arrays:
        final long extraLength = length - intLength;
        final int numFullPartitions = (int) (extraLength >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int lastPartitionSize = (int) extraLength & PARTITION_MASK;

        intAddressableElements = (T[]) new Object[intLength];
        longAddressableElements = (T[][]) new Object[numFullPartitions + 1][];
        // full long-addressable-only partitions:
        for (int i = 0; i < numFullPartitions; i++) {
            longAddressableElements[i] = (T[]) new Object[MAX_EXTRA_PARTITION_SIZE];
        }
        // Last partition with leftover long-addressable-only size:
        longAddressableElements[numFullPartitions] = (T[]) new Object[lastPartitionSize];
    }

    private void storeElementInLocalStorageAtIndex(T element, long index0) {
        // place in proper internal storage location:
        if (index0 < Integer.MAX_VALUE) {
            intAddressableElements[(int) index0] = element;
            return;
        }

        // Calculate index into long-addressable-only partitions:
        final long longIndex0 = (index0 - Integer.MAX_VALUE);
        final int partitionIndex = (int) (longIndex0 >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int partitionOffset = (int) longIndex0 & PARTITION_MASK;

        longAddressableElements[partitionIndex][partitionOffset] = element;
    }

    //
    //
    // ConstructorMagic support:
    //
    //

    /**
     * OPTIMIZATION NOTE: The ConstructorMagic will likely not need to be modified in any way even for
     * optimized JDK implementations. It resides in this class for scoping reasons.
     */

    private static class ConstructorMagic {
        private boolean isActive() {
            return active;
        }

        private void setActive(boolean active) {
            this.active = active;
        }

        private void setConstructionArgs(AbstractStructuredArrayModel arrayModel) {
            this.arrayModel = arrayModel;
        }

        private AbstractStructuredArrayModel getArrayModel() {
            return arrayModel;
        }

        private boolean active = false;
        AbstractStructuredArrayModel arrayModel;
    }

    private static final ThreadLocal<ConstructorMagic> threadLocalConstructorMagic =
            new ThreadLocal<ConstructorMagic>() {
                @Override protected ConstructorMagic initialValue() {
                    return new ConstructorMagic();
                }
            };

    private static ConstructorMagic getConstructorMagic() {
        return threadLocalConstructorMagic.get();
    }

    private static void checkConstructorMagic() {
        if (!getConstructorMagic().isActive()) {
            throw new IllegalArgumentException(
                    "StructuredArray<> must not be directly instantiated with a constructor. Use newInstance(...) instead.");
        }
    }
}
