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

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.db.SvnNodesPristineTrigger;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

/**
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNSqlJetDeleteStatement extends SVNSqlJetSelectStatement {

    public SVNSqlJetDeleteStatement(SVNSqlJetDb sDb, Enum<?> fromTable) throws SVNException {
        super(sDb, fromTable);
        transactionMode = SqlJetTransactionMode.WRITE;
        if (SVNWCDbSchema.NODES == fromTable) {
            SvnNodesPristineTrigger trigger = new SvnNodesPristineTrigger();
            addTrigger(trigger);
        }
    }
    
    protected void beforeDelete(ISqlJetCursor cursor) {
        for (ISVNSqlJetTrigger trigger : getTriggers()) {
            try {
                trigger.beforeDelete(cursor);
            } catch (SqlJetException e) {
                //
            }
        }
    }

    public long exec() throws SVNException {
        long n = 0;
        statementStarted();
        while (!eof()) {
            try {
                beforeDelete(getCursor());
                getCursor().delete();
            } catch (SqlJetException e) {
                statementCompleted(e);
                SVNSqlJetDb.createSqlJetError(e);
                return n;
            }
            n++;
        }
        statementCompleted(null);
        return n;
    }

}
