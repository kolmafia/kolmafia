package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * SELECT 1 FROM nodes WHERE wc_id = ?1 AND local_relpath = ?2
 * LIMIT 1 
 */
public class SVNWCDBDoesNodeExists extends SVNSqlJetSelectStatement {

    public SVNWCDBDoesNodeExists(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        ISqlJetCursor cursor = super.openCursor();
        if (cursor != null) {
            try {
                cursor.setLimit(1);
            } catch (SqlJetException e) {
                cursor = null;
            }
        }
        return cursor;
    }
    
    

}
