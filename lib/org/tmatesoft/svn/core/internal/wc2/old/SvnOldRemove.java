package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc2.SvnScheduleForRemoval;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldRemove extends SvnOldRunner<Void, SvnScheduleForRemoval> {

    @Override
    protected Void run() throws SVNException {
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.setAddParameters(SVNWCClient.DEFAULT_ADD_PARAMETERS);
        
        for (SvnTarget target : getOperation().getTargets()) {
            client.doDelete(target.getFile(), 
                    getOperation().isForce(), 
                    getOperation().isDeleteFiles(), 
                    getOperation().isDryRun());
        }
        return null;
    }

}
