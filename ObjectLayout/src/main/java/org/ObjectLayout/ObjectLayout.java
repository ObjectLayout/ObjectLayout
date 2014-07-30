/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;

/**
 * Support the declaration of object instances that are intrinsic to other containing objects.
 * <p>
 * Intrinsic object instances are instances of java Objects (or any derived constructable class)
 * that are instantiated as part of the initialization of a containing object instance. Where
 * possible, a JVM runtime may lay out the contents of the containing object instance such that
 * the objects intrinsic to it are contiguous in memory, providing potentially optimized access
 * to the contained object. Specifically, accessing an intrinsified object through a containing
 * object may be "dead-reckoned" and incur no reference-following costs.
 * <p>
 * To support optimized layout on capable JVMs, intrinsic objects must be declared final, and
 * must be instantiated using one of the {@link #createIntrinsicObject} methods. For example:
 * <pre>
 * <code>
 * class SomeClass {
 *     int a;
 *     final SomeOtherClass o = IntrinsifiedObjects.createIntrinsicObject(SomeOtherClass.class);
 *     ...
 * }
 * </code>
 *
 */
public abstract class ObjectLayout {
    private static final Class[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * Instantiate an object of the given objectClass using it's default constructor. If called to
     * initialize a final field in a containing object instance, JVMs capable of optimized object
     * layout for intrinsic objects may choose to lay out the containing object contents in memory
     * and in a way that would eliminate or minimize reference-following costs.
     *
     * @param objectClass
     * @param <IntrinsicType>
     * @return An instance of objectClass constructed using a default constructor
     */
    public static <IntrinsicType> IntrinsicType createIntrinsicObject(final Class<IntrinsicType> objectClass) {
        return createIntrinsicObject(objectClass, EMPTY_ARG_TYPES, EMPTY_ARGS);
    }

    /**
     * Instantiate an object of the given objectClass using the constructor indicated by constructorArgTypes
     * and the arguments in constructorArgs. If called to initialize a final field in a
     * containing object instance, JVMs capable of optimized object layout for intrinsic objects
     * may choose to lay out the containing object contents in memory and in a way that would
     * eliminate or minimize reference-following costs.
     *
     * @param objectClass
     * @param constructorArgTypes
     * @param constructorArgs
     * @param <IntrinsicType>
     * @return An instance of objectClass constructed with constructorArgs arguments to a matching constructor
     */
    public static <IntrinsicType> IntrinsicType createIntrinsicObject(final Class<IntrinsicType> objectClass,
                                                                      final Class[] constructorArgTypes,
                                                                      final Object... constructorArgs) {
        try {
            final Constructor<IntrinsicType> constructor = objectClass.getConstructor(constructorArgTypes);
            return constructor.newInstance(constructorArgs);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Instantiate an object of the given objectClass using the constructor and arguments indicated by
     * objectCtorAndArgs. If called to initialize a final field in a containing object instance, JVMs
     * capable of optimized object layout for intrinsic objects may choose to lay out the containing
     * object contents in memory and in a way that would eliminate or minimize reference-following costs.
     *
     * @param objectCtorAndArgs
     * @param <IntrinsicType>
     * @return An object instance constructed with the given constructor and args.
     */
    public static <IntrinsicType> IntrinsicType createIntrinsicObject(
            final CtorAndArgs<IntrinsicType> objectCtorAndArgs) {
        try {
            return objectCtorAndArgs.getConstructor().newInstance(objectCtorAndArgs.getArgs());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private ObjectLayout() {}
}
