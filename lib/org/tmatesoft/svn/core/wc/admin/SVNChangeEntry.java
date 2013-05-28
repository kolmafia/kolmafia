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
package org.tmatesoft.svn.core.wc.admin;

import org.tmatesoft.svn.core.SVNNodeKind;


/**
 * <b>SVNChangeEntry</b> objects are used to pass path change information to clients.
 * These objects are passed to {@link ISVNChangeEntryHandler}.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNChangeEntry {
    /**
     * Char <span class="javastring">'A'</span> (item added).
     */
    public static final char TYPE_ADDED = 'A';

    /**
     * Char <span class="javastring">'D'</span> (item deleted).
     */
    public static final char TYPE_DELETED = 'D';
    
    /**
     * Char <span class="javastring">'U'</span> (item updated).
     */
    public static final char TYPE_UPDATED = 'U';
    
    private String myPath;
    private char myType;
    private String myCopyFromPath;
    private long myCopyFromRevision;
    private boolean myHasTextModifications;
    private boolean myHasPropModifications;
    private SVNNodeKind myKind;
    
    /**
     * Constructs a change entry object.
     * 
     * @param path                  the path of a changed item
     * @param kind                  node kind 
     * @param type                  a change type (one of static fields)
     * @param copyFromPath          a copy-from source path (if the item is copied)
     * @param copyFromRevision      a revision of a copy-from source (if the item is copied)
     * @param hasTextModifications  <span class="javakeyword">true</span> if <code>path</code> 
     *                              is a file and it's modified, <span class="javakeyword">false</span> 
     *                              otherwise
     * @param hasPropModifications  <span class="javakeyword">true</span> if the item has
     *                              property modifications
     */
    public SVNChangeEntry(String path, SVNNodeKind kind, char type, String copyFromPath, long copyFromRevision, boolean hasTextModifications, boolean hasPropModifications) {
        myPath = path;
        myKind = kind;
        myType = type;
        myCopyFromPath = copyFromPath;
        myCopyFromRevision = copyFromRevision;
        myHasTextModifications = hasTextModifications;
        myHasPropModifications = hasPropModifications;
    }
    
    /**
     * Returns a copy-from source path. 
     * 
     * @return a copy-from path
     */
    public String getCopyFromPath() {
        return myCopyFromPath;
    }
    
    /**
     * Returns a copy-from source revision.
     * 
     * @return a copy-from revision number
     */
    public long getCopyFromRevision() {
        return myCopyFromRevision;
    }
    
    /**
     * Returns the absolute path of the changed item represented by 
     * this object.
     * 
     * @return the absolute path
     */
    public String getPath() {
        return myPath;
    }
    
    /**
     * Returns the type of the item change.  
     * 
     * @return a char that is one of static fields of this class
     */
    public char getType() {
        return myType;
    }
    
    /**
     * Says whether the item's properties were 
     * modified. 
     * 
     * @return <span class="javakeyword">true</span> if 
     *         the item has property modifications, otherwise 
     *         <span class="javakeyword">false</span>
     */
    public boolean hasPropertyModifications() {
        return myHasPropModifications;
    }
    
    /**
     * Says whether the file item's contents were 
     * modified. This method is relevant only for 
     * file contents.
     * 
     * @return <span class="javakeyword">true</span> if 
     *         the item has text modifications, otherwise 
     *         <span class="javakeyword">false</span>
     */
    public boolean hasTextModifications() {
        return myHasTextModifications;
    }

    /**
     * Returns the node kind of the item.
     * 
     * @return an item node kind
     */
    public SVNNodeKind getKind() {
        return myKind;
    }
}
