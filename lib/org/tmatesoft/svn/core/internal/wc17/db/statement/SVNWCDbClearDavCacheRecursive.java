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
 * -- STMT_CLEAR_BASE_NODE_RECURSIVE_DAV_CACHE
 * UPDATE nodes SET dav_cache = NULL
 * WHERE 
 * dav_cache IS NOT NULL 
 * AND wc_id = ?1 
 * AND op_depth = 0
 * AND (?2 = ''
 *      OR local_relpath = ?2
 *      OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 *  
 *  primary index: wc_id, local_relpath, op_depth    
 */
public class SVNWCDbClearDavCacheRecursive extends SVNSqlJetUpdateStatement {

    public SVNWCDbClearDavCacheRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }
    
    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> rowValues = getRowValues();
        rowValues.put(SVNWCDbSchema.NODES__Fields.dav_cache.toString(), null);
        return rowValues;
    }
    
    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
    
    @Override
    protected boolean isFilterPassed() throws SVNException {
    	if (isColumnNull(SVNWCDbSchema.NODES__Fields.dav_cache)) {
            return false;
        }    	
        if (getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) != 0) {
            return false;
        }
        return true;
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }
}
