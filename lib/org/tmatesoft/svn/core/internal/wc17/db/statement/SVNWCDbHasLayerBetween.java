package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/*
 * SELECT 1 FROM NODES
 * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth > ?3 AND op_depth < ?4
 *
 * @version 1.8
 */
public class SVNWCDbHasLayerBetween extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbHasLayerBetween(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        long opDepth = getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
        return opDepth > (Long)getBind(3) && opDepth < (Long)getBind(4);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }
}
