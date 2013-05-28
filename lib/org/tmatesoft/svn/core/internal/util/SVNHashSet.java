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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNHashSet extends AbstractSet implements Set, Serializable {

    private static final long serialVersionUID = 4845L;

    private static final Object OBJECT = new Object();

    private transient SVNHashMap myMap;

    public SVNHashSet() {
        myMap = new SVNHashMap();
    }

    public SVNHashSet(Collection values) {
        myMap = new SVNHashMap();
        addAll(values);
    }

    public boolean add(Object o) {
        return myMap.put(o, OBJECT) == null;
    }

    public void clear() {
        myMap.clear();
    }

    public boolean contains(Object o) {
        return myMap.containsKey(o);
    }

    public Iterator iterator() {
        return myMap.keySet().iterator();
    }

    public boolean remove(Object o) {
        return myMap.remove(o) != null;
    }

    public int size() {
        return myMap.size();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeInt(myMap.size());
        if (myMap.size() == 0) {
            return;
        }

        for (Iterator i = iterator(); i.hasNext();) {
            s.writeObject(i.next());
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        myMap = new SVNHashMap();
        int size = s.readInt();
        for (int i = 0; i < size; i++) {
            Object o = s.readObject();
            myMap.put(o, OBJECT);
        }
    }
}
