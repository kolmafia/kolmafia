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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * DELETE FROM wc_lock
 * WHERE wc_id = ?1
 * AND (?2 = ''
 *      OR local_dir_relpath = ?2
 *      OR (local_dir_relpath > ?2 || '/' AND local_dir_relpath < ?2 || '0'))
 * AND NOT EXISTS (SELECT 1 FROM nodes
 *                  WHERE nodes.wc_id = ?1
 *                    AND nodes.local_relpath = wc_lock.local_dir_relpath)
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteLockOrphanRecursive extends SVNSqlJetDeleteStatement {
    
    SVNSqlJetSelectStatement select;

    public SVNWCDbDeleteLockOrphanRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.WC_LOCK);
        select = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return isRecursive() ? new Object[] {getBind(1)} : new Object[] {getBind(1), getBind(2)}; 
    }
    
    protected boolean isRecursive() {
        return true;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        if (isColumnNull(SVNWCDbSchema.WC_LOCK__Fields.local_dir_relpath)) {
            return false;
        }
        String rowPath = getColumnString(SVNWCDbSchema.WC_LOCK__Fields.local_dir_relpath);
        if (isRecursive()) {
            String selectPath = getBind(2).toString();
            if (!("".equals(selectPath) || rowPath.equals(selectPath) || rowPath.startsWith(selectPath + '/'))) {
                return false;
            }
        }
        try {
            select.bindf("is", 
                    getColumnLong(SVNWCDbSchema.WC_LOCK__Fields.wc_id), 
                    rowPath);
            return !select.next();
        } finally {
            select.reset();
        }
    }
}
