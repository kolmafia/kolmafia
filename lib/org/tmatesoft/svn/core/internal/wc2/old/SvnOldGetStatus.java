package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNStatusClient16;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNDebugLog;

public class SvnOldGetStatus extends SvnOldRunner<SvnStatus, SvnGetStatus> implements ISVNStatusHandler {

    @Override
    protected SvnStatus run() throws SVNException {        
        SVNStatusClient16 client = new SVNStatusClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setFilesProvider(SvnCodec.fileListProvider(getOperation().getFileListHook()));
        client.setIgnoreExternals(!getOperation().isReportExternals());
        client.setEventHandler(getOperation().getEventHandler());
        client.setDebugLog(SVNDebugLog.getDefaultLog());
        long revision = client.doStatus(getFirstTarget(),
                getOperation().getRevision(),
                getOperation().getDepth(),
                getOperation().isRemote(),
                getOperation().isReportAll(),
                getOperation().isReportIgnored(),
                getOperation().isCollectParentExternals(),
                this,
                getOperation().getApplicableChangelists());
        getOperation().setRemoteRevision(revision);
        
        return getOperation().first();
    }

    public void handleStatus(SVNStatus status) throws SVNException {
        getOperation().receive(SvnTarget.fromFile(status.getFile()), SvnCodec.status(status));
    }
}
