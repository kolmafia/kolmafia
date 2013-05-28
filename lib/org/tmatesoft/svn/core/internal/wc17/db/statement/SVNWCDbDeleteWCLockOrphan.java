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
package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;

/**
 * DELETE FROM wc_lock
 * WHERE wc_id = ?1 AND local_dir_relpath = ?2
 * AND NOT EXISTS (SELECT 1 FROM nodes
 *                WHERE nodes.wc_id = ?1
 *                  AND nodes.local_relpath = wc_lock.local_dir_relpath)
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteWCLockOrphan extends SVNWCDbDeleteLockOrphanRecursive {

    public SVNWCDbDeleteWCLockOrphan(SVNSqlJetDb sDb) throws SVNException {
        super(sDb);
    }

    @Override
    protected boolean isRecursive() {
        return false;
    }
}
