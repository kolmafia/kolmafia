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
package org.tmatesoft.svn.core.internal.wc17.db;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;

/**
 * This structure records all the information that we need to deal with a given
 * working copy directory.
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbDir {

    /** The absolute path to this working copy directory. */
    private File localAbsPath;

    /** What wcroot does this directory belong to? */
    private SVNWCDbRoot wcRoot;

    /** The parent directory's per-dir information. */
    private SVNWCDbDir parent;

    /** Hold onto the old-style access baton that corresponds to this PDH. */
    private SVNWCAccess admAccess;

    public SVNWCDbDir(File localAbsPath) {
        this.localAbsPath = localAbsPath;
    }

    public File getLocalAbsPath() {
        return localAbsPath;
    }

    public SVNWCDbRoot getWCRoot() {
        return wcRoot;
    }

    public SVNWCDbDir getParent() {
        return parent;
    }

    public SVNWCAccess getAdmAccess() {
        return admAccess;
    }

    public void setLocalAbsPath(File localAbsPath) {
        this.localAbsPath = localAbsPath;
    }

    public void setWCRoot(SVNWCDbRoot wcRoot) {
        this.wcRoot = wcRoot;
    }

    public void setParent(SVNWCDbDir parent) {
        this.parent = parent;
    }

    public static boolean isUsable(SVNWCDbDir pdh) {
        return pdh != null && pdh.getWCRoot() != null && pdh.getWCRoot().getFormat() == ISVNWCDb.WC_FORMAT_17;
    }

    public File computeRelPath() {
        final String relativePath = SVNPathUtil.getRelativePath(wcRoot.getAbsPath().toString(), localAbsPath.toString());
        return SVNFileUtil.createFilePath(relativePath);
    }

    public void flushEntries(File localAbspath) throws SVNException {
        if (admAccess != null) {
            admAccess.close();
        }
        if (localAbspath != null && localAbspath.equals(this.localAbsPath) && !localAbspath.equals(wcRoot.getAbsPath())) {
            SVNWCDbDir parentPdh = wcRoot.getDb().navigateToParent(this, Mode.ReadOnly);
            if (parentPdh != null && parentPdh.getAdmAccess() != null) {
                parentPdh.getAdmAccess().close();
            }
        }
    }

}
