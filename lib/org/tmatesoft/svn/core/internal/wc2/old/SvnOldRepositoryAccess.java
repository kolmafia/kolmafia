package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.admin.SVNEntry;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.ISvnOperationOptionsProvider;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnOldRepositoryAccess extends SvnRepositoryAccess {

    public SvnOldRepositoryAccess(ISvnOperationOptionsProvider operationOptionsProvider) throws SVNException {
        super(operationOptionsProvider, null);
    }
    
    public SvnCopySource createRemoteCopySource(SVNWCContext context, SvnCopySource localCopySource) throws SVNException {
        final SVNWCAccess wcAccess = SVNWCAccess.newInstance(null);
        final File path = localCopySource.getSource().getFile();
        try {
            wcAccess.probeOpen(path, false, 0);
            SVNEntry entry = wcAccess.getEntry(path, false);
            SVNURL url = entry.isCopied() ? entry.getCopyFromSVNURL() : entry.getSVNURL();
            if (url == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' does not have a URL associated with it", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            SVNRevision pegRevision = localCopySource.getSource().getResolvedPegRevision();
            SVNRevision revision = localCopySource.getRevision();
            if (pegRevision == SVNRevision.UNDEFINED || pegRevision == SVNRevision.WORKING || pegRevision == SVNRevision.BASE) {
                pegRevision = entry.isCopied() ? SVNRevision.create(entry.getCopyFromRevision()) : SVNRevision.create(entry.getRevision());
            }
            if (revision == SVNRevision.BASE) {
                revision= entry.isCopied() ? SVNRevision.create(entry.getCopyFromRevision()) : SVNRevision.create(entry.getRevision());
            }
            return SvnCopySource.create(SvnTarget.fromURL(url, pegRevision), revision);
        } finally {
            wcAccess.close();
        }
    }

    @Override
    public Structure<RepositoryInfo> createRepositoryFor(SvnTarget target, SVNRevision revision, SVNRevision pegRevision, File baseDirectory) throws SVNException {
        SVNURL url = getURL(target);
        if (url == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", target.getFile());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNRevision[] resolvedRevisions = resolveRevisions(pegRevision, revision, target.isURL(), true);
        SVNRevision pegRev = resolvedRevisions[0];
        SVNRevision startRev = resolvedRevisions[1];
        SVNRepository repository = createRepository(url, null, true);
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

    @Override
    public Structure<RevisionsPair> getRevisionNumber(SVNRepository repository, SvnTarget path, SVNRevision revision, Structure<RevisionsPair> youngestRevision) throws SVNException {
        Structure<RevisionsPair> result = youngestRevision == null ? Structure.obtain(RevisionsPair.class) : youngestRevision;
        if (repository == null && (revision == SVNRevision.HEAD || revision.getDate() != null)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_RA_ACCESS_REQUIRED);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (revision.getNumber() >= 0) {
            result.set(RevisionsPair.revNumber, revision.getNumber());
        } else if (revision.getDate() != null) {
            result.set(RevisionsPair.revNumber, repository.getDatedRevision(revision.getDate()));
        } else if (revision == SVNRevision.HEAD) {
            if (youngestRevision != null && youngestRevision.hasValue(RevisionsPair.youngestRevision) && youngestRevision.lng(RevisionsPair.youngestRevision) >= 0) {
                result.set(RevisionsPair.revNumber, youngestRevision.lng(RevisionsPair.youngestRevision));
            } else {
                long latestRevision = repository.getLatestRevision();
                result.set(RevisionsPair.revNumber, latestRevision);
                result.set(RevisionsPair.youngestRevision, latestRevision);
            }
        } else if (revision == SVNRevision.COMMITTED || revision == SVNRevision.WORKING || revision == SVNRevision.BASE || revision == SVNRevision.PREVIOUS) {
            if (path == null || path.isURL()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_VERSIONED_PATH_REQUIRED);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            SVNWCAccess wcAccess = createWCAccess();
            wcAccess.probeOpen(path.getFile(), false, 0);
            SVNEntry entry = null;
            try {
                entry = wcAccess.getVersionedEntry(path.getFile(), false);
            } finally {
                wcAccess.close();
            }
            if (revision == SVNRevision.WORKING || revision == SVNRevision.BASE) {
                result.set(RevisionsPair.revNumber, entry.getRevision());
            } else if (entry.getCommittedRevision() < 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Path ''{0}'' has no committed revision", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            } else {
                result.set(RevisionsPair.revNumber, revision == SVNRevision.PREVIOUS ? entry.getCommittedRevision() - 1 : entry.getCommittedRevision());
            }
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Unrecognized revision type requested for ''{0}''", path != null ? path : (Object) repository.getLocation());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return result;
    }
    
    protected SVNWCAccess createWCAccess() {
        SVNWCAccess access = SVNWCAccess.newInstance(getOperationOptionsProvider().getEventHandler());
        access.setOptions(getOperationOptionsProvider().getOptions());
        return access;
    }

    @Override
    public Structure<UrlInfo> getURLFromPath(SvnTarget path, SVNRevision revision, SVNRepository repository) throws SVNException {
        Structure<UrlInfo> urlInfo = Structure.obtain(UrlInfo.class);
        SVNURL url = null;
        SVNWCAccess wcAccess = SVNWCAccess.newInstance(null);
        try {
            wcAccess.openAnchor(path.getFile(), false, 0);
            SVNEntry entry = wcAccess.getVersionedEntry(path.getFile(), false);
            if (entry.getCopyFromURL() != null && revision == SVNRevision.WORKING) {
                url = entry.getCopyFromSVNURL();
                urlInfo.set(UrlInfo.pegRevision, entry.getCopyFromRevision());
                if (entry.getURL() == null || !entry.getURL().equals(entry.getCopyFromURL())) {
                    urlInfo.set(UrlInfo.dropRepsitory, true);
                    repository = null;
                }
            } else if (entry.getURL() != null) {
                url = entry.getSVNURL();
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } finally {
            wcAccess.close();
        }
        urlInfo.set(UrlInfo.url, url);
        return urlInfo;
    }
    
    protected SVNURL getURL(SvnTarget target) throws SVNException {
        if (target.isURL()) {
            return target.getURL();
        }
        return deriveLocation(target.getFile(), null, null, SVNRevision.UNDEFINED, null, null);
    }

    protected SVNURL deriveLocation(File path, SVNURL url, long[] pegRevisionNumber, SVNRevision pegRevision, SVNRepository repos, SVNWCAccess access) throws SVNException {
        if (path != null) {
            SVNEntry entry = null;
            if (access != null) {
                entry = access.getVersionedEntry(path, false);
            } else {
                SVNWCAccess wcAccess = createWCAccess();
                try {
                    wcAccess.probeOpen(path, false, 0);
                    entry = wcAccess.getVersionedEntry(path, false);
                } finally {
                    wcAccess.close();
                }
            }
            url = getEntryLocation(path, entry, pegRevisionNumber, pegRevision);
        }
        if (pegRevisionNumber != null && pegRevisionNumber.length > 0 && !SVNRevision.isValidRevisionNumber(pegRevisionNumber[0])) {
            boolean closeRepository = false;
            try {
                if (repos == null) {
                    repos = createRepository(url, null, false);
                    closeRepository = true;
                }
                Structure<RevisionsPair> revPair = getRevisionNumber(repos, path != null ? SvnTarget.fromFile(path) : null, pegRevision, null); 
                pegRevisionNumber[0] = revPair.lng(RevisionsPair.revNumber);
            } finally {
                if (closeRepository) {
                    repos.closeSession();
                }
            }
        }
        return url;
    }
    
    protected SVNURL getEntryLocation(File path, SVNEntry entry, long[] revNum, SVNRevision pegRevision) throws SVNException {
        SVNURL url = null;
        if (entry.getCopyFromURL() != null && pegRevision == SVNRevision.WORKING) {
            url = entry.getCopyFromSVNURL();
            if (revNum != null && revNum.length > 0) {
                revNum[0] = entry.getCopyFromRevision();
            }
        } else if (entry.getURL() != null) {
            url = entry.getSVNURL();
            if (revNum != null && revNum.length > 0) {
                revNum[0] = entry.getRevision();
            }
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "Entry for ''{0}'' has no URL", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return url;
    }

}
