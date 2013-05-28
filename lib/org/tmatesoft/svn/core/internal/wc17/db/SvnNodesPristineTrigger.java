package org.tmatesoft.svn.core.internal.wc17.db;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.internal.db.ISVNSqlJetTrigger;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

public class SvnNodesPristineTrigger implements ISVNSqlJetTrigger {
    
    private Map<String, Integer> checksumTriggerValues;

    public void beforeUpdate(ISqlJetCursor cursor, Map<String, Object> values) throws SqlJetException {
        if (values.containsKey(NODES__Fields.checksum.toString())) {
            String newChecksum = (String) values.get(NODES__Fields.checksum.toString());
            String oldChecksum = (String) cursor.getString(NODES__Fields.checksum.toString());
            
            changeRefCount(oldChecksum,-1);
            changeRefCount(newChecksum, 1);
        }
    }

    public void beforeDelete(ISqlJetCursor cursor) throws SqlJetException {
        String checksumValue = cursor.getString(NODES__Fields.checksum.toString());
        changeRefCount(checksumValue, -1);
    }

    public void beforeInsert(SqlJetConflictAction conflictAction, ISqlJetTable table, Map<String, Object> newValues) throws SqlJetException {
        if (conflictAction == SqlJetConflictAction.REPLACE) {
            Object o1 = newValues.get(NODES__Fields.wc_id.toString());
            Object o2 = newValues.get(NODES__Fields.local_relpath.toString());
            Object o3 = newValues.get(NODES__Fields.op_depth.toString());
            ISqlJetCursor cursor = table.lookup(null, new Object[] {o1, o2, o3});
            try { 
                if (!cursor.eof()) {
                    changeRefCount(cursor.getString(NODES__Fields.checksum.toString()), -1);
                }
            } finally {
                cursor.close();
            }
        }
        String newChecksumValue = (String) newValues.get(NODES__Fields.checksum.toString());
        changeRefCount(newChecksumValue, 1);
    }

    private void changeRefCount(String checksum, int delta) {
        if (checksum != null) {
            if (!getTriggerValues().containsKey(checksum)) {
                getTriggerValues().put(checksum, delta);
            } else {
                getTriggerValues().put(checksum, getTriggerValues().get(checksum) + delta);
            }
        }
    }
    
    private Map<String, Integer> getTriggerValues() {
        if (checksumTriggerValues == null) {
            checksumTriggerValues = new HashMap<String, Integer>();
        }
        return checksumTriggerValues;
    }

    public void statementStarted(SqlJetDb db) throws SqlJetException {
    }

    public void statementCompleted(SqlJetDb db, SqlJetException error) throws SqlJetException {
        try {
            if (error == null && !getTriggerValues().isEmpty()) {
                Map<String, Object> values = new HashMap<String, Object>();
                ISqlJetTable pristineTable = db.getTable(SVNWCDbSchema.PRISTINE.toString());
                for (String checksum : getTriggerValues().keySet()) {
                    long delta = getTriggerValues().get(checksum); 
                    if (delta == 0) {
                        continue;
                    }
                    ISqlJetCursor cursor = pristineTable.lookup(null, checksum);
                    if (cursor != null && !cursor.eof()) {                        
                        long refcount = cursor.getInteger(SVNWCDbSchema.PRISTINE__Fields.refcount.toString());
                        refcount += delta;
                        if (refcount < 0) {
                            refcount = 0;
                        }
                        values.put(SVNWCDbSchema.PRISTINE__Fields.refcount.toString(), refcount);
                        cursor.updateByFieldNames(values);
                    }
                    cursor.close();
                }
            }
        } finally {
            checksumTriggerValues = null;
        }
    }

}
