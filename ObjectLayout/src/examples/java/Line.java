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
 * <li>Intrinsic object members must be initialized (either at
 * field declaration, initialization code section, or in the containing object's constructor) using
 * one of the {@link org.ObjectLayout.IntrinsicObjects#constructWithin} variants</li>
 * <li>Intrinsic object members cannot be initialized to null</li>
 * <li>Intrinsic object members cannot be initialized with the value of another intrinsic object member</li>
 * <li>Intrinsic object members must be constructed with their containing object as a parameter</li>
 * <li>No intrinsic object member can be accessed until all intrinsic object members in the containing
 * object instance are correctly initialized, and {@link IntrinsicObjects#makeIntrinsicObjectsAccessible}
 * has been successfully called on the containing object. Attempts at earlier access may/will results
 * in NPEs.</li>
 * </ul>
 * Attempts to construct members with the wrong containing object, or by initializing
 * them to a value of an already initialized field, or by initializing them directly by assigning them
 * to a value received from a constructor or factory other than the
 * {@link org.ObjectLayout.IntrinsicObjects#constructWithin} variants, will lead to a failure to
 * successfully execute {@link IntrinsicObjects#makeIntrinsicObjectsAccessible}, and
 * to a failure to construct the containing object if it's constructor calls
 * {@link IntrinsicObjects#makeIntrinsicObjectsAccessible} (which it should), or to a failure to
 * access intrinsic object members if {@link IntrinsicObjects#makeIntrinsicObjectsAccessible} is not
 * called.
 * <p>
 *
 * This class demonstrates
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
         * Must make intrinsic object fields accessible before accessing them. Otherwise access
         * attempts will generate NPEs. Should usually be done within constructor:
         */
        IntrinsicObjects.makeIntrinsicObjectsAccessible(this);

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
            xy_constructor = Point.class.getConstructor(new Class[] {long.class, long.class});
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}
