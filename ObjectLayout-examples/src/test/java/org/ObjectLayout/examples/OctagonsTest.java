package org.ObjectLayout.examples;/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.examples.Octagons;
import org.ObjectLayout.examples.PointArray;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.Long.valueOf;

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

        PointArray points = octagons.get(0).getPoints();

        Assert.assertThat(valueOf(points.get(0).getX()), CoreMatchers.is(20L));
        Assert.assertThat(valueOf(points.get(0).getY()), CoreMatchers.is(0L));
        Assert.assertThat(valueOf(points.get(7).getX()), CoreMatchers.is(14L));
        Assert.assertThat(valueOf(points.get(7).getY()), CoreMatchers.is(-14L));

        points = octagons.get(10).getPoints();

        Assert.assertThat(valueOf(points.get(0).getX()), CoreMatchers.is(1000 + 20L));
        Assert.assertThat(valueOf(points.get(0).getY()), CoreMatchers.is(1000 + 0L));
        Assert.assertThat(valueOf(points.get(7).getX()), CoreMatchers.is(1000 + 14L));
        Assert.assertThat(valueOf(points.get(7).getY()), CoreMatchers.is(1000 + -14L));

        Octagons octagons2 = Octagons.copyInstance(octagons);
        points = octagons2.get(0).getPoints();

        Assert.assertThat(valueOf(points.get(0).getX()), CoreMatchers.is(20L));
        Assert.assertThat(valueOf(points.get(0).getY()), CoreMatchers.is(0L));
        Assert.assertThat(valueOf(points.get(7).getX()), CoreMatchers.is(14L));
        Assert.assertThat(valueOf(points.get(7).getY()), CoreMatchers.is(-14L));

        points = octagons.get(10).getPoints();

        Assert.assertThat(valueOf(points.get(0).getX()), CoreMatchers.is(1000 + 20L));
        Assert.assertThat(valueOf(points.get(0).getY()), CoreMatchers.is(1000 + 0L));
        Assert.assertThat(valueOf(points.get(7).getX()), CoreMatchers.is(1000 + 14L));
        Assert.assertThat(valueOf(points.get(7).getY()), CoreMatchers.is(1000 + -14L));
    }
}
