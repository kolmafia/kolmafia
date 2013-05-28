package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.REVERT_LIST__Fields;

public class SVNWCDbSelectRevertListCopiedChildren extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectRevertListCopiedChildren(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.REVERT_LIST);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        if (isColumnNull(REVERT_LIST__Fields.repos_id)) {
            return false;
        }
        String path = getColumnString(REVERT_LIST__Fields.local_relpath);
        if ("".equals(path)) {
            return false;
        }
        String selectPath = (String) getBind(1);
        if ("".equals(selectPath) || path.startsWith(selectPath + "/")) {
            Long selectOpDepth = (Long) getBind(2);
            long opDepth = getColumnLong(REVERT_LIST__Fields.op_depth);
            return opDepth >= selectOpDepth;
        }
        return false;
    }
    
    

}
