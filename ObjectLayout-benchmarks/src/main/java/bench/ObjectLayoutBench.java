package bench;

import org.ObjectLayout.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/*
  Run all benchmarks:
    $ java -jar target/benchmarks.jar

  Run selected benchmarks:
    $ java -jar target/benchmarks.jar (regexp)

  Run the profiling (Linux only):
     $ java -Djmh.perfasm.events=cycles,cache-misses -jar target/benchmarks.jar -f 1 -prof perfasm
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
@State(Scope.Thread)

public class ObjectLayoutBench {

    StructuredArray<MockStructure> array;
    StructuredArrayOfMockStructure subclassedArray;
    GenericEncapsulatedArray<MockStructure> genericEncapsulatedArray;
    EncapsulatedArray encapsulatedArray;
    EncapsulatedRandomizedArray encapsulatedRandomizedArray;

    @Setup
    public void setup() throws NoSuchMethodException {
        final int length = 1000000;

        final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider =
                new DefaultMockCtorAndArgsProvider();

        array = StructuredArray.newInstance(MockStructure.class, ctorAndArgsProvider, length);
        subclassedArray = StructuredArrayOfMockStructure.newInstance(ctorAndArgsProvider, length);
        encapsulatedArray = new EncapsulatedArray(length);
        encapsulatedRandomizedArray = new EncapsulatedRandomizedArray(length);
        genericEncapsulatedArray =
                new GenericEncapsulatedArray<MockStructure>(
                        MockStructure.class.getConstructor(DefaultMockCtorAndArgsProvider.argsTypes), length);
    }

    // TODO: We should probably sink the values into Blackhole.consume,
    // instead of summing them up, and subjecting ourselves with loop optimizations.

    @Benchmark
    public long arrayLoopSumTest() {
        long sum = 0;
        for (int i = 0 ; i < array.getLength(); i++) {
            sum += array.get(i).getTestValue();
        }
        return sum;
    }

    @Benchmark
    public long subclassedArrayLoopSumTest() {
        long sum = 0;
        for (int i = 0 ; i < array.getLength(); i++) {
            sum += subclassedArray.get(i).getTestValue();
        }
        return sum;
    }

    @Benchmark
    public long loopGenericEncapsulatedArraySumTest() {
        long sum = 0;
        for (int i = 0 ; i < genericEncapsulatedArray.getLength(); i++) {
            sum += genericEncapsulatedArray.get(i).getTestValue();
        }
        return sum;
    }

    @Benchmark
    public long loopEncapsulatedArraySumTest() {
        long sum = 0;
        for (int i = 0 ; i < encapsulatedArray.getLength(); i++) {
            sum += encapsulatedArray.get(i).getTestValue();
        }
        return sum;
    }

    @Benchmark
    public long loopEncapsulatedRandomizedArraySumTest() {
        long sum = 0;
        for (int i = 0 ; i < encapsulatedRandomizedArray.getLength(); i++) {
            sum += encapsulatedRandomizedArray.get(i).getTestValue();
        }
        return sum;
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

    private static class DefaultMockCtorAndArgsProvider extends AbstractCtorAndArgsProvider<MockStructure>
    {

        static final Class[] argsTypes = {Long.TYPE, Long.TYPE};

        @Override
        public CtorAndArgs<MockStructure> getForContext(final ConstructionContext context) throws NoSuchMethodException {
            long indexSum = context.getIndex();
            Object[] args = {indexSum, indexSum * 2};
            // We could do this much more efficiently with atomic caching of a single allocated CtorAndArgs,
            // as CopyCtorAndArgsProvider does, but no need to put in the effort in a test...
            return new CtorAndArgs<MockStructure>(MockStructure.class.getConstructor(argsTypes), args);
        }

    }

    public static class StructuredArrayOfMockStructure extends StructuredArray<MockStructure> {
        public static StructuredArrayOfMockStructure newInstance(
                final CtorAndArgsProvider<MockStructure> ctorAndArgsProvider,final long length) {
            return StructuredArray.newSubclassInstance(
                    StructuredArrayOfMockStructure.class, MockStructure.class, ctorAndArgsProvider, length);
        }

    }

    static class EncapsulatedArray {
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

    static class EncapsulatedRandomizedArray {
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

    static class GenericEncapsulatedArray<E> {
        final E[] array;

        GenericEncapsulatedArray(Constructor<E> constructor, int length)
                throws NoSuchMethodException {
            array = (E[]) new Object[length];
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
}
