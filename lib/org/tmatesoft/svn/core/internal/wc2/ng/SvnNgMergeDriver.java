package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNCancellableEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.ConflictInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.PropDiffs;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.ExternalNodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbExternals;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbReader;
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
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnMerge;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgMergeDriver implements ISVNEventHandler {

    public static class MergeSource {
        SVNURL url1;
        long rev1;
        SVNURL url2;
        long rev2;
    }

    boolean force;

    boolean dryRun;
    boolean recordOnly;
    boolean sourcesAncestral;
    boolean sameRepos;
    private boolean mergeinfoCapable;
    private boolean ignoreAncestry;
    private boolean targetMissingChild;
    boolean reintegrateMerge;
    
    File targetAbsPath;
    File addedPath;
    SVNURL reposRootUrl;

    // TODO use merge source instead.
    MergeSource source;
    
    private SVNMergeRangeList implicitSrcGap;
    SVNWCContext context;
    
    private boolean addNecessiatedMerge;
    
    Collection<File> dryRunDeletions;
    Collection<File> dryRunAdded;
    private Collection<File> conflictedPaths;
    
    Collection<File> pathsWithNewMergeInfo;
    Collection<File> pathsWithDeletedMergeInfo;

    SVNDiffOptions diffOptions;
    
    SVNRepository repos1;
    SVNRepository repos2;
    
    SvnMerge operation;
    SvnRepositoryAccess repositoryAccess; 
    
    private Map<File, MergePath> childrenWithMergeInfo;

    private int currentAncestorIndex;
    private boolean singleFileMerge;
    
    public SvnNgMergeDriver(SVNWCContext context, SvnMerge operation, SvnRepositoryAccess repositoryAccess, SVNDiffOptions diffOptions) {
        this.context = context;
        this.operation = operation;
        this.repositoryAccess = repositoryAccess;
        this.diffOptions = diffOptions;
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
    
    public void mergeCousinsAndSupplementMergeInfo(File targetWCPath, 
            SVNRepository repository1, SVNRepository repository2, 
            SVNURL url1, long rev1, SVNURL url2, long rev2, long youngestCommonRev, 
            SVNURL sourceReposRoot, SVNURL wcReposRoot, SVNDepth depth, 
            boolean ignoreAncestry, boolean force, boolean recordOnly, boolean dryRun) throws SVNException {
        
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
            
            modifiedSubtrees = doMerge(null, fauxSources, targetWCPath, false, true, sameRepos, 
                    ignoreAncestry, force, dryRun, false, null, true, false, depth, diffOptions); 
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
            
            doMerge(addResultsCatalog, addSources, targetWCPath, true, true, sameRepos, 
                    ignoreAncestry, force, dryRun, true, modifiedSubtrees, true, true, depth, diffOptions);
            
            doMerge(removeResultsCatalog, removeSources, targetWCPath, true, true, sameRepos, 
                    ignoreAncestry, force, dryRun, true, modifiedSubtrees, true, true, depth, diffOptions);
            
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

    protected Collection<File> doMerge(
            Map<File, Map<String, SVNMergeRangeList>> resultCatalog, 
            List<MergeSource> mergeSources, 
            File targetAbsPath, 
            boolean sourcesAncestral, 
            boolean sourcesRelated, 
            boolean sameRepository, 
            boolean ignoreAncestry, 
            boolean force, 
            boolean dryRun, 
            boolean recordOnly, 
            Collection<File> recordOnlyPaths,
            boolean reintegrateMerge, 
            boolean squelcheMergeInfoNotifications,
            SVNDepth depth,
            SVNDiffOptions diffOptions) throws SVNException {
        
       if (recordOnly) {
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
                return null;
            }
        }
        
        SVNNodeKind targetKind = context.readKind(targetAbsPath, false);
        if (targetKind != SVNNodeKind.DIR && targetKind != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
                    "Merge target ''{0}'' does not exist in the working copy");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        if (depth == SVNDepth.UNKNOWN) {
            depth = SVNDepth.INFINITY;
        }
        this.force = force;
        this.dryRun = dryRun;
        this.recordOnly = recordOnly;
        this.ignoreAncestry = ignoreAncestry;
        this.sameRepos = sameRepository;
        this.mergeinfoCapable = false;
        this.sourcesAncestral = sourcesAncestral;
        this.targetMissingChild = false;
        this.reintegrateMerge = reintegrateMerge;
        this.targetAbsPath = targetAbsPath;
        this.reposRootUrl = context.getNodeReposInfo(targetAbsPath).reposRootUrl;
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
        this.childrenWithMergeInfo = null;
        this.currentAncestorIndex = -1;
        
        boolean checkedMergeInfoCapability = false;
        Collection<File> modifiedSubtrees = new HashSet<File>();
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
                this.repos1 = ensureRepository(repos1, url1);
                this.repos2 = ensureRepository(repos2, url2);
                this.source = mergeSource;
                this.implicitSrcGap = null;                
                this.addedPath = null;
                this.addNecessiatedMerge = false;
                this.dryRunDeletions = dryRun ? new HashSet<File>() : null;
                this.dryRunAdded = dryRun ? new HashSet<File>() : null;
                this.conflictedPaths = null;
                this.pathsWithDeletedMergeInfo = null;
                this.pathsWithNewMergeInfo = null;

                if (!checkedMergeInfoCapability) {
                    this.mergeinfoCapable = repos1.hasCapability(SVNCapability.MERGE_INFO);
                    checkedMergeInfoCapability = true;
                }
                
                if (targetKind == SVNNodeKind.FILE) {
                    doFileMerge(targetAbsPath, resultCatalog, url1, revision1, url2, revision2, depth, sourcesRelated, squelcheMergeInfoNotifications);
                } else if (targetKind == SVNNodeKind.DIR) {
                    boolean abortOnConflicts = i < mergeSources.size() - 1; 
                    doDirectoryMerge(targetAbsPath, resultCatalog, url1, revision1, url2, revision2, depth, abortOnConflicts, squelcheMergeInfoNotifications);
                    
                    if (addedPaths != null) {
                        modifiedSubtrees.addAll(addedPaths);
                    }
                    if (mergedPaths != null) {
                        modifiedSubtrees.addAll(mergedPaths);
                    }
                    if (treeConflictedPaths != null) {
                        modifiedSubtrees.addAll(treeConflictedPaths);
                    } 
                    if (skippedPaths != null) {
                        modifiedSubtrees.addAll(skippedPaths);
                    }
                }
                
                if (!dryRun) {
                    SvnNgMergeinfoUtil.elideMergeInfo(context, repos1, targetAbsPath, null);
                }
                if (context.getEventHandler() != null) {
                    SVNEvent mergeCompletedEvent = SVNEventFactory.createSVNEvent(targetAbsPath, SVNNodeKind.NONE, null, SVNRepository.INVALID_REVISION, 
                            SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE, SVNStatusType.LOCK_INAPPLICABLE, SVNEventAction.MERGE_COMPLETE, 
                            null, null, null); 
                    context.getEventHandler().handleEvent(mergeCompletedEvent, ISVNEventHandler.UNKNOWN);
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
        return modifiedSubtrees;
    }

    protected void doMergeInfoUnawareDirectoryMerge(File targetPath, SVNURL url1, long revision1, SVNURL url2, long revision2, SVNDepth depth) throws SVNException {
        boolean isRollBack = revision1 > revision2;

        MergePath item = new MergePath(targetPath);
        SVNMergeRange itemRange = new SVNMergeRange(revision1, revision2, true);
        item.remainingRanges = new SVNMergeRangeList(itemRange);
        
        childrenWithMergeInfo = new TreeMap<File, SvnNgMergeDriver.MergePath>();
        childrenWithMergeInfo.put(targetPath, item);

        SvnNgMergeCallback mergeCallback = new SvnNgMergeCallback(this);
        driveMergeReportEditor(targetPath, url1, revision1, url2, revision2, childrenWithMergeInfo, isRollBack,  depth, mergeCallback);
    }
    protected void doFileMerge(File targetPath, Map<File, Map<String, SVNMergeRangeList>> resultCatalog, 
            SVNURL url1, long revision1, SVNURL url2, long revision2, SVNDepth depth, boolean sourceRelated, boolean squelchMergeinfoNotifications) throws SVNException {
        singleFileMerge = true;
        SVNURL targetURL = context.getNodeUrl(targetPath);
        SVNMergeRange range = new SVNMergeRange(revision1, revision2, true);
        boolean isRollback = revision1 > revision2;
        SVNURL primaryURL = isRollback ? url1 : url2;
        SVNMergeRangeList remainingRanges = null;
        String mergeInfoPath = null;
        MergePath mergeTarget = null;
        boolean[] inherited = new boolean[1];
        Map<String, SVNMergeRangeList> targetMergeInfo = null;

        if (isHonorMergeInfo()) {
            mergeTarget = new MergePath(targetPath);
            SVNURL sourceReposRootURL = repos1.getRepositoryRoot(true);
            mergeInfoPath = getPathRelativeToRoot(primaryURL, sourceReposRootURL, repos1);
            
            repos1.setLocation(targetURL, false);
            try {
                Map<String, SVNMergeRangeList>[] mis = getFullMergeInfo(true, true, inherited, SVNMergeInfoInheritance.INHERITED, repos1, targetPath, 
                        Math.max(revision1, revision2), Math.min(revision1, revision2));
                mergeTarget.implicitMergeInfo = mis[1];
                targetMergeInfo = mis[0];
            } catch (SVNException e) {
                throw e;
            }
            repos1.setLocation(url1, false);
            if (!recordOnly) {
                calculateRemainingRanges(null, mergeTarget, 
                        sourceReposRootURL, 
                        url1, revision1, url2, revision2, 
                        targetMergeInfo, 
                        implicitSrcGap, false, false, repos1);
                remainingRanges = mergeTarget.remainingRanges;
            }
        }
        if (recordOnly || !isHonorMergeInfo()) {
            remainingRanges = new SVNMergeRangeList(range);
        }
        SVNMergeRange conflictedRange = null;
        if (!recordOnly) {
            SVNMergeRangeList ranges = remainingRanges;
            if (sourcesAncestral && remainingRanges.getSize() > 1) {
                SVNURL oldUrl = ensureSessionURL(repos1, primaryURL);
                ranges = removeNoOpMergeRanges(repos1, remainingRanges);
                if (oldUrl != null) {
                    repos1.setLocation(oldUrl, false);
                }
            }
            SVNMergeRange[] rangesToMerge = ranges.getRanges();
            
            for (int i = 0; i < rangesToMerge.length; i++) {
                SVNMergeRange rng = rangesToMerge[i];
                SVNEvent mergeBeginEvent = SVNEventFactory.createSVNEvent(targetPath, 
                        SVNNodeKind.FILE, 
                        null, -1, sameRepos ? SVNEventAction.MERGE_BEGIN : SVNEventAction.FOREIGN_MERGE_BEGIN, 
                                null, null, sourcesAncestral ? rng : null);
                SVNRepository rep1 = repos1;
                SVNRepository rep2 = repos2;
                if (isHonorMergeInfo() && !url1.equals(url2)) {
                    if (!isRollback && rng.getStartRevision() != revision1) {
                        rep1 = rep2;
                    } else if (isRollback && rng.getEndRevision() != revision2) {
                        rep2 = rep1;
                    }
                }
                File tmpDir = context.getDb().getWCRootTempDir(targetPath);
                File tmpFile1 = SVNFileUtil.createUniqueFile(tmpDir, targetPath.getName(), ".tmp", false);
                File tmpFile2 = SVNFileUtil.createUniqueFile(tmpDir, targetPath.getName(), ".tmp", false);
                SVNProperties props1 = new SVNProperties();
                SVNProperties props2 = new SVNProperties();
                OutputStream os1 = null;
                OutputStream os2 = null;
                try {
                    os1 = SVNFileUtil.openFileForWriting(tmpFile1);
                    try {
                        rep1.getFile("", rng.getStartRevision(), props1, os1);
                    } finally {
                        SVNFileUtil.closeFile(os1);
                    }
                    os2 = SVNFileUtil.openFileForWriting(tmpFile2);
                    try {
                        rep2.getFile("", rng.getEndRevision(), props2, os2);
                    } finally {
                        SVNFileUtil.closeFile(os2);
                    }
                    
                    String mType1 = props1.getStringValue(SVNProperty.MIME_TYPE);
                    String mType2 = props2.getStringValue(SVNProperty.MIME_TYPE);
                    
                    SVNProperties propChanges = props1.compareTo(props2);
                    SvnNgMergeCallback callback = new SvnNgMergeCallback(this);
                    SvnDiffCallbackResult result = new SvnDiffCallbackResult();
                    if (!(ignoreAncestry || sourceRelated)) {
                        callback.fileDeleted(result, targetPath, tmpFile1, tmpFile2, mType1, mType2, props1);
                        boolean sent = singleFileMergeNotify(targetPath, 
                                result.treeConflicted ? SVNEventAction.TREE_CONFLICT : SVNEventAction.UPDATE_DELETE, 
                                result.contentState, SVNStatusType.UNKNOWN, 
                                mergeBeginEvent, false);
                        result = result.reset();
                        callback.fileAdded(result, targetPath, tmpFile1, tmpFile2, 
                                rng.getStartRevision(), rng.getEndRevision(), 
                                mType1, mType2, 
                                null, -1, propChanges, props1);
                        singleFileMergeNotify(targetPath, 
                                result.treeConflicted ? SVNEventAction.TREE_CONFLICT : SVNEventAction.UPDATE_ADD, 
                                result.contentState, result.propState, 
                                mergeBeginEvent, sent);
                    } else {
                        callback.fileChanged(result, targetPath, tmpFile1, tmpFile2, 
                                rng.getStartRevision(), rng.getEndRevision(), 
                                mType1, mType2, 
                                propChanges, props1);
                        singleFileMergeNotify(targetPath, 
                                result.treeConflicted ? SVNEventAction.TREE_CONFLICT : SVNEventAction.UPDATE_UPDATE, 
                                result.contentState, result.propState, 
                                mergeBeginEvent, false);
                    }
                } finally {
                    SVNFileUtil.deleteFile(tmpFile1);
                    SVNFileUtil.deleteFile(tmpFile2);
                }
                
                if (i < rangesToMerge.length - 1 && conflictedPaths != null && conflictedPaths.size() > 0) {
                    conflictedRange = rng;
                    break;
                }
            }
        }
        if (isRecordMergeInfo() && remainingRanges.getSize() > 0) {
            SVNMergeRangeList filteredRangeList = filterNaturalHistoryFromMergeInfo(mergeInfoPath, mergeTarget.implicitMergeInfo, range);
            if (!filteredRangeList.isEmpty() && (skippedPaths == null || skippedPaths.isEmpty())) {
                Map<File, SVNMergeRangeList> merges = new TreeMap<File, SVNMergeRangeList>();
                if (inherited[0]) {
                    recordMergeinfo(targetPath, targetMergeInfo, false);
                }
                merges.put(targetPath, filteredRangeList);
                if (!squelchMergeinfoNotifications) {
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
                
                updateWCMergeInfo(resultCatalog, targetPath, mergeInfoPath, merges, isRollback);
            }
        }
        if (conflictedRange != null) {
            SVNErrorMessage err = makeMergeConflictError(targetPath, conflictedRange);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }
    
    private boolean singleFileMergeNotify(File path, SVNEventAction action, SVNStatusType contents, SVNStatusType props, SVNEvent header, boolean headerSent) throws SVNException {
        SVNEventAction expectedAction = SVNEventAction.SKIP;
        if (contents == SVNStatusType.MISSING) {
            expectedAction = action;
            action = SVNEventAction.SKIP;
        }
        SVNEvent event = SVNEventFactory.createSVNEvent(path, SVNNodeKind.FILE, null, -1, contents, props,SVNStatusType.LOCK_INAPPLICABLE, action, expectedAction, null, null, null);
        if (isOperativeNotification(event) && header != null && !headerSent) {
            handleEvent(header, -1);
            headerSent = true;
        }
        handleEvent(event, -1);
        return headerSent;
    }

    protected void doDirectoryMerge(File targetPath, Map<File, Map<String, SVNMergeRangeList>> resultCatalog, SVNURL url1, long revision1, SVNURL url2, long revision2, SVNDepth depth, boolean abortOnConflicts, boolean squelchMergeinfoNotifications) throws SVNException {        
        boolean isRollBack = revision1 > revision2;
        SVNURL primaryURL = isRollBack ? url1 : url2;
        boolean honorMergeInfo = isHonorMergeInfo();
        boolean recordMergeInfo = isRecordMergeInfo();
        boolean sameURLs = url1.equals(url2);

        SvnNgMergeCallback mergeCallback = new SvnNgMergeCallback(this);
        
        Map<File, MergePath> childrenWithMergeInfo = new TreeMap<File, SvnNgMergeDriver.MergePath>();
        if (!honorMergeInfo) {
            doMergeInfoUnawareDirectoryMerge(targetPath, url1, revision1, url2, revision2, depth);
            return;
        }
        
        SVNRepository repository = isRollBack ? repos1 : repos2;
        SVNURL sourceRootURL = repository.getRepositoryRoot(true);
        String mergeInfoPath = getPathRelativeToRoot(primaryURL, sourceRootURL, null);
        childrenWithMergeInfo = getMergeInfoPaths(childrenWithMergeInfo, mergeInfoPath, sourceRootURL, url1, url2, revision1, revision2, repository, depth);
        this.childrenWithMergeInfo = childrenWithMergeInfo;

        MergePath targetMergePath = childrenWithMergeInfo.values().iterator().next();
        targetMissingChild = targetMergePath.missingChild;
        populateRemainingRanges(childrenWithMergeInfo, sourceRootURL, url1, revision1, url2, revision2, honorMergeInfo, repository, mergeInfoPath);
        
        SVNMergeRange range = new SVNMergeRange(revision1, revision2, true);
        SVNErrorMessage err = null;
        if (honorMergeInfo && !reintegrateMerge) {
            long newRangeStart = getMostInclusiveStartRevision(childrenWithMergeInfo, isRollBack);
            if (SVNRevision.isValidRevisionNumber(newRangeStart)) {
                range.setStartRevision(newRangeStart);
            }
            if (!isRollBack) {
                removeNoOpSubtreeRanges(url1, revision1, url2, revision2, repository);
            }
            fixDeletedSubtreeRanges(url1, revision1, url2, revision2, repository);
            long startRev = getMostInclusiveStartRevision(childrenWithMergeInfo, isRollBack);
            
            if (SVNRevision.isValidRevisionNumber(startRev)) {
                long endRev = getMostInclusiveEndRevision(childrenWithMergeInfo, isRollBack);
                while (SVNRevision.isValidRevisionNumber(endRev)) {
                    SVNMergeRange firstTargetRange = targetMergePath.remainingRanges != null && !targetMergePath.remainingRanges.isEmpty() ? targetMergePath.remainingRanges.getRanges()[0] : null;
                    if (firstTargetRange != null && startRev != firstTargetRange.getStartRevision()) {
                        if (isRollBack) {
                            if (endRev < firstTargetRange.getStartRevision()) {
                                endRev = firstTargetRange.getStartRevision();
                            }
                        } else {
                            if (endRev > firstTargetRange.getStartRevision()) {
                                endRev = firstTargetRange.getStartRevision();
                            }
                        }
                    }
                    sliceRemainingRanges(childrenWithMergeInfo, isRollBack, endRev);
                    currentAncestorIndex = -1;
                     
                    SVNURL realURL1 = url1;
                    SVNURL realURL2 = url2;
                    SVNURL oldURL1 = null;
                    SVNURL oldURL2 = null;
                    long nextEndRev = SVNRepository.INVALID_REVISION;
                    
                    if (!sameURLs) {
                        if (isRollBack && endRev != revision2) {
                            realURL2 = url1;
                            oldURL2 = ensureSessionURL(repos2, realURL2);
                        }
                        if (!isRollBack && startRev != revision1) {
                            realURL1 = url2;
                            oldURL1 = ensureSessionURL(repos1, realURL1);
                        }
                    }
                    
                    try {
                        driveMergeReportEditor(this.targetAbsPath, realURL1, startRev, realURL2, endRev, 
                                childrenWithMergeInfo, isRollBack, depth, mergeCallback);
                    } finally {
                        if (oldURL1 != null) {
                            repos1.setLocation(oldURL1, false);
                        }
                        if (oldURL2 != null) {
                            repos2.setLocation(oldURL2, false);
                        }
                    }
                    
                    processChildrenWithNewMergeInfo(childrenWithMergeInfo);
                    processChildrenWithDeletedMergeInfo(childrenWithMergeInfo);
                    
                    removeFirstRangeFromRemainingRanges(endRev, childrenWithMergeInfo);
                    nextEndRev = getMostInclusiveEndRevision(childrenWithMergeInfo, isRollBack);

                    if ((nextEndRev != -1 || abortOnConflicts) && (conflictedPaths != null && !conflictedPaths.isEmpty())) {
                        SVNMergeRange conflictedRange = new SVNMergeRange(startRev, endRev, false);
                        err = makeMergeConflictError(targetAbsPath, conflictedRange);
                        range.setEndRevision(endRev);
                        break;
                    }
                    startRev = getMostInclusiveStartRevision(childrenWithMergeInfo, isRollBack);
                    endRev = nextEndRev;
                }
            }
        } else {
            if (!recordOnly) {
                currentAncestorIndex = -1;
                driveMergeReportEditor(targetAbsPath, url1, revision1, url2, revision2, null, isRollBack, 
                        depth, mergeCallback);
            }
        }

        if (recordMergeInfo) {
            recordMergeInfoForDirectoryMerge(resultCatalog, range, mergeInfoPath, depth, squelchMergeinfoNotifications);
            if (range.getStartRevision() < range.getEndRevision()) {
                recordMergeInfoForAddedSubtrees(range, mergeInfoPath, depth, squelchMergeinfoNotifications);
            }
        }
        
        if (err != null) {
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    private void removeNoOpSubtreeRanges(SVNURL url1, long revision1, SVNURL url2, long revision2, SVNRepository repository) throws SVNException {
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
    


    public SvnNgRemoteDiffEditor driveMergeReportEditor(File targetWCPath, SVNURL url1, long revision1,
            SVNURL url2, final long revision2, final Map<File, MergePath> childrenWithMergeInfo, final boolean isRollBack, 
            SVNDepth depth, SvnNgMergeCallback mergeCallback) throws SVNException {
        final boolean honorMergeInfo = isHonorMergeInfo();
        long targetStart = revision1;
        
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

        SvnNgRemoteDiffEditor editor = SvnNgRemoteDiffEditor.createEditor(context, targetAbsPath, depth, repos2, revision1, false, dryRun, false, mergeCallback, this);

        SVNURL oldURL = ensureSessionURL(repos2, url1);
        try {
            final SVNDepth reportDepth = depth;
            final long reportStart = targetStart;
            final String targetPath = targetWCPath.getAbsolutePath().replace(File.separatorChar, '/');

            repos1.diff(url2, revision2, revision2, null, ignoreAncestry, depth, true,
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
            if (oldURL != null) {
                repos2.setLocation(oldURL, false);
            }
            editor.cleanup();
        }
        
        if (conflictedPaths == null) {
            conflictedPaths = mergeCallback.getConflictedPaths();
        }
        return editor;
    }

    protected boolean isHonorMergeInfo() {
        return sourcesAncestral && sameRepos && !ignoreAncestry && mergeinfoCapable;
    }

    public boolean isRecordMergeInfo() {
        return !dryRun && isHonorMergeInfo();
    }

    protected SVNURL ensureSessionURL(SVNRepository repository, SVNURL url) throws SVNException {
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

    private int findNearestAncestor(Object[] childrenWithMergeInfoArray, boolean pathIsOwnAncestor, File path) {
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

    private Map<File, MergePath> getMergeInfoPaths(
            final Map<File, MergePath> children, 
            final String mergeSrcPath, 
            final SVNURL sourceRootURL,
            final SVNURL url1,
            final SVNURL url2,
            final long revision1, 
            final long revision2, 
            final SVNRepository repository, 
            final SVNDepth depth) throws SVNException {

        final Map<File, MergePath> childrenWithMergeInfo = children == null ? new TreeMap<File, MergePath>() : children;
        
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
        
        if (!subtreesWithMergeinfo.isEmpty()) {
            for (File wcPath : subtreesWithMergeinfo.keySet()) {
                String value = subtreesWithMergeinfo.get(wcPath);
                MergePath mp = new MergePath(wcPath);
                Map<String, SVNMergeRangeList> mi;
                try {
                    mi = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(value), null);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_INVALID_MERGEINFO_NO_MERGETRACKING,
                                "Invalid mergeinfo detected on ''{0}'', mergetracking not possible");
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }
                    throw e;
                }
                mp.preMergeMergeInfo = mi;
                mp.hasNonInheritable = value != null && value.indexOf(SVNMergeRangeList.MERGE_INFO_NONINHERITABLE_STRING) >= 0;
                childrenWithMergeInfo.put(mp.absPath, mp);
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
            parentMp.missingChild = true;
        } else {
            parentMp = new MergePath(parentPath);
            parentMp.missingChild = true;
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
    
    private void processChildrenWithDeletedMergeInfo(Map<File, MergePath> childrenWithMergeInfo) {
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

    protected void recordMergeInfoForDirectoryMerge(Map<File, Map<String, SVNMergeRangeList>> resultCatalog, SVNMergeRange mergeRange, String mergeInfoPath, SVNDepth depth, boolean squelchMergeinfoNotifications) throws SVNException {
        
        boolean isRollBack = mergeRange.getStartRevision() > mergeRange.getEndRevision();
        boolean operativeMerge = false;
        
        SVNMergeRange range = mergeRange.dup();
        if ((mergedPaths != null && !mergedPaths.isEmpty()) 
            || (skippedPaths != null && !skippedPaths.isEmpty())
            || (addedPaths != null && !addedPaths.isEmpty())
            || (treeConflictedPaths != null && !treeConflictedPaths.isEmpty())) {
            operativeMerge = true;
        }
        if (!operativeMerge) {
            range.setInheritable(true);
        }
        removeAbsentChildren(targetAbsPath, childrenWithMergeInfo);
        Map<File, String> inoperativeImmediateChildren = null;
        if (!recordOnly && range.getStartRevision() <= range.getEndRevision() && depth == SVNDepth.IMMEDIATES) {
            inoperativeImmediateChildren = getInoperativeImmediateChildrent(mergeInfoPath, range.getStartRevision() + 1, range.getEndRevision(), targetAbsPath, repos1);
        }
        Object[] array = childrenWithMergeInfo.values().toArray();
        for (int index = 0; index < array.length; index++) {
            MergePath child = (MergePath) array[index];
            if (child.absent) {
                continue;
            }
            if (index > 0
                    && (!recordOnly || reintegrateMerge)
                    && (!child.immediateChildDir || child.preMergeMergeInfo != null)
                    && (!operativeMerge || !isSubtreeTouchedByMerge(child.absPath))) {
                if (child.childOfNonInheritable) {
                    recordMergeinfo(child.absPath, null, false);
                }
            } else {
                boolean childNodeDeleted = context.isNodeStatusDeleted(child.absPath);
                if (childNodeDeleted) {
                    continue;
                }
                if (inoperativeImmediateChildren != null && inoperativeImmediateChildren.containsKey(child.absPath)) {
                    continue;
                }
                String childReposPath = SVNWCUtils.getPathAsChild(targetAbsPath, child.absPath);
                if (childReposPath == null) {
                    childReposPath = "";
                }
                String childMergeSrcCanonPath = SVNPathUtil.append(mergeInfoPath, childReposPath);
                if (!childMergeSrcCanonPath.startsWith("/")) {
                    childMergeSrcCanonPath = "/" + childMergeSrcCanonPath;
                }
                SVNMergeRangeList childMergeRangelist = filterNaturalHistoryFromMergeInfo(childMergeSrcCanonPath, child.implicitMergeInfo, range);
                if (childMergeRangelist.isEmpty()) {
                    continue;
                }
                if (!squelchMergeinfoNotifications) {
                    SVNEvent mergeBeginEvent = SVNEventFactory.createSVNEvent(child.absPath, 
                            SVNNodeKind.NONE, 
                            null, -1, SVNEventAction.MERGE_RECORD_INFO_BEGIN,
                                    null, null, mergeRange);
                    if (context.getEventHandler() != null) {
                        context.getEventHandler().handleEvent(mergeBeginEvent, -1);
                    }
                }
                if (index == 0) {
                    recordSkips(mergeInfoPath, childMergeRangelist, isRollBack);
                } else if (skippedPaths != null && skippedPaths.contains(child.absPath)) {
                    continue;
                }
                
                boolean rangelistInheritance = calculateMergeInheritance(childMergeRangelist, child.absPath, index == 0, child.missingChild, depth);
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
                Map<File, SVNMergeRangeList> childMerges = new TreeMap<File, SVNMergeRangeList>();
                if ((!recordOnly || reintegrateMerge) && !isRollBack) {
                    SVNURL subtreeMergeUrl = reposRootUrl.appendPath(childMergeSrcCanonPath, false);
                    SVNURL oldUrl = ensureSessionURL(repos2, subtreeMergeUrl);
                    Map<String, SVNMergeRangeList> subtreeHistory = null;
                    try {
                         subtreeHistory = repositoryAccess.getHistoryAsMergeInfo(repos2, SvnTarget.fromURL(subtreeMergeUrl, SVNRevision.create(mergeRange.getEndRevision())), 
                                    Math.max(mergeRange.getStartRevision(), mergeRange.getEndRevision()), 
                                    Math.min(mergeRange.getStartRevision(), mergeRange.getEndRevision()));
                         SVNMergeRangeList childMergeSrcRangelist = subtreeHistory.get(childMergeSrcCanonPath);
                         childMergeRangelist = childMergeRangelist.intersect(childMergeSrcRangelist, false);
                         if (!rangelistInheritance) {
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
                updateWCMergeInfo(resultCatalog, child.absPath, childMergeSrcCanonPath, childMerges, isRollBack);
            }
            if (index > 0) {
                boolean inSwitchedSubtree = false;
                if (child.switched) {
                    inSwitchedSubtree = true;
                } else if (index > 1) {
                    int j = index - 1;
                    for(; j > 0; j--) {
                        MergePath parent = (MergePath) array[j];
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
        Map<File, SVNMergeRangeList> merges = new TreeMap<File, SVNMergeRangeList>();
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

        SvnNgPropertiesManager.setProperty(context, localAbsPath, SVNProperty.MERGE_INFO, 
                mergeInfoValue != null ? SVNPropertyValue.create(mergeInfoValue) : null, 
                SVNDepth.EMPTY, true, null, null);
        
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


    private Map<File, String> getInoperativeImmediateChildrent(String mergeSourceReposAbsPath, long oldestRev, long youngestRev, File targetAbsPath, SVNRepository repos) throws SVNException {
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

    private void recordMergeInfoForAddedSubtrees(SVNMergeRange range, String mergeInfoPath, SVNDepth depth, boolean squelchMergeinfoNotifications) throws SVNException {
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
    
    private void fixDeletedSubtreeRanges(SVNURL url1, long revision1, SVNURL url2, long revision2, SVNRepository repository) throws SVNException {
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
        protected boolean hasNonInheritable;
        protected boolean absent;
        protected boolean childOfNonInheritable;

        protected SVNMergeRangeList remainingRanges;
        protected Map<String, SVNMergeRangeList> preMergeMergeInfo;
        protected Map<String, SVNMergeRangeList> implicitMergeInfo;

        protected boolean inheritedMergeInfo;
        protected boolean scheduledForDeletion;
        protected boolean immediateChildDir;
        
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

    private int operativeNotifications;
    private int notifications;
    
    private Collection<File> addedPaths;
    private Collection<File> mergedPaths;
    private Collection<File> skippedPaths;
    private Collection<File> treeConflictedPaths;
    
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

    public void handleEvent(SVNEvent event, double progress) throws SVNException {
        if (recordOnly &&
                (event.getAction() != SVNEventAction.UPDATE_UPDATE && event.getAction() != SVNEventAction.MERGE_RECORD_INFO_BEGIN)) {
            return;
        }
        boolean operative = false;
        if (isOperativeNotification(event)) {
            operativeNotifications++;
            operative = true;
        }
        if (sourcesAncestral || reintegrateMerge) {
            if (event.getContentsStatus() == SVNStatusType.MERGED ||
                    event.getContentsStatus() == SVNStatusType.CHANGED ||
                    event.getPropertiesStatus() == SVNStatusType.MERGED ||
                    event.getPropertiesStatus() == SVNStatusType.CHANGED ||
                    event.getAction() == SVNEventAction.UPDATE_ADD) {
                if (mergedPaths == null) {
                    mergedPaths = new HashSet<File>();
                }
                mergedPaths.add(event.getFile());
            }
            if (event.getAction() == SVNEventAction.SKIP) {
                if (skippedPaths == null) {
                    skippedPaths = new HashSet<File>();
                }
                skippedPaths.add(event.getFile());
            }
            if (event.getAction() == SVNEventAction.TREE_CONFLICT) {
                if (treeConflictedPaths == null) {
                    treeConflictedPaths = new HashSet<File>();
                }
                treeConflictedPaths.add(event.getFile());
            }
            if (event.getAction() == SVNEventAction.UPDATE_ADD) {
                boolean subtreeRoot = true;
                if (addedPaths == null) {
                    addedPaths = new HashSet<File>();
                } else {
                    File addedPathParent = SVNFileUtil.getFileDir(event.getFile());
                    while (!targetAbsPath.equals(addedPathParent)) {
                        if (addedPaths.contains(addedPathParent)) {
                            subtreeRoot = false;
                            break;
                        }
                        addedPathParent = SVNFileUtil.getFileDir(addedPathParent);
                    }
                }
                if (subtreeRoot) {
                    addedPaths.add(event.getFile());
                }
            }
            if (event.getAction() == SVNEventAction.UPDATE_DELETE) {
                if (addedPaths != null) {
                    addedPaths.remove(event.getFile());
                }
            }
        }
        
        if (sourcesAncestral) {
            notifications++;
            if (!singleFileMerge && operative) {
                Object[] array = childrenWithMergeInfo.values().toArray();
                int index = findNearestAncestor(array, event.getAction() != SVNEventAction.UPDATE_DELETE, event.getFile());
                if (index != currentAncestorIndex) {
                    MergePath child = (MergePath) array[index];
                    currentAncestorIndex = index;
                    if (context.getEventHandler() != null && !child.absent && !child.remainingRanges.isEmpty() && !(index == 0 && child.remainingRanges == null)) {
                        SVNEvent mergeBeginEvent = SVNEventFactory.createSVNEvent(child.absPath, 
                                SVNNodeKind.NONE, 
                                null, -1, sameRepos ? SVNEventAction.MERGE_BEGIN : SVNEventAction.FOREIGN_MERGE_BEGIN, 
                                        null, null, child.remainingRanges.getRanges()[0]);
                        context.getEventHandler().handleEvent(mergeBeginEvent, -1);
                    }
                }
            }
        } else if (context.getEventHandler() != null && !singleFileMerge && operativeNotifications == 1 && operative) {
            SVNEvent mergeBeginEvent = SVNEventFactory.createSVNEvent(targetAbsPath, 
                    SVNNodeKind.NONE, 
                    null, -1, sameRepos ? SVNEventAction.MERGE_BEGIN : SVNEventAction.FOREIGN_MERGE_BEGIN, 
                            null, null, null);
            context.getEventHandler().handleEvent(mergeBeginEvent, -1);
            
        }
        if (context.getEventHandler() != null) {
            context.getEventHandler().handleEvent(event, -1);
        }
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
}
