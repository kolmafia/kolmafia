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
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.db.SvnNodesPristineTrigger;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

/**
 *
 * @author TMate Software Ltd.
 */
public abstract class SVNSqlJetUpdateStatement extends SVNSqlJetSelectStatement {

    public SVNSqlJetUpdateStatement(SVNSqlJetDb sDb, Enum<?> fromTable) throws SVNException {
        this(sDb, fromTable, null);
    }

    public SVNSqlJetUpdateStatement(SVNSqlJetDb sDb, Enum<?> fromTable, Enum<?> indexName) throws SVNException {
        super(sDb, fromTable, indexName);
        if (SVNWCDbSchema.NODES == fromTable) {
            SvnNodesPristineTrigger trigger = new SvnNodesPristineTrigger();
            addTrigger(trigger);
        }
        transactionMode = SqlJetTransactionMode.WRITE;
    }

    public void update(final Map<String, Object> values) throws SqlJetException {
        if (getCursor() == null) {
            throw new UnsupportedOperationException();
        }
        beforeUpdate(getCursor(), values);
        getCursor().updateByFieldNames(values);
    }

    private void beforeUpdate(ISqlJetCursor cursor, Map<String, Object> values) {
        for (ISVNSqlJetTrigger trigger : getTriggers()) {
            try {
                trigger.beforeUpdate(cursor, values);
            } catch (SqlJetException e) {
                //
            }
        }
    }

    public long exec() throws SVNException {
        long n = 0;
        try {
            statementStarted();
            while (next()) {
                Map<String, Object> values = getUpdateValues();
                update(values);
                n++;
            }
            statementCompleted(null);
        } catch (SqlJetException e) {
            statementCompleted(e);
            SVNSqlJetDb.createSqlJetError(e);
        }
        return n;
    }

    public abstract Map<String, Object> getUpdateValues() throws SVNException;

}
