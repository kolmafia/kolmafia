package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.ISVNUpdateEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
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
    private boolean showCopiesAsAdds;
    private ISvnDiffCallback2 callback;
    private Collection<String> changelists;
    private boolean ignoreAncestry;
    private boolean useGitDiffFormat;
    private ISVNCanceller canceller;
    private boolean reverseOrder; //actually, has the opposite meaning
    private boolean localBeforeRemote;
    private boolean diffPristine;

    //mutable
    private long revision;
    private boolean rootOpened;
    private Entry rootEntry;
    private Entry currentEntry;
    private Collection<File> tempFiles;
    private SVNWCDbRoot wcRoot;

    //once initialized
    private final SVNDeltaProcessor deltaProcessor;
    private final SvnDiffCallbackResult result;

    public SvnDiffEditor(File anchorAbspath, String target, ISvnDiffCallback callback, SVNDepth depth, SVNWCContext context, boolean reverseOrder, boolean useTextBase, boolean showCopiesAsAdds, boolean ignoreAncestry, Collection<String> changelists, boolean useGitDiffFormat, ISVNCanceller canceller) {
        if (useGitDiffFormat) {
            showCopiesAsAdds = true;
        }
        this.depth = depth;
        this.context = context;
        this.db = context.getDb();
        this.anchorAbspath = anchorAbspath;
        this.target = target;
        this.diffPristine = useTextBase;
        this.showCopiesAsAdds = showCopiesAsAdds;
        this.callback = new SvnDiffCallbackWrapper(callback, true, anchorAbspath);
        if (!showCopiesAsAdds) {
            this.callback = new SvnCopyAsChangedDiffCallback(this.callback);
        }
        if (reverseOrder) {
            this.callback = new SvnReverseOrderDiffCallback(this.callback, null);
        }
        this.changelists = changelists;
        if (showCopiesAsAdds) {
            ignoreAncestry = false;
        }
        this.ignoreAncestry = ignoreAncestry;
        this.useGitDiffFormat = useGitDiffFormat;
        this.canceller = canceller;
        this.reverseOrder = reverseOrder;
        this.deltaProcessor = new SVNDeltaProcessor();
        this.tempFiles = new ArrayList<File>();
        this.result = new SvnDiffCallbackResult();
    }

    public SvnDiffEditor() {
        this.deltaProcessor = new SVNDeltaProcessor();
        this.result = new SvnDiffCallbackResult();
    }

    public void targetRevision(long revision) throws SVNException {
        this.revision = revision;
    }

    public void openRoot(long revision) throws SVNException {
        this.rootOpened = true;
        rootEntry = new Entry(false, "", null, false, depth, anchorAbspath);
        currentEntry = rootEntry;

        if (target.length() == 0) {
            currentEntry.leftSource = new SvnDiffSource(this.revision);
            currentEntry.rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);

            callback.dirOpened(result, new File(""), currentEntry.leftSource, currentEntry.rightSource, null, null);
            currentEntry.skip = result.skip;
            currentEntry.skipChildren = result.skipChildren;
        } else {
            currentEntry.skip = true;
        }
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        File localAbspath = getLocalAbspath(path);
        String name = SVNPathUtil.tail(path);
        Entry pb = currentEntry;

        if (pb.deletes == null) {
            pb.deletes = new HashSet<String>();
        }
        pb.deletes.add(name);
    }

    public void absentDir(String path) throws SVNException {
    }

    public void absentFile(String path) throws SVNException {
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        SVNDepth subdirDepth = (depth == SVNDepth.IMMEDIATES) ? SVNDepth.EMPTY : depth;

        Entry pb = currentEntry;
        currentEntry = new Entry(false, path, currentEntry, true, subdirDepth, getLocalAbspath(path));
        if (pb.reposOnly || !ignoreAncestry) {
            currentEntry.reposOnly = true;
        } else {
            pb.ensureLocalInfo();
            ISVNWCDb.SVNWCDbInfo info = pb.localInfo.get(currentEntry.name);

            if (info == null || info.kind != ISVNWCDb.SVNWCDbKind.Dir || info.status.isNotPresent()) {
                currentEntry.reposOnly = true;
            }

            if (!currentEntry.reposOnly && info.status != ISVNWCDb.SVNWCDbStatus.Added) {
                currentEntry.reposOnly = true;
            }

            if (!currentEntry.reposOnly) {
                currentEntry.rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);
                currentEntry.ignoringAncestry = true;
                if (pb.compared == null) {
                    pb.compared = new HashSet<String>();
                }
                pb.compared.add(currentEntry.name);
            }
        }
        currentEntry.leftSource = new SvnDiffSource(this.revision);
        if (localBeforeRemote && !currentEntry.reposOnly && !currentEntry.ignoringAncestry) {
            handleLocalOnly(pb, currentEntry.name);
        }
        result.reset();
        callback.dirOpened(result, new File(currentEntry.path), currentEntry.leftSource, currentEntry.rightSource, null, null);
        currentEntry.skip = result.skip;
        currentEntry.skipChildren = result.skipChildren;
    }

    public void openDir(String path, long revision) throws SVNException {
        SVNDepth subdirDepth = (depth == SVNDepth.IMMEDIATES) ? SVNDepth.EMPTY : depth;
        Entry pb = currentEntry;
        currentEntry = new Entry(false, path, currentEntry, false, subdirDepth, getLocalAbspath(path));

        if (pb.reposOnly) {
            currentEntry.reposOnly = true;
        } else {
            pb.ensureLocalInfo();

            ISVNWCDb.SVNWCDbInfo info = pb.localInfo.get(currentEntry.name);

            if (info == null || info.kind != ISVNWCDb.SVNWCDbKind.Dir || info.status.isNotPresent()) {
                currentEntry.reposOnly = true;
            }

            if (!currentEntry.reposOnly) {
                switch (info.status) {
                    case Normal:
                        break;
                    case Deleted:
                        currentEntry.reposOnly = true;
                        if (!info.haveMoreWork) {
                            if (pb.compared == null) {
                                pb.compared = new HashSet<String>();
                            }
                            pb.compared.add(currentEntry.name);
                        }
                        break;
                    case Added:
                        if (ignoreAncestry) {
                            currentEntry.ignoringAncestry = true;
                        } else {
                            currentEntry.reposOnly = true;
                        }
                        break;
                    default:
                        SVNErrorManager.assertionFailure(false, null, SVNLogType.WC);
                        break;
                }
            }

            if (!currentEntry.reposOnly) {
                currentEntry.rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);
                if (pb.compared == null) {
                    pb.compared = new HashSet<String>();
                }
                pb.compared.add(currentEntry.name);
            }
        }

        currentEntry.leftSource = new SvnDiffSource(this.revision);
        if (localBeforeRemote && !currentEntry.reposOnly && !currentEntry.ignoringAncestry) {
            handleLocalOnly(pb, currentEntry.name);
        }

        callback.dirOpened(result, new File(currentEntry.path), currentEntry.leftSource, currentEntry.rightSource, null, null);
        currentEntry.skip = result.skip;
        currentEntry.skipChildren = result.skipChildren;
    }

    public void closeDir() throws SVNException {
        try {
        boolean reportedClosed = false;
        Entry pb = currentEntry.parent;

        if (!currentEntry.skipChildren && currentEntry.deletes != null && currentEntry.deletes.size() > 0) {
            List<String> children = new ArrayList<String>(currentEntry.deletes);
            Collections.sort(children);

            for (String name : children) {
                handleLocalOnly(currentEntry, name);
                if (currentEntry.compared == null) {
                    currentEntry.compared = new HashSet<String>();
                }
                currentEntry.compared.add(name);
            }
        }

        if (!currentEntry.reposOnly && !currentEntry.skipChildren) {
            walkLocalNodesDiff(currentEntry.localAbspath, currentEntry.path, currentEntry.depth, currentEntry.compared);
        }

        if (currentEntry.skip) {

        } else if (currentEntry.propChanges.size() > 0 || currentEntry.reposOnly || currentEntry.changePropertyCalled) {
            SVNProperties reposProps;
            if (currentEntry.added) {
                reposProps = new SVNProperties();
            } else {
                reposProps = db.getBaseProps(currentEntry.localAbspath);
            }

            if (currentEntry.propChanges.size() > 0 || currentEntry.changePropertyCalled) {
                reposProps.putAll(currentEntry.propChanges);
                reposProps.removeNullValues();
            }

            if (currentEntry.reposOnly) {
                result.reset();
                callback.dirDeleted(result, new File(currentEntry.path), currentEntry.leftSource, reposProps, null);
                reportedClosed = true;
            } else {
                SVNProperties localProps;
                if (diffPristine) {
                    Structure<StructureFields.PristineInfo> pristineInfoStructure = db.readPristineInfo(currentEntry.localAbspath);
                    localProps = pristineInfoStructure.get(StructureFields.PristineInfo.props);
                } else {
                    localProps = db.readProperties(currentEntry.localAbspath);
                }
                SVNProperties propChanges = reposProps.compareTo(localProps);
                if (propChanges.size() > 0) {
                    result.reset();
                    callback.dirChanged(result, new File(currentEntry.path), currentEntry.leftSource, currentEntry.rightSource, reposProps, localProps, propChanges, null);
                    reportedClosed = true;
                }
            }
        }

        if (!reportedClosed && !currentEntry.skip) {
            result.reset();
            callback.dirClosed(result, new File(currentEntry.path), currentEntry.leftSource, currentEntry.rightSource, null);
        }
        if (pb != null && !localBeforeRemote && !currentEntry.reposOnly && !currentEntry.ignoringAncestry) {
            handleLocalOnly(pb, currentEntry.name);
        }
        } finally {
            currentEntry = currentEntry.parent;
        }
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        Entry pb = currentEntry;
        currentEntry = new Entry(true, path, currentEntry, true, SVNDepth.UNKNOWN, getLocalAbspath(path));

        if (pb.skipChildren) {
            currentEntry.skip = true;
            return;
        } else if (pb.reposOnly || !ignoreAncestry) {
            currentEntry.reposOnly = true;
        } else {
            pb.ensureLocalInfo();
            ISVNWCDb.SVNWCDbInfo info = pb.localInfo.get(currentEntry.name);

            if (info == null || info.kind != ISVNWCDb.SVNWCDbKind.File || info.status.isNotPresent()) {
                currentEntry.reposOnly = true;
            }

            if (!currentEntry.reposOnly && info.status != ISVNWCDb.SVNWCDbStatus.Added) {
                currentEntry.reposOnly = true;
            }

            if (!currentEntry.reposOnly) {
                currentEntry.rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);
                currentEntry.ignoringAncestry = true;

                if (pb.compared == null) {
                    pb.compared = new HashSet<String>();
                }
                pb.compared.add(currentEntry.name);
            }
        }

        currentEntry.leftSource = new SvnDiffSource(this.revision);

        result.reset();
        callback.fileOpened(result, new File(currentEntry.path), currentEntry.leftSource, currentEntry.rightSource, null, false, null);
        currentEntry.skip = result.skip;
    }

    public void openFile(String path, long revision) throws SVNException {
        Entry pb = currentEntry;
        currentEntry = new Entry(true, path, currentEntry, false, SVNDepth.UNKNOWN, getLocalAbspath(path));

        if (pb.skipChildren) {
            currentEntry.skip = true;
        } else if (pb.reposOnly) {
            currentEntry.reposOnly = true;
        } else {
            pb.ensureLocalInfo();
            ISVNWCDb.SVNWCDbInfo info = pb.localInfo.get(currentEntry.name);

            if (info == null || info.kind != ISVNWCDb.SVNWCDbKind.File || info.status.isNotPresent()) {
                currentEntry.reposOnly = true;
            }
            if (!currentEntry.reposOnly) {
                switch (info.status) {
                    case Normal:
                        break;
                    case Deleted:
                        currentEntry.reposOnly = true;
                        if (!info.haveMoreWork) {
                            if (pb.compared == null) {
                                pb.compared = new HashSet<String>();
                            }
                            pb.compared.add(currentEntry.name);
                        }
                        break;
                    case Added:
                        if (ignoreAncestry) {
                            currentEntry.ignoringAncestry = true;
                        } else {
                            currentEntry.reposOnly = true;
                        }
                        break;
                    default:
                        SVNErrorManager.assertionFailure(false, null, SVNLogType.WC);
                        break;
                }
            }
            if (!currentEntry.reposOnly) {
                currentEntry.rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);
                if (pb.compared == null) {
                    pb.compared = new HashSet<String>();
                }
                pb.compared.add(currentEntry.name);
            }
        }
        currentEntry.leftSource = new SvnDiffSource(this.revision);

        ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(currentEntry.localAbspath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum, ISVNWCDb.WCDbBaseInfo.BaseInfoField.props);
        currentEntry.baseChecksum = baseInfo.checksum;
        currentEntry.baseProps = baseInfo.props;

        result.reset();
        callback.fileOpened(result, new File(currentEntry.path), currentEntry.leftSource, currentEntry.rightSource, null, false, null);
        currentEntry.skip = result.skip;
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        if (currentEntry.skip) {
            return;
        }
        InputStream sourceStream;
        if (baseChecksum != null && currentEntry.baseChecksum != null) {
            SvnChecksum baseMd5 = db.getPristineMD5(anchorAbspath, currentEntry.baseChecksum);
            if (baseMd5 != null && !baseMd5.getDigest().equals(baseChecksum)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''", currentEntry.localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            sourceStream = db.readPristine(currentEntry.localAbspath, currentEntry.baseChecksum);
        } else if (currentEntry.baseChecksum != null) {
            sourceStream = db.readPristine(currentEntry.localAbspath, currentEntry.baseChecksum);
        } else {
            sourceStream = SVNFileUtil.DUMMY_IN;
        }

        currentEntry.tempFile = createTempFile(db.getWCRootTempDir(currentEntry.localAbspath));
        deltaProcessor.applyTextDelta(sourceStream, currentEntry.tempFile, true);
    }

    public void closeFile(String path, String textChecksum) throws SVNException {
        try {
        Entry pb = currentEntry.parent;

        if (!currentEntry.skip && textChecksum != null) {

            SvnChecksum resultChecksum;
            if (currentEntry.tempFile != null) {
                resultChecksum = currentEntry.resultChecksum;
            } else {
                resultChecksum = currentEntry.baseChecksum;
            }

            if (resultChecksum.getKind() != SvnChecksum.Kind.md5) {
                resultChecksum = db.getPristineMD5(currentEntry.localAbspath, resultChecksum);
            }

            if (!textChecksum.equals(resultChecksum.getDigest())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''", currentEntry.localAbspath);
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
        }

        if (localBeforeRemote && !currentEntry.reposOnly && !currentEntry.ignoringAncestry) {
            handleLocalOnly(pb, currentEntry.name);
        }

        SVNProperties propBase;

        if (currentEntry.added) {
            propBase = new SVNProperties();
        } else {
            propBase = currentEntry.baseProps;
        }

        SVNProperties reposProps = new SVNProperties(propBase);
        reposProps.putAll(currentEntry.propChanges);
        reposProps.removeNullValues();

        File reposFile = currentEntry.tempFile;
        if (reposFile == null) {
            assert currentEntry.baseChecksum != null;
            reposFile = SvnWcDbPristines.getPristinePath(getWcRoot(), currentEntry.baseChecksum);
        }

        if (currentEntry.skip) {

        } else if (currentEntry.reposOnly) {
            result.reset();
            callback.fileDeleted(result, new File(currentEntry.path), currentEntry.leftSource, currentEntry.tempFile, reposProps);
        } else {
            File localFile;
            SVNProperties localProps;
            if (diffPristine) {
                Structure<StructureFields.PristineInfo> pristineInfoStructure = db.readPristineInfo(currentEntry.localAbspath);
                SvnChecksum checksum = pristineInfoStructure.get(StructureFields.PristineInfo.checksum);
                localProps = pristineInfoStructure.get(StructureFields.PristineInfo.props);

                assert checksum != null;

                localFile = SvnWcDbPristines.getPristinePath(getWcRoot(), checksum);
            } else {
                localProps = db.readProperties(currentEntry.localAbspath);
                localFile = context.getTranslatedFile(currentEntry.localAbspath, currentEntry.localAbspath, true, false, false, false, false); //TODO: cancellation?
            }

            SVNProperties propChanges = reposProps.compareTo(localProps);

            result.reset();
            callback.fileChanged(result, new File(currentEntry.path), currentEntry.leftSource, currentEntry.rightSource, reposFile, localFile, reposProps, localProps, true, propChanges);
        }

        if (!localBeforeRemote && !currentEntry.reposOnly && !currentEntry.ignoringAncestry) {
            handleLocalOnly(pb, currentEntry.name);
        }
        } finally {
            currentEntry = currentEntry.parent;
        }
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        if (SVNProperty.isWorkingCopyProperty(name)) {
            return;
        } else if (SVNProperty.isRegularProperty(name)) {
            currentEntry.hasPropChange = true;
            if (currentEntry.propChanges == null) {
                currentEntry.propChanges = new SVNProperties();
            }
            currentEntry.propChanges.put(name, value);
        }
        currentEntry.changePropertyCalled = true;
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        if (SVNProperty.isWorkingCopyProperty(propertyName)) {
            return;
        } else if (SVNProperty.isRegularProperty(propertyName)) {
            currentEntry.hasPropChange = true;
            if (currentEntry.propChanges == null) {
                currentEntry.propChanges = new SVNProperties();
            }
            currentEntry.propChanges.put(propertyName, propertyValue);
        }
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

    private File getLocalAbspath(String path) {
        return new File(anchorAbspath, path);
    }

    private void checkCancelled() throws SVNCancelException {
        canceller.checkCancelled();
    }
//
//    private SVNProperties applyPropsChanges(SVNProperties props, SVNProperties propChanges) {
//        SVNProperties result = new SVNProperties(props);
//        if (propChanges != null) {
//            for(Iterator names = propChanges.nameSet().iterator(); names.hasNext();) {
//                String name = (String) names.next();
//                SVNPropertyValue value = propChanges.getSVNPropertyValue(name);
//                if (value == null) {
//                    result.remove(name);
//                } else {
//                    result.put(name, value);
//                }
//            }
//        }
//        return result;
//    }
//
//    private static void reversePropChanges(SVNProperties base, SVNProperties diff) {
//        Collection<String> namesList = new ArrayList<String>(diff.nameSet());
//        for (Iterator names = namesList.iterator(); names.hasNext();) {
//            String name = (String) names.next();
//            SVNPropertyValue newValue = diff.getSVNPropertyValue(name);
//            SVNPropertyValue oldValue = base.getSVNPropertyValue(name);
//            if (oldValue == null && newValue != null) {
//                base.put(name, newValue);
//                diff.put(name, (SVNPropertyValue) null);
//            } else if (oldValue != null && newValue == null) {
//                base.put(name, (SVNPropertyValue) null);
//                diff.put(name, oldValue);
//            } else if (oldValue != null && newValue != null) {
//                base.put(name, newValue);
//                diff.put(name, oldValue);
//            }
//        }
//    }

    private void walkLocalNodesDiff(File localAbspath, String path, SVNDepth depth, Set<String> compared) throws SVNException {

        if (diffPristine) {
            return;
        }

        boolean inAnchorNotTarget = path.length() == 0 && target.length() != 0;

        ISVNWCDb.WCDbInfo wcDbInfo = db.readInfo(localAbspath, ISVNWCDb.WCDbInfo.InfoField.revision, ISVNWCDb.WCDbInfo.InfoField.propsMod);
        long revision = wcDbInfo.revision;
        boolean propsMod = wcDbInfo.propsMod;

        SvnDiffSource leftSource = new SvnDiffSource(revision);
        SvnDiffSource rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);

        boolean skip = false;
        boolean skipChildren = false;

        if (compared != null) {
            //
            skip = true;
        } else if (!inAnchorNotTarget) {
            result.reset();
            callback.dirOpened(result, new File(path), leftSource, rightSource, null, null);
            skip = result.skip;
            skipChildren = result.skipChildren;
        }

        if (!skipChildren && depth != SVNDepth.EMPTY) {
            SVNDepth depthBelowHere = depth;

            if (depthBelowHere == SVNDepth.IMMEDIATES) {
                depthBelowHere = SVNDepth.EMPTY;
            }

            boolean diffFiles = (depth == SVNDepth.UNKNOWN || depth.compareTo(SVNDepth.FILES) >= 0);
            boolean diffDirectories = (depth == SVNDepth.UNKNOWN || depth.compareTo(SVNDepth.IMMEDIATES) >= 0);

            Map<String, ISVNWCDb.SVNWCDbInfo> nodes = new HashMap<String, ISVNWCDb.SVNWCDbInfo>();
            db.readChildren(localAbspath, nodes, new HashSet<String>());

            List<String> children = new ArrayList<String>(nodes.keySet());
            Collections.sort(children);

            for (String name : children) {
                ISVNWCDb.SVNWCDbInfo info = nodes.get(name);

                if (inAnchorNotTarget && !target.equals(name)) {
                    continue;
                }

                if (compared != null && compared.contains(name)) {
                    continue;
                }

                if (info.status.isNotPresent()) {
                    continue;
                }

                assert info.status == ISVNWCDb.SVNWCDbStatus.Normal ||
                        info.status == ISVNWCDb.SVNWCDbStatus.Added ||
                        info.status == ISVNWCDb.SVNWCDbStatus.Deleted;

                File childAbsPath = SVNFileUtil.createFilePath(localAbspath, name);
                File childRelPath = SVNFileUtil.createFilePath(path, name);

                boolean reposOnly = false;
                boolean localOnly = false;
                ISVNWCDb.SVNWCDbKind baseKind = ISVNWCDb.SVNWCDbKind.Unknown;

                if (!info.haveBase) {
                    localOnly = true;
                } else if (info.status == ISVNWCDb.SVNWCDbStatus.Normal) {
                    baseKind = info.kind;
                } else if (info.status == ISVNWCDb.SVNWCDbStatus.Deleted && (!diffPristine || !info.haveMoreWork)) {
                    reposOnly = true;
                    ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(childAbsPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.status, ISVNWCDb.WCDbBaseInfo.BaseInfoField.kind);
                    baseKind = baseInfo.kind;
                    if (baseInfo.status.isNotPresent()) {
                        continue;
                    }
                } else {
                    ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(childAbsPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.status, ISVNWCDb.WCDbBaseInfo.BaseInfoField.kind);
                    baseKind = baseInfo.kind;
                    if (baseInfo.status.isNotPresent()) {
                        localOnly = true;
                    } else if (baseKind != info.kind || !ignoreAncestry) {
                        reposOnly = true;
                        localOnly = true;
                    }
                }

                if (localBeforeRemote && localOnly) {
                    if (info.kind == ISVNWCDb.SVNWCDbKind.File && diffFiles) {
                        SvnNgDiffUtil.diffLocalOnlyFile(childAbsPath, childRelPath, changelists, this.diffPristine, this.context, this.callback);
                    } else if (info.kind == ISVNWCDb.SVNWCDbKind.Dir && diffDirectories) {
                        SvnNgDiffUtil.diffLocalOnlyDirectory(childAbsPath, childRelPath, depthBelowHere, changelists, this.diffPristine, this.context, this.callback);
                    }
                }

                if (reposOnly) {
                    if (baseKind == ISVNWCDb.SVNWCDbKind.File && diffFiles) {
                        SvnNgDiffUtil.diffBaseOnlyFile(childAbsPath, childRelPath, this.revision, this.db, this.callback);
                    } else if (baseKind == ISVNWCDb.SVNWCDbKind.Dir && diffDirectories) {
                        SvnNgDiffUtil.diffBaseOnlyDirectory(childAbsPath, childRelPath, this.revision, depthBelowHere, this.db, this.callback);
                    }
                } else if (!localOnly) {
                    if (info.kind == ISVNWCDb.SVNWCDbKind.File && diffFiles) {
                        if (info.status != ISVNWCDb.SVNWCDbStatus.Normal || !diffPristine) {
                            SvnNgDiffUtil.diffBaseWorkingDiff(childAbsPath, childRelPath, this.revision, changelists, this.diffPristine, this.context, this.callback);
                        }
                    } else if (info.kind == ISVNWCDb.SVNWCDbKind.Dir && diffDirectories) {
                        walkLocalNodesDiff(childAbsPath, SVNFileUtil.getFilePath(childRelPath), depthBelowHere, null);
                    }
                }

                if (!localBeforeRemote && localOnly) {
                    if (info.kind == ISVNWCDb.SVNWCDbKind.File && diffFiles) {
                        SvnNgDiffUtil.diffLocalOnlyFile(childAbsPath, childRelPath, changelists, this.diffPristine, this.context, this.callback);
                    } else if (info.kind == ISVNWCDb.SVNWCDbKind.Dir && diffDirectories) {
                        SvnNgDiffUtil.diffLocalOnlyDirectory(childAbsPath, childRelPath, depthBelowHere, changelists, this.diffPristine, this.context, this.callback);
                    }
                }
            }
        }

        if (compared != null) {
            return;
        }

        if (!skip && changelists == null && !inAnchorNotTarget && propsMod) {
            SVNWCContext.PropDiffs propDiffs = context.getPropDiffs(localAbspath);
            SVNProperties propChanges = propDiffs.propChanges;
            SVNProperties leftProps = propDiffs.originalProps;
            SVNProperties rightProps = new SVNProperties(leftProps);
            rightProps.putAll(propChanges);
            rightProps.removeNullValues();

            result.reset();
            callback.dirChanged(result, new File(path), leftSource, rightSource, leftProps, rightProps, propChanges, null);
        } else if (!skip) {
            result.reset();
            callback.dirClosed(result, new File(path), leftSource, rightSource, null);
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
        if (props2 != null) {
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
        }
        if (props1 != null) {
            for (Iterator names = props1.nameSet().iterator(); names.hasNext();) {
                String oldPropName = (String) names.next();
                if (!props2.containsName(oldPropName)) {
                    // deleted
                    propsDiff.put(oldPropName, (String) null);
                }
            }
        }
        return propsDiff;
    }

    private SVNWCDbRoot getWcRoot() throws SVNException {
        if (wcRoot == null) {
            SVNWCDb.DirParsedInfo parsed = ((SVNWCDb) db).parseDir(anchorAbspath, SVNSqlJetDb.Mode.ReadOnly);
            wcRoot = parsed.wcDbDir.getWCRoot();
        }
        return wcRoot;
    }

    private void handleLocalOnly(Entry pb, String name) throws SVNException {
        boolean reposDelete = (pb.deletes != null && pb.deletes.contains(name));

        assert !name.contains("/");
        assert !pb.added || ignoreAncestry;

        if (pb.skipChildren) {
            return;
        }

        pb.ensureLocalInfo();
        ISVNWCDb.SVNWCDbInfo info = pb.localInfo.get(name);

        if (info == null || info.status.isNotPresent()) {
            return;
        }

        switch (info.status) {
            case Incomplete:
                return;
            case Normal:
                if (!reposDelete) {
                    return;
                }
                pb.deletes.remove(name);
                break;
            case Deleted:
                if (!(diffPristine && reposDelete)) {
                    return;
                }
                break;
            case Added:
            default:
                break;
        }

        if (info.kind == ISVNWCDb.SVNWCDbKind.Dir) {
            if (pb.depth == SVNDepth.INFINITY || pb.depth == SVNDepth.UNKNOWN) {
                depth = pb.depth;
            } else {
                depth = SVNDepth.EMPTY;
            }

            SvnNgDiffUtil.diffLocalOnlyDirectory(SVNFileUtil.createFilePath(pb.localAbspath, name), SVNFileUtil.createFilePath(pb.path, name), reposDelete ? SVNDepth.INFINITY : depth, changelists, this.diffPristine, this.context, this.callback);
        } else {
            SvnNgDiffUtil.diffLocalOnlyFile(SVNFileUtil.createFilePath(pb.localAbspath, name), SVNFileUtil.createFilePath(pb.path, name), changelists, this.diffPristine, this.context, this.callback);
        }
    }

    private class Entry {
        private boolean isFile;
        private Entry parent;
        private String path;
        private String name;
        private boolean added;
        private SVNDepth depth;
        private SVNProperties propChanges;
        private File localAbspath;
        private SvnChecksum baseChecksum;
        private SvnChecksum resultChecksum;
//        private File file;
        private Set<String> compared;
        private SvnDiffSource leftSource;
        private SvnDiffSource rightSource;
        private boolean skip;
        private boolean skipChildren;
        private Set<String> deletes;
        private boolean reposOnly;
        private boolean ignoringAncestry;
        private File tempFile;
        private Map<String, ISVNWCDb.SVNWCDbInfo> localInfo;
        private SVNProperties baseProps;
        private boolean hasPropChange;
        private boolean changePropertyCalled;

        public Entry(boolean file, String path, Entry parent, boolean added, SVNDepth depth, File localAbspath) {
            this.isFile = file;
            this.path = path;
            this.name = SVNPathUtil.tail(path);
            this.parent = parent;
            this.added = added;
            this.depth = depth;
            this.localAbspath = localAbspath;
            this.propChanges = new SVNProperties();
            this.compared = new HashSet<String>();
        }

        public void ensureLocalInfo() throws SVNException {
            if (localInfo != null) {
                return;
            }
            localInfo = new HashMap<String, ISVNWCDb.SVNWCDbInfo>();
            db.readChildren(localAbspath, localInfo, new HashSet<String>());
        }
    }
}
