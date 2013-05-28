package org.tmatesoft.svn.core.internal.wc2.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.ISVNCommitPathHandler;
import org.tmatesoft.svn.core.internal.wc.SVNCommitUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnCommitItem;
import org.tmatesoft.svn.core.wc2.SvnRemoteMkDir;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteRemoteMkDir extends SvnRemoteOperationRunner<SVNCommitInfo, SvnRemoteMkDir>  {

    @Override
    protected SVNCommitInfo run() throws SVNException {
        SVNCommitInfo info = doRun();
        if (info != null) {
            Collection<SvnTarget> targets = getOperation().getTargets();
            if (targets != null && targets.size() != 0) {
                SvnTarget firstTarget = targets.iterator().next();

                SVNRepository repository = getRepositoryAccess().createRepository(firstTarget.getURL(), null, true);
                SVNURL repositoryRoot = repository.getRepositoryRoot(true);

                getOperation().receive(SvnTarget.fromURL(repositoryRoot), info);
            }
        }
        return info;
    }
    
    protected SVNCommitInfo doRun() throws SVNException {
    	int i = 0;
        SVNURL[] urls = new SVNURL[getOperation().getTargets().size()];
        for (SvnTarget target : getOperation().getTargets()) {
            urls[i++] = target.getURL();
        }
        
    	if (getOperation().isMakeParents()) {
            List<SVNURL> allURLs = new LinkedList<SVNURL>();
            for (i = 0; i < urls.length; i++) {
                SVNURL url = urls[i];
                addURLParents(allURLs, url);
            }
            urls = (SVNURL[]) allURLs.toArray(new SVNURL[allURLs.size()]);
        }
        if (urls == null || urls.length == 0) {
            return SVNCommitInfo.NULL;
        }
        Collection<String> paths = new SVNHashSet();
        SVNURL rootURL = SVNURLUtil.condenceURLs(urls, paths, false);
        if (rootURL == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "Can not compute common root URL for specified URLs");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }
        if (paths.isEmpty()) {
            paths.add(SVNPathUtil.tail(rootURL.getURIEncodedPath()));
            rootURL = rootURL.removePathTail();
        }
        if (paths.contains("")) {
            List<String> convertedPaths = new ArrayList<String>();
            String tail = SVNPathUtil.tail(rootURL.getURIEncodedPath());
            rootURL = rootURL.removePathTail();
            for (Iterator<String> commitPaths = paths.iterator(); commitPaths.hasNext();) {
                String path = (String) commitPaths.next();
                if ("".equals(path)) {
                    convertedPaths.add(tail);
                } else {
                    convertedPaths.add(SVNPathUtil.append(tail, path));
                }
            }
            paths = convertedPaths;
        }
        List<String> sortedPaths = new ArrayList<String>(paths);
        Collections.sort(sortedPaths, SVNPathUtil.PATH_COMPARATOR);
        String commitMessage;
        if (getOperation().getCommitHandler() != null) {
	        SvnCommitItem[] commitItems = new SvnCommitItem[sortedPaths.size()];
	        for (i = 0; i < commitItems.length; i++) {
	            String path = (String) sortedPaths.get(i);
	            SvnCommitItem item = new SvnCommitItem();
	            item.setKind(SVNNodeKind.DIR);
	            item.setUrl(rootURL.appendPath(path, true));
	            item.setFlags(SvnCommitItem.ADD);
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
        List<String> decodedPaths = new ArrayList<String>();
        for (Iterator<String> commitPaths = sortedPaths.iterator(); commitPaths.hasNext();) {
            String path = commitPaths.next();
            decodedPaths.add(SVNEncodingUtil.uriDecode(path));
        }
        paths = decodedPaths;
        SVNRepository repository = getRepositoryAccess().createRepository(rootURL, null, true);
        commitMessage = SVNCommitUtil.validateCommitMessage(commitMessage);
        SVNPropertiesManager.validateRevisionProperties(getOperation().getRevisionProperties());
        ISVNEditor commitEditor = repository.getCommitEditor(commitMessage, null, false, getOperation().getRevisionProperties(), null);
        ISVNCommitPathHandler creater = new ISVNCommitPathHandler() {

            public boolean handleCommitPath(String commitPath, ISVNEditor commitEditor) throws SVNException {
                SVNPathUtil.checkPathIsValid(commitPath);
                commitEditor.addDir(commitPath, null, -1);
                return true;
            }
        };
        SVNCommitInfo info;
        try {
            SVNCommitUtil.driveCommitEditor(creater, paths, commitEditor, -1);
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
    
    private void addURLParents(List<SVNURL> targets, SVNURL url) throws SVNException {
        SVNURL parentURL = url.removePathTail();
        SVNRepository repository = getRepositoryAccess().createRepository(parentURL, null, true);
        SVNNodeKind kind = repository.checkPath("", SVNRepository.INVALID_REVISION);
        if (kind == SVNNodeKind.NONE) {
            addURLParents(targets, parentURL);
        }
        targets.add(url);
    }
}
