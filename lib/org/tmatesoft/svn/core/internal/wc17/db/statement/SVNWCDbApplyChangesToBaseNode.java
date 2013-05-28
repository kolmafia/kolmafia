/*
 * ====================================================================
 * Copyright (c) 2004-2011 TMate Software Ltd.  All rights reserved.
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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * INSERT OR REPLACE INTO nodes ( wc_id, local_relpath, op_depth,
 * parent_relpath, repos_id, repos_path, revision, presence, depth, kind,
 * changed_revision, changed_date, changed_author, checksum, properties,
 * dav_cache, symlink_target, file_external ) VALUES (?1, ?2, 0, ?3, ?4, ?5, ?6, ?7, ?8, ?9,
 * ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17);
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbApplyChangesToBaseNode extends SVNSqlJetInsertStatement {

    public SVNWCDbApplyChangesToBaseNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES, SqlJetConflictAction.REPLACE);
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.NODES__Fields.wc_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.NODES__Fields.local_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.NODES__Fields.op_depth.toString(), 0);
        values.put(SVNWCDbSchema.NODES__Fields.parent_relpath.toString(), getBind(3));
        values.put(SVNWCDbSchema.NODES__Fields.repos_id.toString(), getBind(4));
        values.put(SVNWCDbSchema.NODES__Fields.repos_path.toString(), getBind(5));
        values.put(SVNWCDbSchema.NODES__Fields.revision.toString(), getBind(6));
        values.put(SVNWCDbSchema.NODES__Fields.presence.toString(), getBind(7));
        values.put(SVNWCDbSchema.NODES__Fields.depth.toString(), getBind(8));
        values.put(SVNWCDbSchema.NODES__Fields.kind.toString(), getBind(9));
        values.put(SVNWCDbSchema.NODES__Fields.changed_revision.toString(), getBind(10));
        values.put(SVNWCDbSchema.NODES__Fields.changed_date.toString(), getBind(11));
        values.put(SVNWCDbSchema.NODES__Fields.changed_author.toString(), getBind(12));
        values.put(SVNWCDbSchema.NODES__Fields.checksum.toString(), getBind(13));
        values.put(SVNWCDbSchema.NODES__Fields.properties.toString(), getBind(14));
        values.put(SVNWCDbSchema.NODES__Fields.dav_cache.toString(), getBind(15));
        values.put(SVNWCDbSchema.NODES__Fields.symlink_target.toString(), getBind(16));
        
        SVNSqlJetStatement stmt = sDb.getStatement(SVNWCDbStatements.SELECT_BASE_NODE);
        try {
            stmt.bindf("is", getBind(1), getBind(2));
            if (stmt.next() && !stmt.isColumnNull(NODES__Fields.file_external)) {
                values.put(SVNWCDbSchema.NODES__Fields.file_external.toString(), stmt.getColumn(NODES__Fields.file_external));
            }
        } finally {
            if (stmt != null) {
                stmt.reset();
            }
        }
        
        return values;
    }

}
