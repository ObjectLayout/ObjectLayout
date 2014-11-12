/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.*;
import org.junit.Test;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PointArrayTest {

    @Test
    public void shouldConstructArrayOfGivenDirectLengths() throws NoSuchMethodException {
        final long[] lengths = {7L, 8L, 9L};

        @SuppressWarnings("unchecked")
        final StructuredArray<StructuredArray<PointArray>> array =
                new StructuredArrayBuilder(
                        StructuredArray.class,
                        new StructuredArrayBuilder(
                                StructuredArray.class,
                                new StructuredArrayBuilder<PointArray, Point>(
                                        PointArray.class,
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
        assertTrue(array.get(0).getElementClass().isAssignableFrom(PointArray.class));
        assertTrue(array.get(0).get(0).getElementClass() == Point.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthWithNewInstance() throws NoSuchMethodException {
        long length = 9L;
        PointArray array = PointArray.newInstance(length);

        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == Point.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthWithBuilder() throws NoSuchMethodException {
        long length = 9L;
        PointArray array =
                new StructuredArrayBuilder<PointArray, Point>(
                        PointArray.class,
                        Point.class,
                        length
                ).build();

        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == Point.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthAndInitValues() throws NoSuchMethodException {
        final long initialX = 4;
        final long initialY = 777;

        long length = 9L;

        final PointArray array =
                PointArray.newInstance(length, initialX, initialY);

        assertCorrectFixedInitialisation(initialX, initialY, new long[] {length}, array);
    }

    @Test
    public void shouldConstructArrayElementsViaConstantCtorAndArgsProvider() throws NoSuchMethodException {
        final Class[] initArgTypes = {long.class, long.class};
        final long initialX = 4;
        final long initialY = 777;

        long length = 9L;

        final CtorAndArgs<Point> ctorAndArgs =
                new CtorAndArgs<Point>(Point.class, initArgTypes, initialX, initialY);
        final CtorAndArgsProvider<Point> ctorAndArgsProvider =
                new CtorAndArgsProvider<Point>() {
                    @Override
                    public CtorAndArgs<Point> getForContext(
                            ConstructionContext<Point> context) throws NoSuchMethodException {
                        return ctorAndArgs;
                    }
                };

        final PointArray array =
                PointArray.newInstance(
                        PointArray.class, Point.class, length, ctorAndArgsProvider);

        assertCorrectFixedInitialisation(initialX, initialY, new long[] {length}, array);
    }

    @Test
    public void shouldConstructArrayElementsViaCtorAndArgsProvider() throws NoSuchMethodException {
        final long[] lengths = {9};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();
        final PointArray array =
                PointArray.newInstance(lengths[0], ctorAndArgsProvider);

        assertCorrectVariableInitialisation(lengths, array);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructArrayElementsViaCtorAndArgsProvider3D() throws NoSuchMethodException {
        final long[] lengths = {7, 8, 9};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<PointArray>>,
                StructuredArray<PointArray>> builder = get3dBuilder(lengths);
        builder.getStructuredSubArrayBuilder().
                getStructuredSubArrayBuilder().
                elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<PointArray>> array = builder.build();

        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldSetAndGetCorrectValueAtGivenIndex() throws NoSuchMethodException {
        final long[] lengths = {11, 10, 3};
        final StructuredArray<StructuredArray<PointArray>> array = get3dBuilder(lengths).build();

        initValues(lengths, array);
        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldIterateOverArray() throws NoSuchMethodException {
        final long[] lengths = {11};
        final PointArray array =
                PointArray.newInstance(lengths[0]);

        initValues(lengths, array);

        PointArray.ElementIterator iter = array.iterator();

        long sum = 0;
        long elementCount = 0;
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final Point point = iter.next();

            assertThat(valueOf(point.getX()), is(valueOf(index)));
            assertThat(valueOf(point.getY()), is(valueOf(index * 2)));
            sum += index;
            elementCount++;
        }

        long sum2 = 0;
        long elementCount2 = 0;
        for (final Point point : array) {
            sum2 += point.getX();
            elementCount2++;
        }

        assertThat(valueOf(elementCount), is(valueOf(array.getLength())));
        assertThat(valueOf(sum), is(valueOf(sum2)));
        assertThat(valueOf(elementCount), is(valueOf(elementCount2)));
    }

    @Test
    public void shouldIterateOverArrayAndResetAgain() throws NoSuchMethodException {
        final long length = 11;
        final PointArray array =
                PointArray.newInstance(length);

        initValues(new long[] {length}, array);

        int i = 0;
        final PointArray.ElementIterator iter = array.iterator();
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final Point point = iter.next();
            assertThat(valueOf(point.getX()), is(valueOf(index)));
            assertThat(valueOf(point.getY()), is(valueOf(index * 2)));
            i++;
        }

        iter.reset();
        i = 0;
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final Point point = iter.next();
            assertThat(valueOf(point.getX()), is(valueOf(index)));
            assertThat(valueOf(point.getY()), is(valueOf(index * 2)));
            i++;
        }

        assertThat(valueOf(i), is(valueOf(length)));
    }

    @Test
    public void shouldConstructCopyOfArray() throws NoSuchMethodException {
        final long length = 15;
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();
        final PointArray sourceArray =
                PointArray.newInstance(length, ctorAndArgsProvider);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(length)));
        assertTrue(sourceArray.getElementClass() == Point.class);

        final PointArray newArray =
                (PointArray) StructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(new long[] {length}, newArray);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructCopyOfArray3D() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<PointArray>>,
                StructuredArray<PointArray>> builder = get3dBuilder(lengths);
        builder.getStructuredSubArrayBuilder().
                getStructuredSubArrayBuilder().
                elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<PointArray>> sourceArray = builder.build();

        StructuredArray<PointArray> subArray1 = sourceArray.get(0);
        PointArray subArray2 = subArray1.get(0);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(subArray1.getLength()), is(valueOf(lengths[1])));
        assertThat(valueOf(subArray2.getLength()), is(valueOf(lengths[2])));
        assertTrue(subArray2.getElementClass() == Point.class);

        final StructuredArray<StructuredArray<PointArray>> newArray =
                StructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(lengths, newArray);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructCopyOfArrayRange() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final PointCtorAndArgsProvider ctorAndArgsProvider = new PointCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<PointArray>>,
                StructuredArray<PointArray>> builder = get3dBuilder(lengths);
        builder.getStructuredSubArrayBuilder().
                getStructuredSubArrayBuilder().
                elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<PointArray>> sourceArray = builder.build();

        StructuredArray<PointArray> subArray1 = sourceArray.get(0);
        PointArray subArray2 = subArray1.get(0);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(subArray1.getLength()), is(valueOf(lengths[1])));
        assertThat(valueOf(subArray2.getLength()), is(valueOf(lengths[2])));
        assertTrue(subArray2.getElementClass() == Point.class);

        long[] offsets = {2, 2, 2};
        long[] counts = {13, 5, 3};
        final StructuredArray<StructuredArray<PointArray>> newArray =
                StructuredArray.copyInstance(sourceArray, offsets, counts);

        assertCorrectVariableInitialisation(counts, newArray, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowOutOfBoundExceptionForAccessesOutOfBounds() throws NoSuchMethodException {
        final long length = 11;
        final PointArray array =
                PointArray.newInstance(length);

        array.get(length);
    }

    @Test
    public void shouldNotThrowIncompatibleTypeExceptionForGetsOfProperTypes() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArray<StructuredArray<PointArray>> array = get3dBuilder(lengths).build();

        // Step by step gets of the correct type (array vs. element) per dimension:
        StructuredArray<PointArray> subArray1 = array.get(2);
        PointArray subArray2 = subArray1.get(2);
        subArray2.get(2);
    }

    @Test
    public void shouldCopyRegionLeftInArray() throws NoSuchMethodException {
        final long length = 11;
        final PointArray array =
                PointArray.newInstance(length);

        initValues(new long[]{length}, array);

        StructuredArray.shallowCopy(array, 4, array, 3, 2, false);

        assertThat(valueOf(array.get(3).getX()), is(valueOf(4)));
        assertThat(valueOf(array.get(4).getX()), is(valueOf(5)));
        assertThat(valueOf(array.get(5).getX()), is(valueOf(5)));
    }

    @Test
    public void shouldCopyRegionRightInArray() throws NoSuchMethodException {
        final long length = 11;
        final PointArray array =
                PointArray.newInstance(length);

        initValues(new long[]{length}, array);

        StructuredArray.shallowCopy(array, 5, array, 6, 2, false);

        assertThat(valueOf(array.get(5).getX()), is(valueOf(5)));
        assertThat(valueOf(array.get(6).getX()), is(valueOf(5)));
        assertThat(valueOf(array.get(7).getX()), is(valueOf(6)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToConstructDirectly() throws NoSuchMethodException {
        final PointArray array = new PointArray();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Test support below
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void assertCorrectFixedInitialisation(final long expectedX, final long expectedY, final long[] lengths,
                                                  final StructuredArray array) {
        StructuredArray a = array;
        for (int i = 0; i < lengths.length - 1; i++) {
            assertThat(valueOf(a.getLength()), is(valueOf(lengths[i])));
            a = (StructuredArray) a.get(0);
        }
        PointArray ap = (PointArray) a;
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
            ap = (PointArray) a;
            Point point = ap.get(cursors[cursors.length - 1]);

            assertThat(valueOf(point.getX()), is(valueOf(expectedX)));
            assertThat(valueOf(point.getY()), is(valueOf(expectedY)));

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
        PointArray ap = (PointArray) a;
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
            ap = (PointArray) a;
            Point point = ap.get(cursors[cursors.length - 1]);

            long indexSum = 0;
            String cursorsString = "";
            for (long index : cursors) {
                indexSum += index + indexOffset;
                cursorsString += index + ",";
            }

            assertThat("elementCountToCursor: " + elementCountToCursor + " cursors: " + cursorsString,
                    valueOf(point.getX()), is(valueOf(indexSum)));
            assertThat("elementCountToCursor: " + elementCountToCursor + " cursors: " + cursorsString,
                    valueOf(point.getY()), is(valueOf(indexSum * 2)));

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
            PointArray ap = (PointArray) a;
            Point point = ap.get(cursors[cursors.length - 1]);

            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }

            point.set(indexSum, indexSum * 2);

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

        private final Class[] argsTypes = {long.class, long.class};

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


    StructuredArrayBuilder<StructuredArray<StructuredArray<PointArray>>,
            StructuredArray<PointArray>> get3dBuilder(long... lengths) {
        @SuppressWarnings("unchecked")
        StructuredArrayBuilder<StructuredArray<StructuredArray<PointArray>>,
                StructuredArray<PointArray>> builder =
                new StructuredArrayBuilder(
                        StructuredArray.class,
                        new StructuredArrayBuilder(
                                StructuredArray.class,
                                new StructuredArrayBuilder<PointArray, Point>(
                                        PointArray.class,
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
