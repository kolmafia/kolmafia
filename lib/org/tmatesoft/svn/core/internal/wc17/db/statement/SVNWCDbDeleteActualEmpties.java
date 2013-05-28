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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;

/**
 * DELETE FROM actual_node
 * WHERE wc_id = ?1
 * AND properties IS NULL
 * AND conflict_old IS NULL
 * AND conflict_new IS NULL
 * AND prop_reject IS NULL
 * AND changelist IS NULL
 * AND text_mod IS NULL
 * AND tree_conflict_data IS NULL
 * AND older_checksum IS NULL
 * AND right_checksum IS NULL
 * AND left_checksum IS NULL
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteActualEmpties extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteActualEmpties(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    protected boolean isFilterPassed() throws SVNException {
    	if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.properties)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.text_mod)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.older_checksum)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.right_checksum)) {
            return false;
        }
        if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.left_checksum)) {
            return false;
        }
        
        return true;
    }
    
    protected Object[] getWhere() throws SVNException {
        bindLong(1, (Long)getBind(1));
        return super.getWhere();
    }

}
