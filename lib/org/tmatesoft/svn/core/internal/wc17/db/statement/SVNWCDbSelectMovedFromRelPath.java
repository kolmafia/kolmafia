package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * SELECT local_relpath, op_depth FROM nodes
 * WHERE wc_id = ?1 AND moved_to = ?2 AND op_depth > 0
 *
 */
public class SVNWCDbSelectMovedFromRelPath extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectMovedFromRelPath(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }
    
    

    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) > 0;
    }

    @Override
    protected String getIndexName() {
        return SVNWCDbSchema.NODES__Indices.I_NODES_MOVED.toString();
    }

}
