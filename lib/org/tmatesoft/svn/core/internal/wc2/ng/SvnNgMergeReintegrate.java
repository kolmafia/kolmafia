package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
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
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.LocationsInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnMerge;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgMergeReintegrate extends SvnNgOperationRunner<Void, SvnMerge>{

    @Override
    public boolean isApplicable(SvnMerge operation, SvnWcGeneration wcGeneration) throws SVNException {
        if (super.isApplicable(operation, wcGeneration)) {
            return operation.isReintegrate();
        }
        return false;
    }

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        File lockPath = getLockPath(getFirstTarget());
        if (getOperation().isDryRun()) {
            merge(context, getOperation().getSource(), getFirstTarget(), getOperation().isDryRun());
            
        } else {
            
            try {
                lockPath = getWcContext().acquireWriteLock(lockPath, false, true);
                merge(context, getOperation().getSource(), getFirstTarget(), getOperation().isDryRun());
            } finally {
                getWcContext().releaseWriteLock(lockPath);
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

    private void merge(SVNWCContext context, SvnTarget mergeSource, File mergeTarget, boolean dryRun) throws SVNException {
        SVNFileType targetKind = SVNFileType.getType(mergeTarget);
        if (targetKind == SVNFileType.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "Path ''{0}'' does not exist", mergeTarget);
            SVNErrorManager.error(err, SVNLogType.WC);
        }     
        
        SVNURL url2 = getRepositoryAccess().getTargetURL(mergeSource);
        if (url2 == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", mergeTarget);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        SVNURL wcReposRoot = context.getNodeReposInfo(mergeTarget).reposRootUrl;
        Structure<RepositoryInfo> sourceReposInfo = getRepositoryAccess().createRepositoryFor(mergeSource, mergeSource.getPegRevision(), mergeSource.getPegRevision(), null);
        SVNURL sourceReposRoot = ((SVNRepository) sourceReposInfo.get(RepositoryInfo.repository)).getRepositoryRoot(true);
        
        if (!wcReposRoot.equals(sourceReposRoot)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_UNRELATED_RESOURCES, 
                    "''{0}'' must be from the same repositor as ''{1}''", mergeSource.getURL(), mergeTarget);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SvnNgMergeDriver mergeDriver = new SvnNgMergeDriver(getWcContext(), getOperation(), getRepositoryAccess(), getOperation().getMergeOptions());

        mergeDriver.ensureWcIsSuitableForMerge(mergeTarget, false, false, false);
        
        long targetBaseRev = context.getNodeBaseRev(mergeTarget);
        long rev1 = targetBaseRev;
        File sourceReposRelPath = new File(SVNURLUtil.getRelativeURL(wcReposRoot, url2, false));
        File targetReposRelPath = context.getNodeReposRelPath(mergeTarget);
        
        if ("".equals(sourceReposRelPath.getPath()) || "".equals(targetReposRelPath.getPath())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, 
                    "Neither reintegrate source nor target can be the root of repository");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        final Map<File, String> explicitMergeInfo = new HashMap<File, String>();
        SvnGetProperties pg = getOperation().getOperationFactory().createGetProperties();
        pg.setDepth(SVNDepth.INFINITY);
        pg.setSingleTarget(SvnTarget.fromFile(mergeTarget));
        pg.setReceiver(new ISvnObjectReceiver<SVNProperties>() {
            public void receive(SvnTarget target, SVNProperties props) throws SVNException {
                final String value = props.getStringValue(SVNProperty.MERGE_INFO);
                if (value != null) {
                    explicitMergeInfo.put(target.getFile(), value);
                }
            }
        });
        pg.run();
        
        if (!explicitMergeInfo.isEmpty()) {
            final Map<File, File> externals = context.getDb().getExternalsDefinedBelow(mergeTarget);
            for (Iterator<File> wcPaths = explicitMergeInfo.keySet().iterator(); wcPaths.hasNext();) {
                final File wcPath = wcPaths.next();
                if (externals.containsKey(wcPath)) {
                    wcPaths.remove();
                }
            }
        }
        
        sourceReposInfo = getRepositoryAccess().createRepositoryFor(SvnTarget.fromURL(url2), SVNRevision.UNDEFINED, mergeSource.getPegRevision(), null);
        SVNRepository sourceRepository = sourceReposInfo.get(RepositoryInfo.repository);
        long rev2 = sourceReposInfo.lng(RepositoryInfo.revision);
        url2 = sourceReposInfo.get(RepositoryInfo.url);
        sourceReposInfo.release();
        
        SVNURL targetUrl = context.getNodeUrl(mergeTarget);
        SVNRepository targetRepository = getRepositoryAccess().createRepository(targetUrl, null, false);
        //
        try {
            Map<File, Map<String, SVNMergeRangeList>> mergedToSourceCatalog = new HashMap<File, Map<String,SVNMergeRangeList>>();
            Map<File, Map<String, SVNMergeRangeList>> unmergedToSourceCatalog = new HashMap<File, Map<String,SVNMergeRangeList>>();
            SvnTarget url1 = calculateLeftHandSide(context,
                    mergedToSourceCatalog,
                    unmergedToSourceCatalog, 
                    mergeTarget,
                    targetReposRelPath,
                    explicitMergeInfo,
                    targetBaseRev,
                    sourceReposRelPath,
                    sourceReposRoot,
                    wcReposRoot,
                    rev2,
                    sourceRepository,
                    targetRepository);
            
            if (url1 == null) {
                return;
            }
            
            if (!url1.getURL().equals(targetUrl)) {
                targetRepository.setLocation(url1.getURL(), false);
            }
            rev1 = url1.getPegRevision().getNumber();
            SVNLocationSegment yc = getRepositoryAccess().getYoungestCommonAncestor(url2, rev2, url1.getURL(), rev1);
            
            if (yc == null || !(yc.getPath() != null && yc.getStartRevision() >= 0)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, 
                        "''{0}''@''{1}'' must be ancestrally related to ''{2}''@''{3}''", url1, new Long(rev1), url2, new Long(rev2));
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            
            if (rev1 > yc.getStartRevision()) {
                Map<File, Map<String, SVNMergeRangeList>> finalUnmergedCatalog = new HashMap<File, Map<String,SVNMergeRangeList>>();
                String ycPath = yc.getPath();
                if (ycPath.startsWith("/")) {
                    ycPath = ycPath.substring(1);
                }
                findUnsyncedRanges(sourceReposRelPath, new File(ycPath), 
                        unmergedToSourceCatalog, mergedToSourceCatalog, finalUnmergedCatalog, targetRepository);
                
                if (!finalUnmergedCatalog.isEmpty()) {
                    String catalog = SVNMergeInfoUtil.formatMergeInfoCatalogToString2(finalUnmergedCatalog, "  ", "    Missing ranges: ");
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, 
                            "Reintegrate can only be used if revisions {0} through {1} were " +
                            "previously merged from {2} to the reintegrate source, but this is " +
                            "not the case:\n{3}", new Object[] {new Long(yc.getStartRevision() + 1), new Long(rev2), targetUrl, catalog});
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            
            mergeDriver.mergeCousinsAndSupplementMergeInfo(mergeTarget, 
                    targetRepository, sourceRepository, 
                    url1.getURL(), rev1, 
                    url2, rev2, 
                    yc.getStartRevision(), 
                    sourceReposRoot, 
                    wcReposRoot, 
                    SVNDepth.INFINITY, 
                    false, 
                    false, 
                    false, 
                    dryRun);
        } finally {
            targetRepository.closeSession();
        }
    }
    
    private void findUnsyncedRanges(
            final File sourceReposRelPath,
            final File targetReposRelPath,
            Map<File, Map<String, SVNMergeRangeList>> unmergedToSourceCatalog,
            final Map<File, Map<String, SVNMergeRangeList>> mergedToSourceCatalog,
            final Map<File, Map<String, SVNMergeRangeList>> finalUnmergedCatalog, SVNRepository repos) throws SVNException {
        
        SVNMergeRangeList potentiallyUnmergedRanges = null;
        if (unmergedToSourceCatalog != null) {
            potentiallyUnmergedRanges = new SVNMergeRangeList(new SVNMergeRange[0]);
            for (File cpath : unmergedToSourceCatalog.keySet()) {
                Map<String, SVNMergeRangeList> mi = unmergedToSourceCatalog.get(cpath);
                for (SVNMergeRangeList mrl : mi.values()) {
                    potentiallyUnmergedRanges = potentiallyUnmergedRanges.merge(mrl);
                }
            }
        }
        if (potentiallyUnmergedRanges != null && !potentiallyUnmergedRanges.isEmpty()) {
            long youngest = potentiallyUnmergedRanges.getRanges()[0].getStartRevision() + 1;
            long oldest = potentiallyUnmergedRanges.getRanges()[potentiallyUnmergedRanges.getSize() - 1].getEndRevision();
            repos.log(new String[] {""}, youngest, oldest, true, false, -1, false, null, new ISVNLogEntryHandler() {
                public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                    for (String changedPath : logEntry.getChangedPaths().keySet()) {
                        if (changedPath.startsWith("/")) {
                            changedPath = changedPath.substring(1);
                        }
                        String relPath = SVNWCUtils.isChild(SVNFileUtil.getFilePath(targetReposRelPath), changedPath);
                        if (relPath == null) {
                            continue;
                        }
                        File sourceRelpath = SVNFileUtil.createFilePath(sourceReposRelPath, relPath);
                        String mergeInfoForPath = "/" + changedPath + ":" + logEntry.getRevision();
                        Map<String, SVNMergeRangeList> mi = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(mergeInfoForPath), null);
                        File[] subtreeMissing = new File[1];
                        boolean inCatalog = isMergeinfoInCatalog(sourceRelpath, subtreeMissing, mi, logEntry.getRevision(), mergedToSourceCatalog);
                        if (!inCatalog) {
                            File missingPath = null;
                            File subtreeMissingInThisRev = subtreeMissing[0];
                            if (subtreeMissingInThisRev == null) {
                                subtreeMissingInThisRev = sourceReposRelPath;
                            }
                            if (subtreeMissingInThisRev != null && !subtreeMissingInThisRev.equals(sourceRelpath)) {
                                missingPath = SVNWCUtils.skipAncestor(subtreeMissingInThisRev, sourceRelpath);
                                missingPath = new File(changedPath.substring(0, changedPath.length() - missingPath.getPath().length()));
                            } else {
                                missingPath = new File(changedPath);
                            }
                            Map<String, SVNMergeRangeList> mi2 = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer("/" + SVNFileUtil.getFilePath(missingPath) + ":" + logEntry.getRevision()), null);
                            Map<String, SVNMergeRangeList> existing = finalUnmergedCatalog.get(missingPath);
                            if (existing != null) {
                                existing = SVNMergeInfoUtil.mergeMergeInfos(existing, mi2);
                            } else {
                                existing = mi2;
                            }
                            finalUnmergedCatalog.put(subtreeMissingInThisRev, existing);
                        }
                    }
                }
            });
        }
        
    }
    
    private boolean isMergeinfoInCatalog(File sourceRelpath, File[] catPath, Map<String, SVNMergeRangeList> mergeinfo, 
            long revision, Map<File, Map<String, SVNMergeRangeList>> catalog) throws SVNException {
        if (mergeinfo != null && catalog != null && !catalog.isEmpty()) {
            File path = sourceRelpath;
            String walkPath = null;
            Map<String, SVNMergeRangeList> miInCatalog = null;
            while(true) {
                miInCatalog = catalog.get(path);
                if (miInCatalog != null) {
                    if (catPath != null) {
                        catPath[0] = path;
                    }
                    break;
                } else {
                    walkPath = walkPath != null ? SVNPathUtil.append(SVNFileUtil.getFileName(path), walkPath) : SVNFileUtil.getFileName(path);
                    path = path.getParentFile();
                    if (path == null) {
                        break;
                    }
                }
            }
            if (miInCatalog != null) {
                if (walkPath != null) { 
                    miInCatalog = SVNMergeInfoUtil.adjustMergeInfoSourcePaths(null, walkPath, miInCatalog);
                }
                miInCatalog = SVNMergeInfoUtil.intersectMergeInfo(miInCatalog, mergeinfo, true);
                return SVNMergeInfoUtil.mergeInfoEquals(mergeinfo, miInCatalog, true);
                
            }
        }
        return false;
    }

    private SvnTarget calculateLeftHandSide(SVNWCContext context,
            Map<File, Map<String, SVNMergeRangeList>>  mergedToSourceCatalog,
            Map<File, Map<String, SVNMergeRangeList>>  unmergedToSourceCatalog,
            File targetAbsPath,
            File targetReposRelPath,
            Map<File, String> subtreesWithMergeInfo,
            long targetRev,
            File sourceReposRelPath,
            SVNURL sourceReposRoot,
            SVNURL targetReposRoot,
            long sourceRev,
            SVNRepository sourceRepository,
            SVNRepository targetRepository)  throws SVNException {
        
        if (!subtreesWithMergeInfo.containsKey(targetAbsPath)) {
            subtreesWithMergeInfo.put(targetAbsPath, "");
        }
        
        final Map<File, List<SVNLocationSegment>> segmentsMap = new HashMap<File, List<SVNLocationSegment>>();
        
        for (File path : subtreesWithMergeInfo.keySet()) {
            String miValue = subtreesWithMergeInfo.get(path);
            try {
                SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(miValue), null);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_INVALID_MERGEINFO_NO_MERGETRACKING,
                            "Invalid mergeinfo detected on ''{0}'', reintegrate merge not possible", path);
                    SVNErrorManager.error(err, SVNLogType.WC);                    
                }
                throw e;
            }
            File pathReposRelPath = context.getNodeReposRelPath(path);
            File pathSessionRelPath = SVNWCUtils.skipAncestor(targetReposRelPath, pathReposRelPath);
            if (pathSessionRelPath == null && pathReposRelPath.equals(targetReposRelPath)) {
                pathSessionRelPath = new File("");
            }
            
            List<SVNLocationSegment> segments = targetRepository.getLocationSegments(SVNFileUtil.getFilePath(pathSessionRelPath), targetRev, targetRev, -1);
            segmentsMap.put(pathReposRelPath, segments);
        }
        
        SVNURL sourceUrl = SVNWCUtils.join(sourceReposRoot, sourceReposRelPath);
        SVNURL targetUrl = SVNWCUtils.join(targetReposRoot, targetReposRelPath);
        SVNLocationSegment yc = getRepositoryAccess().getYoungestCommonAncestor(sourceUrl, sourceRev, targetUrl, targetRev);
        if (!(yc != null && yc.getPath() != null && yc.getStartRevision() >= 0)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, 
                    "''{0}''@''{1}'' must be ancestrally related to ''{2}''@''{3}''", sourceUrl, new Long(sourceRev), targetUrl, new Long(targetRev));
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        if (sourceRev == yc.getStartRevision()) {
            return null;
        }
        
        Map<File, Map<String, SVNMergeRangeList>> mergeInfoCatalog = 
                SvnNgMergeinfoUtil.convertToCatalog2(sourceRepository.getMergeInfo(new String[] {""}, sourceRev, SVNMergeInfoInheritance.INHERITED, true));
        if (mergedToSourceCatalog != null) {
            mergedToSourceCatalog.putAll(mergeInfoCatalog);
        }
        UnmergedMergeInfo unmergedMergeInfo = findUnmergedMergeInfo(yc.getStartRevision(), mergeInfoCatalog, segmentsMap, sourceReposRelPath, targetReposRelPath, targetRev, sourceRev, sourceRepository, targetRepository);
        unmergedMergeInfo.catalog = SVNMergeInfoUtil.elideMergeInfoCatalog(unmergedMergeInfo.catalog);
        if (unmergedToSourceCatalog != null && unmergedMergeInfo.catalog != null) {
            for (String path : unmergedMergeInfo.catalog.keySet()) {
                Map<String, SVNMergeRangeList> mi = unmergedMergeInfo.catalog.get(path);
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                path = path.replace(File.separatorChar, '/');
                unmergedToSourceCatalog.put(new File(path), mi);
            }
        }
        if (unmergedMergeInfo.neverSynced) {
            return SvnTarget.fromURL(sourceReposRoot.appendPath(yc.getPath(), false), SVNRevision.create(yc.getStartRevision()));
        } else {
            Structure<LocationsInfo> locations = getRepositoryAccess().getLocations(targetRepository, SvnTarget.fromURL(targetUrl), 
                    SVNRevision.create(targetRev), 
                    SVNRevision.create(unmergedMergeInfo.youngestMergedRevision), 
                    SVNRevision.UNDEFINED);
            SVNURL youngestUrl = locations.get(LocationsInfo.startUrl);
            locations.release();
            return SvnTarget.fromURL(youngestUrl, SVNRevision.create(unmergedMergeInfo.youngestMergedRevision));
        }
    }

    private UnmergedMergeInfo findUnmergedMergeInfo(long ycAncestorRev, Map<File, Map<String, SVNMergeRangeList>> sourceCatalog, Map<File, List<SVNLocationSegment>> targetSegments,
            File sourceReposRelPath, File targetReposRelPath, long targetRev, long sourceRev, SVNRepository sourceRepos, SVNRepository targetRepos) throws SVNException {
        UnmergedMergeInfo result = new UnmergedMergeInfo();
        result.neverSynced = true;
        Map<String, Map<String, SVNMergeRangeList>> newCatalog = new TreeMap<String, Map<String,SVNMergeRangeList>>();
        for (File path : targetSegments.keySet()) {
            List<SVNLocationSegment> segments = targetSegments.get(path);
            File sourcePathRelToSession = SVNWCUtils.skipAncestor(targetReposRelPath, path);
            if (sourcePathRelToSession == null && targetReposRelPath.equals(path)) {
                sourcePathRelToSession = new File("");
            }
            File sourcePath = SVNFileUtil.createFilePath(sourceReposRelPath, sourcePathRelToSession);
            Map<String, SVNMergeRangeList> targetHistoryAsMergeInfo = SvnRepositoryAccess.getMergeInfoFromSegments(segments);
            targetHistoryAsMergeInfo = SVNMergeInfoUtil.filterMergeInfoByRanges(targetHistoryAsMergeInfo, sourceRev, ycAncestorRev);
            Map<String, SVNMergeRangeList> sourceMergeInfo = sourceCatalog.get(sourcePath);
            
            if (sourceMergeInfo != null) {
                sourceCatalog.remove(SVNFileUtil.getFileDir(sourcePath));
                Map<String, SVNMergeRangeList> explicitIntersection = SVNMergeInfoUtil.intersectMergeInfo(sourceMergeInfo, targetHistoryAsMergeInfo, true);
                if (explicitIntersection != null && !explicitIntersection.isEmpty()) {
                    result.neverSynced = false;
                    long[] endPoints = SVNMergeInfoUtil.getRangeEndPoints(explicitIntersection);
                    if (result.youngestMergedRevision < 0 || endPoints[0] > result.youngestMergedRevision) {
                        result.youngestMergedRevision = endPoints[0];
                    }
                }
            } else {
                SVNNodeKind kind = sourceRepos.checkPath(SVNFileUtil.getFilePath(sourcePathRelToSession), sourceRev);
                if (kind == SVNNodeKind.NONE) {
                    continue;
                }
                Map<File, Map<String, SVNMergeRangeList>> subtreeCatalog = 
                        SvnNgMergeinfoUtil.convertToCatalog2(sourceRepos.getMergeInfo(new String[] {SVNFileUtil.getFilePath(sourcePathRelToSession)}, sourceRev, SVNMergeInfoInheritance.INHERITED, false));
                sourceMergeInfo = subtreeCatalog.get(sourcePathRelToSession);
                if (sourceMergeInfo == null) {
                    sourceMergeInfo = new HashMap<String, SVNMergeRangeList>();
                }
            }
            
            segments = sourceRepos.getLocationSegments(SVNFileUtil.getFilePath(sourcePathRelToSession), sourceRev, sourceRev, -1);
            Map<String, SVNMergeRangeList> sourceHistroryAsMergeInfo = SvnRepositoryAccess.getMergeInfoFromSegments(segments);
            sourceMergeInfo = SVNMergeInfoUtil.mergeMergeInfos(sourceMergeInfo, sourceHistroryAsMergeInfo);
            Map<String, SVNMergeRangeList> commonMergeInfo = SVNMergeInfoUtil.intersectMergeInfo(sourceMergeInfo, targetHistoryAsMergeInfo, true);
            Map<String, SVNMergeRangeList> filteredMergeInfo = SVNMergeInfoUtil.removeMergeInfo(commonMergeInfo, targetHistoryAsMergeInfo, true);
            
            newCatalog.put(SVNFileUtil.getFilePath(sourcePath), filteredMergeInfo);
        }
        
        if (!sourceCatalog.isEmpty()) {
            for(File path : sourceCatalog.keySet()) {
                File sourcePathRelToSession =  sourceReposRelPath.equals(path) ? new File("") : SVNWCUtils.skipAncestor(sourceReposRelPath, path);
                
                File targetPath = sourceReposRelPath.equals(path) ? new File("") : SVNWCUtils.skipAncestor(sourceReposRelPath, path);
                List<SVNLocationSegment> segments = null;
                Map<String, SVNMergeRangeList> sourceMergeInfo = sourceCatalog.get(path);
                try {
                    segments = targetRepos.getLocationSegments(SVNFileUtil.getFilePath(targetPath), targetRev, targetRev, -1);
                } catch (SVNException e) {
                    SVNErrorCode ec = e.getErrorMessage().getErrorCode();
                    if (ec == SVNErrorCode.FS_NOT_FOUND || ec == SVNErrorCode.RA_DAV_REQUEST_FAILED) {
                        continue;
                    }
                    throw e;
                }
                
                Map<String, SVNMergeRangeList> targetHistoryAsMergeInfo = SvnRepositoryAccess.getMergeInfoFromSegments(segments);
                Map<String, SVNMergeRangeList> explicitIntersection = SVNMergeInfoUtil.intersectMergeInfo(sourceMergeInfo, targetHistoryAsMergeInfo, true);
                if (explicitIntersection != null && !explicitIntersection.isEmpty()) {
                    result.neverSynced = false;
                    long[] endPoints = SVNMergeInfoUtil.getRangeEndPoints(explicitIntersection);
                    if (result.youngestMergedRevision < 0 || endPoints[0] > result.youngestMergedRevision) {
                        result.youngestMergedRevision = endPoints[0];
                    }
                }
                segments = sourceRepos.getLocationSegments(SVNFileUtil.getFilePath(sourcePathRelToSession), targetRev, targetRev, -1);
                Map<String, SVNMergeRangeList> sourceHistoryAsMergeInfo = SvnRepositoryAccess.getMergeInfoFromSegments(segments);
                sourceMergeInfo = SVNMergeInfoUtil.mergeMergeInfos(sourceMergeInfo, sourceHistoryAsMergeInfo);
                Map<String, SVNMergeRangeList> commonMergeInfo = SVNMergeInfoUtil.intersectMergeInfo(sourceMergeInfo, targetHistoryAsMergeInfo, true);
                Map<String, SVNMergeRangeList> filteredMergeInfo = SVNMergeInfoUtil.removeMergeInfo(commonMergeInfo, targetHistoryAsMergeInfo, true);
                if (!filteredMergeInfo.isEmpty()) {
                    newCatalog.put(SVNFileUtil.getFilePath(path), filteredMergeInfo);
                }
            }
        }
        if (result.youngestMergedRevision >= 0) {
            newCatalog = SVNMergeInfoUtil.filterCatalogByRanges(newCatalog, result.youngestMergedRevision, 0);
        }
        result.catalog = newCatalog;
        return result;
    }
    
    private static class UnmergedMergeInfo {
        private Map<String, Map<String, SVNMergeRangeList>> catalog;
        private boolean neverSynced;
        private long youngestMergedRevision; 
    }
}
