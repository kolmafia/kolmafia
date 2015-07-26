package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

/**
 * DELETE FROM externals
 * WHERE wc_id = ?1
 * AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 * AND kind != MAP_DIR
 *
 * @version 1.8
 */
public class SVNWCDbDeleteFileExternalRegistrations extends SVNSqlJetDeleteStatement {
    public SVNWCDbDeleteFileExternalRegistrations(SVNSqlJetDb sDb) throws SVNException {
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
    protected boolean isFilterPassed() throws SVNException {
        return SvnWcDbStatementUtil.getColumnKind(this, SVNWCDbSchema.EXTERNALS__Fields.kind) != ISVNWCDb.SVNWCDbKind.Dir;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }
}
