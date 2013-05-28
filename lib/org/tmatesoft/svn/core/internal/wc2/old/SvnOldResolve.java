package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.wc2.SvnResolve;

public class SvnOldResolve extends SvnOldRunner<Void, SvnResolve> {

    @Override
    protected Void run() throws SVNException {
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.doResolve(
        		getOperation().getFirstTarget().getFile(), 
        		getOperation().getDepth(), 
        		getOperation().isResolveContents(),
        		getOperation().isResolveProperties(),
        		getOperation().isResolveTree(),
        		getOperation().getConflictChoice());

        return null;
    }

}
