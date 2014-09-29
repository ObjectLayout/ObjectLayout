package org.ObjectLayout;

import java.lang.Object;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.ObjectLayout.intrinsifiable.AbstractIntrinsicObjectModel;

/**
 * An {@link IntrinsicObjectModel} declares a specific instance field of it's containing class to be intrinsic
 * to the instances of that containing class. Intrinsic objects may have their layout within the containing
 * object instance optimized by JDK implementations, such that access to their content is faster, and avoids
 * certain de-referencing steps.
 * <p>
 * An example of declaring an intrinsic object is:
 * <p><blockquote><pre>
 * public class Line {
 *     //
 *     // Model declaration of an intrinsic object fields:
 *     //
 *     private static final IntrinsicObjectModel&ltPoint&gt endPoint1Model =
 *         new IntrinsicObjectModel&ltPoint&gt("endPoint"){};
 *     ...
 *     //
 *     // Simple intrinsic object declaration and initialization:
 *     //
 *     private final Point endPoint = endPointModel.constructWithin(this);
 *     ...
 *
 *     // Later, in a constructor or instance initializer:
 *     { IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this); }
 * }
 * </pre></blockquote></p>
 * <p>
 * The declaration of an {@link IntrinsicObjectModel} is strongly tied to the class containing the intrinsic
 * object, and to the field declaring the intrinsic object within the containing class. Each
 * {@link IntrinsicObjectModel} instance is specific to a single intrinsic object field in a single class.
 * The intrinsic object field must be a non-static, private, and final field of the containing class, and
 * it's declared type must match the generic type of the {@link IntrinsicObjectModel}.
 * <p>
 * Each {@link IntrinsicObjectModel} instance must be declared as a static final instance of a nested class
 * instance within the class that will contain the intrinsic object. It must be declared with a generic
 * type that matches the intrinsic object's type, and the field name of the intrinsic object must be passed
 * as a parameter to the {@link IntrinsicObjectModel} constructor.
 * <p>
 * Intrinsic object instances are not accessible until
 * {@link IntrinsicObjectModel#makeIntrinsicObjectsAccessible(Object) makeIntrinsicObjectsAccessible()}
 * has been called on the containing object instance. Attempts to access intrinsic object members of a
 * containing object instance that has not had it's fields made accessible with
 * {@link IntrinsicObjectModel#makeIntrinsicObjectsAccessible(Object) makeIntrinsicObjectsAccessible()}
 * can (and likely will) result in {@link java.lang.NullPointerException} exceptions.
 *
 *
 * @param <T> The type of the intrinsic object
 */
public abstract class IntrinsicObjectModel<T> extends AbstractIntrinsicObjectModel<T> {

    private IntrinsicObjectModel(
            final String fieldNameInContainingObject,
            final PrimitiveArrayModel primitiveArrayModel,
            final StructuredArrayModel structuredArrayModel) {
        super(
                fieldNameInContainingObject,
                primitiveArrayModel,
                structuredArrayModel
        );
    }

    /**
     * Construct an {@link org.ObjectLayout.IntrinsicObjectModel} that models and can be used to construct
     * an intrinsic object within this class's enclosing class, at the given fieldNameInContainingObject.
     *
     * @param fieldNameInContainingObject The name of this {@link org.ObjectLayout.IntrinsicObjectModel}'s field in
     *                                    the containing object
     */
    public IntrinsicObjectModel(final String fieldNameInContainingObject) {
        this (fieldNameInContainingObject, null, null);
    }

    /**
     * Construct an {@link org.ObjectLayout.IntrinsicObjectModel} that models and can be used to construct
     * an intrinsic object of type {@link org.ObjectLayout.intrinsifiable.PrimitiveArray} within this
     * class's enclosing class, at the given fieldNameInContainingObject.
     *
     * @param fieldNameInContainingObject The name of this {@link org.ObjectLayout.IntrinsicObjectModel}'s field in
     *                                    the containing object
     * @param arrayModel The model of the array, including the array type and it's length.
     */
    public IntrinsicObjectModel(
            final String fieldNameInContainingObject,
            final PrimitiveArrayModel arrayModel) {
        this (fieldNameInContainingObject, arrayModel, null);
    }

    /**
     * Construct an {@link org.ObjectLayout.IntrinsicObjectModel} that models and can be used to construct
     * an intrinsic object of type {@link org.ObjectLayout.intrinsifiable.PrimitiveArray} within this
     * class's enclosing class, at the given fieldNameInContainingObject.
     *
     * @param fieldNameInContainingObject The name of this {@link org.ObjectLayout.IntrinsicObjectModel}'s field in
     *                                    the containing object
     * @param arrayModel The model of the array, including the array type, and it's length, the type of the
     *                   elements in the array, and any potential sub-array nesting models.
     */
    public IntrinsicObjectModel(
            final String fieldNameInContainingObject,
            final StructuredArrayModel arrayModel) {
        this (fieldNameInContainingObject, null, arrayModel);
    }

    /**
     * Construct an intrinsic object within the containing object, using a default constructor.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @return A reference to the the newly constructed intrinsic object
     */
    public final T constructWithin(final Object containingObject) {
        try {
            return instantiate(
                    containingObject,
                    getObjectClass().getConstructor(),
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
    public final T constructWithin(
            final Object containingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        if (getObjectClass() != objectConstructor.getDeclaringClass()) {
            throw new IllegalArgumentException(
                    "The declaring class of the constructor (" +
                            objectConstructor.getDeclaringClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            getObjectClass().getName() +
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
    public final T constructWithin(
            final Object containingObject,
            final CtorAndArgs<T> objectCtorAndArgs) {
        if (getObjectClass() != objectCtorAndArgs.getConstructor().getDeclaringClass()) {
            throw new IllegalArgumentException(
                    "The declaring class of the constructor (" +
                            objectCtorAndArgs.getConstructor().getDeclaringClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            getObjectClass().getName() +
                            ")");
        }
        return instantiate(
                containingObject,
                objectCtorAndArgs.getConstructor(),
                objectCtorAndArgs.getArgs()
        );
    }

    /**
     * Construct an intrinsic object within the containing object, using the supplied StructuredArrayBuilder.
     * This form of constructWithin() can only be used to construct intrinsic objects that derive from
     * StructuredArray.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param arrayBuilder The StructuredArrayBuilder instance to be used in constructing the array
     * @return A reference to the the newly constructed intrinsic object
     */
    public final T constructWithin(
            final Object containingObject,
            final StructuredArrayBuilder arrayBuilder) {
        if (!_isStructuredArray()) {
            throw new IllegalArgumentException(
                    "The StructuredArrayBuilder argument cannot be used on IntrinsicObjectModel" +
                            " that do not model StructuredArray intrinsic objects"
            );
        }
        if (getObjectClass() != arrayBuilder.getArrayModel().getArrayClass()) {
            throw new IllegalArgumentException(
                    "The class in the array builder (" +
                            arrayBuilder.getArrayModel().getArrayClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            getObjectClass().getName() +
                            ")");
        }
        if (!arrayBuilder.getArrayModel().equals(_getStructuredArrayModel())) {
            throw new IllegalArgumentException(
                    "The array model in supplied array builder does not match the array model used" +
                            " to create this IntrinsicObjectModel instance."
            );
        }
        return instantiate(containingObject, arrayBuilder);
    }

    private T instantiate(
            final Object containingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        try {
            _makeApplicable();
            _sanityCheckInstantiation(containingObject);

            if (_isPrimitiveArray()) {
                constructPrimitiveArrayWithin(containingObject, objectConstructor, args);
            } else if (_isStructuredArray()) {
                @SuppressWarnings("unchecked")
                StructuredArrayBuilder arrayBuilder =
                        new StructuredArrayBuilder((StructuredArrayModel)_getStructuredArrayModel()).
                                arrayCtorAndArgs(objectConstructor, args).resolve();
                StructuredArray.constructStructuredArrayWithin(containingObject, this, arrayBuilder);
            } else {
                constructElementWithin(containingObject, objectConstructor, args);
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

    private T instantiate(
            final Object containingObject,
            final StructuredArrayBuilder arrayBuilder) {
        try {
            _makeApplicable();
            _sanityCheckInstantiation(containingObject);
            StructuredArray.constructStructuredArrayWithin(containingObject, this, arrayBuilder);
            return null;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    public final void makeApplicable() {
        super._makeApplicable();
    }

    public static void makeModelsApplicable(Class containingClass) {
        AbstractIntrinsicObjectModel._makeModelsApplicable(containingClass);
    }

    public static void makeIntrinsicObjectsAccessible(final Object containingObject) {
        AbstractIntrinsicObjectModel._makeIntrinsicObjectsAccessible(containingObject);
    }
}
