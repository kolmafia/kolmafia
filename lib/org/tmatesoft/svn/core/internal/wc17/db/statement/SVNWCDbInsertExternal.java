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

import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;

/**
 * -- STMT_INSERT_EXTERNAL
 * INSERT OR REPLACE INTO externals (
 *   wc_id, local_relpath, parent_relpath, presence, kind, def_local_relpath,
 *   repos_id, def_repos_relpath, def_operational_revision, def_revision)
 * VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertExternal extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertExternal(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.EXTERNALS, SqlJetConflictAction.REPLACE);
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.EXTERNALS__Fields.wc_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.local_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.parent_relpath.toString(), getBind(3));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.presence.toString(), getBind(4));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.kind.toString(), getBind(5));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.def_local_relpath.toString(), getBind(6));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.repos_id.toString(), getBind(7));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.def_repos_relpath.toString(), getBind(8));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.def_operational_revision.toString(), getBind(9));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.def_revision.toString(), getBind(10));
        return values;
    }

}
