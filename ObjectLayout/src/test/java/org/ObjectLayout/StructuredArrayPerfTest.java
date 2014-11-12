/*
* Written by Gil Tene and Martin Thompson, and released to the public domain,
* as explained at http://creativecommons.org/publicdomain/zero/1.0/
*/

package org.ObjectLayout;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Random;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StructuredArrayPerfTest {
    StructuredArray<MockStructure> array;
    StructuredArrayOfMockStructure subclassedArray;
    GenericEncapsulatedArray<MockStructure> genericEncapsulatedArray;
    EncapsulatedArray encapsulatedArray;
    EncapsulatedRandomizedArray encapsulatedRandomizedArray;

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

    class EncapsulatedRandomizedArray {
        final MockStructure[] array;

        EncapsulatedRandomizedArray(int length) {
            array = new MockStructure[length];
            for (int i = 0; i < array.length; i++) {
                array[i] = new MockStructure(i, i*2);
            }
            // swap elements around randomly
            Random generator = new Random();
            for (int i = 0; i < array.length; i++) {
                int target = generator.nextInt(array.length);
                MockStructure temp = array[target];
                array[target] = array[i];
                array[i] = temp;
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

        GenericEncapsulatedArray(Constructor<E> constructor, int length) {
            @SuppressWarnings("unchecked")
            final E[] a = (E[]) new Object[length];
            array = a;
            try {
                for (int i = 0; i < array.length; i++) {
                    array[i] = constructor.newInstance(i, i * 2);
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

    long loopEncapsulatedRandomizedArraySumTest() {
        long sum = 0;
        for (int i = 0 ; i < encapsulatedRandomizedArray.getLength(); i++) {
            sum += encapsulatedRandomizedArray.get(i).getTestValue();
        }
        return sum;
    }

    public void testLoop(int length) {
        long startTime5 = System.nanoTime();
        long sum5 = loopEncapsulatedRandomizedArraySumTest();
        long endTime5 = System.nanoTime();
        double loopsPerSec5 = 1000 * (double) length / (endTime5 - startTime5);

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
                "M), EncapsulatedRandomizedArray: (" + loopsPerSec5 +
                "M) cksum = " + (sum1 + sum2 + sum3 + sum4 + sum5));
    }

    @Test
    public void testLoopingSpeeds() throws NoSuchMethodException {
        final int length = 1000000;

        final Object[] args = new Object[2];
        final CtorAndArgs<MockStructure> ctorAndArgs =
                new CtorAndArgs<MockStructure>(
                        MockStructure.class.getConstructor(MockStructure.constructorArgTypes), args);


        final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider =
                new CtorAndArgsProvider<MockStructure>() {
                    @Override
                    public CtorAndArgs<MockStructure> getForContext(
                            ConstructionContext<MockStructure> context) throws NoSuchMethodException {
                        args[0] = context.getIndex();
                        args[1] = context.getIndex() * 2;
                        return ctorAndArgs;
                    }
                };


        array = StructuredArray.newInstance(MockStructure.class, ctorAndArgsProvider, length);
        subclassedArray = StructuredArrayOfMockStructure.newInstance(ctorAndArgsProvider, length);
        encapsulatedArray = new EncapsulatedArray(length);
        encapsulatedRandomizedArray = new EncapsulatedRandomizedArray(length);
        genericEncapsulatedArray =
                new GenericEncapsulatedArray<MockStructure>(
                        MockStructure.class.getConstructor(MockStructure.constructorArgTypes), length);
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

    public static class MockStructure {

        static final Class[] constructorArgTypes = {Long.TYPE, Long.TYPE};

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

    public static class StructuredArrayOfMockStructure extends StructuredArray<MockStructure> {
        public static StructuredArrayOfMockStructure newInstance(
                final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider,final long length) {
            return StructuredArray.newInstance(
                    StructuredArrayOfMockStructure.class, MockStructure.class, length, ctorAndArgsProvider);
        }

    }
}
