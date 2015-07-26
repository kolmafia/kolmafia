package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

import java.util.HashMap;
import java.util.Map;

/*
 * INSERT OR REPLACE INTO nodes (wc_id, local_relpath, op_depth, parent_relpath,
 *                             kind, moved_to, presence)
 * SELECT wc_id, local_relpath, op_depth, parent_relpath,
 *      kind, moved_to, MAP_BASE_DELETED
 * FROM nodes
 *  WHERE wc_id = ?1
 *   AND (local_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 *   AND op_depth = ?3
 *
 * @version 1.8
 */
public class SVNWCDbReplaceWithBaseDeleted extends SVNSqlJetStatement {

    private ISqlJetTable table;

    public SVNWCDbReplaceWithBaseDeleted(SVNSqlJetDb sDb) throws SqlJetException {
        super(sDb);
        table = sDb.getDb().getTable(SVNWCDbSchema.LOCK.toString());
    }

    @Override
    public long exec() throws SVNException {
        InternalSelectStatement stmt = new InternalSelectStatement(sDb);
        try {
            stmt.bindf("isi", getBind(1), getBind(2), getBind(3));
            boolean haveRow = stmt.next();
            assert haveRow;

            Map<String, Object> values = new HashMap<String, Object>();
            values.put(SVNWCDbSchema.NODES__Fields.wc_id.name(), stmt.getColumnString(SVNWCDbSchema.NODES__Fields.wc_id));
            values.put(SVNWCDbSchema.NODES__Fields.local_relpath.name(), stmt.getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath));
            values.put(SVNWCDbSchema.NODES__Fields.op_depth.name(), stmt.getColumnString(SVNWCDbSchema.NODES__Fields.op_depth));
            values.put(SVNWCDbSchema.NODES__Fields.parent_relpath.name(), stmt.getColumnString(SVNWCDbSchema.NODES__Fields.parent_relpath));
            values.put(SVNWCDbSchema.NODES__Fields.kind.name(), stmt.getColumnString(SVNWCDbSchema.NODES__Fields.kind));
            values.put(SVNWCDbSchema.NODES__Fields.moved_to.name(), stmt.getColumnString(SVNWCDbSchema.NODES__Fields.moved_to));
            values.put(SVNWCDbSchema.NODES__Fields.presence.name(), SvnWcDbStatementUtil.getPresenceText(ISVNWCDb.SVNWCDbStatus.Deleted));
            try {
                return table.insertByFieldNamesOr(SqlJetConflictAction.REPLACE, values);
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
                return 0;
            }
        } finally {
            stmt.reset();
        }
    }

    private static class InternalSelectStatement extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        public InternalSelectStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.wc_id);
            fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
            fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
            fields.add(SVNWCDbSchema.NODES__Fields.parent_relpath);
            fields.add(SVNWCDbSchema.NODES__Fields.kind);
            fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == (Long)getBind(3);
        }

        @Override
        protected String getPathScope() {
            return (String)getBind(2);
        }

        @Override
        protected boolean isStrictiDescendant() {
            return false;
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[] {getBind(1)};
        }
    }
}
