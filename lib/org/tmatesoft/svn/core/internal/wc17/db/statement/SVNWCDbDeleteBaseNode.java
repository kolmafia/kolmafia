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
 * DELETE FROM nodes WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = 0;
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteBaseNode extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteBaseNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    protected Object[] getWhere() throws SVNException {
        return new Object[] {
                getBind(1), getBind(2), 0
        };
    }

    protected boolean isFilterPassed() throws SVNException {
        if (isColumnNull(SVNWCDbSchema.NODES__Fields.op_depth)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.NODES__Fields.wc_id)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.NODES__Fields.local_relpath)) {
            return false;
        }
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0 && getColumn(SVNWCDbSchema.NODES__Fields.wc_id).equals(getBind(1))
                && getColumn(SVNWCDbSchema.NODES__Fields.local_relpath).equals(getBind(2));
    }

}
