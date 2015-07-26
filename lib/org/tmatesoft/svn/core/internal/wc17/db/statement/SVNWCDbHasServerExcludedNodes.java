package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

public class SVNWCDbHasServerExcludedNodes extends SVNSqlJetSelectStatement {

    public SVNWCDbHasServerExcludedNodes(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0
                && SvnWcDbStatementUtil.getColumnPresence(this) == ISVNWCDb.SVNWCDbStatus.ServerExcluded;
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }
}
