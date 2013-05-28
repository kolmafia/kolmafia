package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
/**
 * -- STMT_DELETE_SHADOWED_RECURSIVE
 * DELETE FROM nodes
 *  WHERE wc_id = ?1
 *  AND (local_relpath = ?2
 *      OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 *  AND (op_depth < ?3
 *      OR (op_depth = ?3 AND presence = 'base-deleted')) *
 */
public class SVNWCDbDeleteShadowedRecursive extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteShadowedRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        long rowDepth = getColumnLong(NODES__Fields.op_depth);
        String rowPath = getColumnString(NODES__Fields.local_relpath);
        
        long queryDepth = (Long) getBind(3);
        String queryPath = (String) getBind(2);
        
        if (rowPath.equals(queryPath) || rowPath.startsWith(queryPath + "/")) {
            if (rowDepth < queryDepth) {
                return true;
            }
            if (rowDepth == queryDepth) {
                return "base-deleted".equals(getColumnString(NODES__Fields.presence));
            }
        }
        return false;
    }
}
