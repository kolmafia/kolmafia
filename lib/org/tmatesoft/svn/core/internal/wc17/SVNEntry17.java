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

import java.io.File;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNEntry;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNEntry17 extends SVNEntry {

    private File path;

    public SVNEntry17(File path) {
        this.path = path;
    }

    public SVNAdminArea getAdminArea() {
        return null;
    }
    
    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    @Override
    public Map getTreeConflicts() throws SVNException {
        return null;
    }

    @Override
    public boolean isThisDir() {
        return getName() == null || "".equals(getName());
    }

}
