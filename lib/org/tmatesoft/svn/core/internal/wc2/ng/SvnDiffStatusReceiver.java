package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnDiffStatusReceiver implements ISvnObjectReceiver<SvnStatus> {

    private final SVNWCContext context;
    private final File anchor;
    private final ISVNWCDb db;
    private final ISvnDiffCallback callback;
    private final boolean showCopiesAsAdds;
    private final boolean gitFormat;
    private final boolean ignoreAncestry;
    private final Collection<String> changelists;

    public SvnDiffStatusReceiver(SVNWCContext context, File anchor,
                                 ISVNWCDb db,
                                 ISvnDiffCallback callback,
                                 boolean ignoreAncestry,
                                 boolean showCopiesAsAdds,
                                 boolean gitFormat, Collection<String> changelists) {
        this.context = context;
        this.anchor = anchor;
        this.db = db;
        this.callback = callback;
        this.ignoreAncestry = ignoreAncestry;
        this.showCopiesAsAdds = showCopiesAsAdds;
        this.gitFormat = gitFormat;
        this.changelists = changelists;
    }

    public SVNWCContext getContext() {
        return context;
    }

    public File getAnchor() {
        return anchor;
    }

    public ISVNWCDb getDb() {
        return db;
    }

    public ISvnDiffCallback getCallback() {
        return callback;
    }

    public boolean isIgnoreAncestry() {
        return ignoreAncestry;
    }

    public boolean isShowCopiesAsAdds() {
        return showCopiesAsAdds;
    }

    public boolean isGitFormat() {
        return gitFormat;
    }

    public void receive(SvnTarget target, SvnStatus status) throws SVNException {
        final SVNStatusType nodeStatus = status.getNodeStatus();
        if (nodeStatus == SVNStatusType.STATUS_UNVERSIONED) {
            return;
        } else if (nodeStatus == SVNStatusType.OBSTRUCTED || nodeStatus == SVNStatusType.MISSING) {
            return;
        }

        final File localAbspath = target.getFile();

        if (changelists != null && !context.matchesChangelist(localAbspath, changelists)) {
            return;
        }

        if (status.getKind() == SVNNodeKind.FILE) {

            if (status.getTextStatus() == SVNStatusType.STATUS_MODIFIED
                    || status.getPropertiesStatus() == SVNStatusType.STATUS_MODIFIED
                    || status.getNodeStatus() == SVNStatusType.STATUS_DELETED
                    || status.getNodeStatus() == SVNStatusType.STATUS_REPLACED
                    || (isShowCopiesAsAdds() || isGitFormat()) && status.isCopied()) {

                final String path = SVNPathUtil.getRelativePath(getAnchor().getAbsolutePath(), localAbspath.getAbsolutePath());
                fileDiff(localAbspath, path);
            }

        } else {
            //status.getKind() != SVNNodeKind.FILE
            if (status.getNodeStatus() == SVNStatusType.STATUS_DELETED
                    || status.getNodeStatus() == SVNStatusType.STATUS_REPLACED
                    || status.getPropertiesStatus() == SVNStatusType.STATUS_MODIFIED) {
                String path = SVNPathUtil.getRelativePath(getAnchor().getAbsolutePath(), localAbspath.getAbsolutePath());

                SVNProperties baseProps = getDb().readPristineProperties(localAbspath);
                if (baseProps == null) {
                    baseProps = new SVNProperties();
                }

                SVNProperties actualProps = getDb().readProperties(localAbspath);
                SVNProperties propChanges = SvnDiffEditor.computePropDiff(baseProps, actualProps);

                getCallback().dirPropsChanged(null, localAbspath, false, propChanges, baseProps);
            }
        }
    }

    private void fileDiff(File localAbspath, String path) throws SVNException {
        final Structure<StructureFields.NodeInfo> nodeInfo =
                getDb().readInfo(localAbspath,
                        StructureFields.NodeInfo.status,
                        StructureFields.NodeInfo.kind,
                        StructureFields.NodeInfo.revision,
                        StructureFields.NodeInfo.checksum,
                        StructureFields.NodeInfo.originalReposRelpath,
                        StructureFields.NodeInfo.opRoot,
                        StructureFields.NodeInfo.hadProps,
                        StructureFields.NodeInfo.propsMod,
                        StructureFields.NodeInfo.haveBase,
                        StructureFields.NodeInfo.haveMoreWork);
        ISVNWCDb.SVNWCDbStatus status = nodeInfo.get(StructureFields.NodeInfo.status);
        ISVNWCDb.SVNWCDbKind kind = nodeInfo.get(StructureFields.NodeInfo.kind);
        long revision = nodeInfo.lng(StructureFields.NodeInfo.revision);
        SvnChecksum checksum = nodeInfo.get(StructureFields.NodeInfo.checksum);
        File originalReposRelpath = nodeInfo.get(StructureFields.NodeInfo.originalReposRelpath);
        Object opRoot = nodeInfo.is(StructureFields.NodeInfo.opRoot);
        boolean hadProps = nodeInfo.is(StructureFields.NodeInfo.hadProps);
        boolean propsMod = nodeInfo.is(StructureFields.NodeInfo.propsMod);
        boolean haveBase = nodeInfo.is(StructureFields.NodeInfo.haveBase);
        boolean haveMoreWork = nodeInfo.is(StructureFields.NodeInfo.haveMoreWork);

        boolean replaced = false;
        boolean baseReplace = false;

        SvnChecksum baseChecksum = null;
        ISVNWCDb.SVNWCDbKind baseKind;
        ISVNWCDb.SVNWCDbStatus baseStatus;
        long baseRevision;

        File emptyFile;
        File pristineAbspath;

        if ((status == ISVNWCDb.SVNWCDbStatus.Added) && (haveBase || haveMoreWork)) {
            final ISVNWCDb.SVNWCDbNodeCheckReplaceData checkReplaceData =
                    getDb().nodeCheckReplace(localAbspath);
            replaced = checkReplaceData.replace;
            baseReplace = checkReplaceData.baseReplace;

            if (replaced && baseReplace) {
                ISVNWCDb.WCDbBaseInfo baseInfo = getDb().getBaseInfo(localAbspath,
                        ISVNWCDb.WCDbBaseInfo.BaseInfoField.status,
                        ISVNWCDb.WCDbBaseInfo.BaseInfoField.kind,
                        ISVNWCDb.WCDbBaseInfo.BaseInfoField.revision,
                        ISVNWCDb.WCDbBaseInfo.BaseInfoField.checksum);
                baseStatus = baseInfo.status;
                baseKind = baseInfo.kind;
                baseRevision = baseInfo.revision;
                baseChecksum = baseInfo.checksum;

                if (baseStatus != ISVNWCDb.SVNWCDbStatus.Normal || (baseKind != kind)) {
                    replaced = false;
                    baseReplace = false;
                }
            } else {
                replaced = false;
                baseReplace = false;
            }
        }

        if (status == ISVNWCDb.SVNWCDbStatus.Added) {
            ISVNWCDb.WCDbAdditionInfo wcDbAdditionInfo = getDb().scanAddition(localAbspath,
                    ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.status,
                    ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.originalReposRelPath);

            status = wcDbAdditionInfo.status;
            originalReposRelpath = wcDbAdditionInfo.originalReposRelPath;
        }

        //here we can create an empty file
        //but also we can create it on demand, that it better solution

        if (status == ISVNWCDb.SVNWCDbStatus.Deleted || (baseReplace && !isIgnoreAncestry())) {
            SvnChecksum delChecksum;
            SVNProperties delProps;
            File delTextAbspath;
            String delMimeType;

            if (baseReplace && !isIgnoreAncestry()) {
                delProps = getDb().getBaseProps(localAbspath);
                delChecksum = baseChecksum;
            } else {
                assert status == ISVNWCDb.SVNWCDbStatus.Deleted;
                delProps = getContext().getPristineProps(localAbspath);
                delChecksum = getDb().readPristineInfo(localAbspath).get(StructureFields.PristineInfo.checksum);
            }

            assert delChecksum != null;

            delTextAbspath = getDb().getPristinePath(localAbspath, delChecksum);
            if (delProps == null) {
                delProps = new SVNProperties();
            }

            delMimeType = delProps.getStringValue(SVNProperty.MIME_TYPE);

            getCallback().fileDeleted(null,
                    localAbspath,
                    delTextAbspath, getEmptyFile(),
                    delMimeType, null,
                    delProps);

            if (status == ISVNWCDb.SVNWCDbStatus.Deleted) {
                return;
            }
        }

        if (checksum != null) {
            pristineAbspath = getDb().getPristinePath(localAbspath, checksum);
        } else if (baseReplace && isIgnoreAncestry()) {
            pristineAbspath = getDb().getPristinePath(localAbspath, baseChecksum);
        } else {
            pristineAbspath = getEmptyFile();
        }


        if ((! baseReplace && status == ISVNWCDb.SVNWCDbStatus.Added) ||
                (baseReplace && !isIgnoreAncestry()) ||
                ((status == ISVNWCDb.SVNWCDbStatus.Copied ||
                status == ISVNWCDb.SVNWCDbStatus.MovedHere) &&
                (isShowCopiesAsAdds() || isGitFormat()))) {
            File translatedFile;
            SVNProperties pristineProps = new SVNProperties();
            SVNProperties actualProps;
            String actualMimeType;
            SVNProperties propChanges;

            actualProps = getContext().getActualProps(localAbspath);
            if (actualProps == null) {
                actualProps = new SVNProperties();
            }
            actualMimeType = actualProps.getStringValue(SVNProperty.MIME_TYPE);
            propChanges = SvnDiffEditor.computePropDiff(pristineProps, actualProps);

            translatedFile = getContext().getTranslatedFile(localAbspath, localAbspath,
                    true, false, false, false, false);

            getCallback().fileAdded(null, localAbspath,
                    (!isShowCopiesAsAdds() && isGitFormat() && status != ISVNWCDb.SVNWCDbStatus.Added) ?
                    pristineAbspath : getEmptyFile(),
                    translatedFile,
                    0, revision,
                    null,
                    actualMimeType,
                    originalReposRelpath,
                    -1, propChanges,
                    pristineProps);
        } else {
            File translatedFile = null;
            SVNProperties pristineProps;

            boolean modified = getContext().isTextModified(localAbspath, false);
            if (modified) {
                translatedFile = getContext().getTranslatedFile(localAbspath, localAbspath,
                        true, false, false, false, false);
            }

            if (baseReplace && isIgnoreAncestry()) {
                pristineProps = getDb().getBaseProps(localAbspath);
            } else {
                assert !replaced || status == ISVNWCDb.SVNWCDbStatus.Copied || status == ISVNWCDb.SVNWCDbStatus.MovedHere;
                pristineProps = getDb().readPristineProperties(localAbspath);
                if (pristineProps == null) {
                    pristineProps = new SVNProperties();
                }
            }

            String pristineMimeType = pristineProps.getStringValue(SVNProperty.MIME_TYPE);
            SVNProperties actualProps = getDb().readProperties(localAbspath);
            String actualMimeType = actualProps.getStringValue(SVNProperty.MIME_TYPE);

            SVNProperties propChanges = SvnDiffEditor.computePropDiff(pristineProps, actualProps);

            if (modified || !propChanges.isEmpty()) {
                getCallback().fileChanged(null, localAbspath, modified ? pristineAbspath : null,
                        translatedFile,
                        revision,
                        -1,
                        pristineMimeType,
                        actualMimeType,
                        propChanges,
                        pristineProps);
            }
        }
    }

    private File getEmptyFile() throws SVNException {
        return null;
    }
}
