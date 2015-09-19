package org.ObjectLayout.examples.util;/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.ObjectLayout.examples.util.SAHashMap;

import java.util.HashMap;

public class SAHashMapTest {

    @Test
    public void hashMapPopulateTest() throws NoSuchMethodException {
        final SAHashMap<Integer, String> saMap = new SAHashMap<Integer, String>();
        final HashMap<Integer, String> map = new HashMap<Integer, String>();

        final int length = 1<<16;

        // populate hashmaps:
        System.out.println();
        for (int i = 0; i < length; i++) {
            String intAsString = "Int:" + Integer.toString(i);
            saMap.put(i, intAsString);
            map.put(i, intAsString);
            // Verify:
            String e;
            Assert.assertThat("Map sizes should be equal", saMap.size(), CoreMatchers.is(map.size()));
            Assert.assertThat("saMap entry value mismatch", saMap.get(i), CoreMatchers.is(intAsString));
            Assert.assertThat("map entry value mismatch", map.get(i), CoreMatchers.is(intAsString));
        }
    }
}
