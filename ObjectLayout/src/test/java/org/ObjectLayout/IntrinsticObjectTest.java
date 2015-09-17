/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IntrinsticObjectTest {

    @Test
    public void shouldConstructLine() throws NoSuchMethodException {
        Line line = new Line();
        Point endPoint1 = line.getEndPoint1();
        Point endPoint2 = line.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));

        Line line2 = new Line(1, 2, 3, 4);
        endPoint1 = line2.getEndPoint1();
        endPoint2 = line2.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(1L));
        assertThat(valueOf(endPoint1.getY()), is(2L));
        assertThat(valueOf(endPoint2.getX()), is(3L));
        assertThat(valueOf(endPoint2.getY()), is(4L));

        Line line3 = new Line(line2);
        endPoint1 = line3.getEndPoint1();
        endPoint2 = line3.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(1L));
        assertThat(valueOf(endPoint1.getY()), is(2L));
        assertThat(valueOf(endPoint2.getX()), is(3L));
        assertThat(valueOf(endPoint2.getY()), is(4L));
    }

    @Test
    public void shouldSucceedInAccessFakeIntrinsic() throws NoSuchMethodException {
        Line line = new Line();
        Point endPoint1 = line.getEndPoint1();
        Point endPoint2 = line.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));

        // Make a fake intrinsic Point in the line:
        Point badPoint = new Point(0, 0);
        Line line2 = new Line(line, badPoint);

        // Access should still work (may be slower, but should work):
        endPoint1 = line2.getEndPoint1();
        endPoint2 = line2.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));
    }

    /**
     * BadContainerSuperPoint: Wrong size init for intrinsic object member
     */
    static class BadContainerSuperPoint {
        static final MethodHandles.Lookup lookup = MethodHandles.lookup();

        static final Constructor<SuperPoint> constructor;

        static {
            try {
                constructor = SuperPoint.class.getConstructor();
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }

        Object o = constructor;
        @SuppressWarnings("unchecked")
        @Intrinsic
        private final Point intrinsicPoint =
                IntrinsicObjects.constructWithin(lookup,
                        "intrinsicPoint", this, (Constructor<Point>) o, (Object[]) null);

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadContainerSuperPoint() throws NoSuchMethodException {
        BadContainerSuperPoint bad = new BadContainerSuperPoint();
        bad.getPoint().getX();
    }

    /**
     * BadContainerNonPrivate: non @Intrinsic intrinsic object member construction attempt
     */
    static class BadContainerNonIntrinsic {
        static final MethodHandles.Lookup lookup = MethodHandles.lookup();

        private final Point intrinsicPoint = IntrinsicObjects.constructWithin(lookup, "intrinsicPoint", this);

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadContainerNonIntrinsic() throws NoSuchMethodException {
        BadContainerNonIntrinsic bad = new BadContainerNonIntrinsic();
        bad.getPoint().getX();
    }

    /**
     * BadContainerNonPrivate: non private intrinsic object member
     */
    static class BadContainerNonPrivate {
        static final MethodHandles.Lookup lookup = MethodHandles.lookup();

        @Intrinsic
        final Point intrinsicPoint = IntrinsicObjects.constructWithin(lookup, "intrinsicPoint", this);

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadContainerNonPrivate() throws NoSuchMethodException {
        BadContainerNonPrivate bad = new BadContainerNonPrivate();
        bad.getPoint().getX();
    }

    /**
     * BadContainerNonFinal: non final intrinsic object member
     */
    static class BadContainerNonFinal {
        static final MethodHandles.Lookup lookup = MethodHandles.lookup();

        @Intrinsic
        private Point intrinsicPoint = IntrinsicObjects.constructWithin(lookup, "intrinsicPoint", this);

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadContainerNonFinal() throws NoSuchMethodException {
        BadContainerNonFinal bad = new BadContainerNonFinal();
        bad.getPoint().getX();
    }

    /**
     * BadContainerNullMember: null intrinsic object member
     */
    static class BadContainerNullMember {
        @Intrinsic
        private final Point intrinsicPoint = null;

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = NullPointerException.class)
    public void testBadContainerNullMember() throws NoSuchMethodException {
        BadContainerNullMember bad = new BadContainerNullMember();
        bad.getPoint().getX();
    }

    /**
     * BadContainerOtherMemberIsNull: Badly initialized but not accessed member (should still work, may be slower).
     */
    static class BadContainerOtherMemberIsNull {
        static final MethodHandles.Lookup lookup = MethodHandles.lookup();
        @Intrinsic
        private final Point intrinsicPoint1 = IntrinsicObjects.constructWithin(lookup, "intrinsicPoint1", this);
        @Intrinsic
        private final Point intrinsicPoint2 = null;

        Point getPoint() {
            return intrinsicPoint1;
        }
    }

    @Test
    public void testBadContainerOtherMemberIsNull() throws NoSuchMethodException {
        BadContainerOtherMemberIsNull bad = new BadContainerOtherMemberIsNull();
        bad.getPoint().getX();
    }

    /**
     * BadContainerCrossAssignment: Badly initialized but not accessed member (should still work, may be slower).
     */
    static class BadContainerCrossAssignment {
        static final MethodHandles.Lookup lookup = MethodHandles.lookup();

        @Intrinsic
        private final Point intrinsicPoint1 = IntrinsicObjects.constructWithin(lookup, "intrinsicPoint1", this);
        @Intrinsic
        private final Point intrinsicPoint2 = intrinsicPoint1;

        Point getPoint() {
            return intrinsicPoint1;
        }
    }

    @Test
    public void testBadContainerCrossAssignment() throws NoSuchMethodException {
        BadContainerCrossAssignment bad = new BadContainerCrossAssignment();
        bad.getPoint().getX();
    }

    /**
     * BadContainerFieldName: Badly initialized member (using field name that doesn't exist).
     */
    static class BadContainerFieldName {
        static final MethodHandles.Lookup lookup = MethodHandles.lookup();

        @Intrinsic
        private final Point intrinsicPoint = IntrinsicObjects.constructWithin(lookup, "someObject", this);

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadContainerFieldName() throws NoSuchMethodException {
        BadContainerFieldName bad = new BadContainerFieldName();
        bad.getPoint().getX();
    }


    /**
     * BadContainerFieldName: Badly initialized member (using field name that is not assignable from object).
     */
    static class BadContainerFieldType {
        static final MethodHandles.Lookup lookup = MethodHandles.lookup();

        @Intrinsic
        private final Point intrinsicPoint = IntrinsicObjects.constructWithin(lookup, "someObject", this);
        @Intrinsic
        private final long someObject = 0;

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadContainerFieldType() throws NoSuchMethodException {
        BadContainerFieldType bad = new BadContainerFieldType();
        bad.getPoint().getX();
    }

    /**
     * A simple x,y point class to be used as an element in various other examples.
     *
     */
    public static class Point {
        private long x;
        private long y;

        public Point() {
        }

        public Point(final long x, final long y) {
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

    public static class SuperPoint extends Point {
        private long z;

        public SuperPoint() {
        }

        public SuperPoint(final long x, final long y, final long z) {
            super(x, y);
            this.z = z;
        }

        public SuperPoint(final SuperPoint source) {
            this(source.getX(), source.getY(), source.z);
        }

        public long getZ() {
            return z;
        }

        public void setZ(final long z) {
            this.z = z;
        }
    }

    /**
     * A simple Line class example with two intrinsic Point end point objects.
     *
     * This class demonstrates
     *
     */
    public static class Line {

        static final MethodHandles.Lookup lookup = MethodHandles.lookup();
        /**
         * Simple Intrinsic Object declaration and initialization:
         */
        @Intrinsic
//        private final Point endPoint1 = IntrinsicObjects.constructWithin("endPoint1", this);
        private final Point endPoint1 = IntrinsicObjects.constructWithin(lookup, "endPoint1", this);

        /**
         * Declaration of an Intrinsic Object that will be initialized later during construction or other init code:
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
            this.endPoint2 = IntrinsicObjects.constructWithin(lookup, "endPoint2", this, xy_constructor, x2, y2);

            /**
             * Access to IntrinsicObject within constructor:
             */
            this.endPoint1.set(x1, y1);
        }

        /**
         * A bad Line() constructor, initializes an intrinsic member to
         * with the value of an externally supplied parameter.
         *
         * @param source
         * @param endPoint2
         */
        public Line(final Line source, final Point endPoint2) {
            this.endPoint2 = endPoint2;
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

}
