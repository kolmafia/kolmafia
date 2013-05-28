package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;

/**
 *-- STMT_DELETE_ACTUAL_NODE_RECURSIVE
 * DELETE FROM actual_node
 * WHERE wc_id = ?1
 * AND (?2 = ''
 *      OR local_relpath = ?2
 *       OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 */
public class SVNWCDbDeleteActualNodeRecursive extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteActualNodeRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] { getBind(1) };
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        String queryPath = (String) getBind(2);
        if ("".equals(queryPath)) {
            return true;
        }
        String rowPath = getColumnString(ACTUAL_NODE__Fields.local_relpath);
        return queryPath.equals(rowPath) || rowPath.startsWith(queryPath + "/");
    }
    
    

}
