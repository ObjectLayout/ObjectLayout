/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.StructuredArray;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StructuredArrayOfAtomicLongTest {

    @Test
    public void shouldInitializeToCorrectValues() {
        final long length = 1444;
        final StructuredArrayOfAtomicLong array =
                StructuredArrayOfAtomicLong.newInstance(length);

        initSumValues(array);

        assertCorrectVariableInitialisation(length, array);
    }

    @Test
    public void shouldIterateOverArray() {
        final long length = 1444;
        final StructuredArrayOfAtomicLong array =
                StructuredArrayOfAtomicLong.newInstance(length);

        initSumValues(array);

        long sum = 0;
        long elementCount = 0;
        long indexSum = 0;
        long index = 0;
        for (final AtomicLong atomicLong : array) {
            indexSum += index++;
            assertThat(valueOf(atomicLong.get()), is(valueOf(indexSum)));
            sum += indexSum;
            elementCount++;
        }

        long sum2 = 0;
        long elementCount2 = 0;
        for (final AtomicLong atomicLong : array) {
            sum2 += atomicLong.get();
            elementCount2++;
        }

        assertThat(valueOf(elementCount), is(valueOf(array.getLength())));
        assertThat(valueOf(sum), is(valueOf(sum2)));
        assertThat(valueOf(elementCount), is(valueOf(elementCount2)));
    }

    @Test
    public void shouldIterateOverArrayAndResetAgain() throws NoSuchMethodException {
        final long length = 2456;
        final StructuredArrayOfAtomicLong array =
                StructuredArrayOfAtomicLong.newInstance(length);

        initSumValues(array);

        long index = 0;
        long indexSum = 0;
        final StructuredArrayOfAtomicLong.ElementIterator iter = array.iterator();
        while (iter.hasNext()) {
            final AtomicLong atomicLong = iter.next();
            indexSum += index++;
            assertThat(valueOf(atomicLong.get()), is(valueOf(indexSum)));
        }

        iter.reset();
        indexSum = index = 0;
        while (iter.hasNext()) {
            final AtomicLong atomicLong = iter.next();
            indexSum += index++;
            assertThat(valueOf(atomicLong.get()), is(valueOf(indexSum)));
        }

        assertThat(valueOf(index), is(valueOf(length)));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowOutOfBoundExceptionForAccessesOutOfBounds() throws NoSuchMethodException {
        final long length = 111;
        final StructuredArrayOfAtomicLong array =
                StructuredArrayOfAtomicLong.newInstance(length);

        array.get(length);
    }


    @Test
    public void shouldNotThrowIncompatibleTypeExceptionForGetsOfProperTypes() throws NoSuchMethodException {
        final StructuredArrayOfAtomicLong array =
                StructuredArrayOfAtomicLong.newInstance(111);
        array.get(2);
    }

    @Test
    public void shouldCopyRegionLeftInArray() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArrayOfAtomicLong array =
                StructuredArrayOfAtomicLong.newInstance(length);

        initLinearValues(array);

        StructuredArray.shallowCopy(array, 4, array, 3, 2, false);

        assertThat(valueOf(array.get(3).get()), is(valueOf(4)));
        assertThat(valueOf(array.get(4).get()), is(valueOf(5)));
        assertThat(valueOf(array.get(5).get()), is(valueOf(5)));
    }

    @Test
    public void shouldCopyRegionRightInArray() throws NoSuchMethodException {
        final long length = 11;
        final StructuredArrayOfAtomicLong array =
                StructuredArrayOfAtomicLong.newInstance(length);

        initLinearValues(array);

        StructuredArray.shallowCopy(array, 5, array, 6, 2, false);

        assertThat(valueOf(array.get(5).get()), is(valueOf(5)));
        assertThat(valueOf(array.get(6).get()), is(valueOf(5)));
        assertThat(valueOf(array.get(7).get()), is(valueOf(6)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToConstructDirectly() throws NoSuchMethodException {
        final StructuredArrayOfAtomicLong array = new StructuredArrayOfAtomicLong();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Test support below
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void assertCorrectVariableInitialisation(final long length,
                                             final StructuredArrayOfAtomicLong array) {
        assertThat(valueOf(array.getLength()), is(valueOf(length)));
        assertTrue(array.getElementClass() == AtomicLong.class);

        long indexSum = 0;

        for (long index = 0; index < length; index++) {
            indexSum += index;
            assertThat("index: " + index + ": ", valueOf(array.get(index).get()), is(valueOf(indexSum)));
        }
    }

    private void initSumValues(final StructuredArrayOfAtomicLong array) {
        long length = array.getLength();
        long indexSum = 0;

        for (long index = 0; index < length; index++) {
            indexSum += index;
            array.get(index).set(indexSum);
        }
    }

    private void initLinearValues(final StructuredArrayOfAtomicLong array) {
        long length = array.getLength();

        for (long index = 0; index < length; index++) {
            array.get(index).set(index);
        }
    }

    public long accessArrayElementValue(StructuredArrayOfAtomicLong array, long index) {
        return array.get(index).get();
    }

}
