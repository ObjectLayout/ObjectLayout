/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.ObjectLayout.*;
import org.junit.Test;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LineTest {

    @Test
    public void shouldConstructLine() throws NoSuchMethodException {
        Line line = new Line();
        Point endPoint1 = line.getEndPoint1();
        Point endPoint2 = line.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(0L));
        assertThat(valueOf(endPoint1.getY()), is(0L));
        assertThat(valueOf(endPoint2.getX()), is(0L));
        assertThat(valueOf(endPoint2.getY()), is(0L));

        Line line2 = new Line(1, 2, 3, 4);
        endPoint1 = line2.getEndPoint1();
        endPoint2 = line2.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(1L));
        assertThat(valueOf(endPoint1.getY()), is(2L));
        assertThat(valueOf(endPoint2.getX()), is(3L));
        assertThat(valueOf(endPoint2.getY()), is(4L));

        Line line3 = new Line(line2);
        endPoint1 = line3.getEndPoint1();
        endPoint2 = line3.getEndPoint2();

        assertThat(valueOf(endPoint1.getX()), is(1L));
        assertThat(valueOf(endPoint1.getY()), is(2L));
        assertThat(valueOf(endPoint2.getX()), is(3L));
        assertThat(valueOf(endPoint2.getY()), is(4L));
    }
}
