package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
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
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.PristineContentsInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDbDir;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDbRoot;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.AdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.ExternalNodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbExternals;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbReader;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
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
        Collection<SvnCopySource> sources = getOperation().getSources();
        try {
            return tryRun(context, sources, getFirstTarget());
        } catch (SVNException e) {
            SVNErrorCode code = e.getErrorMessage().getErrorCode();
            if (!getOperation().isFailWhenDstExists()
                    && getOperation().getSources().size() == 1 
                    && (code == SVNErrorCode.ENTRY_EXISTS || code == SVNErrorCode.FS_ALREADY_EXISTS)) {
                SvnCopySource source = sources.iterator().next();
                return tryRun(context, sources, new File(getFirstTarget(), source.getSource().getFile().getName()));
            }
            throw e;            
        } finally {
            sleepForTimestamp();
        }        
    }

    protected Void tryRun(SVNWCContext context, Collection<SvnCopySource> sources, File target) throws SVNException {
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
    private Void disjointCopy(SVNWCContext context, File nestedWC) throws SVNException {
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

        return null;
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

    private Void copy(SVNWCContext context, Collection<SvnCopySource> sources, File target) throws SVNException {
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
                    Structure<ExternalNodeInfo> externalInfo = SvnWcDbExternals.readExternal(context, src, src, ExternalNodeInfo.kind);
                    if (externalInfo.hasValue(ExternalNodeInfo.kind) && externalInfo.get(ExternalNodeInfo.kind) != SVNNodeKind.NONE) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CANNOT_MOVE_FILE_EXTERNAL, 
                                "Cannot move the external at ''{0}''; please edit the svn:externals property on ''{1}''.", src);
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
            move(copyPairs);
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
                    copy(context, copyPair.source, dstPath, getOperation().isVirtual());
                }
            } finally {
                context.releaseWriteLock(dstAncestor);
            }
        }
        
        return null;
    }

    private void move(Collection<SvnCopyPair> pairs) throws SVNException {
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
                
                move(getWcContext(), copyPair.source, SVNFileUtil.createFilePath(copyPair.dstParent, copyPair.baseName), getOperation().isVirtual());
            } finally {
                for (File file : lockedPaths) {
                    getWcContext().releaseWriteLock(file);
                }
            }
        }
    }

    private void verifyPaths(Collection<SvnCopyPair> copyPairs, boolean makeParents, boolean move) throws SVNException {
        for (SvnCopyPair copyPair : copyPairs) {
            SVNFileType srcType = SVNFileType.getType(copyPair.source);
            SVNFileType dstType = SVNFileType.getType(copyPair.dst);

            if (getOperation().isVirtual()) {
                verifyPathsExistenceForVirtualCopy(copyPair.source, copyPair.dst, srcType, dstType, copyPair, move);
            } else {
                final boolean caseOnlyRename = verifyPaths(srcType, dstType, copyPair, copyPairs.size(), move);
                if (caseOnlyRename) {
                    return;
                }
            }
            copyPair.dstParent = new File(SVNPathUtil.validateFilePath(SVNFileUtil.getParentFile(copyPair.dst).getAbsolutePath()));
            copyPair.baseName = SVNFileUtil.getFileName(copyPair.dst);

            final SVNFileType dstParentType = SVNFileType.getType(copyPair.dstParent);
            if (makeParents && (dstParentType == SVNFileType.NONE || getOperation().isVirtual())) {
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
                    if (dstParentType == SVNFileType.NONE) {
                        SVNFileUtil.deleteAll(copyPair.dstParent, true);
                    }
                    throw e;
                }
            } else if (dstParentType != SVNFileType.DIRECTORY) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY, "Path ''{0}'' is not a directory", copyPair.dstParent);
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

    protected void move(SVNWCContext context, File source, File dst, boolean metadataOnly) throws SVNException {
        copy(context, source, dst, true);
        if (!metadataOnly) {
            SVNFileUtil.rename(source, dst);
        }
        Structure<NodeInfo> nodeInfo = context.getDb().readInfo(source, NodeInfo.kind, NodeInfo.conflicted);
        if (nodeInfo.get(NodeInfo.kind) == SVNWCDbKind.Dir) {
            // TODO remove conflict markers
        }
        if (nodeInfo.is(NodeInfo.conflicted)) {
            // TODO remove conflict markers
        }
        SvnNgRemove.delete(getWcContext(), source, true, false, this);
    }

    protected void copy(SVNWCContext context, File source, File dst, boolean metadataOnly) throws SVNException {
        File dstDirectory = SVNFileUtil.getParentFile(dst);
        
        Structure<NodeInfo> srcInfo = null;
        try {
            srcInfo = context.getDb().readInfo(source, NodeInfo.status, NodeInfo.kind, NodeInfo.reposRootUrl, NodeInfo.reposUuid, NodeInfo.checksum, NodeInfo.conflicted);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "''{0}'' is not under version control", source);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            throw e;
        }
        
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
        
        Structure<NodeInfo> dstDirInfo = context.getDb().
            readInfo(dstDirectory, NodeInfo.status, NodeInfo.reposRootUrl, NodeInfo.reposUuid);

        SVNURL dstReposRootUrl = dstDirInfo.get(NodeInfo.reposRootUrl);
        String dstReposUuid = dstDirInfo.get(NodeInfo.reposUuid);
        SVNWCDbStatus dstDirStatus = dstDirInfo.get(NodeInfo.status);
        
        dstDirInfo.release();
        
        SVNURL srcReposRootUrl = srcInfo.get(NodeInfo.reposRootUrl);
        String srcReposUuid = srcInfo.get(NodeInfo.reposUuid);
        ISVNWCDb.SVNWCDbKind srcKind = srcInfo.get(NodeInfo.kind);
        SvnChecksum srcChecksum = srcInfo.get(NodeInfo.checksum);
        boolean srcConflicted = srcInfo.is(NodeInfo.conflicted); 
        
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
        
        File tmpDir = context.getDb().getWCRootTempDir(dst);
        if (srcKind == SVNWCDbKind.File || srcKind == SVNWCDbKind.Symlink) {

            final boolean shouldCopyBaseData = shouldCopyBaseData(context, source, metadataOnly, srcStatus);

            if (shouldCopyBaseData)  {
                copyBaseDataOfFile(context, source, dst);
            } else {
                copyVersionedFile(context, source, dst, dst, tmpDir, srcChecksum, metadataOnly, srcConflicted, true);
            }
        } else {
            if (srcStatus == SVNWCDbStatus.Deleted && metadataOnly) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Cannot perform 'virtual' {0}: ''{1}'' is a directory", new Object[] {
                        getOperation().isMove() ? "move" : "copy", source
                });
                SVNErrorManager.error(err, SVNLogType.WC);
            } else {
                copyVersionedDirectory(context, source, dst, dst, tmpDir, metadataOnly, true);
            }
        }
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

        final String relativePath = SVNPathUtil.getRelativePath(context.getDb().getWCRoot(source).getAbsolutePath(), source.getAbsolutePath());

        context.getDb().opCopyFile(dst, pristineProps, changedRev, changedDate, changedAuthor,
                new File(relativePath), reposRootUrl, reposUuid, revision, checksum, null, null);

        final SVNEvent event = SVNEventFactory.createSVNEvent(dst, SVNNodeKind.FILE, null, SVNRepository.INVALID_REVISION, SVNEventAction.COPY, null, null, null);
        handleEvent(event);
    }

    private void copyVersionedDirectory(SVNWCContext wcContext, File source, File dst, File dstOpRoot, File tmpDir, boolean metadataOnly, boolean notify) throws SVNException {
        SVNSkel workItems = null;
        SVNFileType srcType = null;
        
        if (!metadataOnly) {
            srcType = SVNFileType.getType(source);
            File tmpDst = copyToTmpDir(source, tmpDir, false);
            if (tmpDst != null) {
                SVNSkel workItem = wcContext.wqBuildFileMove(tmpDst, dst);
                workItems = wcContext.wqMerge(workItems, workItem);
            }
        }
        wcContext.getDb().opCopy(source, dst, workItems);
        wcContext.wqRun(SVNFileUtil.getParentFile(dst));
        
        if (notify) {
            SVNEvent event = SVNEventFactory.createSVNEvent(dst, SVNNodeKind.DIR, null, -1, SVNEventAction.COPY, SVNEventAction.COPY, null, null, 1, 1);
            handleEvent(event);
        }
        
        Set<String> diskChildren = null;
        if (!metadataOnly && srcType == SVNFileType.DIRECTORY) {
            File[] children = SVNFileListUtil.listFiles(source);
            diskChildren = new HashSet<String>();
            for (int i = 0; children != null && i < children.length; i++) {
                String name = SVNFileUtil.getFileName(children[i]);
                if (!name.equals(SVNFileUtil.getAdminDirectoryName())) {
                    diskChildren.add(name);
                }
            }
        }
        
        Set<String> versionedChildren = wcContext.getDb().readChildren(source);
        for (String childName : versionedChildren) {
            checkCancelled();
            File childSrcPath = SVNFileUtil.createFilePath(source, childName);
            File childDstPath = SVNFileUtil.createFilePath(dst, childName);
            
            Structure<NodeInfo> childInfo = wcContext.getDb().readInfo(childSrcPath, NodeInfo.status, NodeInfo.kind, NodeInfo.checksum, NodeInfo.conflicted,
                    NodeInfo.opRoot);
            
            if (childInfo.is(NodeInfo.opRoot)) {
                wcContext.getDb().opCopyShadowedLayer(childSrcPath, childDstPath);
            }
            SVNWCDbStatus childStatus = childInfo.get(NodeInfo.status);
            SVNWCDbKind childKind = childInfo.get(NodeInfo.kind);
            if (childStatus == SVNWCDbStatus.Normal || childStatus == SVNWCDbStatus.Added || childStatus == SVNWCDbStatus.Incomplete) {
                if (childKind == SVNWCDbKind.File) {
                    boolean skip = false;
                    if (childStatus == SVNWCDbStatus.Normal) {
                        Structure<NodeInfo> baseChildInfo = SvnWcDbReader.getBaseInfo((SVNWCDb) wcContext.getDb(), childSrcPath, NodeInfo.updateRoot);
                        skip = baseChildInfo.is(NodeInfo.updateRoot);
                        baseChildInfo.release();
                    }
                    if (!skip) {
                        SvnChecksum checksum = childInfo.get(NodeInfo.checksum);
                        boolean conflicted = childInfo.is(NodeInfo.conflicted);
                        copyVersionedFile(wcContext, childSrcPath, childDstPath, dstOpRoot, tmpDir, checksum, metadataOnly, conflicted, false);
                    }
                } else if (childKind == SVNWCDbKind.Dir) {
                    copyVersionedDirectory(wcContext, childSrcPath, childDstPath, dstOpRoot, tmpDir, metadataOnly, false);
                } else {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, "cannot handle node kind for ''{0}''", childSrcPath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            } else if (childStatus == SVNWCDbStatus.Deleted ||
                    childStatus == SVNWCDbStatus.NotPresent ||
                    childStatus == SVNWCDbStatus.Excluded) {
                wcContext.getDb().opCopy(childSrcPath, childDstPath, null);                
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, 
                        "Cannot copy ''{0}'' excluded by server", childSrcPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            childInfo.release();
            if (diskChildren != null && 
                    (childStatus == SVNWCDbStatus.Normal  || childStatus == SVNWCDbStatus.Added)) {
                diskChildren.remove(childName);
            }
        }
        
        if (diskChildren != null && !diskChildren.isEmpty()) {
            // TODO get and skip conflict markers.
            for (String childName : diskChildren) {
                checkCancelled();
                
                File childSrcPath = SVNFileUtil.createFilePath(source, childName);
                File childDstPath = SVNFileUtil.createFilePath(dst, childName);
                File tmp = copyToTmpDir(childSrcPath, tmpDir, true);
                if (tmp != null) {
                    SVNSkel moveItem = wcContext.wqBuildFileMove(SVNFileUtil.getParentFile(dst), tmp, childDstPath);
                    getWcContext().getDb().addWorkQueue(dst, moveItem);
                }
            }
            getWcContext().wqRun(dst);
        }
    }

    private void copyVersionedFile(SVNWCContext wcContext, File source, File dst, File dstOpRoot, File tmpDir, SvnChecksum srcChecksum, boolean metadataOnly, boolean conflicted, boolean notify) throws SVNException {
        if (srcChecksum != null) {
            if (!wcContext.getDb().checkPristine(dst, srcChecksum)) {
                SvnChecksum md5 = wcContext.getDb().getPristineMD5(source, srcChecksum);
                PristineContentsInfo pristine = wcContext.getPristineContents(source, false, true);
                File tempFile = SVNFileUtil.createUniqueFile(tmpDir, dst.getName(), ".tmp", false);
                SVNFileUtil.copyFile(pristine.path, tempFile, false);
                wcContext.getDb().installPristine(tempFile, srcChecksum, md5);
            }
        }
        
        SVNSkel workItems = null;
        File toCopy = source;
        if (!metadataOnly) {
            if (conflicted) {
                File conflictWorking = null;
                List<SVNConflictDescription> conflicts = wcContext.getDb().readConflicts(source);
                
                for (SVNConflictDescription conflictDescription : conflicts) {
                    if (conflictDescription.isTextConflict()) {
                        conflictWorking = conflictDescription.getPath();
                        break;
                    }
                }
                if (conflictWorking != null) {
                    if (SVNFileType.getType(conflictWorking) == SVNFileType.FILE) {
                        toCopy = conflictWorking;
                    }
                }
            }
            File tmpDst = copyToTmpDir(toCopy, tmpDir, true);
            if (tmpDst != null) {
                boolean needsLock = wcContext.getProperty(source, SVNProperty.NEEDS_LOCK) != null;
                if (needsLock) {
                    SVNFileUtil.setReadonly(tmpDst, false);
                }
                SVNSkel workItem = wcContext.wqBuildFileMove(tmpDst, dst);
                workItems = wcContext.wqMerge(workItems, workItem);
                
                SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(tmpDst));
                if (kind == SVNNodeKind.FILE) {
                    if (!wcContext.isTextModified(source, false)) {
                        SVNSkel workItem2 = wcContext.wqBuildRecordFileinfo(dst, null);
                        workItems = wcContext.wqMerge(workItems, workItem2);
                    }
                }
            }
        }
        
        wcContext.getDb().opCopy(source, dst, workItems);
        wcContext.wqRun(SVNFileUtil.getParentFile(dst));
        if (notify) {
            SVNEvent event = SVNEventFactory.createSVNEvent(dst, SVNNodeKind.FILE, null, -1, SVNEventAction.COPY, SVNEventAction.COPY, null, null, 1, 1);
            handleEvent(event, -1);
        }
    }
    
    private File copyToTmpDir(File source, File tmpDir, boolean recursive) throws SVNException {
        SVNFileType sourceType = SVNFileType.getType(source); 
        boolean special =  sourceType == SVNFileType.SYMLINK;
        if (sourceType == SVNFileType.NONE) {
            return null;
        } else if (sourceType == SVNFileType.UNKNOWN) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, 
                    "Source ''{0}'' is unexpected kind", source);
            SVNErrorManager.error(err, SVNLogType.WC);
        } 
        
        File dstPath = SVNFileUtil.createUniqueFile(tmpDir, source.getName(), ".tmp", false);
        if (sourceType == SVNFileType.DIRECTORY || special) {
            SVNFileUtil.deleteFile(dstPath);
        }
        
        if (sourceType == SVNFileType.DIRECTORY) {
            if (recursive) {
                SVNFileUtil.copyDirectory(source, dstPath, true, getOperation().getEventHandler());
            } else {
                SVNFileUtil.ensureDirectoryExists(dstPath);
            }
        } else if (!special) {
            SVNFileUtil.copyFile(source, dstPath, false, false);
        } else {
            SVNFileUtil.createSymlink(dstPath, SVNFileUtil.getSymlinkName(source));
        }
        return dstPath;
    }
    
    private static class SvnCopyPair {
        File source;
        File dst;
        File dstParent;
        
        String baseName;
    }
}
