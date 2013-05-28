package org.tmatesoft.svn.core.internal.wc17.db;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.internal.db.ISVNSqlJetTrigger;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.REVERT_LIST__Fields;

public class SvnRevertActualNodesTrigger implements ISVNSqlJetTrigger {
    
    private SVNSqlJetDb db;

    public SvnRevertActualNodesTrigger(SVNSqlJetDb db) {
        this.db = db;
    }
    
    public void beforeDelete(ISqlJetCursor cursor) throws SqlJetException {
        ISqlJetTable table = db.getDb().getTemporaryDatabase().getTable(SVNWCDbSchema.REVERT_LIST.toString());
        
        Map<String, Object> rowValues = new HashMap<String, Object>();
        rowValues.put(REVERT_LIST__Fields.local_relpath.toString(), cursor.getValue(ACTUAL_NODE__Fields.local_relpath.toString()));
        rowValues.put(REVERT_LIST__Fields.actual.toString(), 1);
        rowValues.put(REVERT_LIST__Fields.conflict_old.toString(), cursor.getValue(ACTUAL_NODE__Fields.conflict_old.toString()));
        rowValues.put(REVERT_LIST__Fields.conflict_new.toString(), cursor.getValue(ACTUAL_NODE__Fields.conflict_new.toString()));
        rowValues.put(REVERT_LIST__Fields.conflict_working.toString(), cursor.getValue(ACTUAL_NODE__Fields.conflict_working.toString()));
        rowValues.put(REVERT_LIST__Fields.prop_reject.toString(), cursor.getValue(ACTUAL_NODE__Fields.prop_reject.toString()));
        
        if (!cursor.isNull(ACTUAL_NODE__Fields.properties.toString()) 
                || !cursor.isNull(ACTUAL_NODE__Fields.tree_conflict_data.toString())) {
            rowValues.put(REVERT_LIST__Fields.notify.toString(), 1);
        } else {
            rowValues.put(REVERT_LIST__Fields.notify.toString(), null);
        }
        table.insertByFieldNamesOr(SqlJetConflictAction.REPLACE, rowValues);
    }

    public void beforeUpdate(ISqlJetCursor cursor, Map<String, Object> newValues) throws SqlJetException {
        ISqlJetTable table = db.getDb().getTemporaryDatabase().getTable(SVNWCDbSchema.REVERT_LIST.toString());
        
        Map<String, Object> rowValues = new HashMap<String, Object>();
        rowValues.put(REVERT_LIST__Fields.local_relpath.toString(), cursor.getValue(ACTUAL_NODE__Fields.local_relpath.toString()));
        rowValues.put(REVERT_LIST__Fields.actual.toString(), 1);
        rowValues.put(REVERT_LIST__Fields.conflict_old.toString(), cursor.getValue(ACTUAL_NODE__Fields.conflict_old.toString()));
        rowValues.put(REVERT_LIST__Fields.conflict_new.toString(), cursor.getValue(ACTUAL_NODE__Fields.conflict_new.toString()));
        rowValues.put(REVERT_LIST__Fields.conflict_working.toString(), cursor.getValue(ACTUAL_NODE__Fields.conflict_working.toString()));
        rowValues.put(REVERT_LIST__Fields.prop_reject.toString(), cursor.getValue(ACTUAL_NODE__Fields.prop_reject.toString()));
        
        if (!cursor.isNull(ACTUAL_NODE__Fields.properties.toString()) 
                || !cursor.isNull(ACTUAL_NODE__Fields.tree_conflict_data.toString())) {
            rowValues.put(REVERT_LIST__Fields.notify.toString(), 1);
        } else {
            rowValues.put(REVERT_LIST__Fields.notify.toString(), null);
        }
        table.insertByFieldNamesOr(SqlJetConflictAction.REPLACE, rowValues);
    }

    public void beforeInsert(SqlJetConflictAction conflictAction, ISqlJetTable table, Map<String, Object> newValues) throws SqlJetException {
    }

    public void statementStarted(SqlJetDb db) throws SqlJetException {
        this.db.getDb().getTemporaryDatabase().beginTransaction(SqlJetTransactionMode.WRITE);
    }

    public void statementCompleted(SqlJetDb db, SqlJetException error) throws SqlJetException {
        if (error == null) {
            this.db.getDb().getTemporaryDatabase().commit();
        } else {
            this.db.getDb().getTemporaryDatabase().rollback();
        }
    }

}
