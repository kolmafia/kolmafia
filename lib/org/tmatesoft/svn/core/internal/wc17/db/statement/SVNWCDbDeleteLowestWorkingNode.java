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

/**
 * DELETE FROM nodes WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth =
 * (SELECT MIN(op_depth) FROM nodes WHERE wc_id = ?1 AND local_relpath = ?2 AND
 * op_depth > 0) AND presence = 'base-deleted';
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteLowestWorkingNode extends SVNSqlJetDeleteStatement {

    private SVNWCDbNodesMinOpDepth minOpDepthSelect;
    private Long minOpDepth = null;

    public SVNWCDbDeleteLowestWorkingNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        minOpDepthSelect = new SVNWCDbNodesMinOpDepth(sDb, 1);
    }

    public void reset() throws SVNException {
        minOpDepth = null;
        super.reset();
    }

    protected Object[] getWhere() throws SVNException {
        minOpDepth = minOpDepthSelect.getMinOpDepth((Long) getBind(1), getBind(2).toString());
        return new Object[] {
                getBind(1), getBind(2), minOpDepth
        };
    }

    protected boolean isFilterPassed() throws SVNException {
        if (isColumnNull(SVNWCDbSchema.NODES__Fields.presence)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.NODES__Fields.wc_id)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.NODES__Fields.local_relpath)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.NODES__Fields.op_depth)) {
            return false;
        }
        return "base-deleted".equals(getColumn(SVNWCDbSchema.NODES__Fields.presence)) && getColumn(SVNWCDbSchema.NODES__Fields.wc_id).equals(getBind(1))
                && getColumn(SVNWCDbSchema.NODES__Fields.local_relpath).equals(getBind(2)) && getColumn(SVNWCDbSchema.NODES__Fields.op_depth).equals(minOpDepth);
    }

}
