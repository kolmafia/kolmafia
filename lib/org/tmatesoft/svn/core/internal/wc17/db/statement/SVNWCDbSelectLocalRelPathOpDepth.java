package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/*
 * SELECT local_relpath
 * FROM nodes
 * WHERE wc_id = ?1
 *  AND (local_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 *  AND op_depth = ?3
 *
 *  @version 1.8
 */
public class SVNWCDbSelectLocalRelPathOpDepth extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectLocalRelPathOpDepth(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == (Long)getBind(3);
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return false;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}
