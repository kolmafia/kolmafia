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
 * INSERT INTO wcroot (local_abspath) VALUES (?1);
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertWCRoot extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertWCRoot(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.WCROOT);
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.WCROOT__Fields.local_abspath.toString(), getBind(1));
        return values;
    }

}
