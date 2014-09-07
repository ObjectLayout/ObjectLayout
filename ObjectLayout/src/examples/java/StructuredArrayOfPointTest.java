/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.*;
import org.junit.Test;

import java.awt.*;

import static java.lang.Long.compare;
import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StructuredArrayOfPointTest {

    @Test
    public void shouldConstructArrayOfGivenDirectLengths() throws NoSuchMethodException {
        final long[] lengths = {7L, 8L, 9L};

        @SuppressWarnings("unchecked")
        final StructuredArray<StructuredArray<StructuredArrayOfPoint>> array =
                new StructuredArrayBuilder(
                        StructuredArray.class,
                        new StructuredArrayBuilder(
                                StructuredArray.class,
                                new StructuredArrayBuilder<StructuredArrayOfPoint, Point>(
                                        StructuredArrayOfPoint.class,
                                        Point.class,
                                        lengths[2]
                                ),
                                lengths[1]
                        ),
                        lengths[0]
                ).build();

        assertThat(valueOf(array.getLength()), is(lengths[0]));
        assertThat(valueOf(array.get(0).getLength()), is(lengths[1]));
        assertThat(valueOf(array.get(0).get(0).getLength()), is(lengths[2]));

        assertTrue(array.getElementClass().isAssignableFrom(StructuredArray.class));
        assertTrue(array.get(0).getElementClass().isAssignableFrom(StructuredArrayOfPoint.class));
        assertTrue(array.get(0).get(0).getElementClass() == Point.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthWithNewInstance() throws NoSuchMethodException {
        long length = 9L;
        StructuredArrayOfPoint array = StructuredArrayOfPoint.newInstance(length);

        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == Point.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthWithBuilder() throws NoSuchMethodException {
        long length = 9L;
        StructuredArrayOfPoint array =
                new StructuredArrayBuilder<StructuredArrayOfPoint, Point>(
                        StructuredArrayOfPoint.class,
                        Point.class,
                        length
                ).build();

        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == Point.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthAndInitValues() throws NoSuchMethodException {
        final int initialX = 4;
        final int initialY = 777;

        long length = 9L;

        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(length, initialX, initialY);

        assertCorrectFixedInitialisation(initialX, initialY, new long[] {length}, array);
    }

    @Test
    public void shouldConstructArrayElementsViaConstantCtorAndArgsProvider() throws NoSuchMethodException {
        final Class[] initArgTypes = {int.class, int.class};
        final int initialX = 4;
        final int initialY = 777;

        long length = 9L;

        final CtorAndArgsProvider<Point> ctorAndArgsProvider =
                new ConstantCtorAndArgsProvider<Point>(Point.class, initArgTypes, initialX, initialY);
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(
                        StructuredArrayOfPoint.class, Point.class, ctorAndArgsProvider, length);

        assertCorrectFixedInitialisation(initialX, initialY, new long[] {length}, array);
    }

    @Test
    public void shouldConstructArrayElementsViaCtorAndArgsProvider() throws NoSuchMethodException {
        final long[] lengths = {9};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(ctorAndArgsProvider, lengths[0]);

        assertCorrectVariableInitialisation(lengths, array);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructArrayElementsViaCtorAndArgsProvider3D() throws NoSuchMethodException {
        final long[] lengths = {7, 8, 9};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArrayOfPoint>>,
                StructuredArray<StructuredArrayOfPoint>> builder = get3dBuilder(lengths);
        builder.getStructuredSubArrayBuilder().
                getStructuredSubArrayBuilder().
                elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<StructuredArrayOfPoint>> array = builder.build();

        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldSetAndGetCorrectValueAtGivenIndex() throws NoSuchMethodException {
        final long[] lengths = {11, 10, 3};
        final StructuredArray<StructuredArray<StructuredArrayOfPoint>> array = get3dBuilder(lengths).build();

        initValues(lengths, array);
        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldIterateOverArray() throws NoSuchMethodException {
        final long[] lengths = {11};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths[0]);

        initValues(lengths, array);

        StructuredArrayOfPoint.ElementIterator iter = array.iterator();

        long sum = 0;
        long elementCount = 0;
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final Point point = iter.next();

            assertThat(valueOf(point.x), is(valueOf(index)));
            assertThat(valueOf(point.y), is(valueOf(index * 2)));
            sum += index;
            elementCount++;
        }

        long sum2 = 0;
        long elementCount2 = 0;
        for (final Point point : array) {
            sum2 += point.x;
            elementCount2++;
        }

        assertThat(valueOf(elementCount), is(valueOf(array.getLength())));
        assertThat(valueOf(sum), is(valueOf(sum2)));
        assertThat(valueOf(elementCount), is(valueOf(elementCount2)));
    }

    @Test
    public void shouldIterateOverArrayAndResetAgain() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(length);

        initValues(new long[] {length}, array);

        int i = 0;
        final StructuredArrayOfPoint.ElementIterator iter = array.iterator();
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final Point point = iter.next();
            assertThat(valueOf(point.x), is(valueOf(index)));
            assertThat(valueOf(point.y), is(valueOf(index * 2)));
            i++;
        }

        iter.reset();
        i = 0;
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final Point point = iter.next();
            assertThat(valueOf(point.x), is(valueOf(index)));
            assertThat(valueOf(point.y), is(valueOf(index * 2)));
            i++;
        }

        assertThat(valueOf(i), is(valueOf(length)));
    }

    @Test
    public void shouldConstructCopyOfArray() throws NoSuchMethodException {
        final long length = 15;
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();
        final StructuredArrayOfPoint sourceArray =
                StructuredArrayOfPoint.newInstance(ctorAndArgsProvider, length);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(length)));
        assertTrue(sourceArray.getElementClass() == Point.class);

        final StructuredArrayOfPoint newArray =
                (StructuredArrayOfPoint) StructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(new long[] {length}, newArray);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructCopyOfArray3D() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArrayOfPoint>>,
                StructuredArray<StructuredArrayOfPoint>> builder = get3dBuilder(lengths);
        builder.getStructuredSubArrayBuilder().
                getStructuredSubArrayBuilder().
                elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<StructuredArrayOfPoint>> sourceArray = builder.build();

        StructuredArray<StructuredArrayOfPoint> subArray1 = sourceArray.get(0);
        StructuredArrayOfPoint subArray2 = subArray1.get(0);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(subArray1.getLength()), is(valueOf(lengths[1])));
        assertThat(valueOf(subArray2.getLength()), is(valueOf(lengths[2])));
        assertTrue(subArray2.getElementClass() == Point.class);

        final StructuredArray<StructuredArray<StructuredArrayOfPoint>> newArray =
                StructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(lengths, newArray);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructCopyOfArrayRange() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArrayOfPoint>>,
                StructuredArray<StructuredArrayOfPoint>> builder = get3dBuilder(lengths);
        builder.getStructuredSubArrayBuilder().
                getStructuredSubArrayBuilder().
                elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<StructuredArrayOfPoint>> sourceArray = builder.build();

        StructuredArray<StructuredArrayOfPoint> subArray1 = sourceArray.get(0);
        StructuredArrayOfPoint subArray2 = subArray1.get(0);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(subArray1.getLength()), is(valueOf(lengths[1])));
        assertThat(valueOf(subArray2.getLength()), is(valueOf(lengths[2])));
        assertTrue(subArray2.getElementClass() == Point.class);

        long[] offsets = {2, 2, 2};
        long[] counts = {13, 5, 3};
        final StructuredArray<StructuredArray<StructuredArrayOfPoint>> newArray =
                StructuredArray.copyInstance(sourceArray, offsets, counts);

        assertCorrectVariableInitialisation(counts, newArray, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowOutOfBoundExceptionForAccessesOutOfBounds() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(length);

        array.get(length);
    }

    @Test
    public void shouldNotThrowIncompatibleTypeExceptionForGetsOfProperTypes() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArray<StructuredArray<StructuredArrayOfPoint>> array = get3dBuilder(lengths).build();

        // Step by step gets of the correct type (array vs. element) per dimension:
        StructuredArray<StructuredArrayOfPoint> subArray1 = array.get(2);
        StructuredArrayOfPoint subArray2 = subArray1.get(2);
        subArray2.get(2);
    }

    @Test
    public void shouldCopyRegionLeftInArray() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(length);

        initValues(new long[]{length}, array);

        StructuredArray.shallowCopy(array, 4, array, 3, 2, false);

        assertThat(valueOf(array.get(3).x), is(valueOf(4)));
        assertThat(valueOf(array.get(4).x), is(valueOf(5)));
        assertThat(valueOf(array.get(5).x), is(valueOf(5)));
    }

    @Test
    public void shouldCopyRegionRightInArray() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(length);

        initValues(new long[]{length}, array);

        StructuredArray.shallowCopy(array, 5, array, 6, 2, false);

        assertThat(valueOf(array.get(5).x), is(valueOf(5)));
        assertThat(valueOf(array.get(6).x), is(valueOf(5)));
        assertThat(valueOf(array.get(7).x), is(valueOf(6)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToConstructDirectly() throws NoSuchMethodException {
        final StructuredArrayOfPoint array = new StructuredArrayOfPoint();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Test support below
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void assertCorrectFixedInitialisation(final int expectedX, final int expectedY, final long[] lengths,
                                                  final StructuredArray array) {
        StructuredArray a = array;
        for (int i = 0; i < lengths.length - 1; i++) {
            assertThat(valueOf(a.getLength()), is(valueOf(lengths[i])));
            a = (StructuredArray) a.get(0);
        }
        StructuredArrayOfPoint ap = (StructuredArrayOfPoint) a;
        assertThat(valueOf(ap.getLength()), is(valueOf(lengths[lengths.length - 1])));

        assertTrue(ap.getElementClass() == Point.class);

        long totalElementCount = 1;
        for (long l : lengths) {
            totalElementCount *= l;
        }

        final long[] cursors = new long[lengths.length];

        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            a = array;
            for (int i = 0; i < cursors.length - 1; i++) {
                a = (StructuredArray) a.get(cursors[i]);
            }
            ap = (StructuredArrayOfPoint) a;
            Point point = ap.get(cursors[cursors.length - 1]);

            assertThat(valueOf(point.x), is(valueOf(expectedX)));
            assertThat(valueOf(point.y), is(valueOf(expectedY)));

            // Increment cursors from inner-most dimension out:
            for (int cursorDimension = cursors.length - 1; cursorDimension >= 0; cursorDimension--) {
                if ((++cursors[cursorDimension]) < lengths[cursorDimension]) {
                    break;
                }
                // This dimension wrapped. Reset to zero and continue to one dimension higher
                cursors[cursorDimension] = 0;
            }
            elementCountToCursor++;
        }
    }

    private void assertCorrectVariableInitialisation(final long[] lengths,
                                                     final StructuredArray array) {
        assertCorrectVariableInitialisation(lengths, array, 0);
    }

    private void assertCorrectVariableInitialisation(final long[] lengths,
                                             final StructuredArray array, long indexOffset) {
        StructuredArray a = array;
        for (int i = 0; i < lengths.length - 1; i++) {
            assertThat(valueOf(a.getLength()), is(valueOf(lengths[i])));
            a = (StructuredArray) a.get(0);
        }
        StructuredArrayOfPoint ap = (StructuredArrayOfPoint) a;
        assertThat(valueOf(ap.getLength()), is(valueOf(lengths[lengths.length - 1])));

        assertTrue(ap.getElementClass() == Point.class);

        long totalElementCount = 1;
        for (long l : lengths) {
            totalElementCount *= l;
        }

        final long[] cursors = new long[lengths.length];

        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            a = array;
            for (int i = 0; i < cursors.length - 1; i++) {
                a = (StructuredArray) a.get(cursors[i]);
            }
            ap = (StructuredArrayOfPoint) a;
            Point point = ap.get(cursors[cursors.length - 1]);

            long indexSum = 0;
            String cursorsString = "";
            for (long index : cursors) {
                indexSum += index + indexOffset;
                cursorsString += index + ",";
            }

            assertThat("elementCountToCursor: " + elementCountToCursor + " cursors: " + cursorsString,
                    valueOf(point.x), is(valueOf(indexSum)));
            assertThat("elementCountToCursor: " + elementCountToCursor + " cursors: " + cursorsString,
                    valueOf(point.y), is(valueOf(indexSum * 2)));

            // Increment cursors from inner-most dimension out:
            for (int cursorDimension = cursors.length - 1; cursorDimension >= 0; cursorDimension--) {
                if ((++cursors[cursorDimension]) < lengths[cursorDimension]) {
                    break;
                }
                // This dimension wrapped. Reset to zero and continue to one dimension higher
                cursors[cursorDimension] = 0;
            }
            elementCountToCursor++;
        }
    }

    private void initValues(final long[] lengths, final StructuredArray array) {
        final long[] cursors = new long[lengths.length];
        long totalElementCount = 1;
        for (long l : lengths) {
            totalElementCount *= l;
        }

        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:

            StructuredArray a = array;
            for (int i = 0; i < cursors.length - 1; i++) {
                a = (StructuredArray) a.get(cursors[i]);
            }
            StructuredArrayOfPoint ap = (StructuredArrayOfPoint) a;
            Point point = ap.get(cursors[cursors.length - 1]);

            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }

            point.setLocation(indexSum, indexSum * 2);

            // Increment cursors from inner-most dimension out:
            for (int cursorDimension = cursors.length - 1; cursorDimension >= 0; cursorDimension--) {
                if ((++cursors[cursorDimension]) < lengths[cursorDimension]) {
                    break;
                }

                // This dimension wrapped. Reset to zero and continue to one dimension higher
                cursors[cursorDimension] = 0;
            }
            elementCountToCursor++;
        }
    }


    private static class PointCtorAndArgsProvider implements CtorAndArgsProvider<Point> {

        private final Class[] argsTypes = {Integer.TYPE, Integer.TYPE};

        @Override
        public CtorAndArgs<Point> getForContext(ConstructionContext<Point> context) throws NoSuchMethodException {
            long indexSum = 0;
            for (ConstructionContext c = context; c != null; c = c.getContainingContext()) {
                indexSum += c.getIndex();
            }
            Object[] args = {(int)indexSum, (int)(indexSum * 2)};
            // We could do this much more efficiently with atomic caching of a single allocated CtorAndArgs,
            // as CopyCtorAndArgsProvider does, but no need to put in the effort in a test...
            return new CtorAndArgs<Point>(Point.class.getConstructor(argsTypes), args);        }
    }


    StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArrayOfPoint>>,
            StructuredArray<StructuredArrayOfPoint>> get3dBuilder(long... lengths) {
        @SuppressWarnings("unchecked")
        StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArrayOfPoint>>,
                StructuredArray<StructuredArrayOfPoint>> builder =
                new StructuredArrayBuilder(
                        StructuredArray.class,
                        new StructuredArrayBuilder(
                                StructuredArray.class,
                                new StructuredArrayBuilder<StructuredArrayOfPoint, Point>(
                                        StructuredArrayOfPoint.class,
                                        Point.class,
                                        lengths[2]
                                ),
                                lengths[1]
                        ),
                        lengths[0]
                );
        return builder;
    }
}
