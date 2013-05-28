package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

public class SVNWCDbSelectLock extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectLock(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.LOCK);
    }

}
