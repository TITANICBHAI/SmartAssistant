package java.util;

/**
 * Placeholder implementation of ArrayList for compatibility purposes.
 * This is a simplified version to make compilation succeed.
 */
public class ArrayList<E> implements List<E>, Cloneable, java.io.Serializable {
    private Object[] elementData;
    private int size;
    private static final int DEFAULT_CAPACITY = 10;
    
    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public ArrayList() {
        elementData = new Object[DEFAULT_CAPACITY];
        size = 0;
    }
    
    /**
     * Constructs an empty list with the specified initial capacity.
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            elementData = new Object[0];
        } else {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        size = 0;
    }
    
    /**
     * Constructs a list containing the elements of the specified collection.
     */
    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        size = elementData.length;
    }
    
    /**
     * Returns the number of elements in this list.
     */
    public int size() {
        return size;
    }
    
    /**
     * Returns true if this list contains no elements.
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Returns true if this list contains the specified element.
     */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }
    
    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     */
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++) {
                if (elementData[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (o.equals(elementData[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     */
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size - 1; i >= 0; i--) {
                if (elementData[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = size - 1; i >= 0; i--) {
                if (o.equals(elementData[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     */
    public Object[] toArray() {
        Object[] result = new Object[size];
        System.arraycopy(elementData, 0, result, 0, size);
        return result;
    }
    
    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            return (T[]) toArray();
        }
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }
    
    @SuppressWarnings("unchecked")
    private E elementData(int index) {
        return (E) elementData[index];
    }
    
    /**
     * Returns the element at the specified position in this list.
     */
    public E get(int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return elementData(index);
    }
    
    /**
     * Replaces the element at the specified position in this list with
     * the specified element.
     */
    public E set(int index, E element) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }
    
    /**
     * Appends the specified element to the end of this list.
     */
    public boolean add(E e) {
        ensureCapacity(size + 1);
        elementData[size++] = e;
        return true;
    }
    
    /**
     * Inserts the specified element at the specified position in this list.
     */
    public void add(int index, E element) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        ensureCapacity(size + 1);
        System.arraycopy(elementData, index, elementData, index + 1, size - index);
        elementData[index] = element;
        size++;
    }
    
    /**
     * Removes the element at the specified position in this list.
     */
    public E remove(int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        E oldValue = elementData(index);
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(elementData, index + 1, elementData, index, numMoved);
        }
        elementData[--size] = null;
        return oldValue;
    }
    
    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.
     */
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index >= 0) {
            remove(index);
            return true;
        }
        return false;
    }
    
    /**
     * Removes all of the elements from this list.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            elementData[i] = null;
        }
        size = 0;
    }
    
    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's Iterator.
     */
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacity(size + numNew);
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }
    
    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacity(size + numNew);
        int numMoved = size - index;
        if (numMoved > 0) {
            System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
        }
        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }
    
    /**
     * Removes from this list all of its elements that are contained in the
     * specified collection.
     */
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            if (c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }
    
    /**
     * Retains only the elements in this list that are contained in the
     * specified collection.
     */
    public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }
    
    /**
     * Returns true if this list contains all of the elements of the
     * specified collection.
     */
    public boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     */
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }
    
    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     */
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        return new ListIterator<E>() {
            int cursor = index;
            int lastRet = -1;
            
            public boolean hasNext() {
                return cursor < size;
            }
            
            public E next() {
                if (cursor >= size) {
                    throw new NoSuchElementException();
                }
                lastRet = cursor;
                return elementData(cursor++);
            }
            
            public boolean hasPrevious() {
                return cursor > 0;
            }
            
            public E previous() {
                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }
                lastRet = --cursor;
                return elementData(cursor);
            }
            
            public int nextIndex() {
                return cursor;
            }
            
            public int previousIndex() {
                return cursor - 1;
            }
            
            public void remove() {
                if (lastRet < 0) {
                    throw new IllegalStateException();
                }
                ArrayList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
            }
            
            public void set(E e) {
                if (lastRet < 0) {
                    throw new IllegalStateException();
                }
                ArrayList.this.set(lastRet, e);
            }
            
            public void add(E e) {
                ArrayList.this.add(cursor++, e);
                lastRet = -1;
            }
        };
    }
    
    /**
     * Returns a view of the portion of this list between the specified
     * fromIndex, inclusive, and toIndex, exclusive.
     */
    public List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", size: " + size);
        }
        
        // Create a new ArrayList with the elements in the subList
        ArrayList<E> subList = new ArrayList<>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            subList.add(elementData(i));
        }
        return subList;
    }
    
    /**
     * Ensures that the capacity of this ArrayList is at least the specified
     * minimum capacity.
     */
    private void ensureCapacity(int minCapacity) {
        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            Object[] oldData = elementData;
            elementData = new Object[newCapacity];
            System.arraycopy(oldData, 0, elementData, 0, size);
        }
    }
    
    /**
     * Returns an iterator over the elements in this list in proper sequence.
     */
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int cursor = 0;
            int lastRet = -1;
            
            @Override
            public boolean hasNext() {
                return cursor < size;
            }
            
            @Override
            public E next() {
                if (cursor >= size) {
                    throw new NoSuchElementException();
                }
                lastRet = cursor;
                return elementData(cursor++);
            }
            
            @Override
            public void remove() {
                if (lastRet < 0) {
                    throw new IllegalStateException();
                }
                ArrayList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
            }
        };
    }
    
    /**
     * Creates and returns a copy of this object.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        try {
            ArrayList<E> v = (ArrayList<E>) super.clone();
            v.elementData = new Object[size];
            System.arraycopy(elementData, 0, v.elementData, 0, size);
            v.size = size;
            return v;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }
}