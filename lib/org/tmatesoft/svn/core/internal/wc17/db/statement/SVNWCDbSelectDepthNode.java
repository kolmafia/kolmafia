package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/**
 * SELECT repos_id, repos_path, presence, kind, revision, checksum,
 *   translated_size, changed_revision, changed_date, changed_author, depth,
 *   symlink_target, last_mod_time, properties
 * FROM nodes
 *   WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = ?3
 *
 * @version 1.8
 */
public class SVNWCDbSelectDepthNode extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectDepthNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.repos_id);
        fields.add(SVNWCDbSchema.NODES__Fields.repos_path);
        fields.add(SVNWCDbSchema.NODES__Fields.presence);
        fields.add(SVNWCDbSchema.NODES__Fields.kind);
        fields.add(SVNWCDbSchema.NODES__Fields.revision);
        fields.add(SVNWCDbSchema.NODES__Fields.checksum);
        fields.add(SVNWCDbSchema.NODES__Fields.translated_size);
        fields.add(SVNWCDbSchema.NODES__Fields.changed_revision);
        fields.add(SVNWCDbSchema.NODES__Fields.changed_date);
        fields.add(SVNWCDbSchema.NODES__Fields.changed_author);
        fields.add(SVNWCDbSchema.NODES__Fields.depth);
        fields.add(SVNWCDbSchema.NODES__Fields.symlink_target);
        fields.add(SVNWCDbSchema.NODES__Fields.last_mod_time);
        fields.add(SVNWCDbSchema.NODES__Fields.properties);
    }
}
