package org.tmatesoft.svn.core.internal.wc17.db.statement17;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

public class SVNWCDbSelectActualChildrenInfo17 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

    public SVNWCDbSelectActualChildrenInfo17(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.properties);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getBind(2).equals(getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath));
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}

