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

import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

/**
 * UPDATE actual_node SET conflict_old = NULL, conflict_new = NULL,
 * conflict_working = NULL 
 * WHERE wc_id = ?1
 *  AND (?2 = ''
 *      OR local_relpath = ?2
 *      OR (local_relpath > ?2 || '/' AND local_relpath < ?2 || '0'))
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbClearActualNodeLeavingChangelistRecursive extends SVNSqlJetUpdateStatement {

    public SVNWCDbClearActualNodeLeavingChangelistRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return isRecursive() ? new Object[] {getBind(1)} : new Object[] {getBind(1), getBind(2)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        if (!isRecursive()) {
            return true;
        }
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath)) {
            return false;
        }
        String selectPath = getBind(2).toString();
        if ("".equals(selectPath)) {
            return true;
        }
        String rowPath = getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
        return selectPath.equals(rowPath) || rowPath.startsWith(selectPath + '/');
    }
    
    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> rowValues = getRowValues();
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.properties.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.text_mod.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.older_checksum.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.left_checksum.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.right_checksum.toString(), null);
        return rowValues;
    }
    
    protected boolean isRecursive() {
        return true;
    }

}
