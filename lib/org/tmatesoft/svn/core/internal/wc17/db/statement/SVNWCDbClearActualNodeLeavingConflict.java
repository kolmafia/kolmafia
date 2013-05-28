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
 * UPDATE actual_node SET properties = NULL, text_mod = NULL, changelist = NULL,
 * tree_conflict_data = NULL, conflict_old = NULL, conflict_new = NULL,
 * conflict_working = NULL, prop_reject = NULL, older_checksum = NULL,
 * left_checksum = NULL, right_checksum = NULL WHERE wc_id = ?1 and
 * local_relpath = ?2;
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbClearActualNodeLeavingConflict extends SVNSqlJetUpdateStatement {

    public SVNWCDbClearActualNodeLeavingConflict(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    protected Object[] getWhere() throws SVNException {
        return new Object[] {
                getBind(1), getBind(2)
        };
    }

    protected boolean isFilterPassed() throws SVNException {
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath)) {
            return false;
        }
        return getColumn(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id).equals(getBind(1)) && getColumn(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath).equals(getBind(2));
    }

    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> rowValues = getRowValues();
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.properties.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.text_mod.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist.toString(), null);
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

}
