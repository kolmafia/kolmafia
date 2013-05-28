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

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;

/**
 * INSERT INTO repository (root, uuid) VALUES (?1, ?2);
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertRepository extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertRepository(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.REPOSITORY);
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> rowValues = new HashMap<String, Object>();
        rowValues.put(SVNWCDbSchema.REPOSITORY__Fields.root.toString(), getBind(1));
        rowValues.put(SVNWCDbSchema.REPOSITORY__Fields.uuid.toString(), getBind(2));
        return rowValues;
    }

}
