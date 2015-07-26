package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

import java.util.HashMap;
import java.util.Map;

/**
 * UPDATE nodes SET moved_to = NULL
 * WHERE wc_id = ?1
 * AND IS_STRICT_DESCENDANT_OF(moved_to, ?2)
 *
 * @version 1.8
 */
public class SVNWCDbClearMovedToDescendants extends SVNSqlJetUpdateStatement {

    public SVNWCDbClearMovedToDescendants(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.NODES__Fields.moved_to.name(), null);
        return values;
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected String getRowPath() throws SVNException {
        return getColumnString(SVNWCDbSchema.NODES__Fields.moved_to);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }
}
