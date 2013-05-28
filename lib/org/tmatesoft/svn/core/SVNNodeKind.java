/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */

package org.tmatesoft.svn.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * The <b>SVNNodeKind</b> class is used to describe the kind of a 
 * directory entry (node, in other words). This can be:
 * <ul>
 * <li>a directory - the node is a directory
 * <li>a file      - the node is a file
 * <li>none        - the node is missing (does not exist)
 * <li>unknown     - the node kind can not be recognized
 * </ul>
 *  
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see 	SVNDirEntry
 */
public final class SVNNodeKind implements Comparable, Serializable {
    
    private static final long serialVersionUID = 5851L;

    /**
     * This node kind is used to say that a node is missing  
     */
    public static final SVNNodeKind NONE = new SVNNodeKind(2);
    /**
     * Defines the file node kind
     */
    public static final SVNNodeKind FILE = new SVNNodeKind(1);
    /**
     * Defines the directory node kind
     */
    public static final SVNNodeKind DIR = new SVNNodeKind(0);
    /**
     * This node kind is used to say that the kind of a node is
     * actually unknown
     */
    public static final SVNNodeKind UNKNOWN = new SVNNodeKind(3);

    private int myID;

    private SVNNodeKind(int id) {
        myID = id;
    }
    
    /**
     * Parses the passed string and finds out the node kind. For instance,
     * <code>parseKind(<span class="javastring">"dir"</span>)</code> will return 
     * {@link #DIR}.
     * 
     * @param kind 		a node kind as a string
     * @return 			an <b>SVNNodeKind</b> representation
     */
    public static SVNNodeKind parseKind(String kind) {
        if ("file".equals(kind)) {
            return FILE;
        } else if ("dir".equals(kind)) {
            return DIR;
        } else if ("none".equals(kind) || kind == null) {
            return NONE;
        }
        return UNKNOWN;
    }
    /**
     * Represents the current <b>SVNNodeKind</b> object as a string.
     * 
     * @return a string representation of this object.
     */
    public String toString() {
        if (this == NONE) {
            return "none";
        } else if (this == FILE) {
            return "file";
        } else if (this == DIR) {
            return "dir";
        }
        return "unknown";
    }
    
    /**
     * Compares this object with another one.
     * Each <b>SVNNodeKind</b> constant has got its own unique id.
     * 
     * @param   o an object to compare with
     * @return    <ul>
     *            <li>-1 - if <code>o</code> is either <span class="javakeyword">null</span>,
     *            or is not an instance of <b>SVNNodeKind</b>, or the id of
     *            this object is smaller than the id of <code>o</code>;
     *            </li>
     *            <li>1 - if the id of this object is bigger than the id of 
     *            <code>o</code>;
     *            </li>
     *            <li>0 - if and only if <code>o</code> is the same constant 
     *            value as this one (has the same id)
     *            </li>
     *            </ul>
     */
    public int compareTo(Object o) {
        if (o == null || o.getClass() != SVNNodeKind.class) {
            return -1;
        }
        int otherID = ((SVNNodeKind) o).myID;
        return myID > otherID ? 1 : myID < otherID ? -1 : 0;
    }
    
    private void writeObject(ObjectOutputStream os) throws IOException {
        os.writeInt(myID);
    }

    private void readObject(ObjectInputStream is) throws IOException {
        myID = is.readInt();
    }
    
    private Object readResolve() {
        return fromID(myID);
    }

    private static SVNNodeKind fromID(int id) {
        if (DIR.myID == id) {
            return DIR;
        } else if (FILE.myID == id) {
            return FILE;
        } else if (NONE.myID == id) {
            return NONE;
        } 
        return UNKNOWN;
    }

}
