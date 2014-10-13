package org.ObjectLayout;

import java.lang.reflect.*;
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
    private final String fieldNameInContainingObject;
    private final Class<T> objectClass;
    private final AbstractPrimitiveArrayModel primitiveArrayModel;
    private final AbstractStructuredArrayModel structuredArrayModel;

    private final Field field;

    private boolean applicable = false;

    // Objects that have been constructed but not yet made visible:
    private final ConcurrentHashMap<Object, T> pendingObjects = new ConcurrentHashMap<Object, T>();

    AbstractIntrinsicObjectModel(
            final String fieldNameInContainingObject,
            final AbstractPrimitiveArrayModel primitiveArrayModel,
            final AbstractStructuredArrayModel structuredArrayModel) {

        this.fieldNameInContainingObject = fieldNameInContainingObject;
        this.primitiveArrayModel = primitiveArrayModel;
        this.structuredArrayModel = structuredArrayModel;

        this.containingClass = deriveContainingClass();
        this.objectClass = deriveObjectClass();

        try {
            field = containingClass.getDeclaredField(fieldNameInContainingObject);
            field.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException(ex);
        }

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
                            (model.fieldNameInContainingObject.equals(fieldNameInContainingObject))) {
                        throw new IllegalArgumentException("An IntrinsicObjectModel for the field \"" +
                                fieldNameInContainingObject +
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
                throw new IllegalArgumentException("Intrinsic object field \"" + field.getName() +
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

    final void _makeApplicable() {
        if (applicable) {
            return;
        }

        // Sanity check this model object:
        try {
            // First, verify that a private, static, final field in the containing class refers to this model instance:
            Field myField = null;
            for (Field field : containingClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                        Modifier.isPrivate(field.getModifiers()) &&
                        Modifier.isFinal(field.getModifiers()) &&
                        AbstractIntrinsicObjectModel.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    AbstractIntrinsicObjectModel model = (AbstractIntrinsicObjectModel) field.get(null /* static field */);
                    if (model == this ) {
                        myField = field;
                    }
                }
            }
            if (myField == null) {
                throw new IllegalStateException(
                        "IntrinsicObjectModel instance must be a private, static, final member " +
                                "of the containing object class \"" + containingClass.getName() + "\"");
            }

            // Next, verify that this is the only model instance for this field:
            for (Field field : containingClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                        AbstractIntrinsicObjectModel.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    AbstractIntrinsicObjectModel model = (AbstractIntrinsicObjectModel) field.get(null /* static field */);
                    if ((model != null ) && (model != this) &&
                            (model.fieldNameInContainingObject.equals(fieldNameInContainingObject))) {
                        throw new IllegalArgumentException(
                                "More than one IntrinsicObjectModel instance exists for the field \"" +
                                        fieldNameInContainingObject +
                                        "\" in class \"" +
                                        containingClass.getName() + "\"");
                    }
                }

            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unexpected IllegalAccessException on accessible field: ", ex);
        }

        applicable = true;
    }

    static void _makeModelsApplicable(Class containingClass) {
        try {
            for (Field field : containingClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                        Modifier.isPrivate(field.getModifiers()) &&
                        Modifier.isFinal(field.getModifiers()) &&
                        AbstractIntrinsicObjectModel.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    AbstractIntrinsicObjectModel model = (AbstractIntrinsicObjectModel) field.get(null /* static field */);
                    model._makeApplicable();
                }
            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unexpected IllegalAccessException on accessible field: ", ex);
        }
    }

    private void makeIntrinsicObjectAccessible(final Object containingObject) throws IllegalAccessException {
        if (field.get(containingObject) != null) {
            throw new IllegalStateException(
                    "Bad value for field \"" +
                            fieldNameInContainingObject +
                            "\". Intrinsic object field was initialized without being " +
                            "constructed by IntrinsicObjectModel.constructWithin(). " +
                            "Cannot make any of the intrinsic objects fields accessible."
            );
        }

        T intrinsicObject = pendingObjects.remove(containingObject);
        if (intrinsicObject == null) {
            throw new IllegalStateException(
                    "Missing value for field \"" +
                            fieldNameInContainingObject +
                            "\". No intrinsic object value was constructed by IntrinsicObjectModel.constructWithin(). " +
                            "Cannot make any of the intrinsic objects fields accessible."
            );
        }

        field.set(containingObject, intrinsicObject);
    }

    static void _makeIntrinsicObjectsAccessible(final Object containingObject) {
        Class containingClass = containingObject.getClass();
        if (!containingClass.isInstance(containingObject)) {
            throw new IllegalArgumentException("containingObject is of class " +
                    containingObject.getClass().getName() +
                    ", and is not an instance of " + containingClass.getName());
        }
        boolean successfullyValidated = false;
        try {
            try {
                for (Field field : containingClass.getDeclaredFields()) {
                    if (AbstractIntrinsicObjectModel.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        AbstractIntrinsicObjectModel model = (AbstractIntrinsicObjectModel) field.get(containingObject);
                        model.makeIntrinsicObjectAccessible(containingObject);
                    }
                }
                successfullyValidated = true;
            } finally {
                if (!successfullyValidated) {
                    // There was a failure somewhere, clean up pending objects and null related fields:
                    for (Field field : containingClass.getDeclaredFields()) {
                        if (AbstractIntrinsicObjectModel.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            AbstractIntrinsicObjectModel model = (AbstractIntrinsicObjectModel) field.get(containingObject);
                            model.pendingObjects.remove(containingObject);
                            model.field.set(containingObject, null);
                        }
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "Unexpected field access exception in scanning the containing object", ex);
        }
    }

    private Class<T> deriveObjectClass() {
        @SuppressWarnings("unchecked")
        Class<T> objectClass = (Class<T>) deriveTypeParameter(0);
        return objectClass;
    }

    private Class deriveContainingClass() {
        return this.getClass().getEnclosingClass();
    }


    private Class deriveTypeParameter(int parameterIndex) {
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        return typeToClass(genericSuperclass.getActualTypeArguments()[parameterIndex]);
    }

    private static Class typeToClass(Type t) {
        if (t instanceof Class) {
            return (Class) t;
        } else if (t instanceof ParameterizedType) {
            return (Class) ((ParameterizedType)t).getRawType();
        } else {
            throw new IllegalArgumentException("Must have a declared generic type");
        }
    }
}
