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
 * select id from wcroot where local_abspath is null;
 * 
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectWCRootNull extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.WCROOT__Fields> {

    public SVNWCDbSelectWCRootNull(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.WCROOT);
    }

    protected String getIndexName() {
        return SVNWCDbSchema.WCROOT__Indices.I_LOCAL_ABSPATH.toString();
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.WCROOT__Fields.id);
    }

}
