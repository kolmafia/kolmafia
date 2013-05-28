package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.List;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergeDriver.MergeSource;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc2.SvnMerge;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgMergePegged extends SvnNgOperationRunner<Void, SvnMerge> {
    
    @Override
    public boolean isApplicable(SvnMerge operation, SvnWcGeneration wcGeneration) throws SVNException {
        return super.isApplicable(operation, wcGeneration) 
                && !operation.isReintegrate() 
                && operation.getSource() != null
                && operation.getRevisionRanges() != null;
    }

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        if (getOperation().getRevisionRanges() == null || getOperation().getRevisionRanges().isEmpty()) {
            return null;
        }
        File lockPath = getLockPath(getFirstTarget());
        if (getOperation().isDryRun()) {
            merge(getFirstTarget());
        } else {
            try {
                lockPath = context.acquireWriteLock(lockPath, false, true);                
                merge(getFirstTarget());
            } finally {
                context.releaseWriteLock(lockPath);
                sleepForTimestamp();
            }
            
        }
        return null;
    }
    
    private File getLockPath(File firstTarget) throws SVNException {
        SVNNodeKind kind = getWcContext().readKind(firstTarget, false);
        if (kind == SVNNodeKind.DIR) {
            return firstTarget;
        } else {
            return SVNFileUtil.getParentFile(firstTarget);
        }
    }

    private void merge(File target) throws SVNException {
        SVNFileType ft = SVNFileType.getType(target);
        if (ft == SVNFileType.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "Path ''{0}'' does not exist", target);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNURL url = getRepositoryAccess().getTargetURL(getOperation().getSource());
        if (url == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", getOperation().getSource());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNNodeKind targetKind = getWcContext().readKind(target, false);
        if (targetKind != SVNNodeKind.DIR && targetKind != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Merge target ''{0}'' does not exist in the working copy", target);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SvnNgMergeDriver mergeDriver = new SvnNgMergeDriver(getWcContext(), getOperation(), getRepositoryAccess(), getOperation().getMergeOptions());
        mergeDriver.ensureWcIsSuitableForMerge(target, getOperation().isAllowMixedRevisions(), true, true);
        SVNURL wcReposRootURL = getWcContext().getNodeReposInfo(target).reposRootUrl;
        
        boolean sameRepos = true;

        SVNRepository repos = getRepositoryAccess().createRepository(url, null, false);
        List<MergeSource> sources = null;
        try {
            SVNURL sourceRootURL = repos.getRepositoryRoot(true);
            
            sources = mergeDriver.normalizeMergeSources(getOperation().getSource(), 
                    url, sourceRootURL, 
                    getOperation().getSource().getResolvedPegRevision(), 
                    SvnCodec.oldRevisionRanges(getOperation().getRevisionRanges()), 
                    repos);
            //
            if (!wcReposRootURL.equals(sourceRootURL)) {
                String targetUuid = getWcContext().getNodeReposInfo(target).reposUuid;
                String sourceUuid = repos.getRepositoryUUID(true);
                sameRepos = targetUuid.equals(sourceUuid);
            }
        } finally {
            repos.closeSession();
        }
        mergeDriver.doMerge(null, sources, target, 
                true, 
                true, sameRepos, getOperation().isIgnoreAncestry(), getOperation().isForce(), 
                getOperation().isDryRun(), 
                getOperation().isRecordOnly(), 
                null, false, false, getOperation().getDepth(), getOperation().getMergeOptions());
    }
    
    
}
