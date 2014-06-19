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

public class StructuredArrayPerfTest {
    StructuredArray<MockStructure> array;
    StructuredArrayOfMockStructure subclassedArray;
    GenericEncapsulatedArray<MockStructure> genericEncapsulatedArray;
    EncapsulatedArray encapsulatedArray;

    class EncapsulatedArray {
        final MockStructure[] array;

        EncapsulatedArray(int length) {
            array = new MockStructure[length];
            for (int i = 0; i < array.length; i++) {
                array[i] = new MockStructure(i, i*2);
            }
        }

        MockStructure get(final int index) {
            return array[index];
        }

        int getLength() {
            return array.length;
        }
    }

    class GenericEncapsulatedArray<E> {
        final E[] array;

        GenericEncapsulatedArray(CtorAndArgsProvider ctorAndArgsProvider, int length)  throws NoSuchMethodException {
            long indexes[] = new long[1];
            array = (E[]) new Object[length];
            try {
                for (int i = 0; i < array.length; i++) {
                    indexes[0] = i;
                    final CtorAndArgs<E> ctorAndArgs = ctorAndArgsProvider.getForIndex(indexes);
                    final Constructor<E> constructor = ctorAndArgs.getConstructor();
                    array[i] = constructor.newInstance(ctorAndArgs.getArgs());
                }
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        E get(final int index) {
            return array[index];
        }

        int getLength() {
            return array.length;
        }
    }

    long arrayLoopSumTest() {
        long sum = 0;
        for (int i = 0 ; i < array.getLength(); i++) {
            sum += array.get(i).getTestValue();
        }
        return sum;
    }

    long subclassedArrayLoopSumTest() {
        long sum = 0;
        for (int i = 0 ; i < array.getLength(); i++) {
            sum += subclassedArray.get(i).getTestValue();
        }
        return sum;
    }

    long loopGenericEncapsulatedArraySumTest() {
        long sum = 0;
        for (int i = 0 ; i < genericEncapsulatedArray.getLength(); i++) {
            sum += genericEncapsulatedArray.get(i).getTestValue();
        }
        return sum;
    }

    long loopEncapsulatedArraySumTest() {
        long sum = 0;
        for (int i = 0 ; i < encapsulatedArray.getLength(); i++) {
            sum += encapsulatedArray.get(i).getTestValue();
        }
        return sum;
    }

    public void testLoop(int length) {

        long startTime4 = System.nanoTime();
        long sum4 = loopEncapsulatedArraySumTest();
        long endTime4 = System.nanoTime();
        double loopsPerSec4 = 1000 * (double) length / (endTime4 - startTime4);

        long startTime3 = System.nanoTime();
        long sum3 = loopGenericEncapsulatedArraySumTest();
        long endTime3 = System.nanoTime();
        double loopsPerSec3 = 1000 * (double) length / (endTime3 - startTime3);

        long startTime2 = System.nanoTime();
        long sum2 = subclassedArrayLoopSumTest();
        long endTime2 = System.nanoTime();
        double loopsPerSec2 = 1000 * (double) length / (endTime2 - startTime2);

        long startTime1 = System.nanoTime();
        long sum1 = arrayLoopSumTest();
        long endTime1 = System.nanoTime();
        double loopsPerSec1 = 1000 * (double) length / (endTime1 - startTime1);

        System.out.println("StructuredArray: (" + loopsPerSec1 +
                "M), SubclassedSA: (" + loopsPerSec2 +
                "M), GenericEncapsulatedArray: (" + loopsPerSec3 +
                "M), EncapsulatedArray: (" + loopsPerSec4 +
                "M) cksum = " + (sum1 + sum2 + sum3 + sum4));
    }

    @Test
    public void testLoopingSpeeds() throws NoSuchMethodException {
        final int length = 1000000;

        final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider =
                new DefaultMockCtorAndArgsProvider();


        array = StructuredArray.newInstance(ctorAndArgsProvider, length);
        subclassedArray = StructuredArrayOfMockStructure.newInstance(ctorAndArgsProvider, length);
        encapsulatedArray = new EncapsulatedArray(length);
        genericEncapsulatedArray = new GenericEncapsulatedArray<MockStructure>(ctorAndArgsProvider, length);

        for (int i = 0; i < 10; i++) {
            testLoop(length);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {

            }
        }
    }

    public static void main(String[] args) {
        try {
            StructuredArrayPerfTest test = new StructuredArrayPerfTest();
            // Useful for keeping program alive and active when doing drill-down browsing in an interactive profiler:
            while (true) {
                test.testLoopingSpeeds();
            }
        } catch (Exception ex) {
        }
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
                if ((++cursors[cursorDimension]) < lengths[cursorDimension])
                    break;
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
                if ((++cursors[cursorDimension]) < lengths[cursorDimension])
                    break;
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

        @Override
        public CtorAndArgs<MockStructure> getForIndex(long... indices) throws NoSuchMethodException {
            long indexSum = 0;
            for (long index : indices) {
                indexSum += index;
            }
            Object[] args = {indexSum, indexSum * 2};
            // We could do this much more efficiently with atomic caching of a single allocated CtorAndArgs,
            // as CopyCtorAndArgsProvider does, but no need to put in the effort in a test...
            return new CtorAndArgs<MockStructure>(MockStructure.class.getConstructor(argsTypes), args);
        }

    }

    public static class StructuredArrayOfMockStructure extends StructuredArray<MockStructure> {
        public static StructuredArrayOfMockStructure newInstance(
                final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider,final long length) {
            return (StructuredArrayOfMockStructure)
                    StructuredArray.newSubclassInstance(StructuredArrayOfMockStructure.class, ctorAndArgsProvider, length);
        }

    }
}
