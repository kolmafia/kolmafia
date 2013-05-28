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
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.REVERT_LIST__Fields;

public class SvnRevertNodesTrigger implements ISVNSqlJetTrigger {
    
    private SVNSqlJetDb db;

    public SvnRevertNodesTrigger(SVNSqlJetDb db) {
        this.db = db;
    }
    
    public void beforeDelete(ISqlJetCursor cursor) throws SqlJetException {
        ISqlJetTable table = db.getDb().getTemporaryDatabase().getTable(SVNWCDbSchema.REVERT_LIST.toString());
        
        Map<String, Object> rowValues = new HashMap<String, Object>();
        rowValues.put(REVERT_LIST__Fields.local_relpath.toString(), cursor.getValue(NODES__Fields.local_relpath.toString()));
        rowValues.put(REVERT_LIST__Fields.actual.toString(), 0);
        rowValues.put(REVERT_LIST__Fields.op_depth.toString(), cursor.getValue(NODES__Fields.op_depth.toString()));
        rowValues.put(REVERT_LIST__Fields.repos_id.toString(), cursor.getValue(NODES__Fields.repos_id.toString()));
        rowValues.put(REVERT_LIST__Fields.kind.toString(), cursor.getValue(NODES__Fields.kind.toString()));

        table.insertByFieldNamesOr(SqlJetConflictAction.REPLACE, rowValues);
    }

    public void beforeUpdate(ISqlJetCursor cursor, Map<String, Object> newValues) throws SqlJetException {
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
