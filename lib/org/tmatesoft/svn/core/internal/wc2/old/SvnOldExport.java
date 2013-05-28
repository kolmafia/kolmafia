package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNUpdateClient16;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc2.SvnExport;

public class SvnOldExport extends SvnOldRunner<Long, SvnExport> {

    @Override
    protected Long run() throws SVNException {
        SVNUpdateClient16 client = new SVNUpdateClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        
        client.setIgnoreExternals(getOperation().isIgnoreExternals());
        client.setUpdateLocksOnDemand(getOperation().isUpdateLocksOnDemand());
        client.setEventHandler(getOperation().getEventHandler());
        client.setExportExpandsKeywords(getOperation().isExpandKeywords());
        client.setExternalsHandler(SvnCodec.externalsHandler(getOperation().getExternalsHandler()));
        
        return client.doExport(
                getOperation().getSource().getFile(), 
                getOperation().getFirstTarget().getFile(), 
                getOperation().getSource().getResolvedPegRevision(), 
                getOperation().getRevision(), 
                getOperation().getEolStyle(),
                getOperation().isForce(),
                getOperation().getDepth());
    }
}
