package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;

/*
 * DELETE FROM lock
 * WHERE repos_id = ?1 AND (repos_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(repos_relpath, ?2))
 *
 * @version 1.8
 */

public class SVNWCDbDeleteLockRecursively extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteLockRecursively(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.LOCK);
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return false;
    }

    @Override
    protected String getRowPath() throws SVNException {
        return getColumnString(SVNWCDbSchema.LOCK__Fields.repos_relpath);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}
