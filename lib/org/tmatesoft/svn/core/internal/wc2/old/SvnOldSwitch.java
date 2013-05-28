package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNUpdateClient16;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc2.SvnSwitch;

public class SvnOldSwitch extends SvnOldRunner<Long, SvnSwitch> {

    @Override
    protected Long run() throws SVNException {        
        SVNUpdateClient16 client = new SVNUpdateClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setIgnoreExternals(getOperation().isIgnoreExternals());
        client.setUpdateLocksOnDemand(getOperation().isUpdateLocksOnDemand());
        client.setEventHandler(getOperation().getEventHandler());
        client.setExternalsHandler(SvnCodec.externalsHandler(getOperation().getExternalsHandler()));
        
        return client.doSwitch(getFirstTarget(), 
                getOperation().getSwitchTarget().getURL(), 
                getOperation().getSwitchTarget().getResolvedPegRevision(), 
                getOperation().getRevision(), 
                getOperation().getDepth(), 
                getOperation().isAllowUnversionedObstructions(),
                getOperation().isDepthIsSticky());
    }
}
