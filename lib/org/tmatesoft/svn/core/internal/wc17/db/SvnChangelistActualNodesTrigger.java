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
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.CHANGELIST_LIST__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.REVERT_LIST__Fields;

public class SvnChangelistActualNodesTrigger implements ISVNSqlJetTrigger {
    
    private SVNSqlJetDb db;

    public SvnChangelistActualNodesTrigger(SVNSqlJetDb db) {
        this.db = db;
    }
    
    public void beforeDelete(ISqlJetCursor cursor) throws SqlJetException {
    }
    
    /*
    * 	DROP TRIGGER IF EXISTS   trigger_changelist_list_actual_cl_clear;
	*	CREATE TEMPORARY TRIGGER trigger_changelist_list_actual_cl_clear
	*	BEFORE UPDATE ON actual_node
	*	WHEN OLD.changelist IS NOT NULL AND
	*	        (OLD.changelist != NEW.changelist OR NEW.changelist IS NULL)
	*	BEGIN
	*	    
	*	    INSERT INTO changelist_list(wc_id, local_relpath, notify, changelist)
	*	    VALUES (OLD.wc_id, OLD.local_relpath, 27, OLD.changelist);
	*	END;
	*	
	*	DROP TRIGGER IF EXISTS   trigger_changelist_list_actual_cl_set;
	*	CREATE TEMPORARY TRIGGER trigger_changelist_list_actual_cl_set
	*	BEFORE UPDATE ON actual_node
	*	WHEN NEW.CHANGELIST IS NOT NULL AND
	*	        (OLD.changelist != NEW.changelist OR OLD.changelist IS NULL)
	*	BEGIN
	*	    
	*	    INSERT INTO changelist_list(wc_id, local_relpath, notify, changelist)
	*	    VALUES (NEW.wc_id, NEW.local_relpath, 26, NEW.changelist);
	*	END
	*/

    public void beforeUpdate(ISqlJetCursor cursor, Map<String, Object> newValues) throws SqlJetException {
    	
        ISqlJetTable table = db.getDb().getTemporaryDatabase().getTable(SVNWCDbSchema.CHANGELIST_LIST.toString());
        
        if (!cursor.isNull(ACTUAL_NODE__Fields.changelist.toString()) && 
        	(!cursor.getValue(ACTUAL_NODE__Fields.changelist.toString()).equals(newValues.get(ACTUAL_NODE__Fields.changelist.toString()) )
        	|| (newValues.get(ACTUAL_NODE__Fields.changelist.toString()) == null)))
        {
        	Map<String, Object> rowValues = new HashMap<String, Object>();
	        rowValues.put(CHANGELIST_LIST__Fields.wc_id.toString(), cursor.getValue(ACTUAL_NODE__Fields.wc_id.toString()));
	        rowValues.put(CHANGELIST_LIST__Fields.local_relpath.toString(), cursor.getValue(ACTUAL_NODE__Fields.local_relpath.toString()));
	        rowValues.put(CHANGELIST_LIST__Fields.notify.toString(), 27);
	        rowValues.put(CHANGELIST_LIST__Fields.changelist.toString(), cursor.getValue(ACTUAL_NODE__Fields.changelist.toString()));
	        
	        table.insertByFieldNames(rowValues);
        }
        
        if ( (newValues.get(ACTUAL_NODE__Fields.changelist.toString()) != null) && 
            	(cursor.isNull(ACTUAL_NODE__Fields.changelist.toString())
            			||	!cursor.getValue(ACTUAL_NODE__Fields.changelist.toString()).equals(newValues.get(ACTUAL_NODE__Fields.changelist.toString()) )
            	))
            {
        	
        		Map<String, Object> rowValues = new HashMap<String, Object>();
    	        rowValues.put(CHANGELIST_LIST__Fields.wc_id.toString(), newValues.get(ACTUAL_NODE__Fields.changelist.toString()));
    	        rowValues.put(CHANGELIST_LIST__Fields.local_relpath.toString(), newValues.get(ACTUAL_NODE__Fields.local_relpath.toString()));
    	        rowValues.put(CHANGELIST_LIST__Fields.notify.toString(), 26);
    	        rowValues.put(CHANGELIST_LIST__Fields.changelist.toString(), newValues.get(ACTUAL_NODE__Fields.changelist.toString()));
    	        
    	        table.insertByFieldNames(rowValues);
            }
    }
    
    /**
	*	DROP TRIGGER IF EXISTS   trigger_changelist_list_actual_cl_insert;
	*	CREATE TEMPORARY TRIGGER trigger_changelist_list_actual_cl_insert
	*	BEFORE INSERT ON actual_node
	*	BEGIN
    *		INSERT INTO changelist_list(wc_id, local_relpath, notify, changelist)
    *		VALUES (NEW.wc_id, NEW.local_relpath, 26, NEW.changelist);
	*	END;     
	*/

    public void beforeInsert(SqlJetConflictAction conflictAction, ISqlJetTable table, Map<String, Object> newValues) throws SqlJetException {
    	ISqlJetTable clltable = db.getDb().getTemporaryDatabase().getTable(SVNWCDbSchema.CHANGELIST_LIST.toString());
    	
        Map<String, Object> rowValues = new HashMap<String, Object>();
	    rowValues.put(CHANGELIST_LIST__Fields.wc_id.toString(), newValues.get(ACTUAL_NODE__Fields.wc_id.toString()));
	    rowValues.put(CHANGELIST_LIST__Fields.local_relpath.toString(), newValues.get(ACTUAL_NODE__Fields.local_relpath.toString()));
	    rowValues.put(CHANGELIST_LIST__Fields.notify.toString(), 26);
	    rowValues.put(CHANGELIST_LIST__Fields.changelist.toString(), newValues.get(ACTUAL_NODE__Fields.changelist.toString()));
	        
	    clltable.insertByFieldNames(rowValues);
        
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
