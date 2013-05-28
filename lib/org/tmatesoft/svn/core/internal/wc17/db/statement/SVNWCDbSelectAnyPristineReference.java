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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUnionStatement;

/**
 * SELECT 1 FROM nodes WHERE checksum = ?1 OR checksum = ?2 UNION ALL SELECT 1
 * FROM actual_node WHERE older_checksum = ?1 OR older_checksum = ?2 OR
 * left_checksum = ?1 OR left_checksum = ?2 OR right_checksum = ?1 OR
 * right_checksum = ?2 LIMIT 1
 *
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectAnyPristineReference extends SVNSqlJetUnionStatement {

    public SVNWCDbSelectAnyPristineReference(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, selectNodes(sDb), selectActual(sDb));
    }

    private static SVNSqlJetStatement selectActual(SVNSqlJetDb sDb) throws SVNException {
        return new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES) {

            protected boolean isFilterPassed() throws SVNException {
                if (!isColumnNull(SVNWCDbSchema.NODES__Fields.checksum)) {
                    String checksum = getColumnString(SVNWCDbSchema.NODES__Fields.checksum);
                    return checksum.equals(getBind(1)) || checksum.equals(getBind(2));
                }
                return false;
            }
        };
    }

    private static SVNSqlJetStatement selectNodes(SVNSqlJetDb sDb) throws SVNException {
        return new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.ACTUAL_NODE) {

            protected boolean isFilterPassed() throws SVNException {
                if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.older_checksum)) {
                    String older_checksum = getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.older_checksum);
                    if (older_checksum.equals(getBind(1)) || older_checksum.equals(getBind(2))) {
                        return true;
                    }
                }
                if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.left_checksum)) {
                    String left_checksum = getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.left_checksum);
                    if (left_checksum.equals(getBind(1)) || left_checksum.equals(getBind(2))) {
                        return true;
                    }
                }
                if (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.right_checksum)) {
                    String right_checksum = getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.right_checksum);
                    if (right_checksum.equals(getBind(1)) || right_checksum.equals(getBind(2))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

}
