package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

public class SVNWCDbSelectActualChildrenConflict17 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

    public SVNWCDbSelectActualChildrenConflict17(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    protected boolean isFilterPassed() throws SVNException {
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id)) {
            return false;
        }
        if (isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath)) {
            return false;
        }
        return getColumn(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id).equals(getBind(1)) && getColumn(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath).equals(getBind(2));
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }
}
