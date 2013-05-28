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

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSClosestCopy {

    private FSRevisionRoot myRoot;

    private String path;

    public FSClosestCopy() {
    }

    public FSClosestCopy(FSRevisionRoot root, String newPath) {
        myRoot = root;
        path = newPath;
    }

    public FSRevisionRoot getRevisionRoot() {
        return myRoot;
    }

    public String getPath() {
        return path;
    }

}
