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
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbNodesMaxOpDepth extends SVNSqlJetSelectStatement {
    
    private long minDepth;

    public SVNWCDbNodesMaxOpDepth(SVNSqlJetDb sDb) throws SVNException {
        this(sDb, 1);
    }
    
    public SVNWCDbNodesMaxOpDepth(SVNSqlJetDb sDb, long minDepth) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        this.minDepth = minDepth;
    }

    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) >= minDepth;
    }

    public Long getMaxOpDepth(Long wcId, String localRelpath) throws SVNException {
        ISqlJetCursor c = null;
        try {
            c = getTable().lookup(null, wcId, localRelpath);
            c = c.reverse();
            if (!c.eof()) {
                long rowDepth = c.getInteger(SVNWCDbSchema.NODES__Fields.op_depth.toString());
                if (rowDepth >= minDepth) {
                    return rowDepth;
                }
            }
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        } finally {
            try {
                c.close();
            } catch (SqlJetException e) {
            }
        }
        return null;
    }

}
