package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;

/**

 */
public class SVNWCDbSelectCommittableExternalsBelow extends SVNWCDbSelectCommittableExternalsImmediatelyBelow {
    public SVNWCDbSelectCommittableExternalsBelow(SVNSqlJetDb sDb) throws SVNException {
        super(sDb);
    }

    @Override
    protected boolean checkForImmediate() throws SVNException {
        return true;
    }
}
