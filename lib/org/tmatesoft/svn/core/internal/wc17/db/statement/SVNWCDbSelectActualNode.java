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
 * select prop_reject, changelist, conflict_old, conflict_new,
 * conflict_working, tree_conflict_data, properties from actual_node
 * where wc_id = ?1 and local_relpath = ?2;
 * 
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectActualNode extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

    public SVNWCDbSelectActualNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.properties);
    }

}
