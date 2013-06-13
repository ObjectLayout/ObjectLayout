/*
 * Copyright 2013 Gil Tene
 * Copyright 2012, 2013 Martin Thompson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ObjectLayout;

import org.junit.Test;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MultiDimensionalStructuredArrayTest {

    @Test
    public void shouldConstructArrayOfGivenDirectLengths() throws NoSuchMethodException {
        final Long[] lengths = {7L, 8L, 9L};
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, 7L, 8L, 9L);

        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(lengths[i]));
        }
        assertTrue(array.getElementClass() == MockStructure.class);
    }


    @Test
    public void shouldConstructArrayOfGivenLengthsLongArray() throws NoSuchMethodException {
        final Long[] lengths = {7L, 8L, 9L};
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(lengths[i]));
        }
        assertTrue(array.getElementClass() == MockStructure.class);
    }



    @Test
    public void shouldConstructArrayOfGivenLengthsPrimitiveLongArray() throws NoSuchMethodException {
        final long[] lengths = {7L, 8L, 9L};
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == MockStructure.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthAndInitValues() throws NoSuchMethodException {
        final Class[] initArgTypes = {long.class, long.class};
        final long expectedIndex = 4L;
        final long expectedValue = 777L;
        final long[] lengths = {7, 8, 9};

        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths, initArgTypes, expectedIndex, expectedValue);

        assertCorrectFixedInitialisation(expectedIndex, expectedValue, lengths, array);
    }

    @Test
    public void shouldConstructArrayElementsViaElementConstructorGenerator() throws NoSuchMethodException {
        final long[] lengths = {7, 8, 9};
        final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider =
                new DefaultMockCtorAndArgsProvider();
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(ctorAndArgsProvider, lengths);

        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldSetAndGetCorrectValueAtGivenIndex() throws NoSuchMethodException {
        final long[] lengths = {11, 10, 3};
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        initValues(lengths, array);
        assertCorrectVariableInitialisation(lengths, array);
    }

    @Test
    public void shouldIterateOverArray() throws NoSuchMethodException {
        final long[] lengths = {11, 8, 3};
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        initValues(lengths, array);

        MultiDimensionalStructuredArray<MockStructure>.StructureIterator iter = array.iterator();

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
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        initValues(lengths, array);

        int i = 0;
        final MultiDimensionalStructuredArray<MockStructure>.StructureIterator iter = array.iterator();
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
        final MultiDimensionalStructuredArray<MockStructure> sourceArray =
                MultiDimensionalStructuredArray.newInstance(ctorAndArgsProvider, lengths);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(sourceArray.getTotalElementCount()), is(valueOf(lengths[0] * lengths[1] * lengths[2])));
        assertTrue(sourceArray.getElementClass() == MockStructure.class);

        final MultiDimensionalStructuredArray<MockStructure> newArray =
                MultiDimensionalStructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        assertCorrectVariableInitialisation(lengths, newArray);
    }

    @Test
    public void shouldConstructCopyOfArrayRange() throws NoSuchMethodException {
        final long[] lengths = {15, 7, 5};
        final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider =
                new DefaultMockCtorAndArgsProvider();
        final MultiDimensionalStructuredArray<MockStructure> sourceArray =
                MultiDimensionalStructuredArray.newInstance(ctorAndArgsProvider, lengths);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(lengths[0])));
        assertThat(valueOf(sourceArray.getTotalElementCount()), is(valueOf(lengths[0] * lengths[1] * lengths[2])));
        assertTrue(sourceArray.getElementClass() == MockStructure.class);

        long[] offsets = {1, 1, 1};
        long[] counts = {14, 6, 4};
        final MultiDimensionalStructuredArray<MockStructure> newArray =
                MultiDimensionalStructuredArray.copyInstance(sourceArray, offsets, counts);

        final MultiDimensionalStructuredArray<MockStructure>.StructureIterator iter = newArray.iterator();

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
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        array.getL(lengths);
    }

    @Test(expected = ClassCastException.class)
    public void shouldThrowIncompatibleTypeExceptionForGetOfAnArrayInsteadOfElement() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        array.get(2, 2);
    }

    @Test(expected = ClassCastException.class)
    public void shouldThrowIncompatibleTypeExceptionForGetOfWrongStructuredArrayType() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        // The following will actually be a MultiDimensionalStructuredArray:
        array.getOfStructuredArray(2);
    }

    @Test(expected = ClassCastException.class)
    public void shouldThrowIncompatibleTypeExceptionForGetOfWrongMultiDimensionalStructuredArrayType() throws NoSuchMethodException {
        final long[] lengths = {11, 7, 4};
        final MultiDimensionalStructuredArray<MockStructure> array =
                MultiDimensionalStructuredArray.newInstance(MockStructure.class, lengths);

        // The following will really be a MultiDimensionalStructuredArray:
        MultiDimensionalStructuredArray<MockStructure> subArray1 = array.getOfMultiDimensionalStructuredArray(2);
        // But the following will actually be a SingleDimensionalStructuredArray:
        subArray1.getOfMultiDimensionalStructuredArray(2);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Test support below
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void assertCorrectFixedInitialisation(final long expectedIndex, final long expectedValue, final long[] lengths,
                                                  final MultiDimensionalStructuredArray<MockStructure> array) {
        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == MockStructure.class);

        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            MockStructure mockStructure = array.getL(cursors);
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(expectedIndex)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(expectedValue)));

            // Increment cursors from inner-most dimension out:
            for (int cursorDimension = cursors.length - 1; cursorDimension >= 0; cursorDimension--) {
                if ((++cursors[cursorDimension]) < lengths[cursorDimension])
                    break;
                // This dimension wrapped. Reset to zero and continue to one dimension higher
                cursors[cursorDimension] = 0;
            }
            elementCountToCursor++;
        }
    }

    private void assertCorrectVariableInitialisation(final long[] lengths,
                                             final MultiDimensionalStructuredArray<MockStructure> array) {
        for (int i = 0; i < lengths.length; i++) {
            assertThat(valueOf(array.getLengths()[i]), is(valueOf(lengths[i])));
        }
        assertTrue(array.getElementClass() == MockStructure.class);

        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            MockStructure mockStructure = array.getL(cursors);

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
                if ((++cursors[cursorDimension]) < lengths[cursorDimension])
                    break;
                // This dimension wrapped. Reset to zero and continue to one dimension higher
                cursors[cursorDimension] = 0;
            }
            elementCountToCursor++;
        }
    }

    private void initValues(final long[] lengths, final MultiDimensionalStructuredArray<MockStructure> array) {
        final long[] cursors = new long[lengths.length];
        final long totalElementCount = array.getTotalElementCount();
        long elementCountToCursor = 0;

        while (elementCountToCursor < totalElementCount) {
            // Check element at cursors:
            MockStructure mockStructure = array.getL(cursors);

            long indexSum = 0;
            for (long index : cursors) {
                indexSum += index;
            }

            mockStructure.setIndex(indexSum);
            mockStructure.setTestValue(indexSum * 2);

            // Increment cursors from inner-most dimension out:
            for (int cursorDimension = cursors.length - 1; cursorDimension >= 0; cursorDimension--) {
                if ((++cursors[cursorDimension]) < lengths[cursorDimension])
                    break;
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

        public MockStructure(MockStructure src) {
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

    private static class DefaultMockCtorAndArgsProvider extends CtorAndArgsProvider<MockStructure>
    {

        private final Class[] argsTypes = {Long.TYPE, Long.TYPE};

        public DefaultMockCtorAndArgsProvider() throws NoSuchMethodException {
            super(MockStructure.class);
        }

        public CtorAndArgs<MockStructure> getForIndices(long indices[]) throws NoSuchMethodException {
            long indexSum = 0;
            for (long index : indices) {
                indexSum += index;
            }
            Object[] args = {indexSum, indexSum * 2};
            // We could do this much more efficiently with atomic caching of a single allocated CtorAndArgs,
            // as SingleDimensionalCopyCtorAndArgsProvider does, but no need to put in the effort in a test...
            return new CtorAndArgs<MockStructure>(MockStructure.class.getConstructor(argsTypes), args);
        }

    }
}
