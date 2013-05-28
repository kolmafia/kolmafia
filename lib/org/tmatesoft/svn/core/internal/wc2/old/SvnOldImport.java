package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNCommitClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc2.SvnImport;

public class SvnOldImport extends SvnOldRunner<SVNCommitInfo, SvnImport> {

    public SvnWcGeneration getWcGeneration() {
        return SvnWcGeneration.NOT_DETECTED;
    }

    @Override
    public boolean isApplicable(SvnImport operation, SvnWcGeneration wcGeneration) throws SVNException {
        return true;
    }

    @Override
    protected SVNCommitInfo run() throws SVNException {
        SVNCommitClient16 client = new SVNCommitClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.setCommitHandler(SvnCodec.commitHandler(getOperation().getCommitHandler()));
        SVNCommitInfo info = client.doImport(getOperation().getSource(), 
                getOperation().getFirstTarget().getURL(), 
                getOperation().getCommitMessage(), 
                getOperation().getRevisionProperties(), 
                getOperation().isUseGlobalIgnores(), 
                getOperation().isForce(), 
                getOperation().getDepth());
        
        if (info != null) {
            getOperation().receive(getOperation().getFirstTarget(), info);
        }

        return info;
    }
}
