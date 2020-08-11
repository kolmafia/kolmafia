package org.tmatesoft.svn.core.internal.util;

import java.util.Arrays;

public class SVNIntMap {

    public static final int INITIAL_CAPACITY = 8192;
    public static final int CACHE_SIZE = 8192; //must be a power of 2, then (CACHE_SIZE - 1) can serve as mask
    public static final int CACHE_SIZE_MASK = CACHE_SIZE - 1;

    //table entries
    private int[] keys;
    private Object[] values;
    private int[] nextIndices;

    private int[] hashToIndex;
    private int size;
    private int capacity;

    public SVNIntMap() {
        this.size = 0;
        this.capacity = INITIAL_CAPACITY;
        this.keys = new int[capacity];
        this.values = new Object[capacity];
        this.nextIndices = new int[capacity];
        this.hashToIndex = new int[CACHE_SIZE];

        Arrays.fill(keys, -1);
        Arrays.fill(nextIndices, -1);
        Arrays.fill(hashToIndex, -1);
    }

    public boolean containsKey(int key) {
        final int hash = fitTableSize(hash(key));
        return hashToIndex[hash] != -1;
    }

    public Object get(int key) {
        final int hash = fitTableSize(hash(key));
        int index = hashToIndex[hash];

        while (index != -1) {
            if (keys[index] == key) {
                return values[index];
            }
            index = nextIndices[index];
        }
        return null;
    }

    public void put(int key, Object value) {
        maybeGrowCapacity();
        final int hash = fitTableSize(hash(key));
        int index = hashToIndex[hash];

        int previousIndex = -1;
        while (index != -1) {
            if (keys[index] == key) {
                //the entry already exists, rewrite it
                values[index] = value;
                return;
            }
            previousIndex = index;
            index = nextIndices[index];
        }

        int newIndex = size++;
        if (previousIndex != -1) {
            nextIndices[previousIndex] = newIndex;
        }
        if (hashToIndex[hash] == -1) {
            hashToIndex[hash] = newIndex;
        }
        keys[newIndex] = key;
        values[newIndex] = value;
    }

    private void maybeGrowCapacity() {
        if (size < capacity) {
            return;
        }

        int newCapacity = capacity * 2;
        int[] newKeys = new int[newCapacity];
        Object[] newValues = new Object[newCapacity];
        int[] newNextIndices = new int[newCapacity];

        System.arraycopy(keys, 0, newKeys, 0, size);
        System.arraycopy(values, 0, newValues, 0, size);
        System.arraycopy(nextIndices, 0, newNextIndices, 0, size);

        Arrays.fill(newKeys, size, newKeys.length, -1);
        Arrays.fill(newNextIndices, size, newNextIndices.length, -1);

        keys = newKeys;
        values = newValues;
        nextIndices = newNextIndices;
        capacity = newCapacity;
    }

    private int fitTableSize(int hash) {
        return hash & CACHE_SIZE_MASK;
    }

    private int hash(int key) {
        //here can be some custom hashing algorithm
        return key;
    }
}
