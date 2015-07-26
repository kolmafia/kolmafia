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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/**
 * SELECT (SELECT b.presence FROM nodes AS b
 * WHERE b.wc_id = ?1 AND b.local_relpath = ?2 AND b.op_depth = 0),
 * work.presence, work.op_depth
 * FROM nodes_current AS work
 * WHERE work.wc_id = ?1 AND work.local_relpath = ?2 AND work.op_depth > 0
 * LIMIT 1
 *
 * @version 1.8
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectDeletionInfo extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    private InternalSelect internalStatement;
    private SVNWCDbNodesMaxOpDepth maxOpDepth;

    public SVNWCDbSelectDeletionInfo(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        maxOpDepth = new SVNWCDbNodesMaxOpDepth(sDb, 0);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.presence);
        fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
    }

    public InternalSelect getInternalStatement() throws SVNException {
        if (internalStatement == null) {
            internalStatement = new InternalSelect(sDb);
            internalStatement.bindf("isi", getBind(1), getBind(2), 0);
            internalStatement.next();
        }
        return internalStatement;
    }

    @Override
    public void reset() throws SVNException {
        super.reset();
        if (internalStatement != null) {
            internalStatement.reset();
            internalStatement = null;
        }
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        try {
            ISqlJetCursor cursor = super.openCursor();
            cursor.setLimit(1);
            return cursor;
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
        return null;

    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) > 0;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2), maxOpDepth.getMaxOpDepth((Long)getBind(1), (String)getBind(2))};
    }

    public static class InternalSelect extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        public InternalSelect(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.presence);
        }
    }
}
