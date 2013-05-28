package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.DELETE_LIST__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

public class SVNWCDbInsertDeleteList extends SVNSqlJetInsertStatement {

    private SVNSqlJetSelectStatement select;
    private Map<String, Object> insertValues;

    public SVNWCDbInsertDeleteList(SVNSqlJetDb sDb) throws SVNException {
        super(sDb.getTemporaryDb(), SVNWCDbSchema.DELETE_LIST);

        this.select = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES) {
            @Override
            protected Object[] getWhere() throws SVNException {
                return new Object[] {getBind(1)};
            }

            private boolean isMaxOpDepth(long opDepth) throws SVNException {
                SVNWCDbNodesMaxOpDepth stmt = new SVNWCDbNodesMaxOpDepth(sDb, 0);
                Long maxOpDepth = stmt.getMaxOpDepth((Long) getBind(1), getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath)); 
                return maxOpDepth != null && maxOpDepth == opDepth;
            }
            @Override
            protected boolean isFilterPassed() throws SVNException {
                final String presence = getColumnString(NODES__Fields.presence);
                if ("base-deleted".equals(presence) ||
                    "not-present".equals(presence) ||
                    "excluded".equals(presence) ||
                    "absent".equals(presence)) {
                    return false;
                }
                long selectOpDepth = (Long) getBind(3);
                long rowOpDepth = getColumnLong(NODES__Fields.op_depth);
                if (rowOpDepth < selectOpDepth) {
                    return false;
                }
                return isMaxOpDepth(rowOpDepth);
            }
            
            @Override
            protected String getPathScope() {
                return (String) getBind(2);
            }
            
            
        };
    }

    @Override
    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String,Object> selectedRow = select.getRowValues();
        if (insertValues == null) {
            insertValues = new HashMap<String, Object>();
        }
        insertValues.clear();
        insertValues.put(DELETE_LIST__Fields.local_relpath.toString(), selectedRow.get(NODES__Fields.local_relpath.toString()));
        return insertValues;
    }
    
    public long exec() throws SVNException {
        select.bindf("isi", getBind(1), getBind(2), getBind(3));
        try {
            int n = 0;
            while (select.next()) {
                super.exec();
                n++;
            }
            return n;
        } finally {
            select.reset();
        }
    }

}
