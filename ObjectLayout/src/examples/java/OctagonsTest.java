/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.junit.Test;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OctagonsTest {

    @Test
    public void shouldConstructOctagon() throws NoSuchMethodException {
        Octagons octagons = Octagons.newInstance(
                100,
                "Orange",
                0, 0 /*initial center */,
                20 /* radius */,
                100, 100 /* delta in center between members */
        );

        StructuredArrayOfPoint points = octagons.get(0).getPoints();

        assertThat(valueOf(points.get(0).getX()), is(20L));
        assertThat(valueOf(points.get(0).getY()), is(0L));
        assertThat(valueOf(points.get(7).getX()), is(14L));
        assertThat(valueOf(points.get(7).getY()), is(-14L));

        points = octagons.get(10).getPoints();

        assertThat(valueOf(points.get(0).getX()), is(1000 + 20L));
        assertThat(valueOf(points.get(0).getY()), is(1000 + 0L));
        assertThat(valueOf(points.get(7).getX()), is(1000 + 14L));
        assertThat(valueOf(points.get(7).getY()), is(1000 + -14L));

        Octagons octagons2 = Octagons.copyInstance(octagons);
        points = octagons2.get(0).getPoints();

        assertThat(valueOf(points.get(0).getX()), is(20L));
        assertThat(valueOf(points.get(0).getY()), is(0L));
        assertThat(valueOf(points.get(7).getX()), is(14L));
        assertThat(valueOf(points.get(7).getY()), is(-14L));

        points = octagons.get(10).getPoints();

        assertThat(valueOf(points.get(0).getX()), is(1000 + 20L));
        assertThat(valueOf(points.get(0).getY()), is(1000 + 0L));
        assertThat(valueOf(points.get(7).getX()), is(1000 + 14L));
        assertThat(valueOf(points.get(7).getY()), is(1000 + -14L));
    }
}
