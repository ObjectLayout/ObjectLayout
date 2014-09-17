/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.*;

import java.lang.reflect.Constructor;

public class Octagons extends StructuredArray<Octagon> {

    public Octagons() {
    }

    public Octagons(Octagons source) {
        super(source);
    }

    public static Octagons newInstance(final long length) {
        return StructuredArray.newInstance(Octagons.class, Octagon.class, length);
    }

    public static Octagons newInstance(
            final CtorAndArgsProvider<Octagon> ctorAndArgsProvider,
            final long length) {
        return StructuredArray.newInstance(
                Octagons.class, Octagon.class, ctorAndArgsProvider, length);
    }

    public static Octagons newInstance(
            final long length,
            final String color,
            final long initialCenterX,
            final long initialCenterY,
            final long radius,
            final long deltaX,
            final long deltaY) {
        return StructuredArray.newInstance(
                Octagons.class,
                Octagon.class,
                new CtorAndArgsProvider<Octagon>() {
                    @Override
                    public CtorAndArgs<Octagon> getForContext(
                            ConstructionContext<Octagon> context) throws NoSuchMethodException {
                        return new CtorAndArgs<Octagon>(
                                cxyr_constructor,
                                color,
                                (initialCenterX + context.getIndex() * deltaX),
                                (initialCenterY + context.getIndex() * deltaY),
                                radius
                        );
                    }
                },
                length);
    }

    static final Constructor<Octagon> cxyr_constructor;

    static {
        try {
            cxyr_constructor = Octagon.class.getConstructor(
                    new Class[] {String.class, long.class, long.class, long.class});
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}
