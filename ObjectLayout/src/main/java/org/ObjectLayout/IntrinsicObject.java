package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * IntrinsicObject are used to declare, encapsulate, and access member objects that are intrinsic to and
 * are completely contained within other objects. IntrinsicObject semantics are designed such that the
 * declared intrinsic member objects may be (potentially) laid out in memory such that their access
 * can be optimized. In optimizing JVMs, the encapsulated intrinsic object instances may be "flatly" laid out
 * within the containing objects in which they are declared, allowing for "dead reckoning" directly from
 * the containing objects to fields within the intrinsic objects without the use of data dependant
 * de-referencing operations.
 * <p>
 * Note that the following rules apply to {@link org.ObjectLayout.IntrinsicObject} members:
 * <ul>
 * <li>All {@link org.ObjectLayout.IntrinsicObject} members must be private final fields </li>
 * <li>{@link org.ObjectLayout.IntrinsicObject} members must be initialized (either at
 * field declaration, initialization code section, or in the containing object's constructor) using
 * one of the {@link org.ObjectLayout.IntrinsicObject#construct} variants</li>
 * <li>{@link org.ObjectLayout.IntrinsicObject} members cannot be initialized to null</li>
 * <li>{@link org.ObjectLayout.IntrinsicObject} members cannot be initialized with the value of another
 * {@link org.ObjectLayout.IntrinsicObject} member</li>
 * <li>{@link org.ObjectLayout.IntrinsicObject} members must be constructed with their containing object
 * and their field name as parameters</li>
 * <li>No {@link org.ObjectLayout.IntrinsicObject} member can be accessed with
 * {@link org.ObjectLayout.IntrinsicObject#get()} until all {@link org.ObjectLayout.IntrinsicObject} members in
 * declared in the containing class are correctly initialized</li>
 * </ul>
 * <p>
 * Attempts to construct members with the wrong field name or containing object, or by initializing
 * them to a value of an already initialized field will lead to a failure to construct the containing
 * object.
 * <p>
 * Attempts to access {@link org.ObjectLayout.IntrinsicObject} members before all declared
 * {@link org.ObjectLayout.IntrinsicObject} fields in it's containing class have been initialized will
 * throw an {@link java.lang.IllegalStateException} detailing the cause.
 * <p>
 * @param <T> The type of the encapsulated intrinsic object
 */
public final class IntrinsicObject<T>  {
    private final T objectRef;
    private final Object containingObject;
    private final Field fieldInContainingObject;
    private boolean accessible = false;

    /**
     * Construct an {@link org.ObjectLayout.IntrinsicObject} encapsulating an intrinsic object of the given class,
     * using a default constructor.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param fieldNameInContainingObject The name of this {@link org.ObjectLayout.IntrinsicObject}'s field in
     *                                    the containing object
     * @param objectClass The class of the intrinsic object instance being constructed
     * @param <T> The type of the intrinsic object instance being constructed
     * @return An {@link org.ObjectLayout.IntrinsicObject} encapsulating the newly constructed intrinsic object
     */
    public static <T> IntrinsicObject<T> construct(
            final Object containingObject,
            final String fieldNameInContainingObject,
            final Class<T> objectClass) {
        try {
            return instantiate(
                    containingObject, fieldNameInContainingObject, objectClass.getConstructor(), (Object[]) null);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Construct an {@link org.ObjectLayout.IntrinsicObject} encapsulating an intrinsic object of the given class,
     * using the given constructor and arguments.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param fieldNameInContainingObject The name of this {@link org.ObjectLayout.IntrinsicObject}'s field in
     *                                    the containing object
     * @param objectConstructor The constructor to be used in constructing the encapsulated intrinsic object instance
     * @param args the arguments to be used with the objectConstructor
     * @param <T> The type of the intrinsic object instance being constructed
     * @return An {@link org.ObjectLayout.IntrinsicObject} encapsulating the newly constructed intrinsic object
     */
    public static <T> IntrinsicObject<T> construct(
            final Object containingObject,
            final String fieldNameInContainingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        return instantiate(containingObject, fieldNameInContainingObject, objectConstructor, args);
    }

    /**
     * Construct an {@link org.ObjectLayout.IntrinsicObject} encapsulating an intrinsic object of the given class,
     * using the constructor and arguments supplied in the given objectCtorAndArgs argument.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param fieldNameInContainingObject The name of this {@link org.ObjectLayout.IntrinsicObject}'s field in
     *                                    the containing object
     * @param objectCtorAndArgs The constructor and arguments to be used in constructing the encapsulated
     *                          intrinsic object instance
     * @param <T> The type of the intrinsic object instance being constructed
     * @return An {@link org.ObjectLayout.IntrinsicObject} encapsulating the newly constructed intrinsic object
     */
    public static <T> IntrinsicObject<T> construct(
            final Object containingObject,
            final String fieldNameInContainingObject,
            final CtorAndArgs<T> objectCtorAndArgs) {
        return instantiate(
                containingObject,
                fieldNameInContainingObject,
                objectCtorAndArgs.getConstructor(),
                objectCtorAndArgs.getArgs()
        );
    }

    private static <T> IntrinsicObject<T> instantiate(
            final Object containingObject,
            final String fieldNameInContainingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        try {
            final Field fieldInContainingObject =
                    containingObject.getClass().getDeclaredField(fieldNameInContainingObject);
            if (!(Modifier.isFinal(fieldInContainingObject.getModifiers()) &&
                    Modifier.isPrivate(fieldInContainingObject.getModifiers()))) {
                throw new IllegalArgumentException("IntrinsicObject can only be created on private final fields");
            }
            if (fieldInContainingObject.getType() != IntrinsicObject.class) {
                throw new IllegalArgumentException(
                        "IntrinsicObject can only be created on fields that are IntrinsicObjects");
            }
            fieldInContainingObject.setAccessible(true);
            return new IntrinsicObject<T>(
                    objectConstructor.newInstance(args), containingObject, fieldInContainingObject);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    private IntrinsicObject(final T objectRef, final Object containingObject, final Field fieldInContainingObject) {
        this.objectRef = objectRef;
        this.containingObject = containingObject;
        this.fieldInContainingObject = fieldInContainingObject;
        try {
            IntrinsicObject currentFieldValue = (IntrinsicObject) fieldInContainingObject.get(containingObject);
            if (currentFieldValue != null) {
                throw new IllegalArgumentException(
                        "IntrinsicObject can only be created on a currently uninitialized field in the containing object");
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(
                    "IntrinsicObject can only be created on a legally accessible field in the containing object");
        }
    }

    /**
     * Get a reference to the intrinsic member object
     *
     * @return The contained intrinsic object instance
     * @throws IllegalStateException if access is attempted before all {@link org.ObjectLayout.IntrinsicObject}
     * fields in the containing object have been correctly initialized.
     */
    public final T get() throws IllegalStateException {
        if (!accessible) {
            try {
                makeIntrinsicObjectsAccessible(containingObject, containingObject.getClass());
            } catch (IllegalStateException ex) {
                throw new IllegalStateException("get() of field \"" + fieldInContainingObject.getName() +
                        "\" can only be called after all IntrinsicObject fields in the containing object " +
                        "have been initialized correctly.", ex);
            }
        }
        return objectRef;
    }

    /**
     * Make all {@link org.ObjectLayout.IntrinsicObject} members of the containingObject, as declared in the
     * containingObjectClass, accessible via {@link IntrinsicObject#get()}. This method will verify that all declared
     * {@link org.ObjectLayout.IntrinsicObject} members have been properly initialized before making any of them
     * accessible.
     *
     * @param containingObject The object who's {@link org.ObjectLayout.IntrinsicObject} are to be made accessible
     * @param containingObjectClass  The class fields are declared (may be a superclass of the actual object)
     * @param <E> The class of the containing object
     * @throws IllegalStateException if one of several validation rules are not met (see details in this
     * class's documentation above)
     */
    public static <E> void makeIntrinsicObjectsAccessible(
            E containingObject,
            Class<? extends E> containingObjectClass) throws IllegalStateException {
        if (!containingObjectClass.isInstance(containingObject)) {
            throw new IllegalArgumentException("containingObject is of class " +
                    containingObject.getClass().getName() +
                    ", and is not an instance of " + containingObjectClass.getName());
        }
        for (Field field : containingObjectClass.getDeclaredFields()) {
            if (field.getType() == IntrinsicObject.class) {
                validateIntrinsicObject(containingObject, field);
            }
        }
        for (Field field : containingObjectClass.getDeclaredFields()) {
            if (field.getType() == IntrinsicObject.class) {
                makeIntrinsicObjectAccessible(containingObject, field);
            }
        }
    }

    private static <E> void validateIntrinsicObject(E containingObject, Field field) {
        try {
            field.setAccessible(true);
            IntrinsicObject intrinsicObject =
                    (IntrinsicObject) field.get(containingObject);
            if (intrinsicObject == null) {
                throw new IllegalStateException("field \"" +
                        field.getName() +
                        "\" was not initialized");
            }
            IntrinsicObject referredObject =
                    (IntrinsicObject) intrinsicObject.fieldInContainingObject.get(intrinsicObject.containingObject);
            if ((referredObject != intrinsicObject) ||
                    (referredObject.containingObject != containingObject) ||
                    !field.getName().equals(intrinsicObject.fieldInContainingObject.getName())) {
                String errorString = "field \"" +
                        field.getName() +
                        "\" was initialized with information created " +
                        "for a different field (\"" +
                        intrinsicObject.fieldInContainingObject.getName() +
                        "\") ";
                if (referredObject.containingObject != containingObject) {
                    errorString += "of some other object instance.";
                }

                throw new IllegalStateException(errorString);
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "Unexpected field access exception in scanning the containing object");
        }
    }

    private static <E> void makeIntrinsicObjectAccessible(E containingObject, Field field) {
        try {
            field.setAccessible(true);
            IntrinsicObject intrinsicObject =
                    (IntrinsicObject) field.get(containingObject);
            // Presumably already verified. Set accessible:
            intrinsicObject.accessible = true;
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                    "Unexpected field access exception in scanning the containing object");
        }
    }
}
