package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * INSERT INFO actual_node (conflict_old, conflict_new, conflict_working, prop_reject, tree_conflict_data, parent_relpath)
 * VALUES (?3, ?4, ?5, ?6, ?7, ?8)
 * WHERE wc_id = ?1 AND local_relpath = ?2
 *
 * @version 1.8
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertActualConflictData17 extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertActualConflictData17(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    protected Map<String, Object> getInsertValues() throws SVNException {
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.name(), getBind(3));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.name(), getBind(4));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.name(), getBind(5));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.name(), getBind(6));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.name(), getBind(7));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath.name(), getBind(8));
        return values;
    }
}
