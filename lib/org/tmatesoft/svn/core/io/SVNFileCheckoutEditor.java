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
package org.tmatesoft.svn.core.io;

import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;


/**
 * The <b>SVNFileCheckoutEditor</b> is an adapter which only handles file contents and properties during a 
 * checkout and redirects that information to its {@link ISVNFileCheckoutTarget} handler. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
class SVNFileCheckoutEditor implements ISVNEditor {
    
    private ISVNFileCheckoutTarget myTarget;
    private SVNDeltaProcessor myDeltaProcessor;

    public SVNFileCheckoutEditor(ISVNFileCheckoutTarget target) {
        myTarget = target;
        myDeltaProcessor = new SVNDeltaProcessor();
    }

    public void abortEdit() throws SVNException {
    }

    public void absentDir(String path) throws SVNException {
    }

    public void absentFile(String path) throws SVNException {
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
    }

    public void changeFileProperty(String path, String name, SVNPropertyValue value) throws SVNException {
        myTarget.filePropertyChanged(path, name, value);
    }

    public void closeDir() throws SVNException {
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        return null;
    }

    public void closeFile(String path, String textChecksum) throws SVNException {
    }

    public void deleteEntry(String path, long revision) throws SVNException {
    }

    public void openDir(String path, long revision) throws SVNException {
    }

    public void openFile(String path, long revision) throws SVNException {
    }

    public void openRoot(long revision) throws SVNException {
    }

    public void targetRevision(long revision) throws SVNException {
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        myDeltaProcessor.applyTextDelta(SVNFileUtil.DUMMY_IN, myTarget.getOutputStream(path), false);
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        return myDeltaProcessor.textDeltaChunk(diffWindow);
    }

    public void textDeltaEnd(String path) throws SVNException {
        myDeltaProcessor.textDeltaEnd();
    }

}
