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

import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/**
 * SELECT local_relpath FROM nodes
 * WHERE wc_id = ?1 AND parent_relpath = ?2
 * AND (op_depth > (SELECT MAX(op_depth) FROM nodes
 *                  WHERE wc_id = ?1 AND local_relpath = ?2)
 *      OR
 *      (op_depth = (SELECT MAX(op_depth) FROM nodes
 *                   WHERE wc_id = ?1 AND local_relpath = ?2)
 *        AND presence != 'base-deleted'))
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectWorkingChildren extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {
    
    private long parentMaxOpDepth;

    public SVNWCDbSelectWorkingChildren(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES, SVNWCDbSchema.NODES__Indices.I_NODES_PARENT);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
    }

    protected boolean isFilterPassed() throws SVNException {
        long rowOpDepth = getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
        if (rowOpDepth > parentMaxOpDepth) {
            return true;
        }
        String presence = getColumnString(SVNWCDbSchema.NODES__Fields.presence);
        if (!"base-deleted".equals(presence)) {
            return rowOpDepth == parentMaxOpDepth;
        }
        return false;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        SVNWCDbNodesMaxOpDepth maxOpDepth = new SVNWCDbNodesMaxOpDepth(sDb, 0);     
        parentMaxOpDepth = maxOpDepth.getMaxOpDepth((Long) getBind(1),  (String) getBind(2));
        
        return super.openCursor();
    }
    
    
}
