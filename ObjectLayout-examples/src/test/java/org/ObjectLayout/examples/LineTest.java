package org.ObjectLayout.examples;/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.examples.Line;
import org.ObjectLayout.examples.Point;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.Long.valueOf;

public class LineTest {

    @Test
    public void shouldConstructLine() throws NoSuchMethodException {
        Line line = new Line();
        Point endPoint1 = line.getEndPoint1();
        Point endPoint2 = line.getEndPoint2();

        Assert.assertThat(valueOf(endPoint1.getX()), CoreMatchers.is(0L));
        Assert.assertThat(valueOf(endPoint1.getY()), CoreMatchers.is(0L));
        Assert.assertThat(valueOf(endPoint2.getX()), CoreMatchers.is(0L));
        Assert.assertThat(valueOf(endPoint2.getY()), CoreMatchers.is(0L));

        Line line2 = new Line(1, 2, 3, 4);
        endPoint1 = line2.getEndPoint1();
        endPoint2 = line2.getEndPoint2();

        Assert.assertThat(valueOf(endPoint1.getX()), CoreMatchers.is(1L));
        Assert.assertThat(valueOf(endPoint1.getY()), CoreMatchers.is(2L));
        Assert.assertThat(valueOf(endPoint2.getX()), CoreMatchers.is(3L));
        Assert.assertThat(valueOf(endPoint2.getY()), CoreMatchers.is(4L));

        Line line3 = new Line(line2);
        endPoint1 = line3.getEndPoint1();
        endPoint2 = line3.getEndPoint2();

        Assert.assertThat(valueOf(endPoint1.getX()), CoreMatchers.is(1L));
        Assert.assertThat(valueOf(endPoint1.getY()), CoreMatchers.is(2L));
        Assert.assertThat(valueOf(endPoint2.getX()), CoreMatchers.is(3L));
        Assert.assertThat(valueOf(endPoint2.getY()), CoreMatchers.is(4L));
    }
}
