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
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * -- STMT_INSERT_OR_IGNORE_PRISTINE
 * INSERT OR IGNORE INTO pristine (checksum, md5_checksum, size, refcount)
 * VALUES (?1, ?2, ?3, 0)
 */
public class SVNWCDbInsertOrIgnorePristine extends SVNSqlJetInsertStatement {
	private SVNSqlJetSelectStatement select;

    public SVNWCDbInsertOrIgnorePristine(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.PRISTINE, SqlJetConflictAction.IGNORE);
    }
    
    @Override
    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.PRISTINE__Fields.checksum.toString(), getBind(1));
        values.put(SVNWCDbSchema.PRISTINE__Fields.md5_checksum.toString(), getBind(2));
        values.put(SVNWCDbSchema.PRISTINE__Fields.size.toString(), getBind(3));
        values.put(SVNWCDbSchema.PRISTINE__Fields.refcount.toString(), 0);
        return values;
    }

}
