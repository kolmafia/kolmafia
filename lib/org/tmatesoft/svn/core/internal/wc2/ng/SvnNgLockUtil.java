package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgOperationRunner;

public class SvnNgLockUtil {

    public static SVNURL collectLockInfo(SvnNgOperationRunner opRunner, SVNWCContext context, SvnNgRepositoryAccess wcAccess, Collection<SvnTarget> targets, Map lockInfo, Map lockPaths, boolean lock, boolean stealLock) throws SVNException {
        String[] paths = new String[targets.size()];
        int i = 0;
        for (SvnTarget target : targets) {
        	paths[i] = target.getFile().getAbsolutePath();
            paths[i] = paths[i].replace(File.separatorChar, '/');
            i++;
        }
        
        Collection<String> condencedPaths = new ArrayList<String>();
        String commonParentPath = SVNPathUtil.condencePaths(paths, condencedPaths, false);
        if (condencedPaths.isEmpty()) {
            condencedPaths.add(SVNPathUtil.tail(commonParentPath));
            commonParentPath = SVNPathUtil.removeTail(commonParentPath);
        }
        if (commonParentPath == null || "".equals(commonParentPath)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "No common parent found, unable to operate on dijoint arguments");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        paths = (String[]) condencedPaths.toArray(new String[condencedPaths.size()]);
        int depth = 0;
        for (i = 0; i < paths.length; i++) {
            int segments = SVNPathUtil.getSegmentsCount(paths[i]);
            if (depth < segments) {
                depth = segments;
            }
        }
        
        for (i = 0; i < paths.length; i++) {
            File file = new File(commonParentPath, paths[i]);
            SVNURL url = context.getNodeUrl(file);
            if (url == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "''{0}'' has no URL", file);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            
            if (lock) {
                SVNRevision revision = stealLock ? SVNRevision.UNDEFINED :
                	SVNRevision.create(context.getNodeBaseRev(file));
                lockInfo.put(url, new LockInfo(file, revision));
            } else {
            	ISVNWCDb.SVNWCDbLock dbLock = context.getDb().getBaseInfo(file, BaseInfoField.lock).lock;
            	if (!stealLock && dbLock == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_MISSING_LOCK_TOKEN, "''{0}'' is not locked in this working copy", file);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                lockInfo.put(url, new LockInfo(file, stealLock ? null : dbLock.token));
            }
        }
        
        opRunner.checkCancelled();
        
        SVNURL[] urls = (SVNURL[]) lockInfo.keySet().toArray(new SVNURL[lockInfo.size()]);
        Collection<String> urlPaths = new SVNHashSet();
        final SVNURL topURL = SVNURLUtil.condenceURLs(urls, urlPaths, false);
        if (urlPaths.isEmpty()) {
            urlPaths.add("");
        }
        if (topURL == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Unable to lock/unlock across multiple repositories");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        for (Iterator<String> encodedPaths = urlPaths.iterator(); encodedPaths.hasNext();) {
            String encodedPath = encodedPaths.next();
            SVNURL fullURL = topURL.appendPath(encodedPath, true);
            LockInfo info = (LockInfo) lockInfo.get(fullURL);
            encodedPath = SVNEncodingUtil.uriDecode(encodedPath);
            if (lock) {
                if (info.myRevision == SVNRevision.UNDEFINED) {
                    lockPaths.put(encodedPath, null);
                } else {
                    lockPaths.put(encodedPath, new Long(info.myRevision.getNumber()));
                }
            } else {
                lockPaths.put(encodedPath, info.myToken);
            }
        }
        return topURL;
    }

    
    public static class LockInfo {

        public LockInfo(File file, SVNRevision rev) {
            myFile = file;
            myRevision = rev;
        }

        public LockInfo(File file, String token) {
            myFile = file;
            myToken = token;
        }

        private File myFile;
        private SVNRevision myRevision;
        private String myToken;
        
        public File getFile(){
        	return myFile;
        }
        
        public SVNRevision getRevision(){
        	return myRevision;
        }
        
        public String getToken(){
        	return myToken;
        }
    }

}
