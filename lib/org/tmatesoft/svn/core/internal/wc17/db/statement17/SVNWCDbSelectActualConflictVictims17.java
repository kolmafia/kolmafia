package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

public class SVNWCDbSelectActualConflictVictims17 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

    public SVNWCDbSelectActualConflictVictims17(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    protected boolean isFilterPassed() throws SVNException {
        return getBind(2).equals(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath) && (!(isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject.toString()) && isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString())
                && isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString()) && isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString()) && isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data
                .toString())));
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}

