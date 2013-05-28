/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * -- STMT_INSERT_ACTUAL_EMPTIES
 * INSERT OR IGNORE INTO actual_node (
 *    wc_id, local_relpath, parent_relpath, properties,
 *    conflict_old, conflict_new, conflict_working,
 *    prop_reject, changelist, text_mod, tree_conflict_data )
 * SELECT wc_id, local_relpath, parent_relpath, NULL, NULL, NULL, NULL,
 *      NULL, NULL, NULL, NULL
 * FROM targets_list
 
 */
public class SVNWCDbInsertActualEmpties extends SVNSqlJetInsertStatement {
	private SVNSqlJetSelectStatement select;

    public SVNWCDbInsertActualEmpties(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
        select = new SVNSqlJetSelectStatement(sDb.getTemporaryDb(), SVNWCDbSchema.TARGETS_LIST) {
        };
    }
    
    public long exec() throws SVNException {
        try {
            int n = 0;
            while (select.next()) {
                try {
                    table.insertByFieldNamesOr(SqlJetConflictAction.IGNORE, getInsertValues());
                    n++;
                } catch (SqlJetException e) {
                    SVNSqlJetDb.createSqlJetError(e);
                    return -1;
                }
            }
            return n;
        } finally {
            select.reset();
        }
    }


    protected Map<String, Object> getInsertValues() throws SVNException {
    	Map<String,Object> selectedRow = select.getRowValues();
    	Map<String, Object> insertValues = new HashMap<String, Object>();
    	insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id.toString(), selectedRow.get(SVNWCDbSchema.TARGETS_LIST__Fields.wc_id.toString()));
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString(), selectedRow.get(SVNWCDbSchema.TARGETS_LIST__Fields.local_relpath.toString()));
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath.toString(), selectedRow.get(SVNWCDbSchema.TARGETS_LIST__Fields.parent_relpath.toString()));
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.properties.toString(), null);
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString(), null);
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString(), null);
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString(), null);
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.toString(), null);
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist.toString(), null);
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.text_mod.toString(), null);
        insertValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.toString(), null);
        
        return insertValues;
    }

}
