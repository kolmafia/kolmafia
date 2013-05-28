package org.tmatesoft.svn.core.internal.wc2.remote;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.ISVNCommitPathHandler;
import org.tmatesoft.svn.core.internal.wc.SVNCommitUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnCommitItem;
import org.tmatesoft.svn.core.wc2.SvnRemoteDelete;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteRemoteDelete extends SvnRemoteOperationRunner<SVNCommitInfo, SvnRemoteDelete>  {

    @Override
    protected SVNCommitInfo run() throws SVNException {
        if (getOperation().getTargets().size() == 0) {
            return SVNCommitInfo.NULL;
        }

        SVNHashMap reposInfo = new SVNHashMap();
        SVNHashMap relPathInfo = new SVNHashMap();

        try {

            for (SvnTarget target : getOperation().getTargets()) {
                SVNURL url = target.getURL();
                SVNRepository repository = null;
                SVNURL reposRoot = null;
                String reposRelPath = null;
                ArrayList<String> relPaths;
                SVNNodeKind kind;

                for (Iterator rootUrls = reposInfo.keySet().iterator(); rootUrls.hasNext(); ) {
                    reposRoot = (SVNURL) rootUrls.next();
                    reposRelPath = SVNWCUtils.isChild(reposRoot, url);

                    if (reposRelPath != null) {
                        repository = (SVNRepository) reposInfo.get(reposRoot);
                        relPaths = (ArrayList<String>) relPathInfo.get(reposRoot);
                        relPaths.add(reposRelPath);
                        break; //appropriate SVNRepository/root was found, stop searching
                    }
                }

                if (repository == null) {
                    repository = getRepositoryAccess().createRepository(url, null, false);
                    reposRoot = repository.getRepositoryRoot(true);
                    repository.setLocation(reposRoot, false);
                    reposInfo.put(reposRoot, repository);
                    reposRelPath = SVNWCUtils.isChild(reposRoot, url);
                    relPaths = new ArrayList<String>();
                    relPathInfo.put(reposRoot, relPaths);
                    relPaths.add(reposRelPath);
                }
                if (reposRelPath == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL ''{0}'' not within a repository", url);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }

                kind = repository.checkPath(reposRelPath, -1);
                if (kind == SVNNodeKind.NONE) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "URL ''{0}'' does not exist", url);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }

            SVNPropertiesManager.validateRevisionProperties(getOperation().getRevisionProperties());

            SVNCommitInfo info = null;
            for (Iterator rootUrls = reposInfo.keySet().iterator(); rootUrls.hasNext(); ) {
                SVNURL reposRoot = (SVNURL) rootUrls.next();
                SVNRepository repository = (SVNRepository) reposInfo.get(reposRoot);
                List<String> paths = (List<String>) relPathInfo.get(reposRoot);
                info = singleRepositoryDelete(repository, reposRoot, paths);
                if (info != null) {
                    getOperation().receive(SvnTarget.fromURL(reposRoot), info);
                }
            }

            return info != null ? info : SVNCommitInfo.NULL;
        } finally {
            // SvnRepositoryAccess won't close repositories that are created with SvnRepositoryAccess#createRepository
            // if mayReuse=false, so close all repositories ourselves
            for (Object entry : reposInfo.entrySet()) {
                SVNRepository svnRepository = (SVNRepository) (((Map.Entry) entry).getValue());
                if (svnRepository != null) {
                    svnRepository.closeSession();
                }
            }
        }
    }
    
    private SVNCommitInfo singleRepositoryDelete(SVNRepository repository, SVNURL rootURL, List<String> paths) throws SVNException {
    	if (paths.isEmpty()) {
            paths.add(SVNPathUtil.tail(rootURL.getURIEncodedPath()));
            rootURL = rootURL.removePathTail();
        }
    	String commitMessage;
    	if (getOperation().getCommitHandler() != null) {
	        SvnCommitItem[] commitItems = new SvnCommitItem[paths.size()];
	        for (int i = 0; i < commitItems.length; i++) {
	            String path = (String) paths.get(i);
	            SvnCommitItem item = new SvnCommitItem();
	            item.setKind(SVNNodeKind.NONE);
	            item.setUrl(rootURL.appendPath(path, true));
	            item.setFlags(SvnCommitItem.DELETE);
	            commitItems[i] = item;
	        }
	        commitMessage = getOperation().getCommitHandler().getCommitMessage(getOperation().getCommitMessage(), commitItems);
	        if (commitMessage == null) {
	            return SVNCommitInfo.NULL;
	        }
    	}
    	else {
    		commitMessage = getOperation().getCommitMessage();
    	}
        commitMessage = commitMessage == null ? "" : SVNCommitUtil.validateCommitMessage(commitMessage);

        final String[] pathsArray = new String[paths.size()];
        paths.toArray(pathsArray);
        final List<String> condencedPaths = new ArrayList<String>();
        String commonParentPath = SVNPathUtil.condencePaths(pathsArray, condencedPaths, false);
        if (condencedPaths.isEmpty()) {
            condencedPaths.add(SVNPathUtil.tail(commonParentPath));
            commonParentPath = SVNPathUtil.removeTail(commonParentPath);
        }

        repository.setLocation(rootURL.appendPath(commonParentPath, false), false);

        ISVNEditor commitEditor = repository.getCommitEditor(commitMessage, null, false, getOperation().getRevisionProperties(), null);
        ISVNCommitPathHandler deleter = new ISVNCommitPathHandler() {

            public boolean handleCommitPath(String commitPath, ISVNEditor commitEditor) throws SVNException {
                commitEditor.deleteEntry(commitPath, -1);
                return false;
            }
        };
        SVNCommitInfo info;
        try {
            SVNCommitUtil.driveCommitEditor(deleter, condencedPaths, commitEditor, -1);
            info = commitEditor.closeEdit();
        } catch (SVNException e) {
            try {
                commitEditor.abortEdit();
            } catch (SVNException inner) {
            }
            throw e;
        }
        if (info != null && info.getNewRevision() >= 0) {
            handleEvent(SVNEventFactory.createSVNEvent(null, SVNNodeKind.NONE, null, info.getNewRevision(), SVNEventAction.COMMIT_COMPLETED, null, null, null), ISVNEventHandler.UNKNOWN);
        }
        return info != null ? info : SVNCommitInfo.NULL;
        
    }
}
