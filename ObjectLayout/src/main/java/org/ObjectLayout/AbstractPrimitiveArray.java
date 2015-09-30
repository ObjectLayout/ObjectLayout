/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.invoke.MethodHandles;
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

abstract class AbstractPrimitiveArray {

    static final MethodHandles.Lookup noLookup = null;

    private final long length;

    static <A extends AbstractPrimitiveArray> A _newInstance(
            MethodHandles.Lookup lookup,
            final Class<A> arrayClass,
            final long length) {
        try {
            return instantiate(lookup, length, arrayClass.getDeclaredConstructor(), (Object[]) null);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    static <A extends AbstractPrimitiveArray> A _newInstance(
            final CtorAndArgs<A> arrayCtorAndArgs,
            final long length) {
        return instantiate(length, arrayCtorAndArgs.getConstructor(), arrayCtorAndArgs.getArgs());
    }

    static <A extends AbstractPrimitiveArray> A _newInstance(
            final long length,
            final Constructor<A> arrayConstructor,
            final Object... arrayConstructorArgs) {
        return instantiate(length, arrayConstructor, arrayConstructorArgs);
    }

    static <A extends AbstractPrimitiveArray> A _copyInstance(
            MethodHandles.Lookup lookup,
            A source) throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        final Class<A> sourceArrayClass = (Class<A>) source.getClass();
        Constructor<A> arrayConstructor = sourceArrayClass.getDeclaredConstructor(sourceArrayClass);
        return instantiate(lookup, source._getLength(), arrayConstructor, source);
    }

    /**
     * create a fresh PrimitiveArray intended to occupy a a given intrinsic field in the containing object,
     * at the field described by the supplied intrinsicObjectModel, using the supplied constructor and arguments.
     */
    static <A extends AbstractPrimitiveArray> A constructPrimitiveArrayWithin(
            MethodHandles.Lookup lookup,
            final Object containingObject,
            final AbstractIntrinsicObjectModel<A> intrinsicObjectModel,
            final long length,
            final Constructor<A> arrayConstructor,
            final Object... arrayConstructorArgs) {
        A array = instantiate(lookup, length, arrayConstructor, arrayConstructorArgs);
        intrinsicObjectModel.directlyInitializeTargetField(containingObject, array);
        return array;
    }

    private static <A extends AbstractPrimitiveArray> A instantiate(
            MethodHandles.Lookup lookup,
            final long length,
            final Constructor<A> arrayConstructor,
            final Object... arrayConstructorArgs) {
        try {
            if (lookup != null) {
                if (!CtorAndArgs.belongsToThisPackage(arrayConstructor.getDeclaringClass())) {
                    lookup.unreflectConstructor(arrayConstructor); // May throw IllegalAccessException
                    arrayConstructor.setAccessible(true);
                }
            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        return instantiate(length, arrayConstructor, arrayConstructorArgs);
    }

    private static <A extends AbstractPrimitiveArray> A instantiate(
            final long length,
            final Constructor<A> arrayConstructor,
            final Object... arrayConstructorArgs) {
        try {
            preInstantiation(length, arrayConstructor, arrayConstructorArgs);
            return arrayConstructor.newInstance(arrayConstructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } finally {
            postInstantiation();
        }
    }

    static <A extends AbstractPrimitiveArray> void preInstantiation(
            final long length,
            final Constructor<A> arrayConstructor,
            final Object... arrayConstructorArgs) {
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setArrayConstructorArgs(length);
        constructorMagic.setActive(true);
    }

    static void postInstantiation() {
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setActive(false);
    }

    AbstractPrimitiveArray() {
        checkConstructorMagic();
        ConstructorMagic constructorMagic = getConstructorMagic();
        length = constructorMagic.getLength();
        constructorMagic.setActive(false);
    }

    AbstractPrimitiveArray(AbstractPrimitiveArray source) {
        this();
    }

    final long _getLength() {
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

        private void setArrayConstructorArgs(final long length) {
            this.length = length;
        }

        private long getLength() {
            return length;
        }

        private boolean active = false;
        private long length = 0;
    }

    private static final ThreadLocal<ConstructorMagic> threadLocalConstructorMagic = new ThreadLocal<>();

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

    final Object createIntAddressableElements(Class componentClass) {
        long length = _getLength();
        // Size int-addressable sub arrays:
        final int intLength = (int) Math.min(length, Integer.MAX_VALUE);
        return Array.newInstance(componentClass, intLength);
    }

    final Object createLongAddressableElements(Class componentClass) {
        long length = _getLength();
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
