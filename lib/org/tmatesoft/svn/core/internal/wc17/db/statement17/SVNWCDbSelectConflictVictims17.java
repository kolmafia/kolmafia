package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

public class SVNWCDbSelectConflictVictims17 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

    public SVNWCDbSelectConflictVictims17(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getBind(2).equals(getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath)) &&
                (!isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) ||
                        !isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new) ||
                        !isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working));
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}
