package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.Arrays;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * SELECT local_relpath, repos_path FROM nodes
 * WHERE wc_id = ?1
 * AND local_relpath = ?2
 * AND op_depth = 0
 * AND (inherited_props not null)
 *
 */
public class SVNWCDbSelectIPropsNode extends SVNSqlJetSelectStatement {
    
    private SVNDepth depth;

    public SVNWCDbSelectIPropsNode(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        setDepth(SVNDepth.EMPTY);
    }
    
    public void setDepth(SVNDepth depth) {
        if (depth == SVNDepth.UNKNOWN) {
            depth = SVNDepth.INFINITY;
        }
        this.depth = depth;
    }

    @Override
    protected String getIndexName() {
        if (depth == SVNDepth.IMMEDIATES || depth == SVNDepth.FILES) {
            return SVNWCDbSchema.NODES__Indices.I_NODES_PARENT.toString();
        }
        return super.getIndexName();
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        if (depth == SVNDepth.EMPTY || depth == SVNDepth.IMMEDIATES || depth == SVNDepth.FILES) {
            return new Object[] {getBind(1), getBind(2)};
        }
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        if (getColumnLong(NODES__Fields.op_depth) != 0) {
            return false;
        }
        final byte[] blob = getColumnBlob(SVNWCDbSchema.NODES__Fields.inherited_props);
        return blob != null;// && !Arrays.equals(SvnWcDbShared.EMPTY_PROPS_BLOB, blob);
    }

    @Override
    protected String getPathScope() {
        if (depth == SVNDepth.INFINITY) {
            return (String) getBind(2);
        }
        return null;
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }
    
    
}
