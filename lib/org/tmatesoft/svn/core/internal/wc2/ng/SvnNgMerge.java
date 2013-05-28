package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RevisionsPair;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergeDriver.MergeSource;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnMerge;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgMerge extends SvnNgOperationRunner<Void, SvnMerge> {
    
    @Override
    public boolean isApplicable(SvnMerge operation, SvnWcGeneration wcGeneration) throws SVNException {
        return super.isApplicable(operation, wcGeneration) 
                && !operation.isReintegrate() 
                && operation.getSource() == null
                && operation.getRevisionRanges() == null
                && operation.getFirstSource() != null
                && operation.getSecondSource() != null;
    }

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
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
        SvnTarget source1 = getOperation().getFirstSource();
        SvnTarget source2 = getOperation().getSecondSource();
        
        if (source1.getResolvedPegRevision() == SVNRevision.UNDEFINED || source2.getResolvedPegRevision() == SVNRevision.UNDEFINED) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Not all revisions are specified");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (source1.isURL() != source2.isURL()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, 
                    "Merge sources must both be either paths or URLs");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNURL url1 = getRepositoryAccess().getTargetURL(source1);
        if (url1 == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", source1.getFile());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNURL url2 = getRepositoryAccess().getTargetURL(source2);
        if (url2 == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", source2.getFile());
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
        SVNRepository repos1 = getRepositoryAccess().createRepository(url1, null, false);
        SVNRepository repos2 = getRepositoryAccess().createRepository(url2, null, false);
        try {
            Structure<RevisionsPair> pair = getRepositoryAccess().getRevisionNumber(repos1, SvnTarget.fromURL(url1), source1.getResolvedPegRevision(), null);
            long rev1 = pair.lng(RevisionsPair.revNumber);
            pair.release();
            pair = getRepositoryAccess().getRevisionNumber(repos2, SvnTarget.fromURL(url2), source2.getResolvedPegRevision(), null);
            long rev2 = pair.lng(RevisionsPair.revNumber);
            pair.release();
            
            String uuid1 = repos1.getRepositoryUUID(true);
            String uuid2 = repos2.getRepositoryUUID(true);
            if (!uuid1.equals(uuid2)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_UUID_MISMATCH, "''{0}'' isn''t in the same repository as ''{1}''", url1, url2);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            boolean sameRepos = true;
            SVNURL sourceReposRootURL = repos1.getRepositoryRoot(true);
            if (!wcReposRootURL.equals(sourceReposRootURL)) {
                String targetUuid = getWcContext().getNodeReposInfo(target).reposUuid;
                sameRepos = targetUuid.equals(uuid1);
            }
            SVNLocationSegment yc = null;
            if (!getOperation().isIgnoreAncestry()) {
                yc = getRepositoryAccess().getYoungestCommonAncestor(url1, rev1, url2, rev2);
            }
            List<MergeSource> sources = new ArrayList<SvnNgMergeDriver.MergeSource>();
            boolean sourcesAncestral = false;
            boolean sourcesRelated = false;
            
            if (yc != null && yc.getPath() != null && yc.getStartRevision() >= 0) {
                sourcesRelated = true;
                SVNURL ycURL = sourceReposRootURL.appendPath(yc.getPath(), false);
                if (url2.equals(ycURL) && yc.getStartRevision() == rev2) {
                    sourcesAncestral = true;
                    SVNRevisionRange range = new SVNRevisionRange(SVNRevision.create(rev1), SVNRevision.create(yc.getStartRevision()));
                    Collection<SVNRevisionRange> ranges = new ArrayList<SVNRevisionRange>();
                    ranges.add(range);
                    sources = mergeDriver.normalizeMergeSources(SvnTarget.fromURL(url1), url1, sourceReposRootURL, 
                            SVNRevision.create(rev1), ranges, repos1);
                } else if (url1.equals(ycURL) && yc.getStartRevision() == rev1) {
                    sourcesAncestral = true;
                    SVNRevisionRange range = new SVNRevisionRange(SVNRevision.create(yc.getStartRevision()), SVNRevision.create(rev2));
                    Collection<SVNRevisionRange> ranges = new ArrayList<SVNRevisionRange>();
                    ranges.add(range);
                    sources = mergeDriver.normalizeMergeSources(SvnTarget.fromURL(url2), url2, sourceReposRootURL, 
                            SVNRevision.create(rev2), ranges, repos2);
                } else {
                    mergeDriver.mergeCousinsAndSupplementMergeInfo(target, 
                            repos1, 
                            repos2, 
                            url1, rev1, url2, rev2, 
                            yc.getStartRevision(), 
                            sourceReposRootURL, wcReposRootURL, 
                            getOperation().getDepth(), 
                            getOperation().isIgnoreAncestry(), 
                            getOperation().isForce(), 
                            getOperation().isRecordOnly(), 
                            getOperation().isDryRun());
                }
            } else {
                MergeSource source = new MergeSource();
                source.rev1 = rev1;
                source.rev2 = rev2;
                source.url1 = url1;
                source.url2 = url2;
                sources.add(source);
            }
            
            mergeDriver.doMerge(null, sources, target, 
                    sourcesAncestral, 
                    sourcesRelated, 
                    sameRepos, 
                    getOperation().isIgnoreAncestry(), 
                    getOperation().isForce(), 
                    getOperation().isDryRun(), 
                    getOperation().isRecordOnly(), null, 
                    false, 
                    false, 
                    getOperation().getDepth(), 
                    getOperation().getMergeOptions());
        } finally {
            repos1.closeSession();
            repos2.closeSession();
        }
    }
    
    
}
