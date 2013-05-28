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

import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

/**
 * UPDATE actual_node SET conflict_old = NULL, conflict_new = NULL,
 * conflict_working = NULL WHERE wc_id = ?1 AND local_relpath = ?2;
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbClearTextConflict extends SVNSqlJetUpdateStatement {

    public SVNWCDbClearTextConflict(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> rowValues = getRowValues();
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString(), null);
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString(), null);
        return rowValues;
    }

}
