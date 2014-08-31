/*
* Written by Gil Tene and Martin Thompson, and released to the public domain,
* as explained at http://creativecommons.org/publicdomain/zero/1.0/
*/

package org.ObjectLayout;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StructuredArrayTest {

    @Test
    public void shouldConstructArrayOfGivenDirectLengths() throws NoSuchMethodException {
        final long[] lengths = {7L, 8L, 9L};

        @SuppressWarnings("unchecked")
        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> array =
                new StructuredArrayBuilder(
                        StructuredArray.class,
                        new StructuredArrayBuilder(
                                StructuredArray.class,
                                new StructuredArrayBuilder(
                                        StructuredArray.class,
                                        MockStructure.class,
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
        assertTrue(array.get(0).getElementClass().isAssignableFrom(StructuredArray.class));
        assertTrue(array.get(0).get(0).getElementClass() == MockStructure.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthWithNewInstance() throws NoSuchMethodException {
        long length = 9L;
        StructuredArray<MockStructure> array = StructuredArray.newInstance(MockStructure.class, length);

        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == MockStructure.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthWithBuilder() throws NoSuchMethodException {
        long length = 9L;
        @SuppressWarnings("unchecked")
        StructuredArray<MockStructure> array =
                new StructuredArrayBuilder(
                        StructuredArray.class,
                        MockStructure.class,
                        length
                ).build();

        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == MockStructure.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthAndInitValues() throws NoSuchMethodException {
        final Class[] initArgTypes = {long.class, long.class};
        final long expectedIndex = 4L;
        final long expectedValue = 777L;
        long length = 9L;

        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, length, initArgTypes, expectedIndex, expectedValue);

        assertCorrectFixedInitialisation(expectedIndex, expectedValue, new long[] {length}, array);
    }

    @Test
    public void shouldConstructArrayElementsViaCtorAndArgsProvider() throws NoSuchMethodException {
        final long[] lengths = {9};
        final DefaultMockCtorAndArgsProvider ctorAndArgsProvider = new DefaultMockCtorAndArgsProvider();
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, ctorAndArgsProvider, lengths[0]);

        assertCorrectVariableInitialisation(lengths, array);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructArrayElementsViaCtorAndArgsProvider3D() throws NoSuchMethodException {
        final long[] lengths = {7, 8, 9};
        final DefaultMockCtorAndArgsProvider ctorAndArgsProvider = new DefaultMockCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArray<MockStructure>>>,
                StructuredArray<StructuredArray<MockStructure>>> builder = get3dBuilder(lengths);
        builder.getSubArrayBuilder().getSubArrayBuilder().elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> array = builder.build();

        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldConstructArrayElementsViaLambdas() throws NoSuchMethodException {
//        Uncomment for Java 8, keep commented for Java 7 and 6
//
//        final Constructor<MockStructure> constructor =
//                MockStructure.class.getConstructor(Long.TYPE, Long.TYPE);
//
//        final long length = 8;
//        final StructuredArray<MockStructure> array =
//                StructuredArray.newInstance(MockStructure.class,
//                        context -> new CtorAndArgs<MockStructure>(
//                                constructor,
//                                context.getIndex(), context.getIndex() * 2),
//                        length);
//
//        assertCorrectVariableInitialisation(new long[] {length}, array);
//
//        final long[] lengths = {7, 8, 9};
//
//        final StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArray<MockStructure>>>,
//                StructuredArray<StructuredArray<MockStructure>>> builder = get3dBuilder(lengths);
//        builder.getSubArrayBuilder().getSubArrayBuilder().elementCtorAndArgsProvider(
//                context -> {
//                    long indexSum = 0;
//                    for (ConstructionContext c = context; c != null; c = c.getContainingContext()) {
//                        indexSum += c.getIndex();
//                    }
//                    return new CtorAndArgs<MockStructure>(constructor,
//                            indexSum, (indexSum * 2L));
//                }
//        );
//
//        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> array2 = builder.build();
//
//        assertCorrectVariableInitialisation(lengths, array2);
    }

    @Test
    public void shouldSetAndGetCorrectValueAtGivenIndex() throws NoSuchMethodException {
        final long[] lengths = {11, 10, 3};
        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> array = get3dBuilder(lengths).build();

        initValues(lengths, array);
        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldIterateOverArray() throws NoSuchMethodException {
        final long[] lengths = {11};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths[0]);

        initValues(lengths, array);

        StructuredArray<MockStructure>.ElementIterator iter = array.iterator();

        long sum = 0;
        long elementCount = 0;
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final MockStructure mockStructure = iter.next();

            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(index)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(index * 2)));
            sum += index;
            elementCount++;
        }

        long sum2 = 0;
        long elementCount2 = 0;
        for (final MockStructure mockStructure : array) {
            sum2 += mockStructure.getIndex();
            elementCount2++;
        }

        assertThat(valueOf(elementCount), is(valueOf(array.getLength())));
        assertThat(valueOf(sum), is(valueOf(sum2)));
        assertThat(valueOf(elementCount), is(valueOf(elementCount2)));
    }

    @Test
    public void shouldIterateOverArrayAndResetAgain() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, length);

        initValues(new long[] {length}, array);

        int i = 0;
        final StructuredArray<MockStructure>.ElementIterator iter = array.iterator();
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final MockStructure mockStructure = iter.next();
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(index)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(index * 2)));
            i++;
        }

        iter.reset();
        i = 0;
        while (iter.hasNext()) {
            final long index = iter.getCursor();
            final MockStructure mockStructure = iter.next();
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(index)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(index * 2)));
            i++;
        }

        assertThat(valueOf(i), is(valueOf(length)));
    }

    @Test
    public void shouldConstructCopyOfArray() throws NoSuchMethodException {
        final long length = 15;
        final DefaultMockCtorAndArgsProvider ctorAndArgsProvider = new DefaultMockCtorAndArgsProvider();
        final StructuredArray<MockStructure> sourceArray =
                StructuredArray.newInstance(MockStructure.class, ctorAndArgsProvider, length);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(length)));
        assertTrue(sourceArray.getElementClass() == MockStructure.class);

        final StructuredArray<MockStructure> newArray = StructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(new long[] {length}, newArray);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructCopyOfArray3D() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final DefaultMockCtorAndArgsProvider ctorAndArgsProvider = new DefaultMockCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArray<MockStructure>>>,
                StructuredArray<StructuredArray<MockStructure>>> builder = get3dBuilder(lengths);
        builder.getSubArrayBuilder().getSubArrayBuilder().elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> sourceArray = builder.build();

        StructuredArray<StructuredArray<MockStructure>> subArray1 = sourceArray.get(0);
        StructuredArray<MockStructure> subArray2 = subArray1.get(0);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(subArray1.getLength()), is(valueOf(lengths[1])));
        assertThat(valueOf(subArray2.getLength()), is(valueOf(lengths[2])));
        assertTrue(subArray2.getElementClass() == MockStructure.class);

        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> newArray =
                StructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(lengths, newArray);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldConstructCopyOfArrayRange() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final DefaultMockCtorAndArgsProvider ctorAndArgsProvider = new DefaultMockCtorAndArgsProvider();

        final StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArray<MockStructure>>>,
                StructuredArray<StructuredArray<MockStructure>>> builder = get3dBuilder(lengths);
        builder.getSubArrayBuilder().getSubArrayBuilder().elementCtorAndArgsProvider(ctorAndArgsProvider);

        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> sourceArray = builder.build();

        StructuredArray<StructuredArray<MockStructure>> subArray1 = sourceArray.get(0);
        StructuredArray<MockStructure> subArray2 = subArray1.get(0);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(subArray1.getLength()), is(valueOf(lengths[1])));
        assertThat(valueOf(subArray2.getLength()), is(valueOf(lengths[2])));
        assertTrue(subArray2.getElementClass() == MockStructure.class);

        long[] offsets = {2, 2, 2};
        long[] counts = {13, 5, 3};
        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> newArray =
                StructuredArray.copyInstance(sourceArray, offsets, counts);

        assertCorrectVariableInitialisation(counts, newArray, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowOutOfBoundExceptionForAccessesOutOfBounds() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, length);

        array.get(length);
    }

    @Test
    public void shouldNotThrowIncompatibleTypeExceptionForGetsOfProperTypes() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArray<StructuredArray<StructuredArray<MockStructure>>> array = get3dBuilder(lengths).build();

        // Step by step gets of the correct type (array vs. element) per dimension:
        StructuredArray<StructuredArray<MockStructure>> subArray1 = array.get(2);
        StructuredArray<MockStructure> subArray2 = subArray1.get(2);
        subArray2.get(2);

    }

    @Test
    public void shouldCopyRegionLeftInArray() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, length);

        initValues(new long[]{length}, array);

        StructuredArray.shallowCopy(array, 4, array, 3, 2, false);

        assertThat(valueOf(array.get(3).getIndex()), is(valueOf(4)));
        assertThat(valueOf(array.get(4).getIndex()), is(valueOf(5)));
        assertThat(valueOf(array.get(5).getIndex()), is(valueOf(5)));
    }

    @Test
    public void shouldCopyRegionRightInArray() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, length);

        initValues(new long[]{length}, array);

        StructuredArray.shallowCopy(array, 5, array, 6, 2, false);

        assertThat(valueOf(array.get(5).getIndex()), is(valueOf(5)));
        assertThat(valueOf(array.get(6).getIndex()), is(valueOf(5)));
        assertThat(valueOf(array.get(7).getIndex()), is(valueOf(6)));
    }

    @Test
    public void shouldCopyEvenWithFinalFields() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArray<MockStructureWithFinalField> array =
                StructuredArray.newInstance(MockStructureWithFinalField.class, length);

        StructuredArray.shallowCopy(array, 1, array, 3, 1, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenFinalFieldWouldBeCopied() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArray<MockStructureWithFinalField> array =
                StructuredArray.newInstance(MockStructureWithFinalField.class, length);

        StructuredArray.shallowCopy(array, 1, array, 3, 1);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Test support below
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void assertCorrectFixedInitialisation(final long expectedIndex, final long expectedValue, final long[] lengths,
                                                  final StructuredArray<MockStructure> array) {
        StructuredArray a = array;
        for (int i = 0; i < lengths.length - 1; i++) {
            assertThat(valueOf(a.getLength()), is(valueOf(lengths[i])));
            a = (StructuredArray) a.get(0);
        }
        assertThat(valueOf(a.getLength()), is(valueOf(lengths[lengths.length - 1])));

        assertTrue(a.getElementClass() == MockStructure.class);

        long totalElementCount = 1;
        for (long l : lengths) {
            totalElementCount *= l;
        }

        final long[] cursors = new long[lengths.length];

        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            // Check element at cursors:
            a = array;
            for (int i = 0; i < cursors.length - 1; i++) {
                a = (StructuredArray) a.get(cursors[i]);
            }
            MockStructure mockStructure = (MockStructure) a.get(cursors[cursors.length - 1]);

            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(expectedIndex)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(expectedValue)));

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
        assertThat(valueOf(a.getLength()), is(valueOf(lengths[lengths.length - 1])));

        assertTrue(a.getElementClass() == MockStructure.class);

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
            MockStructure mockStructure = (MockStructure) a.get(cursors[cursors.length - 1]);

            long indexSum = 0;
            String cursorsString = "";
            for (long index : cursors) {
                indexSum += index + indexOffset;
                cursorsString += index + ",";
            }

            assertThat("elementCountToCursor: " + elementCountToCursor + " cursors: " + cursorsString,
                    valueOf(mockStructure.getIndex()), is(valueOf(indexSum)));
            assertThat("elementCountToCursor: " + elementCountToCursor + " cursors: " + cursorsString,
                    valueOf(mockStructure.getTestValue()), is(valueOf(indexSum * 2)));

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
            MockStructure mockStructure = (MockStructure) a.get(cursors[cursors.length - 1]);

            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }

            mockStructure.setIndex(indexSum);
            mockStructure.setTestValue(indexSum * 2);

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

    public static class MockStructure {

        private long index = -1;
        private long testValue = Long.MIN_VALUE;

        public MockStructure() {
        }

        public MockStructure(final long index, final long testValue) {
            this.index = index;
            this.testValue = testValue;
        }

        public MockStructure(final MockStructure src) {
            this.index = src.index;
            this.testValue = src.testValue;
        }

        public long getIndex() {
            return index;
        }

        public void setIndex(final long index) {
            this.index = index;
        }

        public long getTestValue() {
            return testValue;
        }

        public void setTestValue(final long testValue) {
            this.testValue = testValue;
        }

        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final MockStructure that = (MockStructure)o;

            return index == that.index && testValue == that.testValue;
        }

        public int hashCode() {
            int result = (int)(index ^ (index >>> 32));
            result = 31 * result + (int)(testValue ^ (testValue >>> 32));
            return result;
        }

        public String toString() {
            return "MockStructure{" +
                    "index=" + index +
                    ", testValue=" + testValue +
                    '}';
        }
    }

    public static class MockStructureWithFinalField {
        private final int value = 888;
    }

    private static class DefaultMockCtorAndArgsProvider extends AbstractCtorAndArgsProvider<MockStructure> {

        private final Class[] argsTypes = {Long.TYPE, Long.TYPE};

        public CtorAndArgs<MockStructure> getForContext(ConstructionContext<MockStructure> context) throws NoSuchMethodException {
            long indexSum = 0;
            for (ConstructionContext c = context; c != null; c = c.getContainingContext()) {
                indexSum += c.getIndex();
            }
            Object[] args = {indexSum, indexSum * 2};
            // We could do this much more efficiently with atomic caching of a single allocated CtorAndArgs,
            // as CopyCtorAndArgsProvider does, but no need to put in the effort in a test...
            return new CtorAndArgs<MockStructure>(MockStructure.class.getConstructor(argsTypes), args);
        }
    }

    StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArray<MockStructure>>>,
            StructuredArray<StructuredArray<MockStructure>>> get3dBuilder(long... lengths) {
        @SuppressWarnings("unchecked")
        StructuredArrayBuilder<StructuredArray<StructuredArray<StructuredArray<MockStructure>>>,
                StructuredArray<StructuredArray<MockStructure>>> builder =
                new StructuredArrayBuilder(
                        StructuredArray.class,
                        new StructuredArrayBuilder(
                                StructuredArray.class,
                                new StructuredArrayBuilder(
                                        StructuredArray.class,
                                        MockStructure.class,
                                        lengths[2]
                                ),
                                lengths[1]
                        ),
                        lengths[0]
                );
        return builder;
    }
}
