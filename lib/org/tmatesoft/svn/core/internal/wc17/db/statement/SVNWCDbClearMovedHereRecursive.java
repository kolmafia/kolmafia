package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

import java.util.Collections;
import java.util.Map;

/**
 * UPDATE nodes SET moved_here = NULL
 * WHERE wc_id = ?1
 * AND (local_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 * AND op_depth = ?3
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbClearMovedHereRecursive extends SVNSqlJetUpdateStatement {
    public SVNWCDbClearMovedHereRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        return Collections.singletonMap(SVNWCDbSchema.NODES__Fields.moved_here.toString(), null);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == (Long)getBind(3);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isStrictiDescendant() {
        return false;
    }

    @Override
    protected String getPathScope() {
        return getBind(2).toString();
    }
}
