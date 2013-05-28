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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/**
 * SELECT nodes_base.presence, nodes_work.presence, nodes_work.moved_to FROM
 * nodes nodes_work LEFT OUTER JOIN nodes nodes_base ON nodes_base.wc_id =
 * nodes_work.wc_id AND nodes_base.local_relpath = nodes_work.local_relpath AND
 * nodes_base.op_depth = 0 WHERE nodes_work.wc_id = ?1 AND
 * nodes_work.local_relpath = ?2 AND nodes_work.op_depth = (SELECT MAX(op_depth)
 * FROM nodes WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth > 0);
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectDeletionInfo extends SVNSqlJetSelectStatement {

    public static final String NODES_BASE = "nodes_base";
    private SVNWCDbNodesMaxOpDepth myMaxOpDepth;

    public SVNWCDbSelectDeletionInfo(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        myMaxOpDepth = new SVNWCDbNodesMaxOpDepth(sDb);
    }

    public SVNSqlJetStatement getJoinedStatement(String joinedTable) throws SVNException {
        if (!eof() && NODES_BASE.equalsIgnoreCase(joinedTable)) {
            SVNSqlJetSelectStatement baseNodeStmt = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES);
            baseNodeStmt.bindLong(1, getColumnLong(SVNWCDbSchema.NODES__Fields.wc_id));
            baseNodeStmt.bindString(2, getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath));
            baseNodeStmt.bindLong(3, 0);
            return baseNodeStmt;
        }
        return super.getJoinedStatement(joinedTable);
    }

    protected Object[] getWhere() throws SVNException {
        Long maxOpDepth = myMaxOpDepth.getMaxOpDepth((Long) binds.get(0), (String) binds.get(1));
        if (maxOpDepth != null) {
            bindLong(3, maxOpDepth);
        } else {
            bindNull(3);
        }
        return super.getWhere();
    }

}
