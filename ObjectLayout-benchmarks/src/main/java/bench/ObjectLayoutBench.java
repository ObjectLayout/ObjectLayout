package bench;

import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.ObjectLayout.ConstructionContext;
import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/*
  This benchmark creates a one-dimensional array of objects in the following ways:
  - Structured array
  - Encapsulated Java array
  - Encapsulated Java array that references the corresponding elements of the previously created
    structured array. This is to ensure that the memory access pattern will be the same as for
    the structured array.
  - Encapsulated Java array declared with the use of generics
  - Encapsulated Java array declared with the use of generics that references the corresponding
    elements of the previously created structured array. This is to ensure that the memory access
    pattern will be the same as for the structured array.
  - Encapsulated Java array with shuffled elements. This is to mimic the layout of elements in
    arrays after many GC cycles when elements are referenced from multiple objects. In real life
    application we are more likely to see partial re-order.

  For all these arrays this benchmark loops over all elements and does some operation.
  The reported score is the number of operations per second, higher is better.

  There are several parameters to tune this benchmark.

  LengthPow2 - Log-Base-Two of the number of elements in each array

  RandomArrayWalk - When false, the main loop accesses elements in an array in the sequential
  order. When true, elements are accessed in an order that is a random cyclic permutation
  of the sequential order.

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
    @Param({"20"})
    public int LengthPow2;

    @Param({"false", "true"})
    public boolean RandomArrayWalk;

    private static final int shufflingSeed = 37;
    private static final int randomizingSeed = 53;

    private int length;
    private int lengthMask;

    private BasicArray basicArrayDifferentLayout;
    private BasicArray basicArraySameLayout;
    private GenericArray<Element> genericArrayDifferentLayout;
    private GenericArray<Element> genericArraySameLayout;
    private RandomizedBasicArray randomizedBasicArray;
    private StructuredArray<Element> structuredArray;

    private int[] linearNextIndexes;
    private int[] shuffledNextIndexes;

    private Blackhole blackhole = new Blackhole();

    @Setup
    public void setup() throws NoSuchMethodException {
        length = 1 << LengthPow2;
        lengthMask = length - 1;

        initNextIndexes();

        final Object[] args = new Object[2];
        final CtorAndArgs<Element> ctorAndArgs = new CtorAndArgs<Element>(
                Element.class.getConstructor(Element.constructorArgTypes_Long_IntArray), args);
        CtorAndArgsProvider<Element> ctorAndArgsProvider =
                new CtorAndArgsProvider<Element>() {
                    @Override
                    public CtorAndArgs<Element> getForContext(ConstructionContext<Element> context)
                            throws NoSuchMethodException {
                        args[0] = context.getIndex();
                        args[1] = RandomArrayWalk ? shuffledNextIndexes : linearNextIndexes;
                        return ctorAndArgs;
                    }
                };
        structuredArray = StructuredArray.newInstance(Element.class, ctorAndArgsProvider, length);

        basicArrayDifferentLayout = new BasicArray(length,
                RandomArrayWalk ? shuffledNextIndexes : linearNextIndexes);
        basicArraySameLayout = new BasicArray(structuredArray);
        genericArrayDifferentLayout = new GenericArray<Element>(
                Element.class.getConstructor(Element.constructorArgTypes_Long_IntArray),
                length, RandomArrayWalk ? shuffledNextIndexes : linearNextIndexes);
        genericArraySameLayout = new GenericArray<Element>(structuredArray);
        randomizedBasicArray = new RandomizedBasicArray(length,
                RandomArrayWalk ? shuffledNextIndexes : linearNextIndexes);
    }

    private void initNextIndexes() {
        linearNextIndexes = new int[length];
        for (int i = 0; i < length - 1; i++) {
            linearNextIndexes[i] = i + 1;
        }
        linearNextIndexes[length - 1] = 0;

        shuffledNextIndexes = new int[length];
        int[] visitOrder = new int[length];
        Random generator = new Random(shufflingSeed);
        for (int i = 0; i < length; i++) {
            visitOrder[i] = i;
        }
        for (int i = length - 1; i > 0; i--) {
            int j = generator.nextInt(i + 1);
            int temp = visitOrder[i];
            visitOrder[i] = visitOrder[j];
            visitOrder[j] = temp;
        }
        for (int i = 0; i < length - 1; i++) {
            shuffledNextIndexes[visitOrder[i]] = visitOrder[i + 1];
        }
        shuffledNextIndexes[visitOrder[length - 1]] = visitOrder[0];
    }

    private static int getSequenceLength(Element[] a) {
        int length = a.length;
        boolean visited[] = new boolean[length];
        int index = 0;
        int sequenceLength = 0;
        for (int i = 0; i < length; i++) {
            int prevIndex = index;
            index = a[index].getNextIndex();
            if (visited[index]) {
                break;
            }
            sequenceLength++;
            visited[index] = true;
        }
        return sequenceLength;
    }

    private static int getSequenceLength(StructuredArray<Element> sa) {
        int length = (int) sa.getLength();
        boolean visited[] = new boolean[length];
        int index = 0;
        int sequenceLength = 0;
        for (int i = 0; i < length; i++) {
            int prevIndex = index;
            index = sa.get(index).getNextIndex();
            if (visited[index]) {
                break;
            }
            sequenceLength++;
            visited[index] = true;
        }
        return sequenceLength;
    }

    // Benchmarks for basic arrays

    @Benchmark
    public void basicArrayDifferentLayoutDataDependentLoop() {
        long accumulator = 0;
        int index = 0;
        int length = basicArrayDifferentLayout.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = basicArrayDifferentLayout.get(index).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void basicArrayDifferentLayoutLoop() {
        long accumulator = 0;
        int length = basicArrayDifferentLayout.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = basicArrayDifferentLayout.get(i).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void basicArraySameLayoutDataDependentLoop() {
        long accumulator = 0;
        int index = 0;
        int length = basicArraySameLayout.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = basicArraySameLayout.get(index).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void basicArraySameLayoutLoop() {
        long accumulator = 0;
        int length = basicArraySameLayout.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = basicArraySameLayout.get(i).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    // Benchmarks for generic arrays

    @Benchmark
    public void genericArrayDifferentLayoutDataDependentLoop() {
        long accumulator = 0;
        int index = 0;
        int length = genericArrayDifferentLayout.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = genericArrayDifferentLayout.get(index).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void genericArrayDifferentLayoutLoop() {
        long accumulator = 0;
        int length = genericArrayDifferentLayout.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = genericArrayDifferentLayout.get(i).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void genericArraySameLayoutDataDependentLoop() {
        long accumulator = 0;
        int index = 0;
        int length = genericArraySameLayout.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = genericArraySameLayout.get(index).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void genericArraySameLayoutLoop() {
        long accumulator = 0;
        int length = genericArraySameLayout.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = genericArraySameLayout.get(i).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    // Benchmarks for randomized basic arrays

    @Benchmark
    public void randomizedBasicArrayDataDependentLoop() {
        long accumulator = 0;
        int index = 0;
        int length = randomizedBasicArray.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = randomizedBasicArray.get(index).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void randomizedBasicArrayLoop() {
        long accumulator = 0;
        int length = randomizedBasicArray.getLength();
        for (int i = 0; i < length; i++) {
            int nextIndex = randomizedBasicArray.get(i).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    // Benchmarks for structured arrays

    @Benchmark
    public void structuredArrayDataDependentLoop() {
        long accumulator = 0;
        int index = 0;
        long length = structuredArray.getLength();
        for (long i = 0; i < length; i++) {
            int nextIndex = structuredArray.get(index).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void structuredArrayLoop() {
        long accumulator = 0;
        long length = structuredArray.getLength();
        for (long i = 0; i < length; i++) {
            int nextIndex = structuredArray.get(i).getNextIndex();
            accumulator = doSimpleMath(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    // End of benchmarks

    private static long doSimpleMath(long param1, int param2) {
        return param1 ^ param2;
    }

    public static class ElementBase {
        private long index = -1;
        private int nextIndex = -1;

        public ElementBase(long index, int[] nextIndexes) {
            this.index = index;
            this.nextIndex = nextIndexes[(int) index];
        }

        public ElementBase(ElementBase src) {
            this.index = src.index;
            this.nextIndex = src.nextIndex;
        }

        public long getIndex() {
            return index;
        }

        public void setIndex(long index) {
            this.index = index;
        }

        public int getNextIndex() {
            return nextIndex;
        }

        public void setNextIndex(int nextIndex) {
            this.nextIndex = nextIndex;
        }
    }

    public static class Element extends ElementBase {
        static final Class[] constructorArgTypes_Long_IntArray = { long.class, int[].class };
        static final Class[] constructorArgTypes_Element = { Element.class };

        // Padding elements to make sure that each element is greater than
        // 2 x L1 D-Cache line
        private long l0, l1, l2, l3, l4, l5, l6, l7;
        private long l8, l9, l10, l11, l12, l13, l14, l15;

        public Element(long index, int[] nextIndexes) {
            super(index, nextIndexes);
        }

        public Element(Element src) {
            super(src);
        }
    }

    public static class BasicArray {
        private Element[] array;

        public BasicArray(int length, int[] nextIndexes) {
            array = new Element[length];
            for (int i = 0; i < length; i++) {
                array[i] = new Element(i, nextIndexes);
            }
        }

        public BasicArray(StructuredArray<Element> sa) {
            int length = (int) sa.getLength();
            array = new Element[length];
            for (int i = 0; i < length; i++) {
                array[i] = sa.get(i);
            }
        }

        public int getLength() {
            return array.length;
        }

        public Element get(int index) {
            return array[index];
        }
    }

    public static class GenericArray<E> {
        private E[] array;

        public GenericArray(Constructor<E> constructor, int length, int[] nextIndexes) {
            @SuppressWarnings("unchecked")
            final E[] a = (E[]) new Object[length];
            array = a;
            try {
                for (int i = 0; i < length; i++) {
                    array[i] = constructor.newInstance(i, nextIndexes);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public GenericArray(StructuredArray<E> sa) {
            int length = (int) sa.getLength();
            @SuppressWarnings("unchecked")
            final E[] a = (E[]) new Object[length];
            array = a;
            for (int i = 0; i < length; i++) {
                array[i] = sa.get(i);
            }
        }

        public int getLength() {
            return array.length;
        }

        public E get(int index) {
            return array[index];
        }
    }

    public static class RandomizedBasicArray {
        private Element[] array;

        public RandomizedBasicArray(int length, int[] nextIndexes) {
            array = new Element[length];
            for (int i = 0; i < length; i++) {
                array[i] = new Element(i, nextIndexes);
            }

            Random generator = new Random(randomizingSeed);
            for (int i = length - 1; i > 0; i--) {
                int j = generator.nextInt(i + 1);
                Element temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }

            for (int i = 0; i < length; i++) {
                array[i].setNextIndex(nextIndexes[i]);
            }
        }

        public int getLength() {
            return array.length;
        }

        public Element get(int index) {
            return array[index];
        }
    }
}
