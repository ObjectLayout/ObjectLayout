/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.junit.Test;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OctagonTest {

    @Test
    public void shouldConstructOctagon() throws NoSuchMethodException {
        Octagon octagon = new Octagon();
        ArrayOfPoint points = octagon.getPoints();

        assertThat(valueOf(points.get(0).getX()), is(0L));
        assertThat(valueOf(points.get(0).getY()), is(0L));
        assertThat(valueOf(points.get(7).getX()), is(0L));
        assertThat(valueOf(points.get(7).getY()), is(0L));

        int i = 0;
        for (Point p : points) {
            p.setX(++i);
            p.setY(++i);
        }

        assertThat(valueOf(points.get(0).getX()), is(1L));
        assertThat(valueOf(points.get(0).getY()), is(2L));
        assertThat(valueOf(points.get(1).getX()), is(3L));
        assertThat(valueOf(points.get(1).getY()), is(4L));
        assertThat(valueOf(points.get(7).getX()), is(15L));
        assertThat(valueOf(points.get(7).getY()), is(16L));

        Octagon octagon2 = new Octagon(octagon);
        points = octagon2.getPoints();

        assertThat(valueOf(points.get(0).getX()), is(1L));
        assertThat(valueOf(points.get(0).getY()), is(2L));
        assertThat(valueOf(points.get(1).getX()), is(3L));
        assertThat(valueOf(points.get(1).getY()), is(4L));
        assertThat(valueOf(points.get(7).getX()), is(15L));
        assertThat(valueOf(points.get(7).getY()), is(16L));
    }
}
