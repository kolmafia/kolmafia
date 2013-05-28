package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeInfo;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.CheckWCRootInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.SVNWCNodeReposInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnNgMergeinfoUtil {
    
    public static class SvnMergeInfoInfo {
        Map<String, SVNMergeRangeList> mergeinfo;
        boolean inherited;
        String walkRelPath;
    }

    public static class SvnMergeInfoCatalogInfo {
        Map<String, Map<String, SVNMergeRangeList>> catalog;
        boolean inherited;
        String walkRelPath;
    }
    
    private static Map<String, SVNMergeRangeList> parseMergeInfo(SVNWCContext context, File localAbsPath) throws SVNException {
        SVNPropertyValue propValue = context.getPropertyValue(localAbsPath, SVNProperty.MERGE_INFO);
        if (propValue != null && propValue.getString() != null) {
            return SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(propValue.getString()), null);
        }
        return null;
    }
    
    public static void elideMergeInfo(SVNWCContext context, SVNRepository repos, File targetAbsPath, File limitAbsPath) throws SVNException {
        if (limitAbsPath == null || !limitAbsPath.equals(targetAbsPath)) {
            
            SvnMergeInfoInfo targetMergeinfo = null;
            try {
                targetMergeinfo = getWCMergeInfo(context, targetAbsPath, limitAbsPath, SVNMergeInfoInheritance.INHERITED, false);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                    throw e;
                }
                return;
            }
            if (targetMergeinfo == null || targetMergeinfo.inherited || targetMergeinfo.mergeinfo == null) {
                return;
            }
            SvnMergeInfoInfo mergeinfo = null;
            try {
                mergeinfo = getWCMergeInfo(context, targetAbsPath, limitAbsPath, SVNMergeInfoInheritance.NEAREST_ANCESTOR, false);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                    throw e;
                }
                return;
            }
            if ((mergeinfo == null || mergeinfo.mergeinfo == null) && limitAbsPath == null) {
                mergeinfo = new SvnMergeInfoInfo();
                try {
                    mergeinfo.mergeinfo = getWCOrReposMergeInfo(context, targetAbsPath, repos, true, SVNMergeInfoInheritance.NEAREST_ANCESTOR);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                        throw e;
                    }
                    return;
                }
            } 
            if (mergeinfo.mergeinfo == null && limitAbsPath != null) {
                return;
            }
            elideMergeInfo(context, mergeinfo.mergeinfo, targetMergeinfo.mergeinfo, targetAbsPath);
            
        }
    }
    
    private static void elideMergeInfo(SVNWCContext context, Map<String, SVNMergeRangeList> parent, Map<String, SVNMergeRangeList> child, File targetAbsPath) throws SVNException {
        if (SVNMergeInfoUtil.shouldElideMergeInfo(parent, child, null)) {
            SvnNgPropertiesManager.setProperty(context, targetAbsPath, SVNProperty.MERGE_INFO, null, SVNDepth.EMPTY, true, null, null);
            
            if (context.getEventHandler() != null) {
                SVNEvent event = SVNEventFactory.createSVNEvent(targetAbsPath, 
                        SVNNodeKind.UNKNOWN, 
                        null, -1, 
                        SVNStatusType.INAPPLICABLE, 
                        SVNStatusType.INAPPLICABLE, 
                        SVNStatusType.LOCK_INAPPLICABLE, 
                        SVNEventAction.MERGE_ELIDE_INFO, 
                        null, 
                        null, null, null);
                context.getEventHandler().handleEvent(event, -1);
                event = SVNEventFactory.createSVNEvent(targetAbsPath, 
                        SVNNodeKind.UNKNOWN, 
                        null, -1, 
                        SVNStatusType.INAPPLICABLE, 
                        SVNStatusType.CHANGED, 
                        SVNStatusType.LOCK_INAPPLICABLE, 
                        SVNEventAction.UPDATE_UPDATE, 
                        null, 
                        null, null, null);
                context.getEventHandler().handleEvent(event, -1);
            }
        }
    }

    public static SvnMergeInfoInfo getWCMergeInfo(SVNWCContext context, File localAbsPath, File limitAbsPath, SVNMergeInfoInheritance inheritance, 
            boolean ignoreInvalidMergeInfo) throws SVNException {
        long baseRevision = context.getNodeBaseRev(localAbsPath);
        Map<String, SVNMergeRangeList> wcMergeInfo = null;
        String walkRelPath = "";
        SvnMergeInfoInfo result = new SvnMergeInfoInfo();
        
        while(true) {
            if (inheritance == SVNMergeInfoInheritance.NEAREST_ANCESTOR) {
                wcMergeInfo = null;
                inheritance =  SVNMergeInfoInheritance.INHERITED;
            } else {
                try {
                    wcMergeInfo = parseMergeInfo(context, localAbsPath);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                        if (ignoreInvalidMergeInfo || !"".equals(walkRelPath)) {
                            wcMergeInfo = new HashMap<String, SVNMergeRangeList>();
                            break;
                        }
                    }
                    throw e;
                }
            }
            if (wcMergeInfo == null && inheritance != SVNMergeInfoInheritance.EXPLICIT && SVNFileUtil.getParentFile(localAbsPath) != null) {
                if (limitAbsPath != null && localAbsPath.equals(limitAbsPath)) {
                    break;
                }
                CheckWCRootInfo rootInfo = context.checkWCRoot(localAbsPath, true);
                if (rootInfo.wcRoot || (rootInfo.switched && rootInfo.kind == SVNWCDbKind.Dir)) {
                    break;
                }
                walkRelPath = SVNPathUtil.append(SVNFileUtil.getFileName(localAbsPath), walkRelPath);
                localAbsPath = SVNFileUtil.getFileDir(localAbsPath);
                long parentBaseRev = context.getNodeBaseRev(localAbsPath);
                long parentChangedRev = context.getNodeChangedInfo(localAbsPath).changedRev;
                if (baseRevision >= 0 && (baseRevision < parentChangedRev || parentBaseRev < baseRevision)) {
                    break;
                }
                continue;
            }
            break;
        }
        
        if ("".equals(walkRelPath)) {
            result.inherited = false;
            result.mergeinfo = wcMergeInfo;
        } else {
            if (wcMergeInfo != null) {
                result.inherited = true;
                result.mergeinfo = new HashMap<String, SVNMergeRangeList>();                
                result.mergeinfo = SVNMergeInfoUtil.adjustMergeInfoSourcePaths(result.mergeinfo, walkRelPath, wcMergeInfo);
            }
        }
        result.walkRelPath = walkRelPath;
        if (result.inherited && !result.mergeinfo.isEmpty()) {
            result.mergeinfo = SVNMergeInfoUtil.getInheritableMergeInfo(result.mergeinfo, null, -1, -1);
            SVNMergeInfoUtil.removeEmptyRangeLists(result.mergeinfo);
        }
        return result;
    }
    
    
    private static SvnMergeInfoCatalogInfo getWcMergeInfoCatalog(SVNWCContext context, boolean includeDescendants, SVNMergeInfoInheritance inheritance, File localAbsPath, File limitAbsPath, boolean ignoreInvalidMergeInfo) throws SVNException {
        SvnMergeInfoCatalogInfo result = new SvnMergeInfoCatalogInfo();
        SVNWCNodeReposInfo reposInfo = context.getNodeReposInfo(localAbsPath);
        if (reposInfo.reposRootUrl == null) {
            result.walkRelPath = "";
            return result;
        }
        File targetReposRelPath = context.getNodeReposRelPath(localAbsPath);
        SvnMergeInfoInfo mi = getWCMergeInfo(context, localAbsPath, limitAbsPath, inheritance, ignoreInvalidMergeInfo);
        result.walkRelPath = mi.walkRelPath;
        result.inherited = mi.inherited;
        
        if (mi.mergeinfo != null) {
            result.catalog = new TreeMap<String, Map<String,SVNMergeRangeList>>();
            result.catalog.put(SVNFileUtil.getFilePath(targetReposRelPath), mi.mergeinfo);
        }
        if (context.readKind(localAbsPath, false) == SVNNodeKind.DIR && includeDescendants) {
            // recursive propget do.
            final Map<File, String> mergeInfoProperties = new TreeMap<File, String>();
            ((SVNWCDb) context.getDb()).readPropertiesRecursively(localAbsPath, SVNDepth.INFINITY, false, false, null, 
            new ISvnObjectReceiver<SVNProperties>() {
                public void receive(SvnTarget target, SVNProperties object) throws SVNException {
                    if (object.getStringValue(SVNProperty.MERGE_INFO) != null) {
                        mergeInfoProperties.put(target.getFile(), object.getStringValue(SVNProperty.MERGE_INFO));
                    }
                }
            });
            
            for (File childPath : mergeInfoProperties.keySet()) {
                String propValue = mergeInfoProperties.get(childPath);
                File keyPath = context.getNodeReposRelPath(childPath);
                
                Map<String, SVNMergeRangeList> childMergeInfo = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(propValue), null);
                if (result.catalog == null) {
                    result.catalog = new TreeMap<String, Map<String,SVNMergeRangeList>>();                    
                }
                result.catalog.put(SVNFileUtil.getFilePath(keyPath), childMergeInfo);
            }
        }
        return result;
    }
    
    private static SvnMergeInfoCatalogInfo getReposMergeInfoCatalog(SVNRepository repository, String relativePath, long revision, SVNMergeInfoInheritance inheritance, 
            boolean squelchIncapable, boolean includeDescendats) throws SVNException {
        
        SvnMergeInfoCatalogInfo result = new SvnMergeInfoCatalogInfo();
        Map<String, SVNMergeInfo> reposMeregInfo = null;
        
        try {
            reposMeregInfo = repository.getMergeInfo(new String[] {relativePath}, revision, inheritance, includeDescendats);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.UNSUPPORTED_FEATURE && squelchIncapable) {
                return result;
            }
            throw e;
        }
        
        if (reposMeregInfo != null && !reposMeregInfo.isEmpty()) {
            result.catalog = new TreeMap<String, Map<String,SVNMergeRangeList>>();
            for (String reposRelativePath : reposMeregInfo.keySet()) {
                SVNMergeInfo mi = reposMeregInfo.get(reposRelativePath);
                if (reposRelativePath.startsWith("/")) {
                    reposRelativePath = reposRelativePath.substring(1);
                }
                result.catalog.put(reposRelativePath, mi.getMergeSourcesToMergeLists());
            }
        }
        return result;
    }

    static SvnMergeInfoCatalogInfo getWcOrReposMergeInfoCatalog(SVNWCContext context, SVNRepository repository, File wcPath, 
            boolean includeDescendants, boolean reposOnly, boolean ignoreInvalidMergeInfo, SVNMergeInfoInheritance inheritance) throws SVNException {
        SvnMergeInfoCatalogInfo result = new SvnMergeInfoCatalogInfo();
        
        Structure<NodeOriginInfo> nodeOriginInfo = context.getNodeOrigin(wcPath, false, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl);
        SVNURL url = null;
        if (nodeOriginInfo.get(NodeOriginInfo.reposRelpath) != null) {
            url = nodeOriginInfo.get(NodeOriginInfo.reposRootUrl);
            url = SVNWCUtils.join(url, nodeOriginInfo.<File>get(NodeOriginInfo.reposRelpath));
        }
        long revision = nodeOriginInfo.lng(NodeOriginInfo.revision);
        File reposRelPath = nodeOriginInfo.<File>get(NodeOriginInfo.reposRelpath);
        nodeOriginInfo.release();
        
        Map<String, Map<String, SVNMergeRangeList>> wcMergeInfoCatalog = null;
        Map<String, Map<String, SVNMergeRangeList>> reposMergeInfoCatalog = null;
        
        if (!reposOnly) {
            SvnMergeInfoCatalogInfo catalogInfo = getWcMergeInfoCatalog(context, includeDescendants, inheritance, wcPath, null, ignoreInvalidMergeInfo);
            result.inherited = catalogInfo.inherited;
            wcMergeInfoCatalog = catalogInfo.catalog;
            if (!(catalogInfo.inherited 
                    || (inheritance == SVNMergeInfoInheritance.EXPLICIT)
                    || (reposRelPath != null 
                        && wcMergeInfoCatalog != null
                        && wcMergeInfoCatalog.get(SVNFileUtil.getFilePath(reposRelPath)) != null))) {
                reposOnly = true;
                includeDescendants = false;
            }
        }
        if (reposOnly && url != null) {
            SVNProperties originalProperties = context.getPristineProps(wcPath);
            if (!originalProperties.containsName(SVNProperty.MERGE_INFO)) {
                SVNURL oldLocation = repository.getLocation();
                try {
                    repository.setLocation(url, false);
                    SvnMergeInfoCatalogInfo catalogInfo = getReposMergeInfoCatalog(repository, "", revision, inheritance, true, includeDescendants);
                    reposMergeInfoCatalog = catalogInfo.catalog;
                    if (reposMergeInfoCatalog != null && reposMergeInfoCatalog.containsKey(SVNFileUtil.getFilePath(reposRelPath))) {
                        result.inherited = true;
                    }
                } finally {
                    repository.setLocation(oldLocation, false);
                }
                
            }
        }
        if (wcMergeInfoCatalog != null) {
            result.catalog = wcMergeInfoCatalog;
            if (reposMergeInfoCatalog != null) {
                SVNMergeInfoUtil.mergeCatalog(result.catalog, reposMergeInfoCatalog);
            }
        } else if (reposMergeInfoCatalog != null) {
            result.catalog = reposMergeInfoCatalog;
        }
        return result;
    }
    
    public static Map<String, Map<String, SVNMergeRangeList>> getMergeInfo(SVNWCContext context, SvnRepositoryAccess repoAccess, 
            SvnTarget target, boolean includeDescendants, boolean ignoreInvalidMergeInfo, SVNURL[] root) throws SVNException {
        Structure<SvnRepositoryAccess.RepositoryInfo> repositoryInfo = repoAccess.createRepositoryFor(target, SVNRevision.UNDEFINED, target.getPegRevision(), null);
        SVNURL url = repositoryInfo.get(SvnRepositoryAccess.RepositoryInfo.url);
        long pegRev = repositoryInfo.lng(SvnRepositoryAccess.RepositoryInfo.revision);
        long rev = -1;
        SVNRepository repository = repositoryInfo.get(SvnRepositoryAccess.RepositoryInfo.repository);
        
        repositoryInfo.release();
        boolean useURL = target.isURL();
        if (!useURL) {
            SVNURL originURL = null;
            Structure<NodeOriginInfo> nodeOriginInfo = context.getNodeOrigin(target.getFile(), false, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl);
            File reposRelPath = nodeOriginInfo.get(NodeOriginInfo.reposRelpath);
            if (reposRelPath != null) {
                originURL = nodeOriginInfo.get(NodeOriginInfo.reposRootUrl);
                originURL = SVNWCUtils.join(originURL, reposRelPath);
            }
            rev = nodeOriginInfo.lng(NodeOriginInfo.revision);
            if (originURL == null 
                    || !originURL.equals(url)
                    || pegRev != rev) {
                useURL = true;
            }
            nodeOriginInfo.release();
        }
        
        if (root != null && root.length > 0) {
            root[0] = repository.getRepositoryRoot(true);
        }
        
        if (useURL) {
            rev = pegRev;
            return getReposMergeInfoCatalog(repository, "", rev, SVNMergeInfoInheritance.INHERITED, false, includeDescendants).catalog;
        } else {
            return getWcOrReposMergeInfoCatalog(context, repository, target.getFile(), includeDescendants, false, ignoreInvalidMergeInfo, SVNMergeInfoInheritance.INHERITED).catalog;
        }
    }
    
    public static Map<String, SVNMergeRangeList> getWCOrReposMergeInfo(SVNWCContext context, File wcPath, SVNRepository repository, boolean reposOnly, SVNMergeInfoInheritance inheritance) throws SVNException {
        SvnMergeInfoCatalogInfo catalog = getWcOrReposMergeInfoCatalog(context, repository, wcPath, false, reposOnly, false, inheritance);
        if (catalog != null && catalog.catalog != null) {
            return catalog.catalog.values().iterator().next();
        }
        return null;
    }
    
    public static Map<String, Map<String, SVNMergeRangeList>> convertToCatalog(Map<String, SVNMergeInfo> catalog) {
        if (catalog == null) {
            return new TreeMap<String, Map<String,SVNMergeRangeList>>();
        }
        Map<String, Map<String, SVNMergeRangeList>> result = new TreeMap<String, Map<String,SVNMergeRangeList>>();
        for (String path : catalog.keySet()) {
            SVNMergeInfo mi = catalog.get(path);
            result.put(path, mi.getMergeSourcesToMergeLists());
        }
        return result;
    }

    public static Map<File, Map<String, SVNMergeRangeList>> convertToCatalog2(Map<String, SVNMergeInfo> catalog) {
        if (catalog == null) {
            return new TreeMap<File, Map<String,SVNMergeRangeList>>();
        }
        Map<File, Map<String, SVNMergeRangeList>> result = new TreeMap<File, Map<String,SVNMergeRangeList>>();
        for (String path : catalog.keySet()) {
            SVNMergeInfo mi = catalog.get(path);
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            result.put(new File(path.replace(File.separatorChar, '/')), mi.getMergeSourcesToMergeLists());
        }
        return result;
    }
    
    public static Map<String, Map<String, SVNMergeRangeList>> addPrefixToCatalog(Map<String, Map<String, SVNMergeRangeList>> catalog, File prefix) {
        Map<String, Map<String, SVNMergeRangeList>> result = new TreeMap<String, Map<String,SVNMergeRangeList>>();
        for (String path : catalog.keySet()) {
            Map<String, SVNMergeRangeList> mi = catalog.get(path);
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            String prefixedPath = SVNFileUtil.getFilePath(SVNFileUtil.createFilePath(prefix, path));
            result.put(prefixedPath, mi);
        }
        return result;
    }

}
