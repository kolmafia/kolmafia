package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUnionStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * UPDATE nodes SET moved_to = RELPATH_SKIP_JOIN(?2, ?3, moved_to)
 * WHERE wc_id = ?1
 *  AND IS_STRICT_DESCENDANT_OF(moved_to, ?2)
 *
 *
 * @version 1.8
 */
public class SVNWCDbUpdateMovedToDescendants extends SVNSqlJetUpdateStatement {

    public SVNWCDbUpdateMovedToDescendants(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.NODES__Fields.moved_to.name(), relPathSkipJoin());
        return values;
    }

    private Object relPathSkipJoin() throws SVNException {
        String movedTo = getColumnString(SVNWCDbSchema.NODES__Fields.moved_to);
        if (movedTo == null) {
            return null;
        }
        String originalParentPath = String.valueOf(getBind(2));
        String movedParentPath = String.valueOf(getBind(3));
        String relativeMovedTo = SVNPathUtil.getRelativePath(originalParentPath, movedTo);
        String movedMovedTo = SVNPathUtil.append(movedParentPath, relativeMovedTo);
        return movedMovedTo;
    }

    @Override
    protected String getRowPath() throws SVNException {
        return getColumnString(SVNWCDbSchema.NODES__Fields.moved_to);
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
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }
}
