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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * -- STMT_SELECT_ALL_SERVER_EXCLUDED_NODES
 * SELECT local_relpath FROM nodes
 * WHERE wc_id = ?1
 * AND (?2 = ''
 *      OR local_relpath = ?2
 *      OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 *  AND op_depth = 0
 *  AND presence = 'absent'
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectAllServerExcludedNodes extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectAllServerExcludedNodes(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    protected boolean isFilterPassed() throws SVNException {
        if (!"absent".equals(getColumnString(NODES__Fields.presence))) {
            return false;
        }
        final long rowDepth = getColumnLong(NODES__Fields.op_depth);
        if (rowDepth != (Long) getBind(3)) {
            return false;
        }
        final String selectPath = (String) getBind(2);
        if ("".equals(selectPath)) {
            return true;
        }
        final String rowPath = getColumnString(NODES__Fields.local_relpath);
        return selectPath.equals(rowPath) || rowPath.startsWith(selectPath + '/');
    }

}
