package org.tmatesoft.svn.core.internal.wc2.remote;

import java.io.File;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc2.SvnRemoteSetProperty;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnRemoteSetPropertyImpl extends SvnRemoteOperationRunner<SVNCommitInfo, SvnRemoteSetProperty> implements ISVNPropertyHandler {
    
    @Override
    public boolean isApplicable(SvnRemoteSetProperty operation, SvnWcGeneration wcGeneration) throws SVNException {
        return true;
    }

    @Override
    protected SVNCommitInfo run() throws SVNException {
        //
        SVNWCClient16 wcClient = new SVNWCClient16(getOperation().getAuthenticationManager(), getOperation().getOptions());
        wcClient.setCommitHandler(SvnCodec.commitHandler(getOperation().getCommitHandler()));
        
        for (SvnTarget target : getOperation().getTargets()) {
            SVNURL url = target.getURL();
            if (url == null) {
                continue;
            }
            SVNCommitInfo info = wcClient.
                    doSetProperty(url, 
                            getOperation().getPropertyName(), 
                            getOperation().getPropertyValue(), 
                            getOperation().getBaseRevision(), 
                            getOperation().getCommitMessage(), 
                            getOperation().getRevisionProperties(), 
                            getOperation().isForce(),
                            null);
            if (info != null) {
                getOperation().receive(target, info);
            }
        }
        
        return getOperation().first();
    }

    public void handleProperty(File path, SVNPropertyData property) throws SVNException {
    }

    public void handleProperty(SVNURL url, SVNPropertyData property) throws SVNException {
        if (getOperation().getPropertyReceiver() != null) {
            getOperation().getPropertyReceiver().receive(SvnTarget.fromURL(url), property);
        }
    }

    public void handleProperty(long revision, SVNPropertyData property) throws SVNException {
    }

}
