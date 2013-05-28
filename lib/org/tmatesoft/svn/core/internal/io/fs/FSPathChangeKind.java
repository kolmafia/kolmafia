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
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.Serializable;
import java.util.Map;

import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;

/**
 * The kind of change that occurred on the path.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSPathChangeKind implements Serializable {

    private static final long serialVersionUID = 4845L;
    
    public static final String ACTION_MODIFY = "modify";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_REPLACE = "replace";
    public static final String ACTION_RESET = "reset";

    public static final FSPathChangeKind FS_PATH_CHANGE_MODIFY = new FSPathChangeKind(ACTION_MODIFY);
    public static final FSPathChangeKind FS_PATH_CHANGE_ADD = new FSPathChangeKind(ACTION_ADD);
    public static final FSPathChangeKind FS_PATH_CHANGE_DELETE = new FSPathChangeKind(ACTION_DELETE);
    public static final FSPathChangeKind FS_PATH_CHANGE_REPLACE = new FSPathChangeKind(ACTION_REPLACE);
    public static final FSPathChangeKind FS_PATH_CHANGE_RESET = new FSPathChangeKind(ACTION_RESET);

    private String myName;

    private static final Map ACTIONS_TO_CHANGE_KINDS = new SVNHashMap();

    static {
        ACTIONS_TO_CHANGE_KINDS.put(ACTION_MODIFY, FSPathChangeKind.FS_PATH_CHANGE_MODIFY);
        ACTIONS_TO_CHANGE_KINDS.put(ACTION_ADD, FSPathChangeKind.FS_PATH_CHANGE_ADD);
        ACTIONS_TO_CHANGE_KINDS.put(ACTION_DELETE, FSPathChangeKind.FS_PATH_CHANGE_DELETE);
        ACTIONS_TO_CHANGE_KINDS.put(ACTION_REPLACE, FSPathChangeKind.FS_PATH_CHANGE_REPLACE);
        ACTIONS_TO_CHANGE_KINDS.put(ACTION_RESET, FSPathChangeKind.FS_PATH_CHANGE_RESET);
    }

    private FSPathChangeKind(String name) {
        myName = name;
    }

    public String toString() {
        return myName;
    }
    
    public int hashCode() {
        return myName.hashCode();
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != FSPathChangeKind.class) {
            return false;
        }
        return myName.equals(((FSPathChangeKind) o).myName);
    }
    
    private Object readResolve() {
        return ACTIONS_TO_CHANGE_KINDS.get(myName);
    }

    public static FSPathChangeKind fromString(String changeKindStr) {
        return (FSPathChangeKind) ACTIONS_TO_CHANGE_KINDS.get(changeKindStr);
    }

    public static char getType(FSPathChangeKind kind) {
        if (kind == FSPathChangeKind.FS_PATH_CHANGE_ADD) {
            return SVNLogEntryPath.TYPE_ADDED;
        } else if (kind == FSPathChangeKind.FS_PATH_CHANGE_DELETE) {
            return SVNLogEntryPath.TYPE_DELETED;
        } else if (kind == FSPathChangeKind.FS_PATH_CHANGE_MODIFY) {
            return SVNLogEntryPath.TYPE_MODIFIED;
        }

        return SVNLogEntryPath.TYPE_REPLACED;
    }
    

}
