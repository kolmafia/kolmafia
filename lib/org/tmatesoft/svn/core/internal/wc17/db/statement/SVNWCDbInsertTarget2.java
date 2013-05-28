package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.TARGETS_LIST__Fields;


/*
 * STMT_INSERT_TARGET
 * INSERT INTO targets_list(wc_id, local_relpath, parent_relpath, kind)
 * SELECT wc_id, local_relpath, parent_relpath, kind
 * FROM nodes_current WHERE wc_id = ?1 AND local_relpath = ?2
 */

public class SVNWCDbInsertTarget2 extends SVNSqlJetInsertStatement {

    private SVNSqlJetSelectStatement select;
    private Map<String, Object> insertValues;
    
    public SVNWCDbInsertTarget2(SVNSqlJetDb sDb) throws SVNException {
    	super(sDb.getTemporaryDb(), SVNWCDbSchema.TARGETS_LIST);
    	select = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES) {
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
        Map<String,Object> selectedRow = select.getRowValues();
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
            select.bindf("is", (Long)getBind(1), (String)getBind(2));
            if (select.next()) {
                super.exec();
                n++;
            }
            return n;
        } finally {
            select.reset();
        }
    }

}
