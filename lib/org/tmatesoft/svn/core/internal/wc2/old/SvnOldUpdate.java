package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNUpdateClient16;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

public class SvnOldUpdate extends SvnOldRunner<long[], SvnUpdate> {

    @Override
    protected long[] run() throws SVNException {        
        SVNUpdateClient16 client = new SVNUpdateClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setIgnoreExternals(getOperation().isIgnoreExternals());
        client.setUpdateLocksOnDemand(getOperation().isUpdateLocksOnDemand());
        client.setEventHandler(getOperation().getEventHandler());
        client.setExternalsHandler(SvnCodec.externalsHandler(getOperation().getExternalsHandler()));
        
        File[] paths = new File[getOperation().getTargets().size()];
        int i = 0;
        for (SvnTarget tgt : getOperation().getTargets()) {
            paths[i++] = tgt.getFile();
        }
        
        return client.doUpdate(paths, getOperation().getRevision(), getOperation().getDepth(), getOperation().isAllowUnversionedObstructions(), getOperation().isDepthIsSticky());
    }
}
