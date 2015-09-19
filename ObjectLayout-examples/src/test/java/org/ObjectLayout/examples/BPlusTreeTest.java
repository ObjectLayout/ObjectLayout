package org.ObjectLayout.examples;/*
 * Written by Gil Tene, Martin Thompson and Michael Barker, and released 
 * to the public domain, as explained at:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import static java.util.Arrays.sort;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.ObjectLayout.examples.BPlusTree;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class BPlusTreeTest {
    private final BPlusTree<Integer, Integer> tree = new BPlusTree<Integer, Integer>(
            8);

    @Test
    public void test() {
        int[] values = new int[] { 10, 20, 30, 40, 50, 60, 70, 80 };

        for (int value : values) {
            tree.put(value, value);
        }

        tree.put(21, 21);

        Assert.assertThat(tree.get(49), CoreMatchers.is(CoreMatchers.nullValue()));
        Assert.assertThat(tree.get(50), CoreMatchers.is((Object) 50));
        Assert.assertThat(tree.get(51), CoreMatchers.is(CoreMatchers.nullValue()));
        Assert.assertThat(tree.get(21), CoreMatchers.is((Object) 21));
    }

    @Test
    public void insertsAndGetsAndRemovesRandomValues() throws Exception {
        int[] values = nextInts(new Random(5), Integer.MAX_VALUE,
                new int[10000]);

        putAll(tree, values);
        assertGet(tree, values);

        TreeMap<Integer, Integer> treeMap = new TreeMap<Integer, Integer>();
        putAll(treeMap, values);

        Assert.assertThat(tree.size(), CoreMatchers.is(treeMap.size()));
        Assert.assertThat(count(tree), CoreMatchers.is(treeMap.size()));

        int count = 0;
        for (int value : values) {
            Assert.assertThat("{" + count + "}", tree.get(value),
                    CoreMatchers.is(treeMap.get(value)));
            Integer removed = tree.remove(value);
            Integer mapRemoved = treeMap.remove(value);

            Assert.assertThat("{" + count + "}", removed, CoreMatchers.is(mapRemoved));
            Assert.assertThat("{" + count + "}", tree.size(), CoreMatchers.is(treeMap.size()));
            Assert.assertThat("{" + count + "}", count(tree), CoreMatchers.is(treeMap.size()));
            Assert.assertThat("{" + count + "}", tree.get(value), CoreMatchers.is(CoreMatchers.nullValue()));

            count++;
        }
    }

    private int count(BPlusTree<Integer, Integer> tree) {
        Iterator<Entry<Integer, Integer>> iterator = tree.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            iterator.next();
            i++;
        }
        return i;
    }

    @Test
    public void putsAndDeletesWithoutBranching() {
        int[] values = new int[] { 10, 20, 30, 40, 50, 60, 70, 80 };
        putAll(tree, values);

        int size = tree.size();

        for (int value : values) {
            int removed = tree.remove(value);
            Assert.assertThat(removed, CoreMatchers.is(value));
            Assert.assertThat(tree.size(), CoreMatchers.is(--size));
        }
    }

    @Test
    public void putsAndDeletesFromLeftWithBranching() {
        int[] values = new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 90 };
        putAll(tree, values);

        int size = tree.size();

        for (int value : values) {
            int removed = tree.remove(value);
            Assert.assertThat(removed, CoreMatchers.is(value));
            Assert.assertThat(tree.size(), CoreMatchers.is(--size));
        }
    }

    @Test
    public void putsAndDeletesFromRightWithBranching() {
        int[] values = new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 45 };
        putAll(tree, values);

        int size = tree.size();
        sort(values);

        for (int value : reverse(values)) {
            int removed = tree.remove(value);
            Assert.assertThat(removed, CoreMatchers.is(value));
            Assert.assertThat(tree.size(), CoreMatchers.is(--size));
        }
    }

    private int[] reverse(int[] values) {

        int[] reversed = new int[values.length];
        int index = reversed.length;
        for (int value : values) {
            reversed[--index] = value;
        }

        return reversed;
    }

    @Test
    public void splitsLeavesTwiceToRight() throws Exception {
        int[] values = new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 51, 52, 53,
                54, 55 };
        assertPutAndGet(values);
    }

    @Test
    public void splitsLeavesTwiceToLeft() throws Exception {
        int[] values = new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 1, 2, 3, 4,
                5 };
        assertPutAndGet(values);
    }

    @Test
    public void splitsLeavesTwiceBothWays() throws Exception {
        int[] values = new int[] { 10, 20, 30, 40, 50, 60, 70, 80, 41, 42, 43,
                44, 45 };
        assertPutAndGet(values);
    }

    private void assertPutAndGet(int[] values) {
        for (int value : values) {
            tree.put(value, value);
        }

        for (int value : values) {
            Assert.assertThat(tree.get(value), CoreMatchers.is((Object) value));
        }
    }

    private static void putAll(BPlusTree<Integer, Integer> node, int... values) {
        int counter = 0;
        for (int value : values) {
            try {
                node.put(value, value);
                counter++;
            } catch (Exception e) {
                throw new RuntimeException("" + counter, e);
            }
        }
    }

    private static void assertGet(BPlusTree<Integer, Integer> tree, int[] values) {
        for (int value : values) {
            Assert.assertThat(tree.get(value), CoreMatchers.is((Object) value));
        }
    }

    private static void putAll(TreeMap<Integer, Integer> treeMap, int[] values) {
        for (int value : values) {
            treeMap.put(value, value);
        }
    }

    private int[] nextInts(Random r, int maxValue, int[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = r.nextInt(maxValue);
        }

        return values;
    }

}
