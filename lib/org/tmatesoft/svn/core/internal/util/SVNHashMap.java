/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.util;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNHashMap implements Map, Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Object NULL_KEY = new Object();
    private static final int INITIAL_CAPACITY = 16;

    private static boolean ourIsCompatibilityMode = Boolean.getBoolean("svnkit.compatibleHash");
    
    private transient TableEntry[] myTable;
    private transient int myEntryCount;
    private transient int myModCount;
    
    private transient volatile Set myKeySet;
    private transient volatile Set myEntrySet;
    private transient volatile Collection myValueCollection;

    public SVNHashMap() {
        this(null);
    }
    
    public SVNHashMap(Map map) {
        init();
        putAll(map);
    }

    protected void init() {
        myTable = new TableEntry[INITIAL_CAPACITY];
        myEntryCount = 0;
    }

    public void clear() {
        Arrays.fill(myTable, null);
        myEntryCount = 0;
        myModCount++;
    }

    public boolean isEmpty() {
        return myEntryCount == 0;
    }

    public boolean containsKey(Object key) {
        if (isEmpty()) {
            return false;
        }
        key = key == null ? NULL_KEY : key;

        int hash = hashCode(key);
        int index = indexForHash(hash);
        TableEntry entry = myTable[index];
        while (entry != null) {
            if (entry.hash == hash && eq(key, entry.key)) {
                return true;
            }
            entry = entry.next;
        }
        return false;
    }

    public boolean containsValue(Object value) {
        if (isEmpty()) {
            return false;
        }
        if (value == null) {
            return containsNullValue();
        }
        for (int i = 0; i < myTable.length; i++) {
            TableEntry entry = myTable[i];
            while (entry != null) {
                if (value.equals(entry.getValue())) {
                    return true;
                }
                entry = entry.next;
            }
        }
        return false;
    }
    
    private boolean containsNullValue() {
        for (int i = 0; i < myTable.length; i++) {
            TableEntry entry = myTable[i];
            while (entry != null) {
                if (entry.getValue() == null) {
                    return true;
                }
                entry = entry.next;
            }
        }
        return false;
    }

    public Object get(Object key) {
        key = key == null ? NULL_KEY : key;

        int hash = hashCode(key); 
        int index = indexForHash(hash);
        TableEntry entry = myTable[index];
        
        while (entry != null) {
            if (hash == entry.hash && eq(key, entry.key)) {
                return entry.getValue();
            }
            entry = entry.next;
        }
        return null;
    }

    public int size() {
        return myEntryCount;
    }

    public Object put(Object key, Object value) {
        key = key == null ? NULL_KEY : key;
        
        int hash = hashCode(key);
        int index = indexForHash(hash);
        
        TableEntry entry = myTable[index];
        TableEntry previousEntry = null;
        
        while (entry != null) {
            if (entry.hash == hash && entry.key.equals(key)) {
                myModCount++;
                return entry.setValue(value);
            }
            previousEntry = entry;
            entry = entry.next;
        }
        TableEntry newEntry = createTableEntry(key, value, hash);
        
        if (previousEntry != null) {
            previousEntry.next = newEntry;
        } else {
            myTable[index] = newEntry;
        }
        myEntryCount++;
        myModCount++;
        if (myEntryCount >= myTable.length) {
            resize(myTable.length * 2);
        }
        return null;
    }
    
    protected TableEntry createTableEntry(Object key, Object value, int hash) {
        return new TableEntry(key, value, hash);
    }

    public Object remove(Object key) {
        if (isEmpty()) {
            return null;
        }
        key = key == null ? NULL_KEY : key;

        int hash = hashCode(key);
        int index = indexForHash(hash);
        
        TableEntry entry = myTable[index];
        TableEntry previousEntry = null;
        
        while (entry != null) {
            if (entry.hash == hash && entry.key.equals(key)) {
                if (previousEntry != null) {
                    previousEntry.next = entry.next;
                } else {
                    myTable[index] = entry.next;
                }
                myEntryCount--;
                myModCount++;
                return entry.getValue();
            }
            previousEntry = entry;
            entry = entry.next;
        }
        return null;
    }

    public void putAll(Map t) {
        if (t == null || t.isEmpty()) {
            return;
        }
        if (myEntryCount + t.size() >= myTable.length) {
            resize((myEntryCount + t.size())*2);
        }
        for (Iterator entries = t.entrySet().iterator(); entries.hasNext();) {
            Map.Entry entry = (Map.Entry) entries.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public Set keySet() {
        if (myKeySet == null) {
            myKeySet = new KeySet();
        }
        return myKeySet;
    }

    public Set entrySet() {
        if (myEntrySet == null) {
            myEntrySet = new EntrySet();
        }
        return myEntrySet;
    }

    public Collection values() {
        if (myValueCollection == null) {
            myValueCollection = new ValueCollection();
        }
        return myValueCollection;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        Map t = (Map) o;
        if (t.size() != size()) {
            return false;
        }
        try {
            Iterator i = entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                Object key = e.getKey();
                Object value = e.getValue();
                if (value == null) {
                    if (!(t.get(key) == null && t.containsKey(key))) {
                        return false;
                    }
                } else {
                    if (!value.equals(t.get(key))) {
                        return false;
                    }
                }
            }
        } catch(ClassCastException unused)   {
            return false;
        } catch(NullPointerException unused) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        Iterator i = entrySet().iterator();
        while (i.hasNext()) {
            h += i.next().hashCode();
        }
        return h;
    }

    public Object clone() throws CloneNotSupportedException {
        try {
            super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
        
        SVNHashMap result = new SVNHashMap();
        result.myTable = new TableEntry[myTable.length];
        result.myEntryCount = myEntryCount;
        result.myModCount = myModCount;
        result.putAll(this);
        return result;
    }
    
    private void writeObject(ObjectOutputStream s) throws IOException {
        Iterator i = (myEntryCount > 0) ? entrySet().iterator() : null;
        
        s.defaultWriteObject();
        s.writeInt(myTable.length);
        s.writeInt(myEntryCount);

        if ( i == null) {
            return;
        }

        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }
    
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    
        int numBuckets = s.readInt();
        myTable = new TableEntry[numBuckets];
    
        // Read in size (number of Mappings)
        int size = s.readInt();
    
        // Read the keys and values, and put the mappings in the HashMap
        for (int i=0; i < size; i++) {
            Object key = s.readObject();
            Object value = s.readObject();
            put(key, value);
        }
    }


    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{");

        Iterator i = entrySet().iterator();
        boolean hasNext = i.hasNext();
        while (hasNext) {
            Map.Entry e = (Map.Entry) (i.next());
            Object key = e.getKey();
            Object value = e.getValue();
            buf.append(key == this ? "(this Map)" : key);
            buf.append("=");buf.append(value == this ? "(this Map)" : value);

            hasNext = i.hasNext();
            if (hasNext) {
                buf.append(", ");
            }
        }
        buf.append("}");
        return buf.toString();
    }

    private int indexForHash(int hash) {
        return (myTable.length - 1) & hash;
    }
    
    private static int hashCode(Object key) {
        if (ourIsCompatibilityMode && String.class == key.getClass()) {
            int hash = 0;
            String str = (String) key;
            for (int i = 0; i < str.length(); i++) {
                hash = hash*33 + str.charAt(i);
            }
            return hash;
        } else if (key.getClass() == File.class) {
            return hashCode(((File) key).getPath());
        }
        return key.hashCode();
    }
    
    private void resize(int newSize) {
        TableEntry[] oldTable = myTable;
        myTable = new TableEntry[newSize];

        for (int i = 0; i < oldTable.length; i++) {
            TableEntry oldEntry = oldTable[i];
            while (oldEntry != null) {
                int index = indexForHash(oldEntry.hash);
                TableEntry newEntry = myTable[index];
                if (newEntry == null) {
                    myTable[index] = oldEntry;
                } else {
                    while (newEntry.next != null) {
                        newEntry = newEntry.next;
                    }
                    newEntry.next = oldEntry;                    
                }
                TableEntry nextEntry = oldEntry.next;
                oldEntry.next = null;
                oldEntry = nextEntry;
            }
        }
    }
    
    private static boolean eq(Object a, Object b) {
        return a == b || a.equals(b);
    }
    
    private class KeySet extends AbstractSet {
        public Iterator iterator() {
            return new KeyIterator();
        }
        public int size() {
            return myEntryCount;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            return SVNHashMap.this.remove(o) != null;
        }
        public void clear() {
            SVNHashMap.this.clear();
        }
    }

    private class EntrySet extends AbstractSet {
        public Iterator iterator() {
            return new TableIterator();
        }
        
        public int size() {
            return myEntryCount;
        }
        
        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) o;
                if (SVNHashMap.this.containsKey(entry.getKey())) {
                    Object value = SVNHashMap.this.get(entry.getKey());
                    if (value == null) {
                        return entry.getValue() == null;
                    }
                    return value.equals(entry.getValue());
                }
            }
            return false;
        }
        
        public boolean remove(Object o) {
            if (contains(o)) {
                Map.Entry entry = (Map.Entry) o;
                return SVNHashMap.this.remove(entry.getKey()) != null;
            }
            return false;
        }
        public void clear() {
            SVNHashMap.this.clear();
        }
    }

    private class ValueCollection extends AbstractCollection {
        public Iterator iterator() {
            return new ValueIterator();
        }
        public int size() {
            return myEntryCount;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            SVNHashMap.this.clear();
        }
    }
    
    private class TableIterator implements Iterator {
        
        private int index;
        private TableEntry entry;
        private TableEntry previous;
        private int modCount;
        
        public TableIterator() {
            index = 0;
            entry = null;
            modCount = myModCount;
            while (index < myTable.length && entry == null) {
                entry = myTable[index];
                index++;
            }
        }

        public boolean hasNext() {
            return entry != null;
        }

        public Object next() {
            if (myModCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (entry == null) {
                throw new NoSuchElementException();
            }
            previous = entry;
            entry = entry.next;
            while (entry == null && index < myTable.length) {
                entry = myTable[index];
                index++;
            }
            return previous;
        }

        public void remove() {
            if (myModCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (previous != null) {
                SVNHashMap.this.remove(previous.getKey());
                previous = null;
                modCount = myModCount;
            } else {
                throw new IllegalStateException();
            }
        }
    }
    
    private class KeyIterator extends TableIterator {

        public Object next() {
            TableEntry next = (TableEntry) super.next();
            return next.getKey();
        }
    }

    private class ValueIterator extends TableIterator {

        public Object next() {
            TableEntry next = (TableEntry) super.next();
            return next.getValue();
        }
    }
    
    protected static class TableEntry implements Map.Entry {
        
        private TableEntry next;
        private Object key;
        private Object value;
        private int hash;

        protected TableEntry() {            
        }
        
        public TableEntry(Object key, Object value, int hash) {
            init(key, value, hash);
        }

        protected void init(Object key, Object value, int hash) {
            this.key = key;
            setValue(value);
            this.hash = hash;
        }

        public Object setValue(Object value) {
            Object oldValue = getValue();
            this.value = value; 
            return oldValue;
        }

        public Object getValue() {
            return value;
        }

        public Object getKey() {
            return key == NULL_KEY ? null : key;
        }
        
        public int hashCode() {
            return (key == NULL_KEY ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                
                if (v1 == v2 || (v1 != null && v1.equals(v2))) { 
                    return true;
                }
            }
            return false;
        }
    }
}
