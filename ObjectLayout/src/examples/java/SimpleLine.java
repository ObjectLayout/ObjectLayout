/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.IntrinsicObjectModel;

/**
 * A simple Line class example with two intrinsic Point end point objects. Demonstrates use of
 * {@link org.ObjectLayout.IntrinsicObjectModel}
 * members and their initialization.
 *
 */
public class SimpleLine {
    /**
     * Model declaration of two intrinsic object fields:
     */
    private static final IntrinsicObjectModel<Point> endPoint1Model =
            new IntrinsicObjectModel<Point>("endPoint1"){};

    private static final IntrinsicObjectModel<Point> endPoint2Model =
            new IntrinsicObjectModel<Point>("endPoint2"){};

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
