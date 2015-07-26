package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/**
 * SELECT moved_to
 * FROM nodes
 * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = ?3
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectMovedTo extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {
    public SVNWCDbSelectMovedTo(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
    }
}
