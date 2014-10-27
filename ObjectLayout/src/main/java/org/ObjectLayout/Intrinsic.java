/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * The {@link org.ObjectLayout.Intrinsic @Intrisic} annotation defines a declared object field to be
 * intrinsic to the class it is declared in. {@link org.ObjectLayout.Intrinsic @Intrisic} fields
 * must be declared private and final.
 * <p>
 * All fields annotated with {@link org.ObjectLayout.Intrinsic @Intrisic} MUST be initialized using
 * one of the {@link IntrinsicObjects#constructWithin(String, Object) IntrinsicObjects.constructWithin()}
 * variants.
 * <p>
 * Furthermore, {@link org.ObjectLayout.Intrinsic @Intrisic} fields are not accessible until
 * {@link org.ObjectLayout.IntrinsicObjects#makeIntrinsicObjectsAccessible(Object)
 * IntrinsicObjects.makeIntrinsicObjectsAccessible()}
 * has been called on the containing object instance. Attempts to access
 * {@link org.ObjectLayout.Intrinsic @Intrisic} fields of a
 * containing object instance that has not had it's fields made accessible with
 * {@link org.ObjectLayout.IntrinsicObjects#makeIntrinsicObjectsAccessible(Object)
 * IntrinsicObjects.makeIntrinsicObjectsAccessible()}
 * may (and likely will) result in {@link NullPointerException} exceptions.
 * <p>
 * An example of declaring intrinsic objects is:
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
 *
 *
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Intrinsic {
    /**
     * The length applied to the Intrinsic array object annotated with this annotation (optional, and
     * applicable only when annotating fields that are declared as {@link StructuredArray},
     * {@link ReferenceArray}} or one of the primitive array variants. E.g. {@link PrimitiveLongArray},
     * {@link PrimitiveDoubleArray}, etc.
     *
     * @return the length of the Intrinsic array objects annotated (StructuredArray, ReferenceArray,
     * and Primitive Array variants)
     */
    long length() default NO_LENGTH;

    /**
     * The class of the elements of the Intrinsic {@link StructuredArray} object annotated with this
     * annotation (optional, and applicable only when annotating {@link StructuredArray} fields, required
     * only when it cannot be automatically derived from generic fields declarations.
     *
     * @return The class of the elements of the Intrinsic StructuredArray object annotated
     */
    Class elementClass() default NO_DECLARED_CLASS.class;

    /**
     * A no-op class used to indicate no value set for the {@link Intrinsic#elementClass()} element.
     */
    static final class NO_DECLARED_CLASS {}
    static final long NO_LENGTH = -1;
}

