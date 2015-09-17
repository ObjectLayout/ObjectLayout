package org.ObjectLayout;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

/**
 * This class contains the intrinsifiable portions of IntrinsicObjectModel behavior. JDK implementations
 * that choose to intrinsify IntrinsicObjectModel are expected to replace the implementation of this
 * base class.
 *
 * @param <T> The type of the intrinsic object
 */

abstract class AbstractIntrinsicObjectModel<T>  {
    MethodHandles.Lookup lookup;
    private final Field field;
    private final Class containingClass;
    private final Class<T> objectClass;
    private final AbstractPrimitiveArrayModel primitiveArrayModel;
    private final AbstractStructuredArrayModel structuredArrayModel;

    AbstractIntrinsicObjectModel(
            MethodHandles.Lookup lookup,
            final Field field,
            final AbstractPrimitiveArrayModel primitiveArrayModel,
            final AbstractStructuredArrayModel structuredArrayModel) {

        @SuppressWarnings("unchecked")
        Class<T> objectClass = (Class<T>) field.getType();

        this.lookup = lookup;
        this.field = field;
        this.containingClass = field.getDeclaringClass();
        this.objectClass = objectClass;
        this.primitiveArrayModel = primitiveArrayModel;
        this.structuredArrayModel = structuredArrayModel;


        try {
            // Only set field accessible if the caller actually has access to it:
            // Since we cannot get a setter to a final (not asetAccessible) field with
            // lookup.findSetter() or lookup.unreflectSetter, we derive the caller's
            // access permission to the final field by proving that we can get a getter
            // to it (which would not be possible if it were e.g. private and the caller
            // was not the declaring class). Once we prove to ourselves that the
            // caller is allowed to access the field, we make (our internal copy of)
            // field setAccesible to allow overwriting of final fields...
            MethodHandle getter = lookup.unreflectGetter(field); // May throw IllegalAccessException
            field.setAccessible(true);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }


        sanityCheckAtModelConstruction(lookup);
    }

    Class<T> getObjectClass() {
        return objectClass;
    }

    private void sanityCheckAtModelConstruction(MethodHandles.Lookup lookup) {
        if ((primitiveArrayModel != null) &&
                !primitiveArrayModel._getArrayClass().equals(objectClass)) {
            throw new IllegalArgumentException("Generic object class \"" + objectClass +
                    "\" does not match the array class \"" + primitiveArrayModel._getArrayClass() +
                    "\" in the supplied array model");

        }
        if ((structuredArrayModel != null) &&
                !structuredArrayModel._getArrayClass().equals(objectClass)) {
            throw new IllegalArgumentException("Generic object class \"" + objectClass +
                    "\" does not match the array class \"" + structuredArrayModel._getArrayClass() +
                    "\" in the supplied array model");

        }
        // Verify field is private, final, non-static, and an object reference of the proper type:
        if (!Object.class.isAssignableFrom(objectClass)) {
            throw new IllegalArgumentException("Declared generic object class (\"" + objectClass.getName() +
                    "\" is not a reference type (must derive from Object)");
        }
        if (!(Modifier.isFinal(field.getModifiers()) &&
                Modifier.isPrivate(field.getModifiers()))) {
            throw new IllegalArgumentException("Intrinsic objects can only be declared for private final fields");
        }
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("Intrinsic objects can only be declared for instance fields. " +
                    "Cannot be static");
        }
        if (objectClass != field.getType()) {
            throw new IllegalArgumentException(
                    "The field type \"" + field.getType().getName() + "\" does not match the " +
                            "specified objectClass \"" + objectClass.getName() + "\"");
        }
    }

    /**
     * Construct a fresh element intended to occupy a given intrinsic field in the containing object, using the
     * supplied constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    final T constructElementWithin(
            final Object containingObject,
            final Constructor<T> constructor,
            final Object... args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        T element = constructor.newInstance(args);
        directlyInitializeTargetField(containingObject, element);
        return element;
    }

    /**
     * Construct a fresh primitive sub-array intended to occupy a given intrinsic field in the containing object,
     * at the field described by the supplied intrinsicObjectModel, using the supplied constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    final T constructPrimitiveArrayWithin(
            final Object containingObject,
            final Constructor<T> constructor,
            final Object... args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        int length = (int) primitiveArrayModel._getLength();
        @SuppressWarnings("unchecked")
        Constructor<? extends AbstractPrimitiveArray> c = (Constructor<? extends AbstractPrimitiveArray>) constructor;
        @SuppressWarnings("unchecked")
        T element = (T) AbstractPrimitiveArray._newInstance(length, c, args);
        directlyInitializeTargetField(containingObject, element);
        return element;
    }

    final void _sanityCheckInstantiation(final Object containingObject) {
        try {
            if (field.get(containingObject) != null) {
                throw new IllegalStateException("Intrinsic object field \"" + field.getName() +
                        "\" in containing object is already initialized");
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    final boolean _isPrimitiveArray() {
        return (primitiveArrayModel != null);
    }

    final boolean _isStructuredArray() {
        return (structuredArrayModel != null);
    }

    final AbstractStructuredArrayModel _getStructuredArrayModel() {
        return structuredArrayModel;
    }

    final AbstractPrimitiveArrayModel _getPrimitiveArrayModel() {
        return primitiveArrayModel;
    }

    void directlyInitializeTargetField(final Object containingObject,
                                       T intrinsicObject) {
        try {
            if (field.get(containingObject) != null) {
                throw new IllegalStateException(
                        "Bad value for field \"" +
                                field.getName() +
                                "\". Intrinsic object field was initialized without being " +
                                "constructed by IntrinsicObjectModel.constructWithin(). " +
                                "Cannot make any of the intrinsic objects fields accessible."
                );
            }
            field.set(containingObject, intrinsicObject);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
