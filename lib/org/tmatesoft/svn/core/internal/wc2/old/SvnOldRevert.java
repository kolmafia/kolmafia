package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.wc2.SvnRevert;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldRevert extends SvnOldRunner<Void, SvnRevert> {

    @Override
    protected Void run() throws SVNException {
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.setRevertMissingDirectories(getOperation().isRevertMissingDirectories());
        
        File[] paths = new File[getOperation().getTargets().size()];
        int i = 0;
        for (SvnTarget target : getOperation().getTargets()) {
            paths[i++] = target.getFile();
        }
        client.doRevert(paths, getOperation().getDepth(), getOperation().getApplicableChangelists());
        
        return null;
    }

}
