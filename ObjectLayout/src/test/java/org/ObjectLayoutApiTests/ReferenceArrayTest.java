package org.ObjectLayoutApiTests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.ObjectLayout.ProtectedReferenceArray;
import org.ObjectLayout.ProtectedStructuredArray;
import org.ObjectLayout.ReferenceArray;
import org.junit.Test;

import java.lang.invoke.MethodHandles;

public class ReferenceArrayTest {

    static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    public static class StackPublic extends ReferenceArray<Object> {
        private int size = 0;
        private final long capacity;

        public static StackPublic newInstance(final long length) {
            return newInstance(StackPublic.class, length);
        }

        public StackPublic() {
            this.capacity = getLength();
        }
        
        public void push(Object o) {
            if (size == capacity) {
                throw new IndexOutOfBoundsException();
            }
            
            set(size, o);
            size++;
        }
        
        public Object pop() {
            if (size == 0) {
                throw new IndexOutOfBoundsException();
            }
            
            Object o = get(--size);
            return o;
        }
        
        public int size() {
            return size;
        }
    }

    private static class Stack extends ReferenceArray<Object> {
        private int size = 0;
        private final long capacity;

        public static Stack newInstance(final long length) {
            return newInstance(lookup, Stack.class, length);
        }

        public Stack() {
            this.capacity = getLength();
        }

        public void push(Object o) {
            if (size == capacity) {
                throw new IndexOutOfBoundsException();
            }

            set(size, o);
            size++;
        }

        public Object pop() {
            if (size == 0) {
                throw new IndexOutOfBoundsException();
            }

            Object o = get(--size);
            return o;
        }

        public int size() {
            return size;
        }
    }


    public static class StackProtected extends ProtectedReferenceArray<Object> {
        private int size = 0;
        private final long capacity;

        public static StackProtected newInstance(final long length) {
            return newInstance(StackProtected.class, length);
        }

        public StackProtected() {
            this.capacity = getLength();
        }

        public void push(Object o) {
            if (size == capacity) {
                throw new IndexOutOfBoundsException();
            }

            set(size, o);
            size++;
        }

        public Object pop() {
            if (size == 0) {
                throw new IndexOutOfBoundsException();
            }

            Object o = get(--size);
            return o;
        }

        public int size() {
            return size;
        }
    }
    
    @Test
    public void pushesAndPops() throws Exception {
        Stack s0 = Stack.newInstance(lookup, Stack.class, 10); // Just to test the base form of instantiation
        Stack s = Stack.newInstance(10); // The convenient way...

        String foo = "foo";
        s.push(foo);
        
        assertThat(s.size(), is(1));
        assertThat(s.pop(), is((Object) foo));
        assertThat(s.size(), is(0));

        Object r = s.get(0); // So, this is possible on a ReferenceArray subclass (but not on ProtectedReferenceArray)....
    }

    @Test
    public void pushesAndPopsPublic() throws Exception {
        StackPublic s0 = StackPublic.newInstance(StackPublic.class, 10); // Just to test the base form of instantiation
        StackPublic s = StackPublic.newInstance(10); // The convenient way...

        String foo = "foo";
        s.push(foo);

        assertThat(s.size(), is(1));
        assertThat(s.pop(), is((Object) foo));
        assertThat(s.size(), is(0));

        Object r = s.get(0); // So, this is possible on a ReferenceArray subclass (but not on ProtectedReferenceArray)....
    }


    @Test
    public void pushesAndPopsProtected() throws Exception {
        StackProtected s = StackProtected.newInstance(10); // The convenient way...

        String foo = "foo";
        s.push(foo);

        assertThat(s.size(), is(1));
        assertThat(s.pop(), is((Object) foo));
        assertThat(s.size(), is(0));
    }
}
