package org.tmatesoft.svn.core.internal.wc2.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RevisionsPair;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnInheritedProperties;
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
        SVNURL url;
        SVNRepository repository;
        long revnum;
        SvnTarget reposTarget = target;
        SVNRevision revision = getOperation().getRevision();
        SVNRevision pegRevision = target.getResolvedPegRevision();
        if (pegRevision.isLocal() && target.isFile()) {
            final File localAbsPath = getOperation().getFirstTarget().getFile();            
            final Structure<NodeOriginInfo> origin = getWcContext().getNodeOrigin(localAbsPath, false, 
                    NodeOriginInfo.isCopy, NodeOriginInfo.copyRootAbsPath, 
                    NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl,
                    NodeOriginInfo.reposUuid, NodeOriginInfo.revision);
            final File reposRelPath = origin.get(NodeOriginInfo.reposRelpath);
            if (reposRelPath != null) {
                final SVNURL rootURL = origin.get(NodeOriginInfo.reposRootUrl);
                url = rootURL.appendPath(SVNFileUtil.getFilePath(reposRelPath), false);
                reposTarget = SvnTarget.fromURL(url);
                Structure<RevisionsPair> revisionPair = null;
                revisionPair = getRepositoryAccess().getRevisionNumber(null, target, target.getResolvedPegRevision(), revisionPair);
                final long pegrevnum = revisionPair.lng(RevisionsPair.revNumber);
                pegRevision = SVNRevision.create(pegrevnum);
                
                if (getOperation().getRevision().isLocal()) {
                    revisionPair = getRepositoryAccess().getRevisionNumber(null, target, getOperation().getRevision(), revisionPair);
                    revnum = revisionPair.lng(RevisionsPair.revNumber);
                    revision = SVNRevision.create(revnum);
                } 
            } 
        } 

        Structure<RepositoryInfo> repositoryInfo = 
            getRepositoryAccess().createRepositoryFor(
                    reposTarget, 
                    revision, 
                    pegRevision, 
                    null);
        repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        revnum = repositoryInfo.lng(RepositoryInfo.revision);
        url = repositoryInfo.<SVNURL>get(RepositoryInfo.url);
        repositoryInfo.release();
        
        SVNNodeKind kind = repository.checkPath("", revnum);
        if (kind == SVNNodeKind.UNKNOWN) {
            final SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNKNOWN_KIND, "Unknown node kind for ''{0}''", repository.getLocation());
            SVNErrorManager.error(err, SVNLogType.WC);
        } else if (kind == null || kind == SVNNodeKind.NONE) {
            final SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, "''{0}'' does not exist in revision {1}", repository.getLocation(), revnum);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (getOperation().getTargetInheritedPropertiesReceiver() != null) {
            final SVNURL repositoryRoot = repository.getRepositoryRoot(true);
            final Map<String, SVNProperties> inheritedProperties = repository.getInheritedProperties("", revnum, null);
            final List<SvnInheritedProperties> result = new ArrayList<SvnInheritedProperties>();
            for (String path : inheritedProperties.keySet()) {
                final SvnInheritedProperties propItem = new SvnInheritedProperties();
                propItem.setTarget(SvnTarget.fromURL(repositoryRoot.appendPath(path, false)));
                propItem.setProperties(inheritedProperties.get(path));
                result.add(propItem);
            }
            if (!result.isEmpty()) {
                getOperation().getTargetInheritedPropertiesReceiver().receive(target, result);
            }
        }
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
