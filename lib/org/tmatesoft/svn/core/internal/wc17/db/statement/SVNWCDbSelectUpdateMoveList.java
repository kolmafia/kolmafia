package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/*
 * SELECT local_relpath, action, kind, content_state, prop_state
 * FROM update_move_list
 * ORDER BY local_relpath
 *
 * @version 1.8
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectUpdateMoveList extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.UPDATE_MOVE_LIST__Fields> {

    public SVNWCDbSelectUpdateMoveList(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.UPDATE_MOVE_LIST);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.local_relpath);
        fields.add(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.action);
        fields.add(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.kind);
        fields.add(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.content_state);
        fields.add(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.prop_state);
    }
}
