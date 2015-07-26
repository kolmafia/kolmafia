package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/*
 * SELECT op_depth
 * FROM nodes
 * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth < ?3
 * ORDER BY op_depth DESC
 * LIMIT 1
 *
 * @version 1.8
 */
public class SVNWCDbSelectHighestWorkingNode extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectHighestWorkingNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) < (Long) getBind(3);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        try {
            ISqlJetCursor cursor = super.openCursor().reverse();
            cursor.setLimit(1);
            return cursor;
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
        return null;
    }
}
