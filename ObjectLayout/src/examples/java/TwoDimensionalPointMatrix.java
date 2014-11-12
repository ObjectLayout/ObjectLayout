import org.ObjectLayout.*;

import java.lang.reflect.Constructor;

/**
 * A Matrix arranged as a height long array of columns (logically indexed as [x][y])
 */
public class TwoDimensionalPointMatrix extends StructuredArray<ArrayOfPoint> {

    public TwoDimensionalPointMatrix() {
    }

    public TwoDimensionalPointMatrix(TwoDimensionalPointMatrix source) {
        super(source);
    }

    public static TwoDimensionalPointMatrix newInstance(final long width, final long height) {
        final CtorAndArgs<Point> xy_ctorAndArgs = new CtorAndArgs<>(xy_constructor, 0, 0);
        return newInstance(
                width,
                height,
                // This can be a Lambda expression in Java 8:
                new CtorAndArgsProvider<Point>() {
                    @Override
                    public CtorAndArgs<Point> getForContext(
                            ConstructionContext<Point> context) throws NoSuchMethodException {
                        return xy_ctorAndArgs;
                    }
                }
                );
    }

    public static TwoDimensionalPointMatrix newInstance(
            final long width,
            final long height,
            final CtorAndArgsProvider<Point> ctorAndArgsProvider) {
        StructuredArrayBuilder<TwoDimensionalPointMatrix, ArrayOfPoint> builder =
                new StructuredArrayBuilder<>(
                        TwoDimensionalPointMatrix.class,
                        new StructuredArrayBuilder<>(
                                ArrayOfPoint.class,
                                Point.class,
                                width).
                                elementCtorAndArgsProvider(ctorAndArgsProvider),
                        height);

        return builder.build();
    }

    // If you want to support direct construction parameters for elements, with with parameter types you know
    // (statically) have a good constructor associated with the, here is an example:

    public static TwoDimensionalPointMatrix newInstance(
            final long width,
            final long height,
            final long x,
            final long y) {
        final CtorAndArgs<Point> xy_ctorAndArgs = new CtorAndArgs<>(xy_constructor, x, y);
        return newInstance(width, height,
                // This can be a Lambda expression in Java 8:
                new CtorAndArgsProvider<Point>() {
                    @Override
                    public CtorAndArgs<Point> getForContext(
                            ConstructionContext<Point> context) throws NoSuchMethodException {
                        return xy_ctorAndArgs;
                    }
                });
    }

    // An example of how to create a matrix with points pre-initialized to their [x, y]
    // in the matrix. This is a good example of using hierarchical context in determining
    // the ctorAndArgs for a specific array element:
    public static TwoDimensionalPointMatrix newPreInitializedInstance(
            final long width,
            final long height) {
        final CtorAndArgs<Point> xy_ctorAndArgs = new CtorAndArgs<>(xy_constructor, 0, 0);
        return newInstance(
                width,
                height,
                // This can be a Lambda expression in Java 8:
                new CtorAndArgsProvider<Point>() {
                    @Override
                    public CtorAndArgs<Point> getForContext(
                            ConstructionContext<Point> context) throws NoSuchMethodException {
                        return xy_ctorAndArgs.setArgs(
                                context.getIndex(),
                                context.getContainingContext().getIndex()
                        );
                    }
                }
        );
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
