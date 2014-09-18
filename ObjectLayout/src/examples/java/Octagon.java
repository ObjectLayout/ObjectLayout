/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.IntrinsicObjectModel;
import org.ObjectLayout.StructuredArrayModel;

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
                    "points",
                    new StructuredArrayModel<StructuredArrayOfPoint, Point>(8){}
            ){};

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

    /**
     * Build an perfect octagon with center at (centerX, centerY), and first point
     * at (centerX + radius, centerY)
     *
     * @param color
     * @param centerX
     * @param centerY
     * @param radius
     */
    public Octagon(final String color, final long centerX, final long centerY, final long radius) {
        this.color = color;
        for (int i = 0; i < 8; i++) {
            Double angleRad = i * (2.0 * Math.PI) / 8.0;
            long x = centerX + (long)(Math.cos(angleRad) * radius);
            long y = centerY + (long)(Math.sin(angleRad) * radius);
            points.get(i).setX(x);
            points.get(i).setY(y);
        }
    }

    public StructuredArrayOfPoint getPoints() {
        return points;
    }

    public String getColor() {
        return color;
    }
}
