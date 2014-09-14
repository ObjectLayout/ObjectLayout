/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import org.junit.Test;

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

    @Test(expected = IllegalStateException.class)
    public void shouldFailToAccessFakeIntrinsic() throws NoSuchMethodException {
        Line line = new Line();
        Point endPoint1 = line.getEndPoint1();
        Point endPoint2 = line.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));

        IntrinsicObject<Point> badIntrinsic = line.getEndPoint1Intrinsic();
        Line line2 = new Line(line, badIntrinsic);
        endPoint1 = line2.getEndPoint1();
        endPoint2 = line2.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToAccessFakeIntrinsic2() throws NoSuchMethodException {
        Line line = new Line();
        Point endPoint1 = line.getEndPoint1();
        Point endPoint2 = line.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));

        IntrinsicObject<Point> badIntrinsic = line.getEndPoint2Intrinsic();
        Line line2 = new Line(line, badIntrinsic);
        endPoint1 = line2.getEndPoint1();
        endPoint2 = line2.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToAccessFakeIntrinsic3() throws NoSuchMethodException {
        Line line = new Line();
        IntrinsicObject<Point> badInstrinsic = IntrinsicObject.construct(line, "endPoint1", Point.class);
        badInstrinsic.get();
    }

    /**
     * BadContainerNonPrivate: non private {@link org.ObjectLayout.IntrinsicObject} member
     */
    static class BadContainerNonPrivate {
        final IntrinsicObject<Point> intrinsicPoint =
                IntrinsicObject.construct(this, "intrinsicPoint", Point.class);

        Point getPoint() {
            return intrinsicPoint.get();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadContainerNonPrivate() throws NoSuchMethodException {
        BadContainerNonPrivate bad = new BadContainerNonPrivate();
        bad.getPoint();
    }

    /**
     * BadContainerNonFinal: non final {@link org.ObjectLayout.IntrinsicObject} member
     */
    static class BadContainerNonFinal {
        private IntrinsicObject<Point> intrinsicPoint =
                IntrinsicObject.construct(this, "intrinsicPoint", Point.class);

        Point getPoint() {
            return intrinsicPoint.get();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadContainerNonFinal() throws NoSuchMethodException {
        BadContainerNonFinal bad = new BadContainerNonFinal();
        bad.getPoint();
    }

    /**
     * BadContainerNullMember: null {@link org.ObjectLayout.IntrinsicObject} member
     */
    static class BadContainerNullMember {
        private final IntrinsicObject<Point> intrinsicPoint = null;

        Point getPoint() {
            return intrinsicPoint.get();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testBadContainerNullMember() throws NoSuchMethodException {
        BadContainerNullMember bad = new BadContainerNullMember();
        bad.getPoint();
    }

    /**
     * BadContainerOtherMemberIsNull: Badly initialized but not accessed member (should fail on get()).
     */
    static class BadContainerOtherMemberIsNull {
        private final IntrinsicObject<Point> intrinsicPoint1 =
                IntrinsicObject.construct(this, "intrinsicPoint1", Point.class);
        private final IntrinsicObject<Point> intrinsicPoint2 = null;

        Point getPoint() {
            return intrinsicPoint1.get();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testBadContainerOtherMemberIsNull() throws NoSuchMethodException {
        BadContainerOtherMemberIsNull bad = new BadContainerOtherMemberIsNull();
        bad.getPoint();
    }

    /**
     * BadContainerCrossAssignment: Badly initialized but not accessed member (should fail on get()).
     */
    static class BadContainerCrossAssignment {
        private final IntrinsicObject<Point> intrinsicPoint1 =
                IntrinsicObject.construct(this, "intrinsicPoint1", Point.class);
        private final IntrinsicObject<Point> intrinsicPoint2 = intrinsicPoint1;

        Point getPoint() {
            return intrinsicPoint1.get();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testBadContainerCrossAssignment() throws NoSuchMethodException {
        BadContainerCrossAssignment bad = new BadContainerCrossAssignment();
        bad.getPoint();
    }

    /**
     * BadContainerPrematureAccess: get() before all fields are initialized.
     */
    static class BadContainerPrematureAccess {
        private final IntrinsicObject<Point> intrinsicPoint1 =
                IntrinsicObject.construct(this, "intrinsicPoint1", Point.class);
        private final IntrinsicObject<Point> intrinsicPoint2;

        BadContainerPrematureAccess() {
            // The get() is premature because intrinsicPoint2 was not yet initialized:
            intrinsicPoint1.get().set(0, 0);
            intrinsicPoint2 =
                    IntrinsicObject.construct(this, "intrinsicPoint1", Point.class);
        }

        Point getPoint() {
            return intrinsicPoint1.get();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testBadContainerPrematureAccess() throws NoSuchMethodException {
        BadContainerPrematureAccess bad = new BadContainerPrematureAccess();
        bad.getPoint();
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

    /**
     * A simple Line class example with two intrinsic Point end point objects.
     *
     * This class demonstrates
     *
     */
    public static class Line {

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

        /**
         * A bad Line() constructor, initializes an {@link org.ObjectLayout.IntrinsicObject} member to
         * with the value of another, already initialized {@link org.ObjectLayout.IntrinsicObject} parameter.
         *
         * @param source
         * @param iop
         */
        public Line(final Line source, IntrinsicObject<Point> iop) {
            this.endPoint2 = iop;
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

        /**
         * "Bad practice" accessor: should not expose intrinsic to others. Used here to assist in creating
         * cross-instance intrinsic initialization for tests, in order to test error condition detection.
         * @return
         */
        public IntrinsicObject<Point> getEndPoint1Intrinsic() {
            return endPoint1;
        }

        /**
         * "Bad practice" accessor: should not expose intrinsic to others. Used here to assist in creating
         * cross-instance intrinsic initialization for tests, in order to test error condition detection.
         * @return
         */
        public IntrinsicObject<Point> getEndPoint2Intrinsic() {
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
