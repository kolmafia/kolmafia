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
 * SELECT checksum FROM pristine WHERE md5_checksum = ?1
 * 
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectSHA1Checksum extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectSHA1Checksum(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.PRISTINE);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        String md5Checksum = (String) getBind(1);
        return md5Checksum.equals(getColumnString(SVNWCDbSchema.PRISTINE__Fields.md5_checksum));
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return null;
    }
}
