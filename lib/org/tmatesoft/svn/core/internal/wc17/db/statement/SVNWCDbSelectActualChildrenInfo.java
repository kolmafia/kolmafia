package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * 
 * SELECT prop_reject, changelist, conflict_old, conflict_new, 
 * conflict_working, tree_conflict_data, properties, local_relpath,
 * conflict_data
 * FROM actual_node
 * WHERE wc_id = ?1 AND parent_relpath = ?2
 *
 */
public class SVNWCDbSelectActualChildrenInfo extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectActualChildrenInfo(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE, SVNWCDbSchema.ACTUAL_NODE__Indices.I_ACTUAL_PARENT);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }
    
    

}
