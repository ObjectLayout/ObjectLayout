package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class IntrinsicObject<T>  {
    private final T objectRef;
    private final Object containingObject;

    static <T> IntrinsicObject<T> newInstance(
            final Object containingObject,
            final Class<T> objectClass) {
        try {
            return instantiate(containingObject, objectClass.getConstructor(), (Object[]) null);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    static <T> IntrinsicObject<T> newInstance(
            final Object containingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        return instantiate(containingObject, objectConstructor, args);
    }

    static <T> IntrinsicObject<T> newInstance(
            final Object containingObject,
            final CtorAndArgs<T> objectCtorAndArgs) {
        return instantiate(containingObject, objectCtorAndArgs.getConstructor(), objectCtorAndArgs.getArgs());
    }

    static <T> IntrinsicObject<T> newStructuredArrayInstance(
            final Object containingObject,
            final CtorAndArgs<T> objectCtorAndArgs) {
        return instantiate(containingObject, objectCtorAndArgs.getConstructor(), objectCtorAndArgs.getArgs());
    }

    private static <T> IntrinsicObject<T> instantiate(
            final Object containingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        try {
            return new IntrinsicObject<T>(objectConstructor.newInstance(args), containingObject);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private IntrinsicObject(final T objectRef, final Object containingObject) {
        this.objectRef = objectRef;
        this.containingObject = containingObject;
    }

    final T getWithin(final Object containingObject) {
        if (this.containingObject != containingObject) {
            throw new IllegalArgumentException("getWith() can only be called within the original containing object");
        }
        return objectRef;
    }
}
