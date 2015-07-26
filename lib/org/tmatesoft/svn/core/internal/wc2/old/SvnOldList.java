package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNLogClient16;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteList;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldList extends SvnOldRunner<SVNDirEntry, SvnList> implements ISVNDirEntryHandler {

    @Override
    protected SVNDirEntry run() throws SVNException {
        SVNLogClient16 client = new SVNLogClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.doList(getFirstTarget(), getOperation().getFirstTarget().getPegRevision(), getOperation().getRevision(), getOperation().isFetchLocks(), getOperation().getDepth(), SVNDirEntry.DIRENT_ALL, this);
        return getOperation().first();
    }

    public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
        getOperation().receive(SvnTarget.fromURL(dirEntry.getURL()), dirEntry);
    }
}
