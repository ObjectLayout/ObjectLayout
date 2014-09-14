/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.IntrinsicObject;

import java.lang.reflect.Constructor;

/**
 * A simple Line class example with two intrinsic Point end point objects. Demonstrates use of
 * {@link org.ObjectLayout.IntrinsicObject}
 * members and their initialization, either at field initialization or during construction.
 *
 * <p>
 * Note that the following rules apply to {@link org.ObjectLayout.IntrinsicObject} members:
 * <ul>
 * <li>All {@link org.ObjectLayout.IntrinsicObject} members must be private final fields. </li>
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
 * Attempts to construct members with the wrong field name or containing object, or by initializing
 * them to a value of an already initialized field will lead to a failure to construct the containing
 * object.
 * <p>
 *
 * This class demonstrates
 *
 */
public class Line {

    /**
     * Simple IntrinsicObject declaration and initialization:
     */
    private final IntrinsicObject<Point> endPoint1 = IntrinsicObject.construct(this, "endPoint1", Point.class);

    /**
     * Declaration of an IntrinsicObject that will be initialized later during construction or other init code:
     */
    private final IntrinsicObject<Point> endPoint2;

    public Line() {
        this(0, 0, 0, 0);
    }

    public Line(final long x1, final long y1, long x2, long y2) {
        /**
         * Construction-time Initialization of IntrinsicObject:
         */
        this.endPoint2 = IntrinsicObject.construct(this, "endPoint2", xy_constructor, x2, y2);

        /**
         * Access to IntrinsicObject within constructor:
         */
        this.endPoint1.get().set(x1, y1);
    }

    public Line(final Line source) {
        this(source.endPoint1.get().getX(), source.endPoint1.get().getY(),
                source.endPoint2.get().getX(), source.endPoint2.get().getY());
    }

    public Point getEndPoint1() {
        return endPoint1.get();
    }

    public Point getEndPoint2() {
        return endPoint2.get();
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
