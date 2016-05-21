package org.ObjectLayout.examples.util;/*
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Test;

public class SAHashMapTest {

    private static final int LOW_COLLISION_COUNT = 4;
    private static final int HIGH_COLLISION_COUNT = 128;

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
            assertEquals("Map sizes should be equal", saMap.size(), map.size());
            assertEquals("saMap entry value mismatch", saMap.get(i), intAsString);
            assertEquals("map entry value mismatch", map.get(i), intAsString);
        }
    }

    private static Collision[] createCollisions(int collisionCount) {
        Collision[] collisions = new Collision[collisionCount];
        for (int i = 0; i < collisionCount; i++) {
            collisions[i] = new Collision(i);
        }
        return collisions;
    }

    private static Map<Collision, Collision> createCollisionMap(Collision[] collisions) {
        Map<Collision, Collision> saMap = new SAHashMap<>();
        for (Collision collision : collisions) {
            saMap.put(collision, collision);
        }
        return saMap;
    }

    @Test
    public void appendCollisions() {
        appendCollisions(HIGH_COLLISION_COUNT);
        appendCollisions(LOW_COLLISION_COUNT);
    }


    private void appendCollisions(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = new SAHashMap<>();

        for (int i = 0; i < collisions.length; i++) {
            Collision collision = collisions[i];

            assertFalse(saMap.containsKey(collision));
            assertFalse(saMap.containsValue(collision));

            saMap.put(collision, collision);

            for (int j = 0; j <= i; j++) {
                assertEquals(collisions[j], saMap.get(collisions[j]));
            }
        }
    }

    @Test
    public void removeCollisionsAscending() {
        removeCollisionsAscending(LOW_COLLISION_COUNT);
        removeCollisionsAscending(HIGH_COLLISION_COUNT);
    }

    private void removeCollisionsAscending(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = new SAHashMap<>();

        for (Collision collision : collisions) {
            saMap.put(collision, collision);
        }

        for (int i = 0; i < collisions.length; i++) {
            Collision collision = collisions[i];
            assertEquals(collision, saMap.remove(collision));
            assertNull(saMap.remove(saMap));

            for (int j = i + 1; j < collisions.length; j++) {
                assertEquals(collisions[j], saMap.get(collisions[j]));
            }
        }
    }

    @Test
    public void removeCollisionsDescending() {
        removeCollisionsDescending(LOW_COLLISION_COUNT);
        removeCollisionsDescending(HIGH_COLLISION_COUNT);
    }

    private void removeCollisionsDescending(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = new SAHashMap<>();

        for (Collision collision : collisions) {
            saMap.put(collision, collision);
        }

        for (int i = collisions.length - 1; i >= 0; i--) {
            Collision collision = collisions[i];
            assertEquals(collision, saMap.remove(collision));
            assertNull(saMap.remove(saMap));

            for (int j = 0; j < i; j++) {
                assertEquals(collisions[j], saMap.get(collisions[j]));
            }
        }

    }

    @Test
    public void computeIfAbsent() {
        computeIfAbsent(LOW_COLLISION_COUNT);
        computeIfAbsent(HIGH_COLLISION_COUNT);
    }

    private void computeIfAbsent(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = createCollisionMap(collisions);

        Collision smallerKey = new Collision(-1);
        Collision largerKey = new Collision(collisions.length);

        saMap.computeIfAbsent(smallerKey, k -> k);
        assertEquals(smallerKey, saMap.get(smallerKey));

        saMap.computeIfAbsent(largerKey, k -> k);
        assertEquals(largerKey, saMap.get(largerKey));
    }

    @Test
    public void computeIfPresentRemove() {
        computeIfPresentRemove(LOW_COLLISION_COUNT);
        computeIfPresentRemove(HIGH_COLLISION_COUNT);
    }

    private void computeIfPresentRemove(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = createCollisionMap(collisions);

        saMap.computeIfPresent(collisions[0], (k, v) -> null);
        saMap.computeIfPresent(collisions[collisions.length - 1], (k, v) -> null);

        for (int i = 1; i < collisions.length - 1; i++) {
            Collision collision = collisions[i];
            assertEquals(collision, saMap.get(collision));
        }
    }

    @Test
    public void compute() {
        compute(LOW_COLLISION_COUNT);
        compute(HIGH_COLLISION_COUNT);
    }

    private void compute(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = createCollisionMap(collisions);

        Collision max = new Collision(Integer.MAX_VALUE);
        saMap.compute(collisions[0], (k, v) -> max);
        saMap.compute(collisions[collisions.length - 1], (k, v) -> max);

        for (int i = 1; i < collisions.length - 1; i++) {
            Collision collision = collisions[i];
            assertEquals(collision, saMap.get(collision));
        }

        assertEquals(max, saMap.get(collisions[0]));
        assertEquals(max, saMap.get(collisions[collisions.length - 1]));
    }

    @Test
    public void keySet() {
        keySet(LOW_COLLISION_COUNT);
        keySet(HIGH_COLLISION_COUNT);
    }

    private void keySet(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = createCollisionMap(collisions);

        Set<Collision> keySet = saMap.keySet();
        for (Collision collision : collisions) {
            assertTrue(keySet.contains(collision));
        }

        HashSet<Collision> expectedSet = new HashSet<>(Arrays.asList(collisions));

        Set<Collision> keySetCopy1 = keySet.stream().collect(Collectors.toSet());
        assertEquals(expectedSet, keySetCopy1);

        Set<Collision> keySetCopy2 = new HashSet<>();
        keySet.forEach(key -> keySetCopy2.add(key));
        assertEquals(expectedSet, keySetCopy2);
    }

    @Test
    public void values() {
        values(LOW_COLLISION_COUNT);
        values(HIGH_COLLISION_COUNT);
    }

    private void values(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = createCollisionMap(collisions);

        Collection<Collision> values = saMap.values();
        for (Collision collision : collisions) {
            assertTrue(values.contains(collision));
        }

        HashSet<Collision> expectedSet = new HashSet<>(Arrays.asList(collisions));

        Set<Collision> keySetCopy1 = values.stream().collect(Collectors.toSet());
        assertEquals(expectedSet, keySetCopy1);

        Set<Collision> keySetCopy2 = new HashSet<>();
        values.forEach(key -> keySetCopy2.add(key));
        assertEquals(expectedSet, keySetCopy2);
    }


    @Test
    public void entrySet() {
        entrySet(LOW_COLLISION_COUNT);
        entrySet(HIGH_COLLISION_COUNT);
    }

    private void entrySet(int collisionCount) {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = createCollisionMap(collisions);

        int i = 0;
        Iterator<Entry<Collision, Collision>> iterator = saMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Collision, Collision> next = iterator.next();
            assertEquals(Collision.class, next.getKey().getClass());
            assertSame(next.getKey(), next.getValue());
            i += 1;
        }
        assertEquals(collisionCount, i);
    }

    @Test
    public void serialize() throws IOException, ClassNotFoundException {
        serialize(LOW_COLLISION_COUNT);
        serialize(HIGH_COLLISION_COUNT);
    }

    private void serialize(int collisionCount) throws IOException, ClassNotFoundException {
        Collision[] collisions = createCollisions(collisionCount);
        Map<Collision, Collision> saMap = createCollisionMap(collisions);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(saMap);
        }

        Map<?, ?> copy;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais)) {
            copy = (Map<?, ?>) ois.readObject();
        }

        assertEquals(saMap, copy);
    }

    static final class Collision implements Comparable<Collision>, Serializable {

        private final int i;

        Collision(int i) {
            this.i = i;
        }

        int getKey() {
            return i;
        }

        @Override
        public int hashCode() {
            // force collision
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Collision)) {
                return false;
            }
            Collision other = (Collision) obj;
            return this.i == other.i;
        }

        @Override
        public int compareTo(Collision o) {
            return Integer.compare(this.i, o.i);
        }

        @Override
        public String toString() {
            return Integer.toString(i);
        }

    }
}
