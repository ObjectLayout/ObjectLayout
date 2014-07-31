/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.MultiDimensionalCtorAndArgsProvider;
import org.ObjectLayout.SingleDimensionalCtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;

import java.awt.*;

public class StructuredArrayOfPoint extends StructuredArray<Point> {

    // Single dimensional instantiation:

    public static StructuredArrayOfPoint newInstance(final long length) {
        return StructuredArray.newSubclassInstance(StructuredArrayOfPoint.class, Point.class, length);
    }

    public static StructuredArrayOfPoint newInstance(final long length, long initialElementValue) {
        final Class[] elementConstructorArgTypes = {Long.class};
        return StructuredArray.newSubclassInstance(StructuredArrayOfPoint.class, Point.class, length,
                        elementConstructorArgTypes, initialElementValue);
    }

    public static StructuredArrayOfPoint newInstance(final SingleDimensionalCtorAndArgsProvider<Point> ctorAndArgsProvider,
                                                     final long length) {
        return StructuredArray.newSubclassInstance(
                StructuredArrayOfPoint.class, Point.class, ctorAndArgsProvider, length);
    }

    // Multi dimensional instantiation:

    public static StructuredArrayOfPoint newInstance(final long... lengths) {
        return StructuredArray.newSubclassInstance(StructuredArrayOfPoint.class, Point.class, lengths);
    }

    public static StructuredArrayOfPoint newInstance(final long[] lengths,
                                                     final Class[] elementConstructorArgTypes,
                                                     final Object... elementConstructorArgs) {
        return StructuredArray.newSubclassInstance(StructuredArrayOfPoint.class, Point.class, lengths,
                        elementConstructorArgTypes, elementConstructorArgs);
    }

    public static StructuredArrayOfPoint newInstance(final MultiDimensionalCtorAndArgsProvider<Point> ctorAndArgsProvider,
                                                     final long... lengths) {
        return StructuredArray.newSubclassInstance(
                StructuredArrayOfPoint.class, Point.class, ctorAndArgsProvider, lengths);
    }

    // getSubArray convenience version, allowing user to avoid casting boiler plate in partial-dimension access:

    public StructuredArrayOfPoint getSubArray(final int index) throws IllegalArgumentException {
        return (StructuredArrayOfPoint) super.getSubArray(index);
    }

}
