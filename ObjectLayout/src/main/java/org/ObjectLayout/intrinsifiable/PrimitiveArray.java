/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout.intrinsifiable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * A abstract base class for subclassable primitive and reference arrays.
 */

public abstract class PrimitiveArray extends AbstractArray {

    private final int length;

    public static <A extends PrimitiveArray> A newInstance(
            final Class<A> arrayClass,
            final int length) {
        try {
            return instantiate(length, arrayClass.getDeclaredConstructor(), (Object[]) null);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <A extends PrimitiveArray> A newInstance(
            final int length,
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
            final int length,
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

    public int getLength() {
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

        public void setArrayConstructorArgs(final int length) {
            this.length = length;
        }

        public int getLength() {
            return length;
        }

        private boolean active = false;
        private int length = 0;
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
            throw new IllegalArgumentException("PrimitiveArray must not be directly instantiated with a constructor. Use newInstance(...) instead.");
        }
    }
}
