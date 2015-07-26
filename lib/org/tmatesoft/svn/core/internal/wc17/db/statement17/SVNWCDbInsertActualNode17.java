package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

import java.util.HashMap;
import java.util.Map;

public class SVNWCDbInsertActualNode17 extends SVNSqlJetInsertStatement {

    public SVNWCDbInsertActualNode17(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE, SqlJetConflictAction.REPLACE);
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath.toString(), getBind(3));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.properties.toString(), getBind(4));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString(), getBind(5));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString(), getBind(6));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString(), getBind(7));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.toString(), getBind(8));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist.toString(), getBind(9));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.text_mod.toString(), null);
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data.toString(), getBind(10));
        return values;
    }
}
