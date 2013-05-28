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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/**
 * SELECT local_relpath FROM actual_node WHERE wc_id = ?1 AND parent_relpath =
 * ?2 AND NOT((prop_reject IS NULL) AND (conflict_old IS NULL) AND (conflict_new
 * IS NULL) AND (conflict_working IS NULL) AND (tree_conflict_data IS NULL))
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectActualConflictVictims extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

    public SVNWCDbSelectActualConflictVictims(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    protected String getIndexName() {
        return SVNWCDbSchema.ACTUAL_NODE__Indices.I_ACTUAL_PARENT.toString();
    }

    protected boolean isFilterPassed() throws SVNException {
        return !(isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.toString()) && isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString())
                && isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString()) && isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString()) && isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data
                .toString()));
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
    }
}
