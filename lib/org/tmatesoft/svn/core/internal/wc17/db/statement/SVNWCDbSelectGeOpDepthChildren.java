package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

public class SVNWCDbSelectGeOpDepthChildren extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectGeOpDepthChildren(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES, SVNWCDbSchema.NODES__Indices.I_NODES_PARENT);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }


    @Override
    protected boolean isFilterPassed() throws SVNException {
        long opDepth = getColumnLong(NODES__Fields.op_depth);
        long selectOpDepth = (Long) getBind(3);
        return opDepth > selectOpDepth || 
            (opDepth == selectOpDepth && !"base-deleted".equals(getColumnString(NODES__Fields.presence)));
    }
    
    

}
