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
 * SELECT md5_checksum FROM pristine WHERE checksum = ?1
 * 
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectPristineMD5Checksum extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.PRISTINE__Fields> {

    public SVNWCDbSelectPristineMD5Checksum(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.PRISTINE);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.PRISTINE__Fields.md5_checksum);
    }

}
