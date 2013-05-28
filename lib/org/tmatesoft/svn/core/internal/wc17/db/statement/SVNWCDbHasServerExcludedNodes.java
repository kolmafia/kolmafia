package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

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
        if (getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) != 0) {
            return false;
        }
        return "absent".equals(getColumnString(SVNWCDbSchema.NODES__Fields.presence));
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }
}
