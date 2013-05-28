package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;

/**
 * -- STMT_DELETE_ALL_LAYERS
 * DELETE FROM nodes
 * WHERE wc_id = ?1 AND local_relpath = ?2
 *
 */
public class SVNWCDbDeleteAllLayers extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteAllLayers(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }
}
