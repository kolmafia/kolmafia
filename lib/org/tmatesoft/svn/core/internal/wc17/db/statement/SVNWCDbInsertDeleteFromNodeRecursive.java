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
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/**
 * INSERT INTO nodes (
 *   wc_id, local_relpath, op_depth, parent_relpath, presence, kind)
 * SELECT wc_id, local_relpath, ?4 _op_depth_, parent_relpath, 'base-deleted',
 *      kind
 * FROM nodes
 * WHERE wc_id = ?1
 * AND (local_relpath = ?2
 *      OR (local_relpath > ?2 || '/' AND local_relpath < ?2 || '0'))
 * AND op_depth = ?3
 * AND presence NOT IN ('base-deleted', 'not-present', 'excluded', 'absent')
 */
public class SVNWCDbInsertDeleteFromNodeRecursive extends SVNSqlJetInsertStatement {

    protected SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> select;

    public SVNWCDbInsertDeleteFromNodeRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        select = new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields>(sDb, SVNWCDbSchema.NODES) {

            protected Object[] getWhere() throws SVNException {
                return new Object[] {getBind(1)};
            };

            protected boolean isFilterPassed() throws SVNException {
                final long selectDepth = (Long) SVNWCDbInsertDeleteFromNodeRecursive.this.getBind(3);
                final long rowDepth = getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
                if (rowDepth != selectDepth) {
                    return false;
                }
                final String rowPresence = getColumnString(SVNWCDbSchema.NODES__Fields.presence);
                if (!"base-deleted".equals(rowPresence) && !"not-present".equals(rowPresence) && !"excluded".equals(rowPresence) && !"absent".equals(rowPresence)) {
                    return true;
                }
                return false;
            };

            @Override
            protected String getPathScope() {
                return (String) getBind(2);
            }
            
            protected void defineFields() {
                fields.add(SVNWCDbSchema.NODES__Fields.wc_id);
                fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
                fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
                fields.add(SVNWCDbSchema.NODES__Fields.parent_relpath);
                fields.add(SVNWCDbSchema.NODES__Fields.presence);
                fields.add(SVNWCDbSchema.NODES__Fields.kind);
            }
        };
    }

    public long exec() throws SVNException {
        select.bindf("isi", getBind(1), getBind(2), getBind(3));
        long n = 0;
        try {
            while (select.next()) {
                super.exec();
                n++;
            }
        } finally {
            select.reset();
        }
        return n;
    }


    @Override
    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> rowValues = select.getRowValues();
        Map<String, Object> insertValues = new HashMap<String, Object>();
        
        insertValues.put(SVNWCDbSchema.NODES__Fields.op_depth.toString(), getBind(4));
        insertValues.put(SVNWCDbSchema.NODES__Fields.presence.toString(), "base-deleted");
        insertValues.put(SVNWCDbSchema.NODES__Fields.wc_id.toString(), getBind(1));
        insertValues.put(SVNWCDbSchema.NODES__Fields.local_relpath.toString(), rowValues.get(SVNWCDbSchema.NODES__Fields.local_relpath.toString()));
        insertValues.put(SVNWCDbSchema.NODES__Fields.parent_relpath.toString(), rowValues.get(SVNWCDbSchema.NODES__Fields.parent_relpath.toString()));
        insertValues.put(SVNWCDbSchema.NODES__Fields.kind.toString(), rowValues.get(SVNWCDbSchema.NODES__Fields.kind.toString()));
        return insertValues;
    }}
