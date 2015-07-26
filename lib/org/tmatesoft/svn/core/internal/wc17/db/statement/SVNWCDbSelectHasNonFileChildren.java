package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

/**
 * SELECT 1 FROM nodes
 * WHERE wc_id = ?1 AND parent_relpath = ?2 AND op_depth = 0 AND kind != MAP_FILE
 *
 * @version 1.8
 */
public class SVNWCDbSelectHasNonFileChildren extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectHasNonFileChildren(SVNSqlJetDb sDb, Enum<?> fromTable) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnString(SVNWCDbSchema.NODES__Fields.parent_relpath).equals(getBind(2)) &&
                getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0 &&
                SvnWcDbStatementUtil.getColumnKind(this, SVNWCDbSchema.NODES__Fields.kind) != ISVNWCDb.SVNWCDbKind.File;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}
