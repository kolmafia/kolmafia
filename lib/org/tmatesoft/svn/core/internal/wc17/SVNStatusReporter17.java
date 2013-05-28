/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17;

import java.util.HashMap;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNStatusReporter17 implements ISVNReporterBaton, ISVNReporter {

    private SVNRepository repository;
    private SVNReporter17 reportBaton;
    private SVNStatusEditor17 editor;
    private SVNURL repositoryLocation;
    private HashMap<String, SVNLock> locks;
    private ISVNReporter reporter;
    private SVNURL repositoryRoot;

    public SVNStatusReporter17(SVNRepository repository, SVNReporter17 reportBaton, SVNStatusEditor17 editor) {
        this.repository = repository;
        this.reportBaton = reportBaton;
        this.editor = editor;
        this.repositoryLocation = repository.getLocation();
        this.locks = new HashMap<String, SVNLock>();
    }

    public void report(ISVNReporter reporter) throws SVNException {
        this.reporter = reporter;
        reportBaton.report(this);
    }

    public void setPath(String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
        setPath(path, lockToken, revision, SVNDepth.INFINITY, startEmpty);
    }

    public void setPath(String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
        this.reporter.setPath(path, lockToken, revision, depth, startEmpty);
    }

    public void deletePath(String path) throws SVNException {
        this.reporter.deletePath(path);
    }

    public void linkPath(SVNURL url, String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
        setPath(path, lockToken, revision, SVNDepth.INFINITY, startEmpty);
    }

    public void linkPath(SVNURL url, String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {

        SVNURL ancestor = SVNURLUtil.getCommonURLAncestor(url, repositoryLocation);

        /* If we got a shorter ancestor, truncate our current ancestor.
           Note that svn_dirent_get_longest_ancestor will allocate its return
           value even if it identical to one of its arguments. */

        if (SVNPathUtil.getPathAsChild(ancestor.getPath(), repositoryLocation.getPath()) != null) {
            repositoryLocation = ancestor;

            // TODO set depth infinity to reportBaton:
            // This depth is only used to report locks in finishReport() method
            // Currently SVNRepository#getLocks doesn't support depth, so the assignment is useless
            // But when it will, the depth should be kept and passed to SVNRepository#getLocks
            // rb->depth = svn_depth_infinity;
        }

        reporter.linkPath(url, path, lockToken, revision, depth, startEmpty);
    }

    public void finishReport() throws SVNException {

        // collect locks
        SVNLock[] locks = null;
        SVNURL oldLocation = this.repository.getLocation();
        try {
            repositoryRoot = this.repository.getRepositoryRoot(true);
            this.repository.setLocation(repositoryLocation, false);
            locks = this.repository.getLocks("");
        } catch (SVNException e) {
            if (!(e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_IMPLEMENTED)) {
                throw e;
            }
        } finally {
            this.repository.setLocation(oldLocation, false);
            this.repository.closeSession();
        }
        if (locks != null) {
            for (int i = 0; i < locks.length; i++) {
                SVNLock lock = locks[i];
                this.locks.put(lock.getPath(), lock);
            }
        }
        this.editor.setRepositoryInfo(this.repositoryRoot, this.locks);
        this.reporter.finishReport();

    }

    public void abortReport() throws SVNException {
        reporter.abortReport();
    }

}
