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
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * -- STMT_SELECT_ALL_FILES
 * SELECT DISTINCT local_relpath FROM nodes
 * WHERE wc_id = ?1 AND parent_relpath = ?2 AND kind = 'file'
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectAllFiles extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectAllFiles(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES, SVNWCDbSchema.NODES__Indices.I_NODES_PARENT);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
    }

    protected Object[] getWhere() throws SVNException {
    	return new Object[] {(Long)getBind(1), (String)getBind(2)};
    }
    
    @Override
    protected boolean isFilterPassed() throws SVNException {
        return "file".equals(getColumnString(NODES__Fields.kind));
    }

}
