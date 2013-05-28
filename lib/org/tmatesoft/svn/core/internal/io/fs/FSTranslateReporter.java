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

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.ISVNReporter;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSTranslateReporter implements ISVNReporter {
    private FSRepository myDelegate; 
    private boolean myIsRepositoryClosed;
    
    public FSTranslateReporter(FSRepository delegate) {
        myDelegate = delegate;
        myIsRepositoryClosed = false;
    }
    
    public void abortReport() throws SVNException {
        if (!myIsRepositoryClosed) {
            myDelegate.abortReport();
        }
    }

    public void deletePath(String path) throws SVNException {
        if (!myIsRepositoryClosed) {
            myDelegate.deletePath(path);
        }
    }

    public void finishReport() throws SVNException {
        if (!myIsRepositoryClosed) {
            myDelegate.finishReport();
        }
    }

    public void linkPath(SVNURL url, String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
        if (!myIsRepositoryClosed) {
            myDelegate.linkPath(url, path, lockToken, revision, startEmpty);
        }
    }

    public void linkPath(SVNURL url, String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
        if (!myIsRepositoryClosed) {
            myDelegate.linkPath(url, path, lockToken, revision, depth, startEmpty);
        }
    }

    public void setPath(String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
        if (!myIsRepositoryClosed) {
            myDelegate.setPath(path, lockToken, revision, startEmpty);
        }
    }

    public void setPath(String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
        if (!myIsRepositoryClosed) {
            myDelegate.setPath(path, lockToken, revision, depth, startEmpty);
        }
    }
    
    public void closeRepository() throws SVNException {
        if (!myIsRepositoryClosed) {
            myDelegate.closeRepository();
            myIsRepositoryClosed = true;
        }
    }
}
