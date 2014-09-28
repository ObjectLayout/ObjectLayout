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

    @Test(expected = NullPointerException.class)
    public void shouldFailToAccessFakeIntrinsic() throws NoSuchMethodException {
        Line line = new Line();
        Point endPoint1 = line.getEndPoint1();
        Point endPoint2 = line.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));

        Point badPoint = new Point(0, 0);
        Line line2 = new Line(line, badPoint);
        endPoint1 = line2.getEndPoint1();
        endPoint2 = line2.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToAccessFakeIntrinsic2() throws NoSuchMethodException {
        Line line = new Line();
        IntrinsicObjectModel<Point> badIntrinsicModel =
                new IntrinsicObjectModel<Point>("endPoint1"){};
        Point badIntrinsic = badIntrinsicModel.constructWithin(line);
        badIntrinsic.getX();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToAccessFakeIntrinsic3() throws NoSuchMethodException {
        Line line = new Line();
        IntrinsicObjectModel<Point> badIntrinsicModel = line.getEndPoint1Model();
        Point badIntrinsic = badIntrinsicModel.constructWithin(line);
        badIntrinsic.getX();
    }

    /**
     * BadContainerSuperPoint: Wrong size init for intrinsic object member
     */
    static class BadContainerSuperPoint {
        private static final IntrinsicObjectModel<Point> intrinsicPointModel =
                new IntrinsicObjectModel<Point>("intrinsicPoint"){};

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
        private final Point intrinsicPoint =
                intrinsicPointModel.constructWithin(this, (Constructor<Point>) o, (Object[]) null);

        BadContainerSuperPoint() {
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

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
     * BadContainerNonPrivate: non private intrinsic object member
     */
    static class BadContainerNonPrivate {
        private static final IntrinsicObjectModel<Point> intrinsicPointModel =
                new IntrinsicObjectModel<Point>("intrinsicPoint"){};

        final Point intrinsicPoint = intrinsicPointModel.constructWithin(this);

        BadContainerNonPrivate() {
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void testBadContainerNonPrivate() throws NoSuchMethodException {
        BadContainerNonPrivate bad = new BadContainerNonPrivate();
        bad.getPoint().getX();
    }

    /**
     * BadContainerNonFinal: non final intrinsic object member
     */
    static class BadContainerNonFinal {
        private static final IntrinsicObjectModel<Point> intrinsicPointModel =
                new IntrinsicObjectModel<Point>("intrinsicPoint"){};

        private Point intrinsicPoint = intrinsicPointModel.constructWithin(this);

        BadContainerNonFinal() {
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void testBadContainerNonFinal() throws NoSuchMethodException {
        BadContainerNonFinal bad = new BadContainerNonFinal();
        bad.getPoint().getX();
    }

    /**
     * BadContainerNullMember: null intrinsic object member
     */
    static class BadContainerNullMember {
        private static final IntrinsicObjectModel<Point> intrinsicPointModel =
                new IntrinsicObjectModel<Point>("intrinsicPoint"){};

        private final Point intrinsicPoint = null;

        BadContainerNullMember() {
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testBadContainerNullMember() throws NoSuchMethodException {
        BadContainerNullMember bad = new BadContainerNullMember();
        bad.getPoint().getX();
    }

    /**
     * BadContainerOtherMemberIsNull: Badly initialized but not accessed member (should fail).
     */
    static class BadContainerOtherMemberIsNull {
        private static final IntrinsicObjectModel<Point> intrinsicPoint1Model =
                new IntrinsicObjectModel<Point>("intrinsicPoint1"){};
        private static final IntrinsicObjectModel<Point> intrinsicPoint2Model =
                new IntrinsicObjectModel<Point>("intrinsicPoint2"){};

        private final Point intrinsicPoint1 = intrinsicPoint1Model.constructWithin(this);
        private final Point intrinsicPoint2 = null;

        BadContainerOtherMemberIsNull() {
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

        Point getPoint() {
            return intrinsicPoint1;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testBadContainerOtherMemberIsNull() throws NoSuchMethodException {
        BadContainerOtherMemberIsNull bad = new BadContainerOtherMemberIsNull();
        bad.getPoint().getX();
    }

    /**
     * BadContainerCrossAssignment: Badly initialized but not accessed member (should fail).
     */
    static class BadContainerCrossAssignment {
        private static final IntrinsicObjectModel<Point> intrinsicPoint1Model =
                new IntrinsicObjectModel<Point>("intrinsicPoint1"){};
        private static final IntrinsicObjectModel<Point> intrinsicPoint2Model =
                new IntrinsicObjectModel<Point>("intrinsicPoint2"){};

        private final Point intrinsicPoint1 = intrinsicPoint1Model.constructWithin(this);
        private final Point intrinsicPoint2 = intrinsicPoint1;

        BadContainerCrossAssignment() {
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

        Point getPoint() {
            return intrinsicPoint1;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testBadContainerCrossAssignment() throws NoSuchMethodException {
        BadContainerCrossAssignment bad = new BadContainerCrossAssignment();
        bad.getPoint().getX();
    }

    /**
     * BadContainerFieldName: Badly initialized member (using field name that doesn't exist).
     */
    static class BadContainerFieldName {
        private static final IntrinsicObjectModel<Point> intrinsicPointModel =
                new IntrinsicObjectModel<Point>("someObject"){};

        private final Point intrinsicPoint = intrinsicPointModel.constructWithin(this);

        BadContainerFieldName() {
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void testBadContainerFieldName() throws NoSuchMethodException {
        BadContainerFieldName bad = new BadContainerFieldName();
        bad.getPoint().getX();
    }


    /**
     * BadContainerFieldName: Badly initialized member (using field name that is not assignable from object).
     */
    static class BadContainerFieldType {
        private static final IntrinsicObjectModel<Point> intrinsicPointModel =
                new IntrinsicObjectModel<Point>("someObject"){};

        private final Point intrinsicPoint = intrinsicPointModel.constructWithin(this);
        private final long someObject = 0;

        BadContainerFieldType() {
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

        Point getPoint() {
            return intrinsicPoint;
        }
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void testBadContainerFieldType() throws NoSuchMethodException {
        BadContainerFieldType bad = new BadContainerFieldType();
        bad.getPoint().getX();
    }

    /**
     * BadContainerPrematureAccess: accessing one initialized field before all fields are initialized.
     */
    static class BadContainerPrematureAccess {
        private static final IntrinsicObjectModel<Point> intrinsicPoint1Model =
                new IntrinsicObjectModel<Point>("intrinsicPoint1"){};
        private static final IntrinsicObjectModel<Point> intrinsicPoint2Model =
                new IntrinsicObjectModel<Point>("intrinsicPoint2"){};

        private final Point intrinsicPoint1 = intrinsicPoint1Model.constructWithin(this);
        private final Point intrinsicPoint2;

        BadContainerPrematureAccess() {
            // The access is premature because intrinsicPoint2 was not yet initialized.
            // Should get NPE on intrinsicPoint1:
            intrinsicPoint1.set(0, 0);
            intrinsicPoint2 = intrinsicPoint2Model.constructWithin(this);
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
        }

        Point getPoint() {
            return intrinsicPoint1;
        }
    }

    @Test(expected = NullPointerException.class)
    public void testBadContainerPrematureAccess() throws NoSuchMethodException {
        BadContainerPrematureAccess bad = new BadContainerPrematureAccess();
        bad.getPoint().getX();
    }

    /**
     * BadContainerPrematureMakeAccessible: making fields accessible before all fields are initialized.
     */
    static class BadContainerPrematureAccessible {
        private static final IntrinsicObjectModel<Point> intrinsicPoint1Model =
                new IntrinsicObjectModel<Point>("intrinsicPoint1"){};
        private static final IntrinsicObjectModel<Point> intrinsicPoint2Model =
                new IntrinsicObjectModel<Point>("intrinsicPoint2"){};

        private final Point intrinsicPoint1 = intrinsicPoint1Model.constructWithin(this);
        private final Point intrinsicPoint2;

        BadContainerPrematureAccessible() {
            // This makeIntrinsicObjectsAccessible() is premature because intrinsicPoint2 is
            // not yet initialized. Should fail with exception:
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);
            intrinsicPoint1.set(0, 0);
            intrinsicPoint2 = intrinsicPoint2Model.constructWithin(this);
        }

        Point getPoint() {
            return intrinsicPoint1;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testBadContainerPrematureAccessible() throws NoSuchMethodException {
        BadContainerPrematureAccessible bad = new BadContainerPrematureAccessible();
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
        private static final IntrinsicObjectModel<Point> endPoint1Model =
                new IntrinsicObjectModel<Point>("endPoint1"){};

        private static final IntrinsicObjectModel<Point> endPoint2Model =
                new IntrinsicObjectModel<Point>("endPoint2"){};

        /**
         * Simple Intrinsic Object declaration and initialization:
         */
        private final Point endPoint1 = endPoint1Model.constructWithin(this);

        /**
         * Declaration of an Intrinsic Object that will be initialized later during construction or other init code:
         */
        private final Point endPoint2;

        public Line() {
            this(0, 0, 0, 0);
        }

        public Line(final long x1, final long y1, long x2, long y2) {
            /**
             * Construction-time Initialization of IntrinsicObject:
             */
            this.endPoint2 = endPoint2Model.constructWithin(this, xy_constructor, x2, y2);

            /**
             * Must make intrinsic object fields accessible before accessing them. Otherwise access
             * attempts will generate NPEs. Should usually be done within constructor:
             */
            IntrinsicObjectModel.makeIntrinsicObjectsAccessible(this);

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

        public IntrinsicObjectModel<Point> getEndPoint1Model() { return endPoint1Model; }

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
