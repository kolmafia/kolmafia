package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * UPDATE actual_node SET conflict_old = ?3, conflict_new = ?4,
 * conflict_working = ?5, prop_reject = ?6, tree_conflict_data = ?7
 * WHERE wc_id = ?1 AND local_relpath = ?2
 *
 * @version 1.8
 * @author TMate Software Ltd.
 */
public class SVNWCDbUpdateActualConflictData17 extends SVNSqlJetUpdateStatement {

    public SVNWCDbUpdateActualConflictData17(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.name(), getBind(3));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.name(), getBind(4));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.name(), getBind(5));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.name(), getBind(6));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.name(), getBind(7));
        return values;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1), getBind(2)};
    }
}
