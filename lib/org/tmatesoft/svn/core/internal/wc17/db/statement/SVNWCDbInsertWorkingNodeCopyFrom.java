package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

import java.util.HashMap;
import java.util.Map;

/*
* INSERT OR REPLACE INTO nodes (
*    wc_id, local_relpath, op_depth, parent_relpath, repos_id,
*    repos_path, revision, presence, depth, moved_here, kind, changed_revision,
*    changed_date, changed_author, checksum, properties, translated_size,
*    last_mod_time, symlink_target, moved_to )
* SELECT wc_id, ?3 /local_relpath/, ?4 /op_depth/, ?5 /parent_relpath/,
*       repos_id, repos_path, revision, ?6 /presence/, depth,
*       ?7/moved_here/, kind, changed_revision, changed_date,
*       changed_author, checksum, properties, translated_size,
*       last_mod_time, symlink_target,
*       (SELECT dst.moved_to FROM nodes AS dst
*       WHERE dst.wc_id = ?1
*       AND dst.local_relpath = ?3
*       AND dst.op_depth = ?4)
*       FROM nodes_current
*       WHERE wc_id = ?1 AND local_relpath = ?2
*/

public class SVNWCDbInsertWorkingNodeCopyFrom extends SVNSqlJetInsertStatement {

    private SVNSqlJetSelectStatement internalStatement1;
    private SVNSqlJetSelectStatement internalStatement2;

    public SVNWCDbInsertWorkingNodeCopyFrom(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES, SqlJetConflictAction.REPLACE);
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        if (internalStatement1 == null) {
            internalStatement1 = new InternalStatement1(sDb);
            internalStatement1.bindf("is", getBind(1), getBind(2));
            internalStatement1.next();
        }
        if (internalStatement2 == null) {
            internalStatement2 = new InternalStatement2(sDb);
            internalStatement2.bindf("isi", getBind(1), getBind(3), getBind(4));
            internalStatement2.next();
        }
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.NODES__Fields.wc_id.name(), internalStatement1.getColumnLong(SVNWCDbSchema.NODES__Fields.wc_id));
        values.put(SVNWCDbSchema.NODES__Fields.local_relpath.name(), getBind(3));
        values.put(SVNWCDbSchema.NODES__Fields.op_depth.name(), getBind(4));
        values.put(SVNWCDbSchema.NODES__Fields.parent_relpath.name(), getBind(5));
        values.put(SVNWCDbSchema.NODES__Fields.repos_id.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.repos_id));
        values.put(SVNWCDbSchema.NODES__Fields.repos_path.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.repos_path));
        values.put(SVNWCDbSchema.NODES__Fields.revision.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.revision));
        values.put(SVNWCDbSchema.NODES__Fields.presence.name(), getBind(6));
        values.put(SVNWCDbSchema.NODES__Fields.depth.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.depth));
        values.put(SVNWCDbSchema.NODES__Fields.moved_here.name(), getBind(7));
        values.put(SVNWCDbSchema.NODES__Fields.kind.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.kind));
        values.put(SVNWCDbSchema.NODES__Fields.changed_revision.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.changed_revision));
        values.put(SVNWCDbSchema.NODES__Fields.changed_date.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.changed_date));
        values.put(SVNWCDbSchema.NODES__Fields.changed_author.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.changed_author));
        values.put(SVNWCDbSchema.NODES__Fields.checksum.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.checksum));
        values.put(SVNWCDbSchema.NODES__Fields.properties.name(), internalStatement1.getColumnBlob(SVNWCDbSchema.NODES__Fields.properties));
        values.put(SVNWCDbSchema.NODES__Fields.translated_size.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.translated_size));
        values.put(SVNWCDbSchema.NODES__Fields.last_mod_time.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.last_mod_time));
        values.put(SVNWCDbSchema.NODES__Fields.symlink_target.name(), internalStatement1.getColumnString(SVNWCDbSchema.NODES__Fields.symlink_target));
        values.put(SVNWCDbSchema.NODES__Fields.moved_to.name(), internalStatement2.getColumnString(SVNWCDbSchema.NODES__Fields.moved_to));
        return values;
    }

    public void reset() throws SVNException {
        super.reset();
        if (internalStatement1 != null) {
            internalStatement1.reset();
            internalStatement1 = null;
        }
        if (internalStatement2 != null) {
            internalStatement2.reset();
            internalStatement2 = null;
        }
    }

    private static class InternalStatement1 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        private long maxOpDepth;

        public InternalStatement1(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
            maxOpDepth = -1;
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.wc_id);
            fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
            fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
            fields.add(SVNWCDbSchema.NODES__Fields.parent_relpath);
            fields.add(SVNWCDbSchema.NODES__Fields.repos_path);
            fields.add(SVNWCDbSchema.NODES__Fields.revision);
            fields.add(SVNWCDbSchema.NODES__Fields.presence);
            fields.add(SVNWCDbSchema.NODES__Fields.depth);
            fields.add(SVNWCDbSchema.NODES__Fields.moved_here);
            fields.add(SVNWCDbSchema.NODES__Fields.kind);
            fields.add(SVNWCDbSchema.NODES__Fields.changed_revision);
            fields.add(SVNWCDbSchema.NODES__Fields.changed_date);
            fields.add(SVNWCDbSchema.NODES__Fields.changed_author);
            fields.add(SVNWCDbSchema.NODES__Fields.checksum);
            fields.add(SVNWCDbSchema.NODES__Fields.properties);
            fields.add(SVNWCDbSchema.NODES__Fields.translated_size);
            fields.add(SVNWCDbSchema.NODES__Fields.last_mod_time);
            fields.add(SVNWCDbSchema.NODES__Fields.symlink_target);
        }

        private long getMaxOpDepth() throws SVNException {
            if (maxOpDepth == -1) {
                SVNWCDbNodesMaxOpDepth maxOpDepth = new SVNWCDbNodesMaxOpDepth(sDb, 0);
                try {
                    this.maxOpDepth = maxOpDepth.getMaxOpDepth((Long) getBind(1), (String) getBind(2));
                } finally {
                    maxOpDepth.reset();
                }
            }
            return maxOpDepth;
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) >= getMaxOpDepth();
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[]  {getBind(1), getBind(2)};
        }
    }

    private static class InternalStatement2 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        public InternalStatement2(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
        }
    }
}
