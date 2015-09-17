package org.ObjectLayoutApiTests;

import org.ObjectLayout.*;
import org.junit.Test;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PrimitiveLongArrayTest {

    static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    public static class ColoredLongArrayPublic extends PrimitiveLongArray {
        private final String color;

        private static CtorAndArgs<ColoredLongArrayPublic> ctorAndArgs =
                new CtorAndArgs<ColoredLongArrayPublic>(lookup, ColoredLongArrayPublic.class,
                        new Class[]{String.class}, "DefaultColor");

        public static ColoredLongArrayPublic newInstance(String color, final int length) {
            ctorAndArgs.getArgs()[0] = color;
            return newInstance(ctorAndArgs, length);
        }

        public ColoredLongArrayPublic(String color) {
            this.color = color;
        }

        public ColoredLongArrayPublic(ColoredLongArrayPublic sourceArray) {
            super(sourceArray);
            color = sourceArray.color;
        }

        public String getColor() {
            return color;
        }
    }

    private static class ColoredLongArray extends PrimitiveLongArray {
        private final String color;

        private static CtorAndArgs<ColoredLongArray> ctorAndArgs =
                new CtorAndArgs<ColoredLongArray>(lookup, ColoredLongArray.class, new Class[]{String.class}, "DefaultColor");

        public static ColoredLongArray newInstance(String color, final int length) {
            ctorAndArgs.getArgs()[0] = color;
            return newInstance(ctorAndArgs, length);
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
    public void testColoredLongArrayPublic() throws Exception {
        ColoredLongArrayPublic blueArray = ColoredLongArrayPublic.newInstance("Blue", 10);
        ColoredLongArrayPublic redArray = ColoredLongArrayPublic.newInstance("Red", 20);

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

        ColoredLongArray blueArrayCopy = ColoredLongArray.copyInstance(lookup, blueArray);

        assertTrue(redArray.getColor().equals("Red"));
        assertTrue(blueArray.getColor().equals("Blue"));
        assertTrue(blueArrayCopy.getColor().equals("Blue"));
        assertTrue(Arrays.equals(blueArray.asArray(), blueArrayCopy.asArray()));
    }

    @Test
    public void testCopyOfColoredLongArrayPublic() throws Exception {
        ColoredLongArrayPublic blueArray = ColoredLongArrayPublic.newInstance("Blue", 10);
        ColoredLongArrayPublic redArray = ColoredLongArrayPublic.newInstance("Red", 20);

        for (int i = 0; i < redArray.getLength(); i++) {
            redArray.set(i, i);
        }

        for (int i = 0; i < blueArray.getLength(); i++) {
            blueArray.set(i, redArray.get(i));
        }

        ColoredLongArrayPublic blueArrayCopy = ColoredLongArrayPublic.copyInstance(blueArray);

        assertTrue(redArray.getColor().equals("Red"));
        assertTrue(blueArray.getColor().equals("Blue"));
        assertTrue(blueArrayCopy.getColor().equals("Blue"));
        assertTrue(Arrays.equals(blueArray.asArray(), blueArrayCopy.asArray()));
    }

    @Test
    public void testColoredLongArrayBuilder() throws Exception {
        PrimitiveArrayBuilder<ColoredLongArray> builder = new PrimitiveArrayBuilder<ColoredLongArray>(
                ColoredLongArray.class, 50);
        ColoredLongArray blueArray = builder.arrayCtorAndArgs(ColoredLongArray.ctorAndArgs.setArgs("Blue")).build();
        ColoredLongArray redArray = builder.arrayCtorAndArgs(ColoredLongArray.ctorAndArgs.setArgs("Red")).build();

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
    public void testColoredLongArrayBuilderPublic() throws Exception {
        PrimitiveArrayBuilder<ColoredLongArrayPublic> builder = new PrimitiveArrayBuilder<ColoredLongArrayPublic>(
                ColoredLongArrayPublic.class, 50);
        ColoredLongArrayPublic blueArray = builder.arrayCtorAndArgs(ColoredLongArrayPublic.ctorAndArgs.setArgs("Blue")).build();
        ColoredLongArrayPublic redArray = builder.arrayCtorAndArgs(ColoredLongArrayPublic.ctorAndArgs.setArgs("Red")).build();

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
                                arrayCtorAndArgs(ColoredLongArray.ctorAndArgs.setArgs("Yellow")),
                        50).
                        build();
        assertThat(a.getLength(), is(50L));
        assertThat(a.get(0).getLength(), is(20L));
        assertThat(a.get(1).getColor(), is("Yellow"));
    }

    @Test
    public void testStructuredArrayOfColoredLongArraysPublic() throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        StructuredArray<ColoredLongArrayPublic> a =
                new StructuredArrayBuilder(StructuredArray.class,
                        new PrimitiveArrayBuilder(ColoredLongArrayPublic.class, 20).
                                arrayCtorAndArgs(ColoredLongArrayPublic.ctorAndArgs.setArgs("Yellow")),
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
                                        return ColoredLongArray.ctorAndArgs.setArgs("Color-" + context.getIndex());
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
    public void testStructuredArrayOfColoredLongArraysWithVariableNamesPublic() {
        @SuppressWarnings("unchecked")
        StructuredArray<ColoredLongArrayPublic> a =
                new StructuredArrayBuilder(StructuredArray.class,
                        new PrimitiveArrayBuilder(ColoredLongArrayPublic.class, 20),
                        50).
                        elementCtorAndArgsProvider(
                                new CtorAndArgsProvider() {
                                    @Override
                                    public CtorAndArgs getForContext(ConstructionContext context) throws NoSuchMethodException {
                                        return ColoredLongArrayPublic.ctorAndArgs.setArgs("Color-" + context.getIndex());
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
                                arrayCtorAndArgs(ColoredLongArray.ctorAndArgs.setArgs("Yellow")),
                        50).
                        build();

        for (int i = 0; i < a.getLength(); i++) {
            ColoredLongArray ca = a.get(i);
            for (int j = 0; j < ca.getLength(); j++) {
                ca.set(j, j);
            }
        }

        StructuredArray<ColoredLongArray> a2 =
                StructuredArray.copyInstance(lookup, a);

        for (int i = 0; i < a.getLength(); i++) {
            assertTrue(Arrays.equals(a.get(i).asArray(), a2.get(i).asArray()));
        }
    }

    @Test
    public void testCopyStructuredArrayOfColoredLongArraysPublic() throws NoSuchMethodException {
        @SuppressWarnings("unchecked")
        StructuredArray<ColoredLongArrayPublic> a =
                new StructuredArrayBuilder(StructuredArray.class,
                        new PrimitiveArrayBuilder(ColoredLongArrayPublic.class, 20).
                                arrayCtorAndArgs(ColoredLongArrayPublic.ctorAndArgs.setArgs("Yellow")),
                        50).
                        build();

        for (int i = 0; i < a.getLength(); i++) {
            ColoredLongArrayPublic ca = a.get(i);
            for (int j = 0; j < ca.getLength(); j++) {
                ca.set(j, j);
            }
        }

        StructuredArray<ColoredLongArrayPublic> a2 =
                StructuredArray.copyInstance(a);

        for (int i = 0; i < a.getLength(); i++) {
            assertTrue(Arrays.equals(a.get(i).asArray(), a2.get(i).asArray()));
        }
    }
}
