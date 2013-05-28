package org.tmatesoft.svn.core.internal.db;

import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public interface ISVNSqlJetTrigger {
    
    public void beforeUpdate(ISqlJetCursor cursor, Map<String, Object> newValues) throws SqlJetException;

    public void beforeDelete(ISqlJetCursor cursor) throws SqlJetException;
    
    public void beforeInsert(SqlJetConflictAction conflictAction, ISqlJetTable table, Map<String, Object> newValues) throws SqlJetException;
    
    public void statementStarted(SqlJetDb db) throws SqlJetException;
    
    public void statementCompleted(SqlJetDb db, SqlJetException error) throws SqlJetException;
}
