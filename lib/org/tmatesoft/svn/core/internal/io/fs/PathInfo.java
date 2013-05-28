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

import org.tmatesoft.svn.core.SVNDepth;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class PathInfo {

    String myPath;
    String myLinkPath;
    String myLockToken;
    long myRevision;
    boolean startEmpty;
    SVNDepth myDepth;
    
    public PathInfo(String path, String linkPath, String lockToken, long revision, SVNDepth depth, boolean empty) {
        myPath = path;
        myLinkPath = linkPath;
        myLockToken = lockToken;
        myRevision = revision;
        startEmpty = empty;
        myDepth = depth;
    }

    public String getLinkPath() {
        return myLinkPath;
    }

    public String getLockToken() {
        return myLockToken;
    }

    public String getPath() {
        return myPath;
    }

    public long getRevision() {
        return myRevision;
    }

    public boolean isStartEmpty() {
        return startEmpty;
    }

    public static boolean isRelevant(PathInfo pathInfo, String prefix) {
        /* Return true if pathInfo's path is a child of prefix. */
        return pathInfo != null && pathInfo.getPath().startsWith(prefix) && ("".equals(prefix) || pathInfo.getPath().charAt(prefix.length()) == '/');
    }

    public SVNDepth getDepth() {
        return myDepth;
    }

}
