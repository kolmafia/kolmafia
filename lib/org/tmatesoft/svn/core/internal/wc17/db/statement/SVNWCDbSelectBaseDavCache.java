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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/**
 * -- STMT_SELECT_BASE_DAV_CACHE
 * SELECT dav_cache FROM nodes
 * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = 0
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectBaseDavCache extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectBaseDavCache(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.dav_cache);
    }

    protected Object[] getWhere() throws SVNException {
        bindLong(3, 0);
        return super.getWhere();
    }

}
