package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

import java.util.HashMap;
import java.util.Map;

/*
 * INSERT OR REPLACE INTO nodes (
 *   wc_id, local_relpath, op_depth, parent_relpath, repos_id, repos_path,
 *   revision, presence, depth, kind, changed_revision, changed_date,
 *   changed_author, checksum, properties, translated_size, last_mod_time,
 *   symlink_target, moved_here, moved_to )
 * SELECT
 *   wc_id, ?4, ?5, ?6,
 *       repos_id,
 *       repos_path, revision, presence, depth, kind, changed_revision,
 *       changed_date, changed_author, checksum, properties, translated_size,
 *       last_mod_time, symlink_target, 1,
 *       (SELECT dst.moved_to FROM nodes AS dst
 *       WHERE dst.wc_id = ?1
 *       AND dst.local_relpath = ?4
 *       AND dst.op_depth = ?5)
 * FROM nodes
 *   WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = ?3
 * @version 1.8
 */
public class SVNWCDbCopyMoveNode extends SVNSqlJetStatement {

    private ISqlJetTable table;

    public SVNWCDbCopyMoveNode(SVNSqlJetDb sDb) throws SqlJetException {
        super(sDb);
        table = sDb.getDb().getTable(SVNWCDbSchema.NODES.toString());
    }

    @Override
    public long exec() throws SVNException {
        InternalSelectStatement stmt1 = new InternalSelectStatement(sDb);
        InternalSelectStatement stmt2 = new InternalSelectStatement(sDb);
        try {
            stmt1.bindf("isi", getBind(1), getBind(2), getBind(3));
            stmt2.bindf("isi", getBind(1), getBind(4), getBind(5));
            boolean haveRow1 = stmt1.next();
            boolean haveRow2 = stmt2.next();
            assert haveRow1;

            Map<String, Object> values = new HashMap<String, Object>();
            values.put(SVNWCDbSchema.NODES__Fields.wc_id.name(), stmt1.getColumnLong(SVNWCDbSchema.NODES__Fields.wc_id));
            values.put(SVNWCDbSchema.NODES__Fields.local_relpath.name(), getBind(4));
            values.put(SVNWCDbSchema.NODES__Fields.op_depth.name(), getBind(5));
            values.put(SVNWCDbSchema.NODES__Fields.parent_relpath.name(), getBind(6));
            values.put(SVNWCDbSchema.NODES__Fields.repos_id.name(), stmt1.getColumnLong(SVNWCDbSchema.NODES__Fields.repos_id));
            values.put(SVNWCDbSchema.NODES__Fields.repos_path.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.repos_path));
            values.put(SVNWCDbSchema.NODES__Fields.revision.name(), stmt1.getColumnLong(SVNWCDbSchema.NODES__Fields.revision));
            values.put(SVNWCDbSchema.NODES__Fields.presence.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.presence));
            values.put(SVNWCDbSchema.NODES__Fields.depth.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.depth));
            values.put(SVNWCDbSchema.NODES__Fields.kind.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.kind));
            values.put(SVNWCDbSchema.NODES__Fields.changed_revision.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.changed_revision));
            values.put(SVNWCDbSchema.NODES__Fields.changed_date.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.changed_date));
            values.put(SVNWCDbSchema.NODES__Fields.changed_author.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.changed_author));
            values.put(SVNWCDbSchema.NODES__Fields.checksum.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.checksum));
            values.put(SVNWCDbSchema.NODES__Fields.properties.name(), stmt1.getColumnBlob(SVNWCDbSchema.NODES__Fields.properties));
            values.put(SVNWCDbSchema.NODES__Fields.translated_size.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.translated_size));
            values.put(SVNWCDbSchema.NODES__Fields.last_mod_time.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.last_mod_time));
            values.put(SVNWCDbSchema.NODES__Fields.symlink_target.name(), stmt1.getColumnString(SVNWCDbSchema.NODES__Fields.symlink_target));
            values.put(SVNWCDbSchema.NODES__Fields.moved_here.name(), 1);
            values.put(SVNWCDbSchema.NODES__Fields.moved_to.name(), haveRow2 ? stmt2.getColumnString(SVNWCDbSchema.NODES__Fields.moved_to) : null);
            try {
                return table.insertByFieldNamesOr(SqlJetConflictAction.REPLACE, values);
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
                return 0;
            }
        } finally {
            stmt1.reset();
            stmt2.reset();
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
            fields.add(SVNWCDbSchema.NODES__Fields.repos_id);
            fields.add(SVNWCDbSchema.NODES__Fields.repos_path);
            fields.add(SVNWCDbSchema.NODES__Fields.revision);
            fields.add(SVNWCDbSchema.NODES__Fields.presence);
            fields.add(SVNWCDbSchema.NODES__Fields.depth);
            fields.add(SVNWCDbSchema.NODES__Fields.kind);
            fields.add(SVNWCDbSchema.NODES__Fields.changed_revision);
            fields.add(SVNWCDbSchema.NODES__Fields.changed_date);
            fields.add(SVNWCDbSchema.NODES__Fields.changed_author);
            fields.add(SVNWCDbSchema.NODES__Fields.checksum);
            fields.add(SVNWCDbSchema.NODES__Fields.properties);
            fields.add(SVNWCDbSchema.NODES__Fields.translated_size);
            fields.add(SVNWCDbSchema.NODES__Fields.last_mod_time);
            fields.add(SVNWCDbSchema.NODES__Fields.symlink_target);
            fields.add(SVNWCDbSchema.NODES__Fields.moved_here);
            fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[] {getBind(1), getBind(2), getBind(3)};
        }
    }
}
