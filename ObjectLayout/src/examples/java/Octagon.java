/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.IntrinsicObjectModel;
import org.ObjectLayout.StructuredArray;
import org.ObjectLayout.StructuredArrayModel;

import java.lang.reflect.Constructor;

/**
 * A simple Octagon class example with an intrinsic StructuredArray of Points. Demonstrates use of
 * {@link org.ObjectLayout.IntrinsicObjectModel} members and their initialization, when the
 * intrinsic member is a StructuredArray.
 *
 */
public class Octagon {
    /**
     * Model declaration of intrinsic object field:
     */
    private static final IntrinsicObjectModel<StructuredArrayOfPoint> pointsModel =
            new IntrinsicObjectModel<StructuredArrayOfPoint>(
                    Octagon.class,
                    "points",
                    new StructuredArrayModel<StructuredArrayOfPoint, Point>(
                            StructuredArrayOfPoint.class,
                            Point.class,
                            8)
            );

    /**
     * Simple intrinsic object declaration and initialization:
     */
    private final StructuredArrayOfPoint points = pointsModel.constructWithin(this);

    {
        IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
    }

    private final String color;

    public Octagon() {
        this("blank");
    }

    public Octagon(final String color) {
        this.color = color;
    }

    public Octagon(final Octagon source) {
        this.color = source.getColor();
        for (int i = 0; i < points.getLength(); i++) {
            points.get(i).setX(source.getPoints().get(i).getX());
            points.get(i).setY(source.getPoints().get(i).getY());
        }
    }

    public StructuredArrayOfPoint getPoints() {
        return points;
    }

    public String getColor() {
        return color;
    }
}
