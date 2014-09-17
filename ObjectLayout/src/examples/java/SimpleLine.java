import org.ObjectLayout.IntrinsicObjectModel;

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
 * one of the {@link org.ObjectLayout.IntrinsicObjectModel#constructWithin} variants</li>
 * <li>Intrinsic object members cannot be initialized to null</li>
 * <li>Intrinsic object members cannot be initialized with the value of another intrinsic object member</li>
 * <li>Intrinsic object members must be constructed with their containing object as a parameter</li>
 * <li>No intrinsic object member can be accessed until all intrinsic object members in the containing
 * object instance are correctly initialized, and {@link org.ObjectLayout.IntrinsicObjectModel#makeIntrinsicObjectsAccessible}
 * has been successfully called on the containing object. Attempts at earlier access may/will results
 * in NPEs.</li>
 * </ul>
 * Attempts to construct members with the wrong containing object, or by initializing
 * them to a value of an already initialized field, or by initializing them directly by assigning them
 * to a value received from a constructor or factory other than the
 * {@link org.ObjectLayout.IntrinsicObjectModel#constructWithin} variants, will lead to a failure to
 * successfully execute {@link org.ObjectLayout.IntrinsicObjectModel#makeIntrinsicObjectsAccessible}, and
 * to a failure to construct the containing object if it's constructor calls
 * {@link org.ObjectLayout.IntrinsicObjectModel#makeIntrinsicObjectsAccessible} (which it should), or to a failure to
 * access intrinsic object members if {@link org.ObjectLayout.IntrinsicObjectModel#makeIntrinsicObjectsAccessible} is not
 * called.
 * <p>
 *
 * This class demonstrates
 *
 */
public class SimpleLine {
    /**
     * Model declaration of two intrinsic object fields:
     */
    private static final IntrinsicObjectModel<Point> endPoint1Model =
            new IntrinsicObjectModel<Point>(SimpleLine.class, "endPoint1", Point.class);

    private static final IntrinsicObjectModel<Point> endPoint2Model =
            new IntrinsicObjectModel<Point>(SimpleLine.class, "endPoint2", Point.class);

    /**
     * Simple intrinsic object declaration and initialization:
     */
    private final Point endPoint1 = endPoint1Model.constructWithin(this);
    private final Point endPoint2 = endPoint2Model.constructWithin(this);

    /**
     * Use of makeIntrinsicObjectsAccessible in initializer (useful if no constructors exist, for example):
     */
    {
        IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
    }

    public SimpleLine() {
        this(0, 0, 0, 0);
    }

    public SimpleLine(final long x1, final long y1, long x2, long y2) {
        this.endPoint1.set(x1, y1);
        this.endPoint1.set(x2, y2);
    }

    public SimpleLine(final SimpleLine source) {
        this(source.endPoint1.getX(), source.endPoint1.getY(),
                source.endPoint2.getX(), source.endPoint2.getY());
    }

    public Point getEndPoint1() {
        return endPoint1;
    }

    public Point getEndPoint2() {
        return endPoint2;
    }
}
