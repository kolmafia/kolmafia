package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

public class SVNWCDbSelectMovedToNode extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {
    public SVNWCDbSelectMovedToNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
        fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return !isColumnNull(SVNWCDbSchema.NODES__Fields.moved_to);
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

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1), getBind(2)};
    }
}
