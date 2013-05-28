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
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/**
 * INSERT INTO lock (repos_id, repos_relpath, lock_token, lock_owner, lock_comment, lock_date) VALUES (?1, ?2,
 * ?3, ?4, ?5, ?6);
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertLock extends SVNSqlJetStatement {

    private ISqlJetTable table;

    public SVNWCDbInsertLock(SVNSqlJetDb sDb) throws SqlJetException {
        super(sDb);
        table = sDb.getDb().getTable(SVNWCDbSchema.LOCK.toString());
    }

    public long exec() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.LOCK__Fields.repos_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.LOCK__Fields.repos_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.LOCK__Fields.lock_token.toString(), getBind(3));
        values.put(SVNWCDbSchema.LOCK__Fields.lock_owner.toString(), getBind(4));
        values.put(SVNWCDbSchema.LOCK__Fields.lock_comment.toString(), getBind(5));
        values.put(SVNWCDbSchema.LOCK__Fields.lock_date.toString(), getBind(6));
        try {
            return table.insertByFieldNamesOr(SqlJetConflictAction.REPLACE, values);
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
            return 0;
        }
    }

}
