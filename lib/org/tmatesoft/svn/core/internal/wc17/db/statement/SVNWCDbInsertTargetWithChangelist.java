package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.TARGETS_LIST__Fields;


/*
 * 
 * INSERT INTO targets_list(wc_id, local_relpath, parent_relpath, kind)
 * SELECT N.wc_id, N.local_relpath, N.parent_relpath, N.kind
 * FROM actual_node AS A JOIN nodes_current AS N
 *   ON A.wc_id = N.wc_id AND A.local_relpath = N.local_relpath
 * WHERE N.wc_id = ?1 AND A.changelist = ?3 AND N.local_relpath = ?2
 */

public class SVNWCDbInsertTargetWithChangelist extends SVNSqlJetInsertStatement {

    private SVNSqlJetSelectStatement nodeCurrent;
    private SVNSqlJetSelectStatement actualNode;
    private Map<String, Object> insertValues;
    
    public SVNWCDbInsertTargetWithChangelist(SVNSqlJetDb sDb) throws SVNException {
    	super(sDb.getTemporaryDb(), SVNWCDbSchema.TARGETS_LIST);
    	actualNode = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.ACTUAL_NODE) {
    	    
        	@Override
            protected boolean isFilterPassed() throws SVNException {
        	    String rowChangelist = getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist);
        	    if (rowChangelist != null && rowChangelist.equals(SVNWCDbInsertTargetWithChangelist.this.getBind(3))) {
        	        return true;
        	    }
                return false;
            }

            @Override
        	public SVNSqlJetStatement getJoinedStatement(String joinedTable) throws SVNException {
                if (!eof() && "ACTUAL_NODE".equalsIgnoreCase(joinedTable)) {
                    SVNSqlJetSelectStatement actualNodesStmt = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.ACTUAL_NODE);
                    actualNodesStmt.bindLong(1, getColumnLong(SVNWCDbSchema.NODES__Fields.wc_id));
                    actualNodesStmt.bindString(2, getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath));
                    return actualNodesStmt;
                }
                return super.getJoinedStatement(joinedTable);
            }
        };
        nodeCurrent = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES) {
        	@Override
            protected ISqlJetCursor openCursor() throws SVNException {
                ISqlJetCursor cursor = super.openCursor();
                if (cursor != null) {
                    try {
                        cursor = cursor.reverse();
                    } catch (SqlJetException e) {
                        cursor = null;
                    }
                }
                return cursor;
            }
        };
    }

    @Override
    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String,Object> selectedRow = nodeCurrent.getRowValues();
        if (insertValues == null) {
            insertValues = new HashMap<String, Object>();
        }
        insertValues.clear();
        insertValues.put(TARGETS_LIST__Fields.wc_id.toString(), selectedRow.get(NODES__Fields.wc_id.toString()));
        insertValues.put(TARGETS_LIST__Fields.local_relpath.toString(), selectedRow.get(NODES__Fields.local_relpath.toString()));
        insertValues.put(TARGETS_LIST__Fields.parent_relpath.toString(), selectedRow.get(NODES__Fields.parent_relpath.toString()));
        insertValues.put(TARGETS_LIST__Fields.kind.toString(), selectedRow.get(NODES__Fields.kind.toString()));
        return insertValues;
    }
    
    @Override
    public long exec() throws SVNException {
        try {
            int n = 0;
            actualNode.bindf("is", (Long)getBind(1), (String)getBind(2));
            while (actualNode.next()) {
                String actualChangelist = actualNode.getColumnString(ACTUAL_NODE__Fields.changelist);
                if (actualChangelist != null && actualChangelist.equals(getBind(3))) {
                	try {
                		nodeCurrent.bindf("is", (Long)getBind(1), (String)getBind(2));
                		if (nodeCurrent.next()) {
                			super.exec();
                			n++;
                		}
                	}
                	finally {
                		nodeCurrent.reset();
                	}
                }
            }
            return n;
        } finally {
        	actualNode.reset();
        }
    }

}
