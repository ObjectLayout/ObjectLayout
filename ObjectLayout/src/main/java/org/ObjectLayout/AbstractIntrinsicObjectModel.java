package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class contains the intrinsifiable portions of IntrinsicObjectModel behavior. JDK implementations
 * that choose to intrinsify IntrinsicObjectModel are expected to replace the implementation of this
 * base class.
 *
 * @param <T> The type of the intrinsic object
 */

abstract class AbstractIntrinsicObjectModel<T>  {
    private final Class containingClass;
    private final Class<T> objectClass;
    private final AbstractPrimitiveArrayModel primitiveArrayModel;
    private final AbstractStructuredArrayModel structuredArrayModel;

    private final Field field;

    // Objects that have been constructed but not yet made visible:
    private final ConcurrentHashMap<Object, T> pendingObjects = new ConcurrentHashMap<Object, T>();

    AbstractIntrinsicObjectModel(
            final Field field,
            final AbstractPrimitiveArrayModel primitiveArrayModel,
            final AbstractStructuredArrayModel structuredArrayModel) {

        @SuppressWarnings("unchecked")
        Class<T> objectClass = (Class<T>) field.getType();

        this.field = field;
        this.containingClass = field.getDeclaringClass();
        this.objectClass = objectClass;
        this.primitiveArrayModel = primitiveArrayModel;
        this.structuredArrayModel = structuredArrayModel;

        field.setAccessible(true);

        sanityCheckAtModelConstruction();
    }

    Class<T> getObjectClass() {
        return objectClass;
    }

    private void sanityCheckAtModelConstruction() {
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
        // Verify that no other model for this field already exists:
        for (Field field : containingClass.getDeclaredFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers()) &&
                        AbstractIntrinsicObjectModel.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    AbstractIntrinsicObjectModel model = (AbstractIntrinsicObjectModel) field.get(null /* static field */);
                    if ((model != null ) &&
                            (model.field.equals(field))) {
                        throw new IllegalArgumentException("An IntrinsicObjectModel for the field \"" +
                                field.getName() +
                                "\" in class \"" +
                                containingClass.getName() + "\" already exists");
                    }
                }
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Unexpected IllegalAccessException on accessible field: ", ex);
            }
        }
    }

    void registerPendingIntrinsicObject(final Object containingObject, final T pendingObject) {
        pendingObjects.put(containingObject, pendingObject);
    }

    /**
     * Construct a fresh element intended to occupy a given intrinsic field in the containing object, using the
     * supplied constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    final void constructElementWithin(
            final Object containingObject,
            final Constructor<T> constructor,
            final Object... args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        T element = constructor.newInstance(args);
        registerPendingIntrinsicObject(containingObject, element);
    }

    /**
     * Construct a fresh primitive sub-array intended to occupy a given intrinsic field in the containing object,
     * at the field described by the supplied intrinsicObjectModel, using the supplied constructor and arguments.
     *
     * OPTIMIZATION NOTE: Optimized JDK implementations may replace this implementation with a
     * construction-in-place call on a previously allocated memory location associated with the given index.
     */
    final void constructPrimitiveArrayWithin(
            final Object containingObject,
            final Constructor<T> constructor,
            final Object... args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        int length = (int) primitiveArrayModel._getLength();
        @SuppressWarnings("unchecked")
        Constructor<? extends AbstractPrimitiveArray> c = (Constructor<? extends AbstractPrimitiveArray>) constructor;
        @SuppressWarnings("unchecked")
        T element = (T) AbstractPrimitiveArray._newInstance(length, c, args);
        registerPendingIntrinsicObject(containingObject, element);
    }

    final void _sanityCheckInstantiation(final Object containingObject) {
        try {
            if ((field.get(containingObject) != null) || (pendingObjects.get(containingObject) != null)) {
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

    private void makeIntrinsicObjectAccessible(final Object containingObject) throws IllegalAccessException {
        if (field.get(containingObject) != null) {
            throw new IllegalStateException(
                    "Bad value for field \"" +
                            field.getName() +
                            "\". Intrinsic object field was initialized without being " +
                            "constructed by IntrinsicObjectModel.constructWithin(). " +
                            "Cannot make any of the intrinsic objects fields accessible."
            );
        }

        T intrinsicObject = pendingObjects.remove(containingObject);
        if (intrinsicObject == null) {
            throw new IllegalStateException(
                    "Missing value for field \"" +
                            field.getName() +
                            "\". No intrinsic object value was constructed by IntrinsicObjectModel.constructWithin(). " +
                            "Cannot make any of the intrinsic objects fields accessible."
            );
        }

        field.set(containingObject, intrinsicObject);
    }

    static void _makeIntrinsicObjectsInCollectionAccessible(
            final Collection<? extends AbstractIntrinsicObjectModel> models,
            final Object containingObject) {
        try{
            boolean successfullyValidated = false;
            try {
                for (AbstractIntrinsicObjectModel model : models) {
                    model.makeIntrinsicObjectAccessible(containingObject);
                }
                successfullyValidated = true;
            } finally {
                if (!successfullyValidated) {
                    // There was a failure somewhere, clean up pending objects and null related fields:
                    for (AbstractIntrinsicObjectModel model : models) {
                        model.pendingObjects.remove(containingObject);
                        model.field.set(containingObject, null);
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "Unexpected field access exception in scanning the containing object", ex);
        }
    }
}
