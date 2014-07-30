/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import org.junit.Test;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StructuredArrayTest {

    @Test
    public void shouldConstructArrayOfGivenDirectLengths() throws NoSuchMethodException {
        final Long[] lengths = {7L, 8L, 9L};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, 7L, 8L, 9L);

        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(lengths[i]));
        }
        assertTrue(array.getElementClass() == MockStructure.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthsPrimitiveLongArray() throws NoSuchMethodException {
        final long[] lengths = {7L, 8L, 9L};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths);

        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == MockStructure.class);

        final long[] lengths2 = {7L, 8L};
        final StructuredArray<MockStructure> array2 =
                StructuredArray.newInstance(MockStructure.class, lengths2);

        for (int i = 0; i < lengths2.length; i++) {
            assertThat(valueOf(array2.getLengths()[i]), is(valueOf(lengths2[i])));
        }
        assertTrue(array2.getElementClass() == MockStructure.class);

        final long[] lengths3 = {8L};
        final StructuredArray<MockStructure> array3 =
                StructuredArray.newInstance(MockStructure.class, lengths3);

        for (int i = 0; i < lengths3.length; i++) {
            assertThat(valueOf(array3.getLengths()[i]), is(valueOf(lengths3[i])));
        }
        assertTrue(array3.getElementClass() == MockStructure.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthAndInitValues() throws NoSuchMethodException {
        final Class[] initArgTypes = {long.class, long.class};
        final long expectedIndex = 4L;
        final long expectedValue = 777L;
        final long[] lengths = {7, 8, 9};

        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths, initArgTypes, expectedIndex, expectedValue);

        assertCorrectFixedInitialisation(expectedIndex, expectedValue, lengths, array);
    }

    @Test
    public void shouldConstructArrayElementsViaElementConstructorGenerator() throws NoSuchMethodException {
        final long[] lengths = {7, 8, 9};
        final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider =
                new DefaultMockCtorAndArgsProvider();
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, ctorAndArgsProvider, lengths);

        assertCorrectVariableInitialisation(lengths, array);

        final long[] lengths2 = {8};
        final StructuredArray<MockStructure> array2 =
                StructuredArray.newInstance(MockStructure.class, ctorAndArgsProvider, lengths2);

        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldSetAndGetCorrectValueAtGivenIndex() throws NoSuchMethodException {
        final long[] lengths = {11, 10, 3};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths);

        initValues(lengths, array);
        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldIterateOverArray() throws NoSuchMethodException {
        final long[] lengths = {11, 8, 3};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths);

        initValues(lengths, array);

        StructuredArray<MockStructure>.ElementIterator iter = array.iterator();

        long sum = 0;
        long elementCount = 0;
        while (iter.hasNext()) {
            final long[] cursors = iter.getCursors();
            final MockStructure mockStructure = iter.next();
            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(indexSum)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(indexSum * 2)));
            sum += indexSum;
            elementCount++;
        }

        long sum2 = 0;
        long elementCount2 = 0;
        for (final MockStructure mockStructure : array) {
            sum2 += mockStructure.getIndex();
            elementCount2++;
        }

        assertThat(valueOf(elementCount), is(valueOf(array.getTotalElementCount())));
        assertThat(valueOf(sum), is(valueOf(sum2)));
        assertThat(valueOf(elementCount), is(valueOf(elementCount2)));
    }

    @Test
    public void shouldIterateOverArrayAndResetAgain() throws NoSuchMethodException {
        final long[] lengths = {11, 8, 4};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths);

        initValues(lengths, array);

        int i = 0;
        final StructuredArray<MockStructure>.ElementIterator iter = array.iterator();
        while (iter.hasNext()) {
            final long[] cursors = iter.getCursors();
            final MockStructure mockStructure = iter.next();
            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(indexSum)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(indexSum * 2)));
            i++;
        }

        iter.reset();
        i = 0;
        while (iter.hasNext()) {
            final long[] cursors = iter.getCursors();
            final MockStructure mockStructure = iter.next();
            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(indexSum)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(indexSum * 2)));
            i++;
        }

        assertThat(valueOf(i), is(valueOf(lengths[0] * lengths[1] * lengths[2])));
    }

    @Test
    public void shouldConstructCopyOfArray() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider =
                new DefaultMockCtorAndArgsProvider();
        final StructuredArray<MockStructure> sourceArray =
                StructuredArray.newInstance(MockStructure.class, ctorAndArgsProvider, lengths);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(sourceArray.getTotalElementCount()), is(valueOf(lengths[0] * lengths[1] * lengths[2])));
        assertTrue(sourceArray.getElementClass() == MockStructure.class);

        final StructuredArray<MockStructure> newArray =
                StructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(lengths, newArray);
    }

    @Test
    public void shouldConstructCopyOfArrayRange() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider =
                new DefaultMockCtorAndArgsProvider();
        final StructuredArray<MockStructure> sourceArray =
                StructuredArray.newInstance(MockStructure.class, ctorAndArgsProvider, lengths);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(sourceArray.getTotalElementCount()), is(valueOf(lengths[0] * lengths[1] * lengths[2])));
        assertTrue(sourceArray.getElementClass() == MockStructure.class);

        long[] offsets = {1, 1, 1};
        long[] counts = {14, 6, 4};
        final StructuredArray<MockStructure> newArray =
                StructuredArray.copyInstance(sourceArray, offsets, counts);

        final StructuredArray<MockStructure>.ElementIterator iter = newArray.iterator();

        // We expect MockStructure elements to be initialized with index = indexSum, and testValue = indexSum * 2,
        // but with the sums based on index+1 for each cursor (due to the {1, 1, 1} offset above):
        while (iter.hasNext()) {
            final long[] cursors = iter.getCursors();
            final MockStructure mockStructure = iter.next();
            long indexSum = 0;
            for (long index : cursors) {
                indexSum += (index + 1);
            }
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(indexSum)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(indexSum * 2)));
        }
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowOutOfBoundExceptionForAccessesOutOfBounds() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths);

        array.get(lengths);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIncompatibleTypeExceptionForGetOfAnElementInsteadOfArray() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths);

        // A 2D get in a 3D array is expected to throw an NPE:
        array.get(2, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIncompatibleTypeExceptionForGetOfAnArrayInsteadOfElement() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths);

        StructuredArray<MockStructure> subArray1 = array.getSubArray(2);
        StructuredArray<MockStructure> subArray2 = subArray1.getSubArray(2);
        subArray2.getSubArray(2);
    }


    @Test
    public void shouldNotThrowIncompatibleTypeExceptionForGetsOfProperTypes() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final StructuredArray<MockStructure> array =
                StructuredArray.newInstance(MockStructure.class, lengths);

        // Step by step gets of the correct type (array vs. element) per dimension:
        StructuredArray<MockStructure> subArray1 = array.getSubArray(2);
        StructuredArray<MockStructure> subArray2 = subArray1.getSubArray(2);
        subArray2.get(2);

        // The end result of the above is equivalent to this:
        array.get(2, 2, 2);
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
        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == MockStructure.class);

        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            MockStructure mockStructure = array.get(cursors);
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
                                             final StructuredArray<MockStructure> array) {
        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == MockStructure.class);

        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            MockStructure mockStructure = array.get(cursors);

            long indexSum = 0;
            String cursorsString = "";
            for (long index : cursors) {
                indexSum += index;
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

    private void initValues(final long[] lengths, final StructuredArray<MockStructure> array) {
        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            MockStructure mockStructure = array.get(cursors);

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

    private static class DefaultMockCtorAndArgsProvider extends CtorAndArgsProvider<MockStructure> {

        private final Class[] argsTypes = {Long.TYPE, Long.TYPE};

        public DefaultMockCtorAndArgsProvider() throws NoSuchMethodException {
            super(MockStructure.class);
        }

        // Single dimension form is not strictly necessary (will call multi-dimension form if it wasn't here)
        // but this way we get to demonstrate that path too.
        public CtorAndArgs<MockStructure> getForIndex(long index) throws NoSuchMethodException {
            Object[] args = {index, index * 2};
            // We could do this much more efficiently with atomic caching of a single allocated CtorAndArgs,
            // as CopyCtorAndArgsProvider does, but no need to put in the effort in a test...
            return new CtorAndArgs<MockStructure>(MockStructure.class.getConstructor(argsTypes), args);
        }

        // Multi-dimension form needed if we expect to have multi-dimensional arrays:
        public CtorAndArgs<MockStructure> getForIndex(long... index) throws NoSuchMethodException {
            long indexSum = 0;
            for (long index0 : index) {
                indexSum += index0;
            }
            Object[] args = {indexSum, indexSum * 2};
            // We could do this much more efficiently with atomic caching of a single allocated CtorAndArgs,
            // as CopyCtorAndArgsProvider does, but no need to put in the effort in a test...
            return new CtorAndArgs<MockStructure>(MockStructure.class.getConstructor(argsTypes), args);
        }
    }
}
