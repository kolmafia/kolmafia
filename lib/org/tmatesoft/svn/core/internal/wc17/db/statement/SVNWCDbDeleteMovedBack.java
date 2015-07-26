package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;

/**
 * DELETE FROM nodes
 * WHERE wc_id = ?1
 * AND (local_relpath = ?2
 * OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 * AND op_depth = ?3
 *
 * @version 1.8
 */
public class SVNWCDbDeleteMovedBack extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteMovedBack(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isStrictiDescendant() {
        return false;
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == (Long)getBind(3);
    }
}
