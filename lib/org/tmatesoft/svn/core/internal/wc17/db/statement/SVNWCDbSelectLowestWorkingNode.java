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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * SELECT op_depth, presence, kind, moved_to
 * FROM nodes
 * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth > ?3
 * ORDER BY op_depth
 * LIMIT 1
 *
 * @version 1.8
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectLowestWorkingNode extends SVNSqlJetSelectFieldsStatement<NODES__Fields> {

    public SVNWCDbSelectLowestWorkingNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
        fields.add(SVNWCDbSchema.NODES__Fields.presence);
        fields.add(SVNWCDbSchema.NODES__Fields.kind);
        fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
    }

    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) > (Long) getBind(3);
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        ISqlJetCursor cursor = super.openCursor();
        if (cursor != null) {
            try {
                cursor.setLimit(1);
            } catch (SqlJetException e) {
                cursor = null;
            }
        }
        return cursor;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }
}
