package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetFileSize;

public class SvnRepositoryGetFileSizeImpl extends SvnRepositoryOperationRunner<Long, SvnRepositoryGetFileSize> {

    @Override
    protected Long run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(getOperation().getEventHandler());

        long fileSize;
        if (getOperation().getTransactionName() == null) {
            fileSize = lc.doGetFileSize(getOperation().getRepositoryRoot(), getOperation().getPath(), getOperation().getRevision());
        } else {
            fileSize =  lc.doGetFileSize(getOperation().getRepositoryRoot(), getOperation().getPath(), getOperation().getTransactionName());
        }
        return fileSize;
    }
}
