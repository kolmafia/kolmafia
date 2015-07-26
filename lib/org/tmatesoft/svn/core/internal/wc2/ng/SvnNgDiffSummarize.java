package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.io.*;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnDiffStatus;
import org.tmatesoft.svn.core.wc2.SvnDiffSummarize;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;

public class SvnNgDiffSummarize extends SvnNgOperationRunner<SvnDiffStatus, SvnDiffSummarize> {

    private SvnNgRepositoryAccess repositoryAccess;

    @Override
    public boolean isApplicable(SvnDiffSummarize operation, SvnWcGeneration wcGeneration) throws SVNException {
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
    protected SvnDiffStatus run(SVNWCContext context) throws SVNException {
        final SvnTarget source = getOperation().getSource();
        final SvnTarget firstSource = getOperation().getFirstSource();
        final SvnTarget secondSource = getOperation().getSecondSource();

        final ISVNDiffStatusHandler handler = createHandlerForReceiver(getOperation());
        final SVNDepth depth = getOperation().getDepth();
        final boolean useAncestry = !getOperation().isIgnoreAncestry();

        if (source != null) {
            doDiff(source, getOperation().getStartRevision(), source, getOperation().getEndRevision(), source.getPegRevision(), depth, useAncestry, handler);
        } else {
            doDiff(firstSource, firstSource.getResolvedPegRevision(), secondSource, secondSource.getResolvedPegRevision(), SVNRevision.UNDEFINED, depth, useAncestry, handler);
        }
        return null;
    }


    private void doDiff(SvnTarget target1, SVNRevision revision1, SvnTarget target2, SVNRevision revision2, SVNRevision pegRevision, SVNDepth depth, boolean useAncestry, ISVNDiffStatusHandler handler) throws SVNException {
        if ((revision1 == SVNRevision.UNDEFINED) || (revision2 == SVNRevision.UNDEFINED)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Not all required revisions are specified");
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        boolean isLocalRev1 = (revision1 == SVNRevision.BASE) || (revision1 == SVNRevision.WORKING);
        boolean isLocalRev2 = (revision2 == SVNRevision.BASE) || (revision2 == SVNRevision.WORKING);

        if (pegRevision != SVNRevision.UNDEFINED && isLocalRev1 && isLocalRev2) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "At least one revision must be something other than BASE or WORKING when diffing a URL");
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        boolean isRepos1 = !isLocalRev1 || target1.isURL();
        boolean isRepos2 = !isLocalRev2 || target2.isURL();

        if (isRepos1) {
            if (isRepos2) {
                doDiffReposRepos(target1.getURL(), target1.getFile(), revision1,
                        target2.getURL(), target2.getFile(), revision2,
                        pegRevision, depth, useAncestry, handler);
            } else {
                doDiffReposWC(target1, revision1, target2, revision2, pegRevision, false, depth, useAncestry, handler);
            }
        } else {
            if (isRepos2) {
                doDiffReposWC(target2, revision2, target1, revision1, pegRevision, true, depth, useAncestry, handler);
            } else {
                if (revision1 == SVNRevision.WORKING && revision2 == SVNRevision.WORKING) {
                    File path1 = target1.getFile();
                    File path2 = target2.getFile();

                    SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path1));
                    String target;
                    SvnTarget baseTarget;
                    if (kind == SVNNodeKind.DIR) {
                        target = "";
                        baseTarget = target1;
                    } else {
                        target = SVNFileUtil.getFileName(path1);
                        baseTarget = SvnTarget.fromFile(SVNFileUtil.getParentFile(target1.getFile()), target1.getPegRevision());
                    }
                    SvnNgDiffUtil.doArbitraryNodesDiff(target1, target2, depth, getWcContext(), new SvnDiffSummarizeCallback(kind == SVNNodeKind.DIR ? path1 : SVNFileUtil.getParentFile(path1), false, getRepositoryAccess().getTargetURL(baseTarget), baseTarget.getFile(), handler), getOperation().getEventHandler());
                } else {
                    doDiffWCWC(target1, revision1, target2, revision2, depth, useAncestry, handler);
                }
            }
        }
    }

    private void doDiffURL(SVNURL url, File path, SVNRevision startRevision, SVNRevision endRevision, SVNRevision pegRevision,
                           SVNDepth depth, boolean useAncestry, ISVNDiffStatusHandler handler) throws SVNException {
        if (handler == null) {
            return;
        }
        doDiffReposRepos(url, path, startRevision,
                url, path, endRevision,
                pegRevision, depth, useAncestry, handler);
    }

    private void doDiffReposRepos(SVNURL url1, File path1, SVNRevision revision1,
                                  SVNURL url2, File path2, SVNRevision revision2,
                                  SVNRevision pegRevision, SVNDepth depth, boolean useAncestry,
                                  ISVNDiffStatusHandler handler) throws SVNException {

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
        SVNRepository repository1 = createRepository(url1, null, true);
        SVNRepository repository2 = createRepository(url2, null, false);
        long rev1 = getRevisionNumber(revision1, repository1, url1);
        long rev2 = -1;
        SVNNodeKind kind1 = null;
        SVNNodeKind kind2 = null;

        SVNURL anchor1 = url1;
        SVNURL anchor2 = url2;
        String target1 = "";
        String target2 = "";

        try {
            rev2 = getRevisionNumber(revision2, repository2, url2);
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

        if (basePath == null) {
            basePath = nonDir ? new File(SVNPathUtil.tail(url1.toDecodedString())).getAbsoluteFile() : new File("").getAbsoluteFile();
        }

        ISvnDiffCallback oldCallback = new SvnDiffSummarizeCallback(path1 != null ? SVNFileUtil.createFilePath(path1.getParentFile(), target1) : SVNFileUtil.createFilePath(new File("").getAbsolutePath(), target1), false, anchor1, nonDir ? basePath.getParentFile() : basePath, handler);
        ISvnDiffCallback2 callback = new SvnDiffCallbackWrapper(oldCallback, true, nonDir ? basePath.getParentFile() : basePath);

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

        repository1.setLocation(anchor1, true);
        repository2.setLocation(anchor1, true);
        SvnNgRemoteDiffEditor2 editor = null;
        try {
            editor = new SvnNgRemoteDiffEditor2(rev1, false, repository2, callback);
            final long finalRev1 = rev1;
            ISVNReporterBaton reporter = new ISVNReporterBaton() {

                public void report(ISVNReporter reporter) throws SVNException {
                    reporter.setPath("", null, finalRev1, SVNDepth.INFINITY, false);
                    reporter.finishReport();
                }
            };
            repository1.diff(url2, rev2, rev1, target1, !useAncestry, depth, false, reporter, SVNCancellableEditor.newInstance(editor, this, getDebugLog()));
        } finally {
            repository2.closeSession();
            if (editor != null) {
                editor.cleanup();
            }
        }
    }

    private void doDiffReposWC(SvnTarget target1, SVNRevision revision1,
                               SvnTarget target2, SVNRevision revision2,
                               SVNRevision pegRevision, boolean reverse, SVNDepth depth, boolean useAncestry,
                               ISVNDiffStatusHandler handler) throws SVNException {//TODO: changelists?

        SvnNgDiffUtil.doDiffSummarizeReposWC(target1, revision1, pegRevision, target2, revision2, reverse, getRepositoryAccess(), getWcContext(), false, depth, useAncestry, getOperation().getApplicableChangelists(), false, null, handler, this);
    }

    private void doDiffWCWC(SvnTarget target1, SVNRevision revision1,
                            SvnTarget target2, SVNRevision revision2,
                            SVNDepth depth, boolean useAncestry, ISVNDiffStatusHandler handler) throws SVNException {
        assert !target1.isURL();
        assert !target2.isURL();

        File path1 = target1.getFile();
        File path2 = target2.getFile();

        if (!path1.equals(path2) || (!(revision1 == SVNRevision.BASE && revision2 == SVNRevision.WORKING))) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "Summarized diffs are only supported between a path's text-base and its working files at this time");
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path1));

        String targetString1 = (kind == SVNNodeKind.DIR) ? "" : SVNFileUtil.getFileName(path1);
        File basePath = (kind == SVNNodeKind.DIR) ? path1 : SVNFileUtil.getParentFile(path1);

        SVNURL baseUrl = kind == SVNNodeKind.DIR ? getRepositoryAccess().getTargetURL(target1) : getRepositoryAccess().getTargetURL(SvnTarget.fromFile(SVNFileUtil.getParentFile(target1.getFile())));
        ISvnDiffCallback callback = new SvnDiffSummarizeCallback(path1, false, baseUrl, basePath, handler);

        SvnNgDiffUtil.doDiffWCWC(path1, getRepositoryAccess(), getWcContext(), depth, useAncestry, getOperation().getApplicableChangelists(), false, false, null, callback, getOperation().getEventHandler());
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
        SVNURL sessionUrl = repository.getLocation();
        boolean equal = sessionUrl.equals(url);
        if (!equal) {
            repository.setLocation(url, true);
        }
        SVNNodeKind kind = repository.checkPath("", revision);
        if (kind == SVNNodeKind.NONE) {
            if (revision == otherRevision) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "Diff target ''{0}'' was not found in the repository at revision ''{1}''", url, revision);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            } else {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "Diff target ''{0}'' was not found in the repository at revision ''{1}'' or ''{2}''", url, revision, otherRevision);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }
        if (!equal) {
            repository.setLocation(url, true);
        }
    }

    private ISVNDebugLog getDebugLog() {
        return SVNDebugLog.getDefaultLog();
    }

    private long getRevisionNumber(SVNRevision revision1, SVNRepository repository1, SVNURL url1) throws SVNException {
        final Structure<SvnRepositoryAccess.RevisionsPair> revisionNumber = getRepositoryAccess().getRevisionNumber(repository1, SvnTarget.fromURL(url1, revision1), revision1, null);
        return revisionNumber.lng(SvnRepositoryAccess.RevisionsPair.revNumber);
    }

    private SVNURL getURL(File path1) throws SVNException {
        return getRepositoryAccess().getURLFromPath(SvnTarget.fromFile(path1), SVNRevision.UNDEFINED, null).<SVNURL>get(SvnRepositoryAccess.UrlInfo.url);
    }

    protected SVNRepository createRepository(SVNURL url, File path, boolean mayReuse) throws SVNException {
        return getRepositoryAccess().createRepository(url, null, mayReuse);
    }

    private static ISVNDiffStatusHandler createHandlerForReceiver(final ISvnObjectReceiver<SvnDiffStatus> receiver) {
        return new ISVNDiffStatusHandler() {
            public void handleDiffStatus(SVNDiffStatus diffStatus) throws SVNException {
                if (receiver != null) {
                    final SvnTarget target = diffStatus.getURL() != null ?
                            SvnTarget.fromURL(diffStatus.getURL()) :
                            SvnTarget.fromFile(diffStatus.getFile());
                    receiver.receive(target, SvnCodec.diffStatus(diffStatus));
                }
            }
        };
    }

    protected SvnNgRepositoryAccess getRepositoryAccess() throws SVNException {
        if (repositoryAccess == null) {
            repositoryAccess = new SvnNgRepositoryAccess(getOperation(), getWcContext());
        }
        return repositoryAccess;
    }
}
