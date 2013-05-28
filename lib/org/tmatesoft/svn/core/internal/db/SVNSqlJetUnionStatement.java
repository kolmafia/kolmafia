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

import java.util.List;

import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc2.SvnChecksum;

/**
 * @author TMate Software Ltd.
 */
public class SVNSqlJetUnionStatement extends SVNSqlJetStatement {

    private SVNSqlJetStatement[] statements;
    private int current = 0;

    public SVNSqlJetUnionStatement(SVNSqlJetDb sDb, SVNSqlJetStatement... statements) {
        super(sDb);
        this.statements = statements;
    }

    public boolean next() throws SVNException {
        if (statements == null) {
            return false;
        }
        boolean next = false;
        while (!next && current < statements.length) {
            SVNSqlJetStatement stmt = statements[current];
            if (stmt != null) {
                next = stmt.next();
            }
            if (!next) {
                current++;
            }
        }
        return next;
    }

    protected ISqlJetCursor getCursor() {
        if (statements == null) {
            return null;
        }
        if (current < statements.length) {
            return statements[current].getCursor();
        }
        return null;
    }

    public void reset() throws SVNException {
        current = 0;
        if (statements == null) {
            return;
        }
        for (SVNSqlJetStatement stmt : statements) {
            stmt.reset();
        }
    }

    private void updateBinds() {
        if (statements == null) {
            return;
        }
        for (SVNSqlJetStatement stmt : statements) {
            List<Object> stmtBinds = stmt.getBinds();
            stmtBinds.clear();
            stmtBinds.addAll(getBinds());
        }
    }

    public void bindf(String format, Object... data) throws SVNException {
        super.bindf(format, data);
        updateBinds();
    }

    public void bindLong(int i, long v) {
        super.bindLong(i, v);
        updateBinds();
    }

    public void bindString(int i, String string) {
        super.bindString(i, string);
        updateBinds();
    }

    public void bindNull(int i) {
        super.bindNull(i);
        updateBinds();
    }

    public void bindBlob(int i, byte[] serialized) {
        super.bindBlob(i, serialized);
        updateBinds();
    }

    public void bindChecksum(int i, SvnChecksum checksum) {
        super.bindChecksum(i, checksum);
        updateBinds();
    }

    public void bindProperties(int i, SVNProperties props) throws SVNException {
        super.bindProperties(i, props);
        updateBinds();
    }

}
