package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

public class SVNWCDbUpdateOpDepthIncreaseRecursive extends SVNSqlJetUpdateStatement {

    public SVNWCDbUpdateOpDepthIncreaseRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }
    
    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> values = getRowValues();
        values.put(NODES__Fields.op_depth.toString(), ((Long) getBind(3)) + 1);
        return values;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        String selectPath = (String) getBind(2);
        long selectDepth = (Long) getBind(3);
        if ("".equals(selectPath)) {
            return selectDepth == getColumnLong(NODES__Fields.op_depth);
        }
        String rowPath = getColumnString(NODES__Fields.local_relpath);
        if (rowPath.startsWith(selectPath + "/")) {
            return selectDepth == getColumnLong(NODES__Fields.op_depth);
        }
        return false;
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }
}
