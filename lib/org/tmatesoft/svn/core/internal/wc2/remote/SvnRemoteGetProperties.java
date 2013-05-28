package org.tmatesoft.svn.core.internal.wc2.remote;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteGetProperties extends SvnRemoteOperationRunner<SVNProperties, SvnGetProperties> {

    public boolean isApplicable(SvnGetProperties operation, SvnWcGeneration wcGeneration) throws SVNException {
        if (operation.isRevisionProperties()) {
            return false;
        }        
        if (super.isApplicable(operation, wcGeneration)) {
            return true;
        }
        if (!operation.getRevision().isLocal()) {
            return true;
        }
        return false;
    }

    @Override
    protected SVNProperties run() throws SVNException {
        SvnTarget target = getOperation().getFirstTarget();
        Structure<RepositoryInfo> repositoryInfo = 
            getRepositoryAccess().createRepositoryFor(
                    target, 
                    getOperation().getRevision(), 
                    target.getResolvedPegRevision(), 
                    null);
        
        SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        long revnum = repositoryInfo.lng(RepositoryInfo.revision);
        SVNURL url = repositoryInfo.<SVNURL>get(RepositoryInfo.url);
        repositoryInfo.release();
        
        SVNNodeKind kind = repository.checkPath("", revnum);        
        remotePropertyGet(url, kind, "", repository, revnum, getOperation().getDepth());
        
        return getOperation().first();
    }
    
    private void remotePropertyGet(SVNURL url, SVNNodeKind kind, String path, SVNRepository repos, long revNumber, SVNDepth depth) throws SVNException {
        SVNURL fullURL = url.appendPath(path, false);
        SVNProperties props = new SVNProperties();
        final Collection<SVNDirEntry> dirEntries = new LinkedList<SVNDirEntry>();
        if (kind == SVNNodeKind.DIR) {
            ISVNDirEntryHandler handler = SVNDepth.FILES.compareTo(depth) <= 0 ? new ISVNDirEntryHandler() {
                public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
                    dirEntries.add(dirEntry);
                }
            } : null;
            repos.getDir(path, revNumber, props, SVNDirEntry.DIRENT_KIND, handler);
        } else if (kind == SVNNodeKind.FILE) {
            repos.getFile(path, revNumber, props, null);
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNKNOWN_KIND, "Unknown node kind for ''{0}''", fullURL);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (props != null) {
            for (String name : new HashSet<String>(props.nameSet())) {
                if (!SVNProperty.isRegularProperty(name)) {
                    props.remove(name);
                }                
            }
            if (!props.isEmpty()) {
                getOperation().receive(SvnTarget.fromURL(fullURL), props);
            }
        }
        
        if (depth.compareTo(SVNDepth.EMPTY) > 0 && dirEntries != null && kind == SVNNodeKind.DIR) {
            for (SVNDirEntry entry : dirEntries) {
                if (entry.getKind() == SVNNodeKind.FILE || depth.compareTo(SVNDepth.FILES) > 0) {
                    String entryPath = SVNPathUtil.append(path, entry.getName());
                    SVNDepth depthBelow = depth;
                    if (depth == SVNDepth.IMMEDIATES) {
                        depthBelow = SVNDepth.EMPTY;
                    }
                    remotePropertyGet(url, entry.getKind(), entryPath, repos, revNumber, depthBelow);
                }
            }
        }
    }


}
