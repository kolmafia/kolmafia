package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
/**
 * DELETE FROM lock
 * WHERE repos_id = ?1 AND repos_relpath = ?2
 *
 */
public class SVNWCDbDeleteLock extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteLock(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.LOCK);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }
}
