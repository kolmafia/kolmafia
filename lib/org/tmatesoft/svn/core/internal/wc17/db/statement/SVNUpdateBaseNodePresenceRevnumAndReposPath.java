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

import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

/**
 * UPDATE nodes SET presence = ?3, revision = ?4, repos_path = ?5 WHERE wc_id =
 * ?1 AND local_relpath = ?2 AND op_depth = 0;
 *
 * @author TMate Software Ltd.
 */
public class SVNUpdateBaseNodePresenceRevnumAndReposPath extends SVNSqlJetUpdateStatement {

    public SVNUpdateBaseNodePresenceRevnumAndReposPath(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> v = getRowValues();
        v.put(SVNWCDbSchema.NODES__Fields.presence.toString(), getBind(3));
        v.put(SVNWCDbSchema.NODES__Fields.revision.toString(), getBind(4));
        v.put(SVNWCDbSchema.NODES__Fields.repos_path.toString(), getBind(5));
        return v;
    }

    protected Object[] getWhere() throws SVNException {
        return new Object[] {
                getBind(1), getBind(2), 0
        };
    }

}
