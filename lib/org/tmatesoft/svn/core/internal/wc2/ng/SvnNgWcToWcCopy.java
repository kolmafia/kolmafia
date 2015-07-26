package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.*;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.AdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.ExternalNodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnCopy;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgWcToWcCopy extends SvnNgOperationRunner<Void, SvnCopy> {

    @Override
    public boolean isApplicable(SvnCopy operation, SvnWcGeneration wcGeneration) throws SVNException {
        return areAllSourcesLocal(operation) && operation.getFirstTarget().isLocal();
    }
    
    private boolean areAllSourcesLocal(SvnCopy operation) {
        for(SvnCopySource source : operation.getSources()) {
            if (source.getSource().isFile()) {
                if (operation.isMove()) {
                    continue;
                }
                if (isLocalRevision(source.getRevision()) && isLocalRevision(source.getSource().getResolvedPegRevision())) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }
    
    private boolean isLocalRevision(SVNRevision revision) {
        return revision == SVNRevision.WORKING || revision == SVNRevision.UNDEFINED;
    }

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        boolean timestampSleep = false;

        Collection<SvnCopySource> sources = getOperation().getSources();
        try {
            timestampSleep = tryRun(context, sources, getFirstTarget());
            return null;
        } catch (SVNException e) {
            SVNErrorCode code = e.getErrorMessage().getErrorCode();
            if (!getOperation().isFailWhenDstExists()
                    && getOperation().getSources().size() == 1 
                    && (code == SVNErrorCode.ENTRY_EXISTS || code == SVNErrorCode.FS_ALREADY_EXISTS)) {
                SvnCopySource source = sources.iterator().next();
                timestampSleep = tryRun(context, sources, new File(getFirstTarget(), source.getSource().getFile().getName()));
                return null;
            }
            throw e;            
        } finally {
            if (timestampSleep) {
                sleepForTimestamp();
            }
        }        
    }

    protected boolean tryRun(SVNWCContext context, Collection<SvnCopySource> sources, File target) throws SVNException {
        if (getOperation().isDisjoint()) {
            return disjointCopy(context, target);
        } else {
            return copy(context, sources, target);
        }
    }

    /**
     * The method performs "disjoint" copy (see SVNCopyClient#doCopy(File))
     * The algorithm is:
     * 1. Create a fake working copy
     * 2. Move wc.db from the nested working copy to the fake
     * 3. Move all pristine files to the parent working copy
     * 4. Perform metadata copying
     * @param context
     * @param nestedWC
     * @return
     * @throws SVNException
     */
    private boolean disjointCopy(SVNWCContext context, File nestedWC) throws SVNException {
        nestedWC = new File(nestedWC.getAbsolutePath().replace(File.separatorChar, '/'));
        final File nestedWCParent = nestedWC.getParentFile();

        checkForDisjointCopyPossibility(context, nestedWC, nestedWCParent);
        context.getDb().close();

        final File wcRoot = context.getDb().getWCRoot(nestedWCParent);

        final File tempDirectory = new File(getAdminDirectory(wcRoot), "tmp");
        SVNFileUtil.ensureDirectoryExists(tempDirectory);
        final File fakeWorkingCopyDirectory = SVNFileUtil.createUniqueDir(tempDirectory, "disjoint-copy", "tmp", true);
        
        moveWcDb(nestedWC, fakeWorkingCopyDirectory);
        copyPristineFiles(nestedWC, wcRoot, true);
        SVNFileUtil.deleteAll(getAdminDirectory(nestedWC), true);
        context.getDb().forgetDirectoryTemp(nestedWC);

        File lockRoot = null;
        try {
            lockRoot = context.acquireWriteLock(wcRoot, true, true);
            copy(context, fakeWorkingCopyDirectory, nestedWC, true);
        } finally {
            if (lockRoot != null) {
                context.releaseWriteLock(lockRoot);
            }
        }

        return true;
    }

    private void checkForDisjointCopyPossibility(SVNWCContext context, File nestedWC, File nestedWCParent) throws SVNException {
        SVNFileType nestedWCType = SVNFileType.getType(nestedWC);

        if (nestedWCType != SVNFileType.DIRECTORY) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                    "This kind of copy can be run on a root of a disjoint wc directory only");
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        if (nestedWCParent == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                    "{0} seems to be not a disjoint wc since it has no parent", nestedWC);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        if (!(context.getDb() instanceof SVNWCDb)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                    "Unsupported working copy format", nestedWC);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        SVNWCDb wcdb = (SVNWCDb) context.getDb();

        if (hasMetadataInParentWc(wcdb, nestedWC, nestedWCParent)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS,
                    "Entry ''{0}'' already exists in parent directory", nestedWC.getName());
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        final ISVNWCDb.WCDbBaseInfo baseInfo = context.getDb().getBaseInfo(nestedWC,
                ISVNWCDb.WCDbBaseInfo.BaseInfoField.reposRootUrl, ISVNWCDb.WCDbBaseInfo.BaseInfoField.reposRelPath);

        final ISVNWCDb.WCDbBaseInfo parentBaseInfo = context.getDb().getBaseInfo(nestedWCParent,
                ISVNWCDb.WCDbBaseInfo.BaseInfoField.reposRootUrl, ISVNWCDb.WCDbBaseInfo.BaseInfoField.reposRelPath);

        if (baseInfo.reposRootUrl != null && parentBaseInfo.reposRootUrl != null &&
                !baseInfo.reposRootUrl.equals(parentBaseInfo.reposRootUrl)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SCHEDULE,
                    "Cannot copy to ''{0}'', as it is not from repository ''{1}''; it is from ''{2}''",
                    new Object[] { nestedWCParent, baseInfo.reposRootUrl,
                    baseInfo.reposRootUrl });
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        SVNWCContext.ScheduleInternalInfo parentSchedule = context.getNodeScheduleInternal(nestedWCParent, true, true);

        if (parentSchedule.schedule == SVNWCContext.SVNWCSchedule.delete) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SCHEDULE,
                    "Cannot copy to ''{0}'', as it is scheduled for deletion", nestedWCParent);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        SVNWCContext.ScheduleInternalInfo schedule = context.getNodeScheduleInternal(nestedWC, true, true);

        if (schedule.schedule == SVNWCContext.SVNWCSchedule.delete) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SCHEDULE,
                    "Cannot copy ''{0}'', as it is scheduled for deletion", nestedWC);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        File relativePath = baseInfo.reposRelPath;
        File parentRelativePath = parentBaseInfo.reposRelPath;

        if (relativePath == null || parentRelativePath == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SCHEDULE,
                    "Cannot copy ''{0}'': cannot resolve its relative path, perhaps it is obstructed", nestedWC);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        if (SVNPathUtil.isAncestor(relativePath.getPath(), parentRelativePath.getPath())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                    "Cannot copy path ''{0}'' into its own child ''{1}",
                    new Object[] { nestedWC, nestedWCParent });
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        if ((schedule.schedule == SVNWCContext.SVNWCSchedule.add && !schedule.copied) ||
                context.getNodeUrl(nestedWC) == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS,
                    "Cannot copy or move ''{0}'': it is not in repository yet; " +
                    "try committing first", nestedWC);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    private void moveWcDb(File sourceWc, File targetWc) throws SVNException {
        final File sourceWcDbFile = getWCDbFile(sourceWc);
        final File targetWcDbFile = getWCDbFile(targetWc);

        SVNFileUtil.ensureDirectoryExists(targetWcDbFile.getParentFile());
        SVNFileUtil.rename(sourceWcDbFile, targetWcDbFile);
    }

    private void copyPristineFiles(File sourceWc, File targetWc, boolean move) throws SVNException {
        final File sourcePristineDirectory = getPristineDirectory(sourceWc);
        final File targetPristineDirectory = getPristineDirectory(targetWc);

        final File[] sourceDirectories = SVNFileListUtil.listFiles(sourcePristineDirectory);
        if (sourceDirectories != null) {
            for (File sourceDirectory : sourceDirectories) {
                final File targetDirectory = new File(targetPristineDirectory, sourceDirectory.getName());
                SVNFileUtil.ensureDirectoryExists(targetDirectory);
                final File[] sourcePristineFiles = SVNFileListUtil.listFiles(sourceDirectory);
                if (sourcePristineFiles != null) {
                    for (File sourcePristineFile : sourcePristineFiles) {
                        final File targetPristineFile = new File(targetDirectory, sourcePristineFile.getName());
                        if (!targetPristineFile.exists()) {
                            if (move) {
                                SVNFileUtil.rename(sourcePristineFile, targetPristineFile);
                            } else {
                                SVNFileUtil.copyFile(sourcePristineFile, targetPristineFile, false);
                            }
                        }
                    }
                }
            }
        }

        SVNFileUtil.deleteAll(sourcePristineDirectory, true);
    }

    private File getPristineDirectory(File workingCopyDirectory) {
        return new File(getAdminDirectory(workingCopyDirectory), "pristine");
    }

    private File getWCDbFile(File nestedWC) {
        return new File(getAdminDirectory(nestedWC), "wc.db");
    }

    private File getAdminDirectory(File parentWC) {
        final String adminDirectoryName = SVNFileUtil.getAdminDirectoryName();
        return new File(parentWC, adminDirectoryName);
    }

    private boolean hasMetadataInParentWc(SVNWCDb wcdb, File nestedWC, File nestedWCParent) throws SVNException {
        SVNWCDb.DirParsedInfo parsedInfo = wcdb.obtainWcRoot(nestedWCParent);
        SVNWCDbDir wcDbDir = parsedInfo == null ? null : parsedInfo.wcDbDir;
        SVNWCDbRoot wcdbRoot = wcDbDir == null ? null : wcDbDir.getWCRoot();

        if (wcdbRoot == null) {
            return false;
        }

        try {
            wcdb.readInfo(wcdbRoot, new File(SVNPathUtil.getRelativePath(nestedWCParent.getPath(),nestedWC.getPath())));
            return true;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_NOT_FOUND || e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                return false;
            } else {
                throw e;
            }
        }
    }

    private boolean copy(SVNWCContext context, Collection<SvnCopySource> sources, File target) throws SVNException {
        boolean sleepForTimeStamp = false;
        Collection<SvnCopyPair> copyPairs = new ArrayList<SvnCopyPair>();

        if (sources.size() > 1) {
            if (getOperation().isFailWhenDstExists()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_MULTIPLE_SOURCES_DISALLOWED);
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            for (SvnCopySource copySource : sources) {
                SvnCopyPair copyPair = new SvnCopyPair();
                copyPair.source = copySource.getSource().getFile();
                String baseName = copyPair.source.getName();
                copyPair.dst = new File(target, baseName);
                copyPairs.add(copyPair);
            }
        } else if (sources.size() == 1) {
            SvnCopyPair copyPair = new SvnCopyPair();
            SvnCopySource source = sources.iterator().next(); 
            copyPair.source = new File(SVNPathUtil.validateFilePath(source.getSource().getFile().getAbsolutePath()));
            copyPair.dst = target;
            copyPairs.add(copyPair);
        }
        
        for (SvnCopyPair pair : copyPairs) {
            File src = pair.source;
            File dst = pair.dst;
            if (getOperation().isMove() && src.getAbsolutePath().equals(dst.getAbsolutePath())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
                        "Cannot move path ''{0}'' into itself", src);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (SVNWCUtils.isChild(src, dst)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot copy path ''{0}'' into its own child ''{1}''",
                    src, dst);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        if (getOperation().isMove()) {
            for (SvnCopyPair pair : copyPairs) {
                File src = pair.source;
                try {
                    Structure<ExternalNodeInfo> externalInfo = SvnWcDbExternals.readExternal(context, src, src, ExternalNodeInfo.kind, ExternalNodeInfo.definingAbsPath);
                    if (externalInfo.hasValue(ExternalNodeInfo.kind) && externalInfo.get(ExternalNodeInfo.kind) != SVNNodeKind.NONE) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CANNOT_MOVE_FILE_EXTERNAL, 
                                "Cannot move the external at ''{0}''; please edit the svn:externals property on ''{1}''.", src, externalInfo.get(ExternalNodeInfo.definingAbsPath));
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                        throw e;
                    }
                }
            }
        } 
        verifyPaths(copyPairs, getOperation().isMakeParents(), getOperation().isMove());
        if (getOperation().isMove()) {
            sleepForTimeStamp = move(copyPairs);
        } else {
            File dstAncestor = null;
            if (copyPairs.size() >= 1) {
                dstAncestor = copyPairs.iterator().next().dst;
            } 
            if (copyPairs.size() > 1 && dstAncestor != null) {
                dstAncestor = SVNFileUtil.getParentFile(dstAncestor);
            }
            dstAncestor = dstAncestor.getAbsoluteFile();
            dstAncestor = context.acquireWriteLock(dstAncestor, false, true);
            try {
                for (SvnCopyPair copyPair : copyPairs) {
                    checkCancelled();
                    File dstPath = SVNFileUtil.createFilePath(copyPair.dstParent, copyPair.baseName);
                    sleepForTimeStamp = sleepForTimeStamp || copy(context, copyPair.source, dstPath, getOperation().isMetadataOnly() || getOperation().isVirtual());
                }
            } finally {
                context.releaseWriteLock(dstAncestor);
            }
        }
        
        return false;
    }

    private boolean move(Collection<SvnCopyPair> pairs) throws SVNException {
        for (SvnCopyPair copyPair : pairs) {
            Collection<File> lockPaths = new HashSet<File>();
            Collection<File> lockedPaths = new HashSet<File>();
            
            checkCancelled();
            File sourceParent = new File(SVNPathUtil.validateFilePath(SVNFileUtil.getParentFile(copyPair.source).getAbsolutePath()));
            if (sourceParent.equals(copyPair.dstParent) ||
                    SVNWCUtils.isChild(sourceParent, copyPair.dstParent)) {
                lockPaths.add(sourceParent);
            } else if (SVNWCUtils.isChild(copyPair.dstParent, sourceParent)) {
                lockPaths.add(copyPair.dstParent);
            } else {
                lockPaths.add(sourceParent);
                lockPaths.add(copyPair.dstParent);
            }
            try {
                for (File file : lockPaths) {
                    lockedPaths.add(getWcContext().acquireWriteLock(file, false, true));
                }
                
                move(getWcContext(), copyPair.source, SVNFileUtil.createFilePath(copyPair.dstParent, copyPair.baseName), getOperation().isMetadataOnly() || getOperation().isVirtual());
            } finally {
                for (File file : lockedPaths) {
                    getWcContext().releaseWriteLock(file);
                }
            }
        }
        return false;
    }

    private void verifyPaths(Collection<SvnCopyPair> copyPairs, boolean makeParents, boolean move) throws SVNException {
        for (SvnCopyPair copyPair : copyPairs) {
            SVNNodeKind dstKind = SvnWcDbCopy.readKind(getWcContext().getDb(), copyPair.dst, false, true);
            if (dstKind != SVNNodeKind.NONE) {
                SVNWCContext.NodePresence nodePresence = getWcContext().getNodePresence(copyPair.dst, false);
                if (nodePresence.isExcluded || nodePresence.isServerExcluded) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Path ''{0}'' exists, but is excluded", copyPair.dst);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                } else {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Path ''{0}'' already exists", copyPair.dst);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            }

            SVNFileType srcType = SVNFileType.getType(copyPair.source);
            SVNFileType dstType = SVNFileType.getType(copyPair.dst);

            if (!getOperation().isMetadataOnly()) {
                if (getOperation().isVirtual()) {
                    verifyPathsExistenceForVirtualCopy(copyPair.source, copyPair.dst, srcType, dstType, copyPair, move);
                } else {
                    final boolean caseOnlyRename = verifyPaths(srcType, dstType, copyPair, copyPairs.size(), move);
                    if (caseOnlyRename) {
                        return;
                    }
                }
            }
            copyPair.dstParent = new File(SVNPathUtil.validateFilePath(SVNFileUtil.getParentFile(copyPair.dst).getAbsolutePath()));
            copyPair.baseName = SVNFileUtil.getFileName(copyPair.dst);

            SVNNodeKind dstParentKind = SvnWcDbCopy.readKind(getWcContext().getDb(), copyPair.dstParent, false, true);
            if (makeParents && (dstParentKind == SVNNodeKind.NONE || getOperation().isVirtual())) {
                SVNFileUtil.ensureDirectoryExists(copyPair.dstParent);
                
                SvnScheduleForAddition add = getOperation().getOperationFactory().createScheduleForAddition();
                add.setSingleTarget(SvnTarget.fromFile(copyPair.dstParent));
                add.setDepth(getOperation().isVirtual() ? SVNDepth.EMPTY : SVNDepth.INFINITY);
                add.setIncludeIgnored(true);
                add.setForce(false);
                add.setAddParents(true);
                add.setSleepForTimestamp(false);
                
                try {
                    add.run();
                } catch (SVNException e) {
                    if (dstParentKind == SVNNodeKind.NONE) {
                        SVNFileUtil.deleteAll(copyPair.dstParent, true);
                    }
                    throw e;
                }
            } else if (dstParentKind != SVNNodeKind.DIR) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "Path ''{0}'' is not a directory", copyPair.dstParent);
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            dstParentKind = SVNFileType.getNodeKind(SVNFileType.getType(copyPair.dstParent));

            if (dstParentKind != SVNNodeKind.DIR) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_MISSING, "Path ''{0}'' is not a directory", copyPair.dstParent);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
    }

    private boolean verifyPaths(SVNFileType srcType, SVNFileType dstType, SvnCopyPair copyPair, int copyPairsCount, boolean move) throws SVNException {
        if (srcType == SVNFileType.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNKNOWN_KIND, "Path ''{0}'' does not exist", copyPair.source);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        if (dstType != SVNFileType.NONE) {
            if (move && copyPairsCount == 1) {
                File srcDir = SVNFileUtil.getFileDir(copyPair.source);
                File dstDir = SVNFileUtil.getFileDir(copyPair.dst);
                if (srcDir.equals(dstDir)) {
                    // check if it is case-only rename
                    if (copyPair.source.getName().equalsIgnoreCase(copyPair.dst.getName())) {
                        copyPair.dstParent = new File(SVNPathUtil.validateFilePath(dstDir.getAbsolutePath()));
                        copyPair.baseName = SVNFileUtil.getFileName(copyPair.dst);
                        return true;
                    }
                }

            }
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Path ''{0}'' already exists", copyPair.dst);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return false;
    }

    private void verifyPathsExistenceForVirtualCopy(File source, File dst, SVNFileType srcType, SVNFileType dstType, SvnCopyPair copyPair, boolean move) throws SVNException {
        String opName = move ? "move" : "copy";
        if (move && srcType != SVNFileType.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, "Cannot perform 'virtual' {0}: ''{1}'' still exists", new Object[] {
                    opName, copyPair.source
            });
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (dstType == SVNFileType.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "Cannot perform 'virtual' {0}: ''{1}'' does not exist", new Object[] {
                    opName, copyPair.dst
            });
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (dstType == SVNFileType.DIRECTORY) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Cannot perform 'virtual' {0}: ''{1}'' is a directory", new Object[] {
                    opName, copyPair.dst
            });
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (!move && srcType == SVNFileType.DIRECTORY) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Cannot perform 'virtual' {0}: ''{1}'' is a directory", new Object[] {
                    opName, copyPair.source
            });
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        final SvnStatus dstStatus = getStatus(dst);
        if (dstStatus != null &&
                (dstStatus.getNodeStatus() != SVNStatusType.STATUS_UNVERSIONED &&
                        dstStatus.getNodeStatus() != SVNStatusType.STATUS_ADDED &&
                        dstStatus.getNodeStatus() != SVNStatusType.STATUS_REPLACED)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_ATTRIBUTE_INVALID, "Cannot perform 'virtual' {0}: ''{1}'' is scheduled neither for addition nor for replacement",
                    new Object[] {
                            opName, dst
                    });
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        final SvnStatus sourceStatus = getStatus(source);
        if (sourceStatus == null || sourceStatus.getNodeStatus() == SVNStatusType.STATUS_UNVERSIONED)  {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "''{0}'' is not under version control", source);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    private SvnStatus getStatus(File interestingFile) throws SVNException {
        final String interestingPath = SVNFileUtil.getFilePath(interestingFile);

        final SvnStatus[] status2 = new SvnStatus[1];

        final SvnOperationFactory operationFactory = getOperation().getOperationFactory();
        final SvnGetStatus status = operationFactory.createGetStatus();
        status.setDepth(SVNDepth.INFINITY);
        status.setRemote(false);
        status.setReportAll(true);
        status.setReportIgnored(true);
        status.setReportExternals(false);
        status.setApplicalbeChangelists(null);
        status.addTarget(SvnTarget.fromFile(interestingFile.getParentFile()));
        status.setReceiver(new ISvnObjectReceiver<SvnStatus>() {
            public void receive(SvnTarget target, SvnStatus svnStatus) throws SVNException {
                if (svnStatus == null) {
                    return;
                }
                final File path = svnStatus.getPath();
                if (path == null) {
                    return;
                }
                final String statusPath = SVNFileUtil.getFilePath(path);
                if (statusPath.equals(interestingPath)) {
                    status2[0] = svnStatus;
                }
            }
        });
        status.run();
        return status2[0];
    }

    public void move(SVNWCContext context, File source, File dst, boolean metadataOnly) throws SVNException {
        boolean moveDegradedToCopy = copy(context, source, dst, true);
        if (!metadataOnly) {
            SVNFileUtil.rename(source, dst);
        }
        Structure<NodeInfo> nodeInfo = context.getDb().readInfo(source, NodeInfo.kind, NodeInfo.conflicted);
        if (nodeInfo.get(NodeInfo.kind) == SVNWCDbKind.Dir) {
            removeAllConflictMarkers(context.getDb(), source, dst);
        }
        if (nodeInfo.is(NodeInfo.conflicted)) {
            removeAllConflictMarkers(context.getDb(), source, dst);
        }
        SvnNgRemove.delete(getWcContext(), source, moveDegradedToCopy ? null : dst, true, false, this);
    }

    private void removeAllConflictMarkers(ISVNWCDb db, File srcDirAbsPath, File wcDirAbsPath) throws SVNException {
        Map<String, ISVNWCDb.SVNWCDbInfo> children = new HashMap<String, ISVNWCDb.SVNWCDbInfo>();
        Set<String> conflicts = new HashSet<String>();
        db.readChildren(srcDirAbsPath, children, conflicts);

        for (Map.Entry<String, ISVNWCDb.SVNWCDbInfo> entry : children.entrySet()) {
            String name = entry.getKey();
            ISVNWCDb.SVNWCDbInfo info = entry.getValue();

            if (info.conflicted) {
                removeNodeConflictMarkers(db, SVNFileUtil.createFilePath(srcDirAbsPath, name), SVNFileUtil.createFilePath(wcDirAbsPath, name));
            }
            if (info.kind == SVNWCDbKind.Dir) {
                removeAllConflictMarkers(db, SVNFileUtil.createFilePath(srcDirAbsPath, name), SVNFileUtil.createFilePath(wcDirAbsPath, name));
            }

        }
    }

    private void removeNodeConflictMarkers(ISVNWCDb db, File srcAbsPath, File nodeAbsPath) throws SVNException {
        SVNSkel conflict = db.readConflict(srcAbsPath);
        if (conflict != null) {
            File srcDir = SVNFileUtil.getParentFile(srcAbsPath);
            File dstDir = SVNFileUtil.getParentFile(nodeAbsPath);

            List<File> markers = SvnWcDbConflicts.readConflictMarkers((SVNWCDb) db, srcAbsPath, conflict);
            if (markers != null) {
                for (File marker : markers) {
                    File childRelPath = SVNFileUtil.skipAncestor(srcDir, marker);
                    if (childRelPath != null) {
                        File childAbsPath = SVNFileUtil.createFilePath(dstDir, childRelPath);
                        SVNFileUtil.deleteFile(childAbsPath);
                    }
                }
            }
        }
    }

    protected boolean copy(SVNWCContext context, File source, File dst, boolean metadataOnly) throws SVNException {
        SvnCopy operation = getOperation();
        boolean isMove = operation != null && operation.isMove();
        boolean allowMixedRevisions = operation == null || operation.isAllowMixedRevisions();

        File dstDirectory = SVNFileUtil.getParentFile(dst);
        
        Structure<NodeInfo> srcInfo = null;
        try {
            srcInfo = context.getDb().readInfo(source, NodeInfo.status, NodeInfo.kind, NodeInfo.reposRelPath,
                    NodeInfo.reposRootUrl, NodeInfo.reposUuid, NodeInfo.checksum, NodeInfo.conflicted);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "''{0}'' is not under version control", source);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            throw e;
        }

        File srcWcRootAbsPath = context.getDb().getWCRoot(source);

        ISVNWCDb.SVNWCDbStatus srcStatus = srcInfo.get(NodeInfo.status);
        switch (srcStatus) {
        case Deleted:
            if (!metadataOnly) {
                SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS,
                        "Deleted node ''{0}'' can''t be copied.", source);
                SVNErrorManager.error(err1, SVNLogType.WC);
            }
            break;
        case Excluded:
        case ServerExcluded:
        case NotPresent:
            SVNErrorMessage err2 = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, 
                    "The node ''{0}'' was not found.", source);
            SVNErrorManager.error(err2, SVNLogType.WC);
        default:
            break;
        }

        if (isMove && source.equals(srcWcRootAbsPath)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "''{0}'' is the root of a working copy and cannot be moved", source);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        File srcReposRelPath = srcInfo.get(NodeInfo.reposRelPath);
        if (isMove && srcReposRelPath != null && "".equals(SVNFileUtil.getFilePath(srcReposRelPath))) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "''{0}'' represents the repository root and cannot be moved", source);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        Structure<NodeInfo> dstDirInfo = null;
        try {
            dstDirInfo = context.getDb().
                readInfo(dstDirectory, NodeInfo.status, NodeInfo.reposRootUrl, NodeInfo.reposUuid);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "''{0}'' is not under version control", dstDirectory);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }

        SVNURL dstReposRootUrl = dstDirInfo.get(NodeInfo.reposRootUrl);
        String dstReposUuid = dstDirInfo.get(NodeInfo.reposUuid);
        SVNWCDbStatus dstDirStatus = dstDirInfo.get(NodeInfo.status);
        
        dstDirInfo.release();
        
        SVNURL srcReposRootUrl = srcInfo.get(NodeInfo.reposRootUrl);
        String srcReposUuid = srcInfo.get(NodeInfo.reposUuid);

        File dstWcRootAbsPath = context.getDb().getWCRoot(dstDirectory);

        if (srcReposRootUrl == null) {
            if (srcStatus == SVNWCDbStatus.Added) {
                Structure<AdditionInfo> additionInfo = SvnWcDbShared.scanAddition((SVNWCDb) context.getDb(), source);
                srcReposRootUrl = additionInfo.get(AdditionInfo.reposRootUrl);
                srcReposUuid = additionInfo.get(AdditionInfo.reposUuid);
                additionInfo.release();
            } else {
                WCDbRepositoryInfo reposInfo = context.getDb().scanBaseRepository(source, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
                srcReposRootUrl = reposInfo.rootUrl;
                srcReposUuid = reposInfo.uuid;
            }
        }
        
        if (dstReposRootUrl == null) {
            if (dstDirStatus == SVNWCDbStatus.Added) {
                Structure<AdditionInfo> additionInfo = SvnWcDbShared.scanAddition((SVNWCDb) context.getDb(), dstDirectory);
                dstReposRootUrl = additionInfo.get(AdditionInfo.reposRootUrl);
                dstReposUuid = additionInfo.get(AdditionInfo.reposUuid);
                additionInfo.release();
            } else {
                WCDbRepositoryInfo reposInfo = context.getDb().scanBaseRepository(dstDirectory, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
                dstReposRootUrl = reposInfo.rootUrl;
                dstReposUuid = reposInfo.uuid;
            }
        }
        
        if ((srcReposRootUrl != null && dstReposRootUrl != null && !srcReposRootUrl.equals(dstReposRootUrl)) || 
                (srcReposUuid != null && dstReposUuid != null && !srcReposUuid.equals(dstReposUuid))) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SCHEDULE, 
                    "Cannot copy to ''{0}'', as it is not from repository ''{1}''; it is from ''{2}''", dst, srcReposRootUrl, dstReposRootUrl);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (dstDirStatus == SVNWCDbStatus.Deleted) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SCHEDULE, 
                    "Cannot copy to ''{0}'', as it is scheduled for deletion", dst);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        try {
            Structure<NodeInfo> dstInfo = context.getDb().readInfo(dst, NodeInfo.status);
            SVNWCDbStatus dstStatus = dstInfo.get(NodeInfo.status);
            switch (dstStatus) {
            case Excluded:
                SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, 
                        "''{0}'' is already under version control but is excluded.", dst);
                SVNErrorManager.error(err1, SVNLogType.WC);
                break;
            case ServerExcluded:
                SVNErrorMessage err2 = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, 
                        "''{0}'' is already under version control", dst);
                SVNErrorManager.error(err2, SVNLogType.WC);
                break;
            case Deleted:
            case NotPresent:
                break;
            default:
                if (!metadataOnly) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS,
                            "There is already a versioned item ''{0}''", dst);
                    SVNErrorManager.error(err, SVNLogType.WC);
                    break;
                }
            }
            dstInfo.release();
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
        }
        
        if (!metadataOnly) {
            SVNFileType dstType = SVNFileType.getType(dst);
            if (dstType != SVNFileType.NONE) {
                SVNErrorMessage err2 = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, 
                        "''{0}'' already exists and is in the way", dst);
                SVNErrorManager.error(err2, SVNLogType.WC);
            }
        }
        
        File tmpDir = context.getDb().getWCRootTempDir(dstDirectory);
        boolean withinOneWc = srcWcRootAbsPath.equals(dstWcRootAbsPath);

        boolean moveDegradedToCopy = false;

        if (isMove && !withinOneWc) {
            moveDegradedToCopy = true;
            isMove = false;
        }

        if (!withinOneWc) {
            SvnWcDbPristines.transferPristine((SVNWCDb)context.getDb(), source, dstWcRootAbsPath);
        }

        if (srcInfo.get(NodeInfo.kind) == SVNWCDbKind.File || srcInfo.get(NodeInfo.kind) == SVNWCDbKind.Symlink) {
            final boolean shouldCopyBaseData = shouldCopyBaseData(context, source, metadataOnly, srcStatus);

            if (shouldCopyBaseData && getOperation().isVirtual()) {//we check for "virtual" to preserve current behaviour in this case
                copyBaseDataOfFile(context, source, dst);
            } else {
                copyVersionedFile(context, source, dst, dst, tmpDir, metadataOnly, srcInfo.is(NodeInfo.conflicted), isMove, true);
            }
        } else {
            if (isMove && srcStatus == SVNWCDbStatus.Normal) {
                long[] minMaxRevisions = context.getDb().minMaxRevisions(source, false);
                long minRevision = minMaxRevisions[0];
                long maxRevision = minMaxRevisions[1];
                if (SVNRevision.isValidRevisionNumber(minRevision) && SVNRevision.isValidRevisionNumber(maxRevision) && minRevision != maxRevision) {
                    if (!allowMixedRevisions) {
                        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_MIXED_REVISIONS,
                                "Cannot move mixed-revision subtree ''{0}'' [{1}:{2}]; try updating it first", source, minRevision, maxRevision);
                        SVNErrorManager.error(errorMessage, SVNLogType.WC);
                    }
                    isMove = false;
                    moveDegradedToCopy = true;
                }
            }
            if (srcStatus == SVNWCDbStatus.Deleted && metadataOnly) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Cannot perform 'virtual' {0}: ''{1}'' is a directory", new Object[]{
                        isMove ? "move" : "copy", source
                });
                SVNErrorManager.error(err, SVNLogType.WC);
            } else {
                copyVersionedDirectory(context, source, dst, dst, tmpDir, metadataOnly, isMove, true);
            }
        }

        if (isMove) {
            context.getDb().opHandleMoveBack(dst, source, null);
        }
        context.wqRun(dst);
        return moveDegradedToCopy;
    }

    private boolean shouldCopyBaseData(SVNWCContext context, File source, boolean metadataOnly, SVNWCDbStatus srcStatus) throws SVNException {
        if (!metadataOnly) {
            return false;
        }

        if (srcStatus == SVNWCDbStatus.Deleted) {
            return true;
        }

        final SvnStatus svnStatus = SVNStatusEditor17.internalStatus(context, source);
        return svnStatus != null && svnStatus.getNodeStatus() == SVNStatusType.STATUS_REPLACED;
    }

    private void copyBaseDataOfFile(SVNWCContext context, File source, File dst) throws SVNException {
        final SVNProperties pristineProps = context.getPristineProps(source);

        final ISVNWCDb.WCDbBaseInfo baseInfo = context.getDb().getBaseInfo(source,
                ISVNWCDb.WCDbBaseInfo.BaseInfoField.changedAuthor, ISVNWCDb.WCDbBaseInfo.BaseInfoField.changedDate, ISVNWCDb.WCDbBaseInfo.BaseInfoField.changedRev,
                ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum, ISVNWCDb.WCDbBaseInfo.BaseInfoField.revision,
                ISVNWCDb.WCDbBaseInfo.BaseInfoField.reposRootUrl, ISVNWCDb.WCDbBaseInfo.BaseInfoField.reposUuid);

        final String changedAuthor = baseInfo.changedAuthor;
        final SVNDate changedDate = baseInfo.changedDate;
        final long changedRev = baseInfo.changedRev;
        final SvnChecksum checksum = baseInfo.checksum;
        final long revision = baseInfo.revision;
        final SVNURL reposRootUrl = baseInfo.reposRootUrl;
        final String reposUuid = baseInfo.reposUuid;


        context.getDb().opCopyFile(dst, pristineProps, changedRev, changedDate, changedAuthor,
                context.getNodeReposRelPath(source.getAbsoluteFile()), reposRootUrl, reposUuid, revision, checksum, false, null, null, null);

        final SVNEvent event = SVNEventFactory.createSVNEvent(dst, SVNNodeKind.FILE, null, SVNRepository.INVALID_REVISION, SVNEventAction.COPY, null, null, null);
        handleEvent(event);
    }

    private void copyVersionedDirectory(SVNWCContext wcContext, File srcAbsPath, File dstAbsPath, File dstOpRootAbsPath, File tmpDirAbsPath, boolean metadataOnly, boolean isMove, boolean notify) throws SVNException {
        SVNSkel workItems = null;
        File dirAbsPath = SVNFileUtil.getParentFile(dstAbsPath);
        SVNNodeKind diskKind = SVNNodeKind.UNKNOWN;

        if (!metadataOnly) {
            CopyToTmpDir copyToTmpDir = copyToTmpDir(srcAbsPath, dstAbsPath, tmpDirAbsPath, false, false);
            workItems = copyToTmpDir.workItem;
            diskKind = copyToTmpDir.kind;
        }
        wcContext.getDb().opCopy(srcAbsPath, dstAbsPath, dstOpRootAbsPath, isMove, workItems);

        if (notify && wcContext.getEventHandler() != null) {
            SVNEvent event = SVNEventFactory.createSVNEvent(dstAbsPath, SVNNodeKind.DIR, null, SVNRepository.INVALID_REVISION, SVNEventAction.ADD, SVNEventAction.ADD, null, null);
            if (workItems != null) {
                wcContext.wqRun(dirAbsPath);
            }
            wcContext.getEventHandler().handleEvent(event, UNKNOWN);
        }

        Set<String> diskChildren = null;
        if (!metadataOnly && diskKind == SVNNodeKind.DIR) {
            File[] files = SVNFileListUtil.listFiles(srcAbsPath);
            if (files != null) {
                diskChildren = new HashSet<String>();
                for (File file : files) {
                    diskChildren.add(SVNFileUtil.getFileName(file));
                }
            } else {
                diskChildren = null;
            }
        } else {
            diskChildren = null;
        }

        Map<String, ISVNWCDb.SVNWCDbInfo> versionedChildren = new HashMap<String, ISVNWCDb.SVNWCDbInfo>();
        Set<String> conflictedChildren = new HashSet<String>();
        wcContext.getDb().readChildren(srcAbsPath, versionedChildren, conflictedChildren);

        for (final Map.Entry<String, ISVNWCDb.SVNWCDbInfo> entry : versionedChildren.entrySet()) {
            if (wcContext.getEventHandler() != null) {
                wcContext.getEventHandler().checkCancelled();
            }
            String childName = entry.getKey();
            ISVNWCDb.SVNWCDbInfo info = entry.getValue();

            File childSrcAbsPath = SVNFileUtil.createFilePath(srcAbsPath, childName);
            File childDstAbsPath = SVNFileUtil.createFilePath(dstAbsPath, childName);

            if (info.opRoot) {
                wcContext.getDb().opCopyShadowedLayer(childSrcAbsPath, childDstAbsPath, isMove);
            }

            if (info.status == SVNWCDbStatus.Normal || info.status == SVNWCDbStatus.Added) {
                if (info.kind == SVNWCDbKind.File) {
                    if (!info.fileExternal) {
                        copyVersionedFile(wcContext, childSrcAbsPath, childDstAbsPath, dstOpRootAbsPath, tmpDirAbsPath, metadataOnly, info.conflicted, isMove, false);
                    }
                } else if (info.kind == SVNWCDbKind.Dir) {
                    copyVersionedDirectory(wcContext, childSrcAbsPath, childDstAbsPath, dstOpRootAbsPath, tmpDirAbsPath, metadataOnly, isMove, false);
                } else {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, "cannot handle node kind for ''{0}''", childSrcAbsPath);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            } else if (info.status == SVNWCDbStatus.Deleted || info.status == SVNWCDbStatus.NotPresent || info.status == SVNWCDbStatus.Excluded) {
                wcContext.getDb().opCopy(childSrcAbsPath, childDstAbsPath, dstOpRootAbsPath, isMove, null);
            } else if (info.status == SVNWCDbStatus.Incomplete) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot handle status of ''{0}''", childSrcAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            } else {
                assert (info.status == SVNWCDbStatus.ServerExcluded);
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot copy ''{0}'' excluded by server", childSrcAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            if (diskChildren != null && (info.status == SVNWCDbStatus.Normal || info.status == SVNWCDbStatus.Added)) {
                diskChildren.remove(childName);
            }
        }

        if (diskChildren != null && diskChildren.size() > 0) {
            List<File> markerFiles = wcContext.getDb().getConflictMarkerFiles(srcAbsPath);

            workItems = null;

            for (String name : diskChildren) {
                name = SVNPathUtil.tail(name);

                if (name.equals(SVNFileUtil.getAdminDirectoryName())) {
                    continue;
                }

                if (wcContext.getEventHandler() != null) {
                    wcContext.getEventHandler().checkCancelled();
                }

                File unverSrcAbsPath = SVNFileUtil.createFilePath(srcAbsPath, name);
                File unverDstAbsPath = SVNFileUtil.createFilePath(dstAbsPath, name);

                if (markerFiles != null && markerFiles.contains(unverSrcAbsPath)) {
                    continue;
                }
                CopyToTmpDir copyToTmpDir = copyToTmpDir(unverSrcAbsPath, unverDstAbsPath, tmpDirAbsPath, true, true);
                SVNSkel workItem = copyToTmpDir.workItem;

                if (workItem != null) {
                    workItems = SVNWCContext.wqMerge(workItems, workItem);
                }
            }

            wcContext.getDb().addWorkQueue(dstAbsPath, workItems);
        }
    }

    private void copyVersionedFile(SVNWCContext wcContext, File srcAbsPath, File dstAbsPath, File dstOpRootAbsPath, File tmpDirAbsPath, boolean metadataOnly, boolean conflicted, boolean isMove, boolean notify) throws SVNException {
        SVNSkel workItems = null;
        if (!metadataOnly) {
            File mySrcAbsPath = srcAbsPath;
            boolean handleAsUnversioned = false;
            if (conflicted) {
                SVNSkel conflict = wcContext.getDb().readConflict(srcAbsPath);

                File conflictWorking;
                try {
                    Structure<SvnWcDbConflicts.TextConflictInfo> conflictInfoStructure = SvnWcDbConflicts.readTextConflict(wcContext.getDb(), srcAbsPath, conflict);
                    conflictWorking = conflictInfoStructure.get(SvnWcDbConflicts.TextConflictInfo.mineAbsPath);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_MISSING) {
                        throw e;
                    }
                    conflictWorking = null;
                }

                if (conflictWorking != null) {
                    if (SVNFileType.getType(conflictWorking) == SVNFileType.FILE) {
                        handleAsUnversioned = true;
                        mySrcAbsPath = conflictWorking;
                    }
                }
            }
            CopyToTmpDir copyToTmpDir = copyToTmpDir(mySrcAbsPath, dstAbsPath, tmpDirAbsPath, true, handleAsUnversioned);
            workItems = copyToTmpDir.workItem;
        }
        
        wcContext.getDb().opCopy(srcAbsPath, dstAbsPath, dstOpRootAbsPath, isMove, workItems);
        wcContext.wqRun(SVNFileUtil.getParentFile(dstAbsPath));
        if (notify) {
            if (workItems != null) {
                getWcContext().wqRun(dstAbsPath);
            }
            SVNEvent event = SVNEventFactory.createSVNEvent(dstAbsPath, SVNNodeKind.FILE, null, -1, SVNEventAction.ADD, SVNEventAction.ADD, null, null, 1, 1);
            handleEvent(event, -1);
        }
    }
    
    private CopyToTmpDir copyToTmpDir(File srcAbsPath, File dstAbsPath, File tmpDirAbsPath, boolean fileCopy, boolean unversioned) throws SVNException {
        boolean deleteOnClose = false;

        CopyToTmpDir copyToTmpDir = new CopyToTmpDir();
        copyToTmpDir.workItem = null;

        SVNFileType type = SVNFileType.getType(srcAbsPath);
        copyToTmpDir.kind = SVNFileType.getNodeKind(type);

        boolean isSpecial = type == SVNFileType.SYMLINK;

        if (copyToTmpDir.kind == SVNNodeKind.NONE) {
            return copyToTmpDir;
        } else if (copyToTmpDir.kind == SVNNodeKind.UNKNOWN) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, "Source ''{0}'' is unexpected kind", srcAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        } else if (copyToTmpDir.kind == SVNNodeKind.DIR) {
            deleteOnClose = true;
        } else {
            deleteOnClose = false;
        }
        File dstTmpAbsPath = null;
        try {
            if (fileCopy && !unversioned) {
                boolean modified = getWcContext().isTextModified(srcAbsPath, false) || getWcContext().isPropsModified(srcAbsPath);
                if (!modified) {
                    copyToTmpDir.workItem = getWcContext().wqBuildFileInstall(dstAbsPath, null, false, true);
                    return copyToTmpDir;
                }
            }

            dstTmpAbsPath = copyToTmpDir.kind == SVNNodeKind.DIR ? SVNFileUtil.createUniqueDir(tmpDirAbsPath, SVNFileUtil.getFileName(srcAbsPath), ".tmp", false) : SVNFileUtil.createUniqueFile(tmpDirAbsPath, SVNFileUtil.getFileName(srcAbsPath), ".tmp", false);
            if (copyToTmpDir.kind == SVNNodeKind.DIR) {
                if (fileCopy) {
                    SVNFileUtil.copyDirectory(srcAbsPath, dstTmpAbsPath, false, getOperation().getEventHandler());
                } else {
                    SVNFileUtil.ensureDirectoryExists(dstTmpAbsPath);
                }
            } else if (!isSpecial) {
                SVNFileUtil.copyFile(srcAbsPath, dstTmpAbsPath, false, true);
            } else {
                SVNFileUtil.deleteFile(dstTmpAbsPath);
                SVNFileUtil.copySymlink(srcAbsPath, dstTmpAbsPath);
            }

            if (fileCopy) {
                SVNFileUtil.setReadonly(dstTmpAbsPath, false);
            }
            copyToTmpDir.workItem = getWcContext().wqBuildFileMove(dstAbsPath, dstTmpAbsPath, dstAbsPath);
            return copyToTmpDir;
        } finally {
            if (dstTmpAbsPath != null && deleteOnClose && copyToTmpDir.kind == SVNNodeKind.FILE) {
                SVNFileUtil.deleteFile(dstTmpAbsPath);
            }
        }
    }

    private static class CopyToTmpDir {
        SVNSkel workItem;
        SVNNodeKind kind;
    }
    
    private static class SvnCopyPair {
        File source;
        File dst;
        File dstParent;
        
        String baseName;
    }
}
