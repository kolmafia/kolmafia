package org.tmatesoft.svn.core.internal.wc2.remote;

import java.util.Collection;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc16.SVNCopyClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldRepositoryAccess;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNExternalsHandler;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRemoteCopy;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnNgReposToReposCopy extends SvnRemoteOperationRunner<SVNCommitInfo, SvnRemoteCopy>  {

    @Override
    public boolean isApplicable(SvnRemoteCopy operation, SvnWcGeneration wcGeneration) throws SVNException {
        return areAllSourcesRemote(operation) && !operation.getFirstTarget().isLocal();
    }
    
    private boolean areAllSourcesRemote(SvnRemoteCopy operation) {
        for(SvnCopySource source : operation.getSources()) {
            if (source.getSource().isURL() || 
                    (source.getRevision() != SVNRevision.WORKING && source.getRevision() != SVNRevision.UNDEFINED)) {
                continue;
            }
            return false;
        }
        return true;
    }
    
    @Override
    protected SVNCommitInfo run() throws SVNException {
        SVNCopyClient16 client = new SVNCopyClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.setExternalsHandler(ISVNExternalsHandler.DEFAULT);
        client.setOptions(getOperation().getOptions());
        client.setCommitHandler(SvnCodec.commitHandler(getOperation().getCommitHandler()));
        
        SvnTarget target = getOperation().getFirstTarget();
        SVNCopySource[] sources = new SVNCopySource[getOperation().getSources().size()];
        int i = 0;
        for (SvnCopySource newSource : getOperation().getSources()) {
            if (newSource.getSource().isFile()) {
                SvnWcGeneration wcGeneration = SvnOperationFactory.detectWcGeneration(newSource.getSource().getFile(), false);
                if (wcGeneration == SvnWcGeneration.V16) {
                    newSource = new SvnOldRepositoryAccess(getOperation()).createRemoteCopySource(getWcContext(), newSource);
                } else {
                    newSource = new SvnNgRepositoryAccess(getOperation(), getWcContext()).createRemoteCopySource(getWcContext(), newSource);
                }
            }            
            sources[i] = SvnCodec.copySource(newSource);
            i++;
        }

        SVNCommitInfo info = client.doCopy(sources, target.getURL(), getOperation().isMove(), getOperation().isMakeParents(), getOperation().isFailWhenDstExists(), 
                getOperation().getCommitMessage(), getOperation().getRevisionProperties());
        
        if (info != null) {
            Collection<SvnTarget> targets = getOperation().getTargets();
            if (targets != null && targets.size() != 0) {
                SvnTarget firstTarget = targets.iterator().next();

                SVNRepository repository = getRepositoryAccess().createRepository(firstTarget.getURL(), null, true);
                SVNURL repositoryRoot = repository.getRepositoryRoot(true);

                getOperation().receive(SvnTarget.fromURL(repositoryRoot), info);
            }
        }
        return info;

    }
}
