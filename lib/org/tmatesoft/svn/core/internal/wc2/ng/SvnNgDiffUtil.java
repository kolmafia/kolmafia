package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslatorInputStream;
import org.tmatesoft.svn.core.internal.wc17.SVNAmbientDepthFilterEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNReporter17;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNCapability;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SvnNgDiffUtil {

    public static void doDiffSummarizeReposWC(SvnTarget target1, SVNRevision revision1, SVNRevision pegRevision, SvnTarget target2, SVNRevision revision2, boolean reverse, SvnNgRepositoryAccess repositoryAccess, SVNWCContext context, boolean useGitDiffFormat, SVNDepth depth, boolean useAncestry, Collection<String> changelists, boolean showCopiesAsAdds, ISvnDiffGenerator generator, ISVNDiffStatusHandler handler, ISVNCanceller canceller) throws SVNException {
        assert !target2.isURL();

        SVNURL url1 = repositoryAccess.getTargetURL(target1);
        String target = context.getActualTarget(target2.getFile());
        File anchor;
        if (target == null || target.length() == 0) {
            anchor = target2.getFile();
        } else {
            anchor = SVNFileUtil.getParentFile(target2.getFile());
        }
        SVNURL anchorUrl = context.getNodeUrl(anchor);

        if (anchorUrl == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "Directory ''{0}'' has no URL", anchor);
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }

//        final ISvnDiffGenerator generator = getDiffGenerator();

        if (pegRevision != SVNRevision.UNDEFINED) {
            url1 = repositoryAccess.getLocations(null, target1, pegRevision, revision1, SVNRevision.UNDEFINED).get(SvnRepositoryAccess.LocationsInfo.startUrl);

            if (generator != null) {
                if (!reverse) {
                    generator.setOriginalTargets(SvnTarget.fromURL(url1), SvnTarget.fromURL(anchorUrl.appendPath(target, false)));
                    generator.setAnchors(SvnTarget.fromURL(url1), SvnTarget.fromURL(anchorUrl.appendPath(target, false)));
                } else {
                    generator.setOriginalTargets(SvnTarget.fromURL(anchorUrl.appendPath(target, false)), SvnTarget.fromURL(url1));
                    generator.setAnchors(SvnTarget.fromURL(anchorUrl.appendPath(target, false)), SvnTarget.fromURL(url1));
                }
            }
        } else {
            if (generator != null) {
                if (!reverse) {
                    generator.setOriginalTargets(target1, target2);
                    generator.setAnchors(target1, target2);
                } else {
                    generator.setOriginalTargets(target2, target1);
                    generator.setAnchors(target2, target1);
                }
            }
        }

        ISvnDiffCallback callback = new SvnDiffSummarizeCallback(target1.getFile(), reverse, anchorUrl, anchor, handler);
        SVNRepository repository2 = repositoryAccess.createRepository(anchorUrl, null, true);

        if (useGitDiffFormat && generator != null) {
            File wcRoot = context.getDb().getWCRoot(anchor);
            generator.setRepositoryRoot(SvnTarget.fromFile(wcRoot));
        }

        boolean serverSupportsDepth = repository2.hasCapability(SVNCapability.DEPTH);

        long revisionNumber1 = repositoryAccess.getRevisionNumber(repository2, url1.equals(target1.getURL()) ? null : target1, revision1, null).lng(SvnRepositoryAccess.RevisionsPair.revNumber);

//        SvnDiffCallback callback = createDiffCallback(generator, reverse, revisionNumber1, -1);

        SVNReporter17 reporter = new SVNReporter17(target2.getFile(), context, false, !serverSupportsDepth, depth, false, false, true, false, SVNDebugLog.getDefaultLog());
        boolean revisionIsBase = isRevisionBase(revision2);
        SvnDiffEditor svnDiffEditor = new SvnDiffEditor(anchor, target, callback, depth, context, reverse, revisionIsBase, showCopiesAsAdds, !useAncestry, changelists, useGitDiffFormat, canceller);

        ISVNUpdateEditor updateEditor = svnDiffEditor;
        if (!serverSupportsDepth && depth == SVNDepth.UNKNOWN) {
            updateEditor = new SVNAmbientDepthFilterEditor17(updateEditor, context, anchor, target, revisionIsBase);
        }
        ISVNEditor editor = SVNCancellableEditor.newInstance(updateEditor, canceller, SVNDebugLog.getDefaultLog());
        try{
            repository2.diff(url1, revisionNumber1, revisionNumber1, target, !useAncestry, getDiffDepth(depth), true, reporter, editor);
        } finally {
            svnDiffEditor.cleanup();
        }
    }

    public static void doDiffWCWC(File localAbsPath, SvnNgRepositoryAccess repositoryAccess, SVNWCContext context, SVNDepth depth, boolean useAncestry, Collection<String> changelists, boolean showCopiesAsAdds, boolean useGitDiffFormat, ISvnDiffGenerator generator, ISvnDiffCallback callback, ISVNCanceller canceller) throws SVNException {
        assert SVNFileUtil.isAbsolute(localAbsPath);
        SvnDiffCallbackResult result = new SvnDiffCallbackResult();

        SVNNodeKind kind = context.getDb().readKind(localAbsPath, false, true, false);

        File anchorAbsPath;
        if (kind == SVNNodeKind.DIR) {
            anchorAbsPath = localAbsPath;
        } else {
            anchorAbsPath = SVNFileUtil.getParentFile(localAbsPath);
        }

        if (useGitDiffFormat) {
            showCopiesAsAdds = true;
        }
        if (showCopiesAsAdds) {
            useAncestry = true;
        }
        ISvnDiffCallback2 callback2 = new SvnDiffCallbackWrapper(callback, true, anchorAbsPath);
        if (!showCopiesAsAdds && !useGitDiffFormat) {
            callback2 = new SvnCopyAsChangedDiffCallback(callback2);
        }
        boolean getAll;
        if (showCopiesAsAdds || useGitDiffFormat || useAncestry) {
            getAll = true;
        } else {
            getAll = false;
        }

        DiffStatusCallback diffStatusCallback = new DiffStatusCallback(anchorAbsPath, !useAncestry, showCopiesAsAdds, changelists, callback2,  context);

        SVNStatusEditor17 statusEditor = new SVNStatusEditor17(localAbsPath, context, null, true, getAll, depth, diffStatusCallback);
        statusEditor.walkStatus(localAbsPath, depth, getAll, true, false, null);

        while (diffStatusCallback.currentNode != null) {
            DiffStatusCallback.NodeState ns = diffStatusCallback.currentNode;

            if (!ns.skip) {
                if (ns.propChanges != null) {
                    callback2.dirChanged(result, ns.relPath, ns.leftSource, ns.rightSource, ns.leftProps, ns.rightProps, ns.propChanges, null);
                } else {
                    callback2.dirClosed(result, ns.relPath, ns.leftSource, ns.rightSource, null);
                }
            }
            diffStatusCallback.currentNode = ns.parent;
        }
    }

    private static SVNDepth getDiffDepth(SVNDepth depth) {
        return depth != SVNDepth.INFINITY ? depth : SVNDepth.UNKNOWN;
    }

    private static boolean isRevisionBase(SVNRevision revision2) {
        return revision2 == SVNRevision.BASE;
    }

    private static class DiffStatusCallback implements ISvnObjectReceiver<SvnStatus> {

        private final File anchorAbsPath;
        private final boolean ignoreAncestry;
        private final boolean showCopiesAsAdds;
        private final Collection<String> changelists;
        private final ISvnDiffCallback2 callback;
        private final ISVNWCDb db;
        private final SVNWCContext context;

        private NodeState currentNode;

        private DiffStatusCallback(File anchorAbsPath, boolean ignoreAncestry, boolean showCopiesAsAdds, Collection<String> changelists, ISvnDiffCallback2 callback, SVNWCContext context) {
            this.anchorAbsPath = anchorAbsPath;
            this.ignoreAncestry = ignoreAncestry;
            this.showCopiesAsAdds = showCopiesAsAdds;
            this.changelists = changelists;
            this.callback = callback;
            this.db = context.getDb();
            this.context = context;
        }

        public void receive(SvnTarget target, SvnStatus status) throws SVNException {
            SvnDiffCallbackResult result = new SvnDiffCallbackResult();

            File localAbsPath = target.getFile();

            if (!status.isVersioned()) {
                return;
            }

            if (status.getNodeStatus() == SVNStatusType.STATUS_CONFLICTED &&
                    status.getTextStatus() == SVNStatusType.STATUS_NONE &&
                    status.getPropertiesStatus() == SVNStatusType.STATUS_NONE) {
                return;
            }
            if (status.getNodeStatus() == SVNStatusType.STATUS_NORMAL && !status.isCopied()) {
                return;
            }

            while (currentNode != null && !SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(currentNode.localAbsPath), SVNFileUtil.getFilePath(localAbsPath))) {
                NodeState ns = currentNode;

                if (!ns.skip) {
                    if (ns.propChanges != null) {
                        callback.dirChanged(result, ns.relPath, ns.leftSource, ns.rightSource, ns.leftProps, ns.rightProps, ns.propChanges, null);
                    } else {
                        callback.dirClosed(result, ns.relPath, ns.leftSource, ns.rightSource, null);
                    }
                }

                currentNode = ns.parent;

            }
            ensureState(SVNFileUtil.getParentFile(localAbsPath), false);

            if (currentNode != null && currentNode.skipChildren) {
                return;
            }
            if (changelists != null && (status.getChangelist() == null || !changelists.contains(status.getChangelist()))) {
                return;
            }

            SVNNodeKind baseKind = SVNNodeKind.UNKNOWN;
            SVNNodeKind dbKind = status.getKind();
            SVNDepth depthBelowHere = SVNDepth.UNKNOWN;

            File childAbsPath = localAbsPath;
            File childRelPath = SVNFileUtil.skipAncestor(anchorAbsPath, localAbsPath);

            boolean reposOnly = false;
            boolean localOnly = false;

            Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(localAbsPath, StructureFields.NodeInfo.status, StructureFields.NodeInfo.haveBase);
            ISVNWCDb.SVNWCDbStatus dbStatus = nodeInfoStructure.get(StructureFields.NodeInfo.status);
            boolean haveBase = nodeInfoStructure.is(StructureFields.NodeInfo.haveBase);

            ISVNWCDb.SVNWCDbStatus baseStatus;

            if (!haveBase) {
                localOnly = true;
            } else if (dbStatus == ISVNWCDb.SVNWCDbStatus.Normal) {
                baseKind = dbKind;
            } else if (dbStatus == ISVNWCDb.SVNWCDbStatus.Deleted) {
                reposOnly = true;
                ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(localAbsPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.status, ISVNWCDb.WCDbBaseInfo.BaseInfoField.kind);
                baseStatus = baseInfo.status;
                baseKind = baseInfo.kind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;

                if (baseStatus != ISVNWCDb.SVNWCDbStatus.Normal) {
                    return;
                }
            } else {
                ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(localAbsPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.status, ISVNWCDb.WCDbBaseInfo.BaseInfoField.kind);
                baseStatus = baseInfo.status;
                baseKind = baseInfo.kind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;

                if (baseStatus != ISVNWCDb.SVNWCDbStatus.Normal) {
                    localOnly = true;
                } else if (baseKind != dbKind || !ignoreAncestry) {
                    reposOnly = true;
                    localOnly = true;
                }
            }


            if (reposOnly) {
                if (baseKind == SVNNodeKind.FILE) {
                    diffBaseOnlyFile(childAbsPath, childRelPath, SVNRepository.INVALID_REVISION, this.db, this.callback);
                } else if (baseKind == SVNNodeKind.DIR) {
                    diffBaseOnlyDirectory(childAbsPath, childRelPath, SVNRepository.INVALID_REVISION, depthBelowHere, this.db, this.callback);
                }
            } else if (!localOnly) {
                if (dbKind == SVNNodeKind.FILE) {
                    diffBaseWorkingDiff(childAbsPath, childRelPath, SVNRepository.INVALID_REVISION, changelists, false, context, callback);
                } else if (dbKind == SVNNodeKind.DIR) {
                    ensureState(localAbsPath, false);
                    if (status.getPropertiesStatus() != SVNStatusType.STATUS_NONE && status.getPropertiesStatus() != SVNStatusType.STATUS_NORMAL) {
                        currentNode.leftProps = db.getBaseProps(localAbsPath);
                        currentNode.rightProps = db.readProperties(localAbsPath);
                        currentNode.propChanges = currentNode.leftProps.compareTo(currentNode.rightProps);
                    }
                }
            }

            if (localOnly) {
                if (dbKind == SVNNodeKind.FILE) {
                    diffLocalOnlyFile(childAbsPath, childRelPath, changelists, false, context, callback);
                } else if (dbKind == SVNNodeKind.DIR) {
                    diffLocalOnlyDirectory(childAbsPath, childRelPath, depthBelowHere, changelists, false, context, callback);
                }
            }

            if (dbKind == SVNNodeKind.DIR && (localOnly || reposOnly)) {
                ensureState(localAbsPath, true);
            }
        }

        private void ensureState(File localAbsPath, boolean recursiveSkip) throws SVNException {
            if (currentNode == null) {
                if (!SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(anchorAbsPath), SVNFileUtil.getFilePath(localAbsPath))) {
                    return;
                }
                ensureState(SVNFileUtil.getParentFile(localAbsPath), false);
            } else if (SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(currentNode.localAbsPath), SVNFileUtil.getFilePath(localAbsPath))) {
                ensureState(SVNFileUtil.getParentFile(localAbsPath), false);
            } else {
                return;
            }

            if (currentNode != null && currentNode.skipChildren) {
                return;
            }

            NodeState ns = new NodeState();
            ns.localAbsPath = localAbsPath;
            ns.relPath = SVNFileUtil.skipAncestor(anchorAbsPath, ns.localAbsPath);
            ns.parent = currentNode;
            currentNode = ns;

            if (recursiveSkip) {
                ns.skip = true;
                ns.skipChildren = true;
                return;
            }

            long revision;
            try {
                ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(localAbsPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.revision);
                revision = baseInfo.revision;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
                revision = 0;
            }

            ns.leftSource = new SvnDiffSource(revision);
            ns.rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);

            SvnDiffCallbackResult result = new SvnDiffCallbackResult();
            callback.dirOpened(result, ns.relPath, ns.leftSource, ns.rightSource, null, null);
            ns.skip = result.skip;
            ns.skipChildren = result.skipChildren;
        }

        private static class NodeState {
            private NodeState parent;

            private File localAbsPath;
            private File relPath;

            private SvnDiffSource leftSource;
            private SvnDiffSource rightSource;
            private SvnDiffSource copySource;

            private boolean skip;
            private boolean skipChildren;

            private SVNProperties leftProps;
            private SVNProperties rightProps;
            private SVNProperties propChanges;
        }
    }

    protected static void diffBaseOnlyFile(File localAbsPath, File relPath, long revision, ISVNWCDb db, ISvnDiffCallback2 callback) throws SVNException {
        SvnDiffCallbackResult result = new SvnDiffCallbackResult();

        ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(localAbsPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.status, ISVNWCDb.WCDbBaseInfo.BaseInfoField.kind,
                ISVNWCDb.WCDbBaseInfo.BaseInfoField.revision, ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum, ISVNWCDb.WCDbBaseInfo.BaseInfoField.props);
        ISVNWCDb.SVNWCDbStatus status = baseInfo.status;
        ISVNWCDb.SVNWCDbKind kind = baseInfo.kind;
        if (!SVNRevision.isValidRevisionNumber(revision)) {
            revision = baseInfo.revision;
        }
        SvnChecksum checksum = baseInfo.checksum;
        SVNProperties props = baseInfo.props;

        assert status == ISVNWCDb.SVNWCDbStatus.Normal && kind == ISVNWCDb.SVNWCDbKind.File && checksum != null;

        SvnDiffSource leftSource = new SvnDiffSource(revision);

        result.reset();
        callback.fileOpened(result, relPath, leftSource, null, null, false, null);
        boolean skip = result.skip;

        if (skip) {
            return;
        }

        File pristineFile = db.getPristinePath(localAbsPath, checksum);

        result.reset();
        callback.fileDeleted(result, relPath, leftSource, pristineFile, props);
    }

    protected static void diffBaseOnlyDirectory(File localAbsPath, File relPath, long revision, SVNDepth depth, ISVNWCDb db, ISvnDiffCallback2 callback) throws SVNException {
        SvnDiffCallbackResult result = new SvnDiffCallbackResult();

        long reportRevision = revision;
        if (!SVNRevision.isValidRevisionNumber(reportRevision)) {
            ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(localAbsPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.revision);
            reportRevision = baseInfo.revision;
        }
        SvnDiffSource leftSource = new SvnDiffSource(reportRevision);

        result.reset();
        callback.dirOpened(result, relPath, leftSource, null, null, null);
        boolean skip = result.skip;
        boolean skipChildren = result.skipChildren;

        if (!skipChildren && (depth == SVNDepth.UNKNOWN || depth.compareTo(SVNDepth.EMPTY) > 0)) {
            Map<String, ISVNWCDb.WCDbBaseInfo> nodes = db.getBaseChildrenMap(localAbsPath, true);
            List<String> children = new ArrayList<String>(nodes.keySet());
            Collections.sort(children);

            for (String name : children) {
                ISVNWCDb.WCDbBaseInfo info = nodes.get(name);

                if (info.status != ISVNWCDb.SVNWCDbStatus.Normal) {
                    continue;
                }

                File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, name);
                File childRelPath = SVNFileUtil.createFilePath(relPath, name);

                switch (info.kind) {
                    case File:
                    case Symlink:
                        SvnNgDiffUtil.diffBaseOnlyFile(childAbsPath, childRelPath, revision, db, callback);
                        break;
                    case Dir:
                        if (depth.compareTo(SVNDepth.FILES) > 0 || depth == SVNDepth.UNKNOWN) {
                            SVNDepth depthBelowHere = depth;
                            if (depthBelowHere == SVNDepth.IMMEDIATES) {
                                depthBelowHere = SVNDepth.EMPTY;
                            }
                            diffBaseOnlyDirectory(childAbsPath, childRelPath, revision, depthBelowHere, db, callback);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        if (!skip) {
            SVNProperties props = db.getBaseProps(localAbsPath);

            result.reset();
            callback.dirDeleted(result, relPath, leftSource, props, null);
        }
    }

    protected static void diffLocalOnlyFile(File localAbsPath, File relPath, Collection<String> changelists, boolean diffPristine, SVNWCContext context, ISvnDiffCallback2 callback) throws SVNException {
        SvnDiffCallbackResult result = new SvnDiffCallbackResult();
        ISVNWCDb db = context.getDb();

        Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(localAbsPath, StructureFields.NodeInfo.status, StructureFields.NodeInfo.kind, StructureFields.NodeInfo.revision,
                StructureFields.NodeInfo.checksum,
                StructureFields.NodeInfo.originalReposRelpath, StructureFields.NodeInfo.originalRevision,
                StructureFields.NodeInfo.changelist,
                StructureFields.NodeInfo.hadProps, StructureFields.NodeInfo.propsMod);
        ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
        ISVNWCDb.SVNWCDbKind kind = nodeInfoStructure.get(StructureFields.NodeInfo.kind);
        long revision = nodeInfoStructure.lng(StructureFields.NodeInfo.revision);
        SvnChecksum checksum = nodeInfoStructure.get(StructureFields.NodeInfo.checksum);
        File originalReposRelPath = nodeInfoStructure.get(StructureFields.NodeInfo.originalReposRelpath);
        long originalRevision = nodeInfoStructure.lng(StructureFields.NodeInfo.originalRevision);
        String changelist = nodeInfoStructure.get(StructureFields.NodeInfo.changelist);
        boolean hadProps = nodeInfoStructure.is(StructureFields.NodeInfo.hadProps);
        boolean propMods = nodeInfoStructure.is(StructureFields.NodeInfo.propsMod);

        assert kind == ISVNWCDb.SVNWCDbKind.File && (status == ISVNWCDb.SVNWCDbStatus.Normal || status == ISVNWCDb.SVNWCDbStatus.Added || (status == ISVNWCDb.SVNWCDbStatus.Deleted && diffPristine));

        if (changelist != null && changelists != null && !changelists.contains(changelist)) {
            return;
        }

        SVNProperties pristineProps;
        if (status == ISVNWCDb.SVNWCDbStatus.Deleted) {
            assert diffPristine;

            Structure<StructureFields.PristineInfo> pristineInfoStructure = db.readPristineInfo(localAbsPath);
            status = pristineInfoStructure.get(StructureFields.PristineInfo.status);
            kind = pristineInfoStructure.get(StructureFields.PristineInfo.kind);
            checksum = pristineInfoStructure.get(StructureFields.PristineInfo.checksum);
            hadProps = pristineInfoStructure.is(StructureFields.PristineInfo.hadProps);
            pristineProps = pristineInfoStructure.get(StructureFields.PristineInfo.props);
            propMods = false;
        } else if (!hadProps) {
            pristineProps = new SVNProperties();
        } else {
            pristineProps = db.readPristineProperties(localAbsPath);
        }

        SvnDiffSource copyFromSource = null;
        if (originalReposRelPath != null) {
            copyFromSource = new SvnDiffSource(originalRevision);
            copyFromSource.setReposRelPath(originalReposRelPath);
        }

        boolean fileMod = true;
        SvnDiffSource rightSource;
        if (propMods || !SVNRevision.isValidRevisionNumber(revision)) {
            rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);
        } else {
            if (diffPristine) {
                fileMod = false;
            } else {
                fileMod = context.isTextModified(localAbsPath, false);
            }

            if (!fileMod) {
                rightSource = new SvnDiffSource(revision);
            } else {
                rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);
            }
        }

        result.reset();
        callback.fileOpened(result, relPath, null, rightSource, copyFromSource, false, null);
        boolean skip = result.skip;

        if (skip) {
            return;
        }

        SVNProperties rightProps;
        if (propMods && !diffPristine) {
            rightProps = db.readProperties(localAbsPath);
        } else {
            rightProps = new SVNProperties(pristineProps);
        }

        File pristineFile;
        if (checksum != null) {
            pristineFile = db.getPristinePath(localAbsPath, checksum);
        } else {
            pristineFile = null;
        }

        File translatedFile;
        if (diffPristine) {
            translatedFile = pristineFile;
        } else {
            translatedFile = context.getTranslatedFile(localAbsPath, localAbsPath, true, false, true, false, false);
        }

        result.reset();
        callback.fileAdded(result, relPath, copyFromSource, rightSource,
                copyFromSource != null ? pristineFile : null,
                translatedFile,
                copyFromSource != null ? pristineProps : null,
                rightProps);
    }

    protected static void diffLocalOnlyDirectory(File localAbsPath, File relPath, SVNDepth depth, Collection<String> changelists, boolean diffPristine, SVNWCContext context, ISvnDiffCallback2 callback) throws SVNException {
        SvnDiffCallbackResult result = new SvnDiffCallbackResult();
        ISVNWCDb db = context.getDb();

        boolean skip = false;
        boolean skipChildren = false;
        SvnDiffSource rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);
        SVNDepth depthBelowHere = depth;

        result.reset();
        callback.dirOpened(result, relPath, null, rightSource, null, null);
        skip = result.skip;
        skipChildren = result.skipChildren;

        Map<String, ISVNWCDb.SVNWCDbInfo> nodes = new HashMap<String, ISVNWCDb.SVNWCDbInfo>();
        db.readChildren(localAbsPath, nodes, new HashSet<String>());

        if (depthBelowHere == SVNDepth.IMMEDIATES) {
            depthBelowHere = SVNDepth.EMPTY;
        }

        List<String> children = new ArrayList<String>(nodes.keySet());
        Collections.sort(children);

        for (String name : children) {
            File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, name);
            ISVNWCDb.SVNWCDbInfo info = nodes.get(name);

            if (info.status.isNotPresent()) {
                continue;
            }

            if (!diffPristine && info.status == ISVNWCDb.SVNWCDbStatus.Deleted) {
                continue;
            }

            File childRelPath = SVNFileUtil.createFilePath(relPath, name);

            switch (info.kind) {
                case File:
                case Symlink:
                    diffLocalOnlyFile(childAbsPath, childRelPath, changelists, diffPristine, context, callback);
                    break;
                case Dir:
                    if (depth.compareTo(SVNDepth.FILES) > 0 || depth == SVNDepth.UNKNOWN) {
                        diffLocalOnlyDirectory(childAbsPath, childRelPath, depthBelowHere, changelists, diffPristine, context, callback);
                    }
                    break;
                default:
                    break;
            }
        }

        if (!skip) {
            SVNProperties rightProps;
            if (diffPristine) {
                rightProps = db.readPristineProperties(localAbsPath);
            } else {
                rightProps = db.readProperties(localAbsPath);
            }

            result.reset();
            callback.dirAdded(result, relPath, null, rightSource, null, rightProps, null);
        }
    }

    protected static void diffBaseWorkingDiff(File localAbsPath, File relPath, long revision, Collection<String> changeists, boolean diffPristine, SVNWCContext context, ISvnDiffCallback2 callback) throws SVNException {
        SvnDiffCallbackResult result = new SvnDiffCallbackResult();
        ISVNWCDb db = context.getDb();

        Structure<StructureFields.NodeInfo> nodeInfoStructure = db.readInfo(localAbsPath, StructureFields.NodeInfo.status, StructureFields.NodeInfo.revision,
                StructureFields.NodeInfo.checksum, StructureFields.NodeInfo.recordedSize, StructureFields.NodeInfo.recordedTime,
                StructureFields.NodeInfo.changelist, StructureFields.NodeInfo.hadProps, StructureFields.NodeInfo.propsMod);
        ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
        long dbRevision = nodeInfoStructure.lng(StructureFields.NodeInfo.revision);
        SvnChecksum workingChecksum = nodeInfoStructure.get(StructureFields.NodeInfo.checksum);
        long recordedSize = nodeInfoStructure.lng(StructureFields.NodeInfo.recordedSize);
        long recordedTime = nodeInfoStructure.lng(StructureFields.NodeInfo.recordedTime);
        String changelist = nodeInfoStructure.get(StructureFields.NodeInfo.changelist);
        boolean hadProps = nodeInfoStructure.is(StructureFields.NodeInfo.hadProps);
        boolean propsMod = nodeInfoStructure.is(StructureFields.NodeInfo.propsMod);

        SvnChecksum checksum = workingChecksum;

        assert status == ISVNWCDb.SVNWCDbStatus.Normal || status == ISVNWCDb.SVNWCDbStatus.Added || (status == ISVNWCDb.SVNWCDbStatus.Deleted && diffPristine);

        if (changeists != null && !changeists.contains(changelist)) {
            return;
        }

        boolean filesSame = false;

        if (status != ISVNWCDb.SVNWCDbStatus.Normal) {
            ISVNWCDb.WCDbBaseInfo baseInfo = db.getBaseInfo(localAbsPath, ISVNWCDb.WCDbBaseInfo.BaseInfoField.status, ISVNWCDb.WCDbBaseInfo.BaseInfoField.revision, ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum, ISVNWCDb.WCDbBaseInfo.BaseInfoField.hadProps);
            ISVNWCDb.SVNWCDbStatus baseStatus = baseInfo.status;
            dbRevision = baseInfo.revision;
            checksum = baseInfo.checksum;
            hadProps = baseInfo.hadProps;
            recordedSize = -1;
            recordedTime = 0;
            propsMod = true;
        } else if (diffPristine) {
            filesSame = true;
        } else {
            SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath));

            if (kind != SVNNodeKind.FILE || (kind == SVNNodeKind.FILE && SVNFileUtil.getFileLength(localAbsPath) == recordedSize && SVNFileUtil.getFileLastModifiedMicros(localAbsPath) == recordedTime)) {
                filesSame = true;
            }
        }
        if (filesSame && !propsMod) {
            return;
        }
        assert checksum != null;

        if (!SVNRevision.isValidRevisionNumber(revision)) {
            revision = dbRevision;
        }
        SvnDiffSource leftSource = new SvnDiffSource(revision);
        SvnDiffSource rightSource = new SvnDiffSource(SVNRepository.INVALID_REVISION);

        result.reset();
        callback.fileOpened(result, relPath, leftSource, rightSource, null, false, null);
        boolean skip = result.skip;

        if (skip) {
            return;
        }
        File pristineFile = db.getPristinePath(localAbsPath, checksum);

        File localFile;
        if (diffPristine) {
            localFile = db.getPristinePath(localAbsPath, workingChecksum);
        } else if (! (hadProps || propsMod)) {
            localFile = localAbsPath;
        } else if (filesSame) {
            localFile = pristineFile;
        } else {
            localFile = context.getTranslatedFile(localAbsPath, localAbsPath, true, false, true, false, false);
        }

        if (!filesSame) {
            filesSame = SVNFileUtil.compareFiles(localFile, pristineFile, null);
        }

        SVNProperties baseProps;
        if (hadProps) {
            baseProps = db.getBaseProps(localAbsPath);
        } else {
            baseProps = new SVNProperties();
        }

        SVNProperties localProps;
        if (status == ISVNWCDb.SVNWCDbStatus.Normal && (diffPristine || !propsMod)) {
            localProps = baseProps;
        } else if (diffPristine) {
            localProps = db.readPristineProperties(localAbsPath);
        } else {
            localProps = db.readProperties(localAbsPath);
        }

        SVNProperties propChanges = baseProps.compareTo(localProps);

        if (propChanges.size() > 0 || !filesSame) {
            result.reset();
            callback.fileChanged(result, relPath, leftSource, rightSource, pristineFile, localFile, baseProps, localProps, !filesSame, propChanges);
        } else {
            result.reset();
            callback.fileClosed(result, relPath, leftSource, rightSource);
        }
    }

    public static void doArbitraryNodesDiff(SvnTarget target1, SvnTarget target2, SVNDepth depth, SVNWCContext context, ISvnDiffCallback callback, ISVNCanceller canceller) throws SVNException {
        File path1 = target1.getFile();
        File path2 = target2.getFile();
        SVNNodeKind kind1 = SVNFileType.getNodeKind(SVNFileType.getType(path1));
        SVNNodeKind kind2 = SVNFileType.getNodeKind(SVNFileType.getType(path2));

        if (kind1 != kind2) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, "''{0}'' is not the same node kind as ''{1}''", path1, path2);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        if (depth == SVNDepth.UNKNOWN) {
            depth = SVNDepth.INFINITY;
        }
        if (kind1 == SVNNodeKind.FILE) {
            doArbitraryFilesDiff(path1, path2, SVNFileUtil.createFilePath(SVNFileUtil.getFileName(path1)), false, false, null, context, callback, canceller);
        } else if (kind1 == SVNNodeKind.DIR) {
            doArbitraryDirsDiff(path1, path2, null, null, depth, context, callback, canceller);
        } else {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, "''{0}'' is not a file or directory", kind1 == SVNNodeKind.NONE ? path1 : path2);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
    }


    private static void doArbitraryFilesDiff(File localAbsPath1, File localAbsPath2, File path, boolean file1IsEmpty, boolean file2IsEmpty, SVNProperties originalPropertiesOverride, SVNWCContext context, ISvnDiffCallback callback, ISVNCanceller canceller) throws SVNException {
        if (canceller != null) {
            canceller.checkCancelled();
        }

        SvnDiffCallbackResult result = new SvnDiffCallbackResult();

        SVNProperties originalProps;
        if (originalPropertiesOverride != null) {
            originalProps = originalPropertiesOverride;
        } else {
            if (localAbsPath1 != null) {
                try {
                    originalProps = context.getActualProps(localAbsPath1);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND && e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                        throw e;
                    }
                    originalProps = new SVNProperties();
                }
            } else {
                originalProps = new SVNProperties();
            }
        }

        SVNProperties modifiedProps;
        if (localAbsPath2 != null) {
            try {
                modifiedProps = context.getActualProps(localAbsPath2);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND && e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                    throw e;
                }
                modifiedProps = new SVNProperties();
            }
        } else {
            modifiedProps = new SVNProperties();
        }

        SVNProperties propChanges = originalProps.compareTo(modifiedProps);

        String originalMimeType = originalProps.getStringValue(SVNProperty.MIME_TYPE);
        if (!file1IsEmpty && originalMimeType == null) {
            String mimeType = SVNFileUtil.detectMimeType(localAbsPath1, context.getOptions().getFileExtensionsToMimeTypes());

            if (mimeType != null) {
                originalMimeType = mimeType;
            }
        }

        String modifiedMimeType = modifiedProps.getStringValue(SVNProperty.MIME_TYPE);
        if (!file2IsEmpty && modifiedMimeType == null) {
            String mimeType = SVNFileUtil.detectMimeType(localAbsPath2, context.getOptions().getFileExtensionsToMimeTypes());

            if (mimeType != null) {
                modifiedMimeType = mimeType;
            }
        }
        if (file1IsEmpty && !file2IsEmpty) {
            callback.fileAdded(result, path, localAbsPath1, localAbsPath2, SVNRepository.INVALID_REVISION, SVNRepository.INVALID_REVISION,
                    originalMimeType, modifiedMimeType, null, SVNRepository.INVALID_REVISION, propChanges, originalProps);
        } else if (!file1IsEmpty && file2IsEmpty) {
            callback.fileDeleted(result, path, localAbsPath1, localAbsPath2, originalMimeType, modifiedMimeType, originalProps);
        } else {
            InputStream inputStream1 = SVNFileUtil.openFileForReading(localAbsPath1);
            InputStream inputStream2 = SVNFileUtil.openFileForReading(localAbsPath2);

            try {
                if (originalProps != null) {
                    byte[] eol = SVNTranslator.getEOL(originalProps.getStringValue(SVNProperty.EOL_STYLE), context.getOptions());
                    if (eol != null) {
                        inputStream1 = new SVNTranslatorInputStream(inputStream1, eol, true, null, false);
                    }
                }
                if (modifiedProps != null) {
                    byte[] eol = SVNTranslator.getEOL(modifiedProps.getStringValue(SVNProperty.EOL_STYLE), context.getOptions());
                    if (eol != null) {
                        inputStream2 = new SVNTranslatorInputStream(inputStream2, eol, true, null, false);
                    }
                }
                boolean same = SVNFileUtil.compare(inputStream1, inputStream2);
                if (!same || propChanges.size() > 0) {
                    callback.fileChanged(result, path, same ? null : localAbsPath1, same ? null : localAbsPath2, SVNRepository.INVALID_REVISION, SVNRepository.INVALID_REVISION, originalMimeType, modifiedMimeType, propChanges, originalProps);
                }
            } finally {
                SVNFileUtil.closeFile(inputStream1);
                SVNFileUtil.closeFile(inputStream2);
            }
        }
    }

    private static void doArbitraryDirsDiff(File localAbsPath1, File localAbsPath2, File rootAbsPath1, File rootAbsPath2, SVNDepth depth, SVNWCContext context, ISvnDiffCallback callback, ISVNCanceller canceller) throws SVNException {
        SVNNodeKind kind1 = null;
        try {
            kind1 = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath1.getCanonicalFile()));
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        ArbitraryDiffWalker diffWalker = new ArbitraryDiffWalker();
        diffWalker.recursingWithinAddedSubtree = (kind1 != SVNNodeKind.DIR);
        diffWalker.root1AbsPath = rootAbsPath1 != null ? rootAbsPath1 : localAbsPath1;
        diffWalker.root2AbsPath = rootAbsPath2 != null ? rootAbsPath2 : localAbsPath2;
        diffWalker.recursingWithinAdmDir = false;

        if (depth.compareTo(SVNDepth.IMMEDIATES) <= 0) {
            arbitraryDiffThisDir(diffWalker, localAbsPath1, depth, context, callback, canceller);
        } else if (depth == SVNDepth.INFINITY) {
            walkDirectory(diffWalker.recursingWithinAddedSubtree ? localAbsPath2 : localAbsPath1, diffWalker, context, callback, canceller);
        }
    }

    private static void arbitraryDiffThisDir(ArbitraryDiffWalker diffWalker, File localAbsPath, SVNDepth depth, SVNWCContext context, ISvnDiffCallback callback, ISVNCanceller canceller) throws SVNException {
        SvnDiffCallbackResult result = new SvnDiffCallbackResult();

        if (diffWalker.recursingWithinAdmDir) {
            if (SVNFileUtil.skipAncestor(diffWalker.admDirAbsPath, localAbsPath) != null) {
                return;
            } else {
                diffWalker.recursingWithinAdmDir = false;
                diffWalker.admDirAbsPath = null;
            }
        } else if (SVNFileUtil.getFileName(localAbsPath).equals(SVNFileUtil.getAdminDirectoryName())) {
            diffWalker.recursingWithinAdmDir = true;
            diffWalker.admDirAbsPath = localAbsPath;
            return;
        }

        File childRelPath;
        if (diffWalker.recursingWithinAddedSubtree) {
            childRelPath = SVNFileUtil.skipAncestor(diffWalker.root2AbsPath, localAbsPath);
        } else {
            childRelPath = SVNFileUtil.skipAncestor(diffWalker.root1AbsPath, localAbsPath);
        }
        if (childRelPath == null) {
            return;
        }
        File localAbsPath1 = SVNFileUtil.createFilePath(diffWalker.root1AbsPath, childRelPath);
        SVNNodeKind kind1 = null;
        try {
            kind1 = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath1.getCanonicalFile()));
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        File localAbsPath2 = SVNFileUtil.createFilePath(diffWalker.root2AbsPath, childRelPath);
        SVNNodeKind kind2 = null;
        try {
            kind2 = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath2.getCanonicalFile()));
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        File[] children1 = null;
        if (depth.compareTo(SVNDepth.EMPTY) > 0) {
            if (kind1 == SVNNodeKind.DIR) {
                children1 = SVNFileListUtil.listFiles(localAbsPath1);
            } else {
                children1 = new File[0];
            }
        }
        File[] children2 = null;
        if (kind2 == SVNNodeKind.DIR) {
            SVNProperties originalProps;
            try {
                originalProps = context.getActualProps(localAbsPath1);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND && e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                    throw e;
                }
                originalProps = new SVNProperties();
            }
            SVNProperties modifiedProps;
            try {
                modifiedProps = context.getActualProps(localAbsPath2);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND && e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                    throw e;
                }
                modifiedProps = new SVNProperties();
            }
            SVNProperties propChanges = originalProps.compareTo(modifiedProps);
            if (propChanges.size() > 0) {
                callback.dirPropsChanged(result, childRelPath, false, propChanges, originalProps);
            }
            if (depth.compareTo(SVNDepth.EMPTY) > 0) {
                children2 = SVNFileListUtil.listFiles(localAbsPath2);
            }
        } else if (depth.compareTo(SVNDepth.EMPTY) > 0) {
            children2 = new File[0];
        }

        if (depth.compareTo(SVNDepth.EMPTY) <= 0) {
            return;
        }

        Set<String> mergedChildren = new HashSet<String>();
        if (children1 != null) {
            for (File child1 : children1) {
                mergedChildren.add(SVNFileUtil.getFileName(child1));
            }
        }
        if (children2 != null) {
            for (File child2 : children2) {
                mergedChildren.add(SVNFileUtil.getFileName(child2));
            }
        }
        List<String> sortedChildren = new ArrayList<String>(mergedChildren);
        Collections.sort(sortedChildren);
        for (String name : sortedChildren) {
            if (canceller != null) {
                canceller.checkCancelled();
            }

            if (name.equals(SVNFileUtil.getAdminDirectoryName())) {
                continue;
            }
            File childAbsPath1 = SVNFileUtil.createFilePath(localAbsPath1, name);
            File childAbsPath2 = SVNFileUtil.createFilePath(localAbsPath2, name);

            SVNNodeKind childKind1 = null;
            SVNNodeKind childKind2 = null;
            try {
                childKind1 = SVNFileType.getNodeKind(SVNFileType.getType(childAbsPath1.getCanonicalFile()));
                childKind2 = SVNFileType.getNodeKind(SVNFileType.getType(childAbsPath2.getCanonicalFile()));
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            if (childKind1 == SVNNodeKind.DIR && childKind2 == SVNNodeKind.DIR) {
                if (depth == SVNDepth.IMMEDIATES) {
                    doArbitraryDirsDiff(childAbsPath1, childAbsPath2, diffWalker.root1AbsPath, diffWalker.root2AbsPath, SVNDepth.EMPTY, context, callback, canceller);
                } else {
                    continue;
                }
            }
            if (childKind1 == SVNNodeKind.FILE && (childKind2 == SVNNodeKind.DIR || childKind2 == SVNNodeKind.NONE)) {
                doArbitraryFilesDiff(childAbsPath1, null, SVNFileUtil.createFilePath(childRelPath, name), false, true, null, context, callback, canceller);
            }
            if (childKind2 == SVNNodeKind.FILE && (childKind1 == SVNNodeKind.DIR || childKind1 == SVNNodeKind.NONE)) {
                SVNProperties originalProps;
                try {
                    originalProps = context.getActualProps(childAbsPath1);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND && e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                        throw e;
                    }
                    originalProps = new SVNProperties();
                }
                doArbitraryFilesDiff(null, childAbsPath2, SVNFileUtil.createFilePath(childRelPath, name), true, false, originalProps, context, callback, canceller);
            }
            if (childKind1 == SVNNodeKind.FILE && childKind2 == SVNNodeKind.FILE) {
                doArbitraryFilesDiff(childAbsPath1, childAbsPath2, SVNFileUtil.createFilePath(childRelPath, name), false, false, null, context, callback, canceller);
            }

            if (depth.compareTo(SVNDepth.FILES) > 0 && childKind2 == SVNNodeKind.DIR && (childKind1 == SVNNodeKind.FILE || childKind1 == SVNNodeKind.NONE)) {
                doArbitraryDirsDiff(childAbsPath1, childAbsPath2, diffWalker.root1AbsPath, diffWalker.root2AbsPath, depth.compareTo(SVNDepth.IMMEDIATES) <= 0 ? SVNDepth.EMPTY : SVNDepth.INFINITY, context, callback, canceller);
            }
        }
    }

    private static void walkDirectory(File localAbsPath, ArbitraryDiffWalker diffWalker, SVNWCContext context, ISvnDiffCallback callback, ISVNCanceller canceller) throws SVNException {
        visit(localAbsPath, SVNFileType.DIRECTORY, diffWalker, context, callback, canceller);

        File[] children = SVNFileListUtil.listFiles(localAbsPath);
        if (children != null) {
            for (File child : children) {
                SVNFileType type = SVNFileType.getType(child);
                if (type == SVNFileType.DIRECTORY) {
                    walkDirectory(child, diffWalker, context, callback, canceller);
                } else if (type == SVNFileType.FILE || type == SVNFileType.SYMLINK) {
                    visit(child, type, diffWalker, context, callback, canceller);
                }
            }
        }
    }

    private static void visit(File localAbsPath, SVNFileType type, ArbitraryDiffWalker diffWalker, SVNWCContext context, ISvnDiffCallback callback, ISVNCanceller canceller) throws SVNException {
        if (canceller != null) {
            canceller.checkCancelled();
        }
        if (type != SVNFileType.DIRECTORY) {
            return;
        }
        arbitraryDiffThisDir(diffWalker, localAbsPath, SVNDepth.INFINITY, context, callback, canceller);
    }

    private static class ArbitraryDiffWalker {
        private File root1AbsPath;
        private File root2AbsPath;
        private boolean recursingWithinAddedSubtree;
        private boolean recursingWithinAdmDir;
        private File admDirAbsPath;
    }
}
