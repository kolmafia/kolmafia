package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

public class SVNWCDbFindConflictDescendant extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {
    public SVNWCDbFindConflictDescendant(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    protected void defineFields() {
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

    @Override
    protected Enum<?> getRowPathField() throws SVNException {
        return SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath;
    }

    @Override
    protected String getRowPath() throws SVNException {
        return getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
    }

    @Override
    protected boolean isPathScopeInIndex() throws SVNException {
        return super.isPathScopeInIndex();
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return !isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }
}
