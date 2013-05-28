package org.tmatesoft.svn.core.internal.wc17;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.ISVNUpdateEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNStatusEditor;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields;
import org.tmatesoft.svn.core.internal.wc2.ng.ISvnDiffCallback;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffCallbackResult;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

public class SVNDiffEditor17 implements ISVNUpdateEditor {
    private long targetRevision;
    private boolean isRootOpen;
    private File localTarget;
    private SVNDepth depth;
    private SVNDirectoryInfo currentDirectory;
    private SVNWCContext wcContext;
    private File workingCopyRoot;
    private boolean isCompareToBase;
    private boolean isReverseDiff;
    private ISvnDiffCallback diffCallback;
    private boolean useAncestry;
    private SVNFileInfo currentFile;
    private SVNDeltaProcessor deltaProcessor;
    private File tempDirectory;
    private Collection<String> changeLists;
    private String relativeToDirectory;

    private boolean diffUnversioned;
    private boolean diffCopiedAsAdded;

    public SVNDiffEditor17(SVNWCContext wcContext, File workingCopyRoot, File localTarget, SVNDepth depth,
                           boolean compareToBase, boolean reverseDiff, ISvnDiffCallback diffCallback, boolean useAncestry,
                           Collection<String> changeLists, boolean diffUnversioned, boolean diffCopiedAsAdded) {
        this.wcContext = wcContext;
        this.workingCopyRoot = workingCopyRoot;
        this.localTarget = localTarget;
        this.depth = depth;
        isCompareToBase = compareToBase;
        isReverseDiff = reverseDiff;
        this.diffCallback = diffCallback;
        this.useAncestry = useAncestry;
        this.changeLists = changeLists;
        this.diffUnversioned = diffUnversioned;
        this.diffCopiedAsAdded = diffCopiedAsAdded;
        this.deltaProcessor = new SVNDeltaProcessor();
    }

    public long getTargetRevision() {
        return targetRevision;
    }

    public File getWorkingCopyRoot() {
        return workingCopyRoot;
    }

    public ISvnDiffCallback getDiffCallback() {
        return diffCallback;
    }

    private SvnDiffCallbackResult getDiffCallbackResult() {
        return null;
    }

    private boolean isDiffCopiedAsAdded() {
        return diffCopiedAsAdded;
    }

    private boolean isDiffUnversioned() {
        return diffUnversioned;
    }

    private File createTempDirectory() throws SVNException {
        return SVNFileUtil.createTempDirectory("diff");
    }

    public void targetRevision(long revision) throws SVNException {
        this.targetRevision = revision;
    }

    public void openRoot(long revision) throws SVNException {
        this.isRootOpen = true;
        currentDirectory = createDirInfo(null, "", false, depth);
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        File fullPath = new File(getWorkingCopyRoot(), path);

        final SVNWCContext.ScheduleInternalInfo schedule;
        final Structure<StructureFields.NodeInfo> nodeInfo;
        try {
            schedule = wcContext.getNodeScheduleInternal(fullPath, true, false);
            nodeInfo = wcContext.getDb().readInfo(fullPath, StructureFields.NodeInfo.kind);
        } catch (SVNException e) {
            //TODO (if doesn't exist call exit this method)
            throw e;
        }

        String name = SVNPathUtil.tail(path);
        currentDirectory.comparedEntries.add(name);
        if (!isCompareToBase &&  schedule.schedule == SVNWCContext.SVNWCSchedule.delete) {
            return;
        }
        final ISVNWCDb.SVNWCDbKind nodeKind = nodeInfo.get(StructureFields.NodeInfo.kind);
        if (nodeKind == ISVNWCDb.SVNWCDbKind.File) {
            if (isReverseDiff) {
                final SVNProperties baseProps = wcContext.getDb().getBaseProps(fullPath);
                getDiffCallback().fileDeleted(getDiffCallbackResult(), fullPath, null, null, null, null, baseProps);
            } else {
                reportAddedFile(currentDirectory, fullPath);
            }
        } else if (nodeKind == ISVNWCDb.SVNWCDbKind.Dir) {
            SVNDirectoryInfo info = createDirInfo(currentDirectory, path, false, SVNDepth.INFINITY);
            reportAddedDir(info);
        }
    }

    private void reportAddedDir(SVNDirectoryInfo info) throws SVNException {
        final File fullPath = new File(getWorkingCopyRoot(), info.path);
        if (wcContext.matchesChangelist(new File(getWorkingCopyRoot(), info.path), changeLists)) {
            SVNProperties wcProps;
            if (isCompareToBase) {
                wcProps = wcContext.getPristineProps(fullPath);
            } else {
                wcProps = wcContext.getActualProps(fullPath);
            }
            SVNProperties propDiff = computePropsDiff(new SVNProperties(), wcProps);
            if (!propDiff.isEmpty()) {
                getDiffCallback().dirPropsChanged(getDiffCallbackResult(), new File(getWorkingCopyRoot(), info.path), true, propDiff, null);
            }
        }

        final List<File> children = wcContext.getNodeChildren(fullPath, false);
        for (File child : children) {
            final SVNWCContext.ScheduleInternalInfo schedule = wcContext.getNodeScheduleInternal(child, true, false);

            if (!isCompareToBase && schedule.schedule == SVNWCContext.SVNWCSchedule.delete) {
                continue;
            }

            final ISVNWCDb.WCDbBaseInfo baseInfo = wcContext.getDb().getBaseInfo(child, ISVNWCDb.WCDbBaseInfo.BaseInfoField.kind);

            if (baseInfo.kind == ISVNWCDb.SVNWCDbKind.File) {
                reportAddedFile(info, child);
            } else if (baseInfo.kind == ISVNWCDb.SVNWCDbKind.Dir) {
                if (info.depth.compareTo(SVNDepth.FILES) > 0 ||
                    info.depth == SVNDepth.UNKNOWN) {
                    SVNDepth depthBelowHere = info.depth;
                    if (depthBelowHere == SVNDepth.IMMEDIATES) {
                        depthBelowHere = SVNDepth.EMPTY;
                    }
                    SVNDirectoryInfo childInfo = createDirInfo(info,
                                                               SVNPathUtil.append(info.path, child.getName()),
                                                               false,
                                                               depthBelowHere);
                    reportAddedDir(childInfo);
                }
            }
        }
    }

    private void reportAddedFile(SVNDirectoryInfo info, File entryPath) throws SVNException {

        if (!wcContext.matchesChangelist(entryPath, changeLists)) {
            return;
        }

        if (!matchesLocalTarget(entryPath)) {
            return;
        }

        final SVNWCContext.ScheduleInternalInfo schedule = wcContext.getNodeScheduleInternal(entryPath, false, true);
        final ISVNWCDb.WCDbInfo wcDbInfo = wcContext.getDb().readInfo(entryPath, ISVNWCDb.WCDbInfo.InfoField.revision);

        if (schedule.copied) {
            if (isCompareToBase) {
                return;
            }
            reportModifiedFile(info, entryPath);
            return;
        }
        final SVNProperties wcProps;
        if (isCompareToBase) {
            wcProps = wcContext.getPristineProps(entryPath);
        } else {
            wcProps = wcContext.getActualProps(entryPath);
        }
        String mimeType = wcProps.getStringValue(SVNProperty.MIME_TYPE);
        SVNProperties propDiff = computePropsDiff(new SVNProperties(), wcProps);

        File sourceFile;
        if (isCompareToBase) {
            final ISVNWCDb.WCDbBaseInfo baseInfo = wcContext.getDb().getBaseInfo(entryPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum);
            sourceFile = wcContext.getDb().getPristinePath(entryPath, baseInfo.checksum);
        } else {
            sourceFile = detranslateFile(entryPath);
        }
        getDiffCallback().fileAdded(getDiffCallbackResult(), entryPath, null, sourceFile, 0, wcDbInfo.revision, null, mimeType, null, -1, propDiff, null);
    }

    private void reportModifiedFile(SVNDirectoryInfo dirInfo, File entryPath) throws SVNException {
        if (!wcContext.matchesChangelist(entryPath, changeLists)) {
            return;
        }

        if (!matchesLocalTarget(entryPath)) {
            return;
        }

        final SVNWCContext.ScheduleInternalInfo schedule = wcContext.getNodeScheduleInternal(entryPath, true, true);

        SVNWCContext.SVNWCSchedule newSchedule = schedule.schedule;
        if (!isDiffCopiedAsAdded() && schedule.copied) {
            newSchedule = null;
        }
        if (!useAncestry && schedule.schedule == SVNWCContext.SVNWCSchedule.replace) {
            newSchedule = null;
        }
        SVNProperties propDiff = null;
        SVNProperties baseProps = null;


        SvnChecksum checksum;
        try {
            final Structure<StructureFields.NodeInfo> infoStructure = wcContext.getDb().readInfo(entryPath, StructureFields.NodeInfo.checksum);
            checksum = infoStructure.<SvnChecksum>get(StructureFields.NodeInfo.checksum);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_NOT_FOUND) {
                checksum = null;
            } else {
                throw e;
            }
        }
        final File pristineFile = checksum == null ? null : wcContext.getDb().getPristinePath(entryPath, checksum);

        SVNProperties pristineProps = wcContext.getPristineProps(entryPath);
        if (pristineProps == null) {
            pristineProps = new SVNProperties();
        }
        if (schedule.schedule != SVNWCContext.SVNWCSchedule.delete) {
            if (isDiffCopiedAsAdded() && schedule.copied) {
                baseProps = new SVNProperties();
                propDiff = wcContext.getActualProps(entryPath);
            } else {
                baseProps = pristineProps;
                boolean modified = wcContext.isPropsModified(entryPath);
                if (modified) {
                    propDiff = computePropsDiff(baseProps, wcContext.getActualProps(entryPath));
                } else {
                    propDiff = new SVNProperties();
                }
            }
        } else {
            baseProps = pristineProps;
        }
        boolean isAdded = newSchedule != null && schedule.schedule == SVNWCContext.SVNWCSchedule.add;
        if ((schedule.schedule == SVNWCContext.SVNWCSchedule.delete || schedule.schedule == SVNWCContext.SVNWCSchedule.replace)) {
            String mimeType = pristineProps.getStringValue(SVNProperty.MIME_TYPE);
            getDiffCallback().fileDeleted(getDiffCallbackResult(), entryPath, pristineFile, null, mimeType, null,
                    pristineProps);
            isAdded = schedule.schedule == SVNWCContext.SVNWCSchedule.replace;
        }
        if (isAdded) {
            final ISVNWCDb.WCDbInfo wcDbInfo = wcContext.getDb().readInfo(entryPath, ISVNWCDb.WCDbInfo.InfoField.revision);
            String mimeType = wcContext.getActualProps(entryPath).getStringValue(SVNProperty.MIME_TYPE);

            File tmpFile = detranslateFile(entryPath);
            SVNProperties originalProperties = null;
            long revision = wcDbInfo.revision;
            if (schedule.copied && isDiffCopiedAsAdded()) {
                originalProperties = new SVNProperties();
                revision = 0;
            } else {
                originalProperties = pristineProps;
            }
            getDiffCallback().fileAdded(getDiffCallbackResult(), entryPath, null, tmpFile, -1, revision, mimeType, null, null, -1, propDiff, originalProperties);
        } else if (newSchedule == null || schedule.schedule == SVNWCContext.SVNWCSchedule.normal) {
            boolean modified = wcContext.isTextModified(entryPath, false);
            File tmpFile = null;
            if (modified) {
                tmpFile = detranslateFile(entryPath);
            }
            if (modified || (propDiff != null && !propDiff.isEmpty())) {
                String baseMimeType = pristineProps.getStringValue(SVNProperty.MIME_TYPE);
                String mimeType = wcContext.getActualProps(entryPath).getStringValue(SVNProperty.MIME_TYPE);
                final ISVNWCDb.WCDbInfo wcDbInfo = wcContext.getDb().readInfo(entryPath, ISVNWCDb.WCDbInfo.InfoField.revision);

                getDiffCallback().fileChanged(getDiffCallbackResult(), entryPath, modified ? pristineFile : null, tmpFile, wcDbInfo.revision, -1,
                        baseMimeType, mimeType, propDiff, baseProps);
            }
        }

    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        SVNDepth subDirDepth = currentDirectory.depth;
        if (subDirDepth == SVNDepth.IMMEDIATES) {
            subDirDepth = SVNDepth.EMPTY;
        }
        currentDirectory = createDirInfo(currentDirectory, path, true, subDirDepth);
    }

    public void openDir(String path, long revision) throws SVNException {
        SVNDepth subDirDepth = currentDirectory.depth;
        if (subDirDepth == SVNDepth.IMMEDIATES) {
            subDirDepth = SVNDepth.EMPTY;
        }
        currentDirectory = createDirInfo(currentDirectory, path, false, subDirDepth);
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        if (currentDirectory.propertyDiff == null) {
            currentDirectory.propertyDiff = new SVNProperties();
        }
        currentDirectory.propertyDiff.put(name, value);
    }

    public void closeDir() throws SVNException {
        final File fullPath = new File(getWorkingCopyRoot(), currentDirectory.path);

        // display dir prop changes.
        SVNProperties diff = currentDirectory.propertyDiff;
        if (diff != null && !diff.isEmpty()) {
            // reverse changes
            SVNProperties originalProps = null;
            if (currentDirectory.isAdded) {
                originalProps = new SVNProperties();
            } else {
                if (isCompareToBase) {
                    originalProps = wcContext.getPristineProps(fullPath);
                } else {
                    originalProps = wcContext.getActualProps(fullPath);
                    SVNProperties baseProps = wcContext.getPristineProps(fullPath);
                    SVNProperties reposProps = applyPropChanges(baseProps, currentDirectory.propertyDiff);
                    diff = computePropsDiff(originalProps, reposProps);

                }
            }
            if (!isReverseDiff) {
                reversePropChanges(originalProps, diff);
            }
            getDiffCallback().dirPropsChanged(getDiffCallbackResult(), new File(getWorkingCopyRoot(), currentDirectory.path), currentDirectory.isAdded, diff, originalProps);
            currentDirectory.comparedEntries.add("");
        }
        if (!currentDirectory.isAdded) {
            localDirectoryDiff(currentDirectory);
        }
        String name = SVNPathUtil.tail(currentDirectory.path);
        currentDirectory = currentDirectory.parent;
        if (currentDirectory != null) {
            currentDirectory.comparedEntries.add(name);
        }
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        String name = SVNPathUtil.tail(path);
        currentFile = createFileInfo(currentDirectory, path, true);
        currentDirectory.comparedEntries.add(name);
    }

    public void openFile(String path, long revision) throws SVNException {
        String name = SVNPathUtil.tail(path);
        currentFile = createFileInfo(currentDirectory, path, false);
        currentDirectory.comparedEntries.add(name);
    }

    public void changeFileProperty(String path,String name, SVNPropertyValue value) throws SVNException {
        if (currentFile.propertyDiff == null) {
            currentFile.propertyDiff = new SVNProperties();
        }
        currentFile.propertyDiff.put(name, value);
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        final File fullPath = new File(getWorkingCopyRoot(), path);
        SVNWCContext.ScheduleInternalInfo schedule;
        try {
            schedule = wcContext.getNodeScheduleInternal(fullPath, false, true);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                schedule = null;
            } else {
                throw e;
            }
        }
        if (schedule != null && schedule.copied) {
            currentFile.isAdded = false;
        }
        if (!currentFile.isAdded) {
            SVNWCContext.PristineContentsInfo pristineContents = wcContext.getPristineContents(fullPath, false, true);
            currentFile.pristineFile = pristineContents.path;
        }
        currentFile.file = createTempFile();
        deltaProcessor.applyTextDelta(currentFile.pristineFile, currentFile.file, false);
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        return deltaProcessor.textDeltaChunk(diffWindow);
    }

    public void textDeltaEnd(String path) throws SVNException {
        deltaProcessor.textDeltaEnd();
    }

    public void closeFile(String commitPath, String textChecksum) throws SVNException {
        final File fullPath = new File(workingCopyRoot, commitPath);
        SVNProperties baseProperties = null;
        if (!currentFile.isAdded) {
            try {
                baseProperties = wcContext.getDb().getBaseProps(fullPath);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
        }
        if (baseProperties == null) {
            baseProperties = new SVNProperties();
        }
        SVNProperties reposProperties = applyPropChanges(baseProperties, currentFile.propertyDiff);
        String reposMimeType = reposProperties.getStringValue(SVNProperty.MIME_TYPE);
        File reposFile = currentFile.file;
        File localFile = null;
        if (reposFile == null) {
            final ISVNWCDb.WCDbBaseInfo baseInfo = wcContext.getDb().getBaseInfo(fullPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum);
            reposFile = wcContext.getDb().getPristinePath(fullPath, baseInfo.checksum);
        }

        SVNWCContext.ScheduleInternalInfo schedule;
        try {
            schedule = wcContext.getNodeScheduleInternal(fullPath, true, false);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_NOT_FOUND || e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                schedule = null;
            } else {
                throw e;
            }
        }

        if (currentFile.isAdded || (!isCompareToBase && schedule != null && schedule.schedule == SVNWCContext.SVNWCSchedule.delete)) {
            if (isReverseDiff) {
                getDiffCallback().fileAdded(getDiffCallbackResult(), new File(getWorkingCopyRoot(), commitPath), null, reposFile, 0, targetRevision, reposMimeType, null, null, -1,
                        currentFile.propertyDiff, null);
            } else {
                getDiffCallback().fileDeleted(getDiffCallbackResult(), new File(getWorkingCopyRoot(), commitPath), reposFile, null, reposMimeType, null, reposProperties);
            }
            return;
        }
        boolean modified = currentFile.file != null;
        if (!modified && !isCompareToBase) {
            modified = wcContext.isTextModified(fullPath, false);
        }
        if (modified) {
            if (isCompareToBase) {
                final ISVNWCDb.WCDbBaseInfo baseInfo = wcContext.getDb().getBaseInfo(fullPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum);
                localFile = wcContext.getDb().getPristinePath(fullPath, baseInfo.checksum);
            } else {
                localFile = detranslateFile(fullPath);
            }
        } else {
            localFile = null;
            reposFile = null;
        }

        SVNProperties originalProps = null;
        if (isCompareToBase) {
            originalProps = baseProperties;
        } else {
            originalProps = wcContext.getDb().readPristineProperties(fullPath);
            currentFile.propertyDiff = computePropsDiff(originalProps, reposProperties);
        }

        if (localFile != null || (currentFile.propertyDiff != null && !currentFile.propertyDiff.isEmpty())) {
            String originalMimeType = originalProps.getStringValue(SVNProperty.MIME_TYPE);
            if (currentFile.propertyDiff != null && !currentFile.propertyDiff.isEmpty() && !isReverseDiff) {
                reversePropChanges(originalProps, currentFile.propertyDiff);
            }
            if (localFile != null || reposFile != null || (currentFile.propertyDiff != null && !currentFile.propertyDiff.isEmpty())) {
                getDiffCallback().fileChanged(getDiffCallbackResult(), new File(getWorkingCopyRoot(), commitPath),
                        isReverseDiff ? localFile : reposFile,
                        isReverseDiff ? reposFile : localFile,
                        isReverseDiff ? -1 : targetRevision,
                        isReverseDiff ? targetRevision : -1,
                        isReverseDiff ? originalMimeType : reposMimeType,
                        isReverseDiff ? reposMimeType : originalMimeType,
                        currentFile.propertyDiff, originalProps);
            }
        }
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        if (!isRootOpen) {
            localDirectoryDiff(createDirInfo(null, "", false, depth));
        }
        return null;
    }

    public void abortEdit() throws SVNException {
    }

    public void absentDir(String path) throws SVNException {
    }

    public void absentFile(String path) throws SVNException {
    }

    public void cleanup() {
        if (tempDirectory != null) {
            SVNFileUtil.deleteAll(tempDirectory, true);
        }
    }

    private SVNProperties applyPropChanges(SVNProperties props, SVNProperties propChanges) {
        SVNProperties result = new SVNProperties(props);
        if (propChanges != null) {
            for(Iterator names = propChanges.nameSet().iterator(); names.hasNext();) {
                String name = (String) names.next();
                SVNPropertyValue value = propChanges.getSVNPropertyValue(name);
                if (value == null) {
                    result.remove(name);
                } else {
                    result.put(name, value);
                }
            }
        }
        return result;
    }

    private void localDirectoryDiff(SVNDirectoryInfo info) throws SVNException {
        if (isCompareToBase) {
            return;
        }

        final File fullPath = new File(getWorkingCopyRoot(), info.path);

        if (wcContext.matchesChangelist(new File(getWorkingCopyRoot(), info.path), changeLists) && !info.comparedEntries.contains("")) {
            // generate prop diff for dir.
            if (wcContext.isPropsModified(fullPath) && matchesLocalTarget(fullPath)) {
                final SVNProperties baseProps = wcContext.getPristineProps(fullPath);
                SVNProperties propDiff = baseProps.compareTo(wcContext.getActualProps(fullPath));
                getDiffCallback().dirPropsChanged(getDiffCallbackResult(), new File(getWorkingCopyRoot(), info.path), info.isAdded, propDiff, baseProps);
            }
        }

        if (info.depth == SVNDepth.EMPTY) {
            return;
        }

        Set processedFiles = null;
        if (isDiffUnversioned()) {
            processedFiles = new SVNHashSet();
        }

        final List<File> children = wcContext.getNodeChildren(fullPath, false);
        for (File child : children) {
             if (processedFiles != null) {
                processedFiles.add(child.getName());
            }
            if (info.comparedEntries.contains(child.getName())) {
                continue;
            }
            info.comparedEntries.add(child.getName());

            final Structure<StructureFields.NodeInfo> nodeInfoStructure = wcContext.getDb().readInfo(child, StructureFields.NodeInfo.kind);
            final ISVNWCDb.SVNWCDbKind kind = nodeInfoStructure.get(StructureFields.NodeInfo.kind);

            if (kind == ISVNWCDb.SVNWCDbKind.File) {
                reportModifiedFile(info, child);
            } else if (kind == ISVNWCDb.SVNWCDbKind.Dir) {
                if (info.depth.compareTo(SVNDepth.FILES) > 0 ||
                    info.depth == SVNDepth.UNKNOWN) {
                    SVNDepth depthBelowHere = info.depth;
                    if (depthBelowHere == SVNDepth.IMMEDIATES) {
                        depthBelowHere = SVNDepth.EMPTY;
                    }
                    SVNDirectoryInfo childInfo = createDirInfo(info,
                                                               SVNPathUtil.append(info.path, child.getName()),
                                                               false,
                                                               depthBelowHere);
                    localDirectoryDiff(childInfo);
                }
            }
        }
        if (isDiffUnversioned()) {
            throw new UnsupportedOperationException();//TODO
//            String relativePath = dir.getRelativePath(myAdminInfo.getAnchor());
//            diffUnversioned(dir.getRoot(), dir, relativePath, anchor, processedFiles);
        }
    }

    private boolean matchesLocalTarget(File fullPath) {
        return (localTarget == null || SVNPathUtil.isWithinBasePath(localTarget.getAbsolutePath(), fullPath.getAbsolutePath()));
    }

    private void diffUnversioned(File root, SVNAdminArea dir, String parentRelativePath, boolean anchor, Set processedFiles) throws SVNException {
        File[] allFiles = SVNFileListUtil.listFiles(root);
        for (int i = 0; allFiles != null && i < allFiles.length; i++) {
            File file = allFiles[i];
            if (SVNFileUtil.getAdminDirectoryName().equals(file.getName())) {
                continue;
            }
            if (processedFiles != null && processedFiles.contains(file.getName())) {
                continue;
            }
            if (dir != null) {
                Collection globalIgnores = SVNStatusEditor.getGlobalIgnores(wcContext.getOptions());
                Collection ignores = SVNStatusEditor.getIgnorePatterns(dir, globalIgnores);

                String rootRelativePath = null;
                boolean needToComputeRelativePath = false;
                for (Iterator patterns = ignores.iterator(); patterns.hasNext();) {
                    String pattern = (String) patterns.next();
                    if (pattern.startsWith("/")) {
                        needToComputeRelativePath = true;
                        break;
                    }
                }
                if (needToComputeRelativePath) {
                    if (relativeToDirectory == null) {
                        File wcRoot = SVNWCUtil.getWorkingCopyRoot(dir.getRoot(), true);
                        relativeToDirectory = wcRoot.getAbsolutePath().replace(File.separatorChar, '/');
                    }
                    if (relativeToDirectory != null) {
                        rootRelativePath = file.getAbsolutePath().replace(File.separatorChar, '/');
                        rootRelativePath = SVNPathUtil.getPathAsChild(relativeToDirectory, rootRelativePath);
                        if (rootRelativePath != null && !rootRelativePath.startsWith("/")) {
                            rootRelativePath = "/" + rootRelativePath;
                        }
                    }
                }
                if (SVNStatusEditor.isIgnored(ignores, file, rootRelativePath)) {
                    continue;
                }
            }
            // generate patch as for added file.
            SVNFileType fileType = SVNFileType.getType(file);
            if (fileType == SVNFileType.DIRECTORY) {
                diffUnversioned(file, null, SVNPathUtil.append(parentRelativePath, file.getName()), false, null);
            } else if (fileType == SVNFileType.FILE || fileType == SVNFileType.SYMLINK) {
                String mimeType1 = null;
                String mimeType2 = SVNFileUtil.detectMimeType(file, null);
                String filePath = SVNPathUtil.append(parentRelativePath, file.getName());
                getDiffCallback().fileAdded(getDiffCallbackResult(), new File(getWorkingCopyRoot(), filePath), null, file, -1, -1, mimeType1, mimeType2, null, -1, null, null);
            }
        }
    }

    private SVNDirectoryInfo createDirInfo(SVNDirectoryInfo parent, String path, boolean added,
                                           SVNDepth depth) {
        SVNDirectoryInfo info = new SVNDirectoryInfo();
        info.parent = parent;
        info.path = path;
        info.isAdded = added;
        info.depth = depth;
        return info;
    }

    private SVNFileInfo createFileInfo(SVNDirectoryInfo parent, String path, boolean added) {
        SVNFileInfo info = new SVNFileInfo();
        info.path = path;
        info.isAdded = added;
        if (parent.isAdded) {
            while(parent.isAdded) {
                parent = parent.parent;
            }
            info.path = SVNPathUtil.append(parent.path, "fake");
        }
        return info;
    }

    private File detranslateFile(File fullPath) throws SVNException {
        SVNProperties properties = wcContext.getPristineProps(fullPath);
        if (properties == null) {
            properties = new SVNProperties();
        }
//        SVNVersionedProperties properties = dir.getProperties(name);
        String keywords = properties.getStringValue(SVNProperty.KEYWORDS);
        String eolStyle = properties.getStringValue(SVNProperty.EOL_STYLE);
        String charsetProp = properties.getStringValue(SVNProperty.CHARSET);
        String mimeType = properties.getStringValue(SVNProperty.MIME_TYPE);

        ISVNOptions options = wcContext.getOptions();
        String charset = SVNTranslator.getCharset(charsetProp, mimeType, fullPath.getPath(), options);
        boolean special = properties.getSVNPropertyValue(SVNProperty.SPECIAL) != null;

        if (charset == null && keywords == null && eolStyle == null && (!special || !SVNFileUtil.symlinksSupported())) {
            return fullPath;
        }
        byte[] eol = SVNTranslator.getEOL(eolStyle, options);
        File tmpFile = createTempFile();
        Map keywordsMap = SVNTranslator.computeKeywords(keywords, null, null, null, null, null);
        SVNTranslator.translate(fullPath, tmpFile, charset, eol, keywordsMap, special, false);
        return tmpFile;
    }

    private File createTempFile() throws SVNException {
        File tmpFile = null;
        try {
            return File.createTempFile("diff.", ".tmp", getTempDirectory());
        } catch (IOException e) {
            SVNFileUtil.deleteFile(tmpFile);
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }
        return null;
    }

    private File getTempDirectory() throws SVNException {
        if (tempDirectory == null) {
            tempDirectory = createTempDirectory();
        }
        return tempDirectory;
    }

    private static class SVNDirectoryInfo {

        private boolean isAdded;
        private String path;
        private SVNProperties propertyDiff;
        private SVNDirectoryInfo parent;
        private Set comparedEntries = new SVNHashSet();
        private SVNDepth depth;
    }

    private static class SVNFileInfo {

        private boolean isAdded;
        private String path;
        private File file;
        private File pristineFile;
        private SVNProperties propertyDiff;
    }

    private static void reversePropChanges(SVNProperties base, SVNProperties diff) {
        Collection namesList = new ArrayList(diff.nameSet());
        for (Iterator names = namesList.iterator(); names.hasNext();) {
            String name = (String) names.next();
            SVNPropertyValue newValue = diff.getSVNPropertyValue(name);
            SVNPropertyValue oldValue = base.getSVNPropertyValue(name);
            if (oldValue == null && newValue != null) {
                base.put(name, newValue);
                diff.put(name, (SVNPropertyValue) null);
            } else if (oldValue != null && newValue == null) {
                base.put(name, (SVNPropertyValue) null);
                diff.put(name, oldValue);
            } else if (oldValue != null && newValue != null) {
                base.put(name, newValue);
                diff.put(name, oldValue);
            }
        }
    }

    private static SVNProperties computePropsDiff(SVNProperties props1, SVNProperties props2) {
        SVNProperties propsDiff = new SVNProperties();
        for (Iterator names = props2.nameSet().iterator(); names.hasNext();) {
            String newPropName = (String) names.next();
            if (props1.containsName(newPropName)) {
                // changed.
                SVNPropertyValue oldValue = props2.getSVNPropertyValue(newPropName);
                SVNPropertyValue value = props1.getSVNPropertyValue(newPropName);
                if (oldValue != null && !oldValue.equals(value)) {
                    propsDiff.put(newPropName, oldValue);
                } else if (oldValue == null && value != null) {
                    propsDiff.put(newPropName, oldValue);
                }
            } else {
                // added.
                propsDiff.put(newPropName, props2.getSVNPropertyValue(newPropName));
            }
        }
        for (Iterator names = props1.nameSet().iterator(); names.hasNext();) {
            String oldPropName = (String) names.next();
            if (!props2.containsName(oldPropName)) {
                // deleted
                propsDiff.put(oldPropName, (String) null);
            }
        }
        return propsDiff;
    }

}
