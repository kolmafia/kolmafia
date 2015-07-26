package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

import java.util.HashMap;
import java.util.Map;

/*
 * UPDATE nodes SET op_depth = ?4, moved_here = NULL
 * WHERE wc_id = ?1
 *  AND (local_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 *  AND op_depth = ?3
 */
public class SVNWCDbUpdateOpDepthRecursive extends SVNSqlJetUpdateStatement {

    public SVNWCDbUpdateOpDepthRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.NODES__Fields.op_depth.toString(), getBind(4));
        values.put(SVNWCDbSchema.NODES__Fields.moved_here.toString(), null);
        return values;
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return false;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == (Long)getBind(3);
    }
}
