/*
 * Written by Gil Tene, Martin Thompson and Michael Barker, and released 
 * to the public domain, as explained at:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import static java.util.Arrays.sort;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.junit.Test;

public class BPlusTreeTest
{
    private final BPlusTree<Integer, Integer> tree =
        new BPlusTree<Integer, Integer>(8);

    @Test
    public void test()
    {
        int[] values = new int[]
        { 10, 20, 30, 40, 50, 60, 70, 80 };

        for (int value : values)
        {
            tree.put(value, value);
        }

        tree.put(21, 21);

        assertThat(tree.get(49), is(nullValue()));
        assertThat(tree.get(50), is((Object) 50));
        assertThat(tree.get(51), is(nullValue()));
        assertThat(tree.get(21), is((Object) 21));
    }

    @Test
    public void insertsAndGetsAndRemovesRandomValues() throws Exception
    {
        int[] values =
            nextInts(new Random(5), Integer.MAX_VALUE, new int[10000]);

        putAll(tree, values);
        assertGet(tree, values);

        TreeMap<Integer, Integer> treeMap = new TreeMap<Integer, Integer>();
        putAll(treeMap, values);

        assertThat(tree.size(), is(treeMap.size()));
        assertThat(count(tree), is(treeMap.size()));

        int count = 0;
        for (int value : values)
        {
            assertThat(
                "{" + count + "}", tree.get(value), is(treeMap.get(value)));
            Integer removed = tree.remove(value);
            Integer mapRemoved = treeMap.remove(value);

            assertThat("{" + count + "}", removed, is(mapRemoved));
            assertThat("{" + count + "}", tree.size(), is(treeMap.size()));
            assertThat("{" + count + "}", count(tree), is(treeMap.size()));
            assertThat("{" + count + "}", tree.get(value), is(nullValue()));

            count++;
        }
    }

    private int count(BPlusTree<Integer, Integer> tree)
    {
        Iterator<Entry<Integer, Integer>> iterator = tree.iterator();
        int i = 0;
        while (iterator.hasNext())
        {
            iterator.next();
            i++;
        }
        return i;
    }

    @Test
    public void putsAndDeletesWithoutBranching()
    {
        int[] values = new int[]
        { 10, 20, 30, 40, 50, 60, 70, 80 };
        putAll(tree, values);

        int size = tree.size();

        for (int value : values)
        {
            int removed = tree.remove(value);
            assertThat(removed, is(value));
            assertThat(tree.size(), is(--size));
        }
    }

    @Test
    public void putsAndDeletesFromLeftWithBranching()
    {
        int[] values = new int[]
        { 10, 20, 30, 40, 50, 60, 70, 80, 90 };
        putAll(tree, values);

        int size = tree.size();

        for (int value : values)
        {
            int removed = tree.remove(value);
            assertThat(removed, is(value));
            assertThat(tree.size(), is(--size));
        }
    }

    @Test
    public void putsAndDeletesFromRightWithBranching()
    {
        int[] values = new int[]
        { 10, 20, 30, 40, 50, 60, 70, 80, 90, 45 };
        putAll(tree, values);

        int size = tree.size();
        sort(values);

        for (int value : reverse(values))
        {
            int removed = tree.remove(value);
            assertThat(removed, is(value));
            assertThat(tree.size(), is(--size));
        }
    }

    private int[] reverse(int[] values)
    {

        int[] reversed = new int[values.length];
        int index = reversed.length;
        for (int value : values)
        {
            reversed[--index] = value;
        }

        return reversed;
    }

    @Test
    public void splitsLeavesTwiceToRight() throws Exception
    {
        int[] values = new int[]
        { 10, 20, 30, 40, 50, 60, 70, 80, 51, 52, 53, 54, 55 };
        assertPutAndGet(values);
    }

    @Test
    public void splitsLeavesTwiceToLeft() throws Exception
    {
        int[] values = new int[]
        { 10, 20, 30, 40, 50, 60, 70, 80, 1, 2, 3, 4, 5 };
        assertPutAndGet(values);
    }

    @Test
    public void splitsLeavesTwiceBothWays() throws Exception
    {
        int[] values = new int[]
        { 10, 20, 30, 40, 50, 60, 70, 80, 41, 42, 43, 44, 45 };
        assertPutAndGet(values);
    }

    private void assertPutAndGet(int[] values)
    {
        for (int value : values)
        {
            tree.put(value, value);
        }

        for (int value : values)
        {
            assertThat(tree.get(value), is((Object) value));
        }
    }

    private static void putAll(BPlusTree<Integer, Integer> node, int... values)
    {
        int counter = 0;
        for (int value : values)
        {
            try {
                node.put(value, value);
                counter++;
            } catch (Exception e) {
                throw new RuntimeException("" + counter, e);
            }
        }
    }

    private static void assertGet(BPlusTree<Integer, Integer> tree, int[] values)
    {
        for (int value : values)
        {
            assertThat(tree.get(value), is((Object) value));
        }
    }

    private static void putAll(TreeMap<Integer, Integer> treeMap, int[] values)
    {
        for (int value : values)
        {
            treeMap.put(value, value);
        }
    }

    private int[] nextInts(Random r, int maxValue, int[] values)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = r.nextInt(maxValue);
        }

        return values;
    }

}
