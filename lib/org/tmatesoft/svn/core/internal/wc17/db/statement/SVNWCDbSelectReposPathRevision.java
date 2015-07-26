package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/*
 * SELECT local_relpath, repos_path, revision FROM nodes
 *   WHERE wc_id = ?1
 *   AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 *   AND op_depth = 0
 *   ORDER BY local_relpath
 *
 *
 */
public class SVNWCDbSelectReposPathRevision extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectReposPathRevision(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
        fields.add(SVNWCDbSchema.NODES__Fields.repos_path);
        fields.add(SVNWCDbSchema.NODES__Fields.revision);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0;
    }
}
