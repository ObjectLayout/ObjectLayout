package bench;

import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.ObjectLayout.ConstructionContext;
import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
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

  This benchmark creates a 1-D array of structures in 5 different ways
  - Java array of structs
  - Java array declared with the use of generic
  - Hava array with shuffled elements - to mimic layout of elements in arrays after many GC cycles
                                           when elements are referenced from multiple objects.
                                        In real life application we are more likely to see partial re-order
  - Structured Arrray
  - A subclass of Strructured Array

  For all 5 arrays this benchmark loops over all elements and does some operation. The reported score in number of
  operation per second, higher is better.

  There are several parameters to tune this benchmark

  length - number of elements in each array

  SamaLayout - when true, Structured Array is created first, and then all java arrays are initialized by the references
  to elements of Strcutured Array. This insures that memory access pattern is the same for all 5 benchmark loops and
  the only diffrence to be observed is how JIT transforms the main benchmark loop in each case.  When false, each of
  the 5 arrays is created independently.

  RandomArrayWalk - when true, the main loop accesses elements in array is a random order, otherwise elements are read
  sequentially. The main loop does this:  for (...) { val = array.get(index).val ; index = val + 1;}
  In a RandomArrayWalk case the val would always be a random number between 0 and length -1; otherwise
  array.get(index).val == index

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

// Default value is 1048576 = 2 ^ 20 or ~ 1M

    @Param({"20"})
    public int lengthPow2;
    public int length; // Will be initialized is setup() to 1 << lengthPow2;
    public int lengthMask; // Will be initialized in setup() to length - 1;

    @Param({"true"})
    public boolean SameLayout;

    @Param({"false", "true"})
    public boolean RandomArrayWalk;

    StructuredArray<Element> structuredArray;
    StructuredArrayOfMockStructure subclassedStructuredArray;
    GenericEncapsulatedArray<Element> encapsulatedArrayGeneric;
    EncapsulatedArray encapsulatedArray;
    EncapsulatedRandomizedArray encapsulatedArrayRandomized;

    public int[] linearNextIndexes;
    public int[] shuffledNextIndexes;

    private Blackhole blackhole = new Blackhole();

    public static int long2Int(long l) {
        return (int) Math.max(Math.min(Integer.MAX_VALUE, l), Integer.MIN_VALUE);
    }

    @Setup
    public void setup() throws NoSuchMethodException {
        length = 1 << lengthPow2;
        lengthMask = length - 1;

        linearNextIndexes = new int[length];
        for (int i = 0; i < length; i++) {
            linearNextIndexes[i] = (i + 1) & lengthMask;
        }
        shuffledNextIndexes = new int[length];
        shuffleNextIndexes(shuffledNextIndexes);

        final Object[] args = new Object[2];
        final CtorAndArgs<Element> ctorAndArgs =
                new CtorAndArgs<Element>(
                        Element.class.getConstructor(Element.constructorArgTypes_Long_IntArray), args);

        if (Element.class.getConstructor(Element.constructorArgTypes_Long_IntArray) == null) {
            System.out.println("Failed to get constructor");
        }

        final CtorAndArgsProvider<Element> ctorAndArgsProvider =
                new CtorAndArgsProvider<Element>() {
                    @Override
                    public CtorAndArgs<Element> getForContext(
                            ConstructionContext<Element> context) throws NoSuchMethodException {
                        args[0] = context.getIndex();
                        args[1] = RandomArrayWalk ? shuffledNextIndexes : linearNextIndexes;
                        return ctorAndArgs;
                    }
                };

        structuredArray = StructuredArray.newInstance(Element.class, ctorAndArgsProvider, length);
        subclassedStructuredArray = StructuredArrayOfMockStructure.newInstance(ctorAndArgsProvider, length);

        int sequenceLength;
        if ((sequenceLength = findSquenceLength(linearNextIndexes)) != length) {
            System.out.println("sequenceLength(linearNextIndexes) = " + sequenceLength +
                    " (!=length (" + length + "))");
        };
        if ((sequenceLength = findSquenceLength(shuffledNextIndexes)) != length) {
            System.out.println("sequenceLength(shuffledNextIndexes) = " + sequenceLength +
                    " (!=length (" + length + "))");
        }
        if ((sequenceLength = findSquenceLength(structuredArray)) != length) {
            System.out.println("sequenceLength(structuredArray) = " + sequenceLength +
                    " (!=length (" + length + "))");
        }

        if (SameLayout) {
            // use Sructured Array to initialize java arrays
            encapsulatedArray = new EncapsulatedArray(structuredArray);
            encapsulatedArrayRandomized = new EncapsulatedRandomizedArray(structuredArray,
                    shuffledNextIndexes, RandomArrayWalk);
            encapsulatedArrayGeneric =
                    new GenericEncapsulatedArray<Element>(
                            Element.class.getConstructor(Element.constructorArgTypes_SA), structuredArray);
        } else {
            encapsulatedArray = new EncapsulatedArray(long2Int(length),
                    RandomArrayWalk ? shuffledNextIndexes: linearNextIndexes);
            encapsulatedArrayRandomized = new EncapsulatedRandomizedArray(long2Int(length),
                    shuffledNextIndexes);
            encapsulatedArrayGeneric =
                    new GenericEncapsulatedArray<Element>(
                            Element.class.getConstructor(Element.constructorArgTypes_Long_IntArray),
                            long2Int(length), RandomArrayWalk ? shuffledNextIndexes: linearNextIndexes);
        }
    }

    static void shuffleNextIndexes(int[] array) {
        int length = array.length;
        int[] visitOrderArray = new int[length];
        Random generator = new Random(42);
        for (int i = 0; i < length; i++) {
            visitOrderArray[i] = i;
        }
        for (int i = length - 1; i >= 0; i--) {
            int j = generator.nextInt(i + 1);
            int temp = visitOrderArray[i];
            visitOrderArray[i] = visitOrderArray[j];
            visitOrderArray[j] = temp;
        }
        for (int i = 0; i < length - 1; i++) {
            array[visitOrderArray[i]] = visitOrderArray[i + 1];
        }
        array[visitOrderArray[length - 1]] = visitOrderArray[0];
    }

    @Benchmark
    public void structuredArrayDataDependentLoop() {
        long accumulator = 0;
        int index = 0 ;
        final long length = structuredArray.getLength();
        for (long i = 0 ; i < length; i++) {
            int nextIndex = structuredArray.get(index).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void structuredArrayLoop() {
        long accumulator = 0;
        final long length = structuredArray.getLength();
        for (long i = 0 ; i < length; i++) {
            int nextIndex = structuredArray.get(i).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void subclassedStructuredArrayDataDependentLoop() {
        long accumulator = 0;
        int index = 0 ;
        final long length = subclassedStructuredArray.getLength();
        for (long i = 0 ; i < length; i++) {
            int nextIndex = subclassedStructuredArray.get(index).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void subclassedStructuredArrayLoop() {
        long accumulator = 0;
        final long length = subclassedStructuredArray.getLength();
        for (long i = 0 ; i < length; i++) {
            int nextIndex = subclassedStructuredArray.get(i).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void genericArrayDataDependentLoop() {
        long accumulator = 0;
        int index = 0 ;
        final int length = encapsulatedArrayGeneric.getLength();
        for (int i = 0 ; i < length; i++) {
            int nextIndex = encapsulatedArrayGeneric.get(index).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void genericArrayLoop() {
        long accumulator = 0;
        final int length = encapsulatedArrayGeneric.getLength();
        for (int i = 0 ; i < length; i++) {
            int nextIndex = encapsulatedArrayGeneric.get(i).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void basicArrayLoopDataDependentLoop() {
        long accumulator = 0;
        int index = 0 ;
        final int length = encapsulatedArray.getLength();
        for (int i = 0 ; i < length; i++) {
            int nextIndex = encapsulatedArray.get(index).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void basicArrayLoopLoop() {
        long accumulator = 0;
        final int length = encapsulatedArray.getLength();
        for (int i = 0 ; i < length; i++) {
            int nextIndex = encapsulatedArray.get(i).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
         public void randomizedBasicDataDependentLoop() {
        long accumulator = 0;
        int index = 0 ;
        final int length = encapsulatedArrayRandomized.getLength();
        for (int i = 0 ; i < length; i++) {
            int nextIndex = encapsulatedArrayRandomized.get(index).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void randomizedBasicLoop() {
        long accumulator = 0;
        final int length = encapsulatedArrayRandomized.getLength();
        for (int i = 0 ; i < length; i++) {
            int nextIndex = encapsulatedArrayRandomized.get(i).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    public static class Element {

        static int[] intArray = new int[1];
        static final Class[] constructorArgTypes_Long_IntArray = {Long.TYPE, intArray.getClass()};
        static final Class[] constructorArgTypes_SA = {Element.class};

        private long index = -1;
        private int nextIndex = Integer.MIN_VALUE;

        // padding elements - to make sure that each element is greater than 2x L1 D-cache line
        private long l0,l1,l2,l3,l4,l5,l6,l7,l8,l9,l10,l11,l12,l13,l14,l15;

        public Element() {
        }

        public Element(final long index, final int[] nextIndexes) {
            this.index = index;
            this.nextIndex = nextIndexes[(int)index];
        }

        public Element(Element src) {
            this.index = src.index;
            this.nextIndex = src.nextIndex;
        }

        public long getIndex() {
            return index;
        }

        public void setIndex(final long index) {
            this.index = index;
        }

        public int getNextIndex() {
            return nextIndex;
        }

        public void setNextIndex(final int nextIndex) {
            this.nextIndex = nextIndex;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Element that = (Element)o;

            return index == that.index && nextIndex == that.nextIndex;
        }

        @Override
        public int hashCode() {
            int result = (int)(index ^ (index >>> 32));
            result = 31 * result + (int)(nextIndex ^ (nextIndex >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "MockStructure{" +
                    "index=" + index +
                    ", nextIndex=" + nextIndex +
                    '}';
        }

        public static long simple_math(long param1, int param2) {
            return  param1 ^ param2;
        }
    }

    public static class StructuredArrayOfMockStructure extends StructuredArray<Element> {
        public static StructuredArrayOfMockStructure newInstance(
                final CtorAndArgsProvider<Element> ctorAndArgsProvider,final long length) {
            return StructuredArray.newInstance(
                    StructuredArrayOfMockStructure.class, Element.class, length, ctorAndArgsProvider);
        }

    }

    static class EncapsulatedArray {
        final Element[] array;

        EncapsulatedArray(int length, int[] nextIndexes) {
            array = new Element[length];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Element(i, nextIndexes);
            }
        }

        EncapsulatedArray(StructuredArray<Element> sa_array) {
            int length=(int)sa_array.getLength();
            final Element[] a;
            a = new Element[length];
            array = a;
            for (int i = 0; i < length; i++) {
                array[i] = sa_array.get(i);
            }
        }

        Element get(final int index) {
            return array[index];
        }

        int getLength() {
            return array.length;
        }
    }

    static class EncapsulatedRandomizedArray {
        final Element[] array;

        EncapsulatedRandomizedArray(int length, int[] nextIndexes) {
            array = new Element[length];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Element(i, nextIndexes);
            }
        }
        EncapsulatedRandomizedArray(StructuredArray<Element> sa_array, int[] nextIndexes, boolean alreadyShuffled) {
            int length = (int)sa_array.getLength();
            int lengthMask = length - 1;  // Strongly assuming powers of 2 here
            final Element[] a;
            a = new Element[length];
            array = a;
            if (alreadyShuffled) {
                for (int i = 0; i < length; i++) {
                    array[i] = sa_array.get(i);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    int nextIndex = nextIndexes[i];
                    // figure out which saIndex with contain this nextIndex:
                    int saIndex = (nextIndex - 1) & lengthMask;
                    array[i] = sa_array.get(saIndex);
                }
            }
            int sequenceLength;
            if ((sequenceLength = findSquenceLength(array)) != length) {
                System.out.println("EncapsulatedRandomizedArray sequenceLength(array) = " + sequenceLength +
                        " (!=length (" + length + ")");
            }
        }

        Element get(final int index) {
            return array[index];
        }

        int getLength() {
            return array.length;
        }
    }

    static class GenericEncapsulatedArray<E> {
        final E[] array;

        GenericEncapsulatedArray(Constructor<E> constructor, int length, int[] nextIndexes)
                throws NoSuchMethodException {
            @SuppressWarnings("unchecked")
            final E[] a = (E[]) new Object[length];
            array = a;
            try{
                for (int i = 0; i < array.length; i++) {
                    array[i] = constructor.newInstance(i, nextIndexes);
                }
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        GenericEncapsulatedArray(Constructor<E> constructor, StructuredArray<E> sa_array) {
            int length = (int) sa_array.getLength();
            @SuppressWarnings("unchecked")
            final E[] a = (E[]) new Object[length];
            array = a;
            for (int i = 0; i < length; i++) {
                array[i] =  sa_array.get(i);
            }
        }

        E get(final int index) {
            return array[index];
        }

        int getLength() {
            return array.length;
        }

        public static void main(String[] args) throws RunnerException {

            Options opt = new OptionsBuilder()
                    .include(ObjectLayoutBench.class.getSimpleName())
                    .param("arg", "1048576") // Use this to selectively constrain/override parameters
                    .param("SameLayout", "true")
                    .param("RandomArrayWalk", "false")
                    .build();


            new Runner(opt).run();

        }
    }

    // Methods for determining the sequence length within an array (if walked by nextIndex):

    static int findSquenceLength(StructuredArray<Element> sa) {
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
//        System.out.format("*** SA<E> Sequence length = %d (%7.5fx of %d)\n",
//                sequenceLength, (sequenceLength * 1.0 / length), length);
        return sequenceLength;
    }

    static int findSquenceLength(Element[] a) {
        int length = (int) a.length;
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
//        System.out.format("*** E[] Sequence length = %d (%7.5fx of %d)\n",
//                sequenceLength, (sequenceLength * 1.0 / length), length);
        return sequenceLength;
    }

    static int findSquenceLength(int[] a) {
        int length = a.length;
        boolean visited[] = new boolean[length];
        int index = 0;
        int sequenceLength = 0;
//        System.out.println("\nArrays contents: ");
//        for (int i = 0; i < length; i++) {
//            System.out.print(i + "->" + a[i] + ",");
//        }
//        System.out.println("\nSequence through array:");
        for (int i = 0; i < length; i++) {
            int prevIndex = index;
            index = a[index];
            if (visited[index]) {
                break;
            }
//            System.out.print(prevIndex + "->" + index + ",");
            sequenceLength++;
            visited[index] = true;
        }
//        System.out.format("*** int[] Sequence length = %d (%7.5fx of %d)\n",
//                sequenceLength, (sequenceLength * 1.0 / length), length);
        return sequenceLength;
    }
}