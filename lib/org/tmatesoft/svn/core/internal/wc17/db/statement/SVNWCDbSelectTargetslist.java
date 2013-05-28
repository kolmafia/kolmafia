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
 * SELECT local_relpath FROM targets_list 
 * WHERE kind = 'file' AND wc_id = ?1
 * @author TMate Software Ltd.
 */

public class SVNWCDbSelectTargetslist extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.TARGETS_LIST__Fields> {

    public SVNWCDbSelectTargetslist(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.TARGETS_LIST);
    }

    protected boolean isFilterPassed() throws SVNException {
        return ("file".equals(getColumnString(SVNWCDbSchema.TARGETS_LIST__Fields.kind)));
    };
    
    @Override
    protected Object[] getWhere() throws SVNException {
    	bindLong(1, (Long)getBind(1));
    	return super.getWhere();
    }
    
    protected void defineFields() {
        fields.add(SVNWCDbSchema.TARGETS_LIST__Fields.local_relpath);
    }

}
