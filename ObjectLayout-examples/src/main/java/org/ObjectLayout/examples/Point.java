package org.ObjectLayout.examples;/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * A simple x,y point class to be used as an element in various other examples.
 *
 */
public class Point {
    private long x;
    private long y;

    public Point() {
    }

    public Point (final long x, final long y) {
        this.x = x;
        this.y = y;
    }

    public Point(final Point source) {
        this(source.x, source.y);
    }

    public long getX() {
        return x;
    }

    public void setX(final long x) {
        this.x = x;
    }

    public long getY() {
        return y;
    }

    public void setY(final long y) {
        this.y = y;
    }

    public void set(final long x, final long y) {
        this.x = x;
        this.y = y;
    }
}
