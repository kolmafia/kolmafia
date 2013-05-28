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
 * -- STMT_DELETE_ACTUAL_NODE_LEAVING_CHANGELIST_RECURSIVE
 * DELETE FROM actual_node
 * WHERE wc_id = ?1
 * AND (?2 = ''
 *      OR local_relpath = ?2
 *      OR (local_relpath > ?2 || '/' AND local_relpath < ?2 || '0'))
 * AND (changelist IS NULL
 *      OR NOT EXISTS (SELECT 1 FROM nodes_current c
 *                     WHERE c.wc_id = ?1 AND c.local_relpath = local_relpath
 *                       AND kind = 'file'))
 *
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteActualNodeLeavingChangelistRecursive extends SVNSqlJetDeleteStatement {
    
    private SVNSqlJetSelectStatement select;

    public SVNWCDbDeleteActualNodeLeavingChangelistRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
        select = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES) {
            @Override
            protected boolean isFilterPassed() throws SVNException {
                return "file".equals(getColumnString(SVNWCDbSchema.NODES__Fields.kind));
            }
        };
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return isRecursive() ? new Object[] {getBind(1)} : new Object[] {getBind(1), getBind(2)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        String rowPath = getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
        if (isRecursive()) {
            String selectPath = getBind(2).toString();
            if (!("".equals(selectPath) || selectPath.equals(rowPath) || rowPath.startsWith(selectPath + '/'))) {
                return false;
            }
        }
        if (getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist) == null) {
            return true;
        } else {
            try {
                select.bindf("is", getColumn(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id), rowPath);
                return !select.next();
            } finally {
                select.reset();
            }
        }
    }
    
    protected boolean isRecursive() {
        return true;
    }}
