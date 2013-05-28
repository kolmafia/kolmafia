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
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.TARGETS_LIST__Indices;

/**
 * STMT_MARK_SKIPPED_CHANGELIST_DIRS
 * INSERT INTO changelist_list (wc_id, local_relpath, notify, changelist)
 * SELECT wc_id, local_relpath, 7, ?1 
 * FROM targets_list 
 * WHERE kind = 'dir'
 
 */
public class SVNWCDbMarkSkippedChangelistDirs extends SVNSqlJetInsertStatement {
	private SVNSqlJetSelectStatement select;

    public SVNWCDbMarkSkippedChangelistDirs(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.CHANGELIST_LIST);
        
        this.select = new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.TARGETS_LIST__Fields>(sDb, SVNWCDbSchema.TARGETS_LIST) {
        	
        	{
        		setIndexName(TARGETS_LIST__Indices.targets_list_kind.toString());
            }
        	
            @Override
            protected Object[] getWhere() throws SVNException {
        		bindString(1, "dir");
            	return super.getWhere();
            }
            
            protected void defineFields() {
                fields.add(SVNWCDbSchema.TARGETS_LIST__Fields.wc_id);
                fields.add(SVNWCDbSchema.TARGETS_LIST__Fields.local_relpath);
            }
        };
    }
    
    public long exec() throws SVNException {
        try {
            int n = 0;
            while (select.next()) {
                try {
                    table.insertByFieldNames(getInsertValues());
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
    	insertValues.put(SVNWCDbSchema.CHANGELIST_LIST__Fields.wc_id.toString(), selectedRow.get(SVNWCDbSchema.TARGETS_LIST__Fields.wc_id.toString()));
        insertValues.put(SVNWCDbSchema.CHANGELIST_LIST__Fields.local_relpath.toString(), selectedRow.get(SVNWCDbSchema.TARGETS_LIST__Fields.local_relpath.toString()));
        insertValues.put(SVNWCDbSchema.CHANGELIST_LIST__Fields.notify.toString(), 7);
        insertValues.put(SVNWCDbSchema.CHANGELIST_LIST__Fields.changelist.toString(), getBind(1));
        return insertValues;
    }

}
