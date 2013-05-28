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
package org.tmatesoft.svn.core.internal.io.fs.revprop;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.io.fs.FSFS;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNFSFSRevPropSet extends SVNSqlJetStatement {

    private final ISqlJetTable table;

    public SVNFSFSRevPropSet(SVNSqlJetDb sDb) throws SqlJetException {
        super(sDb);
        table = sDb.getDb().getTable(FSFS.REVISION_PROPERTIES_TABLE);
    }

    public long insert(Object... data) throws SVNException {
        try {
            return table.insertOr(SqlJetConflictAction.REPLACE, data);
        } catch (SqlJetException e) {
            SVNErrorMessage err = SVNErrorMessage.create( SVNErrorCode.SQLITE_ERROR, e );
            SVNErrorManager.error(err, SVNLogType.FSFS);
            return -1;
        }
    }

}
