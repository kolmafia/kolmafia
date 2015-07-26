package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;

/**
 * DELETE FROM nodes
 * WHERE wc_id = ?1
 * AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 * AND op_depth = 0
 * AND file_external IS NOT NULL
 *
 * @version 1.8
 */
public class SVNWCDbDeleteFileExternals extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteFileExternals(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0 && !isColumnNull(SVNWCDbSchema.NODES__Fields.file_external);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}
