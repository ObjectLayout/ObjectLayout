package org.ObjectLayout;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class ReferenceArrayTest {

    public static class Stack extends ReferenceArray<Object> {
        private int size = 0;
        private final long capacity;

        public static Stack newInstance(final long length) {
            return newInstance(Stack.class, length);
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
    
    @Test
    public void pushesAndPops() throws Exception {
        Stack s0 = Stack.newInstance(Stack.class, 10); // Just to test the base form of instantiation
        Stack s = Stack.newInstance(10);

        String foo = "foo";
        
        s.push(foo);
        
        assertThat(s.size(), is(1));
        assertThat(s.pop(), is((Object) foo));
        assertThat(s.size(), is(0));
    }
}
