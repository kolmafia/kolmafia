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
 * INSERT OR REPLACE INTO nodes ( wc_id, local_relpath, op_depth,
 * parent_relpath, repos_id, repos_path, revision, presence, depth, kind,
 * changed_revision, changed_date, changed_author, checksum, properties,
 * translated_size, last_mod_time, dav_cache, symlink_target ) VALUES (?1, ?2,
 * ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18,
 * ?19);
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertNode extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES, SqlJetConflictAction.REPLACE);
    }

    @Override
    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("wc_id", getBind(1));
        values.put("local_relpath", getBind(2));
        values.put("op_depth", getBind(3));
        values.put("parent_relpath", getBind(4));
        values.put("repos_id", getBind(5));
        values.put("repos_path", getBind(6));
        values.put("revision", getBind(7));
        values.put("presence", getBind(8));
        values.put("depth", getBind(9));
        values.put("kind", getBind(10));
        values.put("changed_revision", getBind(11));
        values.put("changed_date", getBind(12));
        values.put("changed_author", getBind(13));
        values.put("checksum", getBind(14));
        values.put("properties", getBind(15));
        values.put("translated_size", getBind(16));
        values.put("last_mod_time", getBind(17));
        values.put("dav_cache", getBind(18));
        values.put("symlink_target", getBind(19));
        values.put("file_external", getBind(20));
        return values;
    }

}
