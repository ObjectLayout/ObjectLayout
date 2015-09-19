package bench;

import org.ObjectLayout.ConstructionContext;
import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.ObjectLayout.examples.util.SAHashMap;

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

public class SAHashMapBench {

// Default value is 1048576 = 2 ^ 20 or ~ 1M

    @Param({"20"})
    public int lengthPow2;
    public int length; // Will be initialized is setup() to 1 << lengthPow2;
    public int lengthMask; // Will be initialized in setup() to length - 1;

    @Param({"false", "true"})
    public boolean RandomArrayWalk;

    StructuredArray<Element> structuredArray;

    public int[] linearNextIndexes;
    public int[] shuffledNextIndexes;
    public Integer[] integers;

    public SAHashMap<Integer,Element> saMap = new SAHashMap<Integer, Element>();
    public HashMap<Integer,Element> map = new HashMap<Integer, Element>();

    private Blackhole blackhole = new Blackhole();

    @Setup
    public void setup() throws NoSuchMethodException {
        length = 1 << lengthPow2;
        lengthMask = length - 1;

        integers = new Integer[length];
        linearNextIndexes = new int[length];
        for (int i = 0; i < length; i++) {
            integers[i] = new Integer(i);
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

        // populate hashmaps:
//        System.out.println();
        for (int i = 0; i < length; i++) {
            saMap.put(i, structuredArray.get(i));
            map.put(i, structuredArray.get(i));
            // Verify:
//            Element e;
//            if ((e = saMap.get(i)) != structuredArray.get(i)) {
//                System.out.println("saMap(" + i + ") == " + e +
//                        ", saMap.size() = " + saMap.size() + " (" + map.size() + ")");
//            }
//            if ((e = map.get(i)) != structuredArray.get(i)) {
//                System.out.println("map(" + i + ") == " + e);
//            }
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
    public void mapLoop() {
        long accumulator = 0;
        for (int i = 0 ; i < length; i++) {
            int nextIndex = map.get(integers[i]).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void mapDataDependentLoop() {
        long accumulator = 0;
        int index = 0 ;
        for (int i = 0 ; i < length; i++) {
            int nextIndex = map.get(integers[index]).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void saMapLoop() {
        long accumulator = 0;
        for (int i = 0 ; i < length; i++) {
            int nextIndex = saMap.get(integers[i]).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
        }
        blackhole.consume(accumulator);
    }

    @Benchmark
    public void saMapDataDependentLoop() {
        long accumulator = 0;
        int index = 0 ;
        for (int i = 0 ; i < length; i++) {
            int nextIndex = saMap.get(integers[index]).getNextIndex();
            accumulator = Element.simple_math(accumulator, nextIndex);
            index = nextIndex;
        }
        blackhole.consume(accumulator);
    }

    public static class Element extends ElementBase {
        static int[] intArray = new int[1];
        static final Class[] constructorArgTypes_Long_IntArray = {Long.TYPE, intArray.getClass()};
        static final Class[] constructorArgTypes_SA = {Element.class};

        // padding elements - to make sure that each element is greater than 2x L1 D-cache line
        private long l0,l1,l2,l3,l4,l5,l6,l7,l8,l9,l10,l11,l12,l13,l14,l15;

        public Element() {
        }

        public Element(final long index, final int[] nextIndexes) {
            super(index, nextIndexes);
        }

        public Element(Element src) {
            super(src);
        }
    }

    public static class ElementBase {

        private long index = -1;
        private int nextIndex = Integer.MIN_VALUE;

        public ElementBase() {
        }

        public ElementBase(final long index, final int[] nextIndexes) {
            this.index = index;
            this.nextIndex = nextIndexes[(int)index];
        }

        public ElementBase(ElementBase src) {
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

            final ElementBase that = (ElementBase)o;

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
}