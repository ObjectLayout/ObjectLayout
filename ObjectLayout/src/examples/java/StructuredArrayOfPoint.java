/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;
import org.ObjectLayout.StructuredArrayBuilder;

import java.awt.*;

public class StructuredArrayOfPoint extends StructuredArray<Point> {

    public StructuredArrayOfPoint() {
    }

    public StructuredArrayOfPoint(StructuredArrayOfPoint source) {
        super(source);
    }

    public static StructuredArrayOfPoint newInstance(final long length) {
        return StructuredArray.newSubclassInstance(StructuredArrayOfPoint.class, Point.class, length);
    }

    public static StructuredArrayOfPoint newInstance(final long length, long initialElementValue) {
        final Class[] elementConstructorArgTypes = {Long.class};
        return StructuredArray.newSubclassInstance(StructuredArrayOfPoint.class, Point.class, length,
                        elementConstructorArgTypes, initialElementValue);
    }

    public static StructuredArrayOfPoint newInstance(final CtorAndArgsProvider<Point> ctorAndArgsProvider,
                                                     final long length) {
        return StructuredArray.newSubclassInstance(
                StructuredArrayOfPoint.class, Point.class, ctorAndArgsProvider, length);
    }

    public static StructuredArrayOfPoint newInstance(final long length,
                                                     final Class[] elementConstructorArgTypes,
                                                     final Object... elementConstructorArgs) {
        return StructuredArray.newSubclassInstance(StructuredArrayOfPoint.class, Point.class, length,
                elementConstructorArgTypes, elementConstructorArgs);
    }
}
