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
package org.tmatesoft.svn.core;


/**
 * This class contains enumeration that describes depth,
 * that is used.
 * The order of these depths is important: the higher the number,
 * the deeper it descends.  You can use it to compare two depths
 * numerically to decide which goes deeper.
 *   
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNDepth implements Comparable {

    /**
     * Depth undetermined or ignored.
     */
    public static final SVNDepth UNKNOWN = new SVNDepth(-2, "unknown"); 
    
    /**
     * Exclude (don't descend into) directory D.
     */
    public static final SVNDepth EXCLUDE = new SVNDepth(-1, "exclude"); 
    
    /**
     * Just the named directory D, no entries. For instance, update will not pull in
     * any files or subdirectories.
     */
    public static final SVNDepth EMPTY = new SVNDepth(0, "empty"); 
    
    /**
     * D and its file children, but not subdirectories. For instance, updates will pull in any
     * files, but not subdirectories.
     */
    public static final SVNDepth FILES = new SVNDepth(1, "files"); 
    
    /**
     * D and its immediate children (D and its entries).  Updates will pull in
     * any files or subdirectories without any children.
     */
    public static final SVNDepth IMMEDIATES = new SVNDepth(2, "immediates"); 
    
    /**
     * D and all descendants (full recursion from D).  For instance, updates will pull
     in any files or subdirectories recursively.
     */
    public static final SVNDepth INFINITY = new SVNDepth(3, "infinity"); 
    
    private int myId;
    private String myName;
    
    private SVNDepth(int id, String name) {
        myId = id;
        myName = name;
    }

    /** 
     * Gets numerical Id of depth
     * @return depth Id
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public int getId() {
        return myId;
    }
    
    /** 
     * Gets the name of depth
     * @return depth name
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public String getName() {
        return myName;
    }
    
    /**
     * Returns a string representation of this object.
     * 
     * @return string representation of this object  
     */
    public String toString() {
        return getName();
    }
    
    /** 
    * Returns a recursion boolean based on depth.
    *
    * Although much code has been converted to use depth, some code still
    * takes a recurse boolean.  In most cases, it makes sense to treat
    * unknown or infinite depth as recursive, and any other depth as
    * non-recursive (which in turn usually translates to <code>FILES</code>).
    * @return if recursion is used
    * @since  SVNKit 1.2.0, SVN 1.5.0
    */
    public boolean isRecursive() {
        return this == INFINITY || this == UNKNOWN;
    }
    
    /**
     * Compares this object to another one.
     * 
     * @param o  object to compare with 
     * @return   <code>-1</code> if <code>o</code> is <span class="javakeyword">null</span>, or not an <code>SVNDepth</code>
     *           instance, or its {@link #getId() id} is greater than this object's id; <code>0</code> if ids this object and <code>o</code> 
     *           are equal; <code>1</code> if id of this object is greater than the one of <code>o</code>.
     */
    public int compareTo(Object o) {
        if (o == null || o.getClass() != SVNDepth.class) {
            return -1;
        }
        SVNDepth otherDepth = (SVNDepth) o;
        return myId == otherDepth.myId ? 0 : (myId > otherDepth.myId ? 1 : -1);
    }

    /**
     * Says whether this object and <code>obj</code> are equal.
     * 
     * @param obj another object to compare with
     * @return <span class="javakeyword">true</span> if equal; otherwise <span class="javakeyword">false</span>
     */
    public boolean equals(Object obj) {
        return compareTo(obj) == 0;
    }
    
    /** 
     * Appropriate name of <code>depth</code> is returned. If <code>depth</code> does not represent
     * a recognized depth, <code>"INVALID-DEPTH"</code> is returned.
     * @param depth depth, which name needs to be returned 
     * @return the name of depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static String asString(SVNDepth depth) {
        if (depth != null) {
            return depth.getName();
        } 
        return "INVALID-DEPTH";
    }
    
    /** 
     * Based on depth determines if it is recursive or not.
     * In most cases, it makes sense to treat unknown or infinite depth as recursive, 
     * and any other depth as non-recursive
     * 
     * @param depth depth value
     * @return if it is recursive
     * @see #isRecursive()
     * @see #fromRecurse(boolean)
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static boolean recurseFromDepth(SVNDepth depth) {
        return depth == null || depth == INFINITY || depth == UNKNOWN;
    }
    
    /**
     * Treats recursion as <code>INFINITY</code> depth and <code>FILES</code> otherwise
     * @param recurse indicator of recursion
     * @return depth
     * @see #isRecursive()
     * @see #recurseFromDepth(SVNDepth)
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth fromRecurse(boolean recurse) {
        return recurse ? INFINITY : FILES;
    }
    
    /**
     * Based on string value finds <code>SVNDepth</code> value.
     * @param string depth value represented by string
     * @return depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth fromString(String string) {
        if (EMPTY.getName().equals(string)) {
            return EMPTY;
        } else if (EXCLUDE.getName().equals(string)) {
            return EXCLUDE;
        } else if (FILES.getName().equals(string)) {
            return FILES;
        } else if (IMMEDIATES.getName().equals(string)) {
            return IMMEDIATES;
        } else if (INFINITY.getName().equals(string)) {
            return INFINITY;
        } else {
            return UNKNOWN;
        }
    }
    
    /** 
     * Based on depth id returns <code>SVNDepth</code> value.
     * @param id depth id
     * @return depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth fromID(int id) { 
        switch (id) {
            case 3:
                return INFINITY;
            case 2:
                return IMMEDIATES;
            case 1:
                return FILES;
            case 0:
                return EMPTY;
            case -1:
                return EXCLUDE;
            case -2:
            default:
                return UNKNOWN;
        }
    }
    
    /** 
     * Returns <code>INFINITY</code> if <code>recurse</code> is <code>true</code>, else
     * returns <code>EMPTY</code>.
     * Code should never need to use this, it is called only from pre-depth APIs, for compatibility.
     * @param recurse boolean
     * @return depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth getInfinityOrEmptyDepth(boolean recurse) {
        return recurse ? INFINITY : EMPTY;
    }
    
    /** 
     * The same as {@link #getInfinityOrEmptyDepth(boolean)}, but <code>FILES</code> is returned when recursive.
     * Code should never need to use this, it is called only from pre-depth APIs, for compatibility.
     * @param recurse boolean
     * @return depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth getInfinityOrFilesDepth(boolean recurse) {
        return recurse ? INFINITY : FILES;
    }
    
    /** 
     * The same as {@link #getInfinityOrEmptyDepth(boolean)}, but <code>IMMEDIATES</code> is returned when recursive.
     * Code should never need to use this, it is called only from pre-depth APIs, for compatibility.
     * @param recurse boolean
     * @return depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth getInfinityOrImmediatesDepth(boolean recurse) {
        return recurse ? INFINITY : IMMEDIATES;
    }

    /** 
     * Returns <code>UNKNOWN</code> if <code>recurse</code> is <code>true</code>, else
     * returns <code>EMPTY</code>.
     * Code should never need to use this, it is called only from pre-depth APIs, for compatibility.
     * @param recurse boolean
     * @return depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth getUnknownOrEmptyDepth(boolean recurse) {
        return recurse ? UNKNOWN : EMPTY;
    }
    
    /** 
     * The same as {@link #getUnknownOrEmptyDepth(boolean)}, but <code>FILES</code> is returned when recursive.
     * Code should never need to use this, it is called only from pre-depth APIs, for compatibility.
     * @param recurse boolean
     * @return depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth getUnknownOrFilesDepth(boolean recurse) {
        return recurse ? UNKNOWN : FILES;
    }
    
    /** 
     * The same as {@link #getUnknownOrEmptyDepth(boolean)}, but <code>IMMEDIATES</code> is returned when recursive.
     * Code should never need to use this, it is called only from pre-depth APIs, for compatibility.
     * @param recurse boolean
     * @return depth
     * @since  SVNKit 1.2.0, SVN 1.5.0
     */
    public static SVNDepth getUnknownOrImmediatesDepth(boolean recurse) {
        return recurse ? UNKNOWN : IMMEDIATES;
    }
}
