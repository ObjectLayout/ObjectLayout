/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;


/**
 * The {@link org.ObjectLayout.Intrinsic @Intrisic} annotation defines a declared object field to be
 * intrinsic to the class it is declared in. Intrinsic objects may have their layout within the
 * containing object instance optimized by JDK implementations, such that access to their content is
 * faster, and avoids certain de-referencing steps. {@link org.ObjectLayout.Intrinsic @Intrisic} fields
 * must be declared private and final.
 * <p>
 * Fields annotated with {@link org.ObjectLayout.Intrinsic @Intrisic} SHOULD be initialized using
 * one of the {@link IntrinsicObjects#constructWithin(MethodHandles.Lookup, String, Object)
 * IntrinsicObjects.constructWithin()}
 * variants.
 * <p>
 * An {@link org.ObjectLayout.Intrinsic @Intrisic} field that is NOT initialized using one of
 * the {@link IntrinsicObjects#constructWithin(MethodHandles.Lookup, String, Object)
 * IntrinsicObjects.constructWithin()} variants may not be treated as intrinsic, may result in slower
 * access behaviors, and may generate compile-time warnings.
 *
 * <p>
 * An example of declaring an intrinsic object is:
 * <blockquote><pre>
 * public class Line {
 *     //
 *     // Simple intrinsic object declaration and initialization:
 *     //
 *     {@literal @}Intrinsic
 *     private final Point endPoint1 = IntrinsicObjects.constructWithin("endPoint1", this);
 *     {@literal @}Intrinsic
 *     private final Point endPoint2 = IntrinsicObjects.constructWithin("endPoint2", this);
 *     ...
 * }
 * </pre></blockquote>
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

