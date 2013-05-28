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
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/**
 * INSERT INTO wc_lock (wc_id, local_dir_relpath, locked_levels) VALUES (?1, ?2,
 * ?3);
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertWCLock extends SVNSqlJetStatement {

    private ISqlJetTable table;

    public SVNWCDbInsertWCLock(SVNSqlJetDb sDb) throws SqlJetException {
        super(sDb);
        table = sDb.getDb().getTable(SVNWCDbSchema.WC_LOCK.toString());
    }

    public long exec() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.WC_LOCK__Fields.wc_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.WC_LOCK__Fields.local_dir_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.WC_LOCK__Fields.locked_levels.toString(), getBind(3));
        try {
            return table.insertByFieldNames(values);
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
            return 0;
        }
    }

}
