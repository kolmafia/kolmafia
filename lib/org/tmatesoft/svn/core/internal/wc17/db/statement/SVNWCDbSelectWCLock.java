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
 * SELECT locked_levels FROM WC_LOCK WHERE wc_id = ?1 AND local_dir_relpath =
 * ?2;
 * 
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectWCLock extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.WC_LOCK__Fields> {

    public SVNWCDbSelectWCLock(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.WC_LOCK);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.WC_LOCK__Fields.locked_levels);
    }

}
