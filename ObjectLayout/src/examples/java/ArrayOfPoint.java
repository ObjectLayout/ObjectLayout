/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.ConstructionContext;
import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;
import java.lang.reflect.Constructor;

public class ArrayOfPoint extends StructuredArray<Point> {


    // expose copy constructor:
    public ArrayOfPoint(ArrayOfPoint source) {
        super(source);
    }

    // Default constructor needs to be defined only because we wanted to expose a copy constructor
    public ArrayOfPoint() {
    }

    // newInstance wrapper for convenience (cleaner/fewer parameters for API user):
    public static ArrayOfPoint newInstance(final long length) {
        return StructuredArray.newInstance(ArrayOfPoint.class, Point.class, length);
    }

    // newInstance wrapper for convenience (cleaner/fewer parameters for API user):
    public static ArrayOfPoint newInstance(final long length,
                                           final CtorAndArgsProvider<Point> ctorAndArgsProvider) {
        return StructuredArray.newInstance(
                ArrayOfPoint.class, Point.class, length, ctorAndArgsProvider);
    }

    // If you want to support direct construction parameters for elements, with with
    // parameter types you know (statically) have a good constructor associated with the,
    // here is an example:
    public static ArrayOfPoint newInstance(final long length, final long x, final long y) {
        final CtorAndArgs<Point> xy_ctorAndArgs = new CtorAndArgs<Point>(xy_constructor, x, y);
        return StructuredArray.newInstance(
                ArrayOfPoint.class,
                Point.class,
                length,
                // This can be a Lambda expression in Java 8:
                new CtorAndArgsProvider<Point>() {
                    @Override
                    public CtorAndArgs<Point> getForContext(
                            ConstructionContext<Point> context) throws NoSuchMethodException {
                        return xy_ctorAndArgs;
                    }
                });
    }

    static final Constructor<Point> xy_constructor;

    static {
        try {
            @SuppressWarnings("unchecked")
            Constructor<Point> constructor = Point.class.getConstructor(long.class, long.class);
            xy_constructor = constructor;
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}
