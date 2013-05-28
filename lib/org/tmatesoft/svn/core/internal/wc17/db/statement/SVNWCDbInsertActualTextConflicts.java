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

import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;

/**
 * INSERT INTO actual_node ( wc_id, local_relpath, conflict_old, conflict_new,
 * conflict_working, parent_relpath) VALUES (?1, ?2, ?3, ?4, ?5, ?6);
 *
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertActualTextConflicts extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertActualTextConflicts(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
        transactionMode = SqlJetTransactionMode.WRITE;
    }

    protected Map<String, Object> getInsertValues() {
        assert (getBinds().size() == 6);
        Map<String, Object> v = new HashMap<String, Object>();
        v.put(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id.toString(), getBind(1));
        v.put(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString(), getBind(2));
        v.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString(), getBind(3));
        v.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString(), getBind(4));
        v.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString(), getBind(5));
        v.put(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath.toString(), getBind(6));
        return v;
    }
}
