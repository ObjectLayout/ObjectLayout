package org.ObjectLayoutApiTests;

import org.ObjectLayout.*;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PrimitiveLongArrayTest {

    public static class ColoredLongArray extends PrimitiveLongArray {
        private final String color;

        private static Constructor<ColoredLongArray> constructor;

        static {
            try {
                constructor = ColoredLongArray.class.getConstructor(new Class[]{String.class});
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }

        public static ColoredLongArray newInstance(String color, final int length) {
            return newInstance(length, constructor, color);
        }

        public ColoredLongArray(String color) {
            this.color = color;
        }

        public ColoredLongArray(ColoredLongArray sourceArray) {
            super(sourceArray);
            color = sourceArray.color;
        }

        public String getColor() {
            return color;
        }
    }
    
    @Test
    public void testColoredLongArray() throws Exception {
        ColoredLongArray blueArray = ColoredLongArray.newInstance("Blue", 10);
        ColoredLongArray redArray = ColoredLongArray.newInstance("Red", 20);

        for (int i = 0; i < redArray.getLength(); i++) {
            redArray.set(i, i);
        }

        for (int i = 0; i < blueArray.getLength(); i++) {
            blueArray.set(i, redArray.get(i));
        }

        assertTrue(redArray.getColor().equals("Red"));
        assertTrue(blueArray.getColor().equals("Blue"));
    }

    @Test
    public void testCopyOfColoredLongArray() throws Exception {
        ColoredLongArray blueArray = ColoredLongArray.newInstance("Blue", 10);
        ColoredLongArray redArray = ColoredLongArray.newInstance("Red", 20);

        for (int i = 0; i < redArray.getLength(); i++) {
            redArray.set(i, i);
        }

        for (int i = 0; i < blueArray.getLength(); i++) {
            blueArray.set(i, redArray.get(i));
        }

        ColoredLongArray blueArrayCopy = ColoredLongArray.copyInstance(blueArray);

        assertTrue(redArray.getColor().equals("Red"));
        assertTrue(blueArray.getColor().equals("Blue"));
        assertTrue(blueArrayCopy.getColor().equals("Blue"));
        assertTrue(Arrays.equals(blueArray.asArray(), blueArrayCopy.asArray()));
    }

    @Test
    public void testColoredLongArrayBuilder() throws Exception {
        PrimitiveArrayBuilder<ColoredLongArray> builder = new PrimitiveArrayBuilder<ColoredLongArray>(
                ColoredLongArray.class, 50);
        ColoredLongArray blueArray = builder.arrayCtorAndArgs(ColoredLongArray.constructor, "Blue").build();
        ColoredLongArray redArray = builder.arrayCtorAndArgs(ColoredLongArray.constructor, "Red").build();

        for (int i = 0; i < redArray.getLength(); i++) {
            redArray.set(i, i);
        }

        for (int i = 0; i < blueArray.getLength(); i++) {
            blueArray.set(i, redArray.get(i));
        }

        assertTrue(redArray.getColor().equals("Red"));
        assertTrue(blueArray.getColor().equals("Blue"));
        assertTrue(redArray.getLength() == 50);
        assertTrue(Arrays.equals(redArray.asArray(), blueArray.asArray()));
    }

    @Test
    public void testStructuredArrayOfColoredLongArrays() throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        StructuredArray<ColoredLongArray> a =
                new StructuredArrayBuilder(StructuredArray.class,
                        new PrimitiveArrayBuilder(ColoredLongArray.class, 20).
                                arrayCtorAndArgs(ColoredLongArray.constructor, "Yellow"),
                        50).
                        build();
        assertThat(a.getLength(), is(50L));
        assertThat(a.get(0).getLength(), is(20L));
        assertThat(a.get(1).getColor(), is("Yellow"));
    }

    @Test(expected = RuntimeException.class)
    public void testStructuredArrayOfColoredLongArrays_shouldFail() {
        // Should fail to find default constructor:
        @SuppressWarnings("unchecked")
        StructuredArray<ColoredLongArray> a =
                new StructuredArrayBuilder(StructuredArray.class,
                        new PrimitiveArrayBuilder(ColoredLongArray.class, 20),
                        50).
                        build();
    }

    @Test
    public void testStructuredArrayOfColoredLongArraysWithVariableNames() {
        @SuppressWarnings("unchecked")
        StructuredArray<ColoredLongArray> a =
                new StructuredArrayBuilder(StructuredArray.class,
                        new PrimitiveArrayBuilder(ColoredLongArray.class, 20),
                        50).
                        elementCtorAndArgsProvider(
                                new CtorAndArgsProvider() {
                                    @Override
                                    public CtorAndArgs getForContext(ConstructionContext context) throws NoSuchMethodException {
                                        return new CtorAndArgs(ColoredLongArray.constructor, "Color-" + context.getIndex());
                                    }
                        }).
                        build();
        assertThat(a.getLength(), is(50L));
        assertThat(a.get(0).getLength(), is(20L));
        assertThat(a.get(0).getColor(), is("Color-0"));
        assertThat(a.get(1).getColor(), is("Color-1"));
        assertThat(a.get(2).getColor(), is("Color-2"));
        assertThat(a.get(48).getColor(), is("Color-48"));
        assertThat(a.get(49).getColor(), is("Color-49"));
    }

    @Test
    public void testCopyStructuredArrayOfColoredLongArrays() throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        StructuredArray<ColoredLongArray> a =
                new StructuredArrayBuilder(StructuredArray.class,
                        new PrimitiveArrayBuilder(ColoredLongArray.class, 20).
                                arrayCtorAndArgs(ColoredLongArray.constructor, "Yellow"),
                        50).
                        build();

        for (int i = 0; i < a.getLength(); i++) {
            ColoredLongArray ca = a.get(i);
            for (int j = 0; j < ca.getLength(); j++) {
                ca.set(j, j);
            }
        }

        StructuredArray<ColoredLongArray> a2 =
                StructuredArray.copyInstance(a);

        for (int i = 0; i < a.getLength(); i++) {
            assertTrue(Arrays.equals(a.get(i).asArray(), a2.get(i).asArray()));
        }
    }
}
