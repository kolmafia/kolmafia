package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.ISVNSqlJetTrigger;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbConflicts;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

import java.util.HashMap;
import java.util.Map;

public class SvnRevertActualNodesTrigger17 implements ISVNSqlJetTrigger {

    private SVNSqlJetDb db;

    public SvnRevertActualNodesTrigger17(SVNSqlJetDb db) {
        this.db = db;
    }

    public void beforeDelete(ISqlJetCursor cursor) throws SqlJetException {
        ISqlJetTable table = db.getDb().getTemporaryDatabase().getTable(SVNWCDbSchema.REVERT_LIST.toString());

        Map<String, Object> rowValues = new HashMap<String, Object>();
        rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.local_relpath.toString(), cursor.getValue(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString()));
        rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.actual.toString(), 1);
        rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.conflict_data.toString(), getConflictData(cursor));

        if (!cursor.isNull(SVNWCDbSchema.ACTUAL_NODE__Fields.properties.toString())
                || !cursor.isNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.toString())) {
            rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.notify.toString(), 1);
        } else if (!exists(db.getDb(), cursor.getInteger(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id.toString()), cursor.getString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString()))) {
            rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.notify.toString(), 1);
        } else {
            rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.notify.toString(), null);
        }
        table.insertByFieldNamesOr(SqlJetConflictAction.REPLACE, rowValues);
    }

    public void beforeUpdate(ISqlJetCursor cursor, Map<String, Object> newValues) throws SqlJetException {
        ISqlJetTable table = db.getDb().getTemporaryDatabase().getTable(SVNWCDbSchema.REVERT_LIST.toString());

        Map<String, Object> rowValues = new HashMap<String, Object>();
        rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.local_relpath.toString(), cursor.getValue(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString()));
        rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.actual.toString(), 1);
        rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.conflict_data.toString(), getConflictData(cursor));

        if (!cursor.isNull(SVNWCDbSchema.ACTUAL_NODE__Fields.properties.toString())
                || !cursor.isNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.toString())) {
            rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.notify.toString(), 1);
        } else if (!exists(db.getDb().getTemporaryDatabase(), cursor.getInteger(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id.toString()), cursor.getString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString()))) {
            rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.notify.toString(), 1);
        } else {
            rowValues.put(SVNWCDbSchema.REVERT_LIST__Fields.notify.toString(), null);
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

    private boolean exists(SqlJetDb db, long wcId, String localRelPath) throws SqlJetException {
        ISqlJetTable table = db.getTable(SVNWCDbSchema.NODES.name());
        ISqlJetCursor cursor = table.lookup(null, wcId, localRelPath);
        try {
            return !cursor.eof();
        } finally {
            cursor.close();
        }
    }

    private byte[] getConflictData(ISqlJetCursor cursor) throws SqlJetException {
        try {
            SVNSkel skel = getConflictSkel(cursor);
            return skel == null ? null : skel.unparse();
        } catch (SVNException e) {
            throw new SqlJetException(e);
        }
    }

    private SVNSkel getConflictSkel(ISqlJetCursor cursor) throws SqlJetException, SVNException {
        String conflictOldRelPath = cursor.getString(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.name());
        String conflictNewRelPath = cursor.getString(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.name());
        String conflictWorkingRelPath = cursor.getString(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.name());
        String propRejectRelPath = cursor.getString(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.name());
        byte[] treeConflictData = cursor.getBlobAsArray(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.name());

        return SvnWcDbConflicts.convertToConflictSkel(conflictOldRelPath, conflictWorkingRelPath, conflictNewRelPath, propRejectRelPath, treeConflictData);
    }
}
