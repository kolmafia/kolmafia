package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

import java.util.HashMap;
import java.util.Map;

/**
 * UPDATE NODES SET moved_to = NULL
 * WHERE wc_id = ?1
 *       AND moved_to = ?2
 *
 *
 *
 *
 *
 *
 */
public class SVNWCDbClearMovedToFromDest extends SVNSqlJetUpdateStatement {

    public SVNWCDbClearMovedToFromDest(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.NODES__Fields.moved_to.name(), null);
        return values;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getBind(2).equals(getColumnString(SVNWCDbSchema.NODES__Fields.moved_to));
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}
