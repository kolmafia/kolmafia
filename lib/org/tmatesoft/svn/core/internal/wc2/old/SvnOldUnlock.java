package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUnlock;

public class SvnOldUnlock extends SvnOldRunner<SVNLock, SvnUnlock> {
    @Override
    protected SVNLock run() throws SVNException {
        
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        
        int i = 0;
        if (getOperation().hasRemoteTargets()) {
        	SVNURL[] urls = new SVNURL[getOperation().getTargets().size()];
            for (SvnTarget target : getOperation().getTargets()) {
            	urls[i++] = target.getURL();
            }
            
            client.doUnlock(urls, getOperation().isBreakLock());
        }
        else {
        	File[] paths = new File[getOperation().getTargets().size()];
            for (SvnTarget target : getOperation().getTargets()) {
            	paths[i++] = target.getFile();
            }
            
            client.doUnlock(paths, getOperation().isBreakLock());
        }
        
        return getOperation().first();
    }

}
