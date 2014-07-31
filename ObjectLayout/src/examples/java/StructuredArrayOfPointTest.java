/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;
import org.junit.Test;

import java.awt.*;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StructuredArrayOfPointTest {

    @Test
    public void shouldConstructArrayOfGivenDirectLengths() throws NoSuchMethodException {
        final long[] lengths = {7L, 8L, 9L};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(7L, 8L, 9L);

        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(lengths[i]));
        }
        assertTrue(array.getElementClass() == Point.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthsPrimitiveLongArray() throws NoSuchMethodException {
        final long[] lengths = {7L, 8L, 9L};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths);

        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == Point.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthAndInitValues() throws NoSuchMethodException {
        final Class[] initArgTypes = {int.class, int.class};
        final int initialX = 4;
        final int initialY = 777;
        final long[] lengths = {7, 8, 9};

        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths, initArgTypes, initialX, initialY);

        assertCorrectFixedInitialisation(initialX, initialY, lengths, array);
    }

    @Test
    public void shouldConstructArrayElementsViaElementConstructorGenerator() throws NoSuchMethodException {
        final long[] lengths = {7, 8, 9};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(ctorAndArgsProvider, lengths);

        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldSetAndGetCorrectValueAtGivenIndex() throws NoSuchMethodException {
        final long[] lengths = {11, 10, 3};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths);

        initValues(lengths, array);
        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldIterateOverArray() throws NoSuchMethodException {
        final long[] lengths = {11, 8, 3};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths);

        initValues(lengths, array);

        StructuredArrayOfPoint.ElementIterator iter = array.iterator();

        long sum = 0;
        long elementCount = 0;
        while (iter.hasNext()) {
            final long[] cursors = iter.getCursors();
            final Point point = iter.next();
            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }
            assertThat(valueOf(point.x), is(valueOf(indexSum)));
            assertThat(valueOf(point.y), is(valueOf(indexSum * 2)));
            sum += indexSum;
            elementCount++;
        }

        long sum2 = 0;
        long elementCount2 = 0;
        for (final Point point : array) {
            sum2 += point.x;
            elementCount2++;
        }

        assertThat(valueOf(elementCount), is(valueOf(array.getTotalElementCount())));
        assertThat(valueOf(sum), is(valueOf(sum2)));
        assertThat(valueOf(elementCount), is(valueOf(elementCount2)));
    }

    @Test
    public void shouldIterateOverArrayAndResetAgain() throws NoSuchMethodException {
        final long[] lengths = {11, 8, 4};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths);

        initValues(lengths, array);

        int i = 0;
        final StructuredArrayOfPoint.ElementIterator iter = array.iterator();
        while (iter.hasNext()) {
            final long[] cursors = iter.getCursors();
            final Point point = iter.next();
            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }
            assertThat(valueOf(point.x), is(valueOf(indexSum)));
            assertThat(valueOf(point.y), is(valueOf(indexSum * 2)));
            i++;
        }

        iter.reset();
        i = 0;
        while (iter.hasNext()) {
            final long[] cursors = iter.getCursors();
            final Point point = iter.next();
            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }
            assertThat(valueOf(point.x), is(valueOf(indexSum)));
            assertThat(valueOf(point.y), is(valueOf(indexSum * 2)));
            i++;
        }

        assertThat(valueOf(i), is(valueOf(lengths[0] * lengths[1] * lengths[2])));
    }

    @Test
    public void shouldConstructCopyOfArray() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();
        final StructuredArrayOfPoint sourceArray =
                StructuredArrayOfPoint.newInstance(ctorAndArgsProvider, lengths);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(sourceArray.getTotalElementCount()), is(valueOf(lengths[0] * lengths[1] * lengths[2])));
        assertTrue(sourceArray.getElementClass() == Point.class);

        final StructuredArrayOfPoint newArray =
                (StructuredArrayOfPoint) StructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(lengths, newArray);
    }

    @Test
    public void shouldConstructCopyOfArrayRange() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();
        final StructuredArrayOfPoint sourceArray =
                StructuredArrayOfPoint.newInstance(ctorAndArgsProvider, lengths);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(sourceArray.getTotalElementCount()), is(valueOf(lengths[0] * lengths[1] * lengths[2])));
        assertTrue(sourceArray.getElementClass() == Point.class);

        long[] offsets = {1, 1, 1};
        long[] counts = {14, 6, 4};
        final StructuredArrayOfPoint newArray =
                (StructuredArrayOfPoint) StructuredArray.copyInstance(sourceArray, offsets, counts);

        final StructuredArrayOfPoint.ElementIterator iter = newArray.iterator();

        // We expect MockStructure elements to be initialized with index = indexSum, and testValue = indexSum * 2,
        // but with the sums based on index+1 for each cursor (due to the {1, 1, 1} offset above):
        while (iter.hasNext()) {
            final long[] cursors = iter.getCursors();
            final Point point = iter.next();
            long indexSum = 0;
            for (long index : cursors) {
                indexSum += (index + 1);
            }
            assertThat(valueOf(point.x), is(valueOf(indexSum)));
            assertThat(valueOf(point.y), is(valueOf(indexSum*2)));
        }
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowOutOfBoundExceptionForAccessesOutOfBounds() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths);

        array.get(lengths);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIncompatibleTypeExceptionForGetOfAnElementInsteadOfArray() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths);

        // A 2D get in a 3D array is expected to throw an NPE:
        array.get(2, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIncompatibleTypeExceptionForGetOfAnArrayInsteadOfElement() throws NoSuchMethodException {
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(11,7,4);

        StructuredArrayOfPoint subArray1 = array.getSubArray(2);
        StructuredArrayOfPoint subArray2 = subArray1.getSubArray(2);
        subArray2.getSubArray(2);
    }


    @Test
    public void shouldNotThrowIncompatibleTypeExceptionForGetsOfProperTypes() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArrayOfPoint array =
                StructuredArrayOfPoint.newInstance(lengths);

        // Step by step gets of the correct type (array vs. element) per dimension:
        StructuredArrayOfPoint subArray1 = array.getSubArray(2);
        StructuredArrayOfPoint subArray2 = subArray1.getSubArray(2);
        subArray2.get(2);

        // The end result of the above is equivalent to this:
        array.get(2, 2, 2);
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
                                                  final StructuredArrayOfPoint array) {
        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == Point.class);

        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            Point point = array.get(cursors);
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
                                             final StructuredArrayOfPoint array) {
        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == Point.class);

        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            Point point = array.get(cursors);

            long indexSum = 0;
            String cursorsString = "";
            for (long index : cursors) {
                indexSum += index;
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

    private void initValues(final long[] lengths, final StructuredArrayOfPoint array) {
        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            Point point = array.get(cursors);

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


    private static class PointCtorAndArgsProvider extends CtorAndArgsProvider<Point> {

        private final Class[] argsTypes = {Integer.TYPE, Integer.TYPE};

        public CtorAndArgs<Point> getForIndex(long... indices) throws NoSuchMethodException {
            long indexSum = 0;
            for (long index : indices) {
                indexSum += index;
            }
            Object[] args = {(int)indexSum, (int)(indexSum * 2)};
            // We could do this much more efficiently with atomic caching of a single allocated CtorAndArgs,
            // as CopyCtorAndArgsProvider does, but no need to put in the effort in a test...
            return new CtorAndArgs<Point>(Point.class.getConstructor(argsTypes), args);
        }
    }
}
