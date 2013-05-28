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

/**
 * -- STMT_INSERT_EXTERNAL_UPGRADE
 * INSERT OR REPLACE INTO externals (
 *    wc_id, local_relpath, parent_relpath, presence, kind, def_local_relpath,
 *    repos_id, def_repos_relpath, def_operational_revision, def_revision)
 * VALUES (?1, ?2, ?3, ?4,
 *    CASE WHEN (SELECT file_external FROM nodes
 *       WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = 0)
 *       IS NOT NULL THEN 'file' ELSE 'unknown' END,
 *    ?5, ?6, ?7, ?8, ?9)

 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertExternalUpgrade extends SVNSqlJetInsertStatement {
	
	private SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> select;

    public SVNWCDbInsertExternalUpgrade(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.EXTERNALS, SqlJetConflictAction.REPLACE);
        select = new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields>(sDb, SVNWCDbSchema.NODES) {
            protected Object[] getWhere() throws SVNException {
                return new Object[] { getBind(1), getBind(2), 0 };
            }
            protected void defineFields() {
                fields.add(SVNWCDbSchema.NODES__Fields.file_external);
            }
        };
    }
    
    public long exec() throws SVNException {
        try {
            select.bindf("is", getBind(1), getBind(2));
            select.next();
            try {
                table.insertByFieldNamesOr(SqlJetConflictAction.REPLACE, getInsertValues());
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
                return -1;
            }
            return 1;
        } finally {
            select.reset();
        }
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.EXTERNALS__Fields.wc_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.local_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.parent_relpath.toString(), getBind(3));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.presence.toString(), getBind(4));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.kind.toString(), 
        		select.isColumnNull(SVNWCDbSchema.NODES__Fields.file_external) ? "unknown" : "file");
        values.put(SVNWCDbSchema.EXTERNALS__Fields.def_local_relpath.toString(), getBind(5));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.repos_id.toString(), getBind(6));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.def_repos_relpath.toString(), getBind(7));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.def_operational_revision.toString(), getBind(8));
        values.put(SVNWCDbSchema.EXTERNALS__Fields.def_revision.toString(), getBind(9));
        return values;
    }

}
