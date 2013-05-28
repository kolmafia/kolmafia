package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.TARGETS_LIST__Fields;


/*
 *                     
 * INSERT INTO targets_list(wc_id, local_relpath, parent_relpath, kind)
 * SELECT N.wc_id, N.local_relpath, N.parent_relpath, N.kind
 * FROM actual_node AS A JOIN nodes_current AS N
 *   ON A.wc_id = N.wc_id AND A.local_relpath = N.local_relpath
 * WHERE N.wc_id = ?1 AND A.changelist = ?3
 *      AND ((N.parent_relpath = ?2 AND kind = 'file') OR N.local_relpath = ?2)
 */

public class SVNWCDbInsertTargetDepthFilesWithChangelist extends SVNSqlJetInsertStatement {

    private SVNSqlJetSelectStatement nodeCurrent;
    private SVNSqlJetSelectStatement actualNode;
    private Map<String, Object> insertValues;
    
    public SVNWCDbInsertTargetDepthFilesWithChangelist(SVNSqlJetDb sDb) throws SVNException {
    	super(sDb.getTemporaryDb(), SVNWCDbSchema.TARGETS_LIST);
    	nodeCurrent = new SVNWCDbNodesCurrent(sDb);
    	actualNode = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.ACTUAL_NODE, SVNWCDbSchema.ACTUAL_NODE__Indices.I_ACTUAL_CHANGELIST);
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
            actualNode.bindf("s", getBind(3));
            while (actualNode.next()) {
            	try {
                    long wcId = actualNode.getColumnLong(ACTUAL_NODE__Fields.wc_id);
                    String localRelPath = actualNode.getColumnString(ACTUAL_NODE__Fields.local_relpath);
                    nodeCurrent.bindf("is", wcId, localRelPath);
            		if (nodeCurrent.next()) {
                        String kind =  nodeCurrent.getColumnString(SVNWCDbSchema.NODES__Fields.kind);
                        String parentRelPath = nodeCurrent.getColumnString(SVNWCDbSchema.NODES__Fields.parent_relpath);
                        String selectPath = getBind(2).toString();
                        if ((selectPath.equals(parentRelPath) && "file".equals(kind)) || selectPath.equals(localRelPath)) {
                        	super.exec();
                        	n++;
                        }
            		}
            	}
            	finally {
            		nodeCurrent.reset();
            	}
            }
            return n;
        } finally {
        	actualNode.reset();
        }
    }
}
