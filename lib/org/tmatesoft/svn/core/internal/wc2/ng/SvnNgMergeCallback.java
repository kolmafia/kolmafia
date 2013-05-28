package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.MergeInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.MergePropertiesInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.SVNWCNodeReposInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.LocationsInfo;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergeDriver.ObstructionState;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNConflictHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNConflictResult;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNOperation;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgMergeCallback implements ISvnDiffCallback {
    
    private Collection<File> conflictedPaths;
    private SvnNgMergeDriver driver;
    
    public SvnNgMergeCallback(SvnNgMergeDriver driver) {
        this.driver = driver;
    }

    public Collection<File> getConflictedPaths() {
        return conflictedPaths;
    }

    public void fileOpened(SvnDiffCallbackResult result, File path, long revision) throws SVNException {
        // do nothing
    }

    public void fileChanged(SvnDiffCallbackResult result, final File path,
            File tmpFile1, File tmpFile2, long rev1, long rev2,
            String mimetype1, String mimeType2, SVNProperties propChanges,
            SVNProperties originalProperties) throws SVNException {
        ObstructionState os = driver.performObstructionCheck(path, SVNNodeKind.UNKNOWN);
        if (os.obstructionState != SVNStatusType.INAPPLICABLE) {
            result.contentState = os.obstructionState;
            if (os.obstructionState == SVNStatusType.MISSING) {
                result.propState = SVNStatusType.MISSING;
            }
            return;
        }
        SVNNodeKind wcKind = os.kind;
        boolean isDeleted = os.deleted;
        
        if (wcKind != SVNNodeKind.FILE  || isDeleted) {
            if (wcKind == SVNNodeKind.NONE) {
                SVNDepth parentDepth = getContext().getNodeDepth(SVNFileUtil.getParentFile(path));
                if (parentDepth != SVNDepth.UNKNOWN && parentDepth.compareTo(SVNDepth.FILES) < 0) {
                    result.contentState = SVNStatusType.MISSING;
                    result.propState = SVNStatusType.MISSING;
                    return;
                }
            }

            treeConflict(path, SVNNodeKind.FILE, SVNConflictAction.EDIT, SVNConflictReason.MISSING);
            
            result.treeConflicted = true;
            result.contentState = SVNStatusType.MISSING;
            result.propState = SVNStatusType.MISSING;
            return;
        }
        
        if (!propChanges.isEmpty()) {
            MergePropertiesInfo mergeOutcome = mergePropChanges(path, propChanges, originalProperties);
            result.propState = mergeOutcome != null ? mergeOutcome.mergeOutcome : null;
            if (mergeOutcome != null && mergeOutcome.treeConflicted) {
                result.treeConflicted = true;
                return;
            }
        } else {
            result.propState = SVNStatusType.UNCHANGED;
        }
        
        if (isRecordOnly()) {
            result.contentState = SVNStatusType.UNCHANGED;
            return;
        }
        
        if (tmpFile1 != null) {
            boolean hasLocalMods = getContext().isTextModified(path, false);
            String targetLabel = ".working";
            String leftLabel = ".merge-left.r" + rev1;
            String rightLabel = ".merge-right.r" + rev2;
            SVNConflictVersion[] cvs = makeConflictVersions(path, SVNNodeKind.FILE);
            
            ISVNOptions opts = getContext().getOptions();
            final ISVNConflictHandler[] conflictHandler =  new ISVNConflictHandler[1];
            if (opts instanceof DefaultSVNOptions) {
                conflictHandler[0] = ((DefaultSVNOptions) opts).getConflictResolver();
                ((DefaultSVNOptions) opts).setConflictHandler(new ISVNConflictHandler() {
                    public SVNConflictResult handleConflict(SVNConflictDescription conflictDescription) throws SVNException {
                        SVNConflictResult result = conflictHandler[0] == null ? 
                                new SVNConflictResult(SVNConflictChoice.POSTPONE, null) : 
                                conflictHandler[0].handleConflict(conflictDescription);

                        if (result != null && result.getConflictChoice() == SVNConflictChoice.POSTPONE) {
                            if (conflictedPaths == null) {
                                conflictedPaths = new HashSet<File>();
                            }
                            conflictedPaths.add(path);
                        }
                        return result;
                    }
                });
            }
            try { 
                MergeInfo mergeOutcome = getContext().mergeText(tmpFile1, tmpFile2, path, leftLabel, rightLabel, targetLabel, cvs[0], cvs[1], isDryRun(), getDiffOptions(), propChanges);
                if (mergeOutcome.mergeOutcome == SVNStatusType.CONFLICTED) {
                    result.contentState = SVNStatusType.CONFLICTED;
                } else if (hasLocalMods && mergeOutcome.mergeOutcome != SVNStatusType.UNCHANGED) {
                    result.contentState = SVNStatusType.MERGED;
                } else if (mergeOutcome.mergeOutcome == SVNStatusType.MERGED) {
                    result.contentState = SVNStatusType.CHANGED;
                } else if (mergeOutcome.mergeOutcome == SVNStatusType.NO_MERGE) {
                    result.contentState = SVNStatusType.MISSING;
                } else {
                    result.contentState = SVNStatusType.UNCHANGED;
                }
            } finally {
                if (opts instanceof DefaultSVNOptions) {
                    ((DefaultSVNOptions) opts).setConflictHandler(conflictHandler[0]);
                }
            }
        }
    }

    public void fileAdded(SvnDiffCallbackResult result, File path,
            File leftFile, File rightFile, long rev1, long rev2,
            String mimeType1, String mimeType2, File copyFromPath,
            long copyFromRevision, SVNProperties propChanges,
            SVNProperties originalProperties) throws SVNException {
        if (isRecordOnly()) {
            result.contentState = SVNStatusType.UNCHANGED;
            result.propState = SVNStatusType.UNCHANGED;
            return;
        }
        result.propState = SVNStatusType.UNKNOWN;
        SVNProperties fileProps = new SVNProperties(originalProperties);
        for (String propName : propChanges.nameSet()) {
            if (SVNProperty.isWorkingCopyProperty(propName)) {
                continue;
            }
            if (!isSameRepos() && !SVNProperty.isRegularProperty(propName)) {
                continue;
            }
            if (!isSameRepos() && SVNProperty.MERGE_INFO.equals(propName)) {
                continue;
            }
            if (propChanges.getSVNPropertyValue(propName) != null) {
                fileProps.put(propName, propChanges.getSVNPropertyValue(propName));
            } else {
                fileProps.remove(propName);
            }
        }
        
        ObstructionState os = driver.performObstructionCheck(path, SVNNodeKind.UNKNOWN);
        if (os.obstructionState != SVNStatusType.INAPPLICABLE) {
            if (isDryRun() && getAddedPath() != null && SVNWCUtils.isChild(getAddedPath(), path)) {
                result.contentState = SVNStatusType.CHANGED;
                if (!fileProps.isEmpty()) {
                    result.propState = SVNStatusType.CHANGED;
                }
            } else {
                result.contentState = os.obstructionState;
            }
            return;
        }
        
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path));
        
        if (kind == SVNNodeKind.NONE) {
            if (!isDryRun()) {
                SVNURL copyFromUrl = null;
                long copyFromRev = -1;
                InputStream newBaseContents = null;
                InputStream newContents = null;
                SVNProperties newBaseProps, newProps;
                try {
                    if (isSameRepos()) {
                        String child = SVNWCUtils.getPathAsChild(getTargetPath(), path);
                        if (child != null) {
                            copyFromUrl = getSource2URL().appendPath(child, false);
                        } else {
                            copyFromUrl = getSource2URL();
                        }
                        copyFromRev = rev2;
                        checkReposMatch(path, copyFromUrl);
                        newBaseContents = SVNFileUtil.openFileForReading(rightFile);
                        newContents = null;
                        newBaseProps = fileProps;
                        newProps = null;
                    } else {
                        newBaseProps = new SVNProperties();
                        newProps = fileProps;
                        newBaseContents = SVNFileUtil.DUMMY_IN;
                        newContents = SVNFileUtil.openFileForReading(rightFile);
                    }
                    SVNTreeConflictDescription tc = getContext().getTreeConflict(path);
                    if (tc != null) {
                        treeConflictOnAdd(path, SVNNodeKind.FILE, SVNConflictAction.ADD, SVNConflictReason.ADDED);
                        result.treeConflicted = true;
                    } else {
                        SvnNgReposToWcCopy.addFileToWc(getContext(), path, newBaseContents, newContents, newBaseProps, newProps, copyFromUrl, copyFromRev);
                    }
                } finally {
                    SVNFileUtil.closeFile(newBaseContents);
                    SVNFileUtil.closeFile(newContents);
                }
            }
            result.contentState = SVNStatusType.CHANGED;
            if (!fileProps.isEmpty()) {
                result.propState = SVNStatusType.CHANGED;
            }
        } else if (kind == SVNNodeKind.DIR) {
            treeConflictOnAdd(path, SVNNodeKind.FILE, SVNConflictAction.ADD, SVNConflictReason.OBSTRUCTED);
            result.treeConflicted = true;
            SVNNodeKind wcKind = getContext().readKind(path, false);
            if (wcKind != SVNNodeKind.NONE && driver.isDryRunDeletion(path)) {
                result.contentState = SVNStatusType.CHANGED;
            } else {
                result.contentState = SVNStatusType.OBSTRUCTED;
            }
        } else if (kind == SVNNodeKind.FILE) {
            if (driver.isDryRunDeletion(path)) {
                result.contentState = SVNStatusType.CHANGED;
            } else {
                treeConflictOnAdd(path, SVNNodeKind.FILE, SVNConflictAction.ADD, SVNConflictReason.ADDED);
                result.treeConflicted = true;
            }            
        } else {
            result.contentState = SVNStatusType.UNKNOWN;
        }
    }

    public void fileDeleted(SvnDiffCallbackResult result, File path,
            File leftFile, File rightFile, String mimeType1, String mimeType2,
            SVNProperties originalProperties) throws SVNException {
        
        if (isDryRun()) {
            if (getDryRunDeletions() == null) {
                setDryRunDeletions(new HashSet<File>());
            }
            getDryRunDeletions().add(path);
        }
        if (isRecordOnly()) {
            result.contentState = SVNStatusType.UNCHANGED;
            return;
        }
        ObstructionState os = driver.performObstructionCheck(path, SVNNodeKind.UNKNOWN);
        if (os.obstructionState != SVNStatusType.INAPPLICABLE) {
            result.contentState = os.obstructionState;
            return;
        }
        
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path));
        if (kind == SVNNodeKind.FILE) {
            boolean same = compareFiles(leftFile, originalProperties, path);
            if (same || isForce() || isRecordOnly()) {
                if (!isDryRun()) {
                    SvnNgRemove.delete(getContext(), path, false, true, null);
                }
                result.contentState = SVNStatusType.CHANGED;
            } else {
                treeConflict(path, SVNNodeKind.FILE, SVNConflictAction.DELETE, SVNConflictReason.EDITED);
                result.treeConflicted = true;
                result.contentState = SVNStatusType.OBSTRUCTED;
            }
        } else if (kind == SVNNodeKind.DIR) {
            treeConflict(path, SVNNodeKind.FILE, SVNConflictAction.DELETE, SVNConflictReason.OBSTRUCTED);
            result.treeConflicted = true;
            result.contentState = SVNStatusType.OBSTRUCTED;
        } else if (kind == SVNNodeKind.NONE) {
            treeConflict(path, SVNNodeKind.FILE, SVNConflictAction.DELETE, SVNConflictReason.DELETED);
            result.treeConflicted = true;
            result.contentState = SVNStatusType.MISSING;
        } else {
            result.contentState = SVNStatusType.UNKNOWN;
        }
    }

    private void setDryRunDeletions(Collection<File> set) {
        driver.dryRunDeletions = set;
    }

    private void setDryRunAddtions(Collection<File> set) {
        driver.dryRunAdded = set;
    }

    public void dirDeleted(SvnDiffCallbackResult result, File path) throws SVNException {
        if (isRecordOnly()) {
            result.contentState = SVNStatusType.UNCHANGED;
            return;
        }
        ObstructionState os = driver.performObstructionCheck(path, SVNNodeKind.UNKNOWN);
        boolean isVersioned = os.kind == SVNNodeKind.DIR || os.kind == SVNNodeKind.FILE;
        if (os.obstructionState != SVNStatusType.INAPPLICABLE) {
            result.contentState = os.obstructionState;
            return;
        }
        if (os.deleted) {
            os.kind = SVNNodeKind.NONE;
        }
        if (isDryRun()) {
            if (getDryRunDeletions() == null) {
                setDryRunDeletions(new HashSet<File>());
            }
            getDryRunDeletions().add(path);
        }
        if (os.kind == SVNNodeKind.DIR) {
            if (isVersioned && !os.deleted) {
                try {
                    if (!isForce()) {
                        SvnNgRemove.checkCanDelete(driver.operation.getOperationFactory(), getContext(), path);
                    }
                    if (!isDryRun()) {
                        SvnNgRemove.delete(getContext(), path, false, false, null);
                    }
                    result.contentState = SVNStatusType.CHANGED;
                } catch (SVNException e) {
                    treeConflict(path, SVNNodeKind.DIR, SVNConflictAction.DELETE, SVNConflictReason.EDITED);
                    result.treeConflicted = true;
                    result.contentState = SVNStatusType.CONFLICTED;
                }
            } else {
                treeConflict(path, SVNNodeKind.DIR, SVNConflictAction.DELETE, SVNConflictReason.DELETED);
                result.treeConflicted = true;                
            }
        } else if (os.kind == SVNNodeKind.FILE) {
            result.contentState = SVNStatusType.OBSTRUCTED;
        } else if (os.kind == SVNNodeKind.NONE) {
            treeConflict(path, SVNNodeKind.DIR, SVNConflictAction.DELETE, SVNConflictReason.DELETED);
            result.treeConflicted = true;
            result.contentState = SVNStatusType.MISSING;
        } else {
            result.contentState = SVNStatusType.UNKNOWN;
        }
    }

    public void dirOpened(SvnDiffCallbackResult result, File path, long revision) throws SVNException {
        ObstructionState os = driver.performObstructionCheck(path, SVNNodeKind.UNKNOWN);
        if (os.obstructionState != SVNStatusType.INAPPLICABLE) {
            result.skipChildren = true;
            return;
        }
        if (os.kind != SVNNodeKind.DIR || os.deleted) {
            if (os.kind == SVNNodeKind.NONE) {
                SVNDepth parentDepth = getContext().getNodeDepth(SVNFileUtil.getParentFile(path));
                if (parentDepth != SVNDepth.UNKNOWN && parentDepth.compareTo(SVNDepth.IMMEDIATES) < 0) {
                    result.skipChildren = true;
                    return;
                }
            }
            if (os.kind == SVNNodeKind.FILE) {
                treeConflict(path, SVNNodeKind.DIR, SVNConflictAction.EDIT, SVNConflictReason.REPLACED);
                result.treeConflicted = true;
            } else if (os.deleted || os.kind == SVNNodeKind.NONE) {
                treeConflict(path, SVNNodeKind.DIR, SVNConflictAction.EDIT, SVNConflictReason.DELETED);
                result.treeConflicted = true;
            }
        }
    }

    public void dirAdded(SvnDiffCallbackResult result, File path, long revision, String copyFromPath, long copyFromRevision) throws SVNException {
        if (isRecordOnly()) {
            result.contentState = SVNStatusType.UNCHANGED;
            return;
        }
        File parentPath = SVNFileUtil.getParentFile(path);
        String child = SVNWCUtils.getPathAsChild(getTargetPath(), path);
        SVNURL copyFromUrl = null;
        long copyFromRev = -1;
        if (isSameRepos()) {
            copyFromUrl = getSource2URL().appendPath(child, false);
            copyFromRev = revision;
            checkReposMatch(parentPath, copyFromUrl);
        }
        ObstructionState os = driver.performObstructionCheck(path, SVNNodeKind.UNKNOWN);
        boolean isVersioned = os.kind == SVNNodeKind.DIR || os.kind == SVNNodeKind.FILE;
        if (os.obstructionState == SVNStatusType.OBSTRUCTED && (os.deleted || os.kind == SVNNodeKind.NONE)) {
            SVNFileType diskKind = SVNFileType.getType(path);
            if (diskKind == SVNFileType.DIRECTORY) {
                os.obstructionState = SVNStatusType.INAPPLICABLE;
                os.kind = SVNNodeKind.DIR;
            }
        }
        if (os.obstructionState != SVNStatusType.INAPPLICABLE) {
            if (isDryRun() && getAddedPath() != null && SVNWCUtils.isChild(getAddedPath(), path)) {
                result.contentState = SVNStatusType.CHANGED;
            } else {
                result.contentState = os.obstructionState;
            }
            return;
        }
        if (os.deleted) {
            os.kind = SVNNodeKind.NONE;
        }
        
        if (os.kind == SVNNodeKind.NONE) {
            if (isDryRun()) {
                if (getDryRunAdditions() == null) {
                    setDryRunAddtions(new HashSet<File>());
                }
                getDryRunAdditions().add(path);
                setAddedPath(path);
            } else {
                path.mkdir();
                if (copyFromUrl != null) {
                    SVNWCNodeReposInfo reposInfo = getContext().getNodeReposInfo(parentPath);
                    File reposRelPath = new File(SVNURLUtil.getRelativeURL(reposInfo.reposRootUrl, copyFromUrl, false));
                    getContext().getDb().opCopyDir(path, new SVNProperties(), 
                            copyFromRev, new SVNDate(0, 0), null, 
                            reposRelPath, 
                            reposInfo.reposRootUrl, 
                            reposInfo.reposUuid, 
                            copyFromRev, 
                            null, 
                            SVNDepth.INFINITY, 
                            null, 
                            null);
                } else {
                    getContext().getDb().opAddDirectory(path, null);
                }
            }
            result.contentState = SVNStatusType.CHANGED;
        } else if (os.kind == SVNNodeKind.DIR) {
            if (!isVersioned || os.deleted) {
                if (!isDryRun()) {
                    if (copyFromUrl != null) {
                        SVNWCNodeReposInfo reposInfo = getContext().getNodeReposInfo(parentPath);
                        File reposRelPath = new File(SVNURLUtil.getRelativeURL(reposInfo.reposRootUrl, copyFromUrl, false));
                        getContext().getDb().opCopyDir(path, new SVNProperties(), 
                                copyFromRev, new SVNDate(0, 0), null, 
                                reposRelPath, 
                                reposInfo.reposRootUrl, 
                                reposInfo.reposUuid, 
                                copyFromRev, 
                                null, 
                                SVNDepth.INFINITY, 
                                null, 
                                null);
                    } else {
                        getContext().getDb().opAddDirectory(path, null);
                    }
                } else {
                    setAddedPath(path);
                }
                result.contentState = SVNStatusType.CHANGED;
            } else {
                if (driver.isDryRunDeletion(path)) {
                    result.contentState = SVNStatusType.CHANGED;
                } else {
                    treeConflictOnAdd(path, SVNNodeKind.DIR, SVNConflictAction.ADD, SVNConflictReason.ADDED);
                    result.treeConflicted = true;
                    result.contentState = SVNStatusType.OBSTRUCTED;
                }
            }
        } else if (os.kind == SVNNodeKind.FILE) {
            if (isDryRun()) {
                setAddedPath(null);
            }
            if (isVersioned && driver.isDryRunDeletion(path)) {
                result.contentState = SVNStatusType.CHANGED;
            } else {
                treeConflictOnAdd(path, SVNNodeKind.DIR, SVNConflictAction.ADD, SVNConflictReason.OBSTRUCTED);
                result.treeConflicted = true;
                result.contentState = SVNStatusType.OBSTRUCTED;
            }
        } else {
            if (isDryRun()) {
                setAddedPath(null);
            }
            result.contentState = SVNStatusType.UNKNOWN;
        }
    }

    public void dirPropsChanged(SvnDiffCallbackResult result, File path, boolean isAdded, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException {
        ObstructionState os = driver.performObstructionCheck(path, SVNNodeKind.DIR);
        if (os.obstructionState != SVNStatusType.INAPPLICABLE) {
            result.propState = os.obstructionState;
            return;
        }
        if (isAdded && isDryRun() && driver.isDryRunAddition(path)) {
            return;
        }
        MergePropertiesInfo info = mergePropChanges(path, propChanges, originalProperties);
        result.treeConflicted = info != null ? info.treeConflicted : false;
        result.propState = info != null ? info.mergeOutcome : null;
    }

    public void dirClosed(SvnDiffCallbackResult result, File path, boolean isAdded) throws SVNException {
        if (isDryRun() && getDryRunDeletions() != null) {
            getDryRunDeletions().clear();
        }
    }
    
    private void checkReposMatch(File path, SVNURL url) throws SVNException {
        if (!SVNURLUtil.isAncestor(getReposRootURL(), url)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Url ''{0}'' of ''{1}'' is not in repository ''{2}''", url, path, getReposRootURL());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }
    
    private MergePropertiesInfo mergePropChanges(File localAbsPath, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException {
        SVNProperties props = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(propChanges, props, null, null);
        
        if (isRecordOnly() && !props.isEmpty()) {
            SVNProperties mergeinfoProps = new SVNProperties();
            if (props.containsName(SVNProperty.MERGE_INFO)) {
                mergeinfoProps.put(SVNProperty.MERGE_INFO, props.getStringValue(SVNProperty.MERGE_INFO));
            }
            props = mergeinfoProps;
        }
        
        MergePropertiesInfo mergeOutcome = null;
        if (!props.isEmpty()) {
            if (getSource1Rev() < getSource2Rev() || !areSourcesAncestral()) {
                props = filterSelfReferentialMergeInfo(props, localAbsPath, isHonorMergeInfo(), isSameRepos(), isReintegrateMerge(), getRepos2());
            }
            SVNException err = null;
            try {
                mergeOutcome = getContext().mergeProperties(localAbsPath, null, null, originalProperties, props, isDryRun());
            } catch (SVNException e) {
                err = e;
            }
            
            if (!isDryRun()) {
                for (String propName : props.nameSet()) {
                    if (!SVNProperty.MERGE_INFO.equals(propName)) {
                        continue;
                    }
                    SVNProperties pristineProps = getContext().getPristineProps(localAbsPath);
                    boolean hasPristineMergeInfo = false;
                    if (pristineProps != null && pristineProps.containsName(SVNProperty.MERGE_INFO)) {
                        hasPristineMergeInfo = true;
                    }
                    if (!hasPristineMergeInfo && props.getSVNPropertyValue(propName) != null) {
                        addPathWithAddedMergeInfo(localAbsPath);
                    } else if (hasPristineMergeInfo && props.getSVNPropertyValue(propName) == null) {
                        addPathWithDeletedMergeInfo(localAbsPath);
                    }
                }
            }
            
            if (err != null && (err.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND ||
                    err.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_UNEXPECTED_STATUS)) {
                if (mergeOutcome != null) {
                    mergeOutcome.mergeOutcome = SVNStatusType.MISSING;
                    mergeOutcome.treeConflicted = true;
                }
            } else if (err != null) {
                throw err;
            }
        }
        return mergeOutcome;
    }

    private void addPathWithAddedMergeInfo(File localAbsPath) {
        if (driver.pathsWithNewMergeInfo == null) {
            driver.pathsWithNewMergeInfo = new HashSet<File>();
        }
        driver.pathsWithNewMergeInfo.add(localAbsPath);
    }

    private void addPathWithDeletedMergeInfo(File localAbsPath) {
        if (driver.pathsWithDeletedMergeInfo == null) {
            driver.pathsWithDeletedMergeInfo = new HashSet<File>();
        }
        driver.pathsWithDeletedMergeInfo.add(localAbsPath);
    }

    private SVNProperties filterSelfReferentialMergeInfo(SVNProperties props, File localAbsPath, boolean honorMergeInfo, boolean sameRepos,
            boolean reintegrateMerge, SVNRepository repos) throws SVNException {
        if (!sameRepos) {
            return omitMergeInfoChanges(props);
        }
        if (!honorMergeInfo && !reintegrateMerge) {
            return props;
        }
        
        boolean isAdded = getContext().isNodeAdded(localAbsPath);
        if (isAdded) {
            return props;
        }
        long baseRevision = getContext().getNodeBaseRev(localAbsPath);        
        SVNProperties adjustedProps = new SVNProperties();
        
        for (String propName : props.nameSet()) {
            if (!SVNProperty.MERGE_INFO.equals(propName) || props.getSVNPropertyValue(propName) == null || "".equals(props.getSVNPropertyValue(propName))) {
                adjustedProps.put(propName, props.getSVNPropertyValue(propName));
                continue;
            }
            SVNURL targetUrl = getContext().getUrlFromPath(localAbsPath);
            SVNURL oldUrl = repos.getLocation();
            repos.setLocation(targetUrl, false);
            String mi = props.getStringValue(propName);
            
            Map<String, SVNMergeRangeList> mergeinfo = null;
            Map<String, SVNMergeRangeList> filteredYoungerMergeinfo = null;
            Map<String, SVNMergeRangeList> filteredMergeinfo = null;
            
            try {
                mergeinfo = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(mi), null);
            } catch (SVNException e) {
                adjustedProps.put(propName, props.getSVNPropertyValue(propName));
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                    repos.setLocation(oldUrl, false);
                    continue;
                }
                throw e;
            }
            Map<String, SVNMergeRangeList>[] splitted = splitMergeInfoOnRevision(mergeinfo, baseRevision);
            Map<String, SVNMergeRangeList> youngerMergeInfo = splitted[0];
            mergeinfo = splitted[1];
            
            if (youngerMergeInfo != null) {
                SVNURL mergeSourceRootUrl = repos.getRepositoryRoot(true);
                
                for (Iterator<String> youngerMergeInfoIter = youngerMergeInfo.keySet().iterator(); youngerMergeInfoIter.hasNext();) {
                    String sourcePath = youngerMergeInfoIter.next();
                    SVNMergeRangeList rangeList = (SVNMergeRangeList) youngerMergeInfo.get(sourcePath);
                    SVNMergeRange ranges[] = rangeList.getRanges();
                    List<SVNMergeRange> adjustedRanges = new ArrayList<SVNMergeRange>();
                    
                    SVNURL mergeSourceURL = mergeSourceRootUrl.appendPath(sourcePath, false);
                    for (int i = 0; i < ranges.length; i++) {
                        SVNMergeRange range = ranges[i];
                        Structure<LocationsInfo> locations = null;
                        try {
                            locations = new SvnNgRepositoryAccess(null, getContext()).getLocations(
                                    repos, 
                                    SvnTarget.fromURL(targetUrl), 
                                    SVNRevision.create(baseRevision), 
                                    SVNRevision.create(range.getStartRevision() + 1), 
                                    SVNRevision.UNDEFINED);
                            SVNURL startURL = locations.get(LocationsInfo.startUrl);
                            if (!mergeSourceURL.equals(startURL)) {
                                adjustedRanges.add(range);
                            }
                            locations.release();
                        } catch (SVNException svne) {
                            SVNErrorCode code = svne.getErrorMessage().getErrorCode();
                            if (code == SVNErrorCode.CLIENT_UNRELATED_RESOURCES || 
                                    code == SVNErrorCode.RA_DAV_PATH_NOT_FOUND ||
                                    code == SVNErrorCode.FS_NOT_FOUND ||
                                    code == SVNErrorCode.FS_NO_SUCH_REVISION) {
                                adjustedRanges.add(range);
                            } else {
                                throw svne;
                            }
                        }
                    }

                    if (!adjustedRanges.isEmpty()) {
                        if (filteredYoungerMergeinfo == null) {
                            filteredYoungerMergeinfo = new TreeMap<String, SVNMergeRangeList>();
                        }
                        SVNMergeRangeList adjustedRangeList = SVNMergeRangeList.fromCollection(adjustedRanges); 
                        filteredYoungerMergeinfo.put(sourcePath, adjustedRangeList);
                    }
                }
            }
            if (mergeinfo != null && !mergeinfo.isEmpty()) {
                
                Map<String, SVNMergeRangeList> implicitMergeInfo = 
                        getRepositoryAccess().getHistoryAsMergeInfo(getRepos2(), SvnTarget.fromFile(localAbsPath),  
                                baseRevision, -1);                         
                filteredMergeinfo = SVNMergeInfoUtil.removeMergeInfo(implicitMergeInfo, mergeinfo, true);
            }
            
            if (oldUrl != null) {
                repos.setLocation(oldUrl, false);
            }
            
            if (filteredMergeinfo != null && filteredYoungerMergeinfo != null) {
                filteredMergeinfo = SVNMergeInfoUtil.mergeMergeInfos(filteredMergeinfo, filteredYoungerMergeinfo);
            } else if (filteredYoungerMergeinfo != null) {
                filteredMergeinfo = filteredYoungerMergeinfo;
            }

            if (filteredMergeinfo != null && !filteredMergeinfo.isEmpty()) {
                String filteredMergeInfoStr = SVNMergeInfoUtil.formatMergeInfoToString(filteredMergeinfo, null);
                adjustedProps.put(SVNProperty.MERGE_INFO, filteredMergeInfoStr);
            }
        }
        
        return adjustedProps;
    }

    private Map<String, SVNMergeRangeList>[] splitMergeInfoOnRevision(Map<String, SVNMergeRangeList> mergeinfo, long revision) {
        Map<String, SVNMergeRangeList> youngerMergeinfo = null;
        for (String path : new HashSet<String>(mergeinfo.keySet())) {
            SVNMergeRangeList rl = mergeinfo.get(path);
            for (int i = 0; i < rl.getSize(); i++) {
                SVNMergeRange r = rl.getRanges()[i];
                if (r.getEndRevision() <= revision) {
                    continue;
                } else {
                    SVNMergeRangeList youngerRl = new SVNMergeRangeList(new SVNMergeRange[0]);
                    for (int j = 0; j < rl.getSize(); j++) {
                        SVNMergeRange r2 = rl.getRanges()[j];
                        SVNMergeRange youngerRange = r2.dup();
                        if (i == j && r.getStartRevision() + 1 <= revision) {
                            youngerRange.setStartRevision(revision);
                            r.setEndRevision(revision);
                        }
                        youngerRl.pushRange(youngerRange.getStartRevision(), youngerRange.getEndRevision(), youngerRange.isInheritable());
                    }
                    
                    if (youngerMergeinfo == null) {
                        youngerMergeinfo = new TreeMap<String, SVNMergeRangeList>();
                    }
                    youngerMergeinfo.put(path, youngerRl);
                    mergeinfo = SVNMergeInfoUtil.removeMergeInfo(youngerMergeinfo, mergeinfo, true);
                    break;
                }
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, SVNMergeRangeList>[] result = new Map[2]; 
        result[0] = youngerMergeinfo;
        result[1] = mergeinfo;
        return result;
    }

    private SVNProperties omitMergeInfoChanges(SVNProperties props) {
        SVNProperties result = new SVNProperties();
        for (String name : props.nameSet()) {
            if (SVNProperty.MERGE_INFO.equals(name)) {
                continue;
            }
            SVNPropertyValue pv = props.getSVNPropertyValue(name);
            result.put(name, pv);
        }
        return result;
    }

    private boolean isHonorMergeInfo() {
        return driver.isHonorMergeInfo();
    }

    private SVNConflictVersion[] makeConflictVersions(File target, SVNNodeKind kind) throws SVNException {
        SVNURL srcReposUrl = getRepos1().getRepositoryRoot(true);
        String child = SVNWCUtils.getPathAsChild(getTargetPath(), target);
        SVNURL leftUrl;
        SVNURL rightUrl;
        if (child != null) {
            leftUrl = getSource1URL().appendPath(child, false);
            rightUrl = getSource2URL().appendPath(child, false);
        } else {
            leftUrl = getSource1URL();
            rightUrl = getSource2URL();
        }
        String leftPath = SVNWCUtils.isChild(srcReposUrl, leftUrl);
        String rightPath = SVNWCUtils.isChild(srcReposUrl, rightUrl);
        SVNConflictVersion lv = new SVNConflictVersion(srcReposUrl, leftPath, getSource1Rev(), kind);
        SVNConflictVersion rv = new SVNConflictVersion(srcReposUrl, rightPath, getSource2Rev(), kind);
        
        return new SVNConflictVersion[] {lv, rv};
    }
    
    private void treeConflictOnAdd(File path, SVNNodeKind kind, SVNConflictAction action, SVNConflictReason reason) throws SVNException {
        if (isRecordOnly() || isDryRun()) {
            return;
        }
        SVNTreeConflictDescription tc = makeTreeConflict(path, kind, action, reason);
        SVNTreeConflictDescription existingTc = getContext().getTreeConflict(path);
        
        if (existingTc == null) {
            getContext().getDb().opSetTreeConflict(path, tc);
            if (conflictedPaths == null) {
                conflictedPaths = new HashSet<File>();
            }
            conflictedPaths.add(path);
        } else if (existingTc.getConflictAction() == SVNConflictAction.DELETE && tc.getConflictAction() == SVNConflictAction.ADD) {
            existingTc.setConflictAction(SVNConflictAction.REPLACE);
            getContext().getDb().opSetTreeConflict(path, existingTc);
        }
    }
    
    private SVNTreeConflictDescription makeTreeConflict(File path, SVNNodeKind kind, SVNConflictAction action, SVNConflictReason reason) throws SVNException {
        final SVNConflictVersion[] cvs = makeConflictVersions(path, kind);
        final SVNTreeConflictDescription tc = new SVNTreeConflictDescription(path, kind, action, reason, SVNOperation.MERGE, cvs[0], cvs[1]);
        return tc;
    }
    
    private void treeConflict(File path, SVNNodeKind kind, SVNConflictAction action, SVNConflictReason reason) throws SVNException {
        if (isRecordOnly() || isDryRun()) {
            return;
        }
        SVNTreeConflictDescription tc = getContext().getTreeConflict(path);
        if (tc == null) {
            tc = makeTreeConflict(path, kind, action, reason);
            getContext().getDb().opSetTreeConflict(path, tc);
            
            if (conflictedPaths == null) {
                conflictedPaths = new HashSet<File>();
            }
            conflictedPaths.add(path);
        }
    }

    private boolean compareProps(SVNProperties p1, SVNProperties p2) throws SVNException {
        if (p1 == null || p2 == null) {
            return p1 == p2;
        }
        SVNProperties diff = p1.compareTo(p2);
        for (String propName : diff.nameSet()) {
            if (SVNProperty.isRegularProperty(propName) && !SVNProperty.MERGE_INFO.equals(propName)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean compareFiles(File oldPath, SVNProperties oldProps, File minePath) throws SVNException {
        SVNProperties workingProperties = getContext().getActualProps(minePath);
        boolean same = compareProps(oldProps, workingProperties);
        if (same) {
            InputStream is = null;
            InputStream old = null;
            try {
                if (workingProperties != null && workingProperties.getStringValue(SVNProperty.SPECIAL) != null) {
                    is = SVNFileUtil.readSymlink(minePath);
                } else {
                    is = getContext().getTranslatedStream(minePath, minePath, true, false);
                }
                old = SVNFileUtil.openFileForReading(oldPath);
                same = SVNFileUtil.compare(is, old);
            } finally {
                SVNFileUtil.closeFile(is);
                SVNFileUtil.closeFile(old);
            }
        }
        return same;
    }
    
    private SVNWCContext getContext() {
        return driver.context;
    }
    
    private boolean isReintegrateMerge() {
        return driver.reintegrateMerge;
    }
    
    private boolean isRecordOnly() {
        return driver.recordOnly;
    }
    
    private boolean isDryRun() {
        return driver.dryRun;
    }
    
    private boolean isForce() {
        return driver.force;
    }

    private boolean isSameRepos() {
        return driver.sameRepos;
    }
    
    private SVNDiffOptions getDiffOptions() {
        return driver.diffOptions;
    }
    
    private File getAddedPath() {
        return driver.addedPath;
    }
    
    private void setAddedPath(File path) {
        driver.addedPath = path;
    }
    
    private boolean areSourcesAncestral() {
        return driver.sourcesAncestral;
    }
    
    private File getTargetPath() {
        return driver.targetAbsPath;
    }
    
    private SVNRepository getRepos1() {
        return driver.repos1;
    }

    private SVNRepository getRepos2() {
        return driver.repos2;
    }
    
    private SVNURL getReposRootURL() {
        return driver.reposRootUrl;
    }
    
    private SvnRepositoryAccess getRepositoryAccess() {
        return driver.repositoryAccess;
    }
    
    private SVNURL getSource1URL() {
        return driver.source.url1;
    }
    private SVNURL getSource2URL() {
        return driver.source.url2;
    }
    private long getSource1Rev() {
        return driver.source.rev1;
    }
    private long getSource2Rev() {
        return driver.source.rev2;
    }
    
    private Collection<File> getDryRunDeletions() {
        return driver.dryRunDeletions;
    }
    private Collection<File> getDryRunAdditions() {
        return driver.dryRunAdded;
    }

}
