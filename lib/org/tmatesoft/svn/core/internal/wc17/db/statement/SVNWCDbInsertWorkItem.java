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
 * INSERT INTO work_queue (work) VALUES (?1);
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertWorkItem extends SVNSqlJetStatement {

    private ISqlJetTable table;

    public SVNWCDbInsertWorkItem(SVNSqlJetDb sDb) throws SVNException {
        super(sDb);
        try {
            table = sDb.getDb().getTable(SVNWCDbSchema.WORK_QUEUE.toString());
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
    }

    public long exec() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.WORK_QUEUE__Fields.work.toString(), getBind(1));
        try {
            return table.insertByFieldNames(values);
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
            return 0;
        }
    }

}
