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
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * SELECT IFNULL((SELECT properties FROM actual_node a
 *               WHERE a.wc_id = ?1 AND A.local_relpath = n.local_relpath),
 *             properties),
 *      local_relpath, depth
 * FROM nodes n
 * WHERE wc_id = ?1
 * AND (?2 = ''
 *      OR local_relpath = ?2
 *      OR (local_relpath > ?2 || '/' AND local_relpath < ?2 || '0'))
 * AND kind = 'dir' AND presence='normal'
 * AND op_depth=(SELECT MAX(op_depth) FROM nodes o
 *               WHERE o.wc_id = ?1 AND o.local_relpath = n.local_relpath)
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDBSelectExternalProperties extends SVNSqlJetSelectStatement {

    private SVNWCDbNodesMaxOpDepth maxOpDepthSelect;
    private SVNSqlJetSelectStatement propertiesSelect;

    public SVNWCDBSelectExternalProperties(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        maxOpDepthSelect = new SVNWCDbNodesMaxOpDepth(sDb, 0);
        propertiesSelect = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }
    
    @Override
    protected boolean isFilterPassed() throws SVNException {
        if (!"dir".equals(getColumnString(SVNWCDbSchema.NODES__Fields.kind))) {
            return false;
        }
        if (!"normal".equals(getColumnString(SVNWCDbSchema.NODES__Fields.presence))) {
            return false;
        }
        return hasMaxOpDepth();
    }
    
    private boolean hasMaxOpDepth() throws SVNException {
        long rowOpDepth = getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
        Long maxOpDepth = maxOpDepthSelect.getMaxOpDepth(getColumnLong(NODES__Fields.wc_id), 
                getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath));
        return maxOpDepth != null && maxOpDepth == rowOpDepth;
    }
    
    @Override
    public SVNProperties getColumnProperties(String f) throws SVNException {
        if (!NODES__Fields.properties.toString().equals(f)) {
            return super.getColumnProperties(f);
        }
        SVNProperties properties = null;
        try {
            propertiesSelect.bindf("is", getBind(1), getColumnString(NODES__Fields.local_relpath));
            if (propertiesSelect.next()) {
                properties = propertiesSelect.getColumnProperties(ACTUAL_NODE__Fields.properties);
            }
        } finally {
            propertiesSelect.reset();
        }
        if (properties != null) {
            return properties;
        }
        return super.getColumnProperties(f);
    }

    protected Object[] getWhere() throws SVNException {        
        return new Object[] {getBind(1)};
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

}
