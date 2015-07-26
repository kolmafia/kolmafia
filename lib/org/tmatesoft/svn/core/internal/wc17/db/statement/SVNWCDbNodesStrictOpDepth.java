package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

public class SVNWCDbNodesStrictOpDepth extends SVNSqlJetSelectStatement {

    private long depth;

    public SVNWCDbNodesStrictOpDepth(SVNSqlJetDb sDb, long depth) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        this.depth = depth;
    }

    public boolean existsOpDepth(Long wcId, String localRelpath) throws SVNException {
        try {
            bindLong(1, wcId);
            bindString(2, localRelpath);
            bindLong(3, depth);

            return next();
        } finally {
            reset();
        }
    }
}
