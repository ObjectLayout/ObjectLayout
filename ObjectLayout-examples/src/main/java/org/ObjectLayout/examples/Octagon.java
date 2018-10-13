package org.ObjectLayout.examples;/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */


import org.ObjectLayout.Intrinsic;
import org.ObjectLayout.IntrinsicObjects;

import java.lang.invoke.MethodHandles;

/**
 * A simple Octagon class example with an intrinsic StructuredArray of Points. Demonstrates use of
 * {@link org.ObjectLayout.IntrinsicObjectModel} members and their initialization, when the
 * intrinsic member is a StructuredArray.
 *
 */
public class Octagon {

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    /**
     * Intrinsic object declaration and initialization for a StructuredArray member:
     */
    @Intrinsic(length = 8)
    private final PointArray points = IntrinsicObjects.constructWithin(lookup, "points", this);

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
            double angleRad = i * (2.0 * Math.PI) / 8.0;
            long x = centerX + (long)(Math.cos(angleRad) * radius);
            long y = centerY + (long)(Math.sin(angleRad) * radius);
            points.get(i).setX(x);
            points.get(i).setY(y);
        }
    }

    public PointArray getPoints() {
        return points;
    }

    public String getColor() {
        return color;
    }
}
