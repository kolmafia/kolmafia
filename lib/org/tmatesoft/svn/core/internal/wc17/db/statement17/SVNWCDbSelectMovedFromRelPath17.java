package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

public class SVNWCDbSelectMovedFromRelPath17  extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectMovedFromRelPath17(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    protected boolean isFilterPassed() throws SVNException {
        return getBind(2).equals(getColumnString(SVNWCDbSchema.NODES__Fields.moved_to)) &&
                getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) > 0;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }
}

