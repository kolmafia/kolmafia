package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.OutputStream;
import java.util.*;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNCancellableEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.*;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.ConflictInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.PropDiffs;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.ExternalNodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.LocationsInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RevisionsPair;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergeinfoUtil.SvnMergeInfoCatalogInfo;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergeinfoUtil.SvnMergeInfoInfo;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNCapability;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.*;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgMergeDriver {

    private static final Comparator<? super File> PATH_COMPARATOR = new Comparator<File>() {
        public int compare(File file1, File file2) {
            return SVNPathUtil.PATH_COMPARATOR.compare(SVNFileUtil.getFilePath(file1), SVNFileUtil.getFilePath(file2));
        }
    };

    public static MergePath findNearestAncestorWithIntersectingRanges(long[] revisions, Map<File, MergePath> childrenWithMergeInfo, boolean pathIsOwnAncestor, File localAbsPath) {
        MergePath nearestAncestor = null;

        revisions[0] = -1;
        revisions[1] = -1;

        assert childrenWithMergeInfo != null;

        List<Map.Entry<File, MergePath>> entries = new ArrayList<Map.Entry<File, MergePath>>(childrenWithMergeInfo.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            final Map.Entry<File, MergePath> entry = entries.get(i);
            MergePath child = entry.getValue();

            if (SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(child.absPath), SVNFileUtil.getFilePath(localAbsPath)) && (pathIsOwnAncestor || !child.absPath.equals(localAbsPath))) {
                if (nearestAncestor == null) {
                    nearestAncestor = child;
                    if (child.remainingRanges != null && child.remainingRanges.getSize() > 0) {
                        SVNMergeRange r1 = child.remainingRanges.getRanges()[0];
                        revisions[0] = r1.getStartRevision();
                        revisions[1] = r1.getEndRevision();
                    } else {
                        revisions[0] = -1;
                        revisions[1] = -1;
                        break;
                    }
                } else {
                    SVNMergeRange r1 = nearestAncestor.remainingRanges.getSize() == 0 ? null : nearestAncestor.remainingRanges.getRanges()[0];
                    SVNMergeRange r2 = child.remainingRanges.getSize() == 0 ? null : child.remainingRanges.getRanges()[0];

                    if (r1 != null && r2 != null) {
                        SVNMergeRange range1 = new SVNMergeRange(-1, -1, true);
                        SVNMergeRange range2 = new SVNMergeRange(-1, -1, true);
                        boolean reverseMerge = r1.getStartRevision() > r2.getEndRevision();
                        if (reverseMerge) {
                            range1.setStartRevision(r1.getEndRevision());
                            range1.setEndRevision(r1.getStartRevision());
                            range2.setStartRevision(r2.getEndRevision());
                            range2.setEndRevision(r2.getStartRevision());
                        } else {
                            range1.setStartRevision(r1.getStartRevision());
                            range1.setEndRevision(r1.getEndRevision());
                            range2.setStartRevision(r2.getStartRevision());
                            range2.setEndRevision(r2.getEndRevision());
                        }

                        if (range1.getStartRevision() < range2.getEndRevision() && range2.getStartRevision() < range1.getEndRevision()) {
                            revisions[0] = reverseMerge ? Math.max(r1.getStartRevision(), r2.getStartRevision()) : Math.min(r1.getStartRevision(), r2.getStartRevision());
                            revisions[1] = reverseMerge ? Math.max(r1.getEndRevision(), r2.getEndRevision()) : Math.min(r1.getEndRevision(), r2.getEndRevision());
                            nearestAncestor = child;
                        }
                    }
                }
            }
        }
        return nearestAncestor;
    }

    public static void makeMergeConflictError(SvnConflictReport report) throws SVNException {
        assert report == null || SVNFileUtil.isAbsolute(report.getTargetAbsPath());

        if (report != null && !report.wasLastRange()) {
            assert report.getConflictedRange().rev1 != report.getConflictedRange().rev2;
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, "One or more conflicts were produced while merging r{0}:{1} into\n" +
                    "''{2}'' --\n" +
                    "resolve all conflicts and rerun the merge to apply the remaining\n" +
                    "unmerged revisions",
                    report.getConflictedRange().rev1,
                    report.getConflictedRange().rev2,
                    report.getTargetAbsPath());
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
    }

    public static class NotifyBeginState {
        public File lastAbsPath;
        public Map<File, MergePath> nodesWithMergeInfo;
    }

    public static class MergeSource {
        SVNURL url1;
        long rev1;
        SVNURL url2;
        long rev2;
        boolean ancestral;

        public MergeSource subrange(long startRevision, long endRevision) {
            boolean isRollBack = rev1 > rev2;
            boolean sameUrls = url1.equals(url2);

            MergeSource mergeSource = new MergeSource();
            mergeSource.ancestral = this.ancestral;
            mergeSource.url1 = this.url1;
            mergeSource.url2 = this.url2;
            mergeSource.rev1 = startRevision;
            mergeSource.rev2 = endRevision;

            if (!sameUrls) {
                if (isRollBack && (endRevision != this.rev2)) {
                    mergeSource.url2 = this.url1;
                }
                if ((!isRollBack) && (startRevision != this.rev1)) {
                    mergeSource.url1 = this.url2;
                }
            }
            return mergeSource;
        }
    }

    String diff3Cmd;

    boolean forceDelete;
    public boolean useSleep;

    boolean dryRun;
    boolean recordOnly;
    boolean sourcesAncestral;
    boolean sameRepos;
    boolean ignoreMergeInfo;
    private boolean mergeinfoCapable;
    private boolean diffIgnoreAncestry;
    private boolean targetMissingChild;
    boolean reintegrateMerge;
    
    File targetAbsPath;
    File addedPath;
    SVNURL reposRootUrl;

    MergeSource mergeSource;

    protected SVNMergeRangeList implicitSrcGap;
    SVNWCContext context;
    
    private boolean addNecessiatedMerge;
    
    Collection<File> dryRunDeletions;
    Collection<File> dryRunAdded;
    private int operativeNotifications;
    private int notifications;

    protected Collection<File> addedPaths;
    protected Collection<File> mergedPaths;
    protected Collection<File> skippedPaths;
    protected Collection<File> treeConflictedPaths;
    protected Collection<File> conflictedPaths;

    Collection<File> pathsWithNewMergeInfo;
    Collection<File> pathsWithDeletedMergeInfo;

    SVNDiffOptions diffOptions;
    
    SVNRepository repos1;
    SVNRepository repos2;
    
    SvnMerge operation;
    SvnNgRepositoryAccess repositoryAccess;
    
    private int currentAncestorIndex;
    private boolean singleFileMerge;
    public NotifyBeginState notifyBegin;

    public SvnNgMergeDriver(SVNWCContext context, SvnMerge operation, SvnNgRepositoryAccess repositoryAccess, SVNDiffOptions diffOptions) {
        this.context = context;
        this.operation = operation;
        this.repositoryAccess = repositoryAccess;
        this.diffOptions = diffOptions;
        this.notifyBegin = new NotifyBeginState();
    }
    
    public void ensureWcIsSuitableForMerge(File targetAbsPath, boolean allowMixedRevs, boolean allowLocalMods, boolean allowSwitchedSubtrees) throws SVNException {
        if (!allowMixedRevs) {
            long[] revs = SvnWcDbReader.getMinAndMaxRevisions((SVNWCDb) context.getDb(), targetAbsPath);
            if (revs[0] < 0 && revs[1] >= 0) {
                if (!context.isNodeAdded(targetAbsPath)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, 
                            "Cannot determine revision of working copy");
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            } else if (revs[0] != revs[1]) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_MERGE_UPDATE_REQUIRED, 
                        "Cannot merge into mixed-revision working copy [{0}:{1}]; " +
                		"try updating first", revs[0], revs[1]);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        if (!allowSwitchedSubtrees) {
            if (SvnWcDbReader.hasSwitchedSubtrees((SVNWCDb) context.getDb(), targetAbsPath)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, 
                        "Cannot merge into a working copy with a switched subtree");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        
        if (!allowLocalMods) {
            if (SvnWcDbReader.hasLocalModifications(context, targetAbsPath)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, 
                        "Cannot merge into a working copy that has local modifications");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
    }
    
    public MergeData mergeCousinsAndSupplementMergeInfo(File targetWCPath,
            SVNRepository repository1, SVNRepository repository2, 
            SVNURL url1, long rev1, SVNURL url2, long rev2, long youngestCommonRev,
            SVNURL sourceReposRoot, SVNURL wcReposRoot, SVNDepth depth, boolean ignoreMergeInfo,
            boolean ignoreAncestry, boolean forceDelete, boolean recordOnly, boolean dryRun) throws SVNException {

        assert repository1.getLocation().equals(url1);
        assert repository2.getLocation().equals(url2);

        List<MergeSource> addSources = null;
        List<MergeSource> removeSources = null;
        SVNRevision sRev = SVNRevision.create(rev1);
        SVNRevision eRev = SVNRevision.create(youngestCommonRev);
        SVNRevisionRange range = new SVNRevisionRange(sRev, eRev);
        List<SVNRevisionRange> ranges = new LinkedList<SVNRevisionRange>();
        ranges.add(range);
        repository1.setLocation(url1, false);
        removeSources = normalizeMergeSources(SvnTarget.fromURL(url1), url1, sourceReposRoot, sRev, ranges, repository1);
        
        sRev = eRev;
        eRev = SVNRevision.create(rev2);
        range = new SVNRevisionRange(sRev, eRev);
        ranges.clear();
        ranges.add(range);
        repository2.setLocation(url2, false);
        addSources = normalizeMergeSources(SvnTarget.fromURL(url2), url2, sourceReposRoot, eRev, ranges, repository2);

        SvnConflictReport conflictReport = null;
        MergeData mergeData = null;

        boolean sameRepos = sourceReposRoot.equals(wcReposRoot);
        Collection<File> modifiedSubtrees = null;
        if (!recordOnly) {
            MergeSource fauxSource = new MergeSource();
            fauxSource.url1 = url1;
            fauxSource.url2 = url2;
            fauxSource.rev1 = rev1;
            fauxSource.rev2 = rev2;
            List<MergeSource> fauxSources = new LinkedList<MergeSource>();
            fauxSources.add(fauxSource);

            mergeData = doMerge(null, fauxSources, targetWCPath, repository1, true, sameRepos, ignoreMergeInfo,
                    ignoreAncestry, forceDelete, dryRun, false, null, true, false, depth, diffOptions);
            modifiedSubtrees = mergeData.modifiedSubtrees;
            conflictReport = mergeData.conflictReport;

            if (conflictReport != null && !conflictReport.wasLastRange()) {
                return mergeData;
            }
        } else if (!sameRepos) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, 
                    "Merge from foreign repository is not compatible with mergeinfo modification");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }
        
        if (sameRepos && !dryRun) {
            
            Map<File, Map<String, SVNMergeRangeList>> addResultsCatalog = new TreeMap<File, Map<String, SVNMergeRangeList>>();
            Map<File, Map<String, SVNMergeRangeList>> removeResultsCatalog = new TreeMap<File, Map<String, SVNMergeRangeList>>();
            
            if (context.getEventHandler() != null) {
                SVNEvent mergeStartedEvent = SVNEventFactory.createSVNEvent(targetAbsPath, SVNNodeKind.NONE, null, SVNRepository.INVALID_REVISION, 
                        SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE, SVNStatusType.LOCK_INAPPLICABLE, SVNEventAction.MERGE_RECORD_INFO_BEGIN,
                        null, null, null); 
                context.getEventHandler().handleEvent(mergeStartedEvent, ISVNEventHandler.UNKNOWN);
            }

            mergeData = doMerge(addResultsCatalog, addSources, targetWCPath, repository1, true, sameRepos, ignoreMergeInfo,
                    ignoreAncestry, forceDelete, dryRun, true, modifiedSubtrees, true, true, depth, diffOptions);
            if (mergeData.conflictReport != null && mergeData.conflictReport.wasLastRange()) {
                return mergeData;
            }

            mergeData = doMerge(removeResultsCatalog, removeSources, targetWCPath, repository1, true, sameRepos, ignoreMergeInfo,
                    ignoreAncestry, forceDelete, dryRun, true, modifiedSubtrees, true, true, depth, diffOptions);
            if (mergeData.conflictReport != null && mergeData.conflictReport.wasLastRange()) {
                return mergeData;
            }

            SVNMergeInfoUtil.mergeCatalog(addResultsCatalog, removeResultsCatalog);

            if (!addResultsCatalog.isEmpty()) {
                for (Iterator<File> paths = addResultsCatalog.keySet().iterator(); paths.hasNext();) {
                    File file = paths.next();
                    Map<String, SVNMergeRangeList> mergeinfo = addResultsCatalog.get(file);
                    try {
                        recordMergeinfo(file, mergeinfo, true);
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_NOT_FOUND) {
                            continue;
                        }
                        throw e;
                    }
                }
            }
        }

        return mergeData;
    }
    
    public List<MergeSource> normalizeMergeSources(SvnTarget source, SVNURL sourceURL, SVNURL sourceRootURL, 
            SVNRevision pegRevision, Collection<SVNRevisionRange> rangesToMerge, SVNRepository repository) throws SVNException {
        Structure<RevisionsPair> pair = repositoryAccess.getRevisionNumber(repository, source, pegRevision, null);
        long pegRevNum = pair.lng(RevisionsPair.revNumber);
        
        if (pegRevNum < 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION);
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }
        List<SVNMergeRange> mergeRanges = new ArrayList<SVNMergeRange>();
        for (Iterator<SVNRevisionRange> rangesIter = rangesToMerge.iterator(); rangesIter.hasNext();) {
            SVNRevisionRange revRange = rangesIter.next();
            SVNRevision rangeStart = revRange.getStartRevision();
            SVNRevision rangeEnd = revRange.getEndRevision();
            
            if (!rangeStart.isValid() || !rangeEnd.isValid()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, 
                        "Not all required revisions are specified");
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }            
            pair = repositoryAccess.getRevisionNumber(repository, source, rangeStart, pair);
            long startRev = pair.lng(RevisionsPair.revNumber);
            pair = repositoryAccess.getRevisionNumber(repository, source, rangeEnd, pair); 
            long endRev = pair.lng(RevisionsPair.revNumber);
            if (startRev != endRev) {
                mergeRanges.add(new SVNMergeRange(startRev, endRev, true));
            }
        }
        pair.release();
        
        if (mergeRanges.isEmpty()) {
            return Collections.emptyList();
        }
        
        long oldestRequested = -1;
        long youngesRequested = -1;
        for (SVNMergeRange mergeRange : mergeRanges) {
            long minRev = Math.min(mergeRange.getStartRevision(), mergeRange.getEndRevision());
            long maxRev = Math.max(mergeRange.getStartRevision(), mergeRange.getEndRevision());
            if (oldestRequested < 0 || minRev < oldestRequested) {
                oldestRequested = minRev;
            }
            if (youngesRequested < 0 || maxRev > youngesRequested) {
                youngesRequested = maxRev;
            }
        }
        if (pegRevNum < youngesRequested) {
            repositoryAccess.getLocations(repository, SvnTarget.fromURL(sourceURL), SVNRevision.create(pegRevNum), SVNRevision.create(youngesRequested), SVNRevision.UNDEFINED);
            pegRevNum = youngesRequested;            
        }
        
        List<SVNLocationSegment> segments = repository.getLocationSegments("", pegRevNum, youngesRequested, oldestRequested);
        long trimRevision = -1;
        if (!segments.isEmpty()) {
            SVNLocationSegment segment = segments.get(0);
            if (segment.getStartRevision() != oldestRequested) {
                trimRevision = segment.getStartRevision();
            } else if (segment.getPath() == null) {
                if (segments.size() > 1) {
                    SVNLocationSegment segment2 = (SVNLocationSegment) segments.get(1);
                    SVNURL segmentURL = sourceRootURL.appendPath(segment2.getPath(), false);
                    SVNLocationEntry copyFromLocation = repositoryAccess.getCopySource(SvnTarget.fromURL(segmentURL), SVNRevision.create(segment2.getStartRevision()));
                    String copyFromPath = copyFromLocation.getPath();
                    long copyFromRevision = copyFromLocation.getRevision();
                    if (copyFromPath != null && SVNRevision.isValidRevisionNumber(copyFromRevision)) {
                        SVNLocationSegment newSegment = new SVNLocationSegment(copyFromRevision, 
                                copyFromRevision, copyFromPath);
                        segment.setStartRevision(copyFromRevision + 1);
                        segments.add(0, newSegment);
                    }
                }
                
            }
        }
        SVNLocationSegment[] segmentsArray = (SVNLocationSegment[]) segments.toArray(new SVNLocationSegment[segments.size()]);
        List<MergeSource> resultMergeSources = new ArrayList<MergeSource>();
        for (Iterator<SVNMergeRange> rangesIter = mergeRanges.iterator(); rangesIter.hasNext();) {
            SVNMergeRange range = (SVNMergeRange) rangesIter.next();
            if (SVNRevision.isValidRevisionNumber(trimRevision)) {
                if (Math.max(range.getStartRevision(), range.getEndRevision()) < trimRevision) {
                    continue;
                }
                if (range.getStartRevision() < trimRevision) {
                    range.setStartRevision(trimRevision);
                }
                if (range.getEndRevision() < trimRevision) {
                    range.setEndRevision(trimRevision);
                }
            }
            List<MergeSource> mergeSources = combineRangeWithSegments(range, segmentsArray, sourceRootURL);
            resultMergeSources.addAll(mergeSources);
        }
        return resultMergeSources;
    }

    private List<MergeSource> combineRangeWithSegments(SVNMergeRange range, SVNLocationSegment[] segments,  SVNURL sourceRootURL) throws SVNException {
        long minRev = Math.min(range.getStartRevision(), range.getEndRevision()) + 1;
        long maxRev = Math.max(range.getStartRevision(), range.getEndRevision());
        boolean subtractive = range.getStartRevision() > range.getEndRevision();
        List<MergeSource> mergeSources = new ArrayList<MergeSource>();
        for (int i = 0; i < segments.length; i++) {
            SVNLocationSegment segment = segments[i];
            if (segment.getEndRevision() < minRev || segment.getStartRevision() > maxRev || 
                    segment.getPath() == null) {
                continue;
            }
            
            String path1 = null;
            long rev1 = Math.max(segment.getStartRevision(), minRev) - 1;
            if (minRev <= segment.getStartRevision()) {
                if (i > 0) {
                    path1 = segments[i - 1].getPath();
                }
                if (path1 == null && i > 1) {
                    path1 = segments[i - 2].getPath();
                    rev1 = segments[i - 2].getEndRevision();
                }
            } else {
                path1 = segment.getPath();
            }
            
            if (path1 == null || segment.getPath() == null) {
                continue;
            }
            
            MergeSource mergeSource = new MergeSource();
            mergeSource.url1 = sourceRootURL.appendPath(path1, false);
            mergeSource.url2 = sourceRootURL.appendPath(segment.getPath(), false);
            mergeSource.rev1 = rev1;
            mergeSource.rev2 = Math.min(segment.getEndRevision(), maxRev);
            if (subtractive) {
                long tmpRev = mergeSource.rev1;
                SVNURL tmpURL = mergeSource.url1;
                mergeSource.rev1 = mergeSource.rev2;
                mergeSource.url1 = mergeSource.url2;
                mergeSource.rev2 = tmpRev;
                mergeSource.url2 = tmpURL;
            }
            mergeSource.ancestral = true;
            mergeSources.add(mergeSource);
        }
        
        if (subtractive && !mergeSources.isEmpty()) {
            Collections.sort(mergeSources, new Comparator<MergeSource>() {
                public int compare(MergeSource o1, MergeSource o2) {
                    MergeSource source1 = (MergeSource) o1;
                    MergeSource source2 = (MergeSource) o2;
                    long src1Rev1 = source1.rev1;
                    long src2Rev1 = source2.rev1;
                    if (src1Rev1 == src2Rev1) {
                        return 0;
                    }
                    return src1Rev1 < src2Rev1 ? 1 : -1;
                }
            });
        }
        return mergeSources;
    }

    protected MergeData doMerge(
            Map<File, Map<String, SVNMergeRangeList>> resultCatalog, 
            List<MergeSource> mergeSources, 
            File targetAbsPath,
            SVNRepository sourceRepository,
            boolean sourcesRelated,
            boolean sameRepository,
            boolean ignoreMergeInfo,
            boolean diffIgnoreAncestry,
            boolean forceDelete,
            boolean dryRun, 
            boolean recordOnly, 
            Collection<File> recordOnlyPaths,
            boolean reintegrateMerge, 
            boolean squelcheMergeInfoNotifications,
            SVNDepth depth,
            SVNDiffOptions diffOptions) throws SVNException {

        MergeData result = new MergeData();

       if (recordOnly) {
           boolean sourcesAncestral = true;

           for (MergeSource mergeSource : mergeSources) {
               if (!mergeSource.ancestral) {
                   sourcesAncestral = false;
                   break;
               }
           }

            if (!sourcesAncestral) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS,
                        "Use of two URLs is not compatible with mergeinfo modification"); 
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            if (!sameRepository) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS,
                        "Merge from foreign repository is not compatible with mergeinfo modification");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (dryRun) {
                return result;
            }
        }
        
        if (depth == SVNDepth.UNKNOWN) {
            depth = SVNDepth.INFINITY;
        }
        this.forceDelete = forceDelete;
        this.dryRun = dryRun;
        this.recordOnly = recordOnly;
        this.ignoreMergeInfo = ignoreMergeInfo;
        this.diffIgnoreAncestry = diffIgnoreAncestry;
        this.sameRepos = sameRepository;
        this.mergeinfoCapable = false;
        this.sourcesAncestral = sourcesAncestral;
        this.targetMissingChild = false;
        this.reintegrateMerge = reintegrateMerge;
        this.targetAbsPath = targetAbsPath;
        this.diffOptions = diffOptions;
        
        this.notifications = 0;
        this.operativeNotifications = 0;
        if (recordOnly && recordOnlyPaths != null) {
            this.mergedPaths = recordOnlyPaths;
        } else {
            this.mergedPaths = null;
        }
        this.skippedPaths = null;
        this.addedPaths = null;
        this.treeConflictedPaths = null;
        this.singleFileMerge = false;
        this.currentAncestorIndex = -1;

        ISvnDiffCallback2 mergeProcessor = new SvnNgMergeCallback2(context, this, repositoryAccess);

        SVNRepository repository1 = null;
        SVNRepository repository2 = null;
        SVNURL oldSrcReposUrl = null;

        if (sourceRepository != null) {
            oldSrcReposUrl = sourceRepository.getLocation();
            repository1 = sourceRepository;
        }

        boolean checkedMergeInfoCapability = false;
        result.modifiedSubtrees = new HashSet<File>();
        for (int i = 0; i < mergeSources.size(); i++) {
            MergeSource mergeSource = mergeSources.get(i);
            SVNURL url1 = mergeSource.url1;
            SVNURL url2 = mergeSource.url2;
            long revision1 = mergeSource.rev1;
            long revision2 = mergeSource.rev2;
            if (revision1 == revision2 && mergeSource.url1.equals(mergeSource.url2)) {
                continue;
            }
            
            try {
                this.repos1 = ensureRepository(repository1, url1);
                this.repos2 = ensureRepository(repository2, url2);
                this.reposRootUrl = repos1.getRepositoryRoot(true);
                this.mergeSource = mergeSource;
                this.implicitSrcGap = null;                
                this.conflictedPaths = null;
                this.pathsWithDeletedMergeInfo = null;
                this.pathsWithNewMergeInfo = null;

                if (!checkedMergeInfoCapability) {
                    this.mergeinfoCapable = repos1.hasCapability(SVNCapability.MERGE_INFO);
                    checkedMergeInfoCapability = true;
                }

                repository1 = repos1;
                repository2 = repos2;

                SVNNodeKind src1Kind = repository1.checkPath("", mergeSource.rev1);
                SvnSingleRangeConflictReport conflictedRangeReport = null;
                do {
                    if (src1Kind != SVNNodeKind.DIR) {
                        conflictedRangeReport = doFileMerge(targetAbsPath, resultCatalog, mergeSource, mergeProcessor, sourcesRelated, squelcheMergeInfoNotifications);
                    } else {
                        conflictedRangeReport = doDirectoryMerge(resultCatalog, mergeSource, targetAbsPath, this.reposRootUrl, mergeProcessor, depth, squelcheMergeInfoNotifications);
                    }
                    if (conflictedRangeReport != null && context.getOptions().getConflictResolver() != null && !dryRun) {
                        boolean conflictsRemain = resolveConflicts(conflictedPaths);
                        if (conflictsRemain) {
                            break;
                        }
                        this.conflictedPaths = null;
                        mergeSource = conflictedRangeReport.getRemainingSource();
                        conflictedRangeReport = null;
                    } else {
                        break;
                    }
                } while (mergeSource != null);

                if (!dryRun) {
                    SvnNgMergeinfoUtil.elideMergeInfo(context, repos1, targetAbsPath, null);
                }
                if (conflictedRangeReport != null) {
                    result.conflictReport = new SvnConflictReport(targetAbsPath, conflictedRangeReport.getConflictedRange(),
                            (i == mergeSources.size() - 1) && (conflictedRangeReport.getRemainingSource() == null));
                    break;
                }
            } finally {
                if (repos1 != null) {
                    repos1.closeSession();
                }
                if (repos2 != null) {
                    repos2.closeSession();
                }
            }
        }
//        if (result.conflictReport == null || result.conflictReport.wasLastRange()) {
//            if (context.getEventHandler() != null) {
//                SVNEvent mergeCompletedEvent = SVNEventFactory.createSVNEvent(targetAbsPath, SVNNodeKind.NONE, null, SVNRepository.INVALID_REVISION,
//                        SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE, SVNStatusType.LOCK_INAPPLICABLE, SVNEventAction.MERGE_COMPLETE,
//                        null, null, null);
//                context.getEventHandler().handleEvent(mergeCompletedEvent, ISVNEventHandler.UNKNOWN);
//            }
//        }

        if (addedPaths != null) {
            result.modifiedSubtrees.addAll(addedPaths);
        }
        if (mergedPaths != null) {
            result.modifiedSubtrees.addAll(mergedPaths);
        }
        if (treeConflictedPaths != null) {
            result.modifiedSubtrees.addAll(treeConflictedPaths);
        }
        if (skippedPaths != null) {
            result.modifiedSubtrees.addAll(skippedPaths);
        }

        if (sourceRepository != null) {
            sourceRepository.setLocation(oldSrcReposUrl, false);
        }

        result.useSleep = this.useSleep;
        return result;
    }

    protected static class MergeData {
        Collection<File> modifiedSubtrees;
        SvnConflictReport conflictReport;
        public boolean useSleep;
    }

    protected SvnSingleRangeConflictReport doMergeInfoAwareDirectoryMerge(Map<File, Map<String, SVNMergeRangeList>> resultCatalog,
                                                                          MergeSource source, File targetPath, SVNURL sourceRootUrl,
                                                                          Map<File, MergePath> childrenWithMergeInfo,
                                                                          SVNDepth depth, boolean squelchMergeinfoNotifications,
                                                                          ISvnDiffCallback2 processor) throws SVNException {
        boolean isRollback = source.rev1 > source.rev2;
        assert source.ancestral;

        SvnSingleRangeConflictReport conflictReport = null;
        SVNRepository repository = isRollback ? repos1 : repos2;
        SVNURL primaryURL = isRollback ? source.url1 : source.url2;
        getMergeInfoPaths(childrenWithMergeInfo, targetAbsPath, depth, dryRun, sameRepos);

//        MergePath targetMergePath = childrenWithMergeInfo.get(0);
        MergePath targetMergePath = childrenWithMergeInfo.entrySet().iterator().next().getValue();

        populateRemainingRanges(childrenWithMergeInfo, sourceRootUrl, source.url1, source.rev1, source.url2, source.rev2, true, repository, getPathRelativeToRoot(primaryURL, sourceRootUrl, null));

        SVNMergeRange range = new SVNMergeRange(source.rev1, source.rev2, true);

        if (!reintegrateMerge) {
            long newRangeStart = getMostInclusiveStartRevision(childrenWithMergeInfo, isRollback);
            if (SVNRevision.isValidRevisionNumber(newRangeStart)) {
                range.setStartRevision(newRangeStart);
            }
            if (!isRollback) {
                removeNoOpSubtreeRanges(source.url1, source.rev1, source.url2, source.rev2, targetAbsPath, repository, childrenWithMergeInfo);
            }
            fixDeletedSubtreeRanges(source.url1, source.rev1, source.url2, source.rev2, repository, childrenWithMergeInfo);
            long startRev = getMostInclusiveStartRevision(childrenWithMergeInfo, isRollback);
            if (SVNRevision.isValidRevisionNumber(startRev)) {
                long endRev = getMostInclusiveEndRevision(childrenWithMergeInfo, isRollback);

                while (endRev != -1) {
                    MergeSource realSource;
                    SVNMergeRange firstTargetRange = targetMergePath.remainingRanges.getSize() == 0 ? null : targetMergePath.remainingRanges.getRanges()[0];
                    if (firstTargetRange != null && startRev != firstTargetRange.getStartRevision()) {
                        if (isRollback) {
                            if (endRev < firstTargetRange.getStartRevision()) {
                                endRev = firstTargetRange.getStartRevision();
                            }
                        } else {
                            if (endRev > firstTargetRange.getStartRevision()) {
                                endRev = firstTargetRange.getStartRevision();
                            }
                        }
                    }

                    sliceRemainingRanges(childrenWithMergeInfo, isRollback, endRev);

                    notifyBegin.lastAbsPath = null;

                    realSource = source.subrange(startRev, endRev);
                    driveMergeReportEditor(targetAbsPath, realSource, childrenWithMergeInfo, processor, depth);

                    processChildrenWithNewMergeInfo(childrenWithMergeInfo);
                    removeChildrenWithDeletedMergeInfo(childrenWithMergeInfo);
                    removeFirstRangeFromRemainingRanges(endRev, childrenWithMergeInfo);

                    if (conflictedPaths != null && conflictedPaths.size() > 0) {
                        MergeSource remainingRange = null;
                        if (realSource.rev2 != source.rev2) {
                            remainingRange = source.subrange(realSource.rev2, source.rev2);
                        }
                        conflictReport = new SvnSingleRangeConflictReport(realSource, remainingRange);
                        range.setEndRevision(endRev);
                        break;
                    }

                    startRev = getMostInclusiveStartRevision(childrenWithMergeInfo, isRollback);
                    endRev = getMostInclusiveEndRevision(childrenWithMergeInfo, isRollback);
                }
            }
        } else {
            if (!recordOnly) {
                notifyBegin.lastAbsPath = null;
                driveMergeReportEditor(targetAbsPath, source.url1, source.rev1, source.url2, source.rev2, null, depth, processor);
            }
        }

        if (isRecordMergeInfo()) {
            SVNURL primarySrcUrl = isRollback ? source.url1 : source.url2;
            long primarySrcRev = isRollback ? source.rev1 : source.rev2;
            String mergeInfoPath = getPathRelativeToRoot(primaryURL, reposRootUrl, null);
            recordMergeInfoForDirectoryMerge(resultCatalog, range, mergeInfoPath, depth, squelchMergeinfoNotifications, childrenWithMergeInfo);
            if (range.getStartRevision() < range.getEndRevision()) {
                recordMergeInfoForAddedSubtrees(range, mergeInfoPath, depth, squelchMergeinfoNotifications, childrenWithMergeInfo);
            }
        }

        return conflictReport;
    }

    protected SvnSingleRangeConflictReport doMergeInfoUnawareDirectoryMerge(MergeSource source, File targetPath, Map<File, MergePath> childrenWithMergeInfo, SVNDepth depth) throws SVNException {
        boolean isRollBack = source.rev1 > source.rev2;

        MergePath item = new MergePath(targetPath);
        SVNMergeRange itemRange = new SVNMergeRange(source.rev1, source.rev2, true);
        item.remainingRanges = new SVNMergeRangeList(itemRange);

        if (childrenWithMergeInfo == null) {
            childrenWithMergeInfo = new TreeMap<File, MergePath>();
        }
        childrenWithMergeInfo.put(targetPath, item);

        SvnNgMergeCallback2 mergeCallback = new SvnNgMergeCallback2(context, this, repositoryAccess);
        driveMergeReportEditor(targetPath, source.url1, source.rev1, source.url2, source.rev2, childrenWithMergeInfo, depth, mergeCallback);
        if (conflictedPaths != null && conflictedPaths.size() > 0) {
            return new SvnSingleRangeConflictReport(source, null);
        }
        return null;
    }

    private SvnSingleRangeConflictReport doFileMerge(File targetAbsPath, Map<File, Map<String, SVNMergeRangeList>> resultCatalog,
                                                     MergeSource source, ISvnDiffCallback2 mergeProcessor, boolean sourcesRelated, boolean squelcheMergeInfoNotifications) throws SVNException {
        SvnSingleRangeConflictReport conflictReport = null;
        singleFileMerge = true;
        SVNURL targetURL = context.getNodeUrl(targetAbsPath);
        SVNMergeRange range = new SVNMergeRange(source.rev1, source.rev2, true);
        boolean isRollback = source.rev1 > source.rev2;
        SVNURL primaryURL = isRollback ? source.url1 : source.url2;
        SVNMergeRangeList remainingRanges = null;
        String mergeInfoPath = null;
        MergePath mergeTarget = new MergePath(targetAbsPath);
        boolean[] inherited = new boolean[1];
        Map<String, SVNMergeRangeList> targetMergeInfo = null;
        SVNURL oldRepositoryUrl = null;

        assert SVNFileUtil.isAbsolute(targetAbsPath);

        if (isHonorMergeInfo()) {
            SVNURL sourceReposRootURL = repos1.getRepositoryRoot(true);
            mergeInfoPath = getPathRelativeToRoot(primaryURL, sourceReposRootURL, repos1);
            try {
                Map<String, SVNMergeRangeList>[] mis = getFullMergeInfo(true, true, inherited, SVNMergeInfoInheritance.INHERITED, repos1, targetAbsPath,
                        Math.max(source.rev1, source.rev2), Math.min(source.rev1, source.rev2));
                mergeTarget.implicitMergeInfo = mis[1];
                targetMergeInfo = mis[0];
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CLIENT_INVALID_MERGEINFO_NO_MERGETRACKING,
                            "Invalid mergeinfo detected on merge target ''{0}'', " +
                                    "merge tracking not possible", targetAbsPath, e);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
                throw e;
            }
            if (!recordOnly) {
                repos1.setLocation(source.url1, false);
                calculateRemainingRanges(null, mergeTarget,
                        sourceReposRootURL,
                        source.url1, source.rev1, source.url2, source.rev2,
                        targetMergeInfo,
                        implicitSrcGap, false, false, repos1);
                remainingRanges = mergeTarget.remainingRanges;

                if (remainingRanges.getSize() > 0) {
                    SVNMergeRange[] ranges = remainingRanges.getRanges();
                    SVNMergeRange adjStartRange = ranges[0];
                    SVNMergeRange adjEndRange = ranges[ranges.length - 1];

                    range.setStartRevision(adjStartRange.getStartRevision());
                    range.setEndRevision(adjEndRange.getEndRevision());
                }
            }
        }

        if (recordOnly || !isHonorMergeInfo()) {
            remainingRanges = new SVNMergeRangeList(range);
        }
        SVNMergeRange conflictedRange = null;
        if (!recordOnly) {
            SVNMergeRangeList rangesToMerge = remainingRanges;
            File targetRelPath = SVNFileUtil.createFilePath("");
            MergePath targetInfo = null;

            if (source.ancestral) {
                if (remainingRanges.getSize() > 1) {
                    SVNURL oldUrl = ensureSessionURL(repos1, primaryURL);
                    rangesToMerge = removeNoOpMergeRanges(repos1, remainingRanges);
                    if (oldUrl != null) {
                        repos1.setLocation(oldUrl, false);
                    }
                }

                Map<File, MergePath> childWithMergeInfo = new TreeMap<File, MergePath>();
                targetInfo = new MergePath(mergeTarget.absPath);
                targetInfo.remainingRanges = rangesToMerge;
                childWithMergeInfo.put(mergeTarget.absPath, targetInfo);

                notifyBegin.nodesWithMergeInfo = childWithMergeInfo;
            }

            SVNMergeRange[] ranges = rangesToMerge.getRanges();
            List<SVNMergeRange> rangesToMergeList = new ArrayList<SVNMergeRange>(Arrays.asList(ranges));
            for (Iterator<SVNMergeRange> iterator = rangesToMergeList.iterator(); iterator.hasNext(); ) {
                final SVNMergeRange r = iterator.next();
                notifyBegin.lastAbsPath = null;

                MergeSource realSource;
                if (source.ancestral) {
                    realSource = source.subrange(r.getStartRevision(), r.getEndRevision());
                } else {
                    realSource = source;
                }

                SingleFileMergeData singleFileMergeData1 = singleFileMergeGetFile(repos1, realSource.url1, realSource.rev1, targetAbsPath);
                File leftFile = singleFileMergeData1.file;
                SVNProperties leftProperties = singleFileMergeData1.properties;
                SingleFileMergeData singleFileMergeData2 = singleFileMergeGetFile(repos2, realSource.url2, realSource.rev2, targetAbsPath);
                File rightFile = singleFileMergeData2.file;
                SVNProperties rightProperties = singleFileMergeData2.properties;

                SvnDiffSource leftSource = new SvnDiffSource(r.getStartRevision());
                SvnDiffSource rightSource = new SvnDiffSource(r.getEndRevision());

                SvnDiffCallbackResult result = new SvnDiffCallbackResult();
                if (!(diffIgnoreAncestry || sourcesRelated)) {
                    boolean skip = false;
                    mergeProcessor.fileOpened(result, targetRelPath, leftSource, rightSource, null, true, null);
                    skip = result.skip;
                    Object dirBaton = result.newBaton;
                    result.reset();
                    if (!skip) {
                        mergeProcessor.fileDeleted(result, targetRelPath, leftSource, leftFile, leftProperties);
                        skip = result.skip;
                        result.reset();
                    }

                    mergeProcessor.fileOpened(result, targetRelPath, null, rightSource, null, true, dirBaton);
                    skip = result.skip;
                    result.reset();
                    if (!skip) {
                        mergeProcessor.fileAdded(result, targetRelPath, null, rightSource, null, rightFile, null, rightProperties);
                        skip = result.skip;
                        result.reset();
                    }
                } else {
                    SVNProperties propChanges = leftProperties.compareTo(rightProperties);
                    mergeProcessor.fileOpened(result, targetRelPath, leftSource, rightSource, null, false, null);
                    boolean skip = result.skip;
                    result.reset();
                    if (!skip) {
                        mergeProcessor.fileChanged(result, targetRelPath, leftSource, rightSource, leftFile, rightFile, leftProperties, rightProperties, true, propChanges);
                        result.reset();
                    }
                }
                if (conflictedPaths != null && conflictedPaths.size() > 0) {
                    MergeSource remainingRange = null;
                    if (realSource.rev2 != this.mergeSource.rev2) {
                        remainingRange = this.mergeSource.subrange(realSource.rev2, this.mergeSource.rev2);
                    }
                    conflictReport = new SvnSingleRangeConflictReport(realSource, remainingRange);
                    range.setEndRevision(r.getEndRevision());
                    break;
                }

                iterator.remove();
                if (targetInfo != null && targetInfo.absPath == mergeTarget.absPath && targetInfo.remainingRanges != null) {
                    SVNMergeRange[] resultingRanges = new SVNMergeRange[targetInfo.remainingRanges.getSize() - 1];
                    System.arraycopy(targetInfo.remainingRanges.getRanges(), 1, resultingRanges, 0, resultingRanges.length);
                    targetInfo.remainingRanges = new SVNMergeRangeList(resultingRanges);
                }
            }
            notifyBegin.lastAbsPath = null;
        }

        if (isRecordMergeInfo() && remainingRanges.getSize() > 0) {
            SVNMergeRangeList filteredRangeList = filterNaturalHistoryFromMergeInfo(mergeInfoPath, mergeTarget.implicitMergeInfo, range);
            if (!filteredRangeList.isEmpty() && (skippedPaths == null || skippedPaths.isEmpty())) {
                Map<File, SVNMergeRangeList> merges = new TreeMap<File, SVNMergeRangeList>();
                if (inherited[0]) {
                    recordMergeinfo(targetAbsPath, targetMergeInfo, false);
                }
                merges.put(targetAbsPath, filteredRangeList);
                if (!squelcheMergeInfoNotifications) {
                    long[] revs = SVNMergeInfoUtil.getRangeEndPoints(merges);
                    SVNMergeRange r = new SVNMergeRange(revs[1], revs[0], true);

                    SVNEvent mergeBeginEvent = SVNEventFactory.createSVNEvent(targetAbsPath,
                            SVNNodeKind.FILE,
                            null, -1, SVNEventAction.MERGE_RECORD_INFO_BEGIN,
                            null, null, r);

                    if (context.getEventHandler() != null) {
                        context.getEventHandler().handleEvent(mergeBeginEvent, -1);
                    }
                }
                updateWCMergeInfo(resultCatalog, targetAbsPath, mergeInfoPath, merges, isRollback);
            }
        }
        notifyBegin.nodesWithMergeInfo = null;

        return conflictReport;
    }


    private static class SingleFileMergeData {
        private File file;
        private SVNProperties properties;
    }

    private SingleFileMergeData singleFileMergeGetFile(SVNRepository repository, SVNURL url, long revision, File wcTarget) throws SVNException {
        SingleFileMergeData result = new SingleFileMergeData();
        result.file = SVNFileUtil.createUniqueFile(context.getDb().getWCRootTempDir(wcTarget), "merge", "", false);
        SVNURL oldUrl = ensureSessionURL(repository, url);
        OutputStream outputStream = null;
        try {
            outputStream = SVNFileUtil.openFileForWriting(result.file);
            result.properties = new SVNProperties();
            repository.getFile("", revision, result.properties, outputStream);
            return result;
        } finally {
            SVNFileUtil.closeFile(outputStream);
            if (oldUrl != null) {
                repository.setLocation(oldUrl, false);
            }
        }
    }

    protected SvnSingleRangeConflictReport doDirectoryMerge(Map<File, Map<String, SVNMergeRangeList>> resultCatalog, MergeSource source, File targetAbsPath, SVNURL sourceRootUrl, ISvnDiffCallback2 processor, SVNDepth depth, boolean squelchMergeinfoNotifications) throws SVNException {
        Map<File, MergePath> childrenWithMergeInfo = new TreeMap<File, MergePath>(PATH_COMPARATOR);
        notifyBegin.nodesWithMergeInfo = childrenWithMergeInfo;
        SvnSingleRangeConflictReport svnSingleRangeConflictReport;
        if (isHonorMergeInfo()) {
            svnSingleRangeConflictReport = doMergeInfoAwareDirectoryMerge(resultCatalog, source, targetAbsPath, sourceRootUrl, childrenWithMergeInfo, depth, squelchMergeinfoNotifications, processor);
        } else {
            svnSingleRangeConflictReport = doMergeInfoUnawareDirectoryMerge(source, targetAbsPath, childrenWithMergeInfo, depth);
        }
        notifyBegin.nodesWithMergeInfo = null;
        return svnSingleRangeConflictReport;
    }

    private void removeNoOpSubtreeRanges(SVNURL url1, long revision1, SVNURL url2, long revision2, File targetAbsPath, SVNRepository repository, Map<File, MergePath> childrenWithMergeInfo) throws SVNException {
        if (revision1 > revision2) {
            return;
        }
        if (childrenWithMergeInfo.size() < 2) {
            return;
        }
        Iterator<MergePath> mps = childrenWithMergeInfo.values().iterator();
        MergePath rootPath = mps.next();
        SVNMergeRangeList requestedRanges = new SVNMergeRangeList(
                new SVNMergeRange(Math.min(revision1, revision2), 
                        Math.max(revision1, revision2), true));
        SVNMergeRangeList subtreeGapRanges = requestedRanges.remove(rootPath.remainingRanges, false);
        if (subtreeGapRanges.isEmpty()) {
            return;
        }
        SVNMergeRangeList subtreeRemainingRanges = new SVNMergeRangeList(new SVNMergeRange[0]);
        while(mps.hasNext()) {
            MergePath child = mps.next();
            if (child.remainingRanges != null && !child.remainingRanges.isEmpty()) {
                subtreeRemainingRanges = subtreeRemainingRanges.merge(child.remainingRanges);
            }
        }
        if (subtreeRemainingRanges.isEmpty()) {
            return;
        }
        subtreeGapRanges = subtreeGapRanges.intersect(subtreeRemainingRanges, false);
        if (subtreeGapRanges.isEmpty()) {
            return;
        }
        SVNMergeRange oldestGapRev = subtreeGapRanges.getRanges()[0];
        SVNMergeRange youngestRev = subtreeGapRanges.getRanges()[subtreeGapRanges.getSize() - 1];
        SVNURL reposRootURL = context.getNodeReposInfo(targetAbsPath).reposRootUrl;

        NoopLogHandler logHandler = new NoopLogHandler();
        logHandler.sourceReposAbsPath = getPathRelativeToRoot(url2, reposRootURL, null);
        logHandler.mergedRanges = new SVNMergeRangeList(new SVNMergeRange[0]);
        logHandler.operativeRanges = new SVNMergeRangeList(new SVNMergeRange[0]);
        logHandler.childrenWithMergeInfo = childrenWithMergeInfo;
        
        repository.log(new String[] {""}, oldestGapRev.getStartRevision() + 1, youngestRev.getEndRevision(), true, true, -1, false, null, logHandler);
        
        SVNMergeRangeList inoperativeRanges = new SVNMergeRangeList(oldestGapRev.getStartRevision(), youngestRev.getEndRevision(), true);
        inoperativeRanges = inoperativeRanges.remove(logHandler.operativeRanges, false);
        logHandler.mergedRanges = logHandler.mergedRanges.merge(inoperativeRanges);

        Iterator<MergePath> iterator = childrenWithMergeInfo.values().iterator();
        if (iterator.hasNext()) {
            iterator.next();
        }
        for (; iterator.hasNext(); ) {
            MergePath child = iterator.next();
            if (child.remainingRanges != null && !child.remainingRanges.isEmpty()) {
                child.remainingRanges = child.remainingRanges.remove(logHandler.mergedRanges, false);
            }
        }
    }

    private void driveMergeReportEditor(File targetAbsPath, MergeSource source, final Map<File, MergePath> childrenWithMergeInfo, ISvnDiffCallback2 processor, SVNDepth depth) throws SVNException {
        driveMergeReportEditor(targetAbsPath, source.url1, source.rev1, source.url2, source.rev2, childrenWithMergeInfo, depth, processor);
    }

    public SvnNgRemoteMergeEditor driveMergeReportEditor(File targetWCPath, SVNURL url1, long revision1,
            SVNURL url2, final long revision2, final Map<File, MergePath> childrenWithMergeInfo,
            SVNDepth depth, ISvnDiffCallback2 mergeCallback) throws SVNException {
        final boolean honorMergeInfo = isHonorMergeInfo();
        long targetStart = revision1;
        final boolean isRollBack = revision1 > revision2;
        
        if (honorMergeInfo) {
            targetStart = revision2;
            if (childrenWithMergeInfo != null && !childrenWithMergeInfo.isEmpty()) {                
                MergePath targetMergePath = (MergePath) childrenWithMergeInfo.values().iterator().next();
                SVNMergeRangeList remainingRanges = targetMergePath.remainingRanges; 
                if (remainingRanges != null && !remainingRanges.isEmpty()) {
                    SVNMergeRange[] ranges = remainingRanges.getRanges();
                    SVNMergeRange range = ranges[0];
                    if ((!isRollBack && range.getStartRevision() > revision2) ||
                            (isRollBack && range.getStartRevision() < revision2)) {
                        targetStart = revision2;
                    } else {
                        targetStart = range.getStartRevision();
                    }
                }
            }
        }

        SVNURL oldURL1 = ensureSessionURL(repos1, url1);

//        SvnNgRemoteDiffEditor editor = SvnNgRemoteDiffEditor.createEditor(context, targetAbsPath, depth, repos2, revision1, false, dryRun, false, mergeCallback, this);
        SvnNgRemoteMergeEditor editor = new SvnNgRemoteMergeEditor(targetWCPath, context, repos2, revision1, mergeCallback, true);

        SVNURL oldURL2 = ensureSessionURL(repos2, url1);
        try {
            final SVNDepth reportDepth = depth;
            final long reportStart = targetStart;
            final String targetPath = targetWCPath.getAbsolutePath().replace(File.separatorChar, '/');

            repos1.diff(url2, revision2, revision2, null, diffIgnoreAncestry, depth, true,
            new ISVNReporterBaton() {
                public void report(ISVNReporter reporter) throws SVNException {

                    reporter.setPath("", null, reportStart, reportDepth, false);

                    if (honorMergeInfo && childrenWithMergeInfo != null) {
                        Object[] childrenWithMergeInfoArray = childrenWithMergeInfo.values().toArray();
                        for (int i = 0; i < childrenWithMergeInfoArray.length; i++) {
                            MergePath childMergePath = (MergePath) childrenWithMergeInfoArray[i];
                            MergePath parent = null;
                            if (childMergePath == null || childMergePath.absent) {
                                continue;
                            }
                            //
                            int parentIndex = findNearestAncestor(childrenWithMergeInfoArray, false, childMergePath.absPath);
                            if (parentIndex >= 0 && parentIndex < childrenWithMergeInfoArray.length) {
                                parent = (MergePath) childrenWithMergeInfoArray[parentIndex];
                            }

                            SVNMergeRange range = null;
                            if (childMergePath.remainingRanges != null && !childMergePath.remainingRanges.isEmpty()) {
                                SVNMergeRangeList remainingRangesList = childMergePath.remainingRanges;
                                SVNMergeRange[] remainingRanges = remainingRangesList.getRanges();
                                range = remainingRanges[0];

                                if ((!isRollBack && range.getStartRevision() > revision2) || (isRollBack && range.getStartRevision() < revision2)) {
                                    continue;
                                } else if (parent.remainingRanges != null && !parent.remainingRanges.isEmpty()) {
                                    SVNMergeRange parentRange = parent.remainingRanges.getRanges()[0];
                                    SVNMergeRange childRange = childMergePath.remainingRanges.getRanges()[0];
                                    if (parentRange.getStartRevision() == childRange.getStartRevision()) {
                                        continue;
                                    }
                                }
                            } else {
                                if (parent.remainingRanges == null || parent.remainingRanges.isEmpty()) {
                                    continue;
                                }
                            }

                            String childPath = childMergePath.absPath.getAbsolutePath();
                            childPath = childPath.replace(File.separatorChar, '/');
                            String relChildPath = childPath.substring(targetPath.length());
                            if (relChildPath.startsWith("/")) {
                                relChildPath = relChildPath.substring(1);
                            }

                            if (childMergePath.remainingRanges == null || childMergePath.remainingRanges.isEmpty() || (isRollBack && range.getStartRevision() < revision2) || (!isRollBack && range.getStartRevision() > revision2)) {
                                reporter.setPath(relChildPath, null, revision2, reportDepth, false);
                            } else {
                                reporter.setPath(relChildPath, null, range.getStartRevision(), reportDepth, false);
                            }
                        }
                    }
                    reporter.finishReport();
                }
            }, 
            SVNCancellableEditor.newInstance(editor, operation.getCanceller(), SVNDebugLog.getDefaultLog()));
        } finally {
            if (oldURL1 != null) {
                repos1.setLocation(oldURL1, false);
            }
            if (oldURL2 != null) {
                repos2.setLocation(oldURL2, false);
            }
            editor.cleanup();
        }
        
//        if (conflictedPaths == null) {
//            conflictedPaths = mergeCallback.getConflictedPaths();
//        }
        return editor;
    }

    protected boolean isHonorMergeInfo() {
        return mergeSource.ancestral && sameRepos && !diffIgnoreAncestry && mergeinfoCapable && !ignoreMergeInfo;
    }

    public boolean isRecordMergeInfo() {
        return !dryRun && isHonorMergeInfo();
    }

    protected static SVNURL ensureSessionURL(SVNRepository repository, SVNURL url) throws SVNException {
        SVNURL oldURL = repository.getLocation();
        if (url == null) {
            url = repository.getRepositoryRoot(true);
        }
        if (!url.equals(oldURL)) {
            repository.setLocation(url, false);
            return oldURL;
        }
        return oldURL;
    }

    protected static MergePath findNearestAncestor(Map<File, MergePath> childrenWithMergeInfo, boolean pathIsAncestor, File localAbsPath) {
        assert childrenWithMergeInfo != null;

        for (Map.Entry<File, MergePath> entry : childrenWithMergeInfo.entrySet()) {
            MergePath child = entry.getValue();
            if (SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(child.absPath), SVNFileUtil.getFilePath(localAbsPath)) && (pathIsAncestor || !child.absPath.equals(localAbsPath))) {
                return child;
            }
        }
        return null;
    }

    protected static int findNearestAncestor(Object[] childrenWithMergeInfoArray, boolean pathIsOwnAncestor, File path) {
        if (childrenWithMergeInfoArray == null) {
            return 0;
        }

        int ancestorIndex = 0;
        for (int i = 0; i < childrenWithMergeInfoArray.length; i++) {
            MergePath child = (MergePath) childrenWithMergeInfoArray[i];
            String childPath = child.absPath.getAbsolutePath().replace(File.separatorChar, '/');
            String pathStr = path.getAbsolutePath().replace(File.separatorChar, '/');
            if (SVNPathUtil.isAncestor(childPath, pathStr) && (!childPath.equals(pathStr) || pathIsOwnAncestor)) {
                ancestorIndex = i;
            }
        }
        return ancestorIndex;
    }

    private TreeMap<File, Map<String, SVNMergeRangeList>> getWcExplicitMergeInfoCatalog(File targetAbsPath, SVNDepth depth) throws SVNException {
        final SvnGetProperties pg = operation.getOperationFactory().createGetProperties();
        final Map<File, String> subtreesWithMergeinfo = new TreeMap<File, String>();
        pg.setDepth(depth);
        pg.setSingleTarget(SvnTarget.fromFile(targetAbsPath, SVNRevision.WORKING));
        pg.setRevision(SVNRevision.WORKING);
        pg.setReceiver(new ISvnObjectReceiver<SVNProperties>() {
            public void receive(SvnTarget target, SVNProperties object) throws SVNException {
                String value = object.getStringValue(SVNProperty.MERGE_INFO);
                if (value != null) {
                    subtreesWithMergeinfo.put(target.getFile(), value);
                }
            }
        });
        pg.run();

        Map<File, File> externals = context.getDb().getExternalsDefinedBelow(targetAbsPath);

        TreeMap<File, Map<String, SVNMergeRangeList>> result = new TreeMap<File, Map<String, SVNMergeRangeList>>();

        for (Map.Entry<File, String> entry : subtreesWithMergeinfo.entrySet()) {
            File wcPath = entry.getKey();
            String mergeInfoString = entry.getValue();

            if (externals.containsKey(wcPath)) {
                continue;
            }
            Map<String, SVNMergeRangeList> mergeRangeList = null;
            try {
                mergeRangeList = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(mergeInfoString), null);
            } catch (SVNException e){
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CLIENT_INVALID_MERGEINFO_NO_MERGETRACKING,
                            "Invalid mergeinfo detected on ''{0}'', merge tracking not possible", wcPath, e);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            }
            result.put(wcPath, mergeRangeList);
        }
        return result;
    }

    private Map<File, MergePath> getMergeInfoPaths(Map<File, MergePath> childrenWithMergeInfo, final File targetAbsPath,
                                   SVNDepth depth, boolean dryRun, boolean sameRepos) throws SVNException {
        TreeMap<File, Map<String, SVNMergeRangeList>> subtreesWithMergeInfo = getWcExplicitMergeInfoCatalog(targetAbsPath, depth);
        if (subtreesWithMergeInfo != null) {
            for (Map.Entry<File, Map<String, SVNMergeRangeList>> entry : subtreesWithMergeInfo.entrySet()) {
                final File wcPath = entry.getKey();
                final Map<String, SVNMergeRangeList> mergeInfoString = entry.getValue();

                MergePath mergeInfoChild = new MergePath(wcPath);
                mergeInfoChild.preMergeMergeInfo = mergeInfoString;

                mergeInfoChild.hasNonInheritable = SVNMergeInfoUtil.isNonInheritable(mergeInfoChild.preMergeMergeInfo);
                childrenWithMergeInfo.put(wcPath, mergeInfoChild);
            }
        }
        final Map<File, SVNDepth> shallowSubtrees = new HashMap<File, SVNDepth>();
        final Collection<File> missingSubtrees = new HashSet<File>();
        final Collection<File> switchedSubtrees = new HashSet<File>();

        SVNStatusEditor17 statusEditor = new SVNStatusEditor17(targetAbsPath, context, operation.getOptions(), true, true, depth, new ISvnObjectReceiver<SvnStatus>() {
            public void receive(SvnTarget target, SvnStatus status) throws SVNException {
                boolean fileExternal = false;
                if (status.isVersioned() && status.isSwitched() && status.getKind() == SVNNodeKind.FILE) {
                    try {
                        Structure<ExternalNodeInfo> info = SvnWcDbExternals.readExternal(context, status.getPath(), targetAbsPath, ExternalNodeInfo.kind);
                        fileExternal = info.get(ExternalNodeInfo.kind) == SVNWCDbKind.File;
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                            throw e;
                        }
                    }
                }
                if (status.isSwitched() && !fileExternal) {
                    switchedSubtrees.add(status.getPath());
                }
                if (status.getDepth() == SVNDepth.EMPTY || status.getDepth() == SVNDepth.FILES) {
                    shallowSubtrees.put(status.getPath(), status.getDepth());
                }
                if (status.getNodeStatus() == SVNStatusType.STATUS_MISSING) {
                    boolean parentPresent = false;
                    for (File missingRoot : missingSubtrees) {
                        if (SVNWCUtils.isAncestor(missingRoot, status.getPath())) {
                            parentPresent = true;
                            break;
                        }
                    }
                    if (!parentPresent) {
                        missingSubtrees.add(status.getPath());
                    }
                }
            }
        });
        statusEditor.walkStatus(targetAbsPath, depth, true, true, true, null);

        if (!missingSubtrees.isEmpty()) {
            final StringBuffer errorMessage = new StringBuffer("Merge tracking not allowed with missing "
                    + "subtrees; try restoring these items "
                    + "first:\n");
            final Object[] values = new Object[missingSubtrees.size()];
            int index = 0;
            for(File missingPath : missingSubtrees) {
                values[index] = missingPath;
                errorMessage.append("{" + index + "}\n");
                index++;
            }
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, errorMessage.toString(), values);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        if (!switchedSubtrees.isEmpty()) {
            for (File switchedPath : switchedSubtrees) {
                MergePath child = getChildWithMergeinfo(childrenWithMergeInfo, switchedPath);
                if (child != null) {
                    child.switched = true;
                } else {
                    child = new MergePath(switchedPath);
                    child.switched = true;
                    childrenWithMergeInfo.put(child.absPath, child);
                }
            }
        }

        if (!shallowSubtrees.isEmpty()) {
            for (File shallowPath : shallowSubtrees.keySet()) {
                MergePath child = getChildWithMergeinfo(childrenWithMergeInfo, shallowPath);
                SVNDepth childDepth = shallowSubtrees.get(shallowPath);
                boolean newChild = false;
                if (child != null) {
                    if (childDepth == SVNDepth.EMPTY || childDepth == SVNDepth.FILES) {
                        child.missingChild = true;
                    }
                } else {
                    newChild = true;
                    child = new MergePath(shallowPath);
                    if (childDepth == SVNDepth.EMPTY || childDepth == SVNDepth.FILES) {
                        child.missingChild = true;
                    }
                }
                if (!child.hasNonInheritable && (childDepth == SVNDepth.EMPTY || childDepth == SVNDepth.FILES)) {
                    child.hasNonInheritable = true;
                }
                if (newChild) {
                    childrenWithMergeInfo.put(child.absPath, child);
                }
            }
        }

        Collection<File> excludedTrees = SvnWcDbReader.getServerExcludedNodes((SVNWCDb) context.getDb(), targetAbsPath);
        if (excludedTrees != null && !excludedTrees.isEmpty()) {
            for (File excludedTree : excludedTrees) {
                MergePath mp = getChildWithMergeinfo(childrenWithMergeInfo, excludedTree);
                if (mp != null) {
                    mp.absent = true;
                } else {
                    mp = new MergePath(excludedTree);
                    mp.absent = true;
                    childrenWithMergeInfo.put(mp.absPath, mp);
                }
            }
        }

        if (getChildWithMergeinfo(childrenWithMergeInfo, targetAbsPath) == null) {
            childrenWithMergeInfo.put(targetAbsPath, new MergePath(targetAbsPath));
        }
        if (depth == SVNDepth.IMMEDIATES || depth == SVNDepth.FILES) {
            final List<File> immediateChildren = context.getChildrenOfWorkingNode(targetAbsPath, false);
            for (final File immeidateChild : immediateChildren) {
                SVNNodeKind childKind = context.readKind(immeidateChild, false);
                if ((childKind == SVNNodeKind.DIR && depth == SVNDepth.IMMEDIATES) ||
                        (childKind == SVNNodeKind.FILE && depth == SVNDepth.FILES)) {
                    if (getChildWithMergeinfo(childrenWithMergeInfo, immeidateChild) == null) {
                        MergePath mp = new MergePath(immeidateChild);
                        if (childKind == SVNNodeKind.DIR && depth == SVNDepth.IMMEDIATES) {
                            mp.immediateChildDir = true;
                        }
                        childrenWithMergeInfo.put(mp.absPath, mp);
                    }
                }
            }
        }

        if (depth.compareTo(SVNDepth.EMPTY) <= 0) {
            return childrenWithMergeInfo;
        }

        for (File childPath : new TreeSet<File>(childrenWithMergeInfo.keySet())) {
            MergePath child = childrenWithMergeInfo.get(childPath);
            if (child.hasNonInheritable) {
                List<File> childrenOfNonInheritable = context.getNodeChildren(child.absPath, false);
                for (File childOfNonInheritable : childrenOfNonInheritable) {
                    MergePath mpOfNonInheritable = getChildWithMergeinfo(childrenWithMergeInfo, childOfNonInheritable);
                    if (mpOfNonInheritable == null) {
                        if (depth == SVNDepth.FILES) {
                            SVNNodeKind childKind = context.readKind(childOfNonInheritable, false);
                            if (childKind  != SVNNodeKind.FILE) {
                                continue;
                            }
                        }
                        mpOfNonInheritable = new MergePath(childOfNonInheritable);
                        mpOfNonInheritable.childOfNonInheritable = true;
                        childrenWithMergeInfo.put(mpOfNonInheritable.absPath, mpOfNonInheritable);
                        if (!dryRun && sameRepos) {
                            SvnMergeInfoInfo info = SvnNgMergeinfoUtil.getWCMergeInfo(context, mpOfNonInheritable.absPath, targetAbsPath, SVNMergeInfoInheritance.NEAREST_ANCESTOR, false);
                            recordMergeinfo(childOfNonInheritable, info.mergeinfo, false);
                        }
                    }
                }
            }
            insertParentAndSiblingsOfAbsentDelSubtree(childrenWithMergeInfo, child, depth);
        }
        return childrenWithMergeInfo;
    }

    private void insertParentAndSiblingsOfAbsentDelSubtree(Map<File, MergePath> childrenWithMergeInfo, MergePath child, SVNDepth depth) throws SVNException {
        if (!(child.absent || (child.switched && !targetAbsPath.equals(child.absPath)))) {
            return;
        }
        File parentPath = SVNFileUtil.getParentFile(child.absPath);
        MergePath parentMp = getChildWithMergeinfo(childrenWithMergeInfo, parentPath);
        if (parentMp != null) {
            parentMp.missingChild = child.absent;
            parentMp.switchedChild = child.switched;
        } else {
            parentMp = new MergePath(parentPath);
            parentMp.missingChild = child.absent;
            parentMp.switchedChild = child.switched;
            childrenWithMergeInfo.put(parentPath, parentMp);
        }
        List<File> files = context.getNodeChildren(parentPath, false);
        for (File file : files) {
            MergePath siblingMp = getChildWithMergeinfo(childrenWithMergeInfo, file);
            if (siblingMp == null) {
                if (depth == SVNDepth.FILES) {
                    if (context.readKind(file, false) != SVNNodeKind.FILE) {
                        continue;
                    }
                }
                childrenWithMergeInfo.put(file, new MergePath(file));
            }
        }
    }

    private MergePath getChildWithMergeinfo(Map<File, MergePath> childrenWithMergeInfo, File path) {
        return childrenWithMergeInfo.get(path);
    }

    private void populateRemainingRanges(Map<File, MergePath> childrenWithMergeInfo, SVNURL sourceRootURL,
            SVNURL url1, long revision1, SVNURL url2, long revision2, 
            boolean honorMergeInfo, SVNRepository repository, String parentMergeSrcCanonPath) throws SVNException {

        if (!honorMergeInfo || recordOnly) {
            int index = 0;
            Object[] childrenWithMergeInfoArray = childrenWithMergeInfo.values().toArray();
            for (Iterator<MergePath> childrenIter = childrenWithMergeInfo.values().iterator(); childrenIter.hasNext();) {
                MergePath child = childrenIter.next();
                SVNMergeRange range = new SVNMergeRange(revision1, revision2, true);
                child.remainingRanges = new SVNMergeRangeList(range);
                if (index == 0) {
                    boolean indirect[] = { false };
                    Map<String, SVNMergeRangeList>[] mergeInfo = getFullMergeInfo(false, true, indirect, SVNMergeInfoInheritance.INHERITED, repository, 
                            child.absPath, Math.max(revision1, revision2), Math.min(revision1, revision2));
                    child.implicitMergeInfo = mergeInfo[1];
                } else {
                    int parentIndex = findNearestAncestor(childrenWithMergeInfoArray, false, child.absPath);
                    MergePath parent = (MergePath) childrenWithMergeInfoArray[parentIndex];
                    boolean childInheritsImplicit = parent != null && !child.switched;
                    ensureImplicitMergeinfo(parent, child, childInheritsImplicit, revision1, revision2, repository);
                }
                index++;
            }           
            return;
        }
        
        long[] gap = new long[2];
        findGapsInMergeSourceHistory(gap, parentMergeSrcCanonPath, url1, revision1, url2, revision2, repository);
        if (gap[0] >= 0 && gap[1] >= 0) {
            implicitSrcGap = new SVNMergeRangeList(gap[0], gap[1], true);
        }
        int index = 0;
        
        for (Iterator<MergePath> childrenIter = childrenWithMergeInfo.values().iterator(); childrenIter.hasNext();) {
            MergePath child = childrenIter.next();
            if (child == null || child.absent) {
                index++;
                continue;
            }
            
            String childRelativePath = null;
            if (targetAbsPath.equals(child.absPath)) {
                childRelativePath = "";
            } else {
                childRelativePath = SVNPathUtil.getRelativePath(targetAbsPath.getAbsolutePath(), child.absPath.getAbsolutePath());
            }
            MergePath parent = null;
            SVNURL childURL1 = url1.appendPath(childRelativePath, false);
            SVNURL childURL2 = url2.appendPath(childRelativePath, false);
            
            boolean inherited[] = { false };
            Map mergeInfo[] = getFullMergeInfo(child.preMergeMergeInfo == null, index == 0, inherited, SVNMergeInfoInheritance.INHERITED, 
                    repository, child.absPath, Math.max(revision1, revision2), Math.min(revision1, revision2));
        
            if (child.preMergeMergeInfo == null) {
                child.preMergeMergeInfo = mergeInfo[0];
            }
            if (index == 0) {
                child.implicitMergeInfo = mergeInfo[1];
            }
            child.inheritedMergeInfo = inherited[0];

            if (index > 0) {
                Object[] childrenWithMergeInfoArray = childrenWithMergeInfo.values().toArray();
                int parentIndex = findNearestAncestor(childrenWithMergeInfoArray, false, child.absPath);
                if (parentIndex >= 0 && parentIndex < childrenWithMergeInfoArray.length) {
                    parent = (MergePath) childrenWithMergeInfoArray[parentIndex];
                }                
            }
            boolean childInheritsImplicit = parent != null && !child.switched;
            calculateRemainingRanges(parent, child, sourceRootURL, childURL1, revision1,
                    childURL2, revision2, child.preMergeMergeInfo, implicitSrcGap, 
                    index > 0, childInheritsImplicit, repository);

            if (child.remainingRanges.getSize() > 0  && implicitSrcGap != null) {
                long start, end;
                boolean properSubset = false;
                boolean equals = false;
                boolean overlapsOrAdjoins = false;
                
                if (revision1 > revision2) {
                    child.remainingRanges.reverse();
                }
                for(int j = 0; j < child.remainingRanges.getSize(); j++) {
                    start = child.remainingRanges.getRanges()[j].getStartRevision();
                    end = child.remainingRanges.getRanges()[j].getEndRevision();
                    
                    if ((start <= gap[0] && gap[1] < end) || (start < gap[0] && gap[1] <= end)) {
                        properSubset = true;
                        break;
                    } else if (gap[0] == start && gap[1] == end) {
                        equals = true;
                        break;
                    } else if (gap[0] <= end && start <= gap[1]) {
                        overlapsOrAdjoins = true;
                        break;
                    }
                }
                if (!properSubset) {
                    if (overlapsOrAdjoins) {
                        child.remainingRanges = child.remainingRanges.merge(implicitSrcGap);
                    } else if (equals) {
                        child.remainingRanges = child.remainingRanges.diff(implicitSrcGap, false);
                    }
                }
                if (revision1 > revision2) {
                    child.remainingRanges.reverse();
                }
            }
            index++;
        }
    }
    
    protected Map<String, SVNMergeRangeList>[] getFullMergeInfo(boolean getRecorded, boolean getImplicit, boolean[] inherited, SVNMergeInfoInheritance inherit, SVNRepository repos, File target, long start, long end) throws SVNException {
        Map<String, SVNMergeRangeList>[] result = new Map[2];
        SVNErrorManager.assertionFailure(SVNRevision.isValidRevisionNumber(start) && SVNRevision.isValidRevisionNumber(end) && start > end, null, SVNLogType.WC);
        
        if (getRecorded) {
            SvnMergeInfoCatalogInfo catalog = SvnNgMergeinfoUtil.getWcOrReposMergeInfoCatalog(context, repos, target, false, false, false, inherit);
            if (catalog != null) {
                result[0] = catalog.catalog != null ? catalog.catalog.values().iterator().next() : null;
                inherited[0] = catalog.inherited;
            }
        }

        if (getImplicit) {
            File reposRelPath = null;
            SVNURL reposRootUrl = null;
            long targetRevision = -1;
            Structure<NodeOriginInfo> originInfo = context.getNodeOrigin(target, false, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl);
            reposRelPath = originInfo.get(NodeOriginInfo.reposRelpath);
            reposRootUrl = originInfo.get(NodeOriginInfo.reposRootUrl);
            targetRevision = originInfo.lng(NodeOriginInfo.revision);
            if (reposRelPath == null) {
                result[1] = new TreeMap<String, SVNMergeRangeList>();
            } else if (targetRevision <= end) {
                result[1] = new TreeMap<String, SVNMergeRangeList>();
            } else {
                SVNURL url = SVNWCUtils.join(reposRootUrl, reposRelPath);
                SVNURL sessionUrl = ensureSessionURL(repos, url);
                if (targetRevision < start) {
                    start = targetRevision;
                }
                result[1] = repositoryAccess.getHistoryAsMergeInfo(repos, SvnTarget.fromURL(url, SVNRevision.create(targetRevision)), start, end);
                ensureSessionURL(repos, sessionUrl);
            }
        }
        return result;
    }

    public Map calculateImplicitMergeInfo(SVNRepository repos, SVNURL url, long[] targetRev, long start, long end) throws SVNException {
        Map implicitMergeInfo = null;
        boolean closeSession = false;
        SVNURL sessionURL = null;
        try {
            if (repos != null) {
                sessionURL = ensureSessionURL(repos, url);
            } else {
                repos = repositoryAccess.createRepository(url, null, false);
                closeSession = true;
            }

            if (targetRev[0] < start) {
                repositoryAccess.getLocations(repos, SvnTarget.fromURL(url), SVNRevision.create(targetRev[0]), SVNRevision.create(start), SVNRevision.UNDEFINED);
                targetRev[0] = start;
            }
            implicitMergeInfo = repositoryAccess.getHistoryAsMergeInfo(repos, SvnTarget.fromURL(url, SVNRevision.create(targetRev[0])), start, end);
            if (sessionURL != null) {
                repos.setLocation(sessionURL, false);
            }
        } finally {
            if (closeSession) {
                repos.closeSession();
            }
        }
        return implicitMergeInfo;
    }

    private void inheritImplicitMergeinfoFromParent(MergePath parent, MergePath child, long revision1, long revision2, SVNRepository repository) throws SVNException {
        if (parent.implicitMergeInfo == null) {
            Map[] mergeinfo = getFullMergeInfo(false, true, null, SVNMergeInfoInheritance.INHERITED, repository, parent.absPath, 
                    Math.max(revision1, revision2), Math.min(revision1, revision2));
            parent.implicitMergeInfo = mergeinfo[1];
        }
        child.implicitMergeInfo = new TreeMap<String, SVNMergeRangeList>();
        
        String ancestorPath = SVNPathUtil.getCommonPathAncestor(parent.absPath.getAbsolutePath().replace(File.separatorChar, '/'), child.absPath.getAbsolutePath().replace(File.separatorChar, '/')); 
        String childPath = SVNPathUtil.getPathAsChild(ancestorPath, child.absPath.getAbsolutePath().replace(File.separatorChar, '/'));
        if (childPath.startsWith("/")) {
            childPath = childPath.substring(1);
        }
        SVNMergeInfoUtil.adjustMergeInfoSourcePaths(child.implicitMergeInfo, childPath, parent.implicitMergeInfo);
    }
    
    
    private void ensureImplicitMergeinfo(MergePath parent, MergePath child, boolean childInheritsParent, long revision1, long revision2, SVNRepository repository) throws SVNException {
        if (child.implicitMergeInfo != null) {
            return;
        }
        if (childInheritsParent) {
            inheritImplicitMergeinfoFromParent(parent, child, revision1, revision2, repository);
        } else {
            Map<String, SVNMergeRangeList>[] mergeinfo = getFullMergeInfo(false, true, null, SVNMergeInfoInheritance.INHERITED, repository, child.absPath, Math.max(revision1, revision2), Math.min(revision1, revision2));
            child.implicitMergeInfo = mergeinfo[1];
        }
    }
    protected void findGapsInMergeSourceHistory(long[] gap, String mergeSrcCanonPath, SVNURL url1, long rev1, SVNURL url2, long rev2, SVNRepository repos) throws SVNException {
        long youngRev = Math.max(rev1, rev2);
        long oldRev = Math.min(rev1, rev2);
        SVNURL url = rev2 < rev1 ? url1 : url2;
        gap[0] = gap[1] = -1;
        SVNRevision pegRevision = SVNRevision.create(youngRev);
        
        SVNURL oldURL = null;
        if (repos != null) {
            oldURL = ensureSessionURL(repos, url);            
        }
        Map implicitSrcMergeInfo = null;
        try {
           implicitSrcMergeInfo = repositoryAccess.getHistoryAsMergeInfo(repos, SvnTarget.fromURL(url, pegRevision), youngRev, oldRev);
        } finally {
            if (repos != null && oldURL != null) {
                repos.setLocation(oldURL, false);
            }
        }
        SVNMergeRangeList rangelist = (SVNMergeRangeList) implicitSrcMergeInfo.get(mergeSrcCanonPath);
        if (rangelist != null) {
            if (rangelist.getSize() > 1) {
                gap[0] = Math.min(rev1, rev2);
                gap[1] = rangelist.getRanges()[rangelist.getSize() - 1].getStartRevision();
            } else if (implicitSrcMergeInfo.size() > 1) {
                SVNMergeRangeList implicitMergeRangeList = new SVNMergeRangeList(new SVNMergeRange[0]);
                SVNMergeRangeList requestedMergeRangeList = new SVNMergeRangeList(Math.min(rev1, rev2), Math.max(rev1, rev2), true);
                for(Iterator paths = implicitSrcMergeInfo.keySet().iterator(); paths.hasNext();) {
                    String path = (String) paths.next();
                    rangelist = (SVNMergeRangeList) implicitSrcMergeInfo.get(path);
                    implicitMergeRangeList = implicitMergeRangeList != null ? implicitMergeRangeList.merge(rangelist) : rangelist;
                }
                SVNMergeRangeList gapRangeList = requestedMergeRangeList.diff(implicitMergeRangeList, false);
                if (gapRangeList.getSize() > 0) {
                    gap[0] = gapRangeList.getRanges()[0].getStartRevision();
                    gap[1] = gapRangeList.getRanges()[0].getEndRevision();
                }
            }
        }
    }

//    public void calculateRemainingRanges(MergePath parent, MergePath child, MergeSource source, Map<String, SVNMergeRangeList>  targetMergeInfo, SVNMergeRangeList implicitSrcGap, boolean childInheritsImplicit, SVNRepository repository) {
//        SVNURL primaryUrl = (source.rev1 < source.rev2) ? source.url2 : source.url1;
//        long primaryRevision = (source.rev1 < source.rev2) ? source.rev2 : source.rev1;
//        String mergeInfoPath = getPathRelativeToRoot();
//    }
    
    public void calculateRemainingRanges(MergePath parent, MergePath child, SVNURL sourceRootURL, SVNURL url1, long revision1,
            SVNURL url2, long revision2, Map targetMergeInfo, SVNMergeRangeList implicitSrcGap, 
            boolean isSubtree, boolean childInheritsImplicit, SVNRepository repository) throws SVNException {
        SVNURL primaryURL = revision1 < revision2 ? url2 : url1;
        Map adjustedTargetMergeInfo = null;

        String mergeInfoPath = getPathRelativeToRoot(primaryURL, sourceRootURL, repository);
        if (implicitSrcGap != null && child.preMergeMergeInfo != null) {
            SVNMergeRangeList explicitMergeInfoGapRanges = (SVNMergeRangeList) child.preMergeMergeInfo.get(mergeInfoPath);
            if (explicitMergeInfoGapRanges != null) {
                Map gapMergeInfo = new TreeMap();
                gapMergeInfo.put(mergeInfoPath, implicitSrcGap);
                adjustedTargetMergeInfo = SVNMergeInfoUtil.removeMergeInfo(gapMergeInfo, targetMergeInfo, false);
            }
        } else {
            adjustedTargetMergeInfo = targetMergeInfo;
        }
        
        filterMergedRevisions(parent, child, repository, mergeInfoPath, 
                adjustedTargetMergeInfo, revision1, revision2, childInheritsImplicit); 
        
        long childBaseRevision = context.getNodeBaseRev(child.absPath);
        if (childBaseRevision >= 0 &&
                (child.remainingRanges == null || child.remainingRanges.isEmpty()) &&
                (revision2 < revision1) &&
                (childBaseRevision <= revision2)) {
            try {
                Structure<LocationsInfo> locations = repositoryAccess.getLocations(repository, SvnTarget.fromURL(url1), SVNRevision.create(revision1), SVNRevision.create(childBaseRevision), SVNRevision.UNDEFINED);
                SVNURL startURL = locations.get(LocationsInfo.startUrl);
                if (startURL.equals(context.getNodeUrl(child.absPath))) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, 
                            "Cannot reverse-merge a range from a path's own future history; try updating first");
                    SVNErrorManager.error(err, SVNLogType.DEFAULT);
                }
            } catch (SVNException svne) {
                SVNErrorCode code = svne.getErrorMessage().getErrorCode();
                if (!(code == SVNErrorCode.FS_NOT_FOUND || code == SVNErrorCode.RA_DAV_PATH_NOT_FOUND || 
                        code == SVNErrorCode.CLIENT_UNRELATED_RESOURCES)) {
                    throw svne;
                }
            }
        }
    }
    private void adjustDeletedSubTreeRanges(MergePath child, MergePath parent, long revision1, long revision2, 
            SVNURL primaryURL, SVNRepository repository) throws SVNException {

        SVNErrorManager.assertionFailure(parent.remainingRanges != null, "parent must already have non-null remaining ranges set", SVNLogType.WC);

        String relativePath = getPathRelativeToRoot(primaryURL, repository.getLocation(), repository);
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        
        boolean isRollback = revision2 < revision1;
        long pegRev = isRollback ? revision1 : revision2;
        long youngerRev = pegRev;
        long olderRev = isRollback ? revision2 : revision1;
        
        List locationSegments = null;
        try {
            locationSegments = repository.getLocationSegments(relativePath, pegRev, youngerRev, olderRev);
        } catch (SVNException e) {
            SVNErrorCode errCode = e.getErrorMessage().getErrorCode();
            if (errCode == SVNErrorCode.FS_NOT_FOUND || errCode == SVNErrorCode.RA_DAV_REQUEST_FAILED) {
                SVNNodeKind kind = repository.checkPath(relativePath, olderRev);
                if (kind == SVNNodeKind.NONE) {
                    child.remainingRanges = parent.remainingRanges.dup();
                } else {
                    long primaryURLDeletedRevision = repository.getDeletedRevision(relativePath, olderRev, youngerRev);
                    SVNErrorManager.assertionFailure(SVNRevision.isValidRevisionNumber(primaryURLDeletedRevision), "deleted revision must exist", SVNLogType.WC);
                    if (isRollback) {
                        child.remainingRanges = child.remainingRanges.reverse();
                        parent.remainingRanges = parent.remainingRanges.reverse();
                    }
                    
                    SVNMergeRangeList existingRangeList = new SVNMergeRangeList(new SVNMergeRange(olderRev, primaryURLDeletedRevision - 1, true));
                    child.remainingRanges = child.remainingRanges.intersect(existingRangeList, false);
                    
                    SVNMergeRangeList deletedRangeList = new SVNMergeRangeList(new SVNMergeRange(primaryURLDeletedRevision - 1, pegRev, true));
                    deletedRangeList = parent.remainingRanges.intersect(deletedRangeList, false);
                    child.remainingRanges = child.remainingRanges.merge(deletedRangeList);
                    
                    if (isRollback) {
                        child.remainingRanges = child.remainingRanges.reverse();
                        parent.remainingRanges = parent.remainingRanges.reverse();
                    }
                }
            } else {
                throw e;            
            }
        }
        
        if (locationSegments != null && !locationSegments.isEmpty()) {
            SVNLocationSegment segment = (SVNLocationSegment) locationSegments.get(locationSegments.size() - 1);
            if (segment.getStartRevision() == olderRev) {
                return;
            }
            if (isRollback) {
                child.remainingRanges = child.remainingRanges.reverse();
                parent.remainingRanges = parent.remainingRanges.reverse();
            }
            
            SVNMergeRangeList existingRangeList = new SVNMergeRangeList(new SVNMergeRange(segment.getStartRevision(), pegRev, true));
            child.remainingRanges = child.remainingRanges.intersect(existingRangeList, false);
            SVNMergeRangeList nonExistentRangeList = new SVNMergeRangeList(new SVNMergeRange(olderRev, segment.getStartRevision(), true));
            nonExistentRangeList = parent.remainingRanges.intersect(nonExistentRangeList, false);
            child.remainingRanges = child.remainingRanges.merge(nonExistentRangeList);

            if (isRollback) {
                child.remainingRanges = child.remainingRanges.reverse();
                parent.remainingRanges = parent.remainingRanges.reverse();
            }
        }
    }

    private void filterMergedRevisions(MergePath parent, MergePath child, SVNRepository repository, String mergeInfoPath, Map targetMergeInfo,
            long rev1, long rev2, boolean childInheritsImplicit) throws SVNException {
        SVNMergeRangeList targetRangeList = null;        
        SVNMergeRangeList targetImplicitRangeList = null;        
        SVNMergeRangeList explicitRangeList = null;        
        SVNMergeRangeList requestedMergeRangeList = new SVNMergeRangeList(new SVNMergeRange(rev1, rev2, true));


        if (rev1 > rev2) {
            requestedMergeRangeList = requestedMergeRangeList.reverse();
            if (targetMergeInfo != null) {
                targetRangeList = (SVNMergeRangeList) targetMergeInfo.get(mergeInfoPath);
            } else {
                targetRangeList = null;
            }

            if (targetRangeList != null) {
                explicitRangeList = targetRangeList.intersect(requestedMergeRangeList, false);
            } else {
                explicitRangeList = new SVNMergeRangeList(new SVNMergeRange[0]);
            }

            SVNMergeRangeList[] diff = SVNMergeInfoUtil.diffMergeRangeLists(requestedMergeRangeList, explicitRangeList, false);
            SVNMergeRangeList deletedRangeList = diff[0];
            
            if (deletedRangeList == null || deletedRangeList.isEmpty()) {
                requestedMergeRangeList = requestedMergeRangeList.reverse();
                child.remainingRanges = requestedMergeRangeList.dup();
            } else {
                SVNMergeRangeList implicitRangeList = null;
                ensureImplicitMergeinfo(parent, child, childInheritsImplicit, rev1, rev2, repository);
                targetImplicitRangeList = (SVNMergeRangeList) child.implicitMergeInfo.get(mergeInfoPath);
                
                if (targetImplicitRangeList != null) {
                    implicitRangeList = targetImplicitRangeList.intersect(requestedMergeRangeList, false);
                } else {
                    implicitRangeList = new  SVNMergeRangeList(new SVNMergeRange[0]);
                }
                implicitRangeList = implicitRangeList.merge(explicitRangeList);
                implicitRangeList = implicitRangeList.reverse();
                child.remainingRanges = implicitRangeList.dup();
            }
            
        } else {
            if (targetMergeInfo != null) {
                targetRangeList = (SVNMergeRangeList) targetMergeInfo.get(mergeInfoPath);
            } else {
                targetRangeList = null;
            }
            if (targetRangeList != null) {
                explicitRangeList = requestedMergeRangeList.remove(targetRangeList, false);
            } else {
                explicitRangeList = requestedMergeRangeList.dup();
            }
            if (explicitRangeList == null || explicitRangeList.isEmpty()) {
                child.remainingRanges = new SVNMergeRangeList(new SVNMergeRange[0]);
            } else {
                if (false /*TODO: diffOptions.isAllowAllForwardMergesFromSelf()*/) {
                    child.remainingRanges = explicitRangeList.dup();                
                } else {
                    ensureImplicitMergeinfo(parent, child, childInheritsImplicit, rev1, rev2, repository);
                    targetImplicitRangeList = (SVNMergeRangeList) child.implicitMergeInfo.get(mergeInfoPath);
                    if (targetImplicitRangeList != null) {
                        child.remainingRanges = explicitRangeList.remove(targetImplicitRangeList, false);
                    } else {
                        child.remainingRanges = explicitRangeList.dup();
                    }
                }
            }
        }
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

    private void sliceRemainingRanges(Map<File, MergePath> childrenWithMergeInfo, boolean isRollBack, long endRevision) {
        for (MergePath child:  childrenWithMergeInfo.values()) {
            if (child == null || child.absent) {
                continue;
            }
            
            if (!child.remainingRanges.isEmpty()) {
                SVNMergeRange[] originalRemainingRanges = child.remainingRanges.getRanges();
                SVNMergeRange range = originalRemainingRanges[0];
                if ((isRollBack && range.getStartRevision() > endRevision && 
                        range.getEndRevision() < endRevision) ||
                        (!isRollBack && range.getStartRevision() < endRevision && 
                                range.getEndRevision() > endRevision)) {
                    SVNMergeRange splitRange1 = new SVNMergeRange(range.getStartRevision(), endRevision, 
                            range.isInheritable());
                    SVNMergeRange splitRange2 = new SVNMergeRange(endRevision, range.getEndRevision(), 
                            range.isInheritable());
                    SVNMergeRange[] remainingRanges = new SVNMergeRange[originalRemainingRanges.length + 1];
                    remainingRanges[0] = splitRange1;
                    remainingRanges[1] = splitRange2;
                    System.arraycopy(originalRemainingRanges, 1, remainingRanges, 2, 
                            originalRemainingRanges.length - 1);
                    child.remainingRanges = new SVNMergeRangeList(remainingRanges);
                }
            }
        }
    }
    
    private long getMostInclusiveEndRevision(Map<File, MergePath> childrenWithMergeInfo, boolean isRollBack) {
        long endRev = SVNRepository.INVALID_REVISION;
        for (MergePath child : childrenWithMergeInfo.values()) {
            if (child == null || child.absent) {
                continue;
            }
            if (child.remainingRanges.getSize() > 0) {
                SVNMergeRange ranges[] = child.remainingRanges.getRanges();
                SVNMergeRange range = ranges[0];
                if (!SVNRevision.isValidRevisionNumber(endRev) || 
                        (isRollBack && range.getEndRevision() > endRev) ||
                        (!isRollBack && range.getEndRevision() < endRev)) {
                    endRev = range.getEndRevision();
                }
            }
        }
        return endRev;
    }

    private long getMostInclusiveStartRevision(Map<File, MergePath> childrenWithMergeInfo, boolean isRollBack) {
        long startRev = SVNRepository.INVALID_REVISION;
        boolean first = true;
        for (MergePath child: childrenWithMergeInfo.values()) {
            if (child == null || child.absent) {
                first = false;
                continue;
            }
            if (child.remainingRanges.isEmpty()) {
                first = false;
                continue;
            }
            SVNMergeRange ranges[] = child.remainingRanges.getRanges();
            SVNMergeRange range = ranges[0];
            if (first && range.getStartRevision() == range.getEndRevision()) {
                first = false;
                continue;
            }
            if (!SVNRevision.isValidRevisionNumber(startRev) || 
                    (isRollBack && range.getStartRevision() > startRev) ||
                    (!isRollBack && range.getStartRevision() < startRev)) {
                startRev = range.getStartRevision();
            }
            first = false;
        }
        return startRev;
    }

    private void processChildrenWithNewMergeInfo(Map<File, MergePath> childrenWithMergeInfo) throws SVNException {
        if (pathsWithNewMergeInfo != null && !dryRun) {
            for (Iterator<File> pathsIter = pathsWithNewMergeInfo.iterator(); pathsIter.hasNext();) {
                File pathWithNewMergeInfo = pathsIter.next();
                SvnMergeInfoInfo pathExplicitMergeInfo = SvnNgMergeinfoUtil.getWCMergeInfo(context, pathWithNewMergeInfo, targetAbsPath, SVNMergeInfoInheritance.EXPLICIT, false);
                
                SVNURL oldURL = null;
                if (pathExplicitMergeInfo != null) {
                    oldURL = ensureSessionURL(repos2, context.getNodeUrl(pathWithNewMergeInfo));
                    Map<String, SVNMergeRangeList> pathInheritedMergeInfo = SvnNgMergeinfoUtil.getWCOrReposMergeInfo(context, pathWithNewMergeInfo, repos2, false, SVNMergeInfoInheritance.NEAREST_ANCESTOR);
                    if (pathInheritedMergeInfo != null) {
                        pathExplicitMergeInfo.mergeinfo = SVNMergeInfoUtil.mergeMergeInfos(pathExplicitMergeInfo.mergeinfo, pathInheritedMergeInfo);
                        String value = SVNMergeInfoUtil.formatMergeInfoToString(pathExplicitMergeInfo.mergeinfo, "");
                        SvnNgPropertiesManager.setProperty(context, pathWithNewMergeInfo, SVNProperty.MERGE_INFO, SVNPropertyValue.create(value), SVNDepth.EMPTY, true, null, null);
                    }
                
                    MergePath newChild = new MergePath(pathWithNewMergeInfo);
                    if (!childrenWithMergeInfo.containsKey(newChild.absPath)) {
                        Object[] childrenWithMergeInfoArray = childrenWithMergeInfo.values().toArray();
                        int parentIndex = findNearestAncestor(childrenWithMergeInfoArray, false, pathWithNewMergeInfo);
                        MergePath parent = (MergePath) childrenWithMergeInfoArray[parentIndex];
                        newChild.remainingRanges = parent.remainingRanges.dup();
                        childrenWithMergeInfo.put(newChild.absPath, newChild);
                    }
                }
                
                if (oldURL != null) {
                    repos2.setLocation(oldURL, false);
                }
            }
        }
    }

    private void removeChildrenWithDeletedMergeInfo(Map<File, MergePath> childrenWithMergeInfo) {
        if (pathsWithDeletedMergeInfo != null && !dryRun) {
            Iterator<MergePath> children = childrenWithMergeInfo.values().iterator();
            children.next(); // skip first.
            while(children.hasNext()) {
                MergePath path = children.next();
                if (path != null && pathsWithDeletedMergeInfo.contains(path.absPath)) {
                    children.remove();             
                }
            }
        }
    }
    
    private void removeFirstRangeFromRemainingRanges(long endRevision, Map<File, MergePath> childrenWithMergeInfo) {
        for (Iterator<MergePath> children = childrenWithMergeInfo.values().iterator(); children.hasNext();) {
            MergePath child = (MergePath) children.next();
            if (child == null || child.absent) {
                continue;
            }
            if (!child.remainingRanges.isEmpty()) {
                SVNMergeRange[] originalRemainingRanges = child.remainingRanges.getRanges();
                SVNMergeRange firstRange = originalRemainingRanges[0]; 
                if (firstRange.getEndRevision() == endRevision) {
                    SVNMergeRange[] remainingRanges = new SVNMergeRange[originalRemainingRanges.length - 1];
                    System.arraycopy(originalRemainingRanges, 1, remainingRanges, 0, 
                            originalRemainingRanges.length - 1);
                    child.remainingRanges = new SVNMergeRangeList(remainingRanges);
                }
            }
        }
    }

    private SVNErrorMessage makeMergeConflictError(File targetPath, SVNMergeRange range) {
        SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, 
                "One or more conflicts were produced while merging r{0}:{1} into\n" + 
                "''{2}'' --\n" +
                "resolve all conflicts and rerun the merge to apply the remaining\n" + 
                "unmerged revisions", new Object[] { Long.toString(range.getStartRevision()), 
                Long.toString(range.getEndRevision()), targetPath} );
        return error;
    }

    private void removeAbsentChildren(File targetWCPath, Map<File, MergePath> childrenWithMergeInfo) {
        for (Iterator<File> children = childrenWithMergeInfo.keySet().iterator(); children.hasNext();) {
            File childPath = children.next();
            MergePath child = (MergePath) childrenWithMergeInfo.get(childPath);
            String topDir = targetWCPath.getAbsolutePath().replace(File.separatorChar, '/');
            String childPathStr = child.absPath.getAbsolutePath().replace(File.separatorChar, '/');
            if (child != null && (child.absent || child.scheduledForDeletion) && 
                    SVNPathUtil.isAncestor(topDir, childPathStr)) {
                children.remove();
            }
        }
    }

    protected void recordMergeInfoForDirectoryMerge(Map<File, Map<String, SVNMergeRangeList>> resultCatalog, SVNMergeRange mergeRange, String mergeInfoPath, SVNDepth depth, boolean squelchMergeinfoNotifications, Map<File, MergePath> childrenWithMergeInfo) throws SVNException {
        boolean isRollBack = mergeRange.getStartRevision() > mergeRange.getEndRevision();
        boolean operativeMerge = isSubtreeTouchedByMerge(targetAbsPath);
        
        SVNMergeRange range = mergeRange.dup();
        if (!operativeMerge) {
            range.setInheritable(true);
        }
        removeAbsentChildren(targetAbsPath, childrenWithMergeInfo);

        flagSubTreesNeedingMergeInfo(operativeMerge, range, childrenWithMergeInfo, SVNFileUtil.createFilePath(mergeInfoPath), depth);
        List<Map.Entry<File, MergePath>> entries = new ArrayList<Map.Entry<File, MergePath>>(childrenWithMergeInfo.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            final Map.Entry<File, MergePath> entry = entries.get(i);
            MergePath child = entry.getValue();

            assert child != null;

            if (child.recordMergeInfo) {
                File childReposPath = SVNFileUtil.skipAncestor(targetAbsPath, child.absPath);
                assert childReposPath != null;

                File childMergeSrcPath = SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(mergeInfoPath), childReposPath);

                SVNMergeRangeList childMergeRangelist = filterNaturalHistoryFromMergeInfo(SVNFileUtil.getFilePath(childMergeSrcPath), child.implicitMergeInfo, range);
                if (childMergeRangelist.getSize() == 0) {
                    continue;
                }
                if (!squelchMergeinfoNotifications) {
                    removeSourceGap(range, implicitSrcGap);
                    SVNEvent mergeBeginEvent = SVNEventFactory.createSVNEvent(child.absPath,
                            SVNNodeKind.NONE,
                            null, -1, SVNEventAction.MERGE_RECORD_INFO_BEGIN,
                            null, null, range);
                    if (context.getEventHandler() != null) {
                        context.getEventHandler().handleEvent(mergeBeginEvent, -1);
                    }
                }

                if (i == 0) {
                    recordSkips(mergeInfoPath, childMergeRangelist, isRollBack);
                }

                if (child.recordNonInheritable) {
                    childMergeRangelist.setInheritable(false);
                }
                if (child.inheritedMergeInfo) {
                    recordMergeinfo(child.absPath, child.preMergeMergeInfo, false);
                }
                if (implicitSrcGap != null) {
                    if (isRollBack) {
                        childMergeRangelist = childMergeRangelist.reverse();
                    }
                    childMergeRangelist = childMergeRangelist.remove(implicitSrcGap, false);
                    if (isRollBack) {
                        childMergeRangelist = childMergeRangelist.reverse();
                    }
                }
                Map<File, SVNMergeRangeList> childMerges = new TreeMap<File, SVNMergeRangeList>(PATH_COMPARATOR);
                if ((!recordOnly || reintegrateMerge) && !isRollBack) {
                    SVNURL subtreeMergeUrl = reposRootUrl.appendPath(SVNFileUtil.getFilePath(childMergeSrcPath), false);
                    SVNURL oldUrl = ensureSessionURL(repos2, subtreeMergeUrl);
                    Map<String, SVNMergeRangeList> subtreeHistory = null;
                    try {
                        subtreeHistory = repositoryAccess.getHistoryAsMergeInfo(repos2, SvnTarget.fromURL(subtreeMergeUrl, SVNRevision.create(mergeRange.getEndRevision())),
                                Math.max(mergeRange.getStartRevision(), mergeRange.getEndRevision()),
                                Math.min(mergeRange.getStartRevision(), mergeRange.getEndRevision()));
                        SVNMergeRangeList childMergeSrcRangelist = subtreeHistory.get(SVNFileUtil.getFilePath(childMergeSrcPath));
                        childMergeRangelist = childMergeRangelist.intersect(childMergeSrcRangelist, false);
                        if (child.recordNonInheritable) {
                            childMergeRangelist.setInheritable(false);
                        }
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.FS_NOT_FOUND) {
                            throw e;
                        }
                    }
                    if (oldUrl != null) {
                        repos2.setLocation(oldUrl, false);
                    }
                }
                childMerges.put(child.absPath, childMergeRangelist);
                updateWCMergeInfo(resultCatalog, child.absPath, SVNFileUtil.getFilePath(childMergeSrcPath), childMerges, isRollBack);
                if (addedPaths != null) {
                    addedPaths.remove(child.absPath);
                }
            }
            if (i > 0) {
                boolean inSwitchedSubtree = false;
                if (child.switched) {
                    inSwitchedSubtree = true;
                } else if (i > 1) {
                    int j = i - 1;
                    for(; j > 0; j--) {
                        MergePath parent = (MergePath) entries.get(j).getValue();
                        if (parent != null && parent.switched && SVNWCUtils.isAncestor(parent.absPath, child.absPath)) {
                            inSwitchedSubtree = true;
                            break;
                        }
                    }
                }
                SvnNgMergeinfoUtil.elideMergeInfo(context, repos1, child.absPath, inSwitchedSubtree ? null : targetAbsPath);
            }
        }
    }

    private void removeSourceGap(SVNMergeRange range, SVNMergeRangeList implicitSrcGap) {
        if (implicitSrcGap != null) {
            SVNMergeRange gapRange = implicitSrcGap.getRanges()[0];
            if (range.getStartRevision() < range.getEndRevision()) {
                if (gapRange.getStartRevision() == range.getStartRevision()) {
                    range.setStartRevision(gapRange.getEndRevision());
                }
            } else {
                if (gapRange.getStartRevision() == range.getEndRevision()) {
                    range.setEndRevision(gapRange.getEndRevision());
                }
            }
        }
    }

    private void flagSubTreesNeedingMergeInfo(boolean operativeMerge, SVNMergeRange mergeRange,
                                              Map<File, MergePath> childrenWithMergeInfo,
                                              File mergeInfoPath,
                                              SVNDepth depth) throws SVNException {
        assert !dryRun;

        Map<File, String> operativeImmediateChildren = null;
        if (!recordOnly && mergeRange.getStartRevision() <= mergeRange.getEndRevision() &&
                (depth.compareTo(SVNDepth.INFINITY) < 0)) {
            operativeImmediateChildren = getOperativeImmediateChildren(mergeInfoPath, mergeRange.getStartRevision() + 1, mergeRange.getEndRevision(), targetAbsPath, depth, repos1);
        }
        List<Map.Entry<File, MergePath>> entries = new ArrayList<Map.Entry<File, MergePath>>(childrenWithMergeInfo.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            MergePath child = entries.get(i).getValue();

            if (child.absent) {
                continue;
            }
            assert((i == 0) || pathsWithDeletedMergeInfo == null || !pathsWithDeletedMergeInfo.contains(child.absPath));

            if (skippedPaths != null && skippedPaths.contains(child.absPath)) {
                continue;
            }

            if (i == 0) {
                child.recordMergeInfo = true;
            } else if (recordOnly && !reintegrateMerge) {
                child.recordMergeInfo = true;
            } else if (child.immediateChildDir && child.preMergeMergeInfo == null && operativeImmediateChildren != null && operativeImmediateChildren.containsKey(child.absPath)) {
                child.recordMergeInfo = true;
            }

            if (operativeMerge && isSubtreeTouchedByMerge(child.absPath)) {
                child.recordMergeInfo = true;
                if (!reintegrateMerge && child.missingChild && !isPathSubtree(child.absPath, skippedPaths)) {
                    child.missingChild = false;
                }
                if (child.switchedChild) {
                    boolean operativeSwitchedChild = false;
                    for (int j = i + 1; j < entries.size(); j++) {
                        MergePath potentialChild = entries.get(j).getValue();

                        if (!SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(child.absPath), SVNFileUtil.getFilePath(potentialChild.absPath))) {
                            break;
                        }

                        if (!child.absPath.equals(SVNFileUtil.getParentFile(potentialChild.absPath))) {
                            continue;
                        }

                        if (potentialChild.switched && potentialChild.recordMergeInfo) {
                            operativeSwitchedChild = true;
                            break;
                        }
                    }

                    if (!operativeSwitchedChild) {
                        child.switchedChild = false;
                    }
                }

            }
            if (child.recordMergeInfo ) {
                SVNNodeKind pathKind = context.readKind(child.absPath, false);
                if (pathKind == SVNNodeKind.DIR) {
                    child.recordNonInheritable = child.missingChild || child.switchedChild;

                    if (i == 0) {
                        if (depth.compareTo(SVNDepth.IMMEDIATES) < 0 && operativeImmediateChildren != null && operativeImmediateChildren.size() > 0) {
                            child.recordNonInheritable = true;
                        }
                    } else if (depth == SVNDepth.IMMEDIATES) {
                        if (operativeImmediateChildren.containsKey(child.absPath)) {
                            child.recordNonInheritable = true;
                        }
                    }
                }
            } else {
                if (child.childOfNonInheritable) {
                    recordMergeinfo(child.absPath, null, false);
                }
            }
        }
    }

    private boolean isPathSubtree(File localAbsPath, Collection<File> subtrees) {
        if (subtrees != null) {
            for (File pathTouchedByMerge : subtrees) {
                if (SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(localAbsPath), SVNFileUtil.getFilePath(pathTouchedByMerge))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<File, String>  getOperativeImmediateChildren(File mergeSourceFsPath, long oldestRevision, long youngestRevision, File mergeTargetAbsPath, SVNDepth depth, SVNRepository repository) throws SVNException {
        assert SVNRevision.isValidRevisionNumber(oldestRevision);
        assert SVNRevision.isValidRevisionNumber(youngestRevision);
        assert oldestRevision <= youngestRevision;

        Map<File, String> operativeChildren = new HashMap<File, String>();

        if (depth == SVNDepth.INFINITY) {
            return operativeChildren;
        }

        FindOperativeSubtreeRevisions findOperativeSubtreeRevisions = new FindOperativeSubtreeRevisions(operativeChildren, context, mergeSourceFsPath, mergeTargetAbsPath, depth);
        repository.log(new String[]{""}, youngestRevision, oldestRevision, true, false, 0, false, null, findOperativeSubtreeRevisions);
        return operativeChildren;
    }

    private boolean calculateMergeInheritance(SVNMergeRangeList rangeList, File localAbsPath, boolean wcPathIsMergeTarget, boolean wcPathHasMissingChild, SVNDepth depth) throws SVNException {
        SVNNodeKind kind = context.readKind(localAbsPath, false);
        boolean result = true;
        if (kind == SVNNodeKind.FILE) {
            rangeList.setInheritable(true);
        } else if (kind == SVNNodeKind.DIR) {
            if (wcPathIsMergeTarget) {
                if (wcPathHasMissingChild || depth == SVNDepth.FILES || depth == SVNDepth.EMPTY) {
                    rangeList.setInheritable(false);
                    result = false;
                } else {
                    rangeList.setInheritable(true);
                }
            } else {
                if (wcPathHasMissingChild || depth == SVNDepth.IMMEDIATES) {
                    rangeList.setInheritable(false);
                    result = false;
                } else {
                    rangeList.setInheritable(true);
                }
            }
        }
        return result;
    }
    
    private void recordSkips(String mergeInfoPath, SVNMergeRangeList childMergeRangelist, boolean isRollBack) throws SVNException {
        if (skippedPaths == null || skippedPaths.isEmpty()) {
            return;
        }
        Map<File, SVNMergeRangeList> merges = new TreeMap<File, SVNMergeRangeList>(PATH_COMPARATOR);
        for (File skippedPath : skippedPaths) {
            ObstructionState os = performObstructionCheck(skippedPath, SVNNodeKind.UNKNOWN);
            if (os != null && 
                    (os.obstructionState == SVNStatusType.OBSTRUCTED || os.obstructionState == SVNStatusType.MISSING)) {
                continue;
            }
            merges.put(skippedPath, new SVNMergeRangeList(new SVNMergeRange[0]));
        }
        updateWCMergeInfo(null, targetAbsPath, mergeInfoPath, merges, isRollBack);
    }


    private void updateWCMergeInfo(Map<File, Map<String, SVNMergeRangeList>> resultCatalog, File targetAbsPath, String reposRelPath, Map<File, SVNMergeRangeList> merges, boolean isRollBack) throws SVNException {
        for (File localAbsPath : merges.keySet()) {
            Map<String, SVNMergeRangeList> mergeinfo = null;
            SVNMergeRangeList ranges = merges.get(localAbsPath);
            try {
                SVNProperties properties = context.getDb().readProperties(localAbsPath);
                String propValue = properties != null ? properties.getStringValue(SVNProperty.MERGE_INFO) : null;
                if (propValue != null) {
                    mergeinfo = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(propValue), null);
                }
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_LOCKED ||
                        e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    continue;
                }
                throw e;
            }
            if (mergeinfo == null && ranges.isEmpty()) {
                SvnMergeInfoInfo mergeinfoInfo = SvnNgMergeinfoUtil.getWCMergeInfo(context, localAbsPath, targetAbsPath, SVNMergeInfoInheritance.NEAREST_ANCESTOR, false);
                if (mergeinfoInfo != null) {
                    mergeinfo = mergeinfoInfo.mergeinfo;
                }
            }
            if (mergeinfo == null) {
                mergeinfo = new TreeMap<String, SVNMergeRangeList>();
            }
            String localAbsPathRelToTarget = SVNWCUtils.getPathAsChild(targetAbsPath, localAbsPath);
            String relPath;
            if (localAbsPathRelToTarget != null) {
                relPath = SVNPathUtil.append(reposRelPath, localAbsPathRelToTarget);
            } else {
                relPath = reposRelPath;
            }
            SVNMergeRangeList rangelist = mergeinfo.get(relPath);
            if (rangelist == null) {
                rangelist = new SVNMergeRangeList(new SVNMergeRange[0]);
            }
            if (isRollBack) {
                ranges = ranges.dup().reverse();
                rangelist = rangelist.remove(ranges, false);
            } else {
                rangelist = rangelist.merge(ranges);
            }
            mergeinfo.put(relPath, rangelist);
            if (isRollBack && mergeinfo.isEmpty()) {
                mergeinfo = null;
            }
            SVNMergeInfoUtil.removeEmptyRangeLists(mergeinfo);
            if (resultCatalog != null) {
                Map<String, SVNMergeRangeList> existingMergeInfo = resultCatalog.get(localAbsPath);
                if (existingMergeInfo != null) {
                    mergeinfo = SVNMergeInfoUtil.mergeMergeInfos(mergeinfo, existingMergeInfo);
                }
                resultCatalog.put(localAbsPath, mergeinfo);
            } else {
                try {
                    recordMergeinfo(localAbsPath, mergeinfo, true);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_NOT_FOUND) {
                        continue;
                    }
                    throw e;
                }
            }
        }   
    }


    private void recordMergeinfo(File localAbsPath, Map<String, SVNMergeRangeList> mergeinfo, boolean notify) throws SVNException {
        String mergeInfoValue = null;
        if (mergeinfo != null) {
            mergeInfoValue = SVNMergeInfoUtil.formatMergeInfoToString(mergeinfo, null);
        }                    
        boolean mergeInfoChanged = false;
        if (notify && context.getEventHandler() != null) {
            PropDiffs propDiff = context.getPropDiffs(localAbsPath);
            mergeInfoChanged = propDiff.propChanges != null && propDiff.propChanges.containsName(SVNProperty.MERGE_INFO);
        }

        ISVNEventHandler oldEventHandler = context.getEventHandler();
        context.setEventHandler(null);
        SvnNgPropertiesManager.setProperty(context, localAbsPath, SVNProperty.MERGE_INFO, 
                mergeInfoValue != null ? SVNPropertyValue.create(mergeInfoValue) : null, 
                SVNDepth.EMPTY, true, null, null);
        context.setEventHandler(oldEventHandler);
        
        if (notify && context.getEventHandler() != null) {
            SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, 
                    SVNNodeKind.UNKNOWN, null, -1, 
                    SVNStatusType.INAPPLICABLE,
                    mergeInfoChanged ? SVNStatusType.MERGED : SVNStatusType.CHANGED, 
                    SVNStatusType.LOCK_INAPPLICABLE,
                    SVNEventAction.MERGE_RECORD_INFO, 
                    null, null, null);
            context.getEventHandler().handleEvent(event, -1);
        }
    }

    private boolean isSubtreeTouchedByMerge(File absPath) {
        return isSubtree(absPath, mergedPaths) 
                || isSubtree(absPath, skippedPaths)
                || isSubtree(absPath, addedPaths)
                || isSubtree(absPath, treeConflictedPaths);
    }
    
    private boolean isSubtree(File path, Collection<File> paths) {
        if (path != null && paths != null) {
            for (File subtree : paths) {
                if (SVNWCUtils.isAncestor(path, subtree)) {
                    return true;
                }
            }
        }
        return false;
    }

    private SVNMergeRangeList filterNaturalHistoryFromMergeInfo(String srcPath, Map<String, SVNMergeRangeList> implicitMergeInfo,  SVNMergeRange requestedRange) {
        SVNMergeRangeList requestedRangeList = new SVNMergeRangeList(requestedRange.dup());
        SVNMergeRangeList filteredRangeList = null;
        if (implicitMergeInfo != null && requestedRange.getStartRevision() < requestedRange.getEndRevision()) {
            SVNMergeRangeList impliedRangeList = (SVNMergeRangeList) implicitMergeInfo.get(srcPath);
            if (impliedRangeList != null) {
                filteredRangeList = requestedRangeList.diff(impliedRangeList, false);
            }
        }
        if (filteredRangeList == null) {
            filteredRangeList = requestedRangeList;
        }
        return filteredRangeList;
    }


    private Map<File, String> getInoperativeImmediateChildrent(String mergeSourceReposAbsPath, long oldestRev, long youngestRev, File targetAbsPath, SVNRepository repos, Map<File, MergePath> childrenWithMergeInfo) throws SVNException {
        final Map<File, String> result = new TreeMap<File, String>();
        for (File childPath : childrenWithMergeInfo.keySet()) {
            MergePath child = childrenWithMergeInfo.get(childPath);
            if (child.immediateChildDir) {
                String relPath = SVNWCUtils.getPathAsChild(targetAbsPath, child.absPath);
                String fullPath = SVNPathUtil.append(mergeSourceReposAbsPath, relPath);
                result.put(child.absPath, fullPath);
            }
        }
        
        if (!result.isEmpty()) {
            repos.log(new String[] {""}, 
                    youngestRev, oldestRev, true, false, -1, false, null, new ISVNLogEntryHandler() {
                public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                    for (String changedPath : logEntry.getChangedPaths().keySet()) {
                        File removedPath = null;
                        for (Iterator<File> immediateChildAbsPath = result.keySet().iterator(); immediateChildAbsPath.hasNext();) {
                            File iPath = immediateChildAbsPath.next();
                            String reposPath = result.get(iPath);
                            if (reposPath != null && SVNPathUtil.isAncestor(reposPath, changedPath)) {
                                removedPath = iPath;
                                break;
                            }
                        }
                        if (removedPath != null) {
                            result.remove(removedPath);
                        }
                    }
                }
            });
        }
        return result;
    }

    private void recordMergeInfoForAddedSubtrees(SVNMergeRange range, String mergeInfoPath, SVNDepth depth, boolean squelchMergeinfoNotifications, Map<File, MergePath> childrenWithMergeInfo) throws SVNException {
        if (addedPaths == null) {
            return;
        }
        for (File addedPath : addedPaths) {
            File dirPath = SVNFileUtil.getFileDir(addedPath);
            SvnMergeInfoInfo miInfo = SvnNgMergeinfoUtil.getWCMergeInfo(context, addedPath, null, SVNMergeInfoInheritance.EXPLICIT, false);
            SvnMergeInfoInfo parentMiInfo = null;
            if (miInfo.mergeinfo == null) {
                parentMiInfo = SvnNgMergeinfoUtil.getWCMergeInfo(context, dirPath, null, SVNMergeInfoInheritance.EXPLICIT, false);
                miInfo.inherited = parentMiInfo.inherited;
            }
            if (miInfo.mergeinfo != null || SVNMergeInfoUtil.isNonInheritable(parentMiInfo.mergeinfo)) {
                MergePath targetMergePath = childrenWithMergeInfo.values().iterator().next();
                SVNNodeKind kind = context.readKind(addedPath, false);
                SVNMergeRange rng = range.dup();
                if (kind == SVNNodeKind.FILE) {
                    rng.setInheritable(true);
                } else {
                    rng.setInheritable(!(depth == SVNDepth.INFINITY || depth == SVNDepth.IMMEDIATES));
                }
                SVNMergeRangeList rangelist = new SVNMergeRangeList(rng);
                Map<String, SVNMergeRangeList> mergeMergeino = new TreeMap<String, SVNMergeRangeList>();
                String relAddedPath = SVNWCUtils.getPathAsChild(targetMergePath.absPath, addedPath);
                String addedMergeInfoPath = SVNPathUtil.append(mergeInfoPath, relAddedPath);
                mergeMergeino.put(addedMergeInfoPath, rangelist);
                SVNURL addedMergeInfoUrl = reposRootUrl.appendPath(addedMergeInfoPath, false);
                SVNRevision pegRevision = SVNRevision.create(Math.max(range.getStartRevision(), range.getEndRevision()));
                SVNURL oldUrl = ensureSessionURL(repos2, addedMergeInfoUrl);
                
                Map<String, SVNMergeRangeList> addsHistory = repositoryAccess.getHistoryAsMergeInfo(repos2, 
                        SvnTarget.fromURL(addedMergeInfoUrl, pegRevision), 
                        Math.max(range.getStartRevision(), range.getEndRevision()), 
                        Math.min(range.getStartRevision(), range.getEndRevision()));
                
                if (oldUrl != null) {
                    repos2.setLocation(oldUrl, false);
                }
                
                mergeMergeino = SVNMergeInfoUtil.intersectMergeInfo(mergeMergeino, addsHistory, false);
                if (miInfo.mergeinfo != null) {
                    mergeMergeino = SVNMergeInfoUtil.mergeMergeInfos(mergeMergeino, miInfo.mergeinfo);                    
                }
                recordMergeinfo(addedPath, mergeMergeino, !squelchMergeinfoNotifications);
            }
        }
    }

    private SVNMergeRangeList removeNoOpMergeRanges(SVNRepository repository, SVNMergeRangeList ranges) throws SVNException {
        long oldestRev = SVNRepository.INVALID_REVISION;
        long youngestRev = SVNRepository.INVALID_REVISION;
        
        SVNMergeRange[] mergeRanges = ranges.getRanges();
        for (int i = 0; i < ranges.getSize(); i++) {
            SVNMergeRange range = mergeRanges[i];
            long maxRev = Math.max(range.getStartRevision(), range.getEndRevision());
            long minRev = Math.min(range.getStartRevision(), range.getEndRevision());
            if (!SVNRevision.isValidRevisionNumber(youngestRev) || maxRev > youngestRev) {
                youngestRev = maxRev;
            }
            if (!SVNRevision.isValidRevisionNumber(oldestRev) || minRev < oldestRev) {
                oldestRev = minRev;
            }
        }

        if (SVNRevision.isValidRevisionNumber(oldestRev)) {
            oldestRev++;
        }
        
        final List changedRevs = new LinkedList();
        repository.log(new String[] { "" }, youngestRev, oldestRev, false, false, 0, false, new String[0], 
                new ISVNLogEntryHandler() {
            public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                changedRevs.add(new Long(logEntry.getRevision()));
            }
        });
        
        long youngestChangedRevision = SVNRepository.INVALID_REVISION;
        long oldestChangedRevision = SVNRepository.INVALID_REVISION;
        if (changedRevs.size() > 0) {
            youngestChangedRevision = ((Long) changedRevs.get(0)).longValue();
            oldestChangedRevision = ((Long) changedRevs.get(changedRevs.size() - 1)).longValue();
        }
        
        List operativeRanges = new LinkedList();
        for (int i = 0; i < ranges.getSize(); i++) {
            SVNMergeRange range = mergeRanges[i];
            long rangeMinRev = Math.min(range.getStartRevision(), range.getEndRevision()) + 1;
            long rangeMaxRev = Math.max(range.getStartRevision(), range.getEndRevision());
            if (rangeMinRev > youngestChangedRevision || rangeMaxRev < oldestChangedRevision) {
                continue;
            }
            for (Iterator changedRevsIter = changedRevs.iterator(); changedRevsIter.hasNext();) {
                long changedRev = ((Long) changedRevsIter.next()).longValue();
                if (changedRev >= rangeMinRev && changedRev <= rangeMaxRev) {
                    operativeRanges.add(range);
                    break;
                }
            }
        }
        return SVNMergeRangeList.fromCollection(operativeRanges);
    }
    
    private void fixDeletedSubtreeRanges(SVNURL url1, long revision1, SVNURL url2, long revision2, SVNRepository repository, Map<File, MergePath> childrenWithMergeInfo) throws SVNException {
        boolean isRollback = revision2 < revision1;
        SVNURL sourceRootUrl = repository.getRepositoryRoot(true);
        Object[] array= childrenWithMergeInfo.values().toArray();
        
        for (MergePath child : childrenWithMergeInfo.values()) {
            if (child.absent) {
                continue;
            }
            int parentIndex = findNearestAncestor(array, false, child.absPath);
            MergePath parent = (MergePath) array[parentIndex];
            if (isRollback) {
                child.remainingRanges = child.remainingRanges.reverse();
                parent.remainingRanges = parent.remainingRanges.reverse();
            }
            
            SVNMergeRangeList added = child.remainingRanges.diff(parent.remainingRanges, true);
            SVNMergeRangeList deleted = parent.remainingRanges.diff(child.remainingRanges, true);
            
            if (isRollback) {
                child.remainingRanges = child.remainingRanges.reverse();
                parent.remainingRanges = parent.remainingRanges.reverse();
            }
            
            if (!added.isEmpty() || !deleted.isEmpty()) {
                String childReposSrcPath = SVNWCUtils.getPathAsChild(targetAbsPath, child.absPath);
                SVNURL childPrimarySrcUrl = revision1 < revision2 ? url2 : url1;
                childPrimarySrcUrl = childPrimarySrcUrl.appendPath(childReposSrcPath, false);
                adjustDeletedSubTreeRanges(child, parent, revision1, revision2, childPrimarySrcUrl, repository);
            }
        }
    }

    protected class MergePath implements Comparable {

        protected File absPath;
        protected boolean missingChild;
        protected boolean switched;
        protected boolean switchedChild;
        protected boolean hasNonInheritable;
        protected boolean absent;
        protected boolean childOfNonInheritable;
        protected boolean recordMergeInfo;

        protected SVNMergeRangeList remainingRanges;
        protected Map<String, SVNMergeRangeList> preMergeMergeInfo;
        protected Map<String, SVNMergeRangeList> implicitMergeInfo;

        protected boolean inheritedMergeInfo;
        protected boolean scheduledForDeletion;
        protected boolean immediateChildDir;
        protected boolean recordNonInheritable;

        public MergePath(File path) {
            absPath = path;
        }
        
        public int compareTo(Object obj) {
            if (obj == null || obj.getClass() != MergePath.class) {
                return -1;
            }
            MergePath mergePath = (MergePath) obj; 
            if (this == mergePath) {
                return 0;
            }
            return absPath.compareTo(mergePath.absPath);
        }
        
        public boolean equals(Object obj) {
            return compareTo(obj) == 0;
        }
        
        public String toString() {
            String str = String.format("missingChild=%s,switched=%s,hasNonInheritable=%s,absent=%s, childOfNonInh=%s,inhMi=%s,scheduledForDeletion=%s,immediateChildDir=%s", missingChild, switched, hasNonInheritable, absent, childOfNonInheritable, inheritedMergeInfo, scheduledForDeletion, immediateChildDir);
            return absPath.toString() + " [" + str + "]";
        }
    }
    
    private class NoopLogHandler implements ISVNLogEntryHandler {
        
        private SVNMergeRangeList operativeRanges;
        private SVNMergeRangeList mergedRanges;

        private String sourceReposAbsPath;
        private Map<File, MergePath> childrenWithMergeInfo;

        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
            operativeRanges = operativeRanges.mergeRevision(logEntry.getRevision());
            boolean logEntryRevisionRequired = false;
            long revision = logEntry.getRevision();
            
            for (String changedPath : logEntry.getChangedPaths().keySet()) {
                String relativePath = SVNPathUtil.getRelativePath(sourceReposAbsPath, changedPath);
                if (relativePath == null) {
                    continue;
                }
                File cwmiPath = SVNFileUtil.createFilePath(targetAbsPath, relativePath);
                SVNMergeRangeList pathExclplicitRangeList = null;
                boolean mergeInfoInherited = false;
                while(!logEntryRevisionRequired) {
                    MergePath child = childrenWithMergeInfo.get(cwmiPath);
                    if (child != null && child.preMergeMergeInfo != null) {
                        pathExclplicitRangeList = child.preMergeMergeInfo.get(changedPath);
                        break;
                    }
                    if (cwmiPath == null || cwmiPath.equals(targetAbsPath)) {
                        break;
                    }
                    cwmiPath = SVNFileUtil.getParentFile(cwmiPath);
                    changedPath = SVNPathUtil.removeTail(changedPath);
                    mergeInfoInherited = true;
                }
                if (pathExclplicitRangeList != null) {
                    SVNMergeRangeList rl = new SVNMergeRangeList(new SVNMergeRange(revision - 1, revision, true));
                    SVNMergeRangeList intersection = pathExclplicitRangeList.intersect(rl, mergeInfoInherited);
                    if (intersection.getSize() == 0) {
                        logEntryRevisionRequired = true;
                    }
                } else {
                    logEntryRevisionRequired = true;
                }
            }
            
            if (!logEntryRevisionRequired) {
                mergedRanges.mergeRevision(revision);
            }
        }
        
    }

    public void checkCancelled() throws SVNCancelException {
    }

    private static boolean isOperativeNotification(SVNEvent event) {
        if (event.getContentsStatus() == SVNStatusType.CONFLICTED
                || event.getContentsStatus() == SVNStatusType.MERGED 
                || event.getContentsStatus() == SVNStatusType.CHANGED
                || event.getPropertiesStatus() == SVNStatusType.CONFLICTED
                || event.getPropertiesStatus() == SVNStatusType.MERGED
                || event.getPropertiesStatus() == SVNStatusType.CHANGED
                || event.getAction() == SVNEventAction.UPDATE_ADD
                || event.getAction() == SVNEventAction.TREE_CONFLICT) {
            return true;
        }
        return false;
                
    }

    protected SVNRepository ensureRepository(SVNRepository repository, SVNURL url) throws SVNException {
        if (repository != null) {
            try {
                ensureSessionURL(repository, url);
                return repository;
            } catch (SVNException e) {
                //
            }
            repository = null;
        }
        if (repository == null) {
            repository = repositoryAccess.createRepository(url, null, false);
        }
        return repository; 
    }

    public ObstructionState performObstructionCheck(File localAbsPath, SVNNodeKind expectedKind) throws SVNException {
        ObstructionState result = new ObstructionState();
        result.obstructionState = SVNStatusType.INAPPLICABLE;
        result.kind = SVNNodeKind.NONE;

        if (dryRun) {
            if (isDryRunDeletion(localAbsPath)) {
                result.deleted = true;
                if (expectedKind != SVNNodeKind.UNKNOWN &&
                        expectedKind != SVNNodeKind.NONE) {
                    result.obstructionState = SVNStatusType.OBSTRUCTED;
                }
                return result;
            } else if (isDryRunAddition(localAbsPath)) {
                result.added = true;
                result.kind = SVNNodeKind.DIR;
                return result;
            }
        }
        
        boolean checkRoot = localAbsPath.equals(targetAbsPath);
        checkWcForObstruction(result, localAbsPath, checkRoot);
        if (result.obstructionState == SVNStatusType.INAPPLICABLE &&
                expectedKind != SVNNodeKind.UNKNOWN &&
                result.kind != expectedKind) {
            result.obstructionState = SVNStatusType.OBSTRUCTED;
        }
        return result;
    }
    
    boolean isDryRunAddition(File path) {
        return dryRun && dryRunAdded != null && dryRunAdded.contains(path);
    }
    
    boolean isDryRunDeletion(File path) {
        return dryRun && dryRunDeletions != null && dryRunDeletions.contains(path);
    }
    private void checkWcForObstruction(ObstructionState result, File localAbsPath, boolean noWcRootCheck) throws SVNException {
        result.kind = SVNNodeKind.NONE;
        result.obstructionState = SVNStatusType.INAPPLICABLE;
        SVNFileType diskKind = SVNFileType.getType(localAbsPath);
        SVNWCDbStatus status = null;
        SVNWCDbKind dbKind = null;
        boolean conflicted = false;
        try {
            Structure<NodeInfo> info = context.getDb().readInfo(localAbsPath, NodeInfo.status, NodeInfo.kind, NodeInfo.conflicted);
            status = info.get(NodeInfo.status);
            dbKind = info.get(NodeInfo.kind);
            conflicted = info.is(NodeInfo.conflicted);
            
            info.release();
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            if (diskKind != SVNFileType.NONE) {
                result.obstructionState = SVNStatusType.OBSTRUCTED;
                return;
            }
            
            try {
                Structure<NodeInfo> parentInfo = context.getDb().readInfo(SVNFileUtil.getParentFile(localAbsPath), NodeInfo.status, NodeInfo.kind);
                ISVNWCDb.SVNWCDbStatus parentStatus = parentInfo.get(NodeInfo.status);
                ISVNWCDb.SVNWCDbKind parentDbKind = parentInfo.get(NodeInfo.kind);
                if (parentDbKind != SVNWCDbKind.Dir ||
                        (parentStatus != SVNWCDbStatus.Normal &&
                        parentStatus != SVNWCDbStatus.Added)) {
                    result.obstructionState = SVNStatusType.OBSTRUCTED;
                }
                parentInfo.release();
            } catch (SVNException e2) {
                if (e2.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    result.obstructionState = SVNStatusType.OBSTRUCTED;
                    return;
                }
                throw e;
            }
            return;
        }
        if (!noWcRootCheck && dbKind == SVNWCDbKind.Dir && status == SVNWCDbStatus.Normal) {
            boolean isRoot = context.getDb().isWCRoot(localAbsPath);
            if (isRoot) {
                result.obstructionState = SVNStatusType.OBSTRUCTED;
                return;
            }
        }
        result.kind = dbKind.toNodeKind(status, false);
        switch (status) {
        case Deleted:
            result.deleted = true;
        case NotPresent:
            if (diskKind != SVNFileType.NONE) {
                result.obstructionState = SVNStatusType.OBSTRUCTED;
            }
            break;
        case Excluded:
        case ServerExcluded:
        case Incomplete:
            result.obstructionState = SVNStatusType.MISSING;            
            break;
        case Added:
            result.added = true;
        case Normal:
            if (diskKind == SVNFileType.NONE) {
                result.obstructionState = SVNStatusType.MISSING;
            } else {
                SVNNodeKind expectedKind = dbKind.toNodeKind();
                if (SVNFileType.getNodeKind(diskKind) != expectedKind) {
                    result.obstructionState = SVNStatusType.OBSTRUCTED;            
                }
            }
        }
        
        if (conflicted) {
            ConflictInfo ci = context.getConflicted(localAbsPath, true, true, true);
            result.conflicted = ci != null && (ci.propConflicted || ci.textConflicted || ci.treeConflicted);
        }
    }
    
    public static class ObstructionState {
        SVNStatusType obstructionState;
        boolean added;
        boolean deleted;
        boolean conflicted;
        SVNNodeKind kind;
    }

    private boolean resolveConflicts(Collection<File> conflictedPaths) throws SVNException {
        List<String> sortedPaths = new ArrayList<String>();
        for (File conflictedPath : conflictedPaths) {
            sortedPaths.add(SVNFileUtil.getFilePath(conflictedPath));
        }
        Collections.sort(sortedPaths, SVNPathUtil.PATH_COMPARATOR);

        boolean conflictRemains = false;

        for (String path : sortedPaths) {
            File localAbsPath = SVNFileUtil.createFilePath(path);

            context.resolvedConflict(localAbsPath, SVNDepth.EMPTY, true, "", true, null);

            ConflictInfo conflicted;
            try {
                conflicted = context.getConflicted(localAbsPath, true, true, true);
                if (conflicted.textConflicted || conflicted.propConflicted || conflicted.treeConflicted) {
                    conflictRemains = true;
                }
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
        }
        return conflictRemains;
    }

    private static class FindOperativeSubtreeRevisions implements ISVNLogEntryHandler {

        private Map<File, String> operativeChildren;
        private SVNWCContext context;
        private File mergeSourceFsPath;
        private File mergeTargetAbsPath;
        private SVNDepth depth;

        private FindOperativeSubtreeRevisions(Map<File, String> operativeChildren, SVNWCContext context, File mergeSourceFsPath, File mergeTargetAbsPath, SVNDepth depth) {
            this.operativeChildren = operativeChildren;
            this.context = context;
            this.mergeSourceFsPath = mergeSourceFsPath;
            this.mergeTargetAbsPath = mergeTargetAbsPath;
            this.depth = depth;
        }

        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
            if (logEntry.getChangedPaths() == null) {
                return;
            }
            for (Map.Entry<String, SVNLogEntryPath> entry : logEntry.getChangedPaths().entrySet()) {
                String path = entry.getKey();
                SVNLogEntryPath logEntryPath = entry.getValue();

                File relPath = SVNFileUtil.skipAncestor(mergeSourceFsPath, SVNFileUtil.createFilePath(path));

                if (relPath == null || "".equals(SVNFileUtil.getFilePath(relPath))) {
                    continue;
                }

                File child = SVNFileUtil.getFileDir(relPath);
                if ("".equals(SVNFileUtil.getFilePath(child))) {
                    SVNNodeKind nodeKind;
                    if (logEntryPath.getKind() == SVNNodeKind.UNKNOWN) {
                        File wcChildAbsPath = SVNFileUtil.createFilePath(mergeTargetAbsPath, relPath);
                        nodeKind = context.readKind(wcChildAbsPath, false);
                    } else {
                        nodeKind = logEntryPath.getKind();
                    }

                    if (depth == SVNDepth.FILES && nodeKind != SVNNodeKind.DIR) {
                        continue;
                    }

                    if (depth == SVNDepth.IMMEDIATES) {
                        continue;
                    }

                    child = relPath;
                }

                File potentialChild = SVNFileUtil.createFilePath(mergeTargetAbsPath, child);

                if (logEntryPath.getType() == 'A' || !operativeChildren.containsKey(potentialChild)) {
                    operativeChildren.put(potentialChild, path);
                }
            }
        }
    }
}
