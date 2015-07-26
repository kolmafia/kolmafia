package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc17.SVNAmbientDepthFilterEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNReporter17;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNCapability;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgDiff extends SvnNgOperationRunner<Void, SvnDiff> {

    @Override
    public SvnWcGeneration getWcGeneration() {
        return SvnWcGeneration.NOT_DETECTED;
    }

    @Override
    public boolean isApplicable(SvnDiff operation, SvnWcGeneration wcGeneration) throws SVNException {
        if (operation.getSource() != null) {
            if (operation.getSource().isFile() && wcGeneration != SvnWcGeneration.V17) {
                return false;
            }

            return true;
        } else {
            if (operation.getFirstSource().isFile() && wcGeneration != SvnWcGeneration.V17) {
                return false;
            }

            if (operation.getSecondSource().isFile() && wcGeneration != SvnWcGeneration.V17) {
                return false;
            }

            return true;
        }
    }

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        if (isPeggedDiff()) {
            SvnTarget target = getOperation().getSource();
            SVNRevision revision1 = getOperation().getStartRevision();
            SVNRevision revision2 = getOperation().getEndRevision();
            SVNRevision pegRevision = target.getPegRevision();

            doDiff(target, revision1, pegRevision, target, revision2);
        } else {
            SvnTarget target1 = getOperation().getFirstSource();
            SvnTarget target2 = getOperation().getSecondSource();
            SVNRevision revision1 = target1.getPegRevision();
            SVNRevision revision2 = target2.getPegRevision();
            SVNRevision pegRevision = SVNRevision.UNDEFINED;

            doDiff(target1, revision1, pegRevision, target2, revision2);
        }

        return null;
    }

    private void doDiff(SvnTarget target1, SVNRevision revision1, SVNRevision pegRevision, SvnTarget target2, SVNRevision revision2) throws SVNException {
        if (revision1 == SVNRevision.UNDEFINED || revision2 == SVNRevision.UNDEFINED) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Not all required revisions are specified");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }

        final boolean isLocalRev1 = revision1 == SVNRevision.BASE || revision1 == SVNRevision.WORKING;
        final boolean isLocalRev2 = revision2 == SVNRevision.BASE || revision2 == SVNRevision.WORKING;

        boolean isRepos1;
        boolean isRepos2;

        if (pegRevision != SVNRevision.UNDEFINED) {
            if (isLocalRev1 && isLocalRev2) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "At least one revision must be non-local for a pegged diff");
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }

            isRepos1 = !isLocalRev1;
            isRepos2 = !isLocalRev2;
        } else {
            isRepos1 = !isLocalRev1 || target1.isURL();
            isRepos2 = !isLocalRev2 || target2.isURL();
        }

        if (isRepos1) {
            if (isRepos2) {
                doDiffReposRepos(target1, revision1, pegRevision, target2, revision2);
            } else {
                doDiffReposWC(target1, revision1, pegRevision, target2, revision2, false);
            }
        } else {
            if (isRepos2) {
                doDiffReposWC(target2, revision2, pegRevision, target1, revision1, true);
            } else {
                if (revision1 == SVNRevision.WORKING && revision2 == SVNRevision.WORKING) {
                    ISvnDiffGenerator generator = getDiffGenerator();
                    generator.setOriginalTargets(target1, target2);
                    generator.setAnchors(target1, target2);
                    SvnNgDiffUtil.doArbitraryNodesDiff(target1, target2, getOperation().getDepth(), getWcContext(), createDiffCallback(generator, false, -1, -1), getOperation().getEventHandler());
                } else {
                    doDiffWCWC(target1, revision1, target2, revision2);
                }
            }
        }
    }

    private void doDiffReposRepos(SvnTarget svnTarget1, SVNRevision revision1, SVNRevision pegRevision, SvnTarget svnTarget2, SVNRevision revision2) throws SVNException {
        SVNURL url1 = svnTarget1.getURL();
        SVNURL url2 = svnTarget2.getURL();
        File path1 = svnTarget1.getFile();
        File path2 = svnTarget2.getFile();

        if (revision1 == SVNRevision.UNDEFINED || revision2 == SVNRevision.UNDEFINED) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Not all revisions are specified");
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        boolean isLocalRev1 = revision1 == SVNRevision.BASE || revision1 == SVNRevision.WORKING;
        boolean isLocalRev2 = revision2 == SVNRevision.BASE || revision2 == SVNRevision.WORKING;
        boolean isRepos1;
        boolean isRepos2;

        if (pegRevision != SVNRevision.UNDEFINED) {
            if (isLocalRev1 && isLocalRev2) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "At least one revision must be non-local for a pegged diff");
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            isRepos1 = !isLocalRev1;
            isRepos2 = !isLocalRev2;
        } else {
            isRepos1 = !isLocalRev1 || url1 != null;
            isRepos2 = !isLocalRev2 || url2 != null;
        }

        if (!isRepos1 || !isRepos2) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Summarizing diff can only compare repository to repository");
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        File basePath = null;
        if (path1 != null) {
            basePath = path1;
        }
        if (path2 != null) {
            basePath = path2;
        }
        if (pegRevision.isValid()) {
            url2 = resolvePeggedDiffTargetUrl(url2, path2, pegRevision, revision2);
            url1 = resolvePeggedDiffTargetUrl(url1, path1, pegRevision, revision1);

            if (url2 != null && url1 == null) {
                url1 = url2;
            }
            if (url1 != null && url2 == null) {
                url2 = url1;
            }

        } else {
            url1 = url1 == null ? getURL(path1) : url1;
            url2 = url2 == null ? getURL(path2) : url2;
        }
        SVNRepository repository1 = getRepositoryAccess().createRepository(url1, null, true);
        SVNRepository repository2 = getRepositoryAccess().createRepository(url2, null, false);
        long rev1 = getRevisionNumber(revision1, repository1, svnTarget1);
        long rev2 = -1;
        SVNNodeKind kind1 = null;
        SVNNodeKind kind2 = null;

        SVNURL anchor1 = url1;
        SVNURL anchor2 = url2;
        String target1 = "";
        String target2 = "";

        try {
            rev2 = getRevisionNumber(revision2, repository2, svnTarget2);
            kind1 = repository1.checkPath("", rev1);
            kind2 = repository2.checkPath("", rev2);

            if (kind1 == SVNNodeKind.NONE && kind2 == SVNNodeKind.NONE) {
                if (url1.equals(url2)) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "Diff target ''{0}'' was not found in the repository at revisions ''{1}'' and ''{2}''", url1, rev1, rev2);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                } else {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "Diff targets ''{0}'' and ''{1}'' were not found in the repository at revisions ''{2}'' and ''{3}''");
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            } else if (kind1 == SVNNodeKind.NONE) {
                checkDiffTargetExists(url1, rev2, rev1, repository1);
            } else if (kind2 == SVNNodeKind.NONE) {
                checkDiffTargetExists(url2, rev1, rev2, repository2);
            }

            SVNURL repositoryRoot = repository1.getRepositoryRoot(true);
            if (!url1.equals(repositoryRoot) && !url2.equals(repositoryRoot)) {
                anchor1 = url1.removePathTail();
                anchor2 = url2.removePathTail();

                target1 = SVNPathUtil.tail(url1.toDecodedString());
                target2 = SVNPathUtil.tail(url2.toDecodedString());
            }
        } finally {
            repository2.closeSession();
        }

        boolean nonDir = kind1 != SVNNodeKind.DIR || kind2 != SVNNodeKind.DIR;

        SVNURL repositoryRoot = null;

        ISvnDiffGenerator generator = getDiffGenerator();
        generator.setOriginalTargets(SvnTarget.fromURL(url1), SvnTarget.fromURL(url2));
        generator.setAnchors(SvnTarget.fromURL(kind1 == SVNNodeKind.FILE ? anchor1 : url1), SvnTarget.fromURL(kind2 == SVNNodeKind.FILE ? anchor2 : url2));
        if (getOperation().isUseGitDiffFormat()) {
            if (repositoryRoot == null) {
                repositoryRoot = repository1.getRepositoryRoot(true);
            }
            generator.setRepositoryRoot(SvnTarget.fromURL(repositoryRoot));
        }

        SvnDiffCallback oldCallback = createDiffCallback(generator, false, rev1, rev2);
        ISvnDiffCallback2 callback = new SvnDiffCallbackWrapper(oldCallback, true, basePath != null ? (nonDir ? basePath.getParentFile() : basePath) : new File("").getAbsoluteFile());

        if (kind2 == SVNNodeKind.NONE) {
            SVNURL tmpUrl;

            tmpUrl = url1;
            url1 = url2;
            url2 = tmpUrl;

            long tmpRev;
            tmpRev = rev1;
            rev1 = rev2;
            rev2 = tmpRev;

            tmpUrl = anchor1;
            anchor1 = anchor2;
            anchor2 = tmpUrl;

            String tmpTarget;
            tmpTarget = target1;
            target1 = target2;
            target2 = tmpTarget;

            callback = new SvnReverseOrderDiffCallback(callback, null);
        }

        if (kind1 != SVNNodeKind.FILE && kind2 != SVNNodeKind.FILE && target1.length() > 0) {
            callback = new SvnFilterDiffCallback(SVNFileUtil.createFilePath(target1), callback);
        }

        repository1.setLocation(anchor1, true);
        repository2.setLocation(anchor1, true);
        SvnNgRemoteDiffEditor2 editor = null;
        try {
            editor = new SvnNgRemoteDiffEditor2(rev1, true, repository2, callback);
            final long finalRev1 = rev1;
            ISVNReporterBaton reporter = new ISVNReporterBaton() {

                public void report(ISVNReporter reporter) throws SVNException {
                    reporter.setPath("", null, finalRev1, SVNDepth.INFINITY, false);
                    reporter.finishReport();
                }
            };
            repository1.diff(url2, rev2, rev1, target1, getOperation().isIgnoreAncestry(), getOperation().getDepth(), true, reporter, SVNCancellableEditor.newInstance(editor, this, SVNDebugLog.getDefaultLog()));
        } finally {
            repository2.closeSession();
            if (editor != null) {
                editor.cleanup();
            }
        }
    }

    private void doDiffReposWC(SvnTarget target1, SVNRevision revision1, SVNRevision pegRevision, SvnTarget target2, SVNRevision revision2, boolean reverse) throws SVNException {
        assert !target2.isURL();

        SVNURL url1 = getRepositoryAccess().getTargetURL(target1);
        String target = getWcContext().getActualTarget(target2.getFile());
        File anchor;
        if (target == null || target.length() == 0) {
            anchor = target2.getFile();
        } else {
            anchor = SVNFileUtil.getParentFile(target2.getFile());
        }
        SVNURL anchorUrl = getWcContext().getNodeUrl(anchor);

        if (anchorUrl == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "Directory ''{0}'' has no URL", anchor);
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }

        final ISvnDiffGenerator generator = getDiffGenerator();

        if (pegRevision != SVNRevision.UNDEFINED) {
            url1 = getRepositoryAccess().getLocations(null, target1, pegRevision, revision1, SVNRevision.UNDEFINED).get(SvnRepositoryAccess.LocationsInfo.startUrl);

            if (!reverse) {
                generator.setOriginalTargets(SvnTarget.fromURL(url1), SvnTarget.fromURL(anchorUrl.appendPath(target, false)));
                generator.setAnchors(SvnTarget.fromURL(url1), SvnTarget.fromURL(anchorUrl.appendPath(target, false)));
            } else {
                generator.setOriginalTargets(SvnTarget.fromURL(anchorUrl.appendPath(target, false)), SvnTarget.fromURL(url1));
                generator.setAnchors(SvnTarget.fromURL(anchorUrl.appendPath(target, false)), SvnTarget.fromURL(url1));
            }
        } else {
            if (!reverse) {
                generator.setOriginalTargets(target1, target2);
                generator.setAnchors(target1, target2);
            } else {
                generator.setOriginalTargets(target2, target1);
                generator.setAnchors(target2, target1);
            }
        }

        SVNRepository repository2 = getRepositoryAccess().createRepository(anchorUrl, null, true);

        if (getOperation().isUseGitDiffFormat()) {
            File wcRoot = getWcContext().getDb().getWCRoot(anchor);
            generator.setRepositoryRoot(SvnTarget.fromFile(wcRoot));
        }

        boolean serverSupportsDepth = repository2.hasCapability(SVNCapability.DEPTH);

        long revisionNumber1 = getRepositoryAccess().getRevisionNumber(repository2, url1.equals(target1.getURL()) ? null : target1, revision1, null).lng(SvnRepositoryAccess.RevisionsPair.revNumber);

        SvnDiffCallback callback = createDiffCallback(generator, reverse, revisionNumber1, -1);

        SVNReporter17 reporter = new SVNReporter17(target2.getFile(), getWcContext(), false, !serverSupportsDepth, getOperation().getDepth(), false, false, true, false, SVNDebugLog.getDefaultLog());
        boolean revisionIsBase = isRevisionBase(revision2);
        SvnDiffEditor svnDiffEditor = new SvnDiffEditor(anchor, target, callback, getOperation().getDepth(), getWcContext(), reverse, revisionIsBase, getOperation().isShowCopiesAsAdds(), getOperation().isIgnoreAncestry(), getOperation().getApplicableChangelists(), getOperation().isUseGitDiffFormat(), this);

        ISVNUpdateEditor updateEditor = svnDiffEditor;
        if (!serverSupportsDepth && getOperation().getDepth() == SVNDepth.UNKNOWN) {
            updateEditor = new SVNAmbientDepthFilterEditor17(updateEditor, getWcContext(), anchor, target, revisionIsBase);
        }
        ISVNEditor editor = SVNCancellableEditor.newInstance(updateEditor, this, SVNDebugLog.getDefaultLog());
        try{
            repository2.diff(url1, revisionNumber1, revisionNumber1, target, getOperation().isIgnoreAncestry(), getDiffDepth(getOperation().getDepth()), true, reporter, editor);
        } finally {
            svnDiffEditor.cleanup();
        }
    }

    private void doDiffWCWC(SvnTarget target1, SVNRevision revision1, SvnTarget target2, SVNRevision revision2) throws SVNException {
        assert (!target1.isURL());
        assert (!target2.isURL());

        File path1 = target1.getFile();
        File path2 = target2.getFile();

        if (!path1.equals(path2) || (!(revision1 == SVNRevision.BASE && revision2 == SVNRevision.WORKING))) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "Summarized diffs are only supported between a path's text-base and its working files at this time");
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        long revisionNumber1;
        try {
            revisionNumber1 = getRepositoryAccess().getRevisionNumber(null, target1, revision1, null).lng(SvnRepositoryAccess.RevisionsPair.revNumber);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.CLIENT_BAD_REVISION) {
                revisionNumber1 = 0;
            } else {
                throw e;
            }
        }

        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path1));

        String targetString1 = (kind == SVNNodeKind.DIR) ? "" : SVNFileUtil.getFileName(path1);

        final ISvnDiffGenerator generator = getDiffGenerator();
        generator.setOriginalTargets(target1, target2);
        generator.setAnchors(target1, target2);

        if (getOperation().isUseGitDiffFormat()) {
            generator.setRepositoryRoot(SvnTarget.fromFile(getWcContext().getDb().getWCRoot(target1.getFile())));
        }

        final SvnDiffCallback callback = createDiffCallback(generator, false, revisionNumber1, -1);

        SvnNgDiffUtil.doDiffWCWC(path1, getRepositoryAccess(), getWcContext(), getOperation().getDepth(), !getOperation().isIgnoreAncestry(), getOperation().getApplicableChangelists(), getOperation().isShowCopiesAsAdds(), getOperation().isUseGitDiffFormat(), generator, callback, getOperation().getEventHandler());
    }

    private void doDiffWC(File localAbspath, ISvnDiffCallback callback) throws SVNException {
        final boolean getAll;
        //noinspection RedundantIfStatement
        if (getOperation().isShowCopiesAsAdds() || getOperation().isUseGitDiffFormat()) {
            getAll = true;
        } else {
            getAll = false;
        }

        final boolean diffIgnored = false;

        final SvnDiffStatusReceiver statusHandler = new SvnDiffStatusReceiver(getWcContext(), localAbspath, getWcContext().getDb(), callback, getOperation().isIgnoreAncestry(), getOperation().isShowCopiesAsAdds(), getOperation().isUseGitDiffFormat(), getOperation().getApplicableChangelists());
        final SVNStatusEditor17 statusEditor = new SVNStatusEditor17(
                localAbspath,
                getWcContext(),
                getOperation().getOptions(),
                !diffIgnored,
                getAll,
                getOperation().getDepth(),
                statusHandler);
        statusEditor.walkStatus(localAbspath, getOperation().getDepth(), getAll, !diffIgnored, false, getOperation().getApplicableChangelists());
    }

    private SVNURL resolvePeggedDiffTargetUrl(SVNURL url, File path, SVNRevision pegRevision, SVNRevision revision) throws SVNException {
        try {
            final Structure<SvnRepositoryAccess.LocationsInfo> locationsInfo = getRepositoryAccess().getLocations(null,
                    url == null ? SvnTarget.fromFile(path) : SvnTarget.fromURL(url),
                    pegRevision, revision, SVNRevision.UNDEFINED);
            return locationsInfo.get(SvnRepositoryAccess.LocationsInfo.startUrl);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.CLIENT_UNRELATED_RESOURCES ||
                    e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private void checkDiffTargetExists(SVNURL url, long revision, long otherRevision, SVNRepository repository) throws SVNException {
        repository.setLocation(url, false);
        SVNNodeKind kind = repository.checkPath("", revision);
        if (kind == SVNNodeKind.NONE) {
            if (revision == otherRevision) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND,
                        "Diff target ''{0}'' was not found in the " +
                                "repository at revision ''{1}''", new Object[]{
                        url, new Long(revision)
                });
                SVNErrorManager.error(err, SVNLogType.WC);
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND,
                        "Diff target ''{0}'' was not found in the " +
                                "repository at revisions ''{1}'' and ''{2}''", new Object[]{
                        url, new Long(revision), new Long(otherRevision)
                });
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
    }

    private SVNURL getURL(File path1) throws SVNException {
        return getRepositoryAccess().getURLFromPath(SvnTarget.fromFile(path1), SVNRevision.UNDEFINED, null).<SVNURL>get(SvnRepositoryAccess.UrlInfo.url);
    }

    private long getRevisionNumber(SVNRevision revision, SVNRepository repository, SvnTarget target) throws SVNException {
        final Structure<SvnRepositoryAccess.RevisionsPair> revisionNumber = getRepositoryAccess().getRevisionNumber(repository, target, revision, null);
        return revisionNumber.lng(SvnRepositoryAccess.RevisionsPair.revNumber);
    }

    private boolean isPeggedDiff() {
        return getOperation().getSource() != null;
    }

    private boolean isRevisionBase(SVNRevision revision2) {
        return revision2 == SVNRevision.BASE;
    }

    private SVNDepth getDiffDepth(SVNDepth depth) {
        return depth != SVNDepth.INFINITY ? depth : SVNDepth.UNKNOWN;
    }

    private SvnDiffCallback createDiffCallback(ISvnDiffGenerator generator, boolean reverse, long revisionNumber1, long revisionNumber2) {
        return new SvnDiffCallback(generator, reverse ? revisionNumber2 : revisionNumber1, reverse ? revisionNumber1 : revisionNumber2, getOperation().isShowCopiesAsAdds(), false, getOperation().getOutput());
    }

    private ISvnDiffGenerator getDiffGenerator() {
        ISvnDiffGenerator diffGenerator = getOperation().getDiffGenerator();
        if (diffGenerator == null) {
            diffGenerator = new SvnDiffGenerator();
        }
        diffGenerator.setUseGitFormat(getOperation().isUseGitDiffFormat());
        if (getOperation().getRelativeToDirectory() != null) {
            if (diffGenerator instanceof SvnDiffGenerator) {
                ((SvnDiffGenerator)diffGenerator).setRelativeToTarget(SvnTarget.fromFile(getOperation().getRelativeToDirectory()));
            } else {
                diffGenerator.setBaseTarget(SvnTarget.fromFile(getOperation().getRelativeToDirectory()));
            }
        }
        return diffGenerator;
    }
}
