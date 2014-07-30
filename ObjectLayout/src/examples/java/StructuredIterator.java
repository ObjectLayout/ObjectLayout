/*
 * Written by Michael Barker, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.ObjectLayout.StructuredArray;


public final class StructuredIterator<T> implements Iterator<T> {

    private StructuredArray<T> array;
    private long cursor;
    private final long end;

    public StructuredIterator(StructuredArray<T> array, long offset, long length) {
        this.array = array;
        this.cursor = offset - 1;
        this.end = offset + length;
    }

    @Override
    public boolean hasNext() {
        return cursor + 1 < end;
    }

    @Override
    public T next() {
        cursor++;

        if (cursor < end) {
            return array.get(cursor);
        } else {
            cursor--;
            throw new NoSuchElementException();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}