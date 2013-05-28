package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnMarkReplaced;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgMarkReplaced extends SvnNgOperationRunner<Void, SvnMarkReplaced> {

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        for (SvnTarget target : getOperation().getTargets()) {
            final File path = target.getFile();
            final File lock = getWcContext().acquireWriteLock(path, true, true);
            try {
                final ISVNWCDb db = context.getDb();

                final SVNWCContext.ScheduleInternalInfo schedule = context.getNodeScheduleInternal(path, true, false);
                final boolean alreadyReplaced = schedule.schedule == SVNWCContext.SVNWCSchedule.replace;

                if (!alreadyReplaced) {
                    doReplace(db, path);
                }
            } finally {
                getWcContext().releaseWriteLock(lock);
            }
        }
        return null;
    }

    private ISVNWCDb.SVNWCDbKind getKind(ISVNWCDb db, File path) throws SVNException {
        final Structure<StructureFields.NodeInfo> info = db.readInfo(path, StructureFields.NodeInfo.kind);
        final ISVNWCDb.SVNWCDbKind kind = info.get(StructureFields.NodeInfo.kind);
        info.release();
        return kind;
    }

    private void doReplace(ISVNWCDb db, File path) throws SVNException {
        if (db.isWCRoot(path)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS,
                    "''{0}'' is the root of a working copy and cannot be replaced", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        final List<File> deletedPaths = markDeletedRecursively(db, path);

        Collections.sort(deletedPaths);

        for (File deletedPath : deletedPaths) {
            markAdded(db, deletedPath); //deleted + added = replaced
        }
    }

    private void markAdded(ISVNWCDb db, File path) throws SVNException {
        final ISVNWCDb.SVNWCDbKind kind = getKind(db, path);
        if (kind == ISVNWCDb.SVNWCDbKind.Dir) {
            db.opAddDirectory(path, null);
        } else if (kind == ISVNWCDb.SVNWCDbKind.File) {
            db.opAddFile(path, null);
        }
        final SVNEvent event = SVNEventFactory.createSVNEvent(path,
                kind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, null, -1, SVNEventAction.ADD,
                SVNEventAction.ADD, null, null, 1, 1);
        handleEvent(event);
    }

    private List<File> markDeletedRecursively(ISVNWCDb db, File path) throws SVNException {
        final List<File> deletedPaths = new ArrayList<File>();
        final ISVNEventHandler eventHandler = getOperation().getEventHandler();

        db.opDelete(path, new ISVNEventHandler() {
            public void handleEvent(SVNEvent event, double progress) throws SVNException {
                if (event != null && event.getAction() == SVNEventAction.DELETE) {
                    deletedPaths.add(event.getFile());
                }
                if (eventHandler != null) {
                    eventHandler.handleEvent(event, progress);
                }
            }

            public void checkCancelled() throws SVNCancelException {
                if (eventHandler != null) {
                    eventHandler.checkCancelled();
                }
            }
        });
        return deletedPaths;
    }
}
