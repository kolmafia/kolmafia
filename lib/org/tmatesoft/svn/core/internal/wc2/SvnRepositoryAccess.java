package org.tmatesoft.svn.core.internal.wc2;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeInfo;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNMergeDriver;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.io.ISVNLocationSegmentHandler;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.ISvnOperationOptionsProvider;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public abstract class SvnRepositoryAccess {
    
    private SVNWCContext context;
    private ISvnOperationOptionsProvider operationOptionsProvider;

    protected SvnRepositoryAccess(ISvnOperationOptionsProvider operationOptionsProvider, SVNWCContext context) throws SVNException {
        this.operationOptionsProvider = operationOptionsProvider;
        this.context = context;
    }
    
    protected ISvnOperationOptionsProvider getOperationOptionsProvider() {
        return this.operationOptionsProvider;
    }
    
    protected SVNWCContext getWCContext() {
        return this.context;
    }
    
    public enum RepositoryInfo {
        repository, revision, url;
    }
    
    public enum LocationsInfo {
        startUrl, startRevision, endUrl, endRevision;
    }
    
    public enum RevisionsPair {
        revNumber, youngestRevision;
    }
    
    public abstract SvnCopySource createRemoteCopySource(SVNWCContext context, SvnCopySource localCopySource) throws SVNException;
    
    public abstract Structure<RepositoryInfo> createRepositoryFor(SvnTarget target, SVNRevision revision, SVNRevision pegRevision, File baseDirectory) throws SVNException;    
   
    public abstract Structure<RevisionsPair> getRevisionNumber(SVNRepository repository, SvnTarget path, SVNRevision revision, Structure<RevisionsPair> youngestRevision) throws SVNException;
    
    public enum UrlInfo {
        url, pegRevision, dropRepsitory; 
    }
    
    public abstract Structure<UrlInfo> getURLFromPath(SvnTarget path, SVNRevision revision, SVNRepository repository) throws SVNException;

    
    protected SVNRevision[] resolveRevisions(SVNRevision pegRevision, SVNRevision revision, boolean isURL, boolean noticeLocalModifications) {
        if (!pegRevision.isValid()) {
            if (isURL) {
                pegRevision = SVNRevision.HEAD;
            } else {
                if (noticeLocalModifications) {
                    pegRevision = SVNRevision.WORKING;
                } else {
                    pegRevision = SVNRevision.BASE;
                }
            }
        }
        if (!revision.isValid()) {
            revision = pegRevision;
        }
        return new SVNRevision[] {
                pegRevision, revision
        };
    }

    public SVNRepository createRepository(SVNURL url, String expectedUuid, boolean mayReuse) throws SVNException {
        SVNRepository repository = null;
        if (getOperationOptionsProvider().getRepositoryPool() == null) {
            repository = SVNRepositoryFactory.create(url, null);
            repository.setAuthenticationManager(getOperationOptionsProvider().getAuthenticationManager());
        } else {
            repository = getOperationOptionsProvider().getRepositoryPool().createRepository(url, mayReuse);
        }
        if (expectedUuid != null) {
            String reposUUID = repository.getRepositoryUUID(true);
            if (!expectedUuid.equals(reposUUID)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_UUID_MISMATCH, "Repository UUID ''{0}'' doesn''t match expected UUID ''{1}''", new Object[] {
                        reposUUID, expectedUuid
                });
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        repository.setCanceller(getOperationOptionsProvider().getCanceller());
        return repository;
    }

    public Structure<LocationsInfo> getLocations(SVNRepository repository, SvnTarget path, SVNRevision revision, SVNRevision start, SVNRevision end) throws SVNException {
        if (!revision.isValid() || !start.isValid()) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION), SVNLogType.DEFAULT);
        }
        
        long pegRevisionNumber = -1;
        long startRevisionNumber;
        long endRevisionNumber;
        SVNURL url = null;
        
        if (path.isFile()) {
            Structure<UrlInfo> urlInfo = getURLFromPath(path, revision, repository);
            if (urlInfo.hasValue(UrlInfo.dropRepsitory) && urlInfo.is(UrlInfo.dropRepsitory)) {
                repository = null;
            }
            url = urlInfo.<SVNURL>get(UrlInfo.url);
            if (urlInfo.hasValue(UrlInfo.pegRevision)) {
                pegRevisionNumber = urlInfo.lng(UrlInfo.pegRevision);
            }
            urlInfo.release();
        } else {
            url = path.getURL();
        }
        
        if (repository == null) {
            repository = createRepository(url, null, true);
        }
    
        Structure<RevisionsPair> pair = null;
        if (pegRevisionNumber < 0) {
            pair = getRevisionNumber(repository, path, revision, pair);
            pegRevisionNumber = pair.lng(RevisionsPair.revNumber);
        }
        pair = getRevisionNumber(repository, path, start, pair);
        startRevisionNumber = pair.lng(RevisionsPair.revNumber);
        if (end == SVNRevision.UNDEFINED) {
            endRevisionNumber = startRevisionNumber;
        } else {
            pair = getRevisionNumber(repository, path, end, pair);
            endRevisionNumber = pair.lng(RevisionsPair.revNumber);
        }
        pair.release();
        
        Structure<LocationsInfo> result = Structure.obtain(LocationsInfo.class);
        result.set(LocationsInfo.startRevision, startRevisionNumber);
        if (end != SVNRevision.UNDEFINED) {
            result.set(LocationsInfo.startRevision, endRevisionNumber);
        }
        if (startRevisionNumber == pegRevisionNumber && endRevisionNumber == pegRevisionNumber) {
            result.set(LocationsInfo.startUrl, url);
            if (end != SVNRevision.UNDEFINED) {
                result.set(LocationsInfo.endUrl, url);
            }
            return result;
        }
        
        SVNURL repositoryRootURL = repository.getRepositoryRoot(true);
        long[] revisionsRange = startRevisionNumber == endRevisionNumber ? 
                new long[] {startRevisionNumber} : new long[] {startRevisionNumber, endRevisionNumber};
                
        Map<?,?> locations = repository.getLocations("", (Map<?,?>) null, pegRevisionNumber, revisionsRange);
        
        SVNLocationEntry startPath = (SVNLocationEntry) locations.get(new Long(startRevisionNumber));
        SVNLocationEntry endPath = (SVNLocationEntry) locations.get(new Long(endRevisionNumber));
        if (startPath == null) {
            Object source = path != null ? (Object) path : (Object) url;
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_UNRELATED_RESOURCES, "Unable to find repository location for ''{0}'' in revision ''{1}''", new Object[] {
                    source, new Long(startRevisionNumber)
            });
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (endPath == null) {
            Object source = path != null ? (Object) path : (Object) url;
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_UNRELATED_RESOURCES, "The location for ''{0}'' for revision {1} does not exist in the "
                    + "repository or refers to an unrelated object", new Object[] {
                    source, new Long(endRevisionNumber)
            });
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        result.set(LocationsInfo.startUrl, repositoryRootURL.appendPath(startPath.getPath(), false));
        if (end.isValid()) {
            result.set(LocationsInfo.endUrl, repositoryRootURL.appendPath(endPath.getPath(), false));
        }
        return result;
    }

    public Map<String, SVNMergeRangeList> getReposMergeInfo(SVNRepository repository, String path, long revision, SVNMergeInfoInheritance inheritance, boolean squelchIncapable) throws SVNException {
        Map<String, SVNMergeInfo> reposMergeInfo = null;
        try {
            reposMergeInfo = repository.getMergeInfo(new String[] {path}, revision, inheritance, false);
        } catch (SVNException svne) {
            if (!squelchIncapable || svne.getErrorMessage().getErrorCode() != SVNErrorCode.UNSUPPORTED_FEATURE) {
                throw svne;
            }
        }
        String rootRelativePath = getPathRelativeToRoot(repository.getLocation(), repository.getRepositoryRoot(false), repository);
        Map<String, SVNMergeRangeList> targetMergeInfo = null;
        if (reposMergeInfo != null) {
            SVNMergeInfo mergeInfo = (SVNMergeInfo) reposMergeInfo.get(rootRelativePath);
            if (mergeInfo != null) {
                targetMergeInfo = mergeInfo.getMergeSourcesToMergeLists();
            }
        }
        return targetMergeInfo;
    }
    
    protected String getPathRelativeToRoot(SVNURL url, SVNURL reposRootURL, SVNRepository repos) throws SVNException {
        if (reposRootURL == null) {
            reposRootURL = repos.getRepositoryRoot(true);
        }
        String reposRootPath = reposRootURL.getPath();
        String absPath = url.getPath();
        if (!absPath.startsWith(reposRootPath)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_UNRELATED_RESOURCES, "URL ''{0}'' is not a child of repository root URL ''{1}''", new Object[] {
                    url, reposRootURL
            });
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        absPath = absPath.substring(reposRootPath.length());
        if (!absPath.startsWith("/")) {
            absPath = "/" + absPath;
        }
        return absPath;
    }

    public String getPathRelativeToSession(SVNURL url, SVNURL sessionURL, SVNRepository repos) {
        if (sessionURL == null) {
            sessionURL = repos.getLocation();
        }
        String reposPath = sessionURL.getPath();
        String absPath = url.getPath();
        if (!absPath.startsWith(reposPath + "/") && !absPath.equals(reposPath)) {
            return null;
        }
        absPath = absPath.substring(reposPath.length());
        if (absPath.startsWith("/")) {
            absPath = absPath.substring(1);
        }
        return absPath;
    }
    
    public SVNLocationSegment getYoungestCommonAncestor(SVNURL url1, long rev1, SVNURL url2, long rev2) throws SVNException {
        boolean[] hasZero1 = new boolean[1];
        boolean[] hasZero2 = new boolean[1];
        Map<String, SVNMergeRangeList> history1 = getHistoryAsMergeInfo(url1, SVNRevision.create(rev1), -1, -1, hasZero1, null);
        Map<String, SVNMergeRangeList> history2 = getHistoryAsMergeInfo(url2, SVNRevision.create(rev2), -1, -1, hasZero2, null);
        long ycRevision = -1;
        String ycPath = null;
        for (Iterator<String> paths = history1.keySet().iterator(); paths.hasNext();) {
            String path = paths.next();
            SVNMergeRangeList ranges1 = history1.get(path);
            SVNMergeRangeList ranges2 = history2.get(path);
            if (ranges2 != null) {
                SVNMergeRangeList intersection = ranges1.intersect(ranges2, true);
                if (intersection != null && !intersection.isEmpty()) {
                    SVNMergeRange ycRange = intersection.getRanges()[intersection.getSize() - 1];
                    if (ycRevision < 0 || ycRange.getEndRevision() > ycRevision) {
                        ycRevision = ycRange.getEndRevision();
                        ycPath = path.substring(1);
                    }
                }
            }
        }
        if (ycPath == null && hasZero1[0] && hasZero2[0]) {
            ycPath = "/";
            ycRevision = 0;
        }
        return new SVNLocationSegment(ycRevision, ycRevision, ycPath);
    }
    
    private Map<String, SVNMergeRangeList> getHistoryAsMergeInfo(SVNURL url, SVNRevision pegRevision, long rangeYoungest, long rangeOldest, boolean[] hasZero, SVNRepository repos) throws SVNException {
        long[] pegRevNum = new long[1];
        Structure<RevisionsPair> pair = getRevisionNumber(repos, SvnTarget.fromURL(url), pegRevision, null);
        pegRevNum[0] = pair.lng(RevisionsPair.revNumber);
        pair.release();
        
        boolean closeSession = false;
        try {
            if (repos == null) {
                repos = createRepository(url, null, false);
                closeSession = true;
            }
            if (!SVNRevision.isValidRevisionNumber(rangeYoungest)) {
                rangeYoungest = pegRevNum[0];
            }
            if (!SVNRevision.isValidRevisionNumber(rangeOldest)) {
                rangeOldest = 0;
            }
            
            List<SVNLocationSegment> segments = repos.getLocationSegments("", pegRevNum[0], rangeYoungest, rangeOldest);
            if (!segments.isEmpty() && hasZero != null && hasZero.length > 0) {
                SVNLocationSegment oldest = segments.get(0);
                hasZero[0] = oldest.getStartRevision() == 0;
            }
            return getMergeInfoFromSegments(segments);
        } finally {
            if (closeSession) {
                repos.closeSession();
            }
        }
    }

    public static Map<String, SVNMergeRangeList> getMergeInfoFromSegments(Collection<SVNLocationSegment> segments) {
        Map<String, Collection<SVNMergeRange>> mergeInfo = new TreeMap<String, Collection<SVNMergeRange>>();
        for (Iterator<SVNLocationSegment> segmentsIter = segments.iterator(); segmentsIter.hasNext();) {
            SVNLocationSegment segment = (SVNLocationSegment) segmentsIter.next();
            if (segment.getPath() == null) {
                continue;
            }
            String sourcePath = segment.getPath();
            Collection<SVNMergeRange> pathRanges = mergeInfo.get(sourcePath);
            if (pathRanges == null) {
                pathRanges = new LinkedList<SVNMergeRange>();
                mergeInfo.put(sourcePath, pathRanges);
            }
            SVNMergeRange range = new SVNMergeRange(Math.max(segment.getStartRevision() - 1, 0), 
                    segment.getEndRevision(), true);
            pathRanges.add(range);
        }
        Map<String, SVNMergeRangeList> result = new TreeMap<String, SVNMergeRangeList>();
        for (Iterator<String> paths = mergeInfo.keySet().iterator(); paths.hasNext();) {
            String path = (String) paths.next();
            Collection<SVNMergeRange> pathRanges = mergeInfo.get(path);
            result.put(path, SVNMergeRangeList.fromCollection(pathRanges));
        }
        return result;
    }

    public SVNLocationEntry getCopySource(SvnTarget target, SVNRevision revision) throws SVNException {
        Structure<RepositoryInfo> repositoryInfo = createRepositoryFor(target, revision, revision, null);
        SVNRepository repository = repositoryInfo.get(RepositoryInfo.repository);
        long atRev = repositoryInfo.lng(RepositoryInfo.revision);
        repositoryInfo.release();
        
        final Object[] copyFrom = new Object[3];
        try {
            repository.getLocationSegments("", atRev, atRev, -1, new ISVNLocationSegmentHandler() {
                public void handleLocationSegment(SVNLocationSegment locationSegment) throws SVNException {
                    // skip first.
                    if (copyFrom[0] == null) {
                        copyFrom[0] = Boolean.TRUE;
                    } else if (copyFrom[1] != null) {
                        return;
                    } else if (locationSegment.getPath() != null) {
                        copyFrom[1] = locationSegment.getPath();
                        copyFrom[2] = locationSegment.getEndRevision();
                    }
                }
            });
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND ||
                    e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_DAV_REQUEST_FAILED) {
                return new SVNLocationEntry(-1, null);
            } else {
                throw e;
            }
        }
        if (copyFrom[1] != null) {
            return new SVNLocationEntry((Long) copyFrom[2], (String) copyFrom[1]);
        }
        return null;
    }

    public Map<String, SVNMergeRangeList> getHistoryAsMergeInfo(SVNRepository repos, SvnTarget target, long youngest, long oldest) throws SVNException {
        long pegRevnum = -1;
        if (repos == null) {
            Structure<RepositoryInfo> reposInfo = createRepositoryFor(target, SVNRevision.UNDEFINED, target.getResolvedPegRevision(), null);
            pegRevnum = reposInfo.lng(RepositoryInfo.revision);
            repos = reposInfo.get(RepositoryInfo.repository);
            reposInfo.release();
        } else {
            if (target.getPegRevision() == SVNRevision.HEAD || target.getPegRevision() == SVNRevision.UNDEFINED) {
                pegRevnum = repos.getLatestRevision();
            } else if (target.getPegRevision().getNumber() >= 0) {
                pegRevnum = target.getPegRevision().getNumber();
            } else if (target.getPegRevision().getDate() != null) {
                pegRevnum = repos.getDatedRevision(target.getPegRevision().getDate());
            } else {
                if (target.isURL()) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_VERSIONED_PATH_REQUIRED);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                Structure<RevisionsPair> pair = getRevisionNumber(repos, target, target.getPegRevision(), null);
                pegRevnum = pair.lng(RevisionsPair.revNumber);
                pair.release();
            }
        }
        
        if (youngest < 0) {
            youngest = pegRevnum;
        }
        if (oldest < 0) {
            oldest = 0;
        }
        List<SVNLocationSegment> segments = repos.getLocationSegments("", pegRevnum, youngest, oldest);
        return SVNMergeDriver.getMergeInfoFromSegments(segments);
    }
}
