package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.ISVNUpdateEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnDiffEditor implements ISVNEditor, ISVNUpdateEditor {

    //immutable

    private SVNDepth depth;
    private SVNWCContext context;
    private ISVNWCDb db;
    private File anchorAbspath;
    private String target;
    private boolean useTextBase;
    private boolean showCopiesAsAdds;
    private ISvnDiffCallback callback;
    private Collection<String> changelists;
    private boolean ignoreAncestry;
    private boolean useGitDiffFormat;
    private ISVNCanceller canceller;
    private boolean reverseOrder; //actually, has the opposite meaning

    //mutable
    private long revision;
    private boolean rootOpened;
    private Entry rootEntry;
    private Entry currentEntry;
    private Collection<File> tempFiles;

    //once initialized
    private final SVNDeltaProcessor deltaProcessor;

    public SvnDiffEditor(File anchorAbspath, String target, ISvnDiffCallback callback, SVNDepth depth, SVNWCContext context, boolean reverseOrder, boolean useTextBase, boolean showCopiesAsAdds, boolean ignoreAncestry, Collection<String> changelists, boolean useGitDiffFormat, ISVNCanceller canceller) {
        this.depth = depth;
        this.context = context;
        this.db = context.getDb();
        this.anchorAbspath = anchorAbspath;
        this.target = target;
        this.useTextBase = useTextBase;
        this.showCopiesAsAdds = showCopiesAsAdds;
        this.callback = callback;
        this.changelists = changelists;
        this.ignoreAncestry = ignoreAncestry;
        this.useGitDiffFormat = useGitDiffFormat;
        this.canceller = canceller;
        this.reverseOrder = reverseOrder;
        this.deltaProcessor = new SVNDeltaProcessor();
        this.tempFiles = new ArrayList<File>();
    }

    public SvnDiffEditor() {
        this.deltaProcessor = new SVNDeltaProcessor();
    }

    public void targetRevision(long revision) throws SVNException {
        this.revision = revision;
    }

    public void openRoot(long revision) throws SVNException {
        this.rootOpened = true;
        rootEntry = new Entry(false, "", null, false, depth, anchorAbspath);
        currentEntry = rootEntry;
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        File localAbspath = getLocalAbspath(path);

        addToCompared(currentEntry, path);

        Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(localAbspath, StructureFields.NodeInfo.status, StructureFields.NodeInfo.kind);
        ISVNWCDb.SVNWCDbKind kind = nodeInfoStructure.get(StructureFields.NodeInfo.kind);
        ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);

        if (!useTextBase && status == ISVNWCDb.SVNWCDbStatus.Deleted) {
            return;
        }

        switch (kind) {
            case File:
            case Symlink:
                if (reverseOrder) {
                    File textBase = getPristineFile(localAbspath, useTextBase);
                    SVNProperties baseProps = context.getPristineProps(localAbspath);
                    String baseMimeType = getPropMimeType(baseProps);

                    callback.fileDeleted(null, localAbspath, textBase, null, baseMimeType, null, baseProps);
                } else {
                    reportFileAdded(localAbspath, path);
                }
                break;
            case Dir:
                reportDirectoryAdded(localAbspath, path, SVNDepth.INFINITY);
                break;
            default:
                break;
        }
    }

    public void absentDir(String path) throws SVNException {
    }

    public void absentFile(String path) throws SVNException {
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        SVNDepth subdirDepth = (depth == SVNDepth.IMMEDIATES) ? SVNDepth.EMPTY : depth;

        addToCompared(currentEntry, path);

        currentEntry = new Entry(false, path, currentEntry, true, subdirDepth, getLocalAbspath(path));
    }

    public void openDir(String path, long revision) throws SVNException {
        SVNDepth subdirDepth = (depth == SVNDepth.IMMEDIATES) ? SVNDepth.EMPTY : depth;

        addToCompared(currentEntry, path);

        currentEntry = new Entry(false, path, currentEntry, false, subdirDepth, getLocalAbspath(path));
    }

    public void closeDir() throws SVNException {
        if (!currentEntry.propChanges.isEmpty()) {
            SVNProperties originalProps;
            if (currentEntry.added) {
                originalProps = new SVNProperties();
            } else {
                if (useTextBase) {
                    originalProps = context.getPristineProps(currentEntry.localAbspath);
                } else {
                    originalProps = context.getActualProps(currentEntry.localAbspath);
                    SVNProperties baseProps = context.getPristineProps(currentEntry.localAbspath);
                    SVNProperties reposProps = applyPropsChanges(baseProps, currentEntry.propChanges);
                    currentEntry.propChanges = computePropDiff(originalProps, reposProps);
                }
            }

            if (!reverseOrder) {
                reversePropChanges(originalProps, currentEntry.propChanges);
            }

            callback.dirPropsChanged(null, currentEntry.localAbspath, currentEntry.added, currentEntry.propChanges, originalProps);

            addToCompared(currentEntry, currentEntry.path);
        }

        if (!currentEntry.added) {
            walkLocalNodesDiff(currentEntry.localAbspath, currentEntry.path, currentEntry.depth, currentEntry.compared);
        }

        callback.dirClosed(null, currentEntry.localAbspath, currentEntry.added);

        currentEntry = currentEntry.parent;
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {

        addToCompared(currentEntry, path);

        currentEntry = new Entry(true, path, currentEntry, true, SVNDepth.UNKNOWN, getLocalAbspath(path));
    }

    public void openFile(String path, long revision) throws SVNException {
        addToCompared(currentEntry, path);

        currentEntry = new Entry(true, path, currentEntry, false, SVNDepth.UNKNOWN, getLocalAbspath(path));

        ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(currentEntry.localAbspath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum);
        currentEntry.baseChecksum = baseInfo.checksum;

        callback.fileOpened(null, currentEntry.localAbspath, revision);
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {

        InputStream sourceStream;
        if (currentEntry.baseChecksum != null) {
            sourceStream = db.readPristine(currentEntry.localAbspath, currentEntry.baseChecksum);
        } else {
            sourceStream = SVNFileUtil.DUMMY_IN;
        }

        currentEntry.file = createTempFile(db.getWCRootTempDir(currentEntry.localAbspath));
        deltaProcessor.applyTextDelta(sourceStream, currentEntry.file, true);
    }

    public void closeFile(String path, String textChecksum) throws SVNException {
        try {
        if (textChecksum != null) {
            SvnChecksum reposChecksum = currentEntry.resultChecksum;

            if (reposChecksum == null) {
                reposChecksum = currentEntry.baseChecksum;
            }

            if (reposChecksum.getKind() != SvnChecksum.Kind.md5) {
                reposChecksum = db.getPristineMD5(currentEntry.localAbspath, reposChecksum);
            }

            if (!reposChecksum.getDigest().equals(textChecksum)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''", currentEntry.localAbspath);
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
        }

        ISVNWCDb.SVNWCDbStatus status;
        SvnChecksum pristineChecksum;
        boolean hadProps;
        boolean propsMod;
        try {
            Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(currentEntry.localAbspath,
                    StructureFields.NodeInfo.status,
                    StructureFields.NodeInfo.checksum,
                    StructureFields.NodeInfo.hadProps,
                    StructureFields.NodeInfo.propsMod);
            status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
            pristineChecksum = nodeInfoStructure.get(StructureFields.NodeInfo.checksum);
            hadProps = nodeInfoStructure.is(StructureFields.NodeInfo.hadProps);
            propsMod = nodeInfoStructure.is(StructureFields.NodeInfo.propsMod);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                status = ISVNWCDb.SVNWCDbStatus.NotPresent;
                pristineChecksum = null;
                hadProps = false;
                propsMod = false;
            } else {
                throw e;
            }
        }


        SVNProperties pristineProps;
        File pristineFile;
        if (currentEntry.added) {
            pristineProps = new SVNProperties();
            pristineFile = null;
        } else {
            if (status != ISVNWCDb.SVNWCDbStatus.Normal) {
                Structure<StructureFields.NodeInfo> nodeInfoStructure = SvnWcDbShared.getBaseInfo((SVNWCDb) db, currentEntry.localAbspath,
                        StructureFields.NodeInfo.checksum,
                        StructureFields.NodeInfo.hadProps);
                pristineChecksum = nodeInfoStructure.get(StructureFields.NodeInfo.checksum);
                hadProps = nodeInfoStructure.is(StructureFields.NodeInfo.hadProps);
            }
            pristineFile = db.getPristinePath(currentEntry.localAbspath, pristineChecksum);

            if (hadProps) {
                pristineProps = db.getBaseProps(currentEntry.localAbspath);
            } else {
                pristineProps = new SVNProperties();
            }
        }

        if (status == ISVNWCDb.SVNWCDbStatus.Added) {
            ISVNWCDb.WCDbAdditionInfo wcDbAdditionInfo = db.scanAddition(currentEntry.localAbspath, ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.status);
            status = wcDbAdditionInfo.status;
        }

        SVNProperties reposProps = applyPropsChanges(pristineProps, currentEntry.propChanges);
        String reposMimeType = getPropMimeType(reposProps);
        File reposFile = currentEntry.file != null ? currentEntry.file : pristineFile;

        if (currentEntry.added || (!useTextBase && status == ISVNWCDb.SVNWCDbStatus.Deleted)) {
            if (reverseOrder) {
                callback.fileAdded(null, currentEntry.localAbspath, null, reposFile, 0, revision, null, reposMimeType, null, -1, currentEntry.propChanges, new SVNProperties());
            } else {
                callback.fileDeleted(null, currentEntry.localAbspath, reposFile, null, reposMimeType, null, reposProps);
            }
            return;
        }

        if ((status == ISVNWCDb.SVNWCDbStatus.Copied || status == ISVNWCDb.SVNWCDbStatus.MovedHere) && showCopiesAsAdds) {
            callback.fileAdded(null, currentEntry.localAbspath, null, currentEntry.localAbspath, 0, revision, null, reposMimeType, null, -1, currentEntry.propChanges, new SVNProperties());
            return;
        }

        boolean modified = currentEntry.file != null;
        if (!modified && !useTextBase) {
            modified = context.isTextModified(currentEntry.localAbspath, false);
        }

        File localFile;
        if (modified) {
            if (useTextBase) {
                localFile = getPristineFile(currentEntry.localAbspath, false);
            } else {
                localFile = context.getTranslatedFile(currentEntry.localAbspath, currentEntry.localAbspath, true, false, false, false, false);

                //TODO: cancellation?
            }
        } else {
            reposFile = null;
            localFile = null;
        }

        SVNProperties originalProps;
        if (useTextBase) {
            originalProps = pristineProps;
        } else {
            originalProps = context.getActualProps(currentEntry.localAbspath);
            currentEntry.propChanges = computePropDiff(originalProps, reposProps);
        }

        if (localFile != null || !currentEntry.propChanges.isEmpty()) {
            String originalMimeType = getPropMimeType(originalProps);

            if (!currentEntry.propChanges.isEmpty() && !reverseOrder) {
                reversePropChanges(originalProps, currentEntry.propChanges);
            }

            callback.fileChanged(null, currentEntry.localAbspath,
                    reverseOrder ? localFile : reposFile,
                    reverseOrder ? reposFile : localFile,
                    reverseOrder ? -1 : revision,
                    reverseOrder ? revision : -1,
                    reverseOrder ? originalMimeType : reposMimeType,
                    reverseOrder ? reposMimeType : originalMimeType,
                    currentEntry.propChanges,
                    originalProps);

            return;
        }
        } finally {

            //there're a lot of returns, but we should switch the entry
            currentEntry = currentEntry.parent;
        }
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        currentEntry.propChanges.put(name, value);
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        currentEntry.propChanges.put(propertyName, propertyValue);
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        if (!rootOpened) {
            walkLocalNodesDiff(anchorAbspath, "", depth, null);
        }
        return null;
    }

    public void abortEdit() throws SVNException {
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        return deltaProcessor.textDeltaChunk(diffWindow);
    }

    public void textDeltaEnd(String path) throws SVNException {
        final String checksum = deltaProcessor.textDeltaEnd();
        currentEntry.resultChecksum = new SvnChecksum(SvnChecksum.Kind.md5, checksum);
    }

    private File getPristineFile(File localAbspath, boolean useTextBase) throws SVNException {
        SvnChecksum checksum;
        if (!useTextBase) {
            Structure<StructureFields.PristineInfo> pristineInfoStructure = db.readPristineInfo(localAbspath);
            checksum = pristineInfoStructure.get(StructureFields.PristineInfo.checksum);
        } else {
            ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(localAbspath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum);
            checksum = baseInfo.checksum;
        }

        if (checksum != null) {
            return db.getPristinePath(localAbspath, checksum);
        }

        return null;
    }

    private String getPropMimeType(SVNProperties baseProps) {
        if (baseProps == null) {
            return null;
        }
        return baseProps.getStringValue(SVNProperty.MIME_TYPE);
    }

    private void reportFileAdded(File localAbspath, String path) throws SVNException {
        if (!context.matchesChangelist(localAbspath, changelists)) {
            return;
        }

        Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(localAbspath,
                StructureFields.NodeInfo.revision,
                StructureFields.NodeInfo.status);
        long revision = nodeInfoStructure.lng(StructureFields.NodeInfo.revision);
        ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);

        if (status == ISVNWCDb.SVNWCDbStatus.Added) {
            ISVNWCDb.WCDbAdditionInfo wcDbAdditionInfo = db.scanAddition(localAbspath, ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.status);
            status = wcDbAdditionInfo.status;
        }

        assert status != ISVNWCDb.SVNWCDbStatus.Deleted || useTextBase;

        if (status == ISVNWCDb.SVNWCDbStatus.Copied || status == ISVNWCDb.SVNWCDbStatus.MovedHere) {
            if (useTextBase) {
                return;
            }

            fileDiff(localAbspath, path);
        }

        SVNProperties emptyProps = new SVNProperties();
        SVNProperties wcProps;
        if (useTextBase) {
            wcProps = context.getPristineProps(localAbspath);
        } else {
            wcProps = context.getActualProps(localAbspath);
        }

        String mimeType = getPropMimeType(wcProps);
        SVNProperties propchanges = computePropDiff(emptyProps, wcProps);

        File sourceFile;
        if (useTextBase) {
            sourceFile = getPristineFile(localAbspath, false);
        } else {
            sourceFile = localAbspath;
        }

        File translatedFile = context.getTranslatedFile(sourceFile, localAbspath, true, false, false, false, false);//TODO; cancel

        callback.fileAdded(null, localAbspath,
                null, translatedFile,
                0, revision,
                null, mimeType,
                null, -1,
                propchanges, emptyProps);

    }

    private void reportDirectoryAdded(File localAbspath, String path, SVNDepth depth) throws SVNException {
        SVNProperties emptyProps = new SVNProperties();
        if (context.matchesChangelist(localAbspath, changelists)) {
            SVNProperties wcProps;
            if (useTextBase) {
                wcProps = context.getPristineProps(localAbspath);
            } else {
                wcProps = context.getActualProps(localAbspath);
            }

            SVNProperties propChanges = computePropDiff(emptyProps, wcProps);
            if (!propChanges.isEmpty()) {
                callback.dirPropsChanged(null, localAbspath, true, propChanges, emptyProps);
            }
        }

        Set<String> children = db.readChildren(localAbspath);
        for (String name : children) {
            checkCancelled();

            final File childAbspath = new File(localAbspath, name);

            Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(childAbspath, StructureFields.NodeInfo.status, StructureFields.NodeInfo.kind);
            ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
            ISVNWCDb.SVNWCDbKind kind = nodeInfoStructure.get(StructureFields.NodeInfo.kind);

            if (status == ISVNWCDb.SVNWCDbStatus.NotPresent
                    || status == ISVNWCDb.SVNWCDbStatus.Excluded
                    || status == ISVNWCDb.SVNWCDbStatus.ServerExcluded) {
                continue;
            }

            if (!useTextBase && status == ISVNWCDb.SVNWCDbStatus.Deleted) {
                continue;
            }

            String childPath = SVNPathUtil.append(path, name);

            switch (kind) {
                case File:
                case Symlink:
                    reportFileAdded(childAbspath, childPath);
                    break;

                case Dir:
                    if (depth.compareTo(SVNDepth.FILES) > 0 || depth == SVNDepth.UNKNOWN) {
                        SVNDepth depthBelowHere = depth;
                        if (depthBelowHere == SVNDepth.IMMEDIATES) {
                            depthBelowHere = SVNDepth.EMPTY;
                        }

                        reportDirectoryAdded(childAbspath, childPath, depthBelowHere);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void fileDiff(File localAbspath, String path) throws SVNException {
        boolean useBase = false;

        assert !useTextBase;

        if (!context.matchesChangelist(localAbspath, changelists)) {
            return;
        }

        Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(localAbspath,
                StructureFields.NodeInfo.status,
                StructureFields.NodeInfo.revision,
                StructureFields.NodeInfo.haveBase);
        ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
        long revision = nodeInfoStructure.lng(StructureFields.NodeInfo.revision);
        boolean haveBase = nodeInfoStructure.is(StructureFields.NodeInfo.haveBase);

        ISVNWCDb.SVNWCDbStatus baseStatus = null;
        long revertBaseRevision = -1;
        if (haveBase) {
            ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(localAbspath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.status, ISVNWCDb.WCDbBaseInfo.BaseInfoField.revision);
            baseStatus = baseInfo.status;
            revertBaseRevision = baseInfo.revision;
        }

        boolean replaced = ((status == ISVNWCDb.SVNWCDbStatus.Added) && haveBase && baseStatus != ISVNWCDb.SVNWCDbStatus.NotPresent);

        File originalReposRelpath = null;

        if (status == ISVNWCDb.SVNWCDbStatus.Added) {
            ISVNWCDb.WCDbAdditionInfo wcDbAdditionInfo = db.scanAddition(localAbspath, ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.status, ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.originalReposRelPath);
            status = wcDbAdditionInfo.status;
            originalReposRelpath =  wcDbAdditionInfo.originalReposRelPath;
        }

        if (replaced && ! (status == ISVNWCDb.SVNWCDbStatus.Copied || status == ISVNWCDb.SVNWCDbStatus.MovedHere)) {
            useBase = true;
            revision = revertBaseRevision;
        }

        File textBase = getPristineFile(localAbspath, useBase);

        if ((!replaced && status == ISVNWCDb.SVNWCDbStatus.Deleted) || (replaced && !ignoreAncestry)) {
            SVNProperties baseProps = context.getPristineProps(localAbspath);

            String baseMimeType;
            if (baseProps != null) {
                baseMimeType = getPropMimeType(baseProps);
            } else {
                baseMimeType = null;
            }

            callback.fileDeleted(null, localAbspath, textBase, null, baseMimeType, null, baseProps);

            if (! (replaced && !ignoreAncestry)) {
                return;
            }
        }

        File translatedFile = null;
        if ((! replaced && status == ISVNWCDb.SVNWCDbStatus.Added) || (replaced && !ignoreAncestry) ||
                ((status == ISVNWCDb.SVNWCDbStatus.Copied || status == ISVNWCDb.SVNWCDbStatus.MovedHere) &&
                (showCopiesAsAdds || useGitDiffFormat))) {
            SVNProperties workingProps = context.getActualProps(localAbspath);
            String workingMimeType = getPropMimeType(workingProps);

            SVNProperties baseProps = new SVNProperties();
            SVNProperties propChanges = computePropDiff(baseProps, workingProps);

            translatedFile = context.getTranslatedFile(localAbspath, localAbspath, true, false, false, false, false);

            callback.fileAdded(null, localAbspath,
                    (!showCopiesAsAdds && useGitDiffFormat && status != ISVNWCDb.SVNWCDbStatus.Added) ? textBase : null,
                    translatedFile,
                    0, revision,
                    null,
                    workingMimeType,
                    originalReposRelpath,
                    -1,
                    propChanges, baseProps);
        } else {
            boolean modified = context.isTextModified(localAbspath, false);
            if (modified) {
                translatedFile = context.getTranslatedFile(localAbspath, localAbspath, true, false, false, false, false);
            }

            SVNProperties baseProps;
            if (replaced && ignoreAncestry) {
                baseProps = db.getBaseProps(localAbspath);
            } else {
                assert (!replaced ||
                        status == ISVNWCDb.SVNWCDbStatus.Copied ||
                        status == ISVNWCDb.SVNWCDbStatus.MovedHere);

                baseProps = context.getPristineProps(localAbspath);

                if (baseProps == null) {
                    baseProps = new SVNProperties();
                }
            }


            String baseMimeType = getPropMimeType(baseProps);

            SVNProperties workingProps = context.getActualProps(localAbspath);
            String workingMimeType = getPropMimeType(workingProps);
            SVNProperties propChanges = computePropDiff(baseProps, workingProps);

            if (modified || !propChanges.isEmpty()) {
                callback.fileChanged(null, localAbspath,
                        modified ? textBase : null, translatedFile,
                        revision, -1,
                        baseMimeType, workingMimeType,
                        propChanges, baseProps);
            }
        }
    }

    private File getLocalAbspath(String path) {
        return new File(anchorAbspath, path);
    }

    private void checkCancelled() throws SVNCancelException {
        canceller.checkCancelled();
    }

    private SVNProperties applyPropsChanges(SVNProperties props, SVNProperties propChanges) {
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

    private static void reversePropChanges(SVNProperties base, SVNProperties diff) {
        Collection<String> namesList = new ArrayList<String>(diff.nameSet());
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

    private void walkLocalNodesDiff(File localAbspath, String path, SVNDepth depth, Set<String> compared) throws SVNException {
        if (useTextBase) {
            return;
        }

        boolean inAnchorNotTarget = path.length() == 0 && target.length() != 0;

        if (context.matchesChangelist(localAbspath, changelists) && !inAnchorNotTarget && (compared == null|| !compared.contains(path))) {
            boolean modified = context.isPropsModified(localAbspath);
            if (modified) {
                SVNWCContext.PropDiffs propDiffs = context.getPropDiffs(localAbspath);
                SVNProperties baseProps = propDiffs.originalProps;
                SVNProperties propChanges = propDiffs.propChanges;

                callback.dirPropsChanged(null, localAbspath, false, propChanges, baseProps);
            }
        }

        if (depth == SVNDepth.EMPTY && !inAnchorNotTarget) {
            return;
        }

        Set<String> children = db.readChildren(localAbspath);
        for (String name : children) {
            checkCancelled();

            if (inAnchorNotTarget && !name.equals(target)) {
                continue;
            }

            File childAbspath = new File(localAbspath, name);

            Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(childAbspath, StructureFields.NodeInfo.status, StructureFields.NodeInfo.kind);
            ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
            ISVNWCDb.SVNWCDbKind kind = nodeInfoStructure.get(StructureFields.NodeInfo.kind);

            if (status == ISVNWCDb.SVNWCDbStatus.NotPresent || status == ISVNWCDb.SVNWCDbStatus.Excluded || status == ISVNWCDb.SVNWCDbStatus.ServerExcluded) {
                continue;
            }

            String childPath = SVNPathUtil.append(path, name);

            if (compared != null && compared.contains(childPath)) {
                continue;
            }


            switch (kind) {
                case File:
                case Symlink:
                    fileDiff(childAbspath, childPath);
                    break;
                case Dir:
                    if (inAnchorNotTarget
                            || (depth.compareTo(SVNDepth.FILES) > 0) || depth == SVNDepth.UNKNOWN) {
                        SVNDepth depthBelowHere = depth;
                        if (depthBelowHere == SVNDepth.IMMEDIATES) {
                            depthBelowHere = SVNDepth.EMPTY;
                        }

                        walkLocalNodesDiff(childAbspath, childPath, depthBelowHere, null);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private File createTempFile(File tempDir) throws SVNException {
        final File tempFile = SVNFileUtil.createUniqueFile(tempDir, "diff.", ".tmp", true);
        tempFiles.add(tempFile);
        return tempFile;
    }

    private void addToCompared(Entry entry, String path) {
        if (entry.compared == null) {
            currentEntry.compared = new HashSet<String>();
        }
        currentEntry.compared.add(path);
    }

    public void cleanup() {
        for (File tempFile : tempFiles) {
            try {
                SVNFileUtil.deleteFile(tempFile);
            } catch (SVNException ignore) {
            }
        }
    }

    public long getTargetRevision() {
        return revision;
    }

    public static SVNProperties computePropDiff(SVNProperties props1, SVNProperties props2) {
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

    private static class Entry {

        private boolean isFile;
        private Entry parent;
        private String path;
        private boolean added;
        private SVNDepth depth;
        private SVNProperties propChanges;
        private File localAbspath;
        private SvnChecksum baseChecksum;
        private SvnChecksum resultChecksum;
        private File file;
        private Set<String> compared;

        public Entry(boolean file, String path, Entry parent, boolean added, SVNDepth depth, File localAbspath) {
            this.isFile = file;
            this.path = path;
            this.parent = parent;
            this.added = added;
            this.depth = depth;
            this.localAbspath = localAbspath;
            this.propChanges = new SVNProperties();
        }
    }
}
