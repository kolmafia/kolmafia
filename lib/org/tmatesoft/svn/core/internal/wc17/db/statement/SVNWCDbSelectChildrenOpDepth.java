package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/*
 * SELECT local_relpath, kind
 * FROM nodes
 * WHERE wc_id = ?1
 * AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 * AND op_depth = ?3
 * ORDER BY local_relpath DESC
 *
 * @version 1.8
 */
public class SVNWCDbSelectChildrenOpDepth extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectChildrenOpDepth(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
        fields.add(SVNWCDbSchema.NODES__Fields.kind);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) > (Long) getBind(3);
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        try {
            return super.openCursor().reverse();
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
        return null;
    }
}
