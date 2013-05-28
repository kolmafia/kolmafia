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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * INSERT INTO nodes ( wc_id, local_relpath, op_depth, parent_relpath, repos_id,
 * repos_path, revision, presence, depth, kind, changed_revision, changed_date,
 * changed_author, checksum, properties, translated_size, last_mod_time,
 * symlink_target ) SELECT wc_id, local_relpath, ?3 AS op_depth, parent_relpath,
 * repos_id, repos_path, revision, 'normal', depth, kind, changed_revision,
 * changed_date, changed_author, checksum, properties, translated_size,
 * last_mod_time, symlink_target FROM nodes WHERE wc_id = ?1 AND local_relpath =
 * ?2 AND op_depth = 0;
 *
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertWorkingNodeNormalFromBase extends SVNSqlJetInsertStatement {

    private SVNSqlJetSelectStatement select;

    public SVNWCDbInsertWorkingNodeNormalFromBase(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        select = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES);
    }

    public long exec() throws SVNException {
        try {
            select.bindf("isi", getBind(1), getBind(2), 0);
            long n = 0;
            while (select.next()) {
                super.exec();
                n++;
            }
            return n;
        } finally {
            select.reset();
        }
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> rowValues = select.getRowValues();
        rowValues.put(SVNWCDbSchema.NODES__Fields.op_depth.toString(), getBind(3));
        rowValues.put(SVNWCDbSchema.NODES__Fields.presence.toString(), "normal");
        return rowValues;
    }

}
