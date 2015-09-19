package org.ObjectLayout.examples;/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.examples.Octagon;
import org.ObjectLayout.examples.Point;
import org.ObjectLayout.examples.PointArray;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.Long.valueOf;

public class OctagonTest {

    @Test
    public void shouldConstructOctagon() throws NoSuchMethodException {
        Octagon octagon = new Octagon();
        PointArray points = octagon.getPoints();

        Assert.assertThat(valueOf(points.get(0).getX()), CoreMatchers.is(0L));
        Assert.assertThat(valueOf(points.get(0).getY()), CoreMatchers.is(0L));
        Assert.assertThat(valueOf(points.get(7).getX()), CoreMatchers.is(0L));
        Assert.assertThat(valueOf(points.get(7).getY()), CoreMatchers.is(0L));

        int i = 0;
        for (Point p : points) {
            p.setX(++i);
            p.setY(++i);
        }

        Assert.assertThat(valueOf(points.get(0).getX()), CoreMatchers.is(1L));
        Assert.assertThat(valueOf(points.get(0).getY()), CoreMatchers.is(2L));
        Assert.assertThat(valueOf(points.get(1).getX()), CoreMatchers.is(3L));
        Assert.assertThat(valueOf(points.get(1).getY()), CoreMatchers.is(4L));
        Assert.assertThat(valueOf(points.get(7).getX()), CoreMatchers.is(15L));
        Assert.assertThat(valueOf(points.get(7).getY()), CoreMatchers.is(16L));

        Octagon octagon2 = new Octagon(octagon);
        points = octagon2.getPoints();

        Assert.assertThat(valueOf(points.get(0).getX()), CoreMatchers.is(1L));
        Assert.assertThat(valueOf(points.get(0).getY()), CoreMatchers.is(2L));
        Assert.assertThat(valueOf(points.get(1).getX()), CoreMatchers.is(3L));
        Assert.assertThat(valueOf(points.get(1).getY()), CoreMatchers.is(4L));
        Assert.assertThat(valueOf(points.get(7).getX()), CoreMatchers.is(15L));
        Assert.assertThat(valueOf(points.get(7).getY()), CoreMatchers.is(16L));
    }
}
