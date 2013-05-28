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

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * SELECT local_dir_relpath, locked_levels FROM wc_lock
 * WHERE wc_id = ?1
 * AND ((local_dir_relpath <= ?2 AND local_dir_relpath >= ?3)
 *      OR local_dir_relpath = '')
 * ORDER BY local_dir_relpath DESC
 */
public class SVNWCDbSelectAncestorWCLocks extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectAncestorWCLocks(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.WC_LOCK);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        try {
            return getTable().scope(getIndexName(), null, getWhere()).reverse();
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
            return null;
        }
    }
    
    
    

}
