package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNCommitUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNCommitter17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgCommitUtil.ISvnUrlKindCallback;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnCommitItem;
import org.tmatesoft.svn.core.wc2.SvnCommitPacket;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnRemoteCopy;
import org.tmatesoft.svn.core.wc2.hooks.ISvnCommitHandler;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgWcToReposCopy extends SvnNgOperationRunner<SVNCommitInfo, SvnRemoteCopy> implements ISvnUrlKindCallback {

    @Override
    public boolean isApplicable(SvnRemoteCopy operation, SvnWcGeneration wcGeneration) throws SVNException {
        return areAllSourcesLocal(operation) && !operation.getFirstTarget().isLocal();
    }
    
    private boolean areAllSourcesLocal(SvnRemoteCopy operation) {
        // need all sources to be wc files at WORKING.
        // BASE revision meas repos_to_repos copy
        for(SvnCopySource source : operation.getSources()) {
            if (source.getSource().isFile() && 
                    (source.getRevision() == SVNRevision.WORKING || source.getRevision() == SVNRevision.UNDEFINED)) {
                continue;
            }
            return false;
        }
        return true;
    }
    
    @Override
    protected SVNCommitInfo run(SVNWCContext context) throws SVNException {
        SVNCommitInfo info = null;
        try {
            info = doRun(context, getOperation().getFirstTarget().getURL());
        } catch (SVNException e) {
            SVNErrorCode code = e.getErrorMessage().getErrorCode();
            if (!getOperation().isFailWhenDstExists()
                    && getOperation().getSources().size() == 1 
                    && (code == SVNErrorCode.ENTRY_EXISTS || code == SVNErrorCode.FS_ALREADY_EXISTS)) {
                
                SvnCopySource source = getOperation().getSources().iterator().next();
                SVNURL target = getOperation().getFirstTarget().getURL();
                target = target.appendPath(source.getSource().getFile().getName(), false);

                info = doRun(context, target);
            } else {
                throw e;
            }
        }
        if (info != null) {
            getOperation().receive(getOperation().getFirstTarget(), info);
        }
        return info;
    }
    
    protected SVNCommitInfo doRun(SVNWCContext context, SVNURL target) throws SVNException {
        if (getOperation().isMove()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                    "Moves between the working copy and the repository are not supported");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        Collection<SvnCopySource> sources = getOperation().getSources();
        Collection<SvnCopyPair> copyPairs = new ArrayList<SvnNgWcToReposCopy.SvnCopyPair>();

        if (sources.size() > 1) {
            for (SvnCopySource copySource : sources) {
                SvnCopyPair copyPair = new SvnCopyPair();
                String baseName;
                copyPair.source = copySource.getSource().getFile();
                baseName = copyPair.source.getName();
                copyPair.dst = target;
                copyPair.dst = copyPair.dst.appendPath(baseName, false);
                copyPairs.add(copyPair);
            }
        } else if (sources.size() == 1) {
            SvnCopyPair copyPair = new SvnCopyPair();
            SvnCopySource source = sources.iterator().next(); 
            copyPair.source= source.getSource().getFile();
            copyPair.dst = target;            
            copyPairs.add(copyPair);
        }

        return copy(copyPairs, getOperation().isMakeParents(), getOperation().getRevisionProperties(), getOperation().getCommitMessage(), 
                getOperation().getCommitHandler());
    }

    private SVNCommitInfo copy(Collection<SvnCopyPair> copyPairs, boolean makeParents, SVNProperties revisionProperties, String commitMessage, ISvnCommitHandler commitHandler) throws SVNException {
        SvnCopyPair firstPair = copyPairs.iterator().next();
        SVNURL topDstUrl = firstPair.dst.removePathTail();
        for (SvnCopyPair pair : copyPairs) {
            topDstUrl = SVNURLUtil.getCommonURLAncestor(topDstUrl, pair.dst);
        }
        File topSrcPath = getCommonCopyAncestor(copyPairs);
        SVNRepository repository = getRepositoryAccess().createRepository(topDstUrl, topSrcPath);
        topDstUrl = repository.getLocation();
        
        Collection<SVNURL> parents = null;
        if (makeParents) {
            parents = findMissingParents(topDstUrl, repository);
        }
        for (SvnCopyPair pair : copyPairs) {
            String path = SVNURLUtil.getRelativeURL(repository.getLocation(), pair.dst, false);
            if (repository.checkPath(path, -1) != SVNNodeKind.NONE) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_ALREADY_EXISTS,
                        "Path ''{0}'' already exists", pair.dst);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        SvnCommitItem[] items = new SvnCommitItem[(parents != null ? parents.size() : 0) + copyPairs.size()];
        int index = 0;
        if (makeParents && parents != null) {
            for (SVNURL parent : parents) {
                SvnCommitItem parentItem = new SvnCommitItem();
                parentItem.setUrl(parent);
                parentItem.setFlags(SvnCommitItem.ADD);
                parentItem.setKind(SVNNodeKind.DIR);
                items[index++] = parentItem;
            }
        }
        for (SvnCopyPair svnCopyPair : copyPairs) {
            SvnCommitItem item = new SvnCommitItem();
            item.setUrl(svnCopyPair.dst);
            item.setPath(svnCopyPair.source);
            item.setFlags(SvnCommitItem.ADD);
            item.setKind(SVNNodeKind.DIR);
            items[index++] = item;
        }
        commitMessage = getOperation().getCommitHandler().getCommitMessage(commitMessage, items);
        if (commitMessage == null) {
            return SVNCommitInfo.NULL;
        }
        commitMessage = SVNCommitUtil.validateCommitMessage(commitMessage);

        revisionProperties = getOperation().getCommitHandler().getRevisionProperties(commitMessage, items, revisionProperties);
        if (revisionProperties == null) {
            return SVNCommitInfo.NULL;
        }
        SvnCommitPacket packet = new SvnCommitPacket();
        SVNURL repositoryRoot = repository.getRepositoryRoot(true);
        if (parents != null) {
            for (SVNURL parent : parents) {
                String parentPath = SVNURLUtil.getRelativeURL(repositoryRoot, parent, false);
                packet.addItem(null, SVNNodeKind.DIR, repositoryRoot, parentPath, -1, null, -1, SvnCommitItem.ADD);
            }
        }
        for (SvnCopyPair svnCopyPair : copyPairs) {
            Map<File, String> externals = getOperation().getExternalsHandler() != null ? new HashMap<File, String>() : null;
            SvnNgCommitUtil.harvestCopyCommitables(getWcContext(), svnCopyPair.source, svnCopyPair.dst, packet, this, getOperation().getCommitParameters(), externals);
            
            SvnCommitItem item = packet.getItem(svnCopyPair.source);
            if (item == null) {
                continue;
            }
            Map<String, SVNMergeRangeList> mergeInfo = calculateTargetMergeInfo(svnCopyPair.source, -1, repository);
            String mergeInfoProperty = getWcContext().getProperty(svnCopyPair.source, SVNProperty.MERGE_INFO);
            Map<String, SVNMergeRangeList> wcMergeInfo = 
                    mergeInfoProperty != null ? SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(mergeInfoProperty), null) : null;
            if (wcMergeInfo != null && mergeInfo != null) {
                mergeInfo = SVNMergeInfoUtil.mergeMergeInfos(mergeInfo, wcMergeInfo);
            } else if (mergeInfo == null) {
                mergeInfo = wcMergeInfo;
            }
            String extendedMergeInfoValue = null;
            if (wcMergeInfo != null) {
                extendedMergeInfoValue = SVNMergeInfoUtil.formatMergeInfoToString(wcMergeInfo, null);
                item.addOutgoingProperty(SVNProperty.MERGE_INFO, SVNPropertyValue.create(extendedMergeInfoValue));
            }
            // append externals changes
            if (externals != null && !externals.isEmpty()) {
                includeExternalsChanges(repository, packet, externals, svnCopyPair);
            }
        }

        if (getOperation().isDisableLocalModifications()) {
            SvnCommitPacket oldPacket = packet;
            packet = filterLocalModifications(packet);
            if (packet.isEmpty()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, buildErrorMessageWithDebugInformation(oldPacket));
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
        }

        Map<String, SvnCommitItem> committables = new TreeMap<String, SvnCommitItem>();
        SVNURL url = SvnNgCommitUtil.translateCommitables(packet.getItems(packet.getRepositoryRoots().iterator().next()), committables);
        repository.setLocation(url, false);
        ISVNEditor commitEditor = repository.getCommitEditor(commitMessage, null, false, revisionProperties, null);
        SVNCommitter17 committer = new SVNCommitter17(getWcContext(), committables, repositoryRoot, null, null, null);
        SVNCommitUtil.driveCommitEditor(committer, committables.keySet(), commitEditor, -1);
        committer.sendTextDeltas(commitEditor);
        SVNCommitInfo info = commitEditor.closeEdit();
        deleteDeleteFiles(committer, getOperation().getCommitParameters());
        return info;
    }

    private String buildErrorMessageWithDebugInformation(SvnCommitPacket oldPacket) {
        StringBuilder stringBuilder = new StringBuilder("Unable to perform wc to remote copy without local modifications:").append('\n');
        stringBuilder.append("Commit packet was:").append('\n');

        final Collection<SVNURL> repositoryRoots = oldPacket.getRepositoryRoots();
        for (SVNURL oldRoot : repositoryRoots) {
            stringBuilder.append(oldRoot).append("  :").append('\n');
            Collection<SvnCommitItem> oldItems = oldPacket.getItems(oldRoot);
            if (oldItems != null) {
                for (SvnCommitItem oldItem : oldItems) {
                    stringBuilder.append("path=").append(oldItem.getPath()).append('\n');
                    stringBuilder.append("kind=").append(oldItem.getKind()).append('\n');
                    stringBuilder.append("url=").append(oldItem.getUrl()).append('\n');
                    stringBuilder.append("revision=").append(oldItem.getRevision()).append('\n');
                    stringBuilder.append("copyUrl=").append(oldItem.getCopyFromUrl()).append('\n');
                    stringBuilder.append("copyRevision=").append(oldItem.getCopyFromRevision()).append('\n');
                    stringBuilder.append("flags=").append(oldItem.getFlags()).append('\n');
                }
            }
        }

        return stringBuilder.toString();
    }

    private SvnCommitPacket filterLocalModifications(SvnCommitPacket packet) throws SVNException {
        final SvnCommitPacket filteredPacket = new SvnCommitPacket();
        filteredPacket.setLockTokens(packet.getLockTokens());
        filteredPacket.setLockingContext(packet.getRunner(), packet.getLockingContext());

        final Collection<SVNURL> repositoryRoots = packet.getRepositoryRoots();
        for (SVNURL repositoryRoot : repositoryRoots) {
            final Collection<SvnCommitItem> items = packet.getItems(repositoryRoot);
            for (SvnCommitItem item : items) {

                if (item.hasFlag(SvnCommitItem.DELETE)) {
                    continue;
                }

                if (item.hasFlag(SvnCommitItem.ADD)) {
                    if (!item.hasFlag(SvnCommitItem.COPY)) {
                        continue;
                    }

                    final SVNURL copyFromUrl = item.getCopyFromUrl();
                    if (copyFromUrl == null) {
                        continue;
                    }

                    final ISVNWCDb.WCDbBaseInfo baseInfo;
                    try {
                        baseInfo = getWcContext().getDb().getBaseInfo(item.getPath(), ISVNWCDb.WCDbBaseInfo.BaseInfoField.reposRootUrl, ISVNWCDb.WCDbBaseInfo.BaseInfoField.reposRelPath);
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                            throw e;
                        } else {
                            continue;
                        }
                    }
                    final SVNURL url = baseInfo.reposRootUrl.appendPath(SVNFileUtil.getFilePath(baseInfo.reposRelPath), false);

                    if (!copyFromUrl.equals(url)) {
                        continue;
                    }
                }

                item.setFlags(item.getFlags() & (~SvnCommitItem.TEXT_MODIFIED) & (~SvnCommitItem.PROPS_MODIFIED));

                filteredPacket.addItem(item, repositoryRoot);
            }
        }

        return filteredPacket;
    }
    
    private void includeExternalsChanges(SVNRepository repos, SvnCommitPacket packet, Map<File, String> externalsStorage, SvnCopyPair svnCopyPair) throws SVNException {
        for (File externalHolder : externalsStorage.keySet()) {
            String externalsPropString = (String) externalsStorage.get(externalHolder);
            SVNExternal[] externals = SVNExternal.parseExternals(externalHolder.getAbsolutePath(), externalsPropString);
            boolean introduceVirtualExternalChange = false;
            List<String> newExternals = new ArrayList<String>();
            SVNURL ownerURL = getWcContext().getNodeUrl(externalHolder);
            if (ownerURL == null) {
                continue;
            }
            long ownerRev = getWcContext().getNodeBaseRev(externalHolder);

            File ownerReposRelPath = getWcContext().getNodeReposRelPath(externalHolder);
            File sourceReposRelPath = getWcContext().getNodeReposRelPath(svnCopyPair.source);
            String relativePath = SVNWCUtils.getPathAsChild(sourceReposRelPath, ownerReposRelPath);
            SVNURL targetURL = svnCopyPair.dst.appendPath(relativePath, false);
            
            for (int k = 0; k < externals.length; k++) {
                File externalWC = new File(externalHolder, externals[k].getPath());
                SVNRevision externalsWCRevision = SVNRevision.UNDEFINED;
                
                try {
                    long rev = getWcContext().getNodeBaseRev(externalWC);
                    if (rev >= 0) {
                        externalsWCRevision = SVNRevision.create(rev);
                    }
                } catch (SVNException e) {
                    // smthing went wrong.                    
                }
                
                SVNURL resolvedURL = externals[k].resolveURL(repos.getRepositoryRoot(true), ownerURL);
                String unresolvedURL = externals[k].getUnresolvedUrl();
                if (unresolvedURL != null && !SVNPathUtil.isURL(unresolvedURL) && unresolvedURL.startsWith("../"))  {
                    unresolvedURL = SVNURLUtil.getRelativeURL(repos.getRepositoryRoot(true), resolvedURL, true);
                    if (unresolvedURL.startsWith("/")) {
                        unresolvedURL = "^" + unresolvedURL;
                    } else {
                        unresolvedURL = "^/" + unresolvedURL;
                    }
                }

                SVNRevision[] revs = getOperation().getExternalsHandler().handleExternal(
                        externalWC,
                        resolvedURL,
                        externals[k].getRevision(),
                        externals[k].getPegRevision(),
                        externals[k].getRawValue(),
                        externalsWCRevision);

                if (revs != null && revs.length == 2 && !revs[0].equals(externals[k].getRevision())) {
                    SVNExternal newExternal = new SVNExternal(externals[k].getPath(),
                            unresolvedURL,
                            revs[1],
                            revs[0], 
                            true, 
                            externals[k].isPegRevisionExplicit(),
                            externals[k].isNewFormat());
                    newExternals.add(newExternal.toString());

                    if (!introduceVirtualExternalChange) {
                        introduceVirtualExternalChange = true;
                    }
                } else if (revs != null) {
                    newExternals.add(externals[k].getRawValue());
                }
            } 
            if (introduceVirtualExternalChange) {
                String newExternalsProp = "";
                for (String external : newExternals) {
                    newExternalsProp += external + '\n';
                }

                SvnCommitItem itemWithExternalsChanges = packet.getItem(externalHolder);
                if (itemWithExternalsChanges == null) {
                    itemWithExternalsChanges = packet.addItem(externalHolder, repos.getRepositoryRoot(true), SVNNodeKind.DIR, targetURL, -1, ownerURL, ownerRev, 
                            SvnCommitItem.PROPS_MODIFIED);
                } 
                itemWithExternalsChanges.addOutgoingProperty(SVNProperty.EXTERNALS, SVNPropertyValue.create(newExternalsProp));
            }
        }
    }
    
    private Collection<SVNURL> findMissingParents(SVNURL targetURL, SVNRepository repository) throws SVNException {
        SVNNodeKind kind = repository.checkPath("", -1);
        Collection<SVNURL> parents = new ArrayList<SVNURL>();
        while (kind == SVNNodeKind.NONE) {
            parents.add(targetURL);
            targetURL = targetURL.removePathTail();
            repository.setLocation(targetURL, false);
            kind = repository.checkPath("", -1);
        }
        if (kind != SVNNodeKind.DIR) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_ALREADY_EXISTS,
                    "Path ''{0}'' already exists, but it is not a directory", targetURL);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return parents;
    }

    private File getCommonCopyAncestor(Collection<SvnCopyPair> copyPairs) {
        File ancestor = null;
        for (SvnCopyPair svnCopyPair : copyPairs) {
            if (ancestor == null) {
                ancestor = svnCopyPair.source;
                continue;
            }
            String ancestorPath = ancestor.getAbsolutePath().replace(File.separatorChar, '/');
            String sourcePath = svnCopyPair.source.getAbsolutePath().replace(File.separatorChar, '/');
            ancestorPath = SVNPathUtil.getCommonPathAncestor(ancestorPath, sourcePath);
            ancestor = new File(ancestorPath);
        }
        return ancestor;
    }

    private Map<String, SVNMergeRangeList> calculateTargetMergeInfo(File srcFile, long srcRevision, SVNRepository repository) throws SVNException {
        SVNURL url = null;
        SVNURL oldLocation = null;
        
        Structure<NodeOriginInfo> nodeOrigin = getWcContext().getNodeOrigin(srcFile, false, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl);
        if (nodeOrigin != null && nodeOrigin.get(NodeOriginInfo.reposRelpath) != null) {
            url = nodeOrigin.get(NodeOriginInfo.reposRootUrl);
            url = SVNWCUtils.join(url, nodeOrigin.<File>get(NodeOriginInfo.reposRelpath));
            srcRevision = nodeOrigin.lng(NodeOriginInfo.revision);
        }
        if (url != null) {
            Map<String, SVNMergeRangeList> targetMergeInfo = null;
            String mergeInfoPath;
            SVNRepository repos = repository;

            try {
                mergeInfoPath = getRepositoryAccess().getPathRelativeToSession(url, null, repos);
                if (mergeInfoPath == null) {
                    oldLocation = repos.getLocation();
                    repos.setLocation(url, false);
                    mergeInfoPath = "";
                }
                targetMergeInfo = getRepositoryAccess().getReposMergeInfo(repos, mergeInfoPath, srcRevision, SVNMergeInfoInheritance.INHERITED, true);
            } finally {
                if (repository == null) {
                    repos.closeSession();
                } else if (oldLocation != null) {
                    repos.setLocation(oldLocation, false);
                }
            }
            return targetMergeInfo;
        }
        return null;
    }

    private static class SvnCopyPair {
        File source;
        SVNURL dst;
    }

    public SVNNodeKind getUrlKind(SVNURL url, long revision) throws SVNException {
        return getRepositoryAccess().createRepository(url, null).checkPath("", revision);
    }
}
