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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/**
 * SELECT op_depth, nodes.repos_id, nodes.repos_path, presence, kind, revision,
 *  checksum, translated_size, changed_revision, changed_date, changed_author,
 * depth, symlink_target, last_mod_time, properties, lock_token, lock_owner,
 * lock_comment, lock_date
 * FROM nodes
 * LEFT OUTER JOIN lock ON nodes.repos_id = lock.repos_id
 * AND nodes.repos_path = lock.repos_relpath
 * WHERE wc_id = ?1 AND local_relpath = ?2
 * ORDER BY op_depth DESC
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectNodeInfoWithLock extends SVNWCDbSelectNodeInfo {

    private static class LockStatement extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.LOCK__Fields> {

        public LockStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.LOCK);
        }

        protected void defineFields() {
            fields.add(SVNWCDbSchema.LOCK__Fields.lock_token);
            fields.add(SVNWCDbSchema.LOCK__Fields.lock_owner);
            fields.add(SVNWCDbSchema.LOCK__Fields.lock_comment);
            fields.add(SVNWCDbSchema.LOCK__Fields.lock_date);
        }
    }

    private LockStatement lockStatement;

    public SVNWCDbSelectNodeInfoWithLock(SVNSqlJetDb sDb) throws SVNException {
        super(sDb);
        lockStatement = new LockStatement(sDb);
    }

    public boolean next() throws SVNException {
        lockStatement.reset();
        final boolean next = super.next();
        if (next) {
            lockStatement.bindLong(1, getColumnLong(SVNWCDbSchema.NODES__Fields.repos_id.toString()));
            lockStatement.bindString(2, getColumnString(SVNWCDbSchema.NODES__Fields.repos_path.toString()));
            lockStatement.next();
        }
        return next;
    }

    public void reset() throws SVNException {
        try {
            lockStatement.reset();
        } finally {
            super.reset();
        }
    }

    public SVNSqlJetStatement getJoinedStatement(String joinedTable) throws SVNException {
        if (SVNWCDbSchema.LOCK.toString().equalsIgnoreCase(joinedTable)) {
            return lockStatement;
        }
        return super.getJoinedStatement(joinedTable);
    }
}
