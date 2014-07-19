/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

abstract public class PrimitiveArray {
    private static final Object[] EMPTY_ARGS = new Object[0];

    public static <A extends PrimitiveArray> A newSubclassInstance(final Class<A> arrayClass,
                                                                   final long length) {
        try {
            CtorAndArgs<A> arrayCtorAndArgs = new CtorAndArgs<A>(arrayClass.getConstructor(), EMPTY_ARGS);
            return instantiate(arrayCtorAndArgs, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <A extends PrimitiveArray> A newSubclassInstance(final CtorAndArgs<A> arrayCtorAndArgs,
                                                                   final long length) {
        try {
            return instantiate(arrayCtorAndArgs, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <A extends PrimitiveArray> A newSubclassInstance(final Class<A> arrayClass,
                                                                   final long length,
                                                                   final Class[] arrayConstructorArgTypes,
                                                                   final Object... arrayConstructorArgs) {
        try {
            final Constructor<A> constructor = arrayClass.getConstructor(arrayConstructorArgTypes);
            CtorAndArgs<A> arrayCtorAndArgs = new CtorAndArgs<A>(constructor, arrayConstructorArgs);
            return instantiate(arrayCtorAndArgs, length);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <A extends PrimitiveArray> A instantiate(final CtorAndArgs<A> arrayCtorAndArgs,
                                              final long length) throws NoSuchMethodException {
        ConstructorMagic constructorMagic = getConstructorMagic();
        constructorMagic.setArrayConstructorArgs(arrayCtorAndArgs, length);
        constructorMagic.setActive(true);
        try {
            A array = arrayCtorAndArgs.getConstructor().newInstance(arrayCtorAndArgs.getArgs());
            array.initializePrimitiveArray(length);
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

    PrimitiveArray() {
        length = getConstructorMagic().getLength();
    }

    // Abstract methods:

    abstract void initializePrimitiveArray(long length);

    // ConstructorMagic support:

    private static class ConstructorMagic {
        private boolean isActive() {
            return active;
        }

        private void setActive(final boolean active) {
            this.active = active;
        }

        public void setArrayConstructorArgs(final CtorAndArgs arrayCtorAndArgs,
                                            final long length) {
            this.arrayCtorAndArgs = arrayCtorAndArgs;
            this.length = length;
        }

        public CtorAndArgs getArrayCtorAndArgs() {
            return arrayCtorAndArgs;
        }

        public long getLength() {
            return length;
        }

        private boolean active = false;

        private CtorAndArgs arrayCtorAndArgs = null;
        private long length = 0;
    }

    private static final ThreadLocal<ConstructorMagic> threadLocalConstructorMagic = new ThreadLocal<ConstructorMagic>();
    private final long length;
    
    public long getLength() {
        return length;
    }

    @SuppressWarnings("unchecked")
    private static ConstructorMagic getConstructorMagic() {
        ConstructorMagic constructorMagic = threadLocalConstructorMagic.get();
        if (constructorMagic == null) {
            constructorMagic = new ConstructorMagic();
            threadLocalConstructorMagic.set(constructorMagic);
        }
        return constructorMagic;
    }

    @SuppressWarnings("unchecked")
    private static void checkConstructorMagic() {
        final ConstructorMagic constructorMagic = threadLocalConstructorMagic.get();
        if ((constructorMagic == null) || !constructorMagic.isActive()) {
            throw new IllegalArgumentException("PrimitiveArray must not be directly instantiated with a constructor. Use newInstance(...) instead.");
        }
    }
}
