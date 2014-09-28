/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * A abstract base class for subclassable primitive and reference arrays.
 *
 * Subclassable arrays are useful for modeling the commonly used "struct with variable array at the end"
 * available in the C family of languages. JDKs may optimize the layout of a PrimitiveArray such that
 * access to the array members may be faster than regular de-referenced access through a nested array
 * reference.
 */

public abstract class PrimitiveArray extends AbstractArray {

    private final long length;

    public static <A extends PrimitiveArray> A newInstance(
            final Class<A> arrayClass,
            final long length) {
        try {
            return instantiate(length, arrayClass.getDeclaredConstructor(), (Object[]) null);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <A extends PrimitiveArray> A newInstance(
            final long length,
            final Constructor<A> arrayConstructor,
            final Object... arrayConstructorArgs) {
        return instantiate(length, arrayConstructor, arrayConstructorArgs);
    }

    public static <A extends PrimitiveArray> A copyInstance(A source) throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        final Class<A> sourceArrayClass = (Class<A>) source.getClass();
        Constructor<A> arrayConstructor = sourceArrayClass.getDeclaredConstructor(sourceArrayClass);
        return instantiate(source.getLength(), arrayConstructor, source);
    }

    private static <A extends PrimitiveArray> A instantiate(
            final long length,
            final Constructor<A> arrayConstructor,
            final Object... arrayConstructorArgs) {
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setArrayConstructorArgs(length);
        try {
            constructorMagic.setActive(true);
            arrayConstructor.setAccessible(true);
            return arrayConstructor.newInstance(arrayConstructorArgs);
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

    protected PrimitiveArray() {
        checkConstructorMagic();
        ConstructorMagic constructorMagic = getConstructorMagic();
        length = constructorMagic.getLength();
        constructorMagic.setActive(false);
    }

    protected PrimitiveArray(PrimitiveArray source) {
        this();
    }

    public long getLength() {
        return length;
    }

    // ConstructorMagic support:

    private static class ConstructorMagic {
        private boolean isActive() {
            return active;
        }

        private void setActive(final boolean active) {
            this.active = active;
        }

        public void setArrayConstructorArgs(final long length) {
            this.length = length;
        }

        public long getLength() {
            return length;
        }

        private boolean active = false;
        private long length = 0;
    }

    private static final ThreadLocal<ConstructorMagic> threadLocalConstructorMagic = new ThreadLocal<ConstructorMagic>();

    private static ConstructorMagic getConstructorMagic() {
        ConstructorMagic constructorMagic = threadLocalConstructorMagic.get();
        if (constructorMagic == null) {
            constructorMagic = new ConstructorMagic();
            threadLocalConstructorMagic.set(constructorMagic);
        }
        return constructorMagic;
    }

    private static void checkConstructorMagic() {
        final ConstructorMagic constructorMagic = threadLocalConstructorMagic.get();
        if ((constructorMagic == null) || !constructorMagic.isActive()) {
            throw new IllegalArgumentException(
                    "PrimitiveArray must not be directly instantiated with a constructor." +
                            " Use newInstance(...) instead.");
        }
    }

    static final int MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT = 30;
    static final int MAX_EXTRA_PARTITION_SIZE = 1 << MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT;
    static final int PARTITION_MASK = MAX_EXTRA_PARTITION_SIZE - 1;

    Object createIntAddressableElements(Class componentClass) {
        long length = getLength();
        // Size int-addressable sub arrays:
        final int intLength = (int) Math.min(length, Integer.MAX_VALUE);
        return Array.newInstance(componentClass, intLength);
    }

    Object createLongAddressableElements(Class componentClass) {
        long length = getLength();
        // Compute size of int-addressable sub array:
        final int intLength = (int) Math.min(length, Integer.MAX_VALUE);
        // Size Subsequent partitions hold long-addressable-only sub arrays:
        final long extraLength = length - intLength;
        final int numFullPartitions = (int) (extraLength >>> MAX_EXTRA_PARTITION_SIZE_POW2_EXPONENT);
        final int lastPartitionSize = (int) extraLength & PARTITION_MASK;

        Object lastPartition = Array.newInstance(componentClass, lastPartitionSize);
        Class partitionClass = lastPartition.getClass();
        Object[] longAddressableElements = (Object[]) Array.newInstance(partitionClass, numFullPartitions + 1);

        // longAddressableElements = new long[numFullPartitions + 1][];

        // full long-addressable-only partitions:
        for (int i = 0; i < numFullPartitions; i++) {
            longAddressableElements[i] = Array.newInstance(componentClass, MAX_EXTRA_PARTITION_SIZE);
        }

        // Last partition with leftover long-addressable-only size:
        longAddressableElements[numFullPartitions] = lastPartition;

        return longAddressableElements;
    }
}
