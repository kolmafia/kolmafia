package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

import java.util.HashMap;
import java.util.Map;

/*
 * INSERT INTO update_move_list(local_relpath, action, kind, content_state,
 * prop_state)
 * VALUES (?1, ?2, ?3, ?4, ?5)
 *
 * @version 1.8
 * @author TMate Software Ltd.
 *
 */
public class SVNWCDbInsertUpdateMoveList extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertUpdateMoveList(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.UPDATE_MOVE_LIST);
    }

    @Override
    protected Map<String, Object> getInsertValues() throws SVNException {
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.local_relpath.toString(), getBind(1));
        values.put(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.action.toString(), getBind(2));
        values.put(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.kind.toString(), getBind(3));
        values.put(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.content_state.toString(), getBind(4));
        values.put(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.prop_state.toString(), getBind(5));
        return values;
    }
}
