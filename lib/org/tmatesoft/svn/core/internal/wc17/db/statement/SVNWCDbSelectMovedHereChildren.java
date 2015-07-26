package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

/**
 * SELECT moved_to, local_relpath FROM nodes
 * WHERE wc_id = ?1 AND op_depth > 0
 * AND IS_STRICT_DESCENDANT_OF(moved_to, ?2)
 *
 * @version 1.8
 */
public class SVNWCDbSelectMovedHereChildren extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectMovedHereChildren(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES, SVNWCDbSchema.NODES__Indices.I_NODES_MOVED);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
    }

    @Override
    protected String getRowPath() throws SVNException {
        return (String)(getRowValues().get(SVNWCDbSchema.NODES__Fields.moved_to.name()));
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) > 0;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }
}
