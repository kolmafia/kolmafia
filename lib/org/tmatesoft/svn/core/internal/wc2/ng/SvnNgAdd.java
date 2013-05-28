package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.ConflictInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbRevert;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.ISvnAddParameters;
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnTarget;
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
                addFromDisk(parentPath, true);
            }
        }
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path));
        try {
            if (kind == SVNNodeKind.DIR) {
                addDirectory(path, getOperation().getDepth());
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
            final Map<?, ?> autoProps = SVNPropertiesManager.computeAutoProperties(getOperation().getOptions(), path, null);
            if (autoProps != null && !autoProps.isEmpty()) {
                properties = SVNProperties.wrap(autoProps);
            }
        } else {
            properties = new SVNProperties();
            properties.put(SVNProperty.SPECIAL, "*");
        }
        addFromDisk(path, false);
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

    private void doRevert(File path) {
        try {
            try {
                getWcContext().getDb().opRevert(path, SVNDepth.EMPTY);
                SvnNgRevert.restore(getWcContext(), path, SVNDepth.EMPTY, false, null);
            } finally {
                SvnWcDbRevert.dropRevertList(getWcContext(), path);
            }
        } catch (SVNException svne) {
            //
        } 
    }

    private void addDirectory(File path, SVNDepth depth) throws SVNException {
        checkCancelled();
        try {
            addFromDisk(path, true);
        } catch (SVNException e) {
            if (!(getOperation().isForce() && e.getErrorMessage().getErrorCode() == SVNErrorCode.ENTRY_EXISTS)) {
                throw e;
            }
        }
        if (depth.compareTo(SVNDepth.EMPTY) <= 0) {
            return;
        }
        Collection<String> ignorePatterns = null;
        if (!getOperation().isIncludeIgnored()) {
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
                addDirectory(children[i], depthBelow);
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

    private void addFromDisk(File path, boolean fireEvent) throws SVNException {
        SVNNodeKind kind = checkCanAddNode(path);
        checkCanAddtoParent(path);
        if (kind == SVNNodeKind.FILE) {
            getWcContext().getDb().opAddFile(path, null);
        } else {
            getWcContext().getDb().opAddDirectory(path, null);
        }
        if (fireEvent) {
            handleEvent(SVNEventFactory.createSVNEvent(path, kind, null, -1, SVNEventAction.ADD, 
                    SVNEventAction.ADD, null, null, 1, 1));
        }
    }

    private void checkCanAddtoParent(File path) throws SVNException {
        File parentPath = SVNFileUtil.getParentFile(path);
        getWcContext().writeCheck(parentPath);
        try {
            Structure<NodeInfo> info = getWcContext().getDb().readInfo(parentPath, NodeInfo.status, NodeInfo.kind);
            ISVNWCDb.SVNWCDbStatus status = info.<ISVNWCDb.SVNWCDbStatus>get(NodeInfo.status);
            ISVNWCDb.SVNWCDbKind kind = info.<ISVNWCDb.SVNWCDbKind>get(NodeInfo.kind);
            if (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.ServerExcluded) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, 
                        "Can''t find parent directory''s node while trying to add ''{0}''", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (status == SVNWCDbStatus.Deleted) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_SCHEDULE_CONFLICT, 
                        "Can''t add ''{0}'' to a parent directory scheduled for deletion", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            } else if (kind != SVNWCDbKind.Dir) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_SCHEDULE_CONFLICT, 
                        "Can''t schedule an addition of ''{0}'' below a not-directory node", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            info.release();
        } catch (SVNException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND, 
                    "Can''t find parent directory''s node while trying to add ''{0}''", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    private SVNNodeKind checkCanAddNode(File path) throws SVNException {
        String name = SVNFileUtil.getFileName(path);
        if (SVNFileUtil.getAdminDirectoryName().equals(name)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_FORBIDDEN, 
                    "Can''t create an entry with a reserved name while trying to add ''{0}''", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNFileType pathType = SVNFileType.getType(path);
        if (pathType == SVNFileType.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, 
                    "''{0}'' not found", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (pathType == SVNFileType.UNKNOWN) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
                    "Unsupported node kind for ''{0}''", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        try {
            Structure<NodeInfo> nodeInfo = getWcContext().getDb().readInfo(path, true, NodeInfo.status, NodeInfo.conflicted);
            if (nodeInfo.is(NodeInfo.conflicted)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, 
                        "''{0}'' is an existing item in conflict; please mark the conflict as resolved before adding a new item here", 
                        path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            switch(nodeInfo.<ISVNWCDb.SVNWCDbStatus>get(NodeInfo.status)) {
            case NotPresent:
                break;
            case Deleted:
                break;
            case Normal:
                // only deal when copy from.
            default:
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_EXISTS, 
                        "''{0}'' is already under version control", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            nodeInfo.release();
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
        }
        
        return SVNFileType.getNodeKind(pathType);
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

}
