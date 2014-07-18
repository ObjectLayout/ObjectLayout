
import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.fill;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.ObjectLayout.CtorAndArgs;
import org.ObjectLayout.CtorAndArgsProvider;
import org.ObjectLayout.StructuredArray;

@SuppressWarnings("rawtypes")
public class BPlusTree<K, V> implements Iterable<Map.Entry<K, V>> {
    private final int nodeSize;
    private final Comparator comparator;
    private final Leaf firstNode;
    private Node root;
    private int size;

    static <T> CtorAndArgs<T> defaultCtorAndArgs(Class<T> clazz) {
        try {
            return new CtorAndArgs<T>(clazz, new Class[0]);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException();
        }
    }

    private static <T> CtorAndArgsProvider<T> defaultProvider(final Class<T> clazz) {
        try {
            return new CtorAndArgsProvider<T>(clazz) {
                @Override
                public CtorAndArgs<T> getForIndex(long... index)
                        throws NoSuchMethodException {
                    return defaultCtorAndArgs(clazz);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static final CtorAndArgs<Leaf> LEAF_CTOR_AND_ARGS = defaultCtorAndArgs(Leaf.class);
    static final CtorAndArgsProvider<Entry> ENTRY_PROVIDER = defaultProvider(Entry.class);

    public BPlusTree(int nodeSize) {
        this(nodeSize, null);
    }


    public BPlusTree(int nodeSize, Comparator<K> comparator) {
        this.nodeSize = nodeSize;
        this.comparator = comparator;

        firstNode = Leaf.newInstance(nodeSize);
        this.root = firstNode;
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V val) {
        if (key == null || val == null) {
            throw new NullPointerException("Keys and values may not be null");
        }

        Object o = root.put(comparator, key, val);

        if (isSplit(o)) {
            Node next = root.next();
            root = new Branch(root, next, nodeSize);

            o = null;
        }

        if (null == o) {
            size++;
        }

        return (V) o;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return (V) root.get(comparator, key);
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {

        Object o = root.remove(comparator, key);

        if (null != o) {
            size--;

            if (root.hasOnlyChild()) {
                root = (Node) root.firstValue();
            }
        }

        return (V) o;
    }

    public int size() {
        return size;
    }

    static class Entry implements Map.Entry {
        private Object key;
        private Object val;

        public Entry() {
        }

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return val;
        }

        @Override
        public Object setValue(Object value) {
            Object oldValue = this.val;
            this.val = value;
            return oldValue;
        }

        @Override
        public String toString() {
            return "[" + key + "->" + val + "]";
        }

        public void set(Object key, Object val) {
            this.key = key;
            this.val = val;
        }

        public void clear() {
            key = null;
            val = null;
        }
    }

    private static int binarySearch(StructuredArray<Entry> entries, int fromIndex,
            int toIndex, Object key, Comparator c) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Object midVal = entries.get(mid).getKey();
            int cmp = compare(c, midVal, key);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1); // key not found.
    }

    static class Leaf extends StructuredArray<Entry> implements Node {
        private int size = 0;
        private final int capacity;
        private Leaf next;

        public Leaf() {
            capacity = (int) this.getLength();
        }

        public Object put(Comparator comparator, Object key, Object val) {
            Object oldVal;

            int search = binarySearch(this, 0, size, key, comparator);
            if (search > -1) {
                Entry entry = get(search);
                oldVal = entry.getValue();
                entry.setValue(val);
            } else if (size < capacity) {
                oldVal = null;

                search = -(search + 1);
                insert(search, key, val);

            } else {
                Leaf next = Leaf.newInstance(capacity);

                int halfSize = size / 2;

                shallowCopy(this, halfSize, next, 0, halfSize);
                clear(halfSize, size);

                size = halfSize;
                next.size = halfSize;

                if (compare(comparator, key, next.firstKey()) < 0) {
                    put(comparator, key, val);
                } else {
                    next.put(comparator, key, val);
                }

                next.next = this.next;
                this.next = next;

                oldVal = Node.Sentinal.SPLIT;
            }

            return oldVal;
        }

        private void clear(int offset, int count) {
            for (int i = offset; i < count; i++) {
                get(i).clear();
            }
        }

        public Object get(Comparator comparator, Object key) {
            int search = binarySearch(this, 0, size, key, comparator);
            if (search < 0) {
                return null;
            }

            return get(search).getValue();
        }

        @Override
        public Object remove(Comparator comparator, Object key) {
            int search = binarySearch(this, 0, size, key, comparator);

            if (search < 0) {
                return null;
            }

            return remove(search);
        }

        public BPlusTree.Leaf next() {
            return next;
        }

        @Override
        public boolean requiresCompacting() {
            return size < capacity / 2;
        }

        @Override
        public Object firstKey() {
            return get(0).getKey();
        }

        @Override
        public Object firstValue() {
            return get(0).getValue();
        }

        @Override
        public Object lastKey() {
            return get(size - 1).getKey();
        }

        @Override
        public Object lastValue() {
            return get(size - 1).getValue();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            for (Entry entry : this) {
                sb.append(entry.getKey()).append("->").append(entry.getValue());
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]");

            return sb.toString();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean hasOnlyChild() {
            return false;
        }

        public void mergeFrom(Node right) {
            assert (right.size() == capacity / 2) || (size == capacity / 2) : "Should have exactly half capacity nodes";

            Leaf leaf = (Leaf) right;

            shallowCopy(leaf, 0, this, size, leaf.size());
            next = leaf.next();

            size += leaf.size();
        }

        public boolean stealFromRight(Leaf right) {
            if (right.size() > capacity / 2) {
                append(right.firstKey(), right.firstValue());
                right.removeFirst();
                return true;
            }

            return false;
        }

        public boolean stealFromLeft(Leaf left) {
            if (left.size() > capacity / 2) {
                push(left.lastKey(), left.lastValue());
                left.clearLast();

                return true;
            }

            return false;
        }

        private void removeFirst() {
            remove(0);
        }

        private Object remove(int search) {
            Object o = this.get(search).getValue();
            if (search != size - 1) {
                shallowCopy(this, search + 1, this, search, size - (search + 1));
            }
            this.get(size - 1).clear();
            size--;
            return o;
        }

        private void insert(int search, Object key, Object val) {
            if (search != size) {
                shallowCopy(this, search, this, search + 1, size - search);
            }
            this.get(search).set(key, val);
            size++;
        }

        private void push(Object key, Object val) {
            insert(0, key, val);
        }

        private void append(Object key, Object val) {
            get(size).set(key, val);
            size++;
        }

        private void clearLast() {
            get(size - 1).clear();
            size--;
        }

        public static Leaf newInstance(int nodeSize) {
            return (Leaf) newSubclassInstance(LEAF_CTOR_AND_ARGS, ENTRY_PROVIDER, nodeSize);
        }
    }

    static class Branch implements Node {
        private final Object[] children;
        private final int capacity;
        private int size;

        public Branch(Node left, Node right, int nodeSize) {
            this(nodeSize);

            children[0] = left;
            children[1] = right.firstKey();
            children[2] = right;

            size = 1;
        }

        public Branch(int nodeSize) {
            capacity = nodeSize;
            children = new Object[nodeSize * 2 + 1];

            size = 0;
        }

        @Override
        public Object put(Comparator comparator, Object key, Object val) {
            Node node = findNode(comparator, key);

            Object oldVal = node.put(comparator, key, val);

            if (isSplit(oldVal)) {
                Node nextNode = node.next();
                Object keyForNextNode = nextNode.firstKey();

                if (size == capacity) {
                    splitBranch(comparator, keyForNextNode, nextNode);
                } else {
                    insertNode(comparator, keyForNextNode, nextNode);
                    oldVal = null;
                }
            }

            return oldVal;
        }

        private void splitBranch(Comparator comparator, Object keyForNextNode,
                Node nextNode) {
            int halfSize = size / 2;

            int comparison = compareWithMidValues(comparator, keyForNextNode,
                    halfSize);

            if (comparison == 0) {
                Branch nextBranch = new Branch(capacity);

                // Copy half of the nodes from the original
                int copyFrom = keyOffset(halfSize);
                int length = arraySize() - copyFrom;
                arraycopy(children, copyFrom, nextBranch.children, 1, length);
                nextBranch.size = halfSize;

                // Key from new node moved up.
                nextBranch.firstKey(keyForNextNode);
                nextBranch.children[0] = nextNode;

                // clear out latter half...
                fill(children, keyOffset(halfSize), children.length, null);
                size = halfSize;

                // temporarily store the nextBranch for the parent.
                next(nextBranch);
            } else if (comparison < 0) {
                Branch nextBranch = new Branch(capacity);

                // Copy half of the nodes from the original
                int copyFrom = keyOffset(halfSize);
                int length = arraySize() - copyFrom;
                arraycopy(children, copyFrom, nextBranch.children, 1, length);
                nextBranch.size = halfSize;

                // Last key from first half moved up.
                nextBranch.firstKey(storedKey(halfSize - 1));
                nextBranch.children[0] = (Node) children[halfSize * 2];

                // clear out latter half...
                fill(children, keyOffset(halfSize - 1), children.length, null);
                size = halfSize - 1;

                // Insert the new node
                insertNode(comparator, keyForNextNode, nextNode);

                // temporarily store the nextBranch for the parent.
                next(nextBranch);
            } else {
                Branch nextBranch = new Branch(capacity);

                // Copy just under half of the nodes from the original
                int copyFrom = keyOffset(halfSize + 1);
                int length = arraySize() - copyFrom;
                arraycopy(children, copyFrom, nextBranch.children, 1, length);
                nextBranch.size = halfSize - 1;

                // First key from second half moved up.
                nextBranch.firstKey(storedKey(halfSize));
                nextBranch.children[0] = (Node) children[(halfSize + 1) * 2];

                // clear out latter half...
                fill(children, keyOffset(halfSize), children.length, null);
                size = halfSize;

                // Insert the new node
                nextBranch.insertNode(comparator, keyForNextNode, nextNode);

                // temporarily store the nextBranch for the parent.
                next(nextBranch);
            }
        }

        @Override
        public Object get(Comparator comparator, Object key) {
            return findNode(comparator, key).get(comparator, key);
        }

        @Override
        public Object remove(Comparator comparator, Object key) {
            int index = findKeyIndex(comparator, key);
            int nodeOffset = index * 2;

            Node node = (Node) children[nodeOffset];
            Object oldVal = node.remove(comparator, key);

            if (node.requiresCompacting()) {
                if (node instanceof Leaf) {
                    compactLeaves((Leaf) node, index, nodeOffset);
                } else {
                    compactBranches((Branch) node, index, nodeOffset);
                }
            }

            return oldVal;
        }

        private void compactLeaves(Leaf node, int index, int nodeOffset) {
            if (index + 1 <= size) {
                Leaf left = node;
                Leaf right = (Leaf) children[nodeOffset + 2];

                if (left.stealFromRight(right)) {
                    children[nodeOffset + 1] = right.firstKey();
                } else {
                    left.mergeFrom(right);
                    removeMergedNode(nodeOffset);
                }
            } else {
                Leaf left = (Leaf) children[nodeOffset - 2];
                Leaf right = node;

                if (right.stealFromLeft(left)) {
                    children[nodeOffset - 1] = right.firstKey();
                } else {
                    left.mergeFrom(right);
                    removeMergedNode(nodeOffset);
                }
            }
        }

        private void compactBranches(Branch node, int index, int nodeOffset) {
            if (index + 1 <= size) {
                Branch right = (Branch) children[nodeOffset + 2];
                Object rightKey = children[nodeOffset + 1];

                if (right.size() > capacity / 2) {
                    node.append(rightKey, right.firstValue());
                    Object poppedKey = right.popKey();
                    children[nodeOffset + 1] = poppedKey;
                } else {
                    node.mergeFrom(rightKey, right);
                    removeMergedNode(nodeOffset);
                }
            } else {
                Branch left = (Branch) children[nodeOffset - 2];
                Object nodeKey = children[nodeOffset - 1];

                if (left.size() > capacity / 2) {
                    node.push(left.lastValue(), nodeKey);
                    children[nodeOffset - 1] = left.lastKey();
                    left.clearLast();
                } else {
                    left.mergeFrom(nodeKey, node);
                    removeMergedNode(nodeOffset);
                }
            }
        }

        private void removeMergedNode(int nodeOffset) {
            int length = arraySize() - (nodeOffset + 3);
            if (length > 0) {
                arraycopy(children, nodeOffset + 3, children, nodeOffset + 1,
                        length);
            }
            size--;

            children[arraySize()] = null;
            children[arraySize() + 1] = null;
        }

        public void mergeFrom(Object rightKey, Node right) {
            Branch branch = (Branch) right;
            children[arraySize()] = rightKey;
            arraycopy(branch.children, 0, children, arraySize() + 1,
                    branch.arraySize());
            size += branch.size() + 1;
        }

        private int compareWithMidValues(Comparator comparator, Object key,
                int halfSize) {
            Object keyA = storedKey(halfSize - 1);
            Object keyB = storedKey(halfSize);

            int compareA = compare(comparator, key, keyA);
            int compareB = compare(comparator, key, keyB);

            assert compareA != 0 : "Should not get a key match on split";
            assert compareB != 0 : "Should not get a key match on split";

            return Integer.signum(compareA) + Integer.signum(compareB);
        }

        private void insertNode(Comparator comparator, Object key, Node child) {
            int keyIndex = findKeyIndex(comparator, key);
            int offset = keyIndex * 2 + 1;
            int length = arraySize() - offset;

            if (length > 0) {
                arraycopy(children, offset, children, offset + 2, length);
            }

            children[offset] = key;
            children[offset + 1] = child;
            size++;
        }

        private void next(Branch nextBranch) {
            assert size < capacity : "Can only store next node if node is not full";
            children[children.length - 1] = nextBranch;
        }

        private void firstKey(Object storedKey) {
            assert size < capacity : "Can only store first key if node is not full";
            children[children.length - 1] = storedKey;
        }

        @Override
        public Node next() {
            int offset = children.length - 1;
            Object nextNode = children[offset];

            assert nextNode != null : "Can only fetch next once";
            children[offset] = null;

            return (Node) nextNode;
        }

        @Override
        public Object firstKey() {
            int offset = children.length - 1;
            Object key = children[offset];

            assert key != null : "Can only fetch next once";
            children[offset] = null;

            return key;
        }

        public Object popKey() {
            Object key = children[1];
            int length = arraySize() - 2;
            arraycopy(children, 2, children, 0, length);
            size--;
            children[arraySize()] = null;
            children[arraySize() + 1] = null;
            return key;
        }

        @Override
        public Node firstValue() {
            return (Node) children[0];
        }

        @Override
        public Object lastKey() {
            assert size > 0 : "Should not be modifying branch with only child";

            return children[arraySize() - 2];
        }

        @Override
        public Node lastValue() {
            assert size > 0 : "Should not be modifying branch with only child";

            return (Node) children[arraySize() - 1];
        }

        private void clearLast() {
            assert size > 0 : "Should not be modifying branch with only child";

            children[arraySize() - 1] = null;
            children[arraySize() - 2] = null;
            size--;
        }

        public void push(Node val, Object key) {
            arraycopy(children, 0, children, 2, arraySize());
            children[0] = val;
            children[1] = key;
            size++;
        }

        public void append(Object key, Node val) {
            children[arraySize()] = key;
            children[arraySize() + 1] = val;
            size++;
        }

        @Override
        public boolean requiresCompacting() {
            return size < capacity / 2;
        }

        private Node findNode(Comparator comparator, Object key) {
            return storedNode(findKeyIndex(comparator, key));
        }

        private int keyOffset(int index) {
            return index * 2 + 1;
        }

        private Node storedNode(int search) {
            return (Node) children[search * 2];
        }

        private Object storedKey(int index) {
            return children[keyOffset(index)];
        }

        private int findKeyIndex(Comparator comparator, Object key) {
            int lo = 0;
            int hi = size - 1;

            while (lo <= hi) {
                final int mid = (lo + hi) >>> 1;

                Object stored = storedKey(mid);
                int comparison = compare(comparator, key, stored);

                if (comparison == 0) {
                    return mid + 1;
                } else if (comparison < 0) {
                    hi = mid - 1;
                } else {
                    lo = mid + 1;
                }
            }

            return lo;
        }

        @Override
        public String toString() {
            return "B:" + Arrays.toString(copyOf(children, size * 2 + 1));
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean hasOnlyChild() {
            return size == 0;
        }

        private int arraySize() {
            return size * 2 + 1;
        }
    }

    interface Node {
        enum Sentinal {
            SPLIT
        };

        Object get(Comparator comparator, Object key);

        Object put(Comparator comparator, Object key, Object val);

        Object remove(Comparator comparator, Object key);

        Object lastValue();

        Object lastKey();

        Object firstValue();

        int size();

        boolean requiresCompacting();

        Object firstKey();

        Node next();

        boolean hasOnlyChild();
    }

    @SuppressWarnings("unchecked")
    private static int compare(Comparator comparator, Object a, Object b) {
        if (null == comparator) {
            return ((Comparable) a).compareTo(b);
        } else {
            return comparator.compare(a, b);
        }
    }

    private static boolean isSplit(Object o) {
        return Node.Sentinal.SPLIT == o;
    }

    @Override
    public String toString() {
        return "BPlusTree [nodeSize=" + nodeSize + ", comparator=" + comparator
                + ", root=" + root + "]";
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return new BPlusTreeIterator(firstNode);
    }

    public class BPlusTreeIterator implements Iterator<Map.Entry<K, V>> {
        private Leaf leaf;
        private int index = -1;

        public BPlusTreeIterator(Leaf leaf) {
            this.leaf = leaf;
        }

        @Override
        public boolean hasNext() {
            return (index + 1) < leaf.size() || leaf.next() != null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map.Entry<K, V> next() {
            if ((index + 1) < leaf.size()) {
                index++;

                Entry entry = leaf.get(index);
                return entry;

            } else if (leaf.next() != null) {
                leaf = leaf.next();
                index = 0;

                Entry entry = leaf.get(index);
                return entry;
            }

            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
