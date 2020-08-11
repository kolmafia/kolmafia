package org.tmatesoft.svn.core.internal.util;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;

public class SVNPathRevision {

    public static SVNPathRevision createWithRepository(SVNRepository svnRepository, SVNURL url, long revision) throws SVNException {
        SVNURL repositoryRoot = svnRepository.getRepositoryRoot(false);
        String repositoryUUID = svnRepository.getRepositoryUUID(false);
        return new SVNPathRevision(url, revision, repositoryRoot, repositoryUUID);
    }

    private final SVNURL url;
    private final long revision;
    private final SVNURL reposRootUrl;
    private final String reposUuid;

    public SVNPathRevision(SVNURL url, long revision, SVNURL reposRootUrl, String reposUuid) {
        this.url = url;
        this.revision = revision;
        this.reposRootUrl = reposRootUrl;
        this.reposUuid = reposUuid;
    }

    public SVNURL getUrl() {
        return url;
    }

    public long getRevision() {
        return revision;
    }

    public SVNURL getReposRootUrl() {
        return reposRootUrl;
    }

    public String getReposUuid() {
        return reposUuid;
    }
}
