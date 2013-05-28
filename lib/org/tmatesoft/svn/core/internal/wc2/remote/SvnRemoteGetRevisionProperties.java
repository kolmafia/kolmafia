package org.tmatesoft.svn.core.internal.wc2.remote;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RevisionsPair;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteGetRevisionProperties extends SvnRemoteOperationRunner<SVNProperties, SvnGetProperties> {

    @Override
    public boolean isApplicable(SvnGetProperties operation, SvnWcGeneration wcGeneration) throws SVNException {
        return operation.isRevisionProperties();
    }
    
    @Override
    protected SVNProperties run() throws SVNException {
        SVNRevision revision = getOperation().getRevision();
        if (!revision.isValid()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Valid revision have to be specified to fetch revision property");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }

        SvnRepositoryAccess access = getRepositoryAccess();
        Structure<RepositoryInfo> repositoryInfo = access.createRepositoryFor(getOperation().getFirstTarget(), SVNRevision.HEAD, SVNRevision.HEAD, null);
        SVNRepository repository = repositoryInfo.get(RepositoryInfo.repository);
        repositoryInfo.release();

        Structure<RevisionsPair> revPair = access.getRevisionNumber(repository, getOperation().getFirstTarget(), getOperation().getRevision(), null);
        long revNumber = revPair.lng(RevisionsPair.revNumber);
        getOperation().setRevisionNumber(revNumber);
        
        SVNProperties revisionProperties = repository.getRevisionProperties(revNumber, null);
        if (revisionProperties != null) {
            getOperation().receive(getOperation().getFirstTarget(), revisionProperties);
        }
        
        return revisionProperties;
    }

}
