package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
/**
 * -- STMT_DELETE_PRISTINE_IF_UNREFERENCED
 * DELETE FROM pristine
 * WHERE checksum = ?1 AND refcount = 0
 */
public class SVNWCDbDeletePristineIfUnreferenced extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeletePristineIfUnreferenced(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.PRISTINE);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
    
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.PRISTINE__Fields.refcount) == 0;
    }
}
