package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.List;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.ExternalNodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbExternals;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnScheduleForRemoval;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgRemove extends SvnNgOperationRunner<Void, SvnScheduleForRemoval> {

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        for(SvnTarget target : getOperation().getTargets()) {
            final File path = target.getFile();
            checkCancelled();            
            final File lockRoot = getWcContext().acquireWriteLock(path, true, true);
            try {
                if (!getOperation().isForce() && getOperation().isDeleteFiles()) {
                    checkCanDelete(getOperation().getOperationFactory(), context, path);
                }
                if (!getOperation().isDryRun()) {
                    delete(context, path, !getOperation().isDeleteFiles(), true, getOperation().getEventHandler());
                }
            } finally {
                getWcContext().releaseWriteLock(lockRoot);
            }
        }
        return null;
    }

    public static void checkCanDelete(SvnOperationFactory opFactory, SVNWCContext context, File path) throws SVNException {
        Structure<ExternalNodeInfo> info = null;
        try {
            info = SvnWcDbExternals.readExternal(context, path, path, ExternalNodeInfo.kind);
            if (info != null && info.get(ExternalNodeInfo.kind) == SVNWCDbKind.File) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CANNOT_DELETE_FILE_EXTERNAL,
                        "Cannot remove the external at ''{0}''; please edit or delete the svn:externals property on ''{1}''",
                        path, SVNFileUtil.getParentFile(path));
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
        } finally {
            if (info != null) {
                info.release();
            }
        }
        SvnGetStatus status = opFactory.createGetStatus();
        status.setSingleTarget(SvnTarget.fromFile(path));
        status.setDepth(SVNDepth.INFINITY);
        status.setReceiver(new ISvnObjectReceiver<SvnStatus>() {
            public void receive(SvnTarget target, SvnStatus status) throws SVNException {
                SVNErrorMessage err = null;
                if (status.getNodeStatus() == SVNStatusType.STATUS_OBSTRUCTED) {
                    err = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, 
                            "''{0}'' is in the way of the resource actually under version control", target.getFile());
                } else if (!status.isVersioned()) {
                    err = SVNErrorMessage.create(SVNErrorCode.UNVERSIONED_RESOURCE, 
                        "''{0}'' is not under version control", target.getFile());
                } else if ((status.getNodeStatus() != SVNStatusType.STATUS_NORMAL &&
                            status.getNodeStatus() != SVNStatusType.STATUS_DELETED &&
                            status.getNodeStatus() != SVNStatusType.STATUS_MISSING) ||
                            (status.getPropertiesStatus() != SVNStatusType.STATUS_NONE &&
                            status.getPropertiesStatus() != SVNStatusType.STATUS_NORMAL)) {
                    err = SVNErrorMessage.create(SVNErrorCode.CLIENT_MODIFIED, 
                    "''{0}'' has local modifications -- commit or revert them first", target.getFile());
                }
                if (err != null) {
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                
            }            
        });
        status.run();
    }

    public static void delete(SVNWCContext context, File path, boolean keepLocal, boolean deleteUnversioned, ISVNEventHandler handler) throws SVNException {
        Structure<NodeInfo> info = null;
        try {
            info = context.getDb().readInfo(path, NodeInfo.status, NodeInfo.kind, NodeInfo.conflicted);
        } catch (SVNException e) {
            if (deleteUnversioned && e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                if (SVNFileType.getType(path) == SVNFileType.NONE) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_FILENAME, "''{0}'' does not exist", path);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                if (!keepLocal) {
                    SVNFileUtil.deleteAll(path, handler);
                }
                return;
            }
            throw e;
        }
        SVNWCDbStatus status = info.get(NodeInfo.status);
        if (status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.NotPresent) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, 
                    "''{0}'' cannot be deleted", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (status == SVNWCDbStatus.Normal && info.get(NodeInfo.kind) == SVNWCDbKind.Dir) {
            if (context.getDb().isWCRoot(path)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, 
                        "''{0}'' is the root of a working copy and cannot be deleted", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        context.writeCheck(SVNFileUtil.getParentFile(path));
        List<SVNConflictDescription> conflicts = null;
        if (!keepLocal && info.is(NodeInfo.conflicted)) {
            conflicts = context.getDb().readConflicts(path);
        }
        info.release();

        context.getDb().opDelete(path, handler);
        if (!keepLocal && conflicts != null) {
            for (SVNConflictDescription conflict : conflicts) {
                if (conflict.isTextConflict()) {
                    if (conflict.getMergeFiles() != null) {
                        SVNFileUtil.deleteFile(conflict.getMergeFiles().getBaseFile());
                        SVNFileUtil.deleteFile(conflict.getMergeFiles().getRepositoryFile());
                        SVNFileUtil.deleteFile(conflict.getMergeFiles().getLocalFile());
                    }
                } else if (conflict.isPropertyConflict() && conflict.getMergeFiles() != null) {
                    SVNFileUtil.deleteFile(conflict.getMergeFiles().getRepositoryFile());
                }
            }
        }
        if (!keepLocal) {
            SVNFileUtil.deleteAll(path, handler);
        }
    }

}
