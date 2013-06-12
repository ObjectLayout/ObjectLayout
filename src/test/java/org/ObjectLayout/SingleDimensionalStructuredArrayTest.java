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

public class SingleDimensionalStructuredArrayTest {

    @Test
    public void shouldConstructArrayOfGivenLength() throws NoSuchMethodException {
        final long length = 7;
        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(MockStructure.class, length);

        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == MockStructure.class);
    }

    @Test
    public void shouldConstructArrayOfGivenLengthAndInitValues() throws NoSuchMethodException {
        final Class[] initArgTypes = {long.class, long.class};
        final long expectedIndex = 4L;
        final long expectedValue = 777L;
        final long length = 7;

        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(MockStructure.class, length, initArgTypes, expectedIndex, expectedValue);

        assertCorrectInitialisation(expectedIndex, expectedValue, length, array);
    }

    @Test
    public void shouldConstructArrayElementsViaElementConstructorGenerator() throws NoSuchMethodException {
        final long length = 7;
        final ConstructorAndArgsLocator<MockStructure> constructorAndArgsLocator =
                new DefaultMockConstructorAndArgsLocator();
        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(constructorAndArgsLocator, length);

        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == MockStructure.class);
        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        for (int i = 0; i < length; i++) {
            final MockStructure mockStructure = array.get(i);

            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(i * 2)));
        }
    }

    @Test
    public void shouldSetAndGetCorrectValueAtGivenIndex() throws NoSuchMethodException {
        final long length = 11;
        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(MockStructure.class, length);

        initValues(length, array);

        for (long i = 0; i < length; i++) {
            final MockStructure mockStructure = array.getL(i);

            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(i * 2)));
        }
    }

    @Test
    public void shouldIterateOverArray() throws NoSuchMethodException {
        final int length = 11;
        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(MockStructure.class, length);

        initValues(length, array);

        int i = 0;
        for (final MockStructure mockStructure : array) {
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(i * 2)));
            i++;
        }

        assertThat(valueOf(i), is(valueOf(length)));
    }

    @Test
    public void shouldIterateOverArrayAndResetAgain() throws NoSuchMethodException {
        final long length = 11;
        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(MockStructure.class, length);

        initValues(length, array);

        int i = 0;
        final SingleDimensionalStructuredArray<MockStructure>.StructureIterator iter = array.iterator();
        while (iter.hasNext()) {
            final MockStructure mockStructure = iter.next();
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(i * 2)));
            i++;
        }

        iter.reset();
        i = 0;
        while (iter.hasNext()) {
            final MockStructure mockStructure = iter.next();
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(i * 2)));
            i++;
        }

        assertThat(valueOf(i), is(valueOf(length)));
    }

    @Test
    public void shouldConstructCopyOfArray() throws NoSuchMethodException {
        final long length = 7;
        final ConstructorAndArgsLocator<MockStructure> constructorAndArgsLocator =
                new DefaultMockConstructorAndArgsLocator();
        final SingleDimensionalStructuredArray<MockStructure> sourceArray =
                SingleDimensionalStructuredArray.newInstance(constructorAndArgsLocator, length);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(length)));
        assertTrue(sourceArray.getElementClass() == MockStructure.class);

        final SingleDimensionalStructuredArray<MockStructure> newArray = SingleDimensionalStructuredArray.copyInstance(sourceArray);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        for (int i = 0; i < length; i++) {
            final MockStructure mockStructure = newArray.get(i);

            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(i * 2)));
        }
    }

    @Test
    public void shouldConstructCopyOfArrayRange() throws NoSuchMethodException {
        final long length = 7;
        final ConstructorAndArgsLocator<MockStructure> constructorAndArgsLocator =
                new DefaultMockConstructorAndArgsLocator();
        final SingleDimensionalStructuredArray<MockStructure> sourceArray =
                SingleDimensionalStructuredArray.newInstance(constructorAndArgsLocator, length);

        assertThat(valueOf(sourceArray.getLength()), is(valueOf(length)));
        assertTrue(sourceArray.getElementClass() == MockStructure.class);

        final SingleDimensionalStructuredArray<MockStructure> newArray = SingleDimensionalStructuredArray.copyInstance(sourceArray, 1, 6);

        // We expect MockStructure elements to be initialized with index = index, and testValue = index * 2:
        for (int i = 0; i < (length - 1); i++) {
            final MockStructure mockStructure = newArray.get(i);

            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i + 1)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf((i + 1) * 2)));
        }
    }

    @Test
    public void shouldCopyRegionLeftInArray() throws NoSuchMethodException {
        final long length = 11;
        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(MockStructure.class, length);

        initValues(length, array);

        SingleDimensionalStructuredArray.shallowCopy(array, 4, array, 3, 2, false);

        assertThat(valueOf(array.get(3).getIndex()), is(valueOf(4)));
        assertThat(valueOf(array.get(4).getIndex()), is(valueOf(5)));
        assertThat(valueOf(array.get(5).getIndex()), is(valueOf(5)));
    }

    @Test
    public void shouldCopyRegionRightInArray() throws NoSuchMethodException {
        final long length = 11;
        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(MockStructure.class, length);

        initValues(length, array);

        SingleDimensionalStructuredArray.shallowCopy(array, 5, array, 6, 2, false);

        assertThat(valueOf(array.get(5).getIndex()), is(valueOf(5)));
        assertThat(valueOf(array.get(6).getIndex()), is(valueOf(5)));
        assertThat(valueOf(array.get(7).getIndex()), is(valueOf(6)));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowOutOfBoundExceptionForAccessesOutOfBounds() throws NoSuchMethodException {
        final long length = 11;
        final SingleDimensionalStructuredArray<MockStructure> array =
                SingleDimensionalStructuredArray.newInstance(MockStructure.class, length);

        array.getL(length);
    }

    @Test
    public void shouldCopyEvenWithFinalFields() throws NoSuchMethodException {
        final long length = 11;
        final SingleDimensionalStructuredArray<MockStructureWithFinalField> array =
                SingleDimensionalStructuredArray.newInstance(MockStructureWithFinalField.class, length);

        SingleDimensionalStructuredArray.shallowCopy(array, 1, array, 3, 1, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenFinalFieldWouldBeCopied() throws NoSuchMethodException {
        final long length = 11;
        final SingleDimensionalStructuredArray<MockStructureWithFinalField> array =
                SingleDimensionalStructuredArray.newInstance(MockStructureWithFinalField.class, length);

        SingleDimensionalStructuredArray.shallowCopy(array, 1, array, 3, 1);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Test support below
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void assertCorrectInitialisation(final long expectedIndex, final long expectedValue, final long length,
                                             final SingleDimensionalStructuredArray<MockStructure> array) {
        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == MockStructure.class);

        for (long i = 0; i < length; i++) {
            MockStructure mockStructure = array.getL(i);
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(expectedIndex)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(expectedValue)));
        }
    }

    private void initValues(final long length, final SingleDimensionalStructuredArray<MockStructure> array) {
        for (long i = 0; i < length; i++) {
            final MockStructure mockStructure = array.getL(i);
            mockStructure.setIndex(i);
            mockStructure.setTestValue(i * 2);
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

    private static class DefaultMockConstructorAndArgsLocator extends ConstructorAndArgsLocator<MockStructure> {

        private final Class[] argsTypes = {Long.TYPE, Long.TYPE};

        public DefaultMockConstructorAndArgsLocator() throws NoSuchMethodException {
            super(MockStructure.class);
        }

        public ConstructorAndArgs<MockStructure> getForIndex(long index)
            throws NoSuchMethodException {
            Object[] args = {index, index * 2};
            // We could do this much more efficiently with atomic caching of a single allocated ConstructorAndArgs,
            // as SingleDimensionalCopyConstructorAndArgsLocator does, but no need to put in the effort in a test...
            return new ConstructorAndArgs<MockStructure>(MockStructure.class.getConstructor(argsTypes), args);
        }

        public ConstructorAndArgs<MockStructure> getForIndices(final long[] indices) throws NoSuchMethodException {
            return getForIndex(indices[0]);
        }
    }
}
