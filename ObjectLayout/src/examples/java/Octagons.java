/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.*;

import java.lang.reflect.Constructor;
import java.util.Date;

public class Octagons extends StructuredArray<Octagon> {

    public Date creationDate = new Date();

    public Octagons() {
    }

    public Octagons(Octagons source) {
        super(source);
    }

    public static Octagons newInstance(final long length) {
        return StructuredArray.newInstance(Octagons.class, Octagon.class, length);
    }

    public static Octagons newInstance(
            final long length,
            final CtorAndArgsProvider<Octagon> ctorAndArgsProvider) {
        return StructuredArray.newInstance(
                Octagons.class, Octagon.class, length, ctorAndArgsProvider);
    }

    public static Octagons newInstance(
            final long length,
            final String color,
            final long initialCenterX,
            final long initialCenterY,
            final long radius,
            final long deltaX,
            final long deltaY) {
        return newInstance(
                length,
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
                });
    }

    static final Constructor<Octagon> cxyr_constructor;

    static {
        try {
            @SuppressWarnings("unchecked")
            Constructor<Octagon>  constructor = Octagon.class.getConstructor(
                    String.class, long.class, long.class, long.class);
            cxyr_constructor = constructor;

        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}
