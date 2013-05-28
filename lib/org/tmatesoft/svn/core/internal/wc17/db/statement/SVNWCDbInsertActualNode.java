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

import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;

/**
 * -- STMT_INSERT_ACTUAL_NODE
 * INSERT OR REPLACE INTO actual_node (
 * wc_id, local_relpath, parent_relpath, properties, conflict_old,
 * conflict_new,
 * conflict_working, prop_reject, changelist, text_mod,
 * tree_conflict_data)
 * VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, NULL, ?10)
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertActualNode extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertActualNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE, SqlJetConflictAction.REPLACE);
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath.toString(), getBind(3));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.properties.toString(), getBind(4));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString(), getBind(5));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString(), getBind(6));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString(), getBind(7));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.toString(), getBind(8));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist.toString(), getBind(9));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.text_mod.toString(), null);
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.toString(), getBind(10));
        return values;
    }

}
