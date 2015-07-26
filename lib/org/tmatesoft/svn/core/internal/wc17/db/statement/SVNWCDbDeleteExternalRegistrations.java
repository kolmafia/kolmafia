package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;

/**
 * DELETE FROM externals
 * WHERE wc_id = ?1
 * AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 *
 * @version 1.8
 */
public class SVNWCDbDeleteExternalRegistrations extends SVNSqlJetDeleteStatement {
    public SVNWCDbDeleteExternalRegistrations(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.EXTERNALS);
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }
}
