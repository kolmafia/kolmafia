package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.SVNWCNodeReposInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.ISvnOperationOptionsProvider;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgRepositoryAccess extends SvnRepositoryAccess {

    public SvnNgRepositoryAccess(ISvnOperationOptionsProvider operationOptionsProvider, SVNWCContext context) throws SVNException {
        super(operationOptionsProvider, context);
    }

    @Override
    public Structure<RepositoryInfo> createRepositoryFor(SvnTarget target, SVNRevision revision, SVNRevision pegRevision, File baseDirectory) throws SVNException {
        SVNURL url = getTargetURL(target);
        if (url == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", target.getFile());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNRevision[] resolvedRevisions = resolveRevisions(pegRevision, revision, target.isURL(), true);
        SVNRevision pegRev = resolvedRevisions[0];
        SVNRevision startRev = resolvedRevisions[1];
        SVNRepository repository = createRepository(url, baseDirectory);
        if (target.isURL() && !url.equals(repository.getLocation())) {
            url = repository.getLocation();
        }
        Structure<LocationsInfo> locationsInfo = getLocations(repository, target, pegRev, startRev, SVNRevision.UNDEFINED);
        long rev = locationsInfo.lng(LocationsInfo.startRevision);
        url = locationsInfo.<SVNURL>get(LocationsInfo.startUrl);
        locationsInfo.release();
        
        repository.setLocation(url, false);
        
        if (rev < 0) {
            Structure<RevisionsPair> revs = getRevisionNumber(repository, target, SVNRevision.HEAD, null);
            rev = revs.lng(RevisionsPair.revNumber);
            revs.release();            
        }
        
        Structure<RepositoryInfo> result = Structure.obtain(RepositoryInfo.class);
        
        result.set(RepositoryInfo.revision, rev);
        result.set(RepositoryInfo.repository, repository);
        result.set(RepositoryInfo.url, url);
        
        return result;
    }
    
    public Structure<UrlInfo> getURLFromPath(SvnTarget path, SVNRevision revision, SVNRepository repository) throws SVNException {
        Structure<UrlInfo> urlInfo = Structure.obtain(UrlInfo.class);
        SVNURL url = null;
        if (revision == SVNRevision.WORKING) {
            Structure<NodeOriginInfo> nodeOrigin = getWCContext().getNodeOrigin(path.getFile(), false);
            urlInfo.set(UrlInfo.pegRevision, nodeOrigin.lng(NodeOriginInfo.revision));            
            if (nodeOrigin.hasValue(NodeOriginInfo.reposRelpath)) {
              url = SVNWCUtils.join(nodeOrigin.<SVNURL>get(NodeOriginInfo.reposRootUrl), nodeOrigin.<File>get(NodeOriginInfo.reposRelpath));  
            }
            if (url != null && nodeOrigin.hasValue(NodeOriginInfo.isCopy) && nodeOrigin.is(NodeOriginInfo.isCopy) && repository != null) {
                if (!url.equals(repository.getLocation())) {
                    urlInfo.set(UrlInfo.dropRepsitory, true);
                    repository = null;
                    
                }
            }
        }
        if (url == null) {
            url = getWCContext().getNodeUrl(path.getFile());
        }
        if (url == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", path.getFile());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        urlInfo.set(UrlInfo.url, url);
        return urlInfo;
    }
    
    @Override
    public Structure<RevisionsPair> getRevisionNumber(SVNRepository repository, SvnTarget path, SVNRevision revision, Structure<RevisionsPair> youngestRevision) throws SVNException {
        Structure<RevisionsPair> result = youngestRevision == null ? Structure.obtain(RevisionsPair.class) : youngestRevision;
        if (youngestRevision == null) {
            // initial value for the latest rev num
            result.set(RevisionsPair.youngestRevision, -1);
        }
        if (repository == null && (revision == SVNRevision.HEAD || revision.getDate() != null)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_RA_ACCESS_REQUIRED);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (revision == SVNRevision.UNDEFINED) {
            result.set(RevisionsPair.revNumber, -1);
        } else if (revision.getNumber() >= 0) {
            result.set(RevisionsPair.revNumber, revision.getNumber());            
        } else if (revision == SVNRevision.HEAD) {
            if (youngestRevision != null && youngestRevision.lng(RevisionsPair.youngestRevision) >= 0) {
                result.set(RevisionsPair.revNumber, youngestRevision.lng(RevisionsPair.youngestRevision));            
            } else {
                if (repository == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_RA_ACCESS_REQUIRED);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                long youngest = repository.getLatestRevision();
                result.set(RevisionsPair.revNumber, youngest);            
                result.set(RevisionsPair.youngestRevision, youngest);            
            }
        } else if (revision == SVNRevision.WORKING || revision == SVNRevision.BASE) {
            if (path == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_VERSIONED_PATH_REQUIRED);
                SVNErrorManager.error(err, SVNLogType.WC);
            } else if (!path.isFile()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "PREV, BASE or COMMITTTED revision keywords are invalid for URL");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            long revnum = -1;
            try {
                revnum = getWCContext().getNodeCommitBaseRev(path.getFile());
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "''{0}'' is not under version control", path.getFile());
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                throw e;
            }
            if (revnum < 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Path ''{0}'' has no committed revision", path.getFile());
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            result.set(RevisionsPair.revNumber, revnum);
        } else if (revision == SVNRevision.COMMITTED || revision == SVNRevision.PREVIOUS) {
            if (path == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_VERSIONED_PATH_REQUIRED);
                SVNErrorManager.error(err, SVNLogType.WC);
            } else if (!path.isFile()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "PREV, BASE or COMMITTTED revision keywords are invalid for URL");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            long revnum = -1;
            try {
                Structure<NodeInfo> info = getWCContext().getDb().readInfo(path.getFile(), NodeInfo.changedRev);
                revnum = info.lng(NodeInfo.changedRev);
                info.release();
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "''{0}'' is not under version control", path.getFile());
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                throw e;
            }
            if (revnum < 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Path ''{0}'' has no committed revision", path.getFile());
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (revision == SVNRevision.PREVIOUS) {
                revnum = revnum - 1;
            }
            result.set(RevisionsPair.revNumber, revnum);
        } else if (revision.getDate() != null) {
            if (repository == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_RA_ACCESS_REQUIRED);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            long revnum = repository.getDatedRevision(revision.getDate());
            result.set(RevisionsPair.revNumber, revnum);
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Unrecognized revision type requested for ''{0}''", path.getFile());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (youngestRevision != null && (revision == SVNRevision.HEAD || revision.getDate() != null) &&
                youngestRevision.lng(RevisionsPair.youngestRevision) >= 0 &&
                result.lng(RevisionsPair.revNumber) >= 0 &&
                result.lng(RevisionsPair.revNumber) > youngestRevision.lng(RevisionsPair.youngestRevision)) {
            result.set(RevisionsPair.revNumber, youngestRevision.lng(RevisionsPair.youngestRevision));
        }
        
        return result;
    }

    public SVNRepository createRepository(SVNURL url, File baseDirectory) throws SVNException {
        String expectedUuid = null;
        if (baseDirectory != null) {
            SVNWCNodeReposInfo nodeReposInfo = null;
            try {
                nodeReposInfo = getWCContext().getNodeReposInfo(baseDirectory);
                expectedUuid = nodeReposInfo.reposUuid;
            } catch (SVNException e) {
                SVNErrorCode errorCode = e.getErrorMessage().getErrorCode();
                if (!(errorCode == SVNErrorCode.WC_NOT_WORKING_COPY ||
                      errorCode == SVNErrorCode.WC_PATH_NOT_FOUND ||
                      errorCode == SVNErrorCode.WC_UPGRADE_REQUIRED)) {
                    throw e;
                }
            }            
        }
        return createRepository(url, expectedUuid, true);
    }

    public SvnCopySource createRemoteCopySource(SVNWCContext context, SvnCopySource localCopySource) throws SVNException {
        Structure<NodeOriginInfo> origin = context.getNodeOrigin(localCopySource.getSource().getFile(), true, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl, NodeOriginInfo.revision);
        SVNURL url = origin.get(NodeOriginInfo.reposRootUrl);
        url = url.appendPath(SVNFileUtil.getFilePath(origin.<File>get(NodeOriginInfo.reposRelpath)), false);
        SVNRevision pegRevision = localCopySource.getSource().getResolvedPegRevision();
        SVNRevision revision = localCopySource.getRevision();
        if (pegRevision == SVNRevision.UNDEFINED || pegRevision == SVNRevision.WORKING || pegRevision == SVNRevision.BASE) {
            pegRevision = SVNRevision.create(origin.lng(NodeOriginInfo.revision));
        }
        if (revision == SVNRevision.BASE) {
            revision = SVNRevision.create(origin.lng(NodeOriginInfo.revision));
        }
        origin.release();
        localCopySource = SvnCopySource.create(SvnTarget.fromURL(url, pegRevision), revision);
        return localCopySource;
    }
    
    protected SVNURL getTargetURL(SvnTarget target) throws SVNException {
        if (target.isURL()) {
            return target.getURL();
        }
        return getWCContext().getNodeUrl(target.getFile());
    }

}
