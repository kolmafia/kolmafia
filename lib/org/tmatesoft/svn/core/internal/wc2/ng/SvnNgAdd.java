package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.ConflictInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbRevert;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.*;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgAdd extends SvnNgOperationRunner<Void, SvnScheduleForAddition> {

	@Override
	protected Void run(SVNWCContext context) throws SVNException {
		final Map<File, List<SvnTarget>> rootToTargets = new HashMap<File, List<SvnTarget>>();
		for (SvnTarget target : getOperation().getTargets()) {
		    final File targetFile = target.getFile();
		    final File root;
		    final SVNFileType targetType = SVNFileType.getType(target.getFile());
		    
		    if (targetType == SVNFileType.FILE || !getWcContext().getDb().isWCRoot(targetFile, true)) {
	            File parentPath = SVNFileUtil.getParentFile(targetFile);
	            if (getOperation().isAddParents()) {
	                parentPath = findExistingParent(parentPath);
	            }
	            root = getWcContext().getDb().getWCRoot(parentPath);
	        } else {
	            root = targetFile;
	        }
		    
			List<SvnTarget> targets = rootToTargets.get(root);
			if (targets == null) {
				targets = new ArrayList<SvnTarget>();
				rootToTargets.put(root, targets);
			}

			targets.add(target);
		}

		for (File root : rootToTargets.keySet()) {
			final List<SvnTarget> targets = rootToTargets.get(root);
			final File lockRoot = getWcContext().acquireWriteLock(root, false, true);
			try {
				for (SvnTarget target : targets) {
					add(target);
				}
			}
			finally {
				getWcContext().releaseWriteLock(lockRoot);
			}
		}

		return null;
	}

	private void add(SvnTarget target) throws SVNException {
        if (target.isURL()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a local path", target.getURL());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        File path = target.getFile();
        File existingParent = path;
        File parentPath = path;
        if (!getWcContext().getDb().isWCRoot(path, true)) {
            parentPath = SVNFileUtil.getParentFile(path);
            existingParent = parentPath;
            if (getOperation().isAddParents()) {
                existingParent = findExistingParent(parentPath);
            }
        }
        SVNFileType targetType = SVNFileType.getType(path);
        
        if (!getOperation().isAddParents() && getOperation().isMkDir() && targetType != SVNFileType.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Can''t create directory " +
            		"''{0}'': Cannot create a file when that file already exists.", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        } else if (getOperation().isMkDir() && getOperation().isAddParents()) {
            SVNFileUtil.ensureDirectoryExists(path);
        } else if (targetType == SVNFileType.NONE && getOperation().isMkDir()) {
            SVNFileUtil.ensureDirectoryExists(path);
        }

        try {
            add(path, parentPath, existingParent);
        } catch (SVNException e) {
            if (targetType == SVNFileType.NONE) {
                SVNFileUtil.deleteAll(path, true);
            }
            throw e;
        }
    }

    private void add(File path, File parentPath, File existingParentPath) throws SVNException {
        if (!existingParentPath.equals(parentPath)) {
            String parent = parentPath.getAbsolutePath().replace(File.separatorChar, '/');
            String existingParent = existingParentPath.getAbsolutePath().replace(File.separatorChar, '/');
            String relativeChildPath = SVNPathUtil.getRelativePath(existingParent, parent);
            parentPath = existingParentPath;
            for(StringTokenizer components = new StringTokenizer(relativeChildPath, "/"); components.hasMoreTokens();) {
                String component = components.nextToken();
                checkCancelled();

                parentPath = SVNFileUtil.createFilePath(parentPath, component);
                SVNFileType pathType = SVNFileType.getType(parentPath);
                if (pathType != SVNFileType.NONE && pathType != SVNFileType.DIRECTORY) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NO_VERSIONED_PARENT, 
                            "''{0}'' prevents creating of '''{1}''", parentPath, path);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                SVNFileUtil.ensureDirectoryExists(parentPath);
                addFromDisk(parentPath, null, true);
            }
        }
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path));
        try {
            if (kind == SVNNodeKind.DIR) {
                addDirectory(path, getOperation().getDepth(), !getOperation().isIncludeIgnored());
            } else if (kind == SVNNodeKind.FILE) {
                addFile(path);
            } else if (kind == SVNNodeKind.NONE) {
                ConflictInfo conflictInfo = null;
                try {
                    conflictInfo = getWcContext().getConflicted(path, false, false, true);
                } catch (SVNException e) {                    
                }
                if (conflictInfo != null && conflictInfo.treeConflicted)  {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, 
                            "''{0}'' is an existing item in conflict; please mark the conflict as resolved before adding a new item here", 
                            path);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, 
                        "''{0}'' not found", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
                        "Unsupported node kind for path ''{0}''", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } catch (SVNException e) {
            if (!(getOperation().isForce() && e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_EXISTS)) {
                throw e;
            }
        }
        
    }

    private void addFile(final File path) throws SVNException {
        final boolean special = SVNFileType.getType(path) == SVNFileType.SYMLINK;
        SVNProperties properties = null;
        
        if (!special) {
            if (getOperation().isApplyAutoProperties()) {
                final Map<?, ?> autoProps = getAllAutoProperties(getOperation().getOptions(), path);
                if (autoProps != null && !autoProps.isEmpty()) {
                    properties = SVNProperties.wrap(autoProps);
                }
            }
        } else {
            properties = new SVNProperties();
            properties.put(SVNProperty.SPECIAL, "*");
        }
        addFromDisk(path, null, false);
        if (properties != null) {
            final ISvnAddParameters addParameters = getOperation().getAddParameters() == null ?
                    ISvnAddParameters.DEFAULT :
                    getOperation().getAddParameters();
            SvnNgPropertiesManager.setAutoProperties(getWcContext(), path, properties, addParameters, new Runnable() {
                public void run() {
                    doRevert(path);
                }
                
            });
        }
        handleEvent(SVNEventFactory.createSVNEvent(path, 
                SVNNodeKind.FILE, 
                properties != null ?
                properties.getStringValue(SVNProperty.MIME_TYPE) : null, -1, 
                SVNEventAction.ADD, 
                SVNEventAction.ADD, 
                null, null, 1, 1));
    }

    private Map getAllAutoProperties(ISVNOptions options, File file) throws SVNException {
        Map<String, String> allAutoProperties = new HashMap<String, String>();
        Map configAutoProperties = SVNPropertiesManager.computeAutoProperties(options, file, null);
        if (configAutoProperties != null) {
            allAutoProperties.putAll(configAutoProperties);
        }

        SVNProperties regularProperties;
        SVNRevision revision = SVNRevision.WORKING;

        File parentFile = SVNFileUtil.getParentFile(file);

        final List<SvnInheritedProperties>[] inheritedConfigAutoProperties = new List[1];
        do {
            SvnGetProperties getProperties = getOperation().getOperationFactory().createGetProperties();
            getProperties.setSingleTarget(SvnTarget.fromFile(parentFile, revision));
            getProperties.setRevision(revision);
            getProperties.setDepth(SVNDepth.EMPTY);
            getProperties.setTargetInheritedPropertiesReceiver(new ISvnObjectReceiver<List<SvnInheritedProperties>>() {
                public void receive(SvnTarget target, List<SvnInheritedProperties> inheritedProperties) throws SVNException {
                    inheritedConfigAutoProperties[0] = inheritedProperties;
                }
            });
            try {
                regularProperties = getProperties.run();
                break;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.UNVERSIONED_RESOURCE) {
                    throw e;
                }
                parentFile = findExistingParent(parentFile);
            }
        } while (true);

        if (inheritedConfigAutoProperties[0] != null) {
            for (SvnInheritedProperties inheritedConfigAutoProperty : inheritedConfigAutoProperties[0]) {
                SVNProperties inheritedProperties = inheritedConfigAutoProperty.getProperties();
                Map<String, SVNPropertyValue> inheritedPropertiesMap = inheritedProperties.asMap();
                for (Map.Entry<String, SVNPropertyValue> entry : inheritedPropertiesMap.entrySet()) {
                    String propertyName = entry.getKey();
                    if (!SVNProperty.INHERITABLE_AUTO_PROPS.equals(propertyName)) {
                        continue;
                    }
                    SVNPropertyValue propertyValue = entry.getValue();
                    allAutoProperties.putAll(SvnNgPropertiesManager.getMatchedAutoProperties(file.getName(), SvnNgPropertiesManager.parseAutoProperties(propertyValue, null)));
                }
            }
        }
        if (regularProperties != null) {
            Map<String, SVNPropertyValue> regularPropertiesMap = regularProperties.asMap();
            for (Map.Entry<String, SVNPropertyValue> entry : regularPropertiesMap.entrySet()) {
                String propertyName = entry.getKey();
                if (!SVNProperty.INHERITABLE_AUTO_PROPS.equals(propertyName)) {
                    continue;
                }
                SVNPropertyValue propertyValue = entry.getValue();
                allAutoProperties.putAll(SvnNgPropertiesManager.getMatchedAutoProperties(file.getName(), SvnNgPropertiesManager.parseAutoProperties(propertyValue, null)));
            }
        }

        return allAutoProperties;
    }

    private void doRevert(File path) {
        try {
            try {
                getWcContext().getDb().opRevert(path, SVNDepth.EMPTY);
                SvnNgRevert.restore(getWcContext(), path, SVNDepth.EMPTY, false, true, null);
            } finally {
                SvnWcDbRevert.dropRevertList(getWcContext(), path);
            }
        } catch (SVNException svne) {
            //
        } 
    }

    private void addDirectory(File path, SVNDepth depth, boolean refreshIgnores) throws SVNException {
        boolean entryExists = false;

        checkCancelled();
        try {
            addFromDisk(path, null, true);
        } catch (SVNException e) {
            if (!(getOperation().isForce() && e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_EXISTS)) {
                throw e;
            }
            entryExists = true;
        }
        if (depth.compareTo(SVNDepth.EMPTY) <= 0) {
            return;
        }
        Collection<String> ignorePatterns = null;
        if (refreshIgnores) {
            ignorePatterns = SvnNgPropertiesManager.getEffectiveIgnores(getWcContext(), path, null);
        }

        File[] children = SVNFileListUtil.listFiles(path);
        for (int i = 0; children != null && i < children.length; i++) {
            checkCancelled();
            String name = children[i].getName();
            if (name.equals(SVNFileUtil.getAdminDirectoryName())) {
                continue;
            }
            if (ignorePatterns != null && SvnNgPropertiesManager.isIgnored(name, ignorePatterns)) {
                continue;
            }
            SVNNodeKind childKind = SVNFileType.getNodeKind(SVNFileType.getType(children[i]));
            if (childKind == SVNNodeKind.DIR && depth.compareTo(SVNDepth.IMMEDIATES) >= 0) {
                SVNDepth depthBelow = depth;
                if (depth == SVNDepth.IMMEDIATES) {
                    depthBelow = SVNDepth.EMPTY;
                }
                if (refreshIgnores && !entryExists) {
                    refreshIgnores = false;
                }
                addDirectory(children[i], depthBelow, refreshIgnores);
            } else if (childKind == SVNNodeKind.FILE && depth.compareTo(SVNDepth.FILES) >= 0) {
                try {
                    addFile(children[i]);
                } catch (SVNException e) {
                    if (!(getOperation().isForce() && e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_EXISTS)) {
                        throw e;
                    }
                }
            }
        }
    }

    public void addFromDisk(File path, SVNProperties props, boolean fireEvent) throws SVNException {
        SVNNodeKind kind = checkCanAddNode(path);
        checkCanAddtoParent(path);
        if (kind == SVNNodeKind.FILE) {
            SVNSkel workItem = null;

            if (props != null && (props.getSVNPropertyValue(SVNProperty.EXECUTABLE) != null || props.getSVNPropertyValue(SVNProperty.NEEDS_LOCK) != null)) {
                workItem = getWcContext().wqBuildSyncFileFlags(path);
            }

            getWcContext().getDb().opAddFile(path, props, workItem);

            if (workItem != null) {
                getWcContext().wqRun(path);
            }
        } else {
            getWcContext().getDb().opAddDirectory(path, props, null);
        }
        if (fireEvent) {
            handleEvent(SVNEventFactory.createSVNEvent(path, kind, null, -1, SVNEventAction.ADD, 
                    SVNEventAction.ADD, null, null, 1, 1));
        }
    }

    protected void add(File localAbsPath, SVNDepth depth, SVNURL copyFromUrl, long copyFromRevision, boolean fireEvent) throws SVNException {
        CheckCanAddNode checkCanAddNode = checkCanAddNode(localAbsPath, copyFromUrl, copyFromRevision);
        SVNNodeKind kind = checkCanAddNode.kind;
        boolean dbRowExists = checkCanAddNode.dbRowExists;
        boolean isWcRoot = checkCanAddNode.isWcRoot;

        CheckCanAddToParent checkCanAddToParent = checkCanAddtoParent(localAbsPath);
        SVNURL reposRootUrl = checkCanAddToParent.reposRootUrl;
        String reposUuid = checkCanAddToParent.reposUuid;
        if (copyFromUrl != null && !SVNPathUtil.isAncestor(reposRootUrl.toString(), copyFromUrl.toString())) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "The URL ''{0}'' has a different repository root than its parent", copyFromUrl);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        if (isWcRoot) {
            ISVNWCDb.WCDbRepositoryInfo repositoryInfo = getWcContext().getDb().scanBaseRepository(localAbsPath, ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField.relPath, ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField.rootUrl, ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField.uuid);
            File reposRelPath = repositoryInfo.relPath;
            SVNURL innerReposRootUrl = repositoryInfo.rootUrl;
            String innerReposUuid = repositoryInfo.uuid;

            if (!innerReposUuid.equals(reposUuid) || !reposRootUrl.equals(innerReposRootUrl)) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                        "Can't schedule the working copy at ''{0}'' from repository ''{1}'' with uuid ''{2}'' " +
                                "for addition under a working copy from repository ''{3}'' with uuid ''{4}''.",
                        localAbsPath, innerReposRootUrl, innerReposUuid, reposRootUrl, reposUuid);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            SVNURL innerUrl = reposRootUrl.appendPath(SVNFileUtil.getFilePath(reposRelPath), false);

            if (!innerUrl.equals(copyFromUrl)) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Can't add ''{0}'' with URL ''{1}'', but with the data from ''{2}''", localAbsPath, copyFromUrl, innerUrl);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }

        if (copyFromUrl == null) {
            addFromDisk(localAbsPath, null, false);
            if (kind == SVNNodeKind.DIR && !dbRowExists) {
                boolean ownsLock = getWcContext().getDb().isWCLockOwns(localAbsPath, false);
                if (!ownsLock) {
                    getWcContext().getDb().obtainWCLock(localAbsPath, 0, false);
                }
            }
        } else if (!isWcRoot) {
            if (kind == SVNNodeKind.FILE) {
                SvnNgReposToWcCopy.addFileToWc(getWcContext(), localAbsPath, null, null, null, null, copyFromUrl, copyFromRevision);
            } else {
                File reposRelPath = SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(reposRootUrl.toDecodedString(), copyFromUrl.toDecodedString()));
                getWcContext().getDb().opCopyDir(localAbsPath, new SVNProperties(), copyFromRevision, SVNDate.NULL, null,
                        reposRelPath, reposRootUrl, reposUuid, copyFromRevision, null, false, null, null, null);
            }
        } else {
            integrateNestedWcAsCopy(localAbsPath);
        }

        if (fireEvent && getWcContext().getEventHandler() != null) {
            SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, kind, null, -1, SVNEventAction.ADD, SVNEventAction.ADD, null, null);
            getWcContext().getEventHandler().handleEvent(event, UNKNOWN);
        }
    }

    private CheckCanAddToParent checkCanAddtoParent(File localAbsPath) throws SVNException {
        File parentPath = SVNFileUtil.getParentFile(localAbsPath);
        getWcContext().writeCheck(parentPath);
        CheckCanAddToParent result = new CheckCanAddToParent();
        try {
            Structure<NodeInfo> info = getWcContext().getDb().readInfo(parentPath, NodeInfo.status, NodeInfo.kind, NodeInfo.reposRootUrl, NodeInfo.reposUuid);
            ISVNWCDb.SVNWCDbStatus status = info.<ISVNWCDb.SVNWCDbStatus>get(NodeInfo.status);
            ISVNWCDb.SVNWCDbKind kind = info.<ISVNWCDb.SVNWCDbKind>get(NodeInfo.kind);
            result.reposRootUrl = info.get(NodeInfo.reposRootUrl);
            result.reposUuid = info.get(NodeInfo.reposUuid);
            if (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.ServerExcluded) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND,
                        "Can''t find parent directory''s node while trying to add ''{0}''", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (status == SVNWCDbStatus.Deleted) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_SCHEDULE_CONFLICT,
                        "Can''t add ''{0}'' to a parent directory scheduled for deletion", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            } else if (kind != SVNWCDbKind.Dir) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_SCHEDULE_CONFLICT,
                        "Can''t schedule an addition of ''{0}'' below a not-directory node", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (result.reposRootUrl == null || result.reposUuid == null) {
                if (status == SVNWCDbStatus.Added) {
                    ISVNWCDb.WCDbAdditionInfo additionInfo = getWcContext().getDb().scanAddition(parentPath, ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.reposRootUrl, ISVNWCDb.WCDbAdditionInfo.AdditionInfoField.reposUuid);
                    result.reposRootUrl = additionInfo.reposRootUrl;
                    result.reposUuid = additionInfo.reposUuid;
                } else {
                    ISVNWCDb.WCDbRepositoryInfo repositoryInfo = getWcContext().getDb().scanBaseRepository(parentPath, ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField.rootUrl, ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField.uuid);
                    result.reposRootUrl = repositoryInfo.rootUrl;
                    result.reposUuid = repositoryInfo.uuid;
                }
            }
            info.release();
        } catch (SVNException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND,
                    "Can''t find parent directory''s node while trying to add ''{0}''", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return result;
    }

    private CheckCanAddNode checkCanAddNode(File localAbsPath, SVNURL copyFromUrl, long copyFromRevision) throws SVNException {
        String name = SVNFileUtil.getFileName(localAbsPath);

        assert SVNFileUtil.isAbsolute(localAbsPath);
        assert (copyFromUrl == null || SVNRevision.isValidRevisionNumber(copyFromRevision));

        if (SVNFileUtil.getAdminDirectoryName().equals(name)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ENTRY_FORBIDDEN, "Can't create an entry with a reserved name while trying to add ''{0}''", localAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        SVNFileType pathType = SVNFileType.getType(localAbsPath);

        if (pathType == SVNFileType.NONE) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "''{0}'' not found", localAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        if (pathType == SVNFileType.UNKNOWN) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Unsupported node kind for ''{0}''", localAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        CheckCanAddNode result = new CheckCanAddNode();
        result.kind = SVNFileType.getNodeKind(pathType);

        try {
            Structure<NodeInfo> nodeInfo = getWcContext().getDb().readInfo(localAbsPath, true, NodeInfo.status, NodeInfo.conflicted);
            result.isWcRoot = false;
            result.dbRowExists = true;
            if (nodeInfo.is(NodeInfo.conflicted)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT,
                        "''{0}'' is an existing item in conflict; please mark the conflict as resolved before adding a new item here",
                        localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            switch(nodeInfo.<ISVNWCDb.SVNWCDbStatus>get(NodeInfo.status)) {
                case NotPresent:
                    break;
                case Deleted:
                    break;
                case Normal:
                    result.isWcRoot = getWcContext().getDb().isWCRoot(localAbsPath);
                    if (result.isWcRoot && copyFromUrl != null) {
                        break;
                    } else if (result.isWcRoot && pathType == SVNFileType.SYMLINK) {
                        break;
                    }
                    // only deal when copy from.
                default:
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS,
                            "''{0}'' is already under version control", localAbsPath);
                    SVNErrorManager.error(err, SVNLogType.WC);
            }
            nodeInfo.release();
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            result.dbRowExists = false;
            result.isWcRoot = false;
        }

        return result;
    }

    private SVNNodeKind checkCanAddNode(File path) throws SVNException {
        CheckCanAddNode checkCanAddNode = checkCanAddNode(path, null, -1);
        return checkCanAddNode.kind;
    }

    private File findExistingParent(File parentPath) throws SVNException {
        SVNNodeKind kind = getWcContext().readKind(parentPath, false);
        if (kind == SVNNodeKind.DIR) {
            if (!getWcContext().isNodeStatusDeleted(parentPath)) {
                return parentPath;
            }
        }
        if (parentPath.getParentFile() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NO_VERSIONED_PARENT);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (SVNFileUtil.getAdminDirectoryName().equals(SVNFileUtil.getFileName(parentPath))) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RESERVED_FILENAME_SPECIFIED,
                    "''{0}'' ends in a reserved name", parentPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        parentPath = SVNFileUtil.getParentFile(parentPath);
        checkCancelled();
        return findExistingParent(parentPath);
    }

    private void integrateNestedWcAsCopy(File localAbsPath) throws SVNException {
        getWcContext().getDb().dropRoot(localAbsPath);

        File tempDir = getWcContext().getDb().getWCRootTempDir(localAbsPath);
        File movedAbsPath = SVNFileUtil.createUniqueFile(tempDir, "", "", false);
        try {
            SVNFileUtil.ensureDirectoryExists(movedAbsPath);
            File admAbsPath = SVNFileUtil.createFilePath(localAbsPath, SVNFileUtil.getAdminDirectoryName());
            File movedAdmAbsPath = SVNFileUtil.createFilePath(movedAbsPath, SVNFileUtil.getAdminDirectoryName());
            SVNFileUtil.moveDir(admAbsPath, movedAdmAbsPath);

            SvnNgWcToWcCopy svnNgWcToWcCopy = new SvnNgWcToWcCopy();
            svnNgWcToWcCopy.copy(getWcContext(), movedAbsPath, localAbsPath, true);

            getWcContext().getDb().dropRoot(movedAbsPath);
        } finally {
            SVNFileUtil.deleteAll(movedAbsPath, null);
        }

        boolean ownsLock = getWcContext().getDb().isWCLockOwns(localAbsPath, false);
        if (!ownsLock) {
            getWcContext().getDb().obtainWCLock(localAbsPath, 0, false);
        }
    }

    private static class CheckCanAddNode {
        public SVNNodeKind kind;
        public boolean dbRowExists;
        public boolean isWcRoot;
    }

    private static class CheckCanAddToParent {
        public SVNURL reposRootUrl;
        public String reposUuid;
    }
}
