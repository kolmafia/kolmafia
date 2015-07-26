package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/**
 * SELECT local_relpath, moved_to FROM nodes
 * WHERE wc_id = ?1
 * AND (local_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 * AND op_depth >= ?3
 * AND moved_to IS NOT NULL
 * AND NOT IS_STRICT_DESCENDANT_OF(moved_to, ?2)
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectMovedOutside extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {
    public SVNWCDbSelectMovedOutside(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return false;
    }

    @Override
    protected String getPathScope() {
        return getBind(2).toString();
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        Long opDepth = (Long) getBind(3);

        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) >= opDepth &&
                getColumn(SVNWCDbSchema.NODES__Fields.moved_to) != null &&
                !isStrictDescendantOf(getColumnString(SVNWCDbSchema.NODES__Fields.moved_to), getBind(2).toString());
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
        fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
    }
}
