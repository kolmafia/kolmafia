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

/**
 * 
 * STMT_SELECT_CHANGELIST_LIST
 * SELECT wc_id, local_relpath, notify, changelist
 * FROM changelist_list
 * ORDER BY wc_id, local_relpath
 * 
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectChangelist extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectChangelist(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.CHANGELIST_LIST);
    }

    
   
}
