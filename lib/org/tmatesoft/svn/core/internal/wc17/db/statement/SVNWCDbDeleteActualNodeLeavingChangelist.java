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

/**
 * -- STMT_DELETE_ACTUAL_NODE_LEAVING_CHANGELIST_RECURSIVE
 * DELETE FROM actual_node
 * WHERE wc_id = ?1 AND local_relpaht = ?2
 * AND (changelist IS NULL
 *      OR NOT EXISTS (SELECT 1 FROM nodes_current c
 *                     WHERE c.wc_id = ?1 AND c.local_relpath = local_relpath
 *                       AND kind = 'file'))
 *
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteActualNodeLeavingChangelist extends SVNWCDbDeleteActualNodeLeavingChangelistRecursive {
    

    public SVNWCDbDeleteActualNodeLeavingChangelist(SVNSqlJetDb sDb) throws SVNException {
        super(sDb);
    }
    
    protected boolean isRecursive() {
        return false;
    }}
