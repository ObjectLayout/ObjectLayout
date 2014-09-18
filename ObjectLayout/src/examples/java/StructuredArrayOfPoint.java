/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.ConstantCtorAndArgsProvider;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;

import java.lang.reflect.Constructor;

public class StructuredArrayOfPoint extends StructuredArray<Point> {

    public StructuredArrayOfPoint() {
    }

    public StructuredArrayOfPoint(StructuredArrayOfPoint source) {
        super(source);
    }

    public static StructuredArrayOfPoint newInstance(final long length) {
        return StructuredArray.newInstance(StructuredArrayOfPoint.class, Point.class, length);
    }

    public static StructuredArrayOfPoint newInstance(final CtorAndArgsProvider<Point> ctorAndArgsProvider,
                                                     final long length) {
        return StructuredArray.newInstance(
                StructuredArrayOfPoint.class, Point.class, ctorAndArgsProvider, length);
    }

    // If you want to support direct construction parameters for elements, with with parameter types you know
    // (statically) have a good constructor associated with the, here is an example:

    public static StructuredArrayOfPoint newInstance(final long length, final long x, final long y) {
        return StructuredArray.newInstance(
                StructuredArrayOfPoint.class,
                Point.class,
                new ConstantCtorAndArgsProvider<Point>(xy_constructor, x, y),
                length);
    }

    static final Class[] argsTypes = new Class[] {long.class, long.class};

    static final Constructor<Point> xy_constructor;

    static {
        try {
            xy_constructor = Point.class.getConstructor(argsTypes);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}
