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
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.4
 * @author TMate Software Ltd.
 */
public abstract class SVNSqlJetSelectFieldsStatement<E extends Enum<E>> extends SVNSqlJetSelectStatement {

    protected final List<E> fields;

    public SVNSqlJetSelectFieldsStatement(SVNSqlJetDb sDb, Enum<?> fromTable) throws SVNException {
        this(sDb, fromTable.toString());
    }

    public SVNSqlJetSelectFieldsStatement(SVNSqlJetDb sDb, String fromTable) throws SVNException {
        super(sDb, fromTable);
        fields = new ArrayList<E>();
        defineFields();
    }

    public SVNSqlJetSelectFieldsStatement(SVNSqlJetDb sDb, Enum<?> fromTable, Enum<?> indexName) throws SVNException {
        super(sDb, fromTable, indexName);
        fields = new ArrayList<E>();
        defineFields();
    }


    protected abstract void defineFields();

    public long getColumnLong(int f) throws SVNException {
        return getColumnLong(getFieldName(f));
    }

    public String getColumnString(int f) throws SVNException {
        return getColumnString(getFieldName(f));
    }

    public boolean isColumnNull(int f) throws SVNException {
        return isColumnNull(getFieldName(f));
    }

    public byte[] getColumnBlob(int f) throws SVNException {
        return getColumnBlob(getFieldName(f));
    }

    protected void checkField(int f) throws SVNException {
        SVNErrorManager.assertionFailure(fields.size() > 0, "fields not defined", SVNLogType.WC);
        SVNErrorManager.assertionFailure(f >= 0 && f < fields.size(), String.format("%d is not valid field index", f), SVNLogType.WC);
        SVNErrorManager.assertionFailure(fields.get(f) != null, String.format("field #%d is not defined", f), SVNLogType.WC);
    }

    protected String getFieldName(int f) throws SVNException {
        checkField(f);
        return fields.get(f).toString();
    }

}
