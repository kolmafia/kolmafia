package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * CREATE VIEW NODES_BASE AS
 *  SELECT * FROM nodes
 *  WHERE op_depth = 0;
 */
public class SVNWCDbNodesBase extends SVNSqlJetSelectStatement {

    private String previousPath;

    public SVNWCDbNodesBase(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        String currentPath = getColumnString(NODES__Fields.local_relpath);
        if (previousPath != null && previousPath.equals(currentPath)) {
            return false;
        }
        previousPath = currentPath;
        return getColumnLong(NODES__Fields.op_depth) == 0;
    }
}
