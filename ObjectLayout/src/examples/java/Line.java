/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.Intrinsic;
import org.ObjectLayout.IntrinsicObjects;

import java.lang.reflect.Constructor;

/**
 * A simple Line class example with two intrinsic Point end point objects. Demonstrates use of
 * {@link org.ObjectLayout.IntrinsicObjectModel}
 * members and their initialization, either at field initialization or during construction.
 *
 * <p>
 * Note that the following rules apply to intrinsic object members:
 * <ul>
 * <li>All intrinsic object members must be private final fields. </li>
 * <li>Intrinsic object members should be initialized (either at
 * field declaration, initialization code section, or in the containing object's constructor) using
 * one of the {@link org.ObjectLayout.IntrinsicObjects#constructWithin} variants</li>
 * <li>Intrinsic object members should not be initialized to null</li>
 * <li>Intrinsic object members should not be initialized with the value of another intrinsic object member</li>
 * <li>Intrinsic object members should be constructed with their containing object as a parameter</li>
 * </ul>
 *
 * Failure to follow these rules can result in error, warnings, or simply in slower execution.
 *
 */
public class Line {
    /**
     * Simple intrinsic object declaration and initialization:
     */
    @Intrinsic
    private final Point endPoint1 = IntrinsicObjects.constructWithin("endPoint1", this);

    /**
     * Declaration of an intrinsic object field that will be initialized later during construction or other init code:
     */
    @Intrinsic
    private final Point endPoint2;

    public Line() {
        this(0, 0, 0, 0);
    }

    public Line(final long x1, final long y1, long x2, long y2) {
        /**
         * Construction-time Initialization of IntrinsicObject:
         */
        this.endPoint2 = IntrinsicObjects.constructWithin("endPoint2", this, xy_constructor, x2, y2);

        /**
         * Access to IntrinsicObject within constructor:
         */
        this.endPoint1.set(x1, y1);
    }

    public Line(final Line source) {
        this(source.endPoint1.getX(), source.endPoint1.getY(),
                source.endPoint2.getX(), source.endPoint2.getY());
    }

    public Point getEndPoint1() {
        return endPoint1;
    }

    public Point getEndPoint2() {
        return endPoint2;
    }


    static final Constructor<Point> xy_constructor;

    static {
        try {
            @SuppressWarnings("unchecked")
            Constructor<Point> constructor = Point.class.getConstructor(long.class, long.class);
            xy_constructor = constructor;
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}
