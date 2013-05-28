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
 * SELECT tree_conflict_data FROM actual_node WHERE wc_id = ?1 AND local_relpath = ?2
 * AND tree_conflict_data IS NOT NULL;
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectActualTreeConflict extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

    public SVNWCDbSelectActualTreeConflict(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    protected Object[] getWhere() throws SVNException {
        return new Object[] {
                getBind(1), getBind(2)
        };
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data);
    }

    protected boolean isFilterPassed() throws SVNException {
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath)) {
            return false;
        }
        return getColumn(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id).equals(getBind(1)) && getColumn(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath).equals(getBind(2));
    }

}
