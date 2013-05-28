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
package org.tmatesoft.svn.core.internal.io.fs;

import java.util.Date;

import org.tmatesoft.svn.core.SVNLock;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSLock extends SVNLock {

    private boolean myIsDAVComment;
    
    public FSLock(String path, String id, String owner, String comment, Date created, Date expires, boolean isDAVComment) {
        super(path, id, owner, comment, created, expires);
        myIsDAVComment = isDAVComment;
    }

    public boolean isDAVComment() {
        return myIsDAVComment;
    }

}
