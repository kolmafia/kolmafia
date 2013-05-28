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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNException;

/**
 * @version 1.4
 * @author TMate Software Ltd.
 */
public abstract class SVNSqlJetTableStatement extends SVNSqlJetStatement {

    protected ISqlJetTable table;
    protected String tableName;
    private Collection<ISVNSqlJetTrigger> triggers;

    public SVNSqlJetTableStatement(SVNSqlJetDb sDb, Enum<?> tableName) throws SVNException {
        this(sDb, tableName.toString());
    }
    
    public String getTableName() {
        return tableName;
    }

    public SVNSqlJetTableStatement(SVNSqlJetDb sDb, String tableName) throws SVNException {
        super(sDb);
        this.tableName = tableName;
        try {
            table = sDb.getDb().getTable(tableName);
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
    }
    
    public void addTrigger(ISVNSqlJetTrigger trigger) {
        if (trigger != null) {
            if (this.triggers == null) {
                this.triggers = new ArrayList<ISVNSqlJetTrigger>();
            }
            this.triggers.add(trigger);
        }
    }
    
    protected Collection<ISVNSqlJetTrigger> getTriggers() {
        if (this.triggers == null) {
            return Collections.emptyList();
        }
        return this.triggers;
    }

    public ISqlJetTable getTable() {
        return table;
    }

    protected void statementStarted() {
        for (ISVNSqlJetTrigger trigger : getTriggers()) {
            try {
                trigger.statementStarted(sDb.getDb());
            } catch (SqlJetException e) {
                //
            }
        }
    }
    
    protected void statementCompleted(SqlJetException error) {
        for (ISVNSqlJetTrigger trigger : getTriggers()) {
            try {
                trigger.statementCompleted(sDb.getDb(), error);
            } catch (SqlJetException e) {
                //
            }
        }
    }

    @Override
    public void reset() throws SVNException {
        super.reset();
        if (this.triggers != null) {
            this.triggers.clear();
        }
    }
}
