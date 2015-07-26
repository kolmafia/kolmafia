package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/*
 * SELECT local_relpath, conflict_data
 *  FROM actual_node
 * WHERE wc_id = ?1 AND parent_relpath = ?2 AND
 *  NOT (conflict_data IS NULL)
 *
 *  @version 1.8
 */
public class SVNWCDbSelectConflictVictims extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.ACTUAL_NODE__Fields> {

    public SVNWCDbSelectConflictVictims(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE, SVNWCDbSchema.ACTUAL_NODE__Indices.I_ACTUAL_PARENT);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
        fields.add(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return !isColumnNull(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }
}
