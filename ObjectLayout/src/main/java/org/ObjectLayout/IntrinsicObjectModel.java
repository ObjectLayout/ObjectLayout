package org.ObjectLayout;

import org.ObjectLayout.intrinsifiable.AbstractArrayModel;
import org.ObjectLayout.intrinsifiable.PrimitiveArray;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @param <T> The type of the encapsulated intrinsic object
 */

public final class IntrinsicObjectModel<T>  {
    private final Class containingClass;
    private final String fieldNameInContainingObject;
    private final Class<T> objectClass;
    private final PrimitiveArrayModel primitiveArrayModel;
    private final StructuredArrayModel structuredArrayModel;

    private final Field field;

    private boolean applicable = false;

    ConcurrentHashMap<Object, T> pendingObjects = new ConcurrentHashMap<Object, T>();

    /**
     * Construct an {@link org.ObjectLayout.IntrinsicObjectModel} that models encapsulating an intrinsic object of
     * the given objectClass within the given containingClass, at the given fieldNameInContainingObject.
     *
     * @param containingClass The class who's instance will contain this intrinsic object
     * @param fieldNameInContainingObject The name of this {@link org.ObjectLayout.IntrinsicObjectModel}'s field in
     *                                    the containing object
     * @param objectClass The class of the intrinsic object instance being constructed
     */
    public IntrinsicObjectModel(
            final Class containingClass,
            final String fieldNameInContainingObject,
            final Class<T> objectClass) {
        this.containingClass = containingClass;
        this.fieldNameInContainingObject = fieldNameInContainingObject;
        this.objectClass = objectClass;
        this.primitiveArrayModel = null;
        this.structuredArrayModel = null;

        try {
            field = containingClass.getDeclaredField(fieldNameInContainingObject);
            field.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException(ex);
        }

        sanityCheckAtConstruction();
    }

    public IntrinsicObjectModel(
            final Class containingClass,
            final String fieldNameInContainingObject,
            final PrimitiveArrayModel arrayModel) {
        this.containingClass = containingClass;
        this.fieldNameInContainingObject = fieldNameInContainingObject;
        @SuppressWarnings("unchecked")
        Class<T> arrayClass = arrayModel.getArrayClass();
        this.objectClass = arrayClass;
        this.primitiveArrayModel = arrayModel;
        this.structuredArrayModel = null;

        try {
            field = containingClass.getDeclaredField(fieldNameInContainingObject);
            field.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException(ex);
        }

        sanityCheckAtConstruction();
    }

    public IntrinsicObjectModel(
            final Class containingClass,
            final String fieldNameInContainingObject,
            final StructuredArrayModel arrayModel) {
        this.containingClass = containingClass;
        this.fieldNameInContainingObject = fieldNameInContainingObject;
        @SuppressWarnings("unchecked")
        Class<T> arrayClass = arrayModel.getArrayClass();
        this.objectClass = arrayClass;
        this.primitiveArrayModel = null;
        this.structuredArrayModel = arrayModel;

        try {
            field = containingClass.getDeclaredField(fieldNameInContainingObject);
            field.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException(ex);
        }

        sanityCheckAtConstruction();
    }

    private void sanityCheckAtConstruction() {
        if (!Object.class.isAssignableFrom(objectClass)) {
            throw new IllegalArgumentException("objectClass (\"" + objectClass.getName() +
                    "\" is not a reference type (must derive from Object)");
        }
        // Verify field is private, final, and an object reference:
        if (!(Modifier.isFinal(field.getModifiers()) &&
                Modifier.isPrivate(field.getModifiers()))) {
            throw new IllegalArgumentException("Intrinsic objects can only be constructed in private final fields");
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
                        field.getType().equals(IntrinsicObjectModel.class)) {
                    field.setAccessible(true);
                    IntrinsicObjectModel model = (IntrinsicObjectModel) field.get(null /* static field */);
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

//    Class getContainingClass() {
//        return containingClass;
//    }
//
//    String getFieldNameInContainingObject() {
//        return fieldNameInContainingObject;
//    }
//
//    Class<T> getObjectClass() {
//        return objectClass;
//    }


    /**
     * Construct an intrinsic object within the containing object, using a default constructor.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @return A reference to the the newly constructed intrinsic object
     */
    public T constructWithin(final Object containingObject) {
        try {
            return instantiate(
                    containingObject,
                    objectClass.getConstructor(),
                    (Object[]) null
            );
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Construct an intrinsic object within the containing object, using the given constructor and arguments.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param objectConstructor The constructor to be used in constructing the intrinsic object instance
     * @param args the arguments to be used with the objectConstructor
     * @return A reference to the the newly constructed intrinsic object
     */
    public T constructWithin(
            final Object containingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        if (objectClass != objectConstructor.getDeclaringClass()) {
            throw new IllegalArgumentException(
                    "The declaring class of the constructor (" +
                            objectConstructor.getDeclaringClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            objectClass.getName() +
                            ")");
        }
        return instantiate(
                containingObject,
                objectConstructor,
                args
        );
    }

    /**
     * Construct an intrinsic object within the containing object, using the constructor and arguments
     * supplied in the given objectCtorAndArgs argument.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param objectCtorAndArgs The constructor and arguments to be used in constructing the
     *                          intrinsic object instance
     * @return A reference to the the newly constructed intrinsic object
     */
    public T constructWithin(
            final Object containingObject,
            final CtorAndArgs<T> objectCtorAndArgs) {
        if (objectClass != objectCtorAndArgs.getConstructor().getDeclaringClass()) {
            throw new IllegalArgumentException(
                    "The declaring class of the constructor (" +
                            objectCtorAndArgs.getConstructor().getDeclaringClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            objectClass.getName() +
                            ")");
        }
        return instantiate(
                containingObject,
                objectCtorAndArgs.getConstructor(),
                objectCtorAndArgs.getArgs()
        );
    }

    private T instantiate(
            final Object containingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        try {
            if (!applicable) {
                makeApplicable();
            }
            if ((field.get(containingObject) != null) || (pendingObjects.get(containingObject) != null)) {
                throw new IllegalArgumentException("Intrinsic object field \"" + field.getName() +
                        "\" in containing object is already initialized");
            }
            if (primitiveArrayModel != null) {
                int length = (int) primitiveArrayModel.getLength(); // Already verified range at instantiation
                @SuppressWarnings("unchecked")
                Constructor<PrimitiveArray> arrayConstructor = (Constructor<PrimitiveArray>) objectConstructor;
                @SuppressWarnings("unchecked")
                T intrinsicObject = (T) PrimitiveArray.newInstance(length, arrayConstructor, args);
                pendingObjects.put(containingObject, intrinsicObject);
            } else if (structuredArrayModel != null) {
                @SuppressWarnings("unchecked")
                Constructor<StructuredArray> arrayConstructor = (Constructor<StructuredArray>) objectConstructor;
                @SuppressWarnings("unchecked")
                T intrinsicObject = (T) new StructuredArrayBuilder(structuredArrayModel).
                        arrayCtorAndArgs(arrayConstructor, args).
                        build();
                pendingObjects.put(containingObject, intrinsicObject);
            } else {
                T intrinsicObject = objectConstructor.newInstance(args);
                pendingObjects.put(containingObject, intrinsicObject);
            }
            return null;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void makeApplicable() {
        // Sanity check this model object:

        try {
            // First, verify that a private, static, final field in the containing class refers to this model instance:
            Field myField = null;
            for (Field field : containingClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                        Modifier.isPrivate(field.getModifiers()) &&
                        Modifier.isFinal(field.getModifiers()) &&
                        field.getType().equals(IntrinsicObjectModel.class)) {
                    field.setAccessible(true);
                    IntrinsicObjectModel model = (IntrinsicObjectModel) field.get(null /* static field */);
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
                        field.getType().equals(IntrinsicObjectModel.class)) {
                    field.setAccessible(true);
                    IntrinsicObjectModel model = (IntrinsicObjectModel) field.get(null /* static field */);
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

    public static void makeModelsApplicable(Class containingClass) {
        try {
            for (Field field : containingClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                        Modifier.isPrivate(field.getModifiers()) &&
                        Modifier.isFinal(field.getModifiers()) &&
                        field.getType().equals(IntrinsicObjectModel.class)) {
                    field.setAccessible(true);
                    IntrinsicObjectModel model = (IntrinsicObjectModel) field.get(null /* static field */);
                    model.makeApplicable();
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

    public static void makeIntrinsicObjectsAccessible(final Object containingObject) {
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
                    if (field.getType() == IntrinsicObjectModel.class) {
                        field.setAccessible(true);
                        IntrinsicObjectModel model = (IntrinsicObjectModel) field.get(containingObject);
                        model.makeIntrinsicObjectAccessible(containingObject);
                    }
                }
                successfullyValidated = true;
            } finally {
                if (!successfullyValidated) {
                    // There was a failure somewhere, clean up pending objects and null related fields:
                    for (Field field : containingClass.getDeclaredFields()) {
                        if (field.getType() == IntrinsicObjectModel.class) {
                            field.setAccessible(true);
                            IntrinsicObjectModel model = (IntrinsicObjectModel) field.get(containingObject);
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
}
