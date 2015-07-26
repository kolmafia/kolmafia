package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.SVNWCNodeReposInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.UniqueFileInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.WritableBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbCopy;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.LocationsInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RevisionsPair;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.*;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgReposToWcCopy extends SvnNgOperationRunner<Void, SvnCopy> {

    @Override
    public boolean isApplicable(SvnCopy operation, SvnWcGeneration wcGeneration) throws SVNException {
        return areAllSourcesRemote(operation) && operation.getFirstTarget().isLocal();
    }
    
    private boolean areAllSourcesRemote(SvnCopy operation) {
        for(SvnCopySource source : operation.getSources()) {
            if (source.getSource().isFile()) {
                if (operation.isMove()) {
                    return false;
                }
                if (isLocalRevision(source.getRevision()) && isLocalRevision(source.getSource().getResolvedPegRevision())) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean isLocalRevision(SVNRevision revision) {
        return revision == SVNRevision.WORKING || revision == SVNRevision.UNDEFINED;
    }
    
    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        if (getOperation().isMove()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                    "Moves between the working copy and the repository are not supported");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        Collection<SvnCopySource> sources = expandCopySources(getOperation().getSources());
        Collection<SvnCopyPair> copyPairs = new ArrayList<SvnNgReposToWcCopy.SvnCopyPair>();
        boolean srcsAreUrls = sources.iterator().next().getSource().isURL();
        if (sources.size() > 1) {
            for (SvnCopySource copySource : sources) {
                SvnCopyPair copyPair = new SvnCopyPair();
                String baseName;
                if (copySource.getSource().isFile()) {
                    copyPair.sourceFile = copySource.getSource().getFile();
                    baseName = copyPair.sourceFile.getName();
                    if (srcsAreUrls) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
                                "Cannot mix repository and working copy sources");
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }
                    // resolve peg rev which might be 'WORKING' or 'BASE'. 
                    if (copySource.getSource().getResolvedPegRevision() == SVNRevision.WORKING) {
                        Structure<RevisionsPair> pair = getRepositoryAccess().getRevisionNumber(null, copySource.getSource(), 
                                copySource.getSource().getResolvedPegRevision(), null);
                        copyPair.sourcePegRevision = SVNRevision.create(pair.lng(RevisionsPair.revNumber));
                        pair.release();
                    } else {
                        copyPair.sourcePegRevision = copySource.getSource().getResolvedPegRevision();
                    }
                    copyPair.sourceRevision = copySource.getRevision();
                } else {
                    copyPair.source = copySource.getSource().getURL();
                    baseName = SVNPathUtil.tail(copyPair.source.getPath());
                    copyPair.sourcePegRevision = copySource.getSource().getResolvedPegRevision();
                    copyPair.sourceRevision = copySource.getRevision();
                }
                copyPair.dst = new File(getFirstTarget(), baseName);
                copyPairs.add(copyPair);
            }
        } else if (sources.size() == 1) {
            SvnCopyPair copyPair = new SvnCopyPair();
            SvnCopySource source = sources.iterator().next();
            String baseName;
            if (source.getSource().isFile()) {
                copyPair.sourceFile = source.getSource().getFile();
                baseName = copyPair.sourceFile.getName();
            } else {
                copyPair.source = source.getSource().getURL();
                baseName = SVNPathUtil.tail(copyPair.source.getPath());
            }
            if (source.getSource().getResolvedPegRevision() == SVNRevision.WORKING) {
                Structure<RevisionsPair> pair = getRepositoryAccess().getRevisionNumber(null, source.getSource(), 
                        source.getSource().getResolvedPegRevision(), null);
                copyPair.sourcePegRevision = SVNRevision.create(pair.lng(RevisionsPair.revNumber));
                pair.release();
            } else {
                copyPair.sourcePegRevision = source.getSource().getResolvedPegRevision();
            }
            copyPair.sourceRevision = source.getRevision();
            copyPair.dst = getFirstTarget();
            if (!getOperation().isFailWhenDstExists() && SVNFileType.getType(copyPair.dst) != SVNFileType.NONE) {
                copyPair.dst = new File(copyPair.dst, baseName);
            }
            
            copyPairs.add(copyPair);
        }
        if (!srcsAreUrls) {
            for (SvnCopyPair pair : copyPairs) {
                File src = pair.sourceFile;
                File dst = pair.dst;
                if (SVNWCUtils.isChild(src, dst)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot copy path ''{0}'' into its own child ''{1}''",
                        src, dst);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                
                Structure<NodeOriginInfo> no = getWcContext().getNodeOrigin(src, true, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl, NodeOriginInfo.revision);
                if (no.get(NodeOriginInfo.reposRelpath) != null) {
                    pair.source = no.<SVNURL>get(NodeOriginInfo.reposRootUrl).appendPath(SVNFileUtil.getFilePath(no.<File>get(NodeOriginInfo.reposRelpath)), false);
                } else {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' does not have an URL associated with it", src);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                if (pair.sourcePegRevision == SVNRevision.BASE) {
                    pair.sourcePegRevision = SVNRevision.create(no.lng(NodeOriginInfo.revision));
                }
                if (pair.sourceRevision == SVNRevision.BASE) {
                    pair.sourceRevision = SVNRevision.create(no.lng(NodeOriginInfo.revision));
                }
            }
        }
        return copy(copyPairs, getOperation().isMakeParents(), getOperation().isIgnoreExternals());
    }


    protected Collection<SvnCopySource> expandCopySources(Collection<SvnCopySource> sources) throws SVNException {
        Collection<SvnCopySource> expanded = new ArrayList<SvnCopySource>(sources.size());
        for (SvnCopySource source : sources) {
            if (source.isCopyContents() && source.getSource().isURL()) {
                // get children at revision.
                SVNRevision pegRevision = source.getSource().getResolvedPegRevision();
                if (!pegRevision.isValid()) {
                    pegRevision = SVNRevision.HEAD;
                }
                SVNRevision startRevision = source.getRevision();
                if (!startRevision.isValid()) {
                    startRevision = pegRevision;
                }

                final SVNRepository svnRepository = getRepositoryAccess().createRepository(source.getSource().getURL(), null, true);

                final Structure<LocationsInfo> locations = getRepositoryAccess().getLocations(svnRepository, source.getSource(), pegRevision, startRevision, SVNRevision.UNDEFINED);
                long revision = locations.lng(LocationsInfo.startRevision);
                Collection<SVNDirEntry> entries = new ArrayList<SVNDirEntry>();
                svnRepository.getDir("", revision, null, 0, entries);
                for (Iterator<SVNDirEntry> ents = entries.iterator(); ents.hasNext();) {
                    SVNDirEntry entry = (SVNDirEntry) ents.next();
                    // add new copy source.
                    expanded.add(SvnCopySource.create(SvnTarget.fromURL(entry.getURL()), source.getRevision()));
                }
            } else {
                expanded.add(source);
            }
        }
        return expanded;
    }

    
    private Void copy(Collection<SvnCopyPair> copyPairs, boolean makeParents, boolean ignoreExternals) throws SVNException {
        for (SvnCopyPair pair : copyPairs) {
            Structure<LocationsInfo> locations = getRepositoryAccess().getLocations(null, 
                    pair.sourceFile != null ? SvnTarget.fromFile(pair.sourceFile) : SvnTarget.fromURL(pair.source), 
                    pair.sourcePegRevision, pair.sourceRevision, SVNRevision.UNDEFINED);
            pair.sourceOriginal = pair.source;
            pair.source = locations.get(LocationsInfo.startUrl);
            locations.release();
        }
        
        SVNURL topSrcUrl = getCommonCopyAncestor(copyPairs);
        File topDst = getCommonCopyDst(copyPairs);
        if (copyPairs.size() == 1) {
            topSrcUrl = topSrcUrl.removePathTail();
            topDst = SVNFileUtil.getParentFile(topDst);
        }
        
        SVNRepository repository = getRepositoryAccess().createRepository(topSrcUrl, null);
        Structure<RevisionsPair> revisionPair = null;
        for (SvnCopyPair pair : copyPairs) {
            revisionPair = getRepositoryAccess().getRevisionNumber(repository, SvnTarget.fromURL(pair.source), pair.sourceRevision, revisionPair);
            pair.revNum = revisionPair.lng(RevisionsPair.revNumber);
        }
        
        for (SvnCopyPair pair : copyPairs) {
            String relativePath = SVNURLUtil.getRelativeURL(topSrcUrl, pair.source, false);
            relativePath = SVNEncodingUtil.uriDecode(relativePath);
            SVNNodeKind sourceKind = repository.checkPath(relativePath, pair.revNum);
            if (sourceKind == SVNNodeKind.NONE) {
                if (pair.revNum >= 0) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, 
                            "Path ''{0}'' not found in revision ''{1}''", pair.source, pair.revNum);
                    SVNErrorManager.error(err, SVNLogType.WC);
                } else {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, 
                            "Path ''{0}'' not found in HEAD revision", pair.source);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            pair.srcKind = sourceKind;
            SVNFileType dstType = SVNFileType.getType(pair.dst);
            if (dstType != SVNFileType.NONE) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, 
                        "Path ''{0}'' already exists", pair.dst);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            File dstParent = SVNFileUtil.getParentFile(pair.dst);
            dstType = SVNFileType.getType(dstParent);
            if (getOperation().isMakeParents() && dstType == SVNFileType.NONE) {
                SVNFileUtil.ensureDirectoryExists(dstParent);
                
                SvnScheduleForAddition add = getOperation().getOperationFactory().createScheduleForAddition();
                add.setSingleTarget(SvnTarget.fromFile(dstParent));
                add.setDepth(SVNDepth.INFINITY);
                add.setIncludeIgnored(true);
                add.setForce(false);
                add.setAddParents(true);
                add.setSleepForTimestamp(false);
                
                try {
                    add.run();
                } catch (SVNException e) {
                    SVNFileUtil.deleteAll(dstParent, true);
                    throw e;
                }
            } else if (dstType != SVNFileType.DIRECTORY) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, 
                        "Path ''{0}'' in not a directory", dstParent);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        
        // now do real copy
        File locked = getWcContext().acquireWriteLock(topDst, false, true);
        try {
            return copy(copyPairs, topDst, ignoreExternals, repository);
        } finally {
            getWcContext().releaseWriteLock(locked);
        }
    }
    
    private Void copy(Collection<SvnCopyPair> copyPairs, File topDst, boolean ignoreExternals, SVNRepository repository) throws SVNException {
        for (SvnCopyPair pair : copyPairs) {
            SVNNodeKind dstKind  = SvnWcDbCopy.readKind(getWcContext().getDb(), pair.dst, false, true);
            if (dstKind != SVNNodeKind.NONE) {
                SVNWCContext.NodePresence nodePresence = getWcContext().getNodePresence(pair.dst, false);
                boolean isExcluded = nodePresence.isExcluded;
                boolean isServerExcluded = nodePresence.isServerExcluded;

                if (isExcluded || isServerExcluded) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Path ''{0}'' exists, but is excluded", pair.dst);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                } else {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Path ''{0}'' already exists", pair.dst);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            }
            dstKind = SVNFileType.getNodeKind(SVNFileType.getType(pair.dst));

            if (dstKind != SVNNodeKind.NONE) {
                if (getOperation().isMove() && copyPairs.size() == 1 && SVNFileUtil.getParentFile(pair.sourceFile).equals(SVNFileUtil.getParentFile(pair.dst))) {
                    try {
                        boolean caseRenamed = pair.sourceFile.getCanonicalPath().equals(pair.dst.getCanonicalPath());
                        if (caseRenamed) {
                            continue;
                        }
                    } catch (IOException e) {
                        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                        SVNErrorManager.error(errorMessage, SVNLogType.WC);
                    }
                }
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Path ''{0}'' already exists as unversioned node", pair.dst);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            File dstParentAbsPath = SVNFileUtil.getParentFile(pair.dst);
            SVNNodeKind dstParentKind = SvnWcDbCopy.readKind(getWcContext().getDb(), dstParentAbsPath, false, true);

            if (getOperation().isMakeParents() && dstParentKind == SVNNodeKind.NONE) {
                SvnScheduleForAddition scheduleForAddition = getOperation().getOperationFactory().createScheduleForAddition();
                scheduleForAddition.setMkDir(true);

                SvnNgAdd svnNgAdd = new SvnNgAdd();
                svnNgAdd.setWcContext(getWcContext());
                svnNgAdd.setOperation(scheduleForAddition);
                svnNgAdd.addFromDisk(dstParentAbsPath, null, true);
            } else if (dstParentKind != SVNNodeKind.DIR) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "Path ''{0}'' is not a directory", dstParentAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            dstParentKind = SVNFileType.getNodeKind(SVNFileType.getType(dstParentAbsPath));

            if (dstParentKind != SVNNodeKind.DIR) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_MISSING, "Path ''{0}'' is not a directory", dstParentAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }
        boolean sameRepositories = false;
        try {
            String sourceUuid = repository.getRepositoryUUID(true);
            SVNWCNodeReposInfo info = getWcContext().getNodeReposInfo(topDst);
            String dstUuid = info != null ? info.reposUuid : null;
            sameRepositories = sourceUuid != null && dstUuid != null && sourceUuid.equals(dstUuid);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.RA_NO_REPOS_UUID) {
                throw e;
            }
        }
        for (SvnCopyPair pair : copyPairs) {
            copy(pair, sameRepositories, ignoreExternals, repository);
        }
        sleepForTimestamp();
        
        return null;
    }

    private long copy(final SvnCopyPair pair, boolean sameRepositories, boolean ignoreExternals, SVNRepository repository) throws SVNException {
        ISVNEventHandler eventHandler = getOperation().getEventHandler();

        if (!sameRepositories && eventHandler != null) {
            SVNEvent event = SVNEventFactory.createSVNEvent(pair.sourceFile, pair.srcKind, null, -1, SVNEventAction.FOREIGN_COPY_BEGIN, SVNEventAction.FOREIGN_COPY_BEGIN, null, null);
            event.setURL(pair.source);
            eventHandler.handleEvent(event, UNKNOWN);
        }
        long rev = -1;
        SVNURL oldLocation = repository.getLocation();
        repository.setLocation(pair.source, false);

        if (pair.srcKind == SVNNodeKind.DIR) {
            if (sameRepositories) {
                File dstParent = SVNFileUtil.getParentFile(pair.dst);
                final File dstPath = SVNFileUtil.createUniqueFile(dstParent, pair.dst.getName(), ".tmp", false);
                try {
                    SVNFileUtil.deleteFile(dstPath);
                    SVNFileUtil.ensureDirectoryExists(dstPath);
                    SvnCheckout co = getOperation().getOperationFactory().createCheckout();
                    co.setSingleTarget(SvnTarget.fromFile(dstPath));
                    co.setSource(SvnTarget.fromURL(pair.sourceOriginal, pair.sourcePegRevision));
                    co.setRevision(pair.sourceRevision);
                    co.setIgnoreExternals(ignoreExternals);
                    co.setDepth(SVNDepth.INFINITY);
                    co.setAllowUnversionedObstructions(false);
                    co.setSleepForTimestamp(false);
                    final ISVNEventHandler oldHandler = getWcContext().getEventHandler();
                    getWcContext().pushEventHandler(new ISVNEventHandler() {
                        public void checkCancelled() throws SVNCancelException {
                            if (oldHandler != null) {
                                oldHandler.checkCancelled();
                            }
                        }

                        public void handleEvent(SVNEvent event, double progress) throws SVNException {
                            File path = event.getFile();
                            if (path != null) {
                                path = SVNWCUtils.skipAncestor(dstPath, path);
                                if (path != null) {
                                    path = new File(pair.dst, path.getPath());
                                    event.setFile(path);
                                } else if (dstPath.equals(event.getFile())) {
                                    event.setFile(pair.dst);
                                }
                            }
                            if (oldHandler != null) {
                                oldHandler.handleEvent(event, progress);
                            }
                        }
                    });
                    try {
                        rev = co.run();
                    } finally {
                        getWcContext().popEventHandler();
                    }

                    eventHandler = getWcContext().getEventHandler();
                    getWcContext().setEventHandler(null);
                    new SvnNgWcToWcCopy().copy(getWcContext(), dstPath, pair.dst, true);
                    getWcContext().setEventHandler(eventHandler);

                    File dstLock = getWcContext().acquireWriteLock(dstPath, false, true);
                    try {
                        getWcContext().removeFromRevisionControl(dstPath, false, false);
                    } finally {
                        try {
                            getWcContext().releaseWriteLock(dstLock);
                        } catch (SVNException e) {
                        }
                    }
                    SVNFileUtil.rename(dstPath, pair.dst);
                } finally {
                    SVNFileUtil.deleteAll(dstPath, true);
                }
            } else {
                copyForeign(pair.source, pair.dst, pair.sourcePegRevision, pair.sourceRevision, SVNDepth.INFINITY, false, true);
                sleepForTimestamp();
                return rev;
            }
        } else {
            String relativePath = "";
            File tmpDir = getWcContext().getDb().getWCRootTempDir(pair.dst);
            UniqueFileInfo ufInfo = SVNWCContext.openUniqueFile(tmpDir, true);
            SVNProperties newProperties = new SVNProperties();
            try {
                pair.revNum = repository.getFile(relativePath, pair.revNum, newProperties, ufInfo.stream);
            } finally {
                SVNFileUtil.closeFile(ufInfo.stream);
            }
            InputStream newContents = SVNFileUtil.openFileForReading(ufInfo.path);
            try {
                addFileToWc(getWcContext(),
                        pair.dst, newContents, null, newProperties, null,
                        sameRepositories ? pair.source : null,
                        sameRepositories ? pair.revNum : -1);
            } finally {
                SVNFileUtil.closeFile(newContents);
            }

        }
        Map<String, SVNMergeRangeList> mergeInfo = calculateTargetMergeInfo(pair.source, pair.revNum, repository);
        repository.setLocation(oldLocation, false);
        
        String mergeInfoProperty = getWcContext().getProperty(pair.dst, SVNProperty.MERGE_INFO);
        Map<String, SVNMergeRangeList> wcMergeInfo = null;
        if (mergeInfoProperty != null) {
            wcMergeInfo = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(mergeInfoProperty), null);
        }
        if (wcMergeInfo != null && mergeInfo != null) {
            wcMergeInfo = SVNMergeInfoUtil.mergeMergeInfos(wcMergeInfo, mergeInfo);
        } else if (wcMergeInfo == null) {
            wcMergeInfo = mergeInfo;
        }
        String extendedMergeInfoValue = null;
        if (wcMergeInfo != null) {
            extendedMergeInfoValue = SVNMergeInfoUtil.formatMergeInfoToString(wcMergeInfo, null);
        }
        
        SvnNgPropertiesManager.setProperty(getWcContext(), pair.dst, SVNProperty.MERGE_INFO, 
                extendedMergeInfoValue != null ? SVNPropertyValue.create(extendedMergeInfoValue) : null, 
                SVNDepth.EMPTY, true, null, null);

        SVNEvent event = SVNEventFactory.createSVNEvent(pair.dst, pair.srcKind, null, pair.revNum, 
                SVNEventAction.COPY, SVNEventAction.COPY, null, null, 1, 1);
        handleEvent(event);
        
        return rev;
    }

    private void copyForeign(SVNURL url, File dstAbsPath, SVNRevision pegRevision, SVNRevision revision, SVNDepth depth, boolean makeParents, boolean alreadyLocked) throws SVNException {
        assert SVNFileUtil.isAbsolute(dstAbsPath);

        Structure<SvnRepositoryAccess.RepositoryInfo> repositoryInfoStructure = getRepositoryAccess().createRepositoryFor(SvnTarget.fromURL(url), pegRevision, revision, null);
        SVNRepository repository = repositoryInfoStructure.get(SvnRepositoryAccess.RepositoryInfo.repository);
        long locRev = repositoryInfoStructure.lng(SvnRepositoryAccess.RepositoryInfo.revision);

        SVNNodeKind kind = repository.checkPath("", locRev);
        if (kind != SVNNodeKind.FILE && kind != SVNNodeKind.DIR) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a valid location inside a repository", url);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        SVNNodeKind wcKind = getWcContext().readKind(dstAbsPath, true);
        if (wcKind != SVNNodeKind.NONE) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "''{0}'' is already under version control", dstAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        File dirAbsPath = SVNFileUtil.getParentFile(dstAbsPath);
        wcKind = getWcContext().readKind(dirAbsPath, false);

        if (wcKind == SVNNodeKind.NONE) {
            if (makeParents) {
                SvnScheduleForAddition scheduleForAddition = getOperation().getOperationFactory().createScheduleForAddition();
                scheduleForAddition.setMkDir(true);
                scheduleForAddition.setAddParents(true);
                scheduleForAddition.setSingleTarget(SvnTarget.fromFile(dirAbsPath));
                scheduleForAddition.run();
            }
            wcKind = getWcContext().readKind(dirAbsPath, false);
        }

        if (wcKind != SVNNodeKind.DIR) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "Can't add ''{0}'', because no parent directory is found", dstAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        if (kind == SVNNodeKind.FILE) {
            SVNProperties props = new SVNProperties();
            SVNProperties regularProps = new SVNProperties();
            OutputStream outputStream = SVNFileUtil.openFileForWriting(dstAbsPath);
            try {
                repository.getFile("", locRev, props, outputStream);
            } finally {
                SVNFileUtil.closeFile(outputStream);
            }
            SvnNgPropertiesManager.categorizeProperties(props, regularProps, null, null);
            regularProps.remove(SVNProperty.MERGE_INFO);

            if (!alreadyLocked) {
                final File lockRoot = getWcContext().acquireWriteLock(dirAbsPath, false, true);
                try {
                    SvnNgAdd svnNgAdd = new SvnNgAdd();
                    svnNgAdd.setWcContext(getWcContext());
                    svnNgAdd.setRepositoryAccess(getRepositoryAccess());
                    svnNgAdd.addFromDisk(dstAbsPath, regularProps, true);
                }
                finally {
                    getWcContext().releaseWriteLock(lockRoot);
                }
            } else {
                SvnNgAdd svnNgAdd = new SvnNgAdd();
                svnNgAdd.setWcContext(getWcContext());
                svnNgAdd.setRepositoryAccess(getRepositoryAccess());
                svnNgAdd.addFromDisk(dstAbsPath, regularProps, true);
            }
        } else {
            if (!alreadyLocked) {
                final File lockRoot = getWcContext().acquireWriteLock(dirAbsPath, false, true);
                try {
                    copyForeignDir(repository, locRev, dstAbsPath, depth);
                }
                finally {
                    getWcContext().releaseWriteLock(lockRoot);
                }
            } else {
                copyForeignDir(repository, locRev, dstAbsPath, depth);
            }
        }
    }

    private void copyForeignDir(SVNRepository repository, final long locRev, File dstAbsPath, final SVNDepth depth) throws SVNException {
        ISVNEditor editor = new SVNCopyForeignEditor(dstAbsPath, getWcContext());
        editor = SVNCancellableEditor.newInstance(editor, getWcContext().getEventHandler(), SVNDebugLog.getDefaultLog());
        repository.update(locRev, "", SVNDepth.INFINITY, false, new ISVNReporterBaton() {
            public void report(ISVNReporter reporter) throws SVNException {
                reporter.setPath("", null, locRev, depth, true);
                reporter.finishReport();
            }
        }, editor);
    }

    private SVNURL getCommonCopyAncestor(Collection<SvnCopyPair> copyPairs) {
        SVNURL ancestor = null;
        for (SvnCopyPair svnCopyPair : copyPairs) {
            if (ancestor == null) {
                ancestor = svnCopyPair.source;
                continue;
            }
            ancestor = SVNURLUtil.getCommonURLAncestor(ancestor, svnCopyPair.source);
        }
        return ancestor;
    }

    private File getCommonCopyDst(Collection<SvnCopyPair> copyPairs) {
        String ancestor = null;
        for (SvnCopyPair svnCopyPair : copyPairs) {
            String path = svnCopyPair.dst.getAbsolutePath().replace(File.separatorChar, '/');
            if (ancestor == null) {
                ancestor = path;
                continue;
            }
            ancestor = SVNPathUtil.getCommonPathAncestor(ancestor, path);
        }
        return new File(ancestor);
    }
    
    public static void addFileToWc(SVNWCContext context, File path, InputStream newBaseContents, InputStream newContents,
            SVNProperties newBaseProps, SVNProperties newProps, SVNURL copyFromURL, long copyFromRev) throws SVNException {
        context.writeCheck(path);
        
        SVNWCDbStatus status = null;
        try {
            Structure<NodeInfo> ni = context.getDb().readInfo(path, NodeInfo.status);
            status = ni.get(NodeInfo.status);
            ni.release();
            
            switch (status) {
            case NotPresent:
            case Deleted:
                break;
            default:
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Node ''{0}'' exists", path);
                SVNErrorManager.error(err, SVNLogType.WC);
                break;
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
        }
        
        File dirPath = SVNFileUtil.getParentFile(path);
        Structure<NodeInfo> ni = context.getDb().readInfo(dirPath, NodeInfo.status, NodeInfo.kind);
        status = ni.get(NodeInfo.status);
        ISVNWCDb.SVNWCDbKind kind = ni.get(NodeInfo.kind);
        ni.release();

        switch (status) {
        case Normal:
        case Added:
            break;
        case Deleted:
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Can''t add ''{0}'' to a parent directory scheduled for deletion", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        default:
            SVNErrorMessage err2 = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Can''t find parent directory''s node while trying to add ''{0}''", path);
            SVNErrorManager.error(err2, SVNLogType.WC);
            break;
        }
        
        if (kind != SVNWCDbKind.Dir) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Can''t schedule an addition of ''{0}'' below a not-directory node", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        SVNURL originalURL = null;
        File originalReposPath = null;
        String originalUuid = null;
        
        if (copyFromURL != null) {
            SVNWCNodeReposInfo reposInfo = context.getNodeReposInfo(dirPath);
            if (!SVNURLUtil.isAncestor(reposInfo.reposRootUrl, copyFromURL)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
                        "Copyfrom-url ''{0}'' has different repository root than ''{0}''", 
                        copyFromURL, reposInfo.reposRootUrl);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            originalUuid = reposInfo.reposUuid;
            originalURL = reposInfo.reposRootUrl;
            String reposPath = SVNURLUtil.getRelativeURL(originalURL, copyFromURL, false);
            if (reposPath.startsWith("/")) {
                reposPath = reposPath.substring("/".length());
            }
            originalReposPath = new File(reposPath);
        } else {
            copyFromRev = -1;
        }
        SVNProperties regularProps = new SVNProperties();
        SVNProperties entryProps = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(newBaseProps, regularProps, entryProps, null);
        newBaseProps = regularProps;
        long changedRev = entryProps.containsName(SVNProperty.COMMITTED_REVISION) ? Long.parseLong(entryProps.getStringValue(SVNProperty.COMMITTED_REVISION)) : - 1;
        String changedAuthor = entryProps.getStringValue(SVNProperty.LAST_AUTHOR);
        SVNDate changedDate = entryProps.containsName(SVNProperty.COMMITTED_REVISION) ? SVNDate.parseDate(entryProps.getStringValue(SVNProperty.COMMITTED_DATE)) : new SVNDate(0, 0);
        
        WritableBaseInfo wbInfo = context.openWritableBase(path, true, true);
        try {
            SVNTranslator.copy(newBaseContents, wbInfo.stream);
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(err, SVNLogType.WC);
        } finally {
            SVNFileUtil.closeFile(wbInfo.stream);
        }
        File sourcePath = null;
        if (newContents != null) {
            File tempDir = context.getDb().getWCRootTempDir(path);
            UniqueFileInfo ufInfo = SVNWCContext.openUniqueFile(tempDir, true);
            try {
                SVNTranslator.copy(newContents, ufInfo.stream);
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(err, SVNLogType.WC);
            } finally {
                SVNFileUtil.closeFile(ufInfo.stream);
            }            
            sourcePath = ufInfo.path;
        }
        
        if (copyFromURL != null) {
            context.getDb().installPristine(wbInfo.tempBaseAbspath, wbInfo.getSHA1Checksum(), wbInfo.getMD5Checksum());
        } 
        
        if (newContents == null && copyFromURL == null) {
            sourcePath = wbInfo.tempBaseAbspath;
        }
        boolean recordFileInfo = newContents == null;
        SVNSkel wi = context.wqBuildFileInstall(path, sourcePath, false, recordFileInfo);
        wi = context.wqMerge(wi, null);
        if (sourcePath != null) {
            SVNSkel remove = context.wqBuildFileRemove(path, sourcePath);
            wi = context.wqMerge(wi, remove);
        }
        
        context.getDb().opCopyFile(path, newBaseProps, changedRev, changedDate, changedAuthor,
                originalReposPath, originalURL, originalUuid, copyFromRev, 
                copyFromURL != null ? wbInfo.getSHA1Checksum() : null, false, null, null, null);
        
        context.getDb().opSetProps(path, newProps, null, false, wi);
        context.wqRun(dirPath);
    }

    private Map<String, SVNMergeRangeList> calculateTargetMergeInfo(SVNURL srcURL, long srcRevision,  SVNRepository repository) throws SVNException {
        SVNURL url = null;
        url = srcURL;

        Map<String, SVNMergeRangeList> targetMergeInfo = null;
        String mergeInfoPath;
        SVNRepository repos = repository;
        if (repos == null) {
            repos = getRepositoryAccess().createRepository(url, null, false);
        }
        SVNURL oldLocation = null;
        try {
            mergeInfoPath = getRepositoryAccess().getPathRelativeToSession(url, null, repos);
            if (mergeInfoPath == null) {
                oldLocation = repos.getLocation();
                repos.setLocation(url, false);
                mergeInfoPath = "";
            }
            targetMergeInfo = getRepositoryAccess().getReposMergeInfo(repos, mergeInfoPath, srcRevision, SVNMergeInfoInheritance.INHERITED, true);
        } finally {
            if (repository == null) {
                repos.closeSession();
            } else if (oldLocation != null) {
                repos.setLocation(oldLocation, false);
            }
        }
        return targetMergeInfo;
    }

    private static class SvnCopyPair {
        SVNNodeKind srcKind;
        long revNum;
        SVNURL sourceOriginal;
        File sourceFile;
        SVNURL source;
        SVNRevision sourceRevision;
        SVNRevision sourcePegRevision;
        
        File dst;
    }

    private class SVNCopyForeignEditor implements ISVNEditor {

        private SVNCopyForeignEditor(File anchorAbsPath, SVNWCContext context) {
            this.anchorAbsPath = anchorAbsPath;
            this.context = context;
        }

        private class DirectoryBaton {
            DirectoryBaton parentBaton;
            File localAbsPath;
            boolean created;
            SVNProperties properties;

            public DirectoryBaton(DirectoryBaton directoryBaton) {
                parentBaton = directoryBaton;
            }
        }

        private class FileBaton {
            DirectoryBaton parentBaton;
            File localAbsPath;
            SVNProperties properties;

            boolean writing;
            String checksum;

            public FileBaton(DirectoryBaton directoryBaton) {
                parentBaton = directoryBaton;
            }
        }


        private final File anchorAbsPath;
        private DirectoryBaton currentDirectory;
        private FileBaton currentFile;
        private final SVNWCContext context;
        SVNDeltaProcessor svnDeltaProcessor = new SVNDeltaProcessor();

        public void targetRevision(long revision) throws SVNException {
        }

        public void openRoot(long revision) throws SVNException {
            currentDirectory = new DirectoryBaton(null);
            currentDirectory.localAbsPath = anchorAbsPath;

            SVNFileUtil.ensureDirectoryExists(anchorAbsPath);
        }

        public void deleteEntry(String path, long revision) throws SVNException {
        }

        public void absentDir(String path) throws SVNException {
        }

        public void absentFile(String path) throws SVNException {
        }

        public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
            currentDirectory = new DirectoryBaton(currentDirectory);
            currentDirectory.localAbsPath = SVNFileUtil.createFilePath(anchorAbsPath, path);
            if (!SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(anchorAbsPath), SVNFileUtil.getFilePath(currentDirectory.localAbsPath))) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Path ''{0}'' is not in the working copy", path);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            SVNFileUtil.ensureDirectoryExists(currentDirectory.localAbsPath);
        }

        public void openDir(String path, long revision) throws SVNException {
        }

        public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
            if (!SVNProperty.isRegularProperty(name) && SVNProperty.MERGE_INFO.equals(name)) {
                return;
            }
            if (!currentDirectory.created) {
                if (currentDirectory.properties == null) {
                    currentDirectory.properties = new SVNProperties();
                }
                if (value != null) {
                    currentDirectory.properties.put(name, value);
                }
            } else {
                SvnNgPropertiesManager.setProperty(context, currentDirectory.localAbsPath, name, value, SVNDepth.EMPTY, false, null, null);
            }
        }

        public void closeDir() throws SVNException {
            ensureAdded(currentDirectory);
        }

        private void ensureAdded(DirectoryBaton currentDirectory) throws SVNException {
            if (currentDirectory.created) {
                return;
            }
            if (currentDirectory.parentBaton != null) {
                ensureAdded(currentDirectory.parentBaton);
            }
            currentDirectory.created = true;

            SvnNgAdd svnNgAdd = new SvnNgAdd();
            svnNgAdd.setWcContext(context);
            svnNgAdd.setOperation(getOperation().getOperationFactory().createScheduleForAddition());
            svnNgAdd.addFromDisk(currentDirectory.localAbsPath, currentDirectory.properties, true);
        }

        public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
            currentFile = new FileBaton(currentDirectory);
            currentFile.localAbsPath = SVNFileUtil.createFilePath(anchorAbsPath, path);
            if (!SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(anchorAbsPath), SVNFileUtil.getFilePath(currentFile.localAbsPath))) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Path ''{0}'' is not in the working copy", path);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }

        public void openFile(String path, long revision) throws SVNException {
        }

        public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
            if (!SVNProperty.isRegularProperty(propertyName) && SVNProperty.MERGE_INFO.equals(propertyName)) {
                return;
            }
            if (currentFile.properties == null) {
                currentFile.properties = new SVNProperties();
            }
            if (propertyValue != null) {
                currentFile.properties.put(propertyName, propertyValue);
            }
        }

        public void closeFile(String path, String textChecksum) throws SVNException {
            ensureAdded(currentFile.parentBaton);
            if (textChecksum != null && currentFile.checksum != null) {
                if (!textChecksum.equals(currentFile.checksum)) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''", currentFile.localAbsPath);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            }
            SvnNgAdd svnNgAdd = new SvnNgAdd();
            svnNgAdd.setWcContext(context);
            svnNgAdd.setOperation(getOperation().getOperationFactory().createScheduleForAddition());
            svnNgAdd.addFromDisk(currentFile.localAbsPath, currentFile.properties, true);
        }

        public SVNCommitInfo closeEdit() throws SVNException {
            return null;
        }

        public void abortEdit() throws SVNException {
        }

        public void applyTextDelta(String path, String baseChecksum) throws SVNException {
            assert !currentFile.writing;

            currentFile.writing = true;

            svnDeltaProcessor.applyTextDelta(SVNFileUtil.DUMMY_IN, SVNFileUtil.openFileForWriting(currentFile.localAbsPath), true);
        }

        public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
            return svnDeltaProcessor.textDeltaChunk(diffWindow);
        }

        public void textDeltaEnd(String path) throws SVNException {
            currentFile.checksum = svnDeltaProcessor.textDeltaEnd();
        }
    }
}
