package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * CREATE VIEW NODES_CURRENT AS
 *  SELECT * FROM nodes AS n
 *   WHERE op_depth = (SELECT MAX(op_depth) FROM nodes AS n2
 *                     WHERE n2.wc_id = n.wc_id
 *                       AND n2.local_relpath = n.local_relpath);
 */
public class SVNWCDbNodesCurrent extends SVNSqlJetSelectStatement {

    private String previousPath;

    public SVNWCDbNodesCurrent(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
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
    
    @Override
    protected boolean isFilterPassed() throws SVNException {
        String currentPath = getColumnString(NODES__Fields.local_relpath);
        if (previousPath != null && previousPath.equals(currentPath)) {
            return false;
        }
        previousPath = currentPath;
        return super.isFilterPassed();
    }
}
