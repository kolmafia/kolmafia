package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * SELECT local_relpath, op_depth, presence, kind
 * FROM nodes
 * WHERE wc_id = ?1 AND parent_relpath = ?2
 * GROUP BY local_relpath
 * ORDER BY op_depth DESC
 *
 * We omit GROUP BY, it is not really needed, and ORDER BY is achieved by use of a reverse cursor. 
 */
public class SVNWCDbSelectNodeChildrenWalkerInfo extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectNodeChildrenWalkerInfo(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES, SVNWCDbSchema.NODES__Indices.I_NODES_PARENT);
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        ISqlJetCursor cursor = super.openCursor();
        if (cursor != null) {
            try {
                cursor = cursor.reverse();
            } catch (SqlJetException e) {
                cursor = null;
            }
        }
        return cursor;
    }    
}
