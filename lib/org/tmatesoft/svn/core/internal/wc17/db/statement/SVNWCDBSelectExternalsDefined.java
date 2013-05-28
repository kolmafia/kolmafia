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
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.EXTERNALS__Fields;

/**
 * SELECT local_relpath, def_local_relpath
 * FROM externals
 * WHERE wc_id = ?1 
 * AND (?2 = ''
 *      OR def_local_relpath = ?2
 *      OR (def_local_relpath > ?2 || '/' AND def_local_relpath < ?2 || '0'))
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDBSelectExternalsDefined extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.EXTERNALS__Fields> {

    public SVNWCDBSelectExternalsDefined(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.EXTERNALS);
    }

    protected void defineFields() {
        fields.add(EXTERNALS__Fields.local_relpath);
        fields.add(EXTERNALS__Fields.def_local_relpath);
    }
    
    @Override
    protected boolean isFilterPassed() throws SVNException {        
        String selectPath = (String) getBind(2);
        if ("".equals(selectPath)) {
            return true;
        }
        String rowPath = getColumnString(EXTERNALS__Fields.def_local_relpath);
        return (selectPath.equals(rowPath) || rowPath.startsWith(selectPath + "/"));
    }

    protected Object[] getWhere() throws SVNException {        
        return new Object[] {getBind(1)};
    }

}
