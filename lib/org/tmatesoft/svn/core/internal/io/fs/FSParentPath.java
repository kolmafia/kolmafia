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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSParentPath {

    private FSRevisionNode myRevNode;

    private String myEntryName;

    private FSParentPath myParent;

    private FSCopyInheritance myCopyInheritance;

    public FSParentPath(FSParentPath newParentPath) {
        myRevNode = newParentPath.myRevNode;
        myEntryName = newParentPath.myEntryName;
        myParent = newParentPath.myParent;
        myCopyInheritance = newParentPath.myCopyInheritance;
    }

    public FSParentPath(FSRevisionNode newRevNode, String newEntry, FSParentPath newParentPath) {
        myRevNode = newRevNode;
        myEntryName = newEntry;
        myParent = newParentPath;
        if (newRevNode != null) {
            myCopyInheritance = new FSCopyInheritance(FSCopyInheritance.COPY_ID_INHERIT_UNKNOWN, newRevNode.getCopyFromPath());
        } else {
            myCopyInheritance = new FSCopyInheritance(FSCopyInheritance.COPY_ID_INHERIT_UNKNOWN, null);
        }
    }

    public FSRevisionNode getRevNode() {
        return myRevNode;
    }

    public void setRevNode(FSRevisionNode newRevNode) {
        myRevNode = newRevNode;
    }

    public String getEntryName() {
        return myEntryName;
    }

    public FSParentPath getParent() {
        return myParent;
    }

    public int getCopyStyle() {
        return myCopyInheritance.getStyle();
    }

    public void setCopyStyle(int newCopyStyle) {
        myCopyInheritance.setStyle(newCopyStyle);
    }

    public String getCopySourcePath() {
        return myCopyInheritance.getCopySourcePath();
    }

    public void setCopySourcePath(String newCopyPath) {
        myCopyInheritance.setCopySourcePath(newCopyPath);
    }

    public void setParentPath(FSRevisionNode newRevNode, String newEntry, FSParentPath newParentPath) {
        myRevNode = newRevNode;
        myEntryName = newEntry;
        myParent = newParentPath;
        myCopyInheritance = new FSCopyInheritance(FSCopyInheritance.COPY_ID_INHERIT_UNKNOWN, null);
    }

    public String getAbsPath() throws SVNException {
        String pathSoFar = "/";
        if (myParent != null) {
            pathSoFar = myParent.getAbsPath();
        }
        return SVNPathUtil.getAbsolutePath(SVNPathUtil.append(pathSoFar, getEntryName()));
    }
    
    public String getRelativePath(FSParentPath ancestor) {
        String pathSoFar = "";
        FSParentPath thisNode = this;
        while (thisNode != ancestor) {
            pathSoFar = SVNPathUtil.append(thisNode.getEntryName(), pathSoFar);
            thisNode = thisNode.getParent();
        }
        return pathSoFar;
    }

}
