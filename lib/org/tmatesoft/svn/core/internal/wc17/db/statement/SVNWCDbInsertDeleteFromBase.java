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
 *  wc_id, local_relpath, op_depth, parent_relpath, presence, kind)
 *  SELECT wc_id, local_relpath, ?3 depth, parent_relpath,
 *   'base-deleted', kind
 * FROM nodes
 * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = 0
 */
public class SVNWCDbInsertDeleteFromBase extends SVNSqlJetInsertStatement {

    protected SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> select;

    public SVNWCDbInsertDeleteFromBase(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        select = new SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields>(sDb, SVNWCDbSchema.NODES) {

            protected Object[] getWhere() throws SVNException {
                return new Object[] {
                        getBind(1), getBind(2), 0
                };
            };

            protected boolean isFilterPassed() throws SVNException {
                if (isColumnNull(SVNWCDbSchema.NODES__Fields.wc_id) && getBind(1) != null) {
                    return false;
                }
                if (isColumnNull(SVNWCDbSchema.NODES__Fields.local_relpath) && getBind(2) != null) {
                    return false;
                }
                return getColumn(SVNWCDbSchema.NODES__Fields.wc_id).equals(getBind(1)) && getColumn(SVNWCDbSchema.NODES__Fields.local_relpath).equals(getBind(2))
                        && getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0;

            };
            
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
        select.bindf("is", getBind(1), getBind(2));
        long n = 0;
        try {
            while (select.next()) {
                super.exec();
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
        
        insertValues.put(SVNWCDbSchema.NODES__Fields.op_depth.toString(), getBind(3));
        insertValues.put(SVNWCDbSchema.NODES__Fields.presence.toString(), "base-deleted");
        insertValues.put(SVNWCDbSchema.NODES__Fields.wc_id.toString(), getBind(1));
        insertValues.put(SVNWCDbSchema.NODES__Fields.local_relpath.toString(), getBind(2));
        insertValues.put(SVNWCDbSchema.NODES__Fields.parent_relpath.toString(), rowValues.get(SVNWCDbSchema.NODES__Fields.parent_relpath.toString()));
        insertValues.put(SVNWCDbSchema.NODES__Fields.kind.toString(), rowValues.get(SVNWCDbSchema.NODES__Fields.kind.toString()));
        return insertValues;
    }


}
