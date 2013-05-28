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
package org.tmatesoft.svn.core.internal.db;

import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.db.SvnNodesPristineTrigger;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

/**
 * @version 1.4
 * @author TMate Software Ltd.
 */
public abstract class SVNSqlJetInsertStatement extends SVNSqlJetTableStatement {

    protected SqlJetConflictAction conflictAction = null;

    public SVNSqlJetInsertStatement(SVNSqlJetDb sDb, Enum<?> tableName) throws SVNException {
        super(sDb, tableName);
        transactionMode = SqlJetTransactionMode.WRITE;
        if (SVNWCDbSchema.NODES == tableName) {
            SvnNodesPristineTrigger trigger = new SvnNodesPristineTrigger();
            addTrigger(trigger);
        }
    }


    public SVNSqlJetInsertStatement(SVNSqlJetDb sDb, Enum<?> tableName, SqlJetConflictAction conflictAction) throws SVNException {
        this(sDb, tableName);
        this.conflictAction = conflictAction;
    }

    public long exec() throws SVNException {
        Map<String, Object> insertValues = getInsertValues();
        if (insertValues == null) {
            return 0;
        }
        statementStarted();
        try {
            beforeInsert(conflictAction, table, insertValues);
            long n = table.insertByFieldNamesOr(conflictAction, insertValues);
            statementCompleted(null);
            return n;
        } catch (SqlJetException e) {
            statementCompleted(e);
            SVNSqlJetDb.createSqlJetError(e);
            return -1;
        }
    }

    private void beforeInsert(SqlJetConflictAction conflictAction, ISqlJetTable table, Map<String, Object> insertValues) {
        for (ISVNSqlJetTrigger trigger : getTriggers()) {
            try {
                trigger.beforeInsert(conflictAction, table, insertValues);
            } catch (SqlJetException e) {
                //
            }
        }
    }


    protected abstract Map<String, Object> getInsertValues() throws SVNException;

}
