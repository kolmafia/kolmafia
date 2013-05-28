package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNCopyClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.ISVNExternalsHandler;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRemoteCopy;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldRemoteCopy extends SvnOldRunner<SVNCommitInfo, SvnRemoteCopy> {
    
    @Override
    public boolean isApplicable(SvnRemoteCopy operation, SvnWcGeneration wcGeneration) throws SVNException {
        if (operation.getFirstTarget().isURL()) {
            for (SvnCopySource source : operation.getSources()) {
                if (source.getSource().getFile() != null) {
                    SvnWcGeneration sourceFormat = SvnOperationFactory.detectWcGeneration(source.getSource().getFile(), false);
                    if (sourceFormat != SvnWcGeneration.V16) {
                        return false;
                    }                
                }
            }
            // copy from old_wc[@rev] to url
            return true;
        }
        return false;
    }

    @Override
    protected SVNCommitInfo run() throws SVNException {
        SVNCopyClient16 client = new SVNCopyClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.setOptions(getOperation().getOptions());
        client.setCommitHandler(SvnCodec.commitHandler(getOperation().getCommitHandler()));
        client.setExternalsHandler(SvnCodec.externalsHandler(getOperation().getExternalsHandler()));
        client.setDisableLocalModificationCopying(getOperation().isDisableLocalModifications());
        client.setCommitParameters(SvnCodec.commitParameters(getOperation().getCommitParameters()));
        
        SvnTarget target = getOperation().getFirstTarget();
        SVNCopySource[] sources = new SVNCopySource[getOperation().getSources().size()];
        int i = 0;
        for (SvnCopySource newSource : getOperation().getSources()) {
            sources[i] = SvnCodec.copySource(newSource);
            i++;
        }

        SVNCommitInfo info = client.doCopy(sources, target.getURL(), getOperation().isMove(), getOperation().isMakeParents(), getOperation().isFailWhenDstExists(), 
                getOperation().getCommitMessage(), getOperation().getRevisionProperties());
        if (info != null) {
            getOperation().receive(getOperation().getFirstTarget(), info);
        }
        return info;

    }
}
