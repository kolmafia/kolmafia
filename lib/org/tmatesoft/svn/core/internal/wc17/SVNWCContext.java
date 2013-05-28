/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17;

import static org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.isAbsolute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.tmatesoft.sqljet.core.internal.SqlJetPagerJournalMode;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.FSMergerBySequence;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.internal.wc.SVNDiffConflictChoiceStyle;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNChecksumOutputStream;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbLock;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbOpenMode;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbAdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbAdditionInfo.AdditionInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbDeletionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbDeletionInfo.DeletionInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbWorkQueueInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.ReposInfo;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.AdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.DeletionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.PristineInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.WalkerChildInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbReader;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbReader.InstallInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldUpgrade;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNConflictHandler;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNMerger;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNConflictResult;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNMergeFileSet;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.core.wc2.ISvnMerger;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnMergeResult;
import org.tmatesoft.svn.util.SVNLogType;

import de.regnis.q.sequence.line.QSequenceLineRAByteData;
import de.regnis.q.sequence.line.QSequenceLineRAData;
import de.regnis.q.sequence.line.QSequenceLineRAFileData;

/**
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCContext {

    public static String CONFLICT_OP_UPDATE = "update";
    public static String CONFLICT_OP_SWITCH = "switch";
    public static String CONFLICT_OP_MERGE = "merge";
    public static String CONFLICT_OP_PATCH = "patch";

    public static String CONFLICT_KIND_TEXT = "text";
    public static String CONFLICT_KIND_PROP = "prop";
    public static String CONFLICT_KIND_TREE = "tree";
    public static String CONFLICT_KIND_REJECT = "reject";
    public static String CONFLICT_KIND_OBSTRUCTED = "obstructed";

    private static List<SVNStatusType> STATUS_ORDERING = new LinkedList<SVNStatusType>();
    static {
        STATUS_ORDERING.add(SVNStatusType.UNKNOWN);
        STATUS_ORDERING.add(SVNStatusType.UNCHANGED);
        STATUS_ORDERING.add(SVNStatusType.INAPPLICABLE);
        STATUS_ORDERING.add(SVNStatusType.CHANGED);
        STATUS_ORDERING.add(SVNStatusType.MERGED);
        STATUS_ORDERING.add(SVNStatusType.OBSTRUCTED);
        STATUS_ORDERING.add(SVNStatusType.CONFLICTED);
    }

    public static final long INVALID_REVNUM = -1;
    public static final int STREAM_CHUNK_SIZE = 16384;

    public static final String THIS_DIR_PREJ = "dir_conflicts";
    public static final String PROP_REJ_EXT = ".prej";

    public static final String CONFLICT_LOCAL_LABEL = "(modified)";
    public static final String CONFLICT_LATEST_LABEL = "(latest)";

    public static final byte[] CONFLICT_START = ("<<<<<<< " + CONFLICT_LOCAL_LABEL).getBytes();
    public static final byte[] CONFLICT_END = (">>>>>>> " + CONFLICT_LATEST_LABEL).getBytes();
    public static final byte[] CONFLICT_SEPARATOR = ("=======").getBytes();

    public static final int WC_NG_VERSION = 12;
    public static final int WC_WCPROPS_MANY_FILES_VERSION = 7;
    public static final int WC_WCPROPS_LOST = 12;
    
    public static final String WC_ADM_FORMAT = "format";
    public static final String WC_ADM_ENTRIES = "entries";
    public static final String WC_ADM_TMP = "tmp";
    public static final String WC_ADM_PRISTINE = "pristine";
    public static final String WC_ADM_NONEXISTENT_PATH = "nonexistent-path";
    public static final String WC_NON_ENTRIES_STRING = "12\n";

    public interface CleanupHandler {

        void cleanup() throws SVNException;
    }

    public static boolean isAdminDirectory(String name) {
        return name != null && (SVNFileUtil.isWindows) ? SVNFileUtil.getAdminDirectoryName().equalsIgnoreCase(name) : SVNFileUtil.getAdminDirectoryName().equals(name);
    }

    public static class SVNEolStyleInfo {

        public static final byte[] NATIVE_EOL_STR = System.getProperty("line.separator").getBytes();
        public static final byte[] LF_EOL_STR = {
            '\n'
        };
        public static final byte[] CR_EOL_STR = {
            '\r'
        };
        public static final byte[] CRLF_EOL_STR = {
                '\r', '\n'
        };

        public SVNEolStyle eolStyle;
        public byte[] eolStr;

        public SVNEolStyleInfo(SVNEolStyle style, byte[] str) {
            this.eolStyle = style;
            this.eolStr = str;
        }

        public static SVNEolStyleInfo fromValue(String value) {
            if (value == null) {
                /* property doesn't exist. */
                return new SVNEolStyleInfo(SVNEolStyle.None, null);
            } else if ("native".equals(value)) {
                return new SVNEolStyleInfo(SVNEolStyle.Native, NATIVE_EOL_STR);
            } else if ("LF".equals(value)) {
                return new SVNEolStyleInfo(SVNEolStyle.Fixed, LF_EOL_STR);
            } else if ("CR".equals(value)) {
                return new SVNEolStyleInfo(SVNEolStyle.Fixed, CR_EOL_STR);
            } else if ("CRLF".equals(value)) {
                return new SVNEolStyleInfo(SVNEolStyle.Fixed, CRLF_EOL_STR);
            } else {
                return new SVNEolStyleInfo(SVNEolStyle.Unknown, null);
            }
        }

    }

    public enum SVNEolStyle {
        /** An unrecognized style */
        Unknown,
        /** EOL translation is "off" or ignored value */
        None,
        /** Translation is set to client's native eol */
        Native,
        /** Translation is set to one of LF, CR, CRLF */
        Fixed
    }

    public interface RunWorkQueueOperation {

        void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException;
    }

    public static enum WorkQueueOperation {

        BASE_REMOVE("base-remove", new RunBaseRemove()),
        FILE_INSTALL("file-install", new RunFileInstall()),
        FILE_COMMIT("file-commit", new RunFileCommit()),        
        FILE_REMOVE("file-remove", new RunFileRemove()),
        FILE_MOVE("file-move", new RunFileMove()),
        FILE_COPY_TRANSLATED("file-translate", new RunFileTranslate()),
        SYNC_FILE_FLAGS("sync-file-flags", new RunSyncFileFlags()),
        PREJ_INSTALL("prej-install", new RunPrejInstall()),
        RECORD_FILEINFO("record-fileinfo", new RunRecordFileInfo()),
        TMP_SET_TEXT_CONFLICT_MARKERS("tmp-set-text-conflict-markers", new RunSetTextConflictMarkersTemp()),
        TMP_SET_PROPERTY_CONFLICT_MARKER("tmp-set-property-conflict-marker", new RunSetPropertyConflictMarkerTemp()),
        POSTUPGRADE("postupgrade", new RunPostUpgrade());

        private final String opName;
        private final RunWorkQueueOperation operation;

        private WorkQueueOperation(String opName, RunWorkQueueOperation operation) {
            this.opName = opName;
            this.operation = operation;
        }

        public String getOpName() {
            return opName;
        }

        public RunWorkQueueOperation getOperation() {
            return operation;
        }

    }

    private ISVNWCDb db;
    private boolean closeDb;
    private Stack<ISVNEventHandler> eventHandler;
    private List<CleanupHandler> cleanupHandlers = new LinkedList<CleanupHandler>();

    public SVNWCContext(ISVNOptions config, ISVNEventHandler eventHandler) {
        this(SVNWCDbOpenMode.ReadWrite, config, true, true, eventHandler);
    }

    public SVNWCContext(SVNWCDbOpenMode mode, ISVNOptions config, boolean autoUpgrade, boolean enforceEmptyWQ, ISVNEventHandler eventHandler) {
        this.db = new SVNWCDb();
        this.db.open(mode, config, autoUpgrade, enforceEmptyWQ);
        this.closeDb = true;
        this.eventHandler = new Stack<ISVNEventHandler>();
        this.eventHandler.push(eventHandler);
    }

    public SVNWCContext(ISVNWCDb db, ISVNEventHandler eventHandler) {
        this.db = db;
        this.closeDb = false;

        this.eventHandler = new Stack<ISVNEventHandler>();
        this.eventHandler.push(eventHandler);
    }
    
    public ISVNEventHandler getEventHandler() {
        return eventHandler.isEmpty() ? null : eventHandler.peek();
    }
    
    public void pushEventHandler(ISVNEventHandler handler) {
        eventHandler.push(handler);
    }

    public void popEventHandler() {
        eventHandler.pop();
    }
    
    public void setEventHandler(ISVNEventHandler handler) {
        if (!eventHandler.isEmpty()) {
            eventHandler.clear();
        }
        if (handler != null) {
            pushEventHandler(handler);
        }
    }

    public void close() {
        if (closeDb) {
            db.close();
        }
    }

    public void registerCleanupHandler(CleanupHandler ch) {
        cleanupHandlers.add(ch);
    }

    public void cleanup() throws SVNException {
        for (CleanupHandler ch : cleanupHandlers) {
            ch.cleanup();
        }
        cleanupHandlers.clear();
    }

    public ISVNWCDb getDb() {
        return db;
    }

    public void checkCancelled() throws SVNCancelException {
        if (getEventHandler() != null) {
            getEventHandler().checkCancelled();
        }
    }

    public ISVNOptions getOptions() {
        return db.getConfig();
    }

    public SVNNodeKind readKind(File localAbsPath, boolean showHidden) throws SVNException {
        try {
            final WCDbInfo info = db.readInfo(localAbsPath, InfoField.status, InfoField.kind);
            /* Make sure hidden nodes return svn_node_none. */
            if (!showHidden) {
                switch (info.status) {
                    case NotPresent:
                    case ServerExcluded:
                    case Excluded:
                        return SVNNodeKind.NONE;

                    default:
                        break;
                }
            }
            return info.kind.toNodeKind();
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                return SVNNodeKind.NONE;
            }
            throw e;
        }
    }

    public boolean isNodeAdded(File path) throws SVNException {
        final WCDbInfo info = db.readInfo(path, InfoField.status);
        final SVNWCDbStatus status = info.status;
        return status == SVNWCDbStatus.Added || status == SVNWCDbStatus.ObstructedAdd;
    }

    /** Equivalent to the old notion of "entry->schedule == schedule_replace" */
    public boolean isNodeReplaced(File path) throws SVNException {
        final WCDbInfo info = db.readInfo(path, InfoField.status, InfoField.haveBase);
        final SVNWCDbStatus status = info.status;
        final boolean haveBase = info.haveBase;
        SVNWCDbStatus baseStatus = null;
        if (haveBase) {
            final WCDbBaseInfo baseInfo = db.getBaseInfo(path, BaseInfoField.status);
            baseStatus = baseInfo.status;
        }
        return ((status == SVNWCDbStatus.Added || status == SVNWCDbStatus.ObstructedAdd) && haveBase && baseStatus != SVNWCDbStatus.NotPresent);
    }

    public long getRevisionNumber(SVNRevision revision, long[] latestRevisionNumber, SVNRepository repository, File path) throws SVNException {
        if (repository == null && (revision == SVNRevision.HEAD || revision.getDate() != null)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_RA_ACCESS_REQUIRED);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (revision.getNumber() >= 0) {
            return revision.getNumber();
        } else if (revision.getDate() != null) {
            return repository.getDatedRevision(revision.getDate());
        } else if (revision == SVNRevision.HEAD) {
            if (latestRevisionNumber != null && latestRevisionNumber.length > 0 && SVNRevision.isValidRevisionNumber(latestRevisionNumber[0])) {
                return latestRevisionNumber[0];
            }
            long latestRevision = repository.getLatestRevision();
            if (latestRevisionNumber != null && latestRevisionNumber.length > 0) {
                latestRevisionNumber[0] = latestRevision;
            }
            return latestRevision;
        } else if (!revision.isValid()) {
            return -1;
        } else if (revision == SVNRevision.WORKING || revision == SVNRevision.BASE) {
            if (path == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_VERSIONED_PATH_REQUIRED);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            long revnum = -1;
            try {
                revnum = getNodeCommitBaseRev(path);
            } catch (SVNException e) {
                /*
                 * Return the same error as older code did (before and at
                 * r935091). At least svn_client_proplist3 promises
                 * SVN_ERR_ENTRY_NOT_FOUND.
                 */
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND);
                    SVNErrorManager.error(err, SVNLogType.WC);
                } else {
                    throw e;
                }
            }
            if (!SVNRevision.isValidRevisionNumber(revnum)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Path ''{0}'' has no committed revision", path);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            return revnum;
        } else if (revision == SVNRevision.COMMITTED || revision == SVNRevision.PREVIOUS) {
            if (path == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_VERSIONED_PATH_REQUIRED);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            final WCDbInfo info = getNodeChangedInfo(path);
            if (!SVNRevision.isValidRevisionNumber(info.changedRev)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Path ''{0}'' has no committed revision", path);
                SVNErrorManager.error(err, SVNLogType.WC);

            }
            return revision == SVNRevision.PREVIOUS ? info.changedRev - 1 : info.changedRev;
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Unrecognized revision type requested for ''{0}''", path != null ? path : (Object) repository.getLocation());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return -1;
    }

    public WCDbInfo getNodeChangedInfo(File path) throws SVNException {
        return db.readInfo(path, InfoField.changedRev, InfoField.changedDate, InfoField.changedAuthor);
    }

    public long getNodeCommitBaseRev(File local_abspath) throws SVNException {
        final WCDbInfo info = db.readInfo(local_abspath, InfoField.status, InfoField.revision, InfoField.haveBase);
        /*
         * If this returned a valid revnum, there is no WORKING node. The node
         * is cleanly checked out, no modifications, copies or replaces.
         */
        long commitBaseRevision = info.revision;
        if (SVNRevision.isValidRevisionNumber(commitBaseRevision)) {
            return commitBaseRevision;
        }
        final SVNWCDbStatus status = info.status;
        final boolean haveBase = info.haveBase;
        if (status == SVNWCDbStatus.Added) {
            /*
             * If the node was copied/moved-here, return the copy/move source
             * revision (not this node's base revision). If it's just added,
             * return INVALID_REVNUM.
             */
            final WCDbAdditionInfo addInfo = db.scanAddition(local_abspath, AdditionInfoField.originalRevision);
            commitBaseRevision = addInfo.originalRevision;
            if (!SVNRevision.isValidRevisionNumber(commitBaseRevision) && haveBase) {
                /*
                 * It is a replace that does not feature a copy/move-here.
                 * Return the revert-base revision.
                 */
                commitBaseRevision = getNodeBaseRev(local_abspath);
            }
        } else if (status == SVNWCDbStatus.Deleted) {
            final WCDbDeletionInfo delInfo = db.scanDeletion(local_abspath, DeletionInfoField.workDelAbsPath);
            final File workDelAbspath = delInfo.workDelAbsPath;
            if (workDelAbspath != null) {
                /*
                 * This is a deletion within a copied subtree. Get the
                 * copied-from revision.
                 */
                final File parentAbspath = SVNFileUtil.getFileDir(workDelAbspath);
                final WCDbInfo parentInfo = db.readInfo(parentAbspath, InfoField.status);
                final SVNWCDbStatus parentStatus = parentInfo.status;
                assert (parentStatus == SVNWCDbStatus.Added || parentStatus == SVNWCDbStatus.ObstructedAdd);
                final WCDbAdditionInfo parentAddInfo = db.scanAddition(parentAbspath, AdditionInfoField.originalRevision);
                commitBaseRevision = parentAddInfo.originalRevision;
            } else
                /* This is a normal delete. Get the base revision. */
                commitBaseRevision = getNodeBaseRev(local_abspath);
        }
        return commitBaseRevision;
    }

    public long getNodeBaseRev(File local_abspath) throws SVNException {
        final WCDbInfo info = db.readInfo(local_abspath, InfoField.status, InfoField.revision, InfoField.haveBase);
        long baseRevision = info.revision;
        if (SVNRevision.isValidRevisionNumber(baseRevision)) {
            return baseRevision;
        }
        final boolean haveBase = info.haveBase;
        if (haveBase) {
            /* The node was replaced with something else. Look at the base. */
            final WCDbBaseInfo baseInfo = db.getBaseInfo(local_abspath, BaseInfoField.revision);
            baseRevision = baseInfo.revision;
        }
        return baseRevision;
    }

    public enum SVNWCSchedule {
        /** Nothing special here */
        normal,

        /** Slated for addition */
        add,

        /** Slated for deletion */
        delete,

        /** Slated for replacement (delete + add) */
        replace

    }

    public class ScheduleInternalInfo {

        public SVNWCSchedule schedule;
        public boolean copied;
    }

    public ScheduleInternalInfo getNodeScheduleInternal(File localAbsPath, boolean schedule, boolean copied) throws SVNException {
        final ScheduleInternalInfo info = new ScheduleInternalInfo();

        if (schedule) {
            info.schedule = SVNWCSchedule.normal;
        }
        if (copied) {
            info.copied = false;
        }

        WCDbInfo readInfo = db.readInfo(localAbsPath, InfoField.status, InfoField.originalReposRelpath, InfoField.opRoot, InfoField.haveBase, InfoField.haveMoreWork, InfoField.haveWork);
        SVNWCDbStatus status = readInfo.status;
        File copyFromRelpath = readInfo.originalReposRelpath;

        switch (status) {
            case NotPresent:
            case ServerExcluded:
            case Excluded:
                if (schedule) {
                    info.schedule = SVNWCSchedule.normal;                
                }
                break;
            case Normal:
            case Incomplete:
                break;

            case Deleted:
            {

                if (schedule) {
                    info.schedule = SVNWCSchedule.delete;
                }

                if (!copied) {
                    break;
                }
                if (readInfo.haveMoreWork || !readInfo.haveBase) {
                    info.copied = true;
                } else {
                    File work_del_abspath = db.scanDeletion(localAbsPath, DeletionInfoField.workDelAbsPath).workDelAbsPath;
                    if (work_del_abspath != null) {
                        info.copied = true;
                    }
                }
                break;
            }
            case Added: 
            {
                if (!readInfo.opRoot) {
                    if (copied) {
                        info.copied = true;
                    }
                    if (schedule) {
                        info.schedule = SVNWCSchedule.normal;
                    }
                    break;
                }
                if (copied) {
                    info.copied = copyFromRelpath != null;
                }
                if (schedule) {
                    info.schedule = SVNWCSchedule.add;
                } else {
                    break;
                }
                if (readInfo.haveBase || readInfo.haveMoreWork) {
                    WCDbInfo workingInfo = db.readInfoBelowWorking(localAbsPath);
                    if (workingInfo.status != SVNWCDbStatus.NotPresent && workingInfo.status != SVNWCDbStatus.Deleted) {
                        info.schedule = SVNWCSchedule.replace;
                        break;
                    }
                }
                break;
            }
            default:
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL), SVNLogType.WC);
                return null;
        }

        return info;
    }
    
    public boolean isTextModified(File localAbsPath, boolean exactComparison) throws SVNException {
        Structure<NodeInfo> nodeInfo = getDb().readInfo(localAbsPath, NodeInfo.status, NodeInfo.kind, NodeInfo.checksum, 
                NodeInfo.recordedSize, NodeInfo.recordedTime, NodeInfo.hadProps, NodeInfo.propsMod);
        SVNWCDbStatus status = nodeInfo.get(NodeInfo.status);
        SVNWCDbKind kind = nodeInfo.get(NodeInfo.kind);
        if (!nodeInfo.hasValue(NodeInfo.checksum)
                || SVNWCDbKind.File != kind
                || (SVNWCDbStatus.Added != status && SVNWCDbStatus.Normal != status)) {
            return true;
        }
        SVNFileType fileType = SVNFileType.getType(localAbsPath);
        if (fileType != SVNFileType.FILE && fileType != SVNFileType.SYMLINK) {
            return false;
        }
        if (!exactComparison && fileType != SVNFileType.SYMLINK) {
            boolean compare = false;
            long recordedSize = nodeInfo.lng(NodeInfo.recordedSize); 
            if (recordedSize != -1 && SVNFileUtil.getFileLength(localAbsPath) != recordedSize) {
                compare = true;
            }
            if (!compare && (nodeInfo.lng(NodeInfo.recordedTime)/1000) != SVNFileUtil.getFileLastModified(localAbsPath)) {
                compare = true;
            }
            if (!compare) {
                return false;
            }
        }
        
        File pristineFile = getDb().getPristinePath(getDb().getWCRoot(localAbsPath), nodeInfo.<SvnChecksum>get(NodeInfo.checksum));
        boolean modified = false;

        modified = compareAndVerify(localAbsPath, pristineFile, nodeInfo.is(NodeInfo.hadProps), nodeInfo.is(NodeInfo.propsMod), exactComparison);
        
        if (!modified) {
            if (getDb().isWCLockOwns(localAbsPath, false)) {
                db.globalRecordFileinfo(localAbsPath, SVNFileUtil.getFileLength(localAbsPath), new SVNDate(SVNFileUtil.getFileLastModified(localAbsPath), 0));
            }
        }
        return modified;
    }
        
    public boolean compareAndVerify(File localAbsPath, File pristineFile, boolean hasProps, boolean propMods, boolean exactComparison) throws SVNException {
        if (propMods) {
            hasProps = true;
        }
        boolean translationRequired = false;

        TranslateInfo translateInfo = null;
        if (hasProps || isGlobalCharsetSpecified()) {
            translateInfo = getTranslateInfo(localAbsPath, true, true, true, true);
            translationRequired = isTranslationRequired(translateInfo.eolStyleInfo.eolStyle, translateInfo.eolStyleInfo.eolStr, translateInfo.charset, translateInfo.keywords, translateInfo.special, true);
        }
        if (!translationRequired && SVNFileUtil.getFileLength(localAbsPath) != pristineFile.length()) {
            return true;
        }
        
        if (translationRequired) {
            InputStream versionedStream = null;
            InputStream pristineStream = null;
            File tmpFile = null;
            try {
                pristineStream = SVNFileUtil.openFileForReading(pristineFile);
                if (translateInfo.special) {
                    if (SVNFileUtil.symlinksSupported()) {
                        versionedStream = readSpecialFile(localAbsPath);
                    } else {
                        versionedStream = SVNFileUtil.openFileForReading(localAbsPath);
                    }
                } else {
                    versionedStream = SVNFileUtil.openFileForReading(localAbsPath);
                    if (!exactComparison) {
                        byte[] eolStr = translateInfo.eolStyleInfo.eolStr; 
                        if (translateInfo.eolStyleInfo.eolStyle == SVNWCContext.SVNEolStyle.Native) {
                            eolStr = SVNTranslator.getBaseEOL(SVNProperty.EOL_STYLE_NATIVE);
                        } else if (translateInfo.eolStyleInfo.eolStyle != SVNWCContext.SVNEolStyle.Fixed &&
                                translateInfo.eolStyleInfo.eolStyle != SVNWCContext.SVNEolStyle.None) {
                            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.IO_UNKNOWN_EOL), SVNLogType.WC);
                        }
                            
                        versionedStream = SVNTranslator.getTranslatingInputStream(
                                versionedStream, 
                                translateInfo.charset,
                                eolStr, 
                                true, 
                                translateInfo.keywords, 
                                false);
                    } else {
                        pristineStream = SVNTranslator.getTranslatingInputStream(
                                pristineStream, 
                                translateInfo.charset,
                                translateInfo.eolStyleInfo.eolStr, 
                                false, 
                                translateInfo.keywords, 
                                true);
                    }
                }
                return !isSameContents(versionedStream, pristineStream);
            } finally {
                SVNFileUtil.closeFile(pristineStream);
                SVNFileUtil.closeFile(versionedStream);
                SVNFileUtil.deleteFile(tmpFile);
            }
        } else {
            return !isSameContents(localAbsPath, pristineFile);
        }
    }

    private boolean isGlobalCharsetSpecified() {
        ISVNOptions options = getOptions();
        if (options instanceof DefaultSVNOptions) {
            DefaultSVNOptions defaultOptions = (DefaultSVNOptions) options;
            String globalCharset = defaultOptions.getGlobalCharset();
            return globalCharset != null;
        }
        return false;
    }

    public static class PristineContentsInfo {

        public InputStream stream;
        public File path;
    }

    public PristineContentsInfo getPristineContents(File localAbspath, boolean openStream, boolean getPath) throws SVNException {
        assert (openStream || getPath);
        PristineContentsInfo info = new PristineContentsInfo();

        final Structure<PristineInfo> readInfo = db.readPristineInfo(localAbspath);

        if (readInfo.<SVNWCDbKind>get(PristineInfo.kind) != SVNWCDbKind.File) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, "Can only get the pristine contents of files;" + "  ''{0}'' is not a file", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        final SVNWCDbStatus status = readInfo.<SVNWCDbStatus>get(PristineInfo.status);
        final SvnChecksum checksum = readInfo.<SvnChecksum>get(PristineInfo.checksum);
        readInfo.release();
        
        if (status == SVNWCDbStatus.Added) {
            final WCDbAdditionInfo scanAddition = db.scanAddition(localAbspath, AdditionInfoField.status);
            if (scanAddition.status == SVNWCDbStatus.Added) {
                return info;
            }
        } else if (status == SVNWCDbStatus.NotPresent) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "Cannot get the pristine contents of ''{0}'' " + "because its delete is already committed", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        } else if (status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.Incomplete) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot get the pristine contents of ''{0}'' " + "because it has an unexpected status", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        } else {
            SVNErrorManager.assertionFailure(status != SVNWCDbStatus.Obstructed && status != SVNWCDbStatus.ObstructedAdd && status != SVNWCDbStatus.ObstructedDelete
                    && status != SVNWCDbStatus.BaseDeleted, null, SVNLogType.WC);
        }

        if (checksum == null) {
            return info;
        }

        SvnChecksum oldchecksum = SvnChecksum.fromString(checksum.toString());
        if (getPath) {
            info.path = db.getPristinePath(localAbspath, oldchecksum);
        }
        if (openStream) {
            info.stream = db.readPristine(localAbspath, oldchecksum);
        }
        return info;
    }

    private boolean isSameContents(File file1, File file2) throws SVNException {
        InputStream stream1 = null;
        InputStream stream2 = null;
        try {
            stream1 = SVNFileUtil.openFileForReading(file1);
            stream2 = SVNFileUtil.openFileForReading(file2);
            return isSameContents(stream1, stream2);
        } finally {
            try {
                SVNFileUtil.closeFile(stream1);
            } finally {
                SVNFileUtil.closeFile(stream2);
            }
        }
    }

    private boolean isSameContents(InputStream stream1, InputStream stream2) throws SVNException {
        try {
            byte[] buf1 = new byte[STREAM_CHUNK_SIZE];
            byte[] buf2 = new byte[STREAM_CHUNK_SIZE];
            long bytes_read1 = STREAM_CHUNK_SIZE;
            long bytes_read2 = STREAM_CHUNK_SIZE;
            boolean same = true; /* assume TRUE, until disproved below */
            while (bytes_read1 == STREAM_CHUNK_SIZE && bytes_read2 == STREAM_CHUNK_SIZE) {
                bytes_read1 = stream1.read(buf1);
                bytes_read2 = stream2.read(buf2);
                if ((bytes_read1 != bytes_read2) || !(Arrays.equals(buf1, buf2))) {
                    same = false;
                    break;
                }
            }
            return same;
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
            return false;
        }
    }

    public static InputStream readSpecialFile(File localAbsPath) throws SVNException {
        if (!SVNFileUtil.symlinksSupported()) {
            return SVNFileUtil.openFileForReading(localAbsPath, SVNLogType.WC);
        }
        /*
         * First determine what type of special file we are detranslating.
         */
        final SVNFileType filetype = SVNFileType.getType(localAbsPath);
        if (SVNFileType.FILE == filetype) {
            /*
             * Nothing special to do here, just create stream from the original
             * file's contents.
             */
            return SVNFileUtil.openFileForReading(localAbsPath, SVNLogType.WC);
        } else if (SVNFileType.SYMLINK == filetype) {
            /* Determine the destination of the link. */
            String linkPath = SVNFileUtil.getSymlinkName(localAbsPath);
            if (linkPath == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot detranslate symbolic link ''{0}''; file does not exist or not a symbolic link", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            String symlinkContents = "link " + linkPath;
            return new ByteArrayInputStream(symlinkContents.getBytes());
        } else {
            SVNErrorManager.assertionFailure(false, "ERR_MALFUNCTION", SVNLogType.WC);
        }
        return null;
    }

    private boolean isTranslationRequired(SVNEolStyle style, byte[] eol, String charset, Map<String, byte[]> keywords, boolean special, boolean force_eol_check) {
        return (special 
                || (keywords != null && !keywords.isEmpty()) 
                || (style != SVNEolStyle.None && force_eol_check)
                || (charset != null)
        // || (style == SVNEolStyle.Native && strcmp(APR_EOL_STR,
        // SVN_SUBST_NATIVE_EOL_STR) != 0)
                || (style == SVNEolStyle.Fixed && !Arrays.equals(SVNEolStyleInfo.NATIVE_EOL_STR, eol)));
    }

    // TODO merget isSpecial()/getEOLStyle()/getKeyWords() into
    // getTranslateInfo()
    public String getCharset(File path) throws SVNException {
        SVNProperties properties = getProperties(path, SVNProperty.MIME_TYPE);
        String mimeType = properties.getStringValue(SVNProperty.MIME_TYPE);
        return SVNTranslator.getCharset(properties.getStringValue(SVNProperty.CHARSET), mimeType, path, getOptions());
    }

    // TODO merget isSpecial()/getEOLStyle()/getKeyWords() into
    // getTranslateInfo()
    public boolean isSpecial(File path) throws SVNException {
        final String property = getProperty(path, SVNProperty.SPECIAL);
        return property != null;
    }

    // TODO merget isSpecial()/getEOLStyle()/getKeyWords() into
    // getTranslateInfo()
    public SVNEolStyleInfo getEolStyle(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));

        /* Get the property value. */
        final String propVal = getProperty(localAbsPath, SVNProperty.EOL_STYLE);

        /* Convert it. */
        return SVNEolStyleInfo.fromValue(propVal);
    }

    // TODO merget isSpecial()/getEOLStyle()/getKeyWords() into
    // getTranslateInfo()
    public Map<String, byte[]> getKeyWords(File localAbsPath, String forceList) throws SVNException {
        assert (isAbsolute(localAbsPath));

        String list;
        /*
         * Choose a property list to parse: either the one that came into this
         * function, or the one attached to PATH.
         */
        if (forceList == null) {
            list = getProperty(localAbsPath, SVNProperty.KEYWORDS);

            /* The easy answer. */
            if (list == null) {
                return null;
            }

        } else
            list = forceList;

        final SVNURL url = getNodeUrl(localAbsPath);
        final WCDbInfo readInfo = db.readInfo(localAbsPath, InfoField.changedRev, InfoField.changedDate, InfoField.changedAuthor);
        return SVNTranslator.computeKeywords(list, url.toString(), readInfo.changedAuthor, readInfo.changedDate.format(), Long.toString(readInfo.changedRev), getOptions());
    }

    public static class TranslateInfo {

        public SVNEolStyleInfo eolStyleInfo;
        public String charset;
        public Map<String, byte[]> keywords;
        public boolean special;
    }
    public TranslateInfo getTranslateInfo(File localAbspath, boolean fetchEolStyle, boolean fetchCharset, boolean fetchKeywords, boolean fetchSpecial) throws SVNException {
        return getTranslateInfo(localAbspath, null, false, fetchEolStyle, fetchCharset, fetchKeywords, fetchSpecial);
    }

    public TranslateInfo getTranslateInfo(File localAbspath, SVNProperties props, boolean forNormalization, boolean fetchEolStyle, boolean fetchCharset, boolean fetchKeywords, boolean fetchSpecial) throws SVNException {
        TranslateInfo info = new TranslateInfo();
        if (props == null) {
            props = getActualProperties(localAbspath);
        }
        if (fetchEolStyle) {
            info.eolStyleInfo = SVNEolStyleInfo.fromValue(props.getStringValue(SVNProperty.EOL_STYLE));
        }
        if (fetchCharset) {
            info.charset = SVNTranslator.getCharset(props.getStringValue(SVNProperty.CHARSET), props.getStringValue(SVNProperty.MIME_TYPE), localAbspath, getOptions());
        }
        if (fetchKeywords) {
            String keywordsProp = props.getStringValue(SVNProperty.KEYWORDS);
            if (keywordsProp == null || "".equals(keywordsProp)) {
                info.keywords = null;
            } else {
                info.keywords = expandKeywords(localAbspath, null, keywordsProp, forNormalization);
            }
        }
        if (fetchSpecial) {
            info.special = props.getStringValue(SVNProperty.SPECIAL) != null;
        }
        return info;
    }
    
    private Map<String, byte[]> expandKeywords(File localAbsPath, File wriAbspath, String keywordsList, boolean forNormalization) throws SVNException {
        String url = null;
        SVNDate changedDate = null;
        long changedRev;
        String changedAuthor = null;
        
        if (!forNormalization) {
            WCDbInfo info = getDb().readInfo(localAbsPath, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.changedRev, InfoField.changedAuthor, InfoField.changedDate);
            changedAuthor = info.changedAuthor;
            changedRev = info.changedRev;
            changedDate = info.changedDate;
            
            if (info.reposRelPath != null) {
                url = info.reposRootUrl.appendPath(SVNFileUtil.getFilePath(info.reposRelPath), false).toString();
            } else {
                SVNURL svnUrl = getNodeUrl(localAbsPath);
                if (svnUrl != null) {
                    url = svnUrl.toString();
                }
            }
        } else {
            url = "";
            changedRev = INVALID_REVNUM;
            changedDate = SVNDate.NULL;
            changedAuthor = "";
        }
        return SVNTranslator.computeKeywords(keywordsList, url, changedAuthor, changedDate.format(), Long.toString(changedRev), getOptions());
    }

    public boolean isFileExternal(File path) throws SVNException {
        final String serialized = db.getFileExternalTemp(path);
        return serialized != null;
    }

    public SVNURL getNodeUrl(File path) throws SVNException {
        Structure<NodeInfo> info = db.readInfo(path, NodeInfo.status, NodeInfo.reposRelPath, NodeInfo.reposId, NodeInfo.haveBase);
        File reposRelPath = info.get(NodeInfo.reposRelPath);
        SVNWCDbStatus status = info.get(NodeInfo.status);
        long reposId = info.lng(NodeInfo.reposId);

        
        if (reposRelPath == null) {
            if (status == SVNWCDbStatus.Added) {
                Structure<AdditionInfo> additionInfo = SvnWcDbShared.scanAddition((SVNWCDb) db, path);
                reposRelPath = additionInfo.get(AdditionInfo.reposRelPath);
                reposId = additionInfo.lng(AdditionInfo.reposId);
                additionInfo.release();
            } else if (status == SVNWCDbStatus.Deleted) {
                File localRelPath = db.toRelPath(path);

                Structure<DeletionInfo> deletionInfo = SvnWcDbShared.scanDeletion((SVNWCDb) db, path);
                File baseDelRelpath = deletionInfo.get(DeletionInfo.baseDelRelPath);
                File workDelRelpath = deletionInfo.get(DeletionInfo.workDelRelPath);
                if (baseDelRelpath != null) {
                    DirParsedInfo dirParsedInfo = ((SVNWCDb) db).parseDir(path, Mode.ReadOnly);

                    Structure<NodeInfo> baseInfo = SvnWcDbShared.getBaseInfo(dirParsedInfo.wcDbDir.getWCRoot(), baseDelRelpath, NodeInfo.reposRelPath, NodeInfo.reposId);
                    reposRelPath = baseInfo.get(NodeInfo.reposRelPath);
                    reposId = baseInfo.lng(NodeInfo.reposId);
                    baseInfo.release();
                    
                    reposRelPath = SVNFileUtil.createFilePath(reposRelPath, SVNWCUtils.skipAncestor(baseDelRelpath, localRelPath));
                } else {
                    File workRelpath = SVNFileUtil.getFileDir(workDelRelpath); 
                    Structure<AdditionInfo> additionInfo = SvnWcDbShared.scanAddition((SVNWCDb) db, db.fromRelPath(db.getWCRoot(path), workRelpath));
                    reposRelPath = additionInfo.get(AdditionInfo.reposRelPath);
                    reposId = additionInfo.lng(AdditionInfo.reposId);
                    additionInfo.release();

                    reposRelPath = SVNFileUtil.createFilePath(reposRelPath, SVNWCUtils.skipAncestor(workRelpath, localRelPath));
                    
                }
            } else if (status == SVNWCDbStatus.Excluded) {
                File parentPath = SVNFileUtil.getParentFile(path);
                SVNURL url = getNodeUrl(parentPath);
                return url.appendPath(SVNFileUtil.getFileName(path), false);
            } else {
                return null;
            }
        }
        
        DirParsedInfo dpi = ((SVNWCDb) db).parseDir(path, Mode.ReadOnly);
        ReposInfo reposInfo = ((SVNWCDb) db).fetchReposInfo(dpi.wcDbDir.getWCRoot().getSDb(), reposId);
        
        return SVNWCUtils.join(SVNURL.parseURIEncoded(reposInfo.reposRootUrl), reposRelPath);       
    }

    public SVNWCContext.ConflictInfo getConflicted(File localAbsPath, boolean isTextNeed, boolean isPropNeed, boolean isTreeNeed) throws SVNException {
        final WCDbInfo readInfo = db.readInfo(localAbsPath, InfoField.kind, InfoField.conflicted);
        final SVNWCContext.ConflictInfo info = new SVNWCContext.ConflictInfo();
        if (!readInfo.conflicted) {
            return info;
        }
        final File dir_path = (readInfo.kind == SVNWCDbKind.Dir) ? localAbsPath : SVNFileUtil.getFileDir(localAbsPath);
        final List<SVNConflictDescription> conflicts = db.readConflicts(localAbsPath);
        for (final SVNConflictDescription cd : conflicts) {
            final SVNMergeFileSet cdf = cd.getMergeFiles();
            if (isTextNeed && cd.isTextConflict()) {
                /*
                 * Look for any text conflict, exercising only as much effort as
                 * necessary to obtain a definitive answer. This only applies to
                 * files, but we don't have to explicitly check that entry is a
                 * file, since these attributes would never be set on a
                 * directory anyway. A conflict file entry notation only counts
                 * if the conflict file still exists on disk.
                 */
                if (cdf.getBaseFile() != null) {
                    final File path = SVNFileUtil.createFilePath(dir_path, cdf.getBaseFile());
                    final SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path));
                    if (kind == SVNNodeKind.FILE) {
                        info.textConflicted = true;
                        info.baseFile = path;
                    }
                }
                if (cdf.getRepositoryFile() != null) {
                    final File path = SVNFileUtil.createFilePath(dir_path, cdf.getRepositoryFile());
                    final SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path));
                    if (kind == SVNNodeKind.FILE) {
                        info.textConflicted = true;
                        info.repositoryFile = path;
                    }
                }
                if (cdf.getLocalFile() != null) {
                    final File path = SVNFileUtil.createFilePath(dir_path, cdf.getLocalFile());
                    final SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path));
                    if (kind == SVNNodeKind.FILE) {
                        info.textConflicted = true;
                        info.localFile = path;
                    }
                }
            } else if (isPropNeed && cd.isPropertyConflict()) {
                if (cdf.getRepositoryFile() != null) {
                    final File path = SVNFileUtil.createFilePath(dir_path, cdf.getRepositoryFile());
                    final SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(path));
                    if (kind == SVNNodeKind.FILE) {
                        info.propConflicted = true;
                        info.propRejectFile = path;
                    }
                }
            } else if (isTreeNeed && cd.isTreeConflict()) {
                info.treeConflicted = true;
                info.treeConflict = (SVNTreeConflictDescription) cd;
            }
        }
        return info;
    }

    public String getProperty(File localAbsPath, String name) throws SVNException {
        SVNProperties properties = getProperties(localAbsPath, name);
        if (properties != null) {
            return properties.getStringValue(name);
        }
        return null;
    }

    public SVNPropertyValue getPropertyValue(File localAbsPath, String name) throws SVNException {
        SVNProperties properties = getProperties(localAbsPath, name);
        if (properties != null) {
            return properties.getSVNPropertyValue(name);
        }
        return null;
    }

    private SVNProperties getProperties(File localAbsPath, String name) throws SVNException {
        assert (isAbsolute(localAbsPath));
        assert (!SVNProperty.isEntryProperty(name));
        SVNProperties properties = null;
        final SVNWCDbKind wcKind = db.readKind(localAbsPath, true);
        if (wcKind == SVNWCDbKind.Unknown) {
            /*
             * The node is not present, or not really "here". Therefore, the
             * property is not present.
             */
            return null;
        }
        final boolean hidden = db.isNodeHidden(localAbsPath);
        if (hidden) {
            /*
             * The node is not present, or not really "here". Therefore, the
             * property is not present.
             */
            return null;
        }
        if (SVNProperty.isWorkingCopyProperty(name)) {
            /*
             * If no dav cache can be found, just set VALUE to NULL (for
             * compatibility with pre-WC-NG code).
             */
            try {
                properties = db.getBaseDavCache(localAbsPath);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    return null;
                }
                throw e;
            }
        } else {
            /* regular prop */
            properties = getActualProperties(localAbsPath);
        }
        return properties;
    }

    private SVNProperties getActualProperties(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));
        /*
         * ### perform some state checking. for example, locally-deleted nodes
         * ### should not have any ACTUAL props.
         */
        return db.readProperties(localAbsPath);
    }

    public SVNURL getUrlFromPath(File localAbsPath) throws SVNException {
        return getEntryLocation(localAbsPath, SVNRevision.UNDEFINED, false).url;
    }

    public static class EntryLocationInfo {

        public SVNURL url;
        public long revNum;
    }

    public EntryLocationInfo getEntryLocation(File localAbsPath, SVNRevision pegRevisionKind, boolean fetchRevnum) throws SVNException {
        /*
         * This function doesn't contact the repository, so error out if asked
         * to do so.
         */
        // TODO what is svn_opt_revision_date in our code ? (sergey)
        if (/* pegRevisionKind == svn_opt_revision_date || */
        pegRevisionKind == SVNRevision.HEAD) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION);
            SVNErrorManager.error(err, SVNLogType.WC);
            return null;
        }

        final NodeCopyFromInfo copyFrom = getNodeCopyFromInfo(localAbsPath, NodeCopyFromField.url, NodeCopyFromField.rev);

        final EntryLocationInfo result = new EntryLocationInfo();

        if (copyFrom.url != null && pegRevisionKind == SVNRevision.WORKING) {
            result.url = copyFrom.url;
            if (fetchRevnum)
                result.revNum = copyFrom.rev;
        } else {
            final SVNURL node_url = getNodeUrl(localAbsPath);

            if (node_url != null) {
                result.url = node_url;
                if (fetchRevnum) {
                    if ((pegRevisionKind == SVNRevision.COMMITTED) || (pegRevisionKind == SVNRevision.PREVIOUS)) {
                        result.revNum = getNodeChangedInfo(localAbsPath).changedRev;
                        if (pegRevisionKind == SVNRevision.PREVIOUS)
                            result.revNum = result.revNum - 1;
                    } else {
                        /*
                         * Local modifications are not relevant here, so
                         * consider svn_opt_revision_unspecified,
                         * svn_opt_revision_number, svn_opt_revision_base, and
                         * svn_opt_revision_working as the same.
                         */
                        result.revNum = getNodeBaseRev(localAbsPath);
                    }
                }
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "Entry for ''{0}'' has no URL", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }

        return result;
    }

    public enum NodeCopyFromField {
        rootUrl, reposRelPath, url, rev, isCopyTarget;
    }

    public static class NodeCopyFromInfo {

        public SVNURL rootUrl = null;
        public File reposRelPath = null;
        public SVNURL url = null;
        public long rev = INVALID_REVNUM;
        public boolean isCopyTarget = false;
    }

    public NodeCopyFromInfo getNodeCopyFromInfo(File localAbsPath, NodeCopyFromField... fields) throws SVNException {
        final EnumSet<NodeCopyFromField> f = SVNWCDb.getInfoFields(NodeCopyFromField.class, fields);

        final WCDbInfo readInfo = db.readInfo(localAbsPath, InfoField.status, InfoField.originalReposRelpath, InfoField.originalRootUrl, InfoField.originalRevision);
        SVNURL original_root_url = readInfo.originalRootUrl;
        File original_repos_relpath = readInfo.originalReposRelpath;
        long original_revision = readInfo.originalRevision;
        SVNWCDbStatus status = readInfo.status;

        final NodeCopyFromInfo copyFrom = new NodeCopyFromInfo();

        if (original_root_url != null && original_repos_relpath != null) {
            /*
             * If this was the root of the copy then the URL is immediately
             * available...
             */
            SVNURL my_copyfrom_url = null;

            if (f.contains(NodeCopyFromField.url) || f.contains(NodeCopyFromField.isCopyTarget))
                my_copyfrom_url = SVNURL.parseURIEncoded(SVNPathUtil.append(original_root_url.toString(), original_repos_relpath.toString()));

            if (f.contains(NodeCopyFromField.rootUrl))
                copyFrom.rootUrl = original_root_url;
            if (f.contains(NodeCopyFromField.reposRelPath))
                copyFrom.reposRelPath = original_repos_relpath;
            if (f.contains(NodeCopyFromField.url))
                copyFrom.url = my_copyfrom_url;

            if (f.contains(NodeCopyFromField.rev))
                copyFrom.rev = original_revision;

            if (f.contains(NodeCopyFromField.isCopyTarget)) {
                /*
                 * ### At this point we'd just set is_copy_target to TRUE, *but*
                 * we currently want to model wc-1 behaviour. Particularly, this
                 * affects mixed-revision copies (e.g. wc-wc copy): - Wc-1 saw
                 * only the root of a mixed-revision copy as the copy's root. -
                 * Wc-ng returns an explicit original_root_url,
                 * original_repos_relpath pair for each subtree with mismatching
                 * revision. We need to compensate for that: Find out if the
                 * parent of this node is also copied and has a matching
                 * copy_from URL. If so, nevermind the revision, just like wc-1
                 * did, and say this was not a separate copy target.
                 */
                final File parent_abspath = SVNFileUtil.getFileDir(localAbsPath);
                final String base_name = SVNFileUtil.getFileName(localAbsPath);

                /*
                 * This is a copied node, so we should never fall off the top of
                 * a working copy here.
                 */
                final SVNURL parent_copyfrom_url = getNodeCopyFromInfo(parent_abspath, NodeCopyFromField.url).url;

                /*
                 * So, count this as a separate copy target only if the URLs
                 * don't match up, or if the parent isn't copied at all.
                 */
                if (parent_copyfrom_url == null || !SVNURL.parseURIEncoded(SVNPathUtil.append(parent_copyfrom_url.toString(), base_name)).equals(my_copyfrom_url)) {
                    copyFrom.isCopyTarget = true;
                }
            }
        } else if ((status == SVNWCDbStatus.Added || status == SVNWCDbStatus.ObstructedAdd)
                && (f.contains(NodeCopyFromField.rev) || f.contains(NodeCopyFromField.url) || f.contains(NodeCopyFromField.rootUrl) || f.contains(NodeCopyFromField.reposRelPath))) {
            /*
             * ...But if this is merely the descendant of an explicitly
             * copied/moved directory, we need to do a bit more work to
             * determine copyfrom_url and copyfrom_rev.
             */

            final WCDbAdditionInfo scanAddition = db.scanAddition(localAbsPath, AdditionInfoField.status, AdditionInfoField.opRootAbsPath, AdditionInfoField.originalReposRelPath,
                    AdditionInfoField.originalRootUrl, AdditionInfoField.originalRevision);
            final File op_root_abspath = scanAddition.opRootAbsPath;
            status = scanAddition.status;
            original_repos_relpath = scanAddition.originalReposRelPath;
            original_root_url = scanAddition.originalRootUrl;
            original_revision = scanAddition.originalRevision;

            if (status == SVNWCDbStatus.Copied || status == SVNWCDbStatus.MovedHere) {
                SVNURL src_parent_url = SVNURL.parseURIEncoded(SVNPathUtil.append(original_root_url.toString(), original_repos_relpath.toString()));
                String src_relpath = SVNPathUtil.getPathAsChild(op_root_abspath.toString(), localAbsPath.toString());

                if (src_relpath != null) {
                    if (f.contains(NodeCopyFromField.rootUrl))
                        copyFrom.rootUrl = original_root_url;
                    if (f.contains(NodeCopyFromField.reposRelPath))
                        copyFrom.reposRelPath = SVNFileUtil.createFilePath(original_repos_relpath, src_relpath);
                    if (f.contains(NodeCopyFromField.url))
                        copyFrom.url = SVNURL.parseURIEncoded(SVNPathUtil.append(src_parent_url.toString(), src_relpath.toString()));
                    if (f.contains(NodeCopyFromField.rev))
                        copyFrom.rev = original_revision;
                }
            }
        }

        return copyFrom;
    }
    
    public Structure<NodeOriginInfo> getNodeOrigin(File localAbsPath, boolean scanDeleted, NodeOriginInfo... fields) throws SVNException {
        final Structure<NodeInfo> readInfo = db.readInfo(localAbsPath, 
                NodeInfo.status, NodeInfo.revision, NodeInfo.reposRelPath, NodeInfo.reposRootUrl, NodeInfo.reposUuid,
                NodeInfo.originalRevision, NodeInfo.originalReposRelpath, NodeInfo.originalRootUrl, NodeInfo.originalUuid, NodeInfo.originalUuid,
                NodeInfo.haveWork);
        
        Structure<NodeOriginInfo> result = Structure.obtain(NodeOriginInfo.class, fields);
        readInfo.
            from(NodeInfo.revision, NodeInfo.reposRelPath, NodeInfo.reposRootUrl, NodeInfo.reposUuid, NodeInfo.haveWork).
            into(result, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl, NodeOriginInfo.reposUuid,
                    NodeOriginInfo.isCopy);
        
        if (readInfo.hasValue(NodeInfo.reposRelPath)) {
            readInfo.release();
            return result;
        }
        
        SVNWCDbStatus status = readInfo.get(NodeInfo.status);
        if (status == SVNWCDbStatus.Deleted && !scanDeleted) {
            if (result.hasValue(NodeOriginInfo.isCopy) && result.is(NodeOriginInfo.isCopy)) {
                result.set(NodeOriginInfo.isCopy, false);
            }
            readInfo.release();
            return result;
        }
        
        if (readInfo.hasValue(NodeInfo.originalReposRelpath)) {
            readInfo.
            from(NodeInfo.originalRevision, NodeInfo.originalReposRelpath, NodeInfo.originalRootUrl, NodeInfo.originalUuid).
            into(result, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl, NodeOriginInfo.reposUuid);
            
            if (!result.hasField(NodeOriginInfo.copyRootAbsPath)) {
                readInfo.release();
                return result;
            }
        }
        
        boolean scanWorking = false;
        if (status == SVNWCDbStatus.Added) {
            scanWorking = true;
        } else if (status == SVNWCDbStatus.Deleted) {
            WCDbInfo belowInfo = db.readInfoBelowWorking(localAbsPath);
            scanWorking = belowInfo.haveWork;
            status = belowInfo.status;
        }
        readInfo.release();
        
        if (scanWorking) {
            WCDbAdditionInfo addInfo = db.scanAddition(localAbsPath, AdditionInfoField.status, AdditionInfoField.opRootAbsPath,
                    AdditionInfoField.originalReposRelPath, AdditionInfoField.originalRootUrl, AdditionInfoField.originalRevision);
            status = addInfo.status;
            result.set(NodeOriginInfo.reposRootUrl, addInfo.originalRootUrl);
            result.set(NodeOriginInfo.reposUuid, addInfo.originalUuid);
            result.set(NodeOriginInfo.revision, addInfo.originalRevision);
            
            if (status == SVNWCDbStatus.Deleted) {
                return result;
            }
            
            File relPath = SVNFileUtil.createFilePath(addInfo.originalReposRelPath, SVNWCUtils.skipAncestor(addInfo.opRootAbsPath, localAbsPath));
            result.set(NodeOriginInfo.reposRelpath, relPath);
            if (result.hasField(NodeOriginInfo.copyRootAbsPath)) {
                result.set(NodeOriginInfo.copyRootAbsPath, addInfo.opRootAbsPath);
            }
        } else {
            WCDbBaseInfo baseInfo = db.getBaseInfo(localAbsPath, BaseInfoField.revision, BaseInfoField.reposRelPath, BaseInfoField.reposRootUrl, BaseInfoField.reposUuid);
            result.set(NodeOriginInfo.reposRootUrl, baseInfo.reposRootUrl);
            result.set(NodeOriginInfo.reposUuid, baseInfo.reposUuid);
            result.set(NodeOriginInfo.revision, baseInfo.revision);
            result.set(NodeOriginInfo.reposRelpath, baseInfo.reposRelPath);
        }
        return result;
    }

    public static boolean isErrorAccess(SVNException e) {
        final SVNErrorCode errorCode = e.getErrorMessage().getErrorCode();
        return errorCode == SVNErrorCode.FS_NOT_FOUND;
    }

    public boolean isPropsModified(File localAbspath) throws SVNException {
        return db.readInfo(localAbspath, InfoField.propsMod).propsMod;
    }

    public interface ISVNWCNodeHandler {

        void nodeFound(File localAbspath, SVNWCDbKind kind) throws SVNException;
    }

    public void nodeWalkChildren(File localAbspath, ISVNWCNodeHandler nodeHandler, boolean showHidden, SVNDepth walkDepth, Collection<String> changelists) throws SVNException {
        assert (walkDepth != null && walkDepth.getId() >= SVNDepth.EMPTY.getId() && walkDepth.getId() <= SVNDepth.INFINITY.getId());
        changelists = changelists != null && changelists.size() > 0 ? new HashSet<String>(changelists) : null;
        Structure<NodeInfo> nodeInfo = db.readInfo(localAbspath, NodeInfo.status, NodeInfo.kind);
        SVNWCDbKind kind = nodeInfo.<SVNWCDbKind>get(NodeInfo.kind);
        SVNWCDbStatus status = nodeInfo.<SVNWCDbStatus>get(NodeInfo.status);
        nodeInfo.release();
        if (matchesChangelist(localAbspath, changelists)) {
            nodeHandler.nodeFound(localAbspath, kind);
        }
        
        if (kind == SVNWCDbKind.File || status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.ServerExcluded) {
            return;
        }
        
        if (kind == SVNWCDbKind.Dir) {
            walkerHelper(localAbspath, nodeHandler, showHidden, walkDepth, changelists);
            return;
        }
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNKNOWN_KIND, "''{0}'' has an unrecognized node kind", localAbspath);
        SVNErrorManager.error(err, SVNLogType.WC);
    }
    
    public boolean matchesChangelist(File localAbspath, Collection<String> changelists) {
        if (changelists == null || changelists.isEmpty()) {
            return true;
        }
        Structure<NodeInfo> nodeInfo = null;
        try {
            nodeInfo = db.readInfo(localAbspath, NodeInfo.changelist);
            return nodeInfo != null && nodeInfo.hasValue(NodeInfo.changelist) && changelists.contains(nodeInfo.text(NodeInfo.changelist));
        } catch (SVNException e) {
            return false;
        } finally {
            if (nodeInfo != null) {
                nodeInfo.release();
            }
        }
    }

    private void walkerHelper(File dirAbspath, ISVNWCNodeHandler nodeHandler, boolean showHidden, SVNDepth depth, Collection<String> changelists) throws SVNException {
        if (depth == SVNDepth.EMPTY) {
            return;
        }
        final Map<String, Structure<WalkerChildInfo>> relChildren = SvnWcDbReader.readWalkerChildrenInfo((SVNWCDb) db, dirAbspath, null);
        
        for (final String child : relChildren.keySet()) {
            checkCancelled();
            
            Structure<WalkerChildInfo> childInfo = relChildren.get(child);            
            SVNWCDbStatus childStatus = childInfo.<SVNWCDbStatus>get(WalkerChildInfo.status);
            SVNWCDbKind childKind = childInfo.<SVNWCDbKind>get(WalkerChildInfo.kind);
            childInfo.release();
            
            if (!showHidden) {
                switch (childStatus) {
                    case NotPresent:
                    case ServerExcluded:
                    case Excluded:
                        continue;
                    default:
                        break;
                }
            }
            File childAbspath = SVNFileUtil.createFilePath(dirAbspath, child);
            if (childKind == SVNWCDbKind.File || depth.getId() >= SVNDepth.IMMEDIATES.getId()) {
                if (matchesChangelist(childAbspath, changelists)) {
                    nodeHandler.nodeFound(childAbspath, childKind);
                }
            }
            if (childKind == SVNWCDbKind.Dir && depth.getId() >= SVNDepth.IMMEDIATES.getId()) {
                SVNDepth depth_below_here = depth;
                if (depth.getId() == SVNDepth.IMMEDIATES.getId()) {
                    depth_below_here = SVNDepth.EMPTY;
                }
                walkerHelper(childAbspath, nodeHandler, showHidden, depth_below_here, changelists);
            }
        }
    }

    public File acquireWriteLock(File localAbspath, boolean lockAnchor, boolean returnLockRoot) throws SVNException {
        localAbspath = obtainAnchorPath(localAbspath, lockAnchor, returnLockRoot);
        db.obtainWCLock(localAbspath, -1, false);
        return localAbspath;
    }

    public File obtainAnchorPath(File localAbspath, boolean lockAnchor, boolean returnLockRoot) throws SVNException {
        SVNWCDbKind kind = db.readKind(localAbspath, returnLockRoot);
        if (!returnLockRoot && kind != SVNWCDbKind.Dir) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "Can''t obtain lock on non-directory ''{0}''", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
            return null;
        }
        if (lockAnchor) {
            assert (returnLockRoot);
            File parentAbspath = SVNFileUtil.getParentFile(localAbspath);
            if (parentAbspath == null) {
                return localAbspath;
            }
            SVNWCDbKind parentKind = SVNWCDbKind.Unknown;
            try {
                parentKind = db.readKind(parentAbspath, true);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY &&
                        e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_UNSUPPORTED_FORMAT) {
                    throw e;
                }
            }
            if (kind == SVNWCDbKind.Dir && parentKind == SVNWCDbKind.Dir) {
                boolean disjoint = isChildDisjoint(localAbspath);
                if (!disjoint) {
                    localAbspath = parentAbspath;
                }
            } else if (parentKind == SVNWCDbKind.Dir) {
                localAbspath = parentAbspath;
            } else if (kind != SVNWCDbKind.Dir) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY, "''{0}'' is not a working copy", localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } else if (kind != SVNWCDbKind.Dir) {
            localAbspath = SVNFileUtil.getFileDir(localAbspath);
            if (kind == SVNWCDbKind.Unknown) {
                kind = db.readKind(localAbspath, false);
                if (kind != SVNWCDbKind.Dir) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "Can't obtain lock on non-directory ''{0}''", localAbspath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
        }
        return localAbspath;
    }

    private boolean isChildDisjoint(File localAbspath) throws SVNException {
        boolean disjoint = db.isWCRoot(localAbspath);
        if (disjoint) {
            return disjoint;
        }
        File parentAbspath = SVNFileUtil.getFileDir(localAbspath);
        String base = SVNFileUtil.getFileName(localAbspath);
        WCDbInfo readInfo = db.readInfo(localAbspath, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.reposUuid);
        SVNURL nodeReposRoot = readInfo.reposRootUrl;
        File nodeReposRelpath = readInfo.reposRelPath;
        String nodeReposUuid = readInfo.reposUuid;
        if (nodeReposRelpath == null) {
            disjoint = false;
            return disjoint;
        }
        readInfo = db.readInfo(parentAbspath, InfoField.status, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.reposUuid);
        SVNWCDbStatus parentStatus = readInfo.status;
        SVNURL parentReposRoot = readInfo.reposRootUrl;
        File parentReposRelpath = readInfo.reposRelPath;
        String parentReposUuid = readInfo.reposUuid;
        if (parentReposRelpath == null) {
            if (parentStatus == SVNWCDbStatus.Added) {
                WCDbAdditionInfo scanAddition = db.scanAddition(parentAbspath, AdditionInfoField.reposRelPath, AdditionInfoField.reposRelPath, AdditionInfoField.reposUuid);
                parentReposRelpath = scanAddition.reposRelPath;
                parentReposRoot = scanAddition.reposRootUrl;
                parentReposUuid = scanAddition.reposUuid;
            } else {
                WCDbRepositoryInfo scanBaseRepository = db.scanBaseRepository(parentAbspath, RepositoryInfoField.values());
                parentReposRelpath = scanBaseRepository.relPath;
                parentReposRoot = scanBaseRepository.rootUrl;
                parentReposUuid = scanBaseRepository.uuid;
            }
        }
        if (!parentReposRoot.equals(nodeReposRoot) || !parentReposUuid.equals(nodeReposUuid) || !SVNFileUtil.createFilePath(parentReposRelpath, base).equals(nodeReposRelpath)) {
            disjoint = true;
        } else {
            disjoint = false;
        }
        return disjoint;
    }

    public void releaseWriteLock(File localAbspath) throws SVNException {
        WCDbWorkQueueInfo wqInfo = db.fetchWorkQueue(localAbspath);
        if (wqInfo.workItem != null) {
            return;
        }
        
        db.releaseWCLock(localAbspath);
        return;
    }

    public static class CheckWCRootInfo {

        public boolean wcRoot;
        public SVNWCDbKind kind;
        public boolean switched;
    }

    public CheckWCRootInfo checkWCRoot(File localAbspath, boolean fetchSwitched) throws SVNException {
        File parentAbspath;
        String name;
        File reposRelpath;
        SVNURL reposRoot;
        String reposUuid;
        SVNWCDbStatus status;
        CheckWCRootInfo info = new CheckWCRootInfo();
        info.wcRoot = true;
        info.switched = false;
        WCDbInfo readInfo = db.readInfo(localAbspath, InfoField.status, InfoField.kind, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.reposUuid);
        status = readInfo.status;
        info.kind = readInfo.kind;
        reposRelpath = readInfo.reposRelPath;
        reposRoot = readInfo.reposRootUrl;
        reposUuid = readInfo.reposUuid;
        if (reposRelpath == null) {
            info.wcRoot = false;
            return info;
        }
        if (info.kind != SVNWCDbKind.Dir) {
            info.wcRoot = false;
        } else if (status == SVNWCDbStatus.Added || status == SVNWCDbStatus.Deleted) {
            info.wcRoot = false;
        } else if (status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.NotPresent) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
            return null;
        } else if (SVNFileUtil.getParentFile(localAbspath) == null) {
            return info;
        }

        if (!info.wcRoot && !fetchSwitched) {
            return info;
        }
        parentAbspath = SVNFileUtil.getParentFile(localAbspath);
        name = SVNFileUtil.getFileName(localAbspath);
        if (info.wcRoot) {
            boolean isRoot = db.isWCRoot(localAbspath);
            if (isRoot) {
                return info;
            }
        }
        {
            WCDbRepositoryInfo parent = db.scanBaseRepository(parentAbspath, RepositoryInfoField.relPath, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
            if (!reposRoot.equals(parent.rootUrl) || !reposUuid.equals(parent.uuid)) {
                return info;
            }
            info.wcRoot = false;
            if (fetchSwitched) {
                File expectedRelpath = SVNFileUtil.createFilePath(parent.relPath, name);
                info.switched = !expectedRelpath.equals(reposRelpath);
            }
        }
        return info;
    }

    public void exclude(File localAbspath) throws SVNException {
        boolean isRoot, isSwitched;
        SVNWCDbStatus status;
        SVNWCDbKind kind;
        long revision;
        File reposRelpath;
        SVNURL reposRoot;
        String reposUuid;
        boolean haveBase;
        CheckWCRootInfo checkWCRoot = checkWCRoot(localAbspath, true);
        isRoot = checkWCRoot.wcRoot;
        isSwitched = checkWCRoot.switched;
        if (isRoot) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot exclude ''{0}'': it is a working copy root", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
            return;
        }
        if (isSwitched) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot exclude ''{0}'': it is a switched path", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
            return;
        }
        WCDbInfo readInfo = db.readInfo(localAbspath, InfoField.status, InfoField.kind, InfoField.revision, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.reposUuid, InfoField.haveBase);
        status = readInfo.status;
        kind = readInfo.kind;
        revision = readInfo.revision;
        reposRelpath = readInfo.reposRelPath;
        reposRoot = readInfo.reposRootUrl;
        reposUuid = readInfo.reposUuid;
        haveBase = readInfo.haveBase;
        switch (status) {
            case ServerExcluded:
            case Excluded:
            case NotPresent: {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }
            case Added: {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot exclude ''{0}'': it is to be added to the repository. Try commit instead", localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }
            case Deleted: {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot exclude ''{0}'': it is to be deleted from the repository. Try commit instead", localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }
            case Normal:
            case Incomplete:
            default:
                break;
        }
        if (haveBase) {
            WCDbBaseInfo baseInfo = db.getBaseInfo(localAbspath, BaseInfoField.kind, BaseInfoField.revision, BaseInfoField.reposRelPath, BaseInfoField.reposRootUrl, BaseInfoField.reposUuid);
            kind = baseInfo.kind;
            revision = baseInfo.revision;
            reposRelpath = baseInfo.reposRelPath;
            reposRoot = baseInfo.reposRootUrl;
            reposUuid = baseInfo.reposUuid;
        }
        try {
            removeFromRevisionControl(localAbspath, true, false);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_LEFT_LOCAL_MOD) {
                throw e;
            }
        }
        db.addBaseExcludedNode(localAbspath, reposRelpath, reposRoot, reposUuid, revision, kind, SVNWCDbStatus.Excluded, null, null);
        if (getEventHandler() != null) {
            SVNEvent event = new SVNEvent(localAbspath, null, null, -1, null, null, null, null, SVNEventAction.DELETE, null, null, null, null, null, null);
            getEventHandler().handleEvent(event, 0);
        }
        return;
    }

    public static class CheckSpecialInfo {

        public SVNNodeKind kind;
        public boolean isSpecial;
    }

    public static CheckSpecialInfo checkSpecialPath(File localAbspath) {
        CheckSpecialInfo info = new CheckSpecialInfo();
        SVNFileType fileType = SVNFileType.getType(localAbspath);
        info.kind = SVNFileType.getNodeKind(fileType);
        info.isSpecial = !SVNFileUtil.symlinksSupported() ? false : fileType == SVNFileType.SYMLINK;
        return info;
    }

    public void removeFromRevisionControl(File localAbspath, boolean destroyWf, boolean instantError) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        checkCancelled();
        boolean leftSomething = false;
        WCDbInfo readInfo = db.readInfo(localAbspath, InfoField.status, InfoField.kind);
        SVNWCDbKind kind = readInfo.kind;
        if (kind == SVNWCDbKind.File || kind == SVNWCDbKind.Symlink) {
            boolean textModified = false;
            SvnChecksum baseSha1Checksum = null;
            SvnChecksum workingSha1Checksum = null;
            boolean wcSpecial = isSpecial(localAbspath);
            CheckSpecialInfo checkSpecialPath = checkSpecialPath(localAbspath);
            boolean localSpecial = checkSpecialPath.isSpecial;
            if (wcSpecial || !localSpecial) {
                textModified = isTextModified(localAbspath, false);
                if (textModified && instantError) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_LEFT_LOCAL_MOD, "File ''{0}'' has local modifications", localAbspath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                    return;
                }
            }
            try {
                baseSha1Checksum = db.getBaseInfo(localAbspath, BaseInfoField.checksum).checksum;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
            try {
                workingSha1Checksum = db.readInfo(localAbspath, InfoField.checksum).checksum;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
            db.opRemoveEntryTemp(localAbspath);
            if (baseSha1Checksum != null) {
                db.removePristine(localAbspath, baseSha1Checksum);
            }
            if (workingSha1Checksum != null && !workingSha1Checksum.equals(baseSha1Checksum)) {
                db.removePristine(localAbspath, workingSha1Checksum);
            }
            if (destroyWf) {
                if ((!wcSpecial && localSpecial) || textModified) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_LEFT_LOCAL_MOD);
                    SVNErrorManager.error(err, SVNLogType.WC);
                    return;
                }
                SVNFileUtil.deleteFile(localAbspath);
            }
        } else {
            Set<String> children = db.readChildren(localAbspath);
            for (String entryName : children) {
                File entryAbspath = SVNFileUtil.createFilePath(localAbspath, entryName);
                boolean hidden = db.isNodeHidden(entryAbspath);
                if (hidden) {
                    db.opRemoveEntryTemp(entryAbspath);
                    continue;
                }
                try {
                    removeFromRevisionControl(entryAbspath, destroyWf, instantError);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_LEFT_LOCAL_MOD) {
                        if (instantError) {
                            throw e;
                        }
                        leftSomething = true;
                    } else if (e.getErrorMessage().getErrorCode() == SVNErrorCode.IO_ERROR) {
                        if (instantError) {
                            throw e;
                        }
                        leftSomething = true;
                    } else {
                        throw e;
                    }
                }
            }
            {
                boolean isRoot = checkWCRoot(localAbspath, false).wcRoot;
                if (!isRoot) {
                    db.opRemoveEntryTemp(localAbspath);
                }
            }
            destroyAdm(localAbspath);
            if (destroyWf && (!leftSomething)) {
                try {
                    SVNFileUtil.deleteFile(localAbspath);
                } catch (SVNException e) {
                    leftSomething = true;
                }
            }

        }
        if (leftSomething) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_LEFT_LOCAL_MOD);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    private void destroyAdm(File dirAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(dirAbspath));
        writeCheck(dirAbspath);
        File admAbspath = db.getWCRoot(dirAbspath);
        db.forgetDirectoryTemp(dirAbspath);
        if (admAbspath.equals(dirAbspath)) {
            SVNFileUtil.deleteAll(SVNWCUtils.admChild(admAbspath, null), true, getEventHandler());
        }
    }

    public void cropTree(File localAbspath, SVNDepth depth) throws SVNException {

        if (depth == SVNDepth.INFINITY) {
            return;
        }
        if (!(depth.getId() > SVNDepth.EXCLUDE.getId() && depth.getId() < SVNDepth.INFINITY.getId())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Can only crop a working copy with a restrictive depth");
            SVNErrorManager.error(err, SVNLogType.WC);
            return;
        }

        {
            WCDbInfo readInfo = db.readInfo(localAbspath, InfoField.status, InfoField.kind);
            SVNWCDbStatus status = readInfo.status;
            SVNWCDbKind kind = readInfo.kind;

            if (kind != SVNWCDbKind.Dir) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Can only crop directories");
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }

            if (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.ServerExcluded) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }

            if (status == SVNWCDbStatus.Deleted) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot crop ''{0}'': it is going to be removed from repository. Try commit instead", localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }

            if (status == SVNWCDbStatus.Added) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot crop ''{0}'': it is to be added to the repository. Try commit instead", localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }
        }
        cropChildren(localAbspath, depth);
    }

    private void cropChildren(File localAbspath, SVNDepth depth) throws SVNException {
        assert (depth.getId() >= SVNDepth.EMPTY.getId() && depth.getId() <= SVNDepth.INFINITY.getId());
        checkCancelled();
        SVNDepth dirDepth = db.readInfo(localAbspath, InfoField.depth).depth;
        if (dirDepth == SVNDepth.UNKNOWN) {
            dirDepth = SVNDepth.INFINITY;
        }
        if (dirDepth.getId() > depth.getId()) {
            db.opSetDirDepthTemp(localAbspath, depth);
        }
        Set<String> children = db.readChildren(localAbspath);
        for (String childName : children) {
            File childAbspath = SVNFileUtil.createFilePath(localAbspath, childName);
            WCDbInfo readInfo = db.readInfo(childAbspath, InfoField.status, InfoField.kind, InfoField.depth);
            SVNWCDbStatus childStatus = readInfo.status;
            SVNWCDbKind kind = readInfo.kind;
            if (childStatus == SVNWCDbStatus.ServerExcluded || childStatus == SVNWCDbStatus.Excluded || childStatus == SVNWCDbStatus.NotPresent) {
                SVNDepth removeBelow = (kind == SVNWCDbKind.Dir) ? SVNDepth.IMMEDIATES : SVNDepth.FILES;
                if (depth.getId() < removeBelow.getId()) {
                    db.opRemoveEntryTemp(localAbspath);
                }
                continue;
            } else if (kind == SVNWCDbKind.File) {
                if (depth == SVNDepth.EMPTY) {
                    try {
                        removeFromRevisionControl(childAbspath, true, false);
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_LEFT_LOCAL_MOD) {
                            throw e;
                        }
                    }
                } else {
                    continue;
                }
            } else if (kind == SVNWCDbKind.Dir) {
                if (depth.getId() < SVNDepth.IMMEDIATES.getId()) {
                    try {
                        removeFromRevisionControl(childAbspath, true, false);
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_LEFT_LOCAL_MOD) {
                            throw e;
                        }
                    }
                } else {
                    cropChildren(childAbspath, SVNDepth.EMPTY);
                    continue;
                }
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNKNOWN_KIND, "Unknown node kind for ''{0}''", childAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }
            if (getEventHandler() != null) {
                SVNEvent event = new SVNEvent(childAbspath, null, null, -1, null, null, null, null, SVNEventAction.DELETE, null, null, null, null, null, null);
                getEventHandler().handleEvent(event, 0);
            }
        }
    }

    public class SVNWCNodeReposInfo {

        public SVNURL reposRootUrl;
        public String reposUuid;
    }

    public SVNWCNodeReposInfo getNodeReposInfo(File localAbspath) throws SVNException {
        // s0 (to return)
        SVNWCNodeReposInfo info = new SVNWCNodeReposInfo();
        info.reposRootUrl = null;
        info.reposUuid = null;
        
        SVNWCDbStatus status;
        try {
            // s1
            WCDbInfo readInfo = db.readInfo(localAbspath, InfoField.status, InfoField.reposRootUrl, InfoField.reposUuid);
            status = readInfo.status;
            info.reposRootUrl = readInfo.reposRootUrl;
            info.reposUuid = readInfo.reposUuid;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND && e.
                    getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                throw e;
            }
            return info;
        }
        if (info.reposRootUrl != null && info.reposUuid != null) {
            return info;
        }
        WCDbRepositoryInfo reposInfo = null;
        WCDbAdditionInfo addInfo = null;
        
        if (status == SVNWCDbStatus.Deleted) {
            // s2
            WCDbDeletionInfo dinfo =db.scanDeletion(localAbspath, DeletionInfoField.baseDelAbsPath, DeletionInfoField.workDelAbsPath);
            if (dinfo.baseDelAbsPath != null) {
                // s3
                reposInfo = db.scanBaseRepository(dinfo.baseDelAbsPath, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
            } else if (dinfo.workDelAbsPath != null) {
                // s4
                addInfo = db.scanAddition(SVNFileUtil.getParentFile(dinfo.workDelAbsPath), 
                        AdditionInfoField.reposRootUrl, AdditionInfoField.reposUuid);
            }
        } else if (status == SVNWCDbStatus.Added) {
            // s5
            addInfo = db.scanAddition(localAbspath, 
                    AdditionInfoField.reposRootUrl, AdditionInfoField.reposUuid);
        } else {
            // s6
            reposInfo = db.scanBaseRepository(localAbspath, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
        }
        if (addInfo != null) {
            info.reposRootUrl = addInfo.reposRootUrl;
            info.reposUuid = addInfo.reposUuid;
        } else if (reposInfo != null) {
            info.reposRootUrl = reposInfo.rootUrl;
            info.reposUuid = reposInfo.uuid;
        }
        return info;
    }

    public SVNTreeConflictDescription getTreeConflict(File victimAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(victimAbspath));
        return db.opReadTreeConflict(victimAbspath);
    }

    public void writeCheck(File localAbspath) throws SVNException {
        boolean locked = db.isWCLockOwns(localAbspath, false);
        if (!locked) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "No write-lock in ''{0}''", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    public SVNProperties getPristineProps(File localAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        SVNWCDbStatus status = db.readInfo(localAbspath, InfoField.status).status;
        if (status == SVNWCDbStatus.Added) {
            status = db.scanAddition(localAbspath, AdditionInfoField.status).status;
        }
        if (status == SVNWCDbStatus.Added || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.NotPresent) {
            return null;
        }
        return db.readPristineProperties(localAbspath);
    }

    public SVNProperties getActualProps(File localAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        return db.readProperties(localAbspath);
    }
    
    public MergePropertiesInfo mergeProperties(File localAbsPath, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, 
            SVNProperties baseProperties, SVNProperties propChanges, boolean dryRun) throws SVNException {
        Structure<NodeInfo> info = getDb().readInfo(localAbsPath, NodeInfo.status, NodeInfo.kind, NodeInfo.hadProps, NodeInfo.propsMod, NodeInfo.haveBase);
        SVNWCDbStatus status = info.get(NodeInfo.status);
        
        if (status == SVNWCDbStatus.NotPresent 
                || status == SVNWCDbStatus.ServerExcluded
                || status == SVNWCDbStatus.Excluded) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        } else if (status != SVNWCDbStatus.Normal
                && status != SVNWCDbStatus.Added
                && status != SVNWCDbStatus.Incomplete) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, 
                    "The node ''{0}'' does not have properties in this state.", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        for (String propName : propChanges.nameSet()) {
            if (!SVNProperty.isRegularProperty(propName)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_PROP_KIND, 
                        "The property ''{0}'' may not be merged into ''{1}''", propName, localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        SVNProperties pristineProps = null;
        if (info.is(NodeInfo.hadProps)) {
            pristineProps = getPristineProps(localAbsPath);
        }
        if (pristineProps == null) {
            pristineProps = new SVNProperties();
        }
        SVNProperties actualProps;
        if (info.is(NodeInfo.propsMod)) {
            actualProps = getActualProperties(localAbsPath);
        } else {
            actualProps = new SVNProperties(pristineProps);
        }
        SVNWCDbKind kind = info.get(NodeInfo.kind);
        info.release();
        MergePropertiesInfo result = mergeProperties2(null, localAbsPath, kind, leftVersion, rightVersion, baseProperties, pristineProps, actualProps, propChanges, false, dryRun);
        if (dryRun) {
            return result;
        }
        File dirAbsPath;
        if (kind == SVNWCDbKind.Dir) {
            dirAbsPath = localAbsPath;
        } else {
            dirAbsPath = SVNFileUtil.getParentFile(localAbsPath);
        }
        writeCheck(dirAbsPath);
        SVNSkel workItems = result.workItems;
        if (workItems == null) {
            workItems = SVNSkel.createEmptyList();
        }
        getDb().opSetProps(localAbsPath, result.newActualProperties, null, hasMagicProperty(propChanges), workItems);
        wqRun(localAbsPath);
        return result;
    }
    
    public MergePropertiesInfo mergeProperties2(MergePropertiesInfo mergeInfo, File localAbsPath, SVNWCDbKind kind, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion,
            SVNProperties serverBaseProperties, SVNProperties pristineProperties, SVNProperties actualProperties, SVNProperties propChanges, 
            boolean baseMerge, boolean dryRun) throws SVNException {
        if (serverBaseProperties == null) {
            serverBaseProperties = pristineProperties;
        }
        if (mergeInfo == null) {
            mergeInfo = new MergePropertiesInfo();
        }
        mergeInfo.mergeOutcome = SVNStatusType.UNCHANGED;
        
        DefaultSvnMerger defaultMerger = createDefaultMerger();
        ISvnMerger customMerger = createCustomMerger();
        
        SvnMergeResult result;
        if (customMerger != null) {
            result = customMerger.mergeProperties(
                    defaultMerger,
                    localAbsPath, 
                    kind == null ? SVNNodeKind.UNKNOWN : kind.toNodeKind(), 
                    leftVersion, 
                    rightVersion, 
                    serverBaseProperties, 
                    pristineProperties, 
                    actualProperties, 
                    propChanges, 
                    baseMerge, 
                    dryRun);
        } else {
            result = defaultMerger.mergeProperties(
                    null,
                    localAbsPath, 
                    kind == null ? SVNNodeKind.UNKNOWN : kind.toNodeKind(), 
                    leftVersion, 
                    rightVersion, 
                    serverBaseProperties, 
                    pristineProperties, 
                    actualProperties, 
                    propChanges, 
                    baseMerge, 
                    dryRun);
        }        
        mergeInfo.mergeOutcome = result.getMergeOutcome();
        mergeInfo.newActualProperties = result.getActualProperties();
        mergeInfo.newBaseProperties = result.getBaseProperties();
        mergeInfo.workItems = defaultMerger.getWorkItems();
        return mergeInfo;
    }
    
    private ISvnMerger createCustomMerger() {
        if (getOptions() != null && getOptions().getMergerFactory() != null) {
            ISVNMerger merger = getOptions().getMergerFactory().createMerger(CONFLICT_START, CONFLICT_END, CONFLICT_SEPARATOR);
            if (merger instanceof ISvnMerger) {
                return (ISvnMerger) merger;
            }
        }
        return null;
    }

    private DefaultSvnMerger createDefaultMerger() {
        return new DefaultSvnMerger(this);
    }


    File getPrejfileAbspath(File localAbspath) throws SVNException {
        List<SVNConflictDescription> conflicts = db.readConflicts(localAbspath);
        for (SVNConflictDescription cd : conflicts) {
            if (cd.isPropertyConflict()) {
                if (cd.getMergeFiles().getRepositoryPath().equals(THIS_DIR_PREJ + PROP_REJ_EXT)) {
                    return SVNFileUtil.createFilePath(localAbspath, THIS_DIR_PREJ + PROP_REJ_EXT);
                }
                return SVNFileUtil.createFilePath(cd.getMergeFiles().getRepositoryPath());
            }
        }
        return null;
    }

    void conflictSkelAddPropConflict(SVNSkel skel, String propName, SVNPropertyValue baseVal, SVNPropertyValue mineVal, SVNPropertyValue toVal, SVNPropertyValue fromVal) throws SVNException {
        SVNSkel propSkel = SVNSkel.createEmptyList();
        prependPropValue(fromVal, propSkel);
        prependPropValue(toVal, propSkel);
        prependPropValue(mineVal, propSkel);
        prependPropValue(baseVal, propSkel);
        
        propSkel.prependString(propName);
        propSkel.prependString(CONFLICT_KIND_PROP);
        skel.appendChild(propSkel);
    }

    private void prependPropValue(SVNPropertyValue fromVal, SVNSkel skel) throws SVNException {
        SVNSkel valueSkel = SVNSkel.createEmptyList();
        if (fromVal != null && (fromVal.getBytes() != null || fromVal.getString() != null)) {
            valueSkel.prependPropertyValue(fromVal);
        }
        skel.addChild(valueSkel);
    }

    SVNStatusType setPropMergeState(SVNStatusType state, SVNStatusType newValue) {
        if (state == null) {
            return null;
        }
        int statusInd = STATUS_ORDERING.indexOf(state);
        int newStatusInd = STATUS_ORDERING.indexOf(newValue);
        if (newStatusInd <= statusInd) {
            return state;
        }
        return newValue;
    }

    static class MergePropStatusInfo {

        public MergePropStatusInfo(SVNStatusType state, boolean conflictRemains) {
            this.state = state;
            this.conflictRemains = conflictRemains;
        }

        public SVNStatusType state;
        public boolean conflictRemains;
    }

    MergePropStatusInfo applySinglePropAdd(SVNStatusType state, File localAbspath, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, boolean isDir, SVNProperties workingProps,
            String propname, SVNPropertyValue baseVal, SVNPropertyValue toVal, ISVNConflictHandler conflictResolver, boolean dryRun) throws SVNException {
        boolean conflictRemains = false;
        SVNPropertyValue workingVal = workingProps.getSVNPropertyValue(propname);
        if (workingVal != null) {
            if (workingVal.equals(toVal)) {
                state = setPropMergeState(state, SVNStatusType.MERGED);
            } else {
                if (SVNProperty.MERGE_INFO.equals(propname)) {
                    String mergedVal = SVNMergeInfoUtil.combineMergeInfoProperties(workingVal.getString(), toVal.getString());
                    workingProps.put(propname, mergedVal);
                    state = setPropMergeState(state, SVNStatusType.MERGED);
                } else {
                    conflictRemains = maybeGeneratePropConflict(localAbspath, leftVersion, rightVersion, isDir, propname, workingProps, null, toVal, baseVal, workingVal, conflictResolver, dryRun);
                }
            }
        } else if (baseVal != null) {
            conflictRemains = maybeGeneratePropConflict(localAbspath, leftVersion, rightVersion, isDir, propname, workingProps, null, toVal, baseVal, null, conflictResolver, dryRun);
        } else {
            workingProps.put(propname, toVal);
        }
        return new MergePropStatusInfo(state, conflictRemains);
    }

    private boolean maybeGeneratePropConflict(File localAbspath, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, boolean isDir, String propname, SVNProperties workingProps,
            SVNPropertyValue oldVal, SVNPropertyValue newVal, SVNPropertyValue baseVal, SVNPropertyValue workingVal, ISVNConflictHandler conflictResolver, boolean dryRun) throws SVNException {
        if (conflictResolver == null || dryRun) {
            return true;
        }
        oldVal = oldVal == null || (oldVal.getString() == null && oldVal.getBytes() == null) ? null : oldVal;
        newVal = newVal == null || (newVal.getString() == null && newVal.getBytes() == null) ? null : newVal;
        baseVal = baseVal == null || (baseVal.getString() == null && baseVal.getBytes() == null) ? null : baseVal;
        workingVal = workingVal == null || (workingVal.getString() == null && workingVal.getBytes() == null) ? null : workingVal;
        
        boolean conflictRemains = false;
        SVNWCConflictDescription17 cdesc = SVNWCConflictDescription17.createProp(localAbspath, isDir ? SVNNodeKind.DIR : SVNNodeKind.FILE, propname);
        cdesc.setSrcLeftVersion(leftVersion);
        cdesc.setSrcRightVersion(rightVersion);
        try {
            if (workingVal != null) {
                cdesc.setMyFile(writeUnique(localAbspath, SVNPropertyValue.getPropertyAsBytes(workingVal)));
            }
            if (newVal != null) {
                cdesc.setTheirFile(writeUnique(localAbspath, SVNPropertyValue.getPropertyAsBytes(newVal)));
            }
            if (baseVal == null && oldVal == null) {
            } else if ((baseVal != null && oldVal == null) || (baseVal == null && oldVal != null)) {
                SVNPropertyValue theVal = baseVal != null ? baseVal : oldVal;
                cdesc.setBaseFile(writeUnique(localAbspath, SVNPropertyValue.getPropertyAsBytes(theVal)));
            } else {
                SVNPropertyValue theVal;
                if (!baseVal.equals(oldVal)) {
                    if (workingVal != null && baseVal.equals(workingVal))
                        theVal = oldVal;
                    else
                        theVal = baseVal;
                } else {
                    theVal = baseVal;
                }
                cdesc.setBaseFile(writeUnique(localAbspath, SVNPropertyValue.getPropertyAsBytes(theVal)));
                if (workingVal != null && newVal != null) {
                    FSMergerBySequence merger = new FSMergerBySequence(CONFLICT_START, CONFLICT_SEPARATOR, CONFLICT_END);
                    OutputStream result = null;
                    try {
                        cdesc.setMergedFile(SVNFileUtil.createUniqueFile(SVNFileUtil.getFileDir(localAbspath), SVNFileUtil.getFileName(localAbspath), ".tmp", false));
                        result = SVNFileUtil.openFileForWriting(cdesc.getMergedFile());
                        QSequenceLineRAData baseData = new QSequenceLineRAByteData(SVNPropertyValue.getPropertyAsBytes(theVal));
                        QSequenceLineRAData localData = new QSequenceLineRAByteData(SVNPropertyValue.getPropertyAsBytes(workingVal));
                        QSequenceLineRAData latestData = new QSequenceLineRAByteData(SVNPropertyValue.getPropertyAsBytes(newVal));
                        merger.merge(baseData, localData, latestData, null, result, null);
                    } catch (IOException e) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
                        SVNErrorManager.error(err, e, SVNLogType.WC);
                    } finally {
                        SVNFileUtil.closeFile(result);
                    }
                }
            }
            String mimePropval = null;
            if (!isDir && workingProps != null) {
                mimePropval = workingProps.getStringValue(SVNProperty.MIME_TYPE);
            }
            cdesc.setMimeType(mimePropval);
            cdesc.setBinary(mimePropval != null ? mimeTypeIsBinary(mimePropval) : false);
            if (oldVal == null && newVal != null) {
                cdesc.setAction(SVNConflictAction.ADD);
            } else if (oldVal != null && newVal == null) {
                cdesc.setAction(SVNConflictAction.DELETE);
            } else {
                cdesc.setAction(SVNConflictAction.EDIT);
            }
            if (baseVal != null && workingVal == null) {
                cdesc.setReason(SVNConflictReason.DELETED);
            } else if (baseVal == null && workingVal != null) {
                cdesc.setReason(SVNConflictReason.OBSTRUCTED);
            } else {
                cdesc.setReason(SVNConflictReason.EDITED);
            }
            SVNConflictResult result = null;
            {
                SVNConflictDescription cd = cdesc.toConflictDescription();
                result = conflictResolver.handleConflict(cd);
            }
            if (result == null) {
                conflictRemains = true;
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "Conflict callback violated API: returned no results.");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            SVNConflictChoice conflictChoice = result.getConflictChoice();
            if (conflictChoice == SVNConflictChoice.POSTPONE) {
                conflictRemains = true;
            } else if (conflictChoice == SVNConflictChoice.MINE_FULL) {
                conflictRemains = false;
            } else if (conflictChoice == SVNConflictChoice.THEIRS_FULL) {
                workingProps.put(propname, newVal);
                conflictRemains = false;
            } else if (conflictChoice == SVNConflictChoice.BASE) {
                workingProps.put(propname, baseVal);
                conflictRemains = false;
            } else if (conflictChoice == SVNConflictChoice.MERGED) {
                if (cdesc.getMergedFile() == null && result.getMergedFile() == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "Conflict callback violated API: returned no merged file.");
                    SVNErrorManager.error(err, SVNLogType.WC);
                } else {
                    String mergedString = SVNFileUtil.readFile(result.getMergedFile() != null ? result.getMergedFile() : cdesc.getMergedFile());
                    workingProps.put(propname, mergedString);
                    conflictRemains = false;
                }
            } else {
                conflictRemains = true;
            }
            return conflictRemains;
        } finally {
            SVNFileUtil.deleteFile(cdesc.getBaseFile());
            SVNFileUtil.deleteFile(cdesc.getMyFile());
            SVNFileUtil.deleteFile(cdesc.getTheirFile());
            SVNFileUtil.deleteFile(cdesc.getMergedFile());
        }
    }

    private boolean mimeTypeIsBinary(String mimeType) {
        int len = mimeType.indexOf(';');
        if (len == -1) {
            len = mimeType.indexOf(' ');
        }
        return ((!"text/".equals(mimeType.substring(0, 5))) && (len != 15 || "image/x-xbitmap".equals(mimeType.substring(0, len))));
    }

    private File writeUnique(File path, byte[] value) throws SVNException {
        File tmpPath = SVNFileUtil.createUniqueFile(SVNFileUtil.getFileDir(path), SVNFileUtil.getFileName(path), ".tmp", false);
        OutputStream os = SVNFileUtil.openFileForWriting(tmpPath);
        try {
            os.write(value);
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
            SVNErrorManager.error(err, e, SVNLogType.WC);
        } finally {
            SVNFileUtil.closeFile(os);
        }
        return tmpPath;
    }

    MergePropStatusInfo applySinglePropDelete(SVNStatusType state, File localAbspath, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, boolean isDir,
            SVNProperties workingProps, String propname, SVNPropertyValue baseVal, SVNPropertyValue oldVal, ISVNConflictHandler conflictResolver, boolean dryRun) throws SVNException {
        boolean conflictRemains = false;
        SVNPropertyValue workingVal = workingProps.getSVNPropertyValue(propname);
        if (baseVal == null) {
            if (workingVal != null && !workingVal.equals(oldVal)) {
                conflictRemains = maybeGeneratePropConflict(localAbspath, leftVersion, rightVersion, isDir, propname, workingProps, oldVal, null, baseVal, workingVal, conflictResolver, dryRun);
            } else {
                workingProps.remove(propname);
                if (oldVal != null) {
                    state = setPropMergeState(state, SVNStatusType.MERGED);
                }
            }
        } else if (baseVal.equals(oldVal)) {
            if (workingVal != null) {
                if (workingVal.equals(oldVal)) {
                    workingProps.remove(propname);
                } else {
                    conflictRemains = maybeGeneratePropConflict(localAbspath, leftVersion, rightVersion, isDir, propname, workingProps, oldVal, null, baseVal, workingVal, conflictResolver, dryRun);
                }
            } else {
                state = setPropMergeState(state, SVNStatusType.MERGED);
            }
        } else {
            conflictRemains = maybeGeneratePropConflict(localAbspath, leftVersion, rightVersion, isDir, propname, workingProps, oldVal, null, baseVal, workingVal, conflictResolver, dryRun);
        }
        return new MergePropStatusInfo(state, conflictRemains);
    }

    MergePropStatusInfo applySinglePropChange(SVNStatusType state, File localAbspath, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, boolean isDir,
            SVNProperties workingProps, String propname, SVNPropertyValue baseVal, SVNPropertyValue oldVal, SVNPropertyValue newVal, ISVNConflictHandler conflictResolver, boolean dryRun)
            throws SVNException {
        if (SVNProperty.MERGE_INFO.equals(propname)) {
            return applySingleMergeinfoPropChange(state, localAbspath, leftVersion, rightVersion, isDir, workingProps, propname, baseVal, oldVal, newVal, conflictResolver, dryRun);
        }
        return applySingleGenericPropChange(state, localAbspath, leftVersion, rightVersion, isDir, workingProps, propname, baseVal, oldVal, newVal, conflictResolver, dryRun);
    }

    private MergePropStatusInfo applySingleGenericPropChange(SVNStatusType state, File localAbspath, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, boolean isDir,
            SVNProperties workingProps, String propname, SVNPropertyValue baseVal, SVNPropertyValue oldVal, SVNPropertyValue newVal, ISVNConflictHandler conflictResolver, boolean dryRun)
            throws SVNException {
        assert (oldVal != null);
        boolean conflictRemains = false;
        SVNPropertyValue workingVal = workingProps.getSVNPropertyValue(propname);
        if (workingVal != null && oldVal != null && workingVal.equals(oldVal)) {
            workingProps.put(propname, newVal);
        } else {
            conflictRemains = maybeGeneratePropConflict(localAbspath, leftVersion, rightVersion, isDir, propname, workingProps, oldVal, newVal, baseVal, workingVal, conflictResolver, dryRun);
        }
        return new MergePropStatusInfo(state, conflictRemains);
    }

    private MergePropStatusInfo applySingleMergeinfoPropChange(SVNStatusType state, File localAbspath, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, boolean isDir,
            SVNProperties workingProps, String propname, SVNPropertyValue baseVal, SVNPropertyValue oldVal, SVNPropertyValue newVal, ISVNConflictHandler conflictResolver, boolean dryRun)
            throws SVNException {
        boolean conflictRemains = false;
        SVNPropertyValue workingVal = workingProps.getSVNPropertyValue(propname);
        if ((workingVal != null && baseVal == null) || (workingVal == null && baseVal != null) || (workingVal != null && baseVal != null && !workingVal.equals(baseVal))) {
            if (workingVal != null) {
                if (workingVal.equals(newVal)) {
                    state = setPropMergeState(state, SVNStatusType.MERGED);
                } else {
                    newVal = SVNPropertyValue.create(SVNMergeInfoUtil.combineForkedMergeInfoProperties(oldVal.getString(), workingVal.getString(), newVal.getString()));
                    if (newVal != null) {
                        workingProps.put(propname, newVal);
                    } else {
                        workingProps.remove(propname);
                    }
                    state = setPropMergeState(state, SVNStatusType.MERGED);
                }
            } else {
                conflictRemains = maybeGeneratePropConflict(localAbspath, leftVersion, rightVersion, isDir, propname, workingProps, oldVal, newVal, baseVal, workingVal, conflictResolver, dryRun);
            }
        } else if (workingVal == null) {
            Map<String, SVNMergeRangeList> deleted = new HashMap<String, SVNMergeRangeList>();
            Map<String, SVNMergeRangeList> added = new HashMap<String, SVNMergeRangeList>();
            SVNMergeInfoUtil.diffMergeInfoProperties(deleted, added, oldVal.getString(), null, newVal.toString(), null);
            String mergeinfoString = SVNMergeInfoUtil.formatMergeInfoToString(added, null);
            workingProps.put(propname, mergeinfoString);
        } else {
            if (oldVal.equals(baseVal)) {
                if (newVal != null) {
                    workingProps.put(propname, newVal);
                } else {
                    workingProps.remove(propname);
                }
            } else {
                newVal = SVNPropertyValue.create(SVNMergeInfoUtil.combineForkedMergeInfoProperties(oldVal.getString(), workingVal.getString(), newVal.getString()));
                if (newVal != null) {
                    workingProps.put(propname, newVal);
                } else {
                    workingProps.remove(propname);
                }
                state = setPropMergeState(state, SVNStatusType.MERGED);
            }
        }
        return new MergePropStatusInfo(state, conflictRemains);
    }

    public static class WritableBaseInfo {

        public OutputStream stream;
        public File tempBaseAbspath;
        public SVNChecksumOutputStream md5ChecksumStream;
        public SVNChecksumOutputStream sha1ChecksumStream;

        public SvnChecksum getMD5Checksum() {
            return md5ChecksumStream == null ? null : new SvnChecksum(SvnChecksum.Kind.md5, md5ChecksumStream.getDigest());
        }

        public SvnChecksum getSHA1Checksum() {
            return sha1ChecksumStream == null ? null : new SvnChecksum(SvnChecksum.Kind.sha1, sha1ChecksumStream.getDigest());
        }

    }

    public WritableBaseInfo openWritableBase(File localAbspath, boolean md5Checksum, boolean sha1Checksum) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        WritableBaseInfo info = new WritableBaseInfo();
        File tempDirAbspath = db.getPristineTempDir(localAbspath);
        info.tempBaseAbspath = SVNFileUtil.createUniqueFile(tempDirAbspath, "svn", ".tmp", true);
        info.stream = SVNFileUtil.openFileForWriting(info.tempBaseAbspath);
        if (md5Checksum) {
            info.md5ChecksumStream = new SVNChecksumOutputStream(info.stream, SVNChecksumOutputStream.MD5_ALGORITHM, true);
            info.stream = info.md5ChecksumStream;
        }
        if (sha1Checksum) {
            info.sha1ChecksumStream = new SVNChecksumOutputStream(info.stream, "SHA1", true);
            info.stream = info.sha1ChecksumStream;
        }
        return info;
    }

    public boolean hasMagicProperty(SVNProperties properties) {
        for (Iterator<String> i = properties.nameSet().iterator(); i.hasNext();) {
            String property = i.next();
            if (SVNProperty.EXECUTABLE.equals(property) || SVNProperty.KEYWORDS.equals(property) || SVNProperty.EOL_STYLE.equals(property) || SVNProperty.SPECIAL.equals(property)
                    || SVNProperty.NEEDS_LOCK.equals(property))
                return true;
        }
        return false;
    }

    public InputStream getTranslatedStream(File localAbspath, File versionedAbspath, boolean translateToNormalForm, boolean repairEOL) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        assert (SVNFileUtil.isAbsolute(versionedAbspath));
        TranslateInfo translateInfo = getTranslateInfo(localAbspath, true, true, true, true);
        boolean special = translateInfo.special;
        SVNEolStyle eolStyle = translateInfo.eolStyleInfo.eolStyle;
        byte[] eolStr = translateInfo.eolStyleInfo.eolStr;
        Map<String, byte[]> keywords = translateInfo.keywords;
        if (special) {
            return readSpecialFile(localAbspath);
        }
        String charset = translateInfo.charset;
        boolean translationRequired = special || keywords != null || eolStyle != null || charset != null;
        if (translationRequired) {
            if (translateToNormalForm) {
                if (eolStyle == SVNEolStyle.Native)
                    eolStr = SVNTranslator.getBaseEOL(SVNProperty.EOL_STYLE_NATIVE);
                else if (eolStyle == SVNEolStyle.Fixed)
                    repairEOL = true;
                else if (eolStyle != SVNEolStyle.None) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_UNKNOWN_EOL);
                    SVNErrorManager.error(err, SVNLogType.DEFAULT);
                    return null;
                }
                return SVNTranslator.getTranslatingInputStream(SVNFileUtil.openFileForReading(localAbspath, SVNLogType.WC), charset, eolStr, repairEOL, keywords, false);
            }
        }
        return SVNFileUtil.openFileForReading(localAbspath, SVNLogType.WC);
    }

    /**
     * When expanding working copy file (which is already expanded, we just have to update EOLs, keywords, etc)
     * One has to pass safelyEncode argument set to true as for this case we have to carefully update necessary parts of
     * the file taking its encoding into account.
     *
     * @param src
     * @param versionedAbspath
     * @param toNormalFormat
     * @param forceEOLRepair
     * @param useGlobalTmp
     * @param forceCopy
     * @param safelyEncode
     * @return
     * @throws SVNException
     */
    public File getTranslatedFile(File src, File versionedAbspath, boolean toNormalFormat, boolean forceEOLRepair, boolean useGlobalTmp, boolean forceCopy, boolean safelyEncode) throws SVNException {
        assert (SVNFileUtil.isAbsolute(versionedAbspath));
        TranslateInfo translateInfo = getTranslateInfo(versionedAbspath, true, true, true, true);
        SVNEolStyle style = translateInfo.eolStyleInfo.eolStyle;
        byte[] eol = translateInfo.eolStyleInfo.eolStr;
        String charset = translateInfo.charset;
        Map<String, byte[]> keywords = translateInfo.keywords;
        boolean special = translateInfo.special;
        File xlated_path;
        if (!isTranslationRequired(style, eol, charset, keywords, special, true) && !forceCopy) {
            xlated_path = src;
        } else {
            File tmpDir;
            File tmpVFile;
            boolean repairForced = forceEOLRepair;
            boolean expand = !toNormalFormat;
            if (useGlobalTmp) {
                tmpDir = null;
            } else {
                tmpDir = db.getWCRootTempDir(versionedAbspath);
            }
            tmpVFile = SVNFileUtil.createUniqueFile(tmpDir, src.getName(), ".tmp", false);
            if (expand) {
                repairForced = true;
            } else {
                if (style == SVNEolStyle.Native) {
                    eol = SVNEolStyleInfo.NATIVE_EOL_STR;
                } else if (style == SVNEolStyle.Fixed) {
                    repairForced = true;
                } else if (style != SVNEolStyle.None) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_UNKNOWN_EOL);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }

            if (expand && charset != null && safelyEncode) {
                File tmp = SVNFileUtil.createUniqueFile(tmpDir, src.getName(), ".tmp", false);
                try {
                    SVNTranslator.copyAndTranslate(src, tmp, charset, eol, keywords, special, false, repairForced);
                    SVNTranslator.copyAndTranslate(tmp, tmpVFile, charset, eol, keywords, special, true, repairForced);
                } finally {
                    SVNFileUtil.deleteFile(tmp);
                }
            } else {
                SVNTranslator.copyAndTranslate(src, tmpVFile, charset, eol, keywords, special, expand, repairForced);
            }

            xlated_path = tmpVFile;
        }
        return xlated_path.getAbsoluteFile();
    }

    public static class MergeInfo {

        public SVNSkel workItems;
        public SVNStatusType mergeOutcome;

    }
    
    public static class MergePropertiesInfo {
        public SVNSkel workItems;
        public SVNStatusType mergeOutcome;
        public SVNProperties newBaseProperties;
        public SVNProperties newActualProperties;
        public boolean treeConflicted;
    }
    
    public MergeInfo mergeText(File left, File right, File target, String leftLabel, String rightLabel, String targetLabel, 
            SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, boolean dryRun, SVNDiffOptions options, SVNProperties propDiff) throws SVNException {
        if (!dryRun) {
            writeCheck(target);
        }
        MergeInfo result = new MergeInfo();
        SVNWCDbKind kind = getDb().readKind(target, true);
        if (kind == SVNWCDbKind.Unknown) {
            result.mergeOutcome = SVNStatusType.NO_MERGE;
            return result;
        }
        if (getDb().isNodeHidden(target)) {
            result.mergeOutcome = SVNStatusType.NO_MERGE;
            return result;
        }
        SVNProperties actualProps = getDb().readProperties(target);
        result = merge(left, leftVersion, right, rightVersion, target, target, leftLabel, rightLabel, targetLabel, actualProps, dryRun, options, propDiff);
        if (!dryRun) {
            getDb().addWorkQueue(target, result.workItems);
            wqRun(target);
        }
        return result;
    }

    public MergeInfo merge(File leftAbspath, SVNConflictVersion leftVersion, File rightAbspath, SVNConflictVersion rightVersion, File targetAbspath, File wriAbspath, String leftLabel,
            String rightLabel, String targetLabel, SVNProperties actualProps, boolean dryRun, SVNDiffOptions options, SVNProperties propDiff) throws SVNException {
        assert (SVNFileUtil.isAbsolute(leftAbspath));
        assert (SVNFileUtil.isAbsolute(rightAbspath));
        assert (SVNFileUtil.isAbsolute(targetAbspath));
        assert (wriAbspath == null || SVNFileUtil.isAbsolute(wriAbspath));
        MergeInfo info = new MergeInfo();
        info.workItems = null;
        if (wriAbspath == null) {
            SVNWCDbKind kind = db.readKind(targetAbspath, true);
            boolean hidden;

            if (kind == SVNWCDbKind.Unknown) {
                info.mergeOutcome = SVNStatusType.NO_MERGE; // svn_wc_merge_no_merge;
                return info;
            }

            hidden = db.isNodeHidden(targetAbspath);
            if (hidden) {
                info.mergeOutcome = SVNStatusType.NO_MERGE; // svn_wc_merge_no_merge;
                return info;
            }
        }
        boolean isBinary = false;
        SVNPropertyValue mimeprop = propDiff.getSVNPropertyValue(SVNProperty.MIME_TYPE);
        if (mimeprop != null && mimeprop.isString()) {
            isBinary = mimeTypeIsBinary(mimeprop.getString());
        } else {
            SVNPropertyValue value = actualProps.getSVNPropertyValue(SVNProperty.MIME_TYPE);            
            isBinary = value != null && mimeTypeIsBinary(value.getString());
        }
        
        File detranslatedTargetAbspath = detranslateWCFile(targetAbspath, !isBinary, propDiff, targetAbspath);
        leftAbspath = maybeUpdateTargetEols(leftAbspath, propDiff);
        info.mergeOutcome = SVNStatusType.NO_MERGE;
        ISvnMerger customMerger = createCustomMerger();
        if (isBinary || customMerger == null) {
            attemptTrivialMerge(info, leftAbspath, rightAbspath, targetAbspath, dryRun);
        }
        if (info.mergeOutcome == SVNStatusType.NO_MERGE) {
            if (isBinary) {
                if (dryRun) {
                    info.mergeOutcome = SVNStatusType.CONFLICTED;
                } else {
                    info = mergeBinaryFile(leftAbspath, rightAbspath, targetAbspath, leftLabel, rightLabel, targetLabel, leftVersion, rightVersion, detranslatedTargetAbspath, mimeprop, 
                            getOptions().getConflictResolver());
                }
            } else {
                info = mergeTextFile(customMerger, leftAbspath, rightAbspath, targetAbspath, wriAbspath, leftLabel, rightLabel, targetLabel, dryRun, options, leftVersion, rightVersion, null, detranslatedTargetAbspath,
                        mimeprop, getOptions().getConflictResolver());
            }
        }
        if (!dryRun) {
            SVNSkel workItem = wqBuildSyncFileFlags(targetAbspath);
            info.workItems = wqMerge(info.workItems, workItem);
        }
        return info;
    }

    private void attemptTrivialMerge(MergeInfo info, File leftAbspath, File rightAbspath, File targetAbspath, boolean dryRun) throws SVNException {
        SVNFileType ft = SVNFileType.getType(targetAbspath);
        if (ft != SVNFileType.FILE) {
            info.mergeOutcome = SVNStatusType.NO_MERGE;
            return;
        }
        boolean sameContents = SVNFileUtil.compareFiles(leftAbspath, targetAbspath, null);
        info.mergeOutcome = SVNStatusType.NO_MERGE;
        
        if (sameContents) {
            sameContents = SVNFileUtil.compareFiles(leftAbspath, rightAbspath, null);
            if (sameContents) {
                info.mergeOutcome = SVNStatusType.UNCHANGED;
            } else {
                info.mergeOutcome = SVNStatusType.MERGED;
                if (!dryRun) {
                    SVNSkel item = wqBuildFileInstall(targetAbspath, rightAbspath, false, false);
                    info.workItems = wqMerge(info.workItems, item);
                }                
            }
        }
    }

    private boolean isMarkedAsBinary(File localAbsPath) throws SVNException {
        String value = getProperty(localAbsPath, SVNProperty.MIME_TYPE);
        if (value != null && mimeTypeIsBinary(value)) {
            return true;
        }
        return false;
    }

    private File detranslateWCFile(File targetAbspath, boolean forceCopy, SVNProperties propDiff, File sourceAbspath) throws SVNException {
        boolean isBinary;
        SVNPropertyValue prop;
        SVNWCDbKind kind = db.readKind(targetAbspath, true);
        if (kind == SVNWCDbKind.File) {
            isBinary = isMarkedAsBinary(targetAbspath);
        } else {
            isBinary = false;
        }
        SVNEolStyle style;
        byte[] eol;
        String charset;
        Map<String, byte[]> keywords;
        boolean special;
        if (isBinary && (((prop = propDiff.getSVNPropertyValue(SVNProperty.MIME_TYPE)) != null && prop.isString() && mimeTypeIsBinary(prop.getString())) || prop == null)) {
            keywords = null;
            special = false;
            eol = null;
            charset = null;
            style = SVNEolStyle.None;
        } else if ((!isBinary) && (prop = propDiff.getSVNPropertyValue(SVNProperty.MIME_TYPE)) != null && prop.isString() && mimeTypeIsBinary(prop.getString())) {
            if (kind == SVNWCDbKind.File) {
                TranslateInfo translateInfo = getTranslateInfo(targetAbspath, true, true, true, true);
                style = translateInfo.eolStyleInfo.eolStyle;
                eol = translateInfo.eolStyleInfo.eolStr;
                charset = translateInfo.charset;
                special = translateInfo.special;
                keywords = translateInfo.keywords;
            } else {
                special = false;
                keywords = null;
                eol = null;
                charset = null;
                style = SVNEolStyle.None;
            }
        } else {
            if (kind == SVNWCDbKind.File) {
                TranslateInfo translateInfo = getTranslateInfo(targetAbspath, true, true, true, true);
                style = translateInfo.eolStyleInfo.eolStyle;
                eol = translateInfo.eolStyleInfo.eolStr;
                charset = translateInfo.charset;
                special = translateInfo.special;
                keywords = translateInfo.keywords;
            } else {
                special = false;
                keywords = null;
                eol = null;
                charset = null;
                style = SVNEolStyle.None;
            }
            if (special) {
                keywords = null;
                eol = null;
                charset = null;
                style = SVNEolStyle.None;
            } else {
                if ((prop = propDiff.getSVNPropertyValue(SVNProperty.EOL_STYLE)) != null && prop.isString()) {
                    SVNEolStyleInfo propEolStyle = SVNEolStyleInfo.fromValue(prop.getString());
                    style = propEolStyle.eolStyle;
                    eol = propEolStyle.eolStr;
                } else if (!isBinary) {
                } else {
                    eol = null;
                    style = SVNEolStyle.None;
                }
                if (isBinary) {
                    keywords = null;
                }
            }
        }
        if (forceCopy || keywords != null || eol != null || charset != null || special) {
            File detranslated = openUniqueFile(getDb().getWCRootTempDir(targetAbspath), false).path;
            if (style == SVNEolStyle.Native) {
                eol = SVNEolStyleInfo.LF_EOL_STR;
            } else if (style != SVNEolStyle.Fixed && style != SVNEolStyle.None) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_UNKNOWN_EOL);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            SVNTranslator.copyAndTranslate(sourceAbspath, detranslated, charset, eol, keywords, special, false, true);
            return detranslated.getAbsoluteFile();
        }
        return sourceAbspath;
    }

    public static class UniqueFileInfo {

        public File path;
        public OutputStream stream;
    }

    public static UniqueFileInfo openUniqueFile(File dirPath, boolean openStream) throws SVNException {
        UniqueFileInfo info = new UniqueFileInfo();
        if (dirPath == null) {
            dirPath = SVNFileUtil.createFilePath(System.getProperty("java.io.tmpdir"));
        }
        info.path = SVNFileUtil.createUniqueFile(dirPath, "svn", ".tmp", false);
        if (openStream) {
            info.stream = SVNFileUtil.openFileForWriting(info.path);
        }
        return info;
    }

    private File maybeUpdateTargetEols(File oldTargetAbspath, SVNProperties propDiff) throws SVNException {
        SVNPropertyValue prop = propDiff.getSVNPropertyValue(SVNProperty.EOL_STYLE);
        if (prop != null && prop.isString()) {
            byte[] eol = SVNEolStyleInfo.fromValue(prop.getString()).eolStr;
            File tmpNew = openUniqueFile(null, false).path;
            SVNTranslator.copyAndTranslate(oldTargetAbspath, tmpNew, null, eol, null, false, false, true);
            return tmpNew;
        }
        return oldTargetAbspath;
    }

    private MergeInfo mergeTextFile(ISvnMerger customMerger, File leftAbspath, File rightAbspath, File targetAbspath, File wriAbspath, String leftLabel, String rightLabel, String targetLabel, boolean dryRun, SVNDiffOptions options,
            SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, File copyfromText, File detranslatedTargetAbspath, SVNPropertyValue mimeprop, ISVNConflictHandler conflictResolver)
            throws SVNException {
        MergeInfo info = new MergeInfo();
        info.workItems = null;
        String baseName = SVNFileUtil.getFileName(targetAbspath);
        File tempDir = db.getWCRootTempDir(wriAbspath == null ? targetAbspath : wriAbspath);
        File resultTarget = SVNFileUtil.createUniqueFile(tempDir, baseName, ".tmp", false);
        boolean containsConflicts = doTextMerge(customMerger, resultTarget, targetAbspath, detranslatedTargetAbspath, leftAbspath, rightAbspath, targetLabel, leftLabel, rightLabel, options);
        if (containsConflicts && !dryRun) {
            info = maybeResolveConflicts(leftAbspath, rightAbspath, targetAbspath, copyfromText, leftLabel, rightLabel, targetLabel, leftVersion, rightVersion, resultTarget,
                    detranslatedTargetAbspath, mimeprop, options, conflictResolver);
            if (info.mergeOutcome == SVNStatusType.CONFLICTED) {
                PresevePreMergeFileInfo preserveInfo = preservePreMergeFiles(leftAbspath, rightAbspath, targetAbspath, leftLabel, rightLabel, targetLabel, detranslatedTargetAbspath);
                SVNSkel workItem = preserveInfo.workItems;
                File leftCopy = preserveInfo.leftCopy;
                File rightCopy = preserveInfo.rightCopy;
                File targetCopy = preserveInfo.targetCopy;
                info.workItems = wqMerge(info.workItems, workItem);
                workItem = wqBuildSetTextConflictMarkersTmp(targetAbspath, leftCopy, rightCopy, targetCopy);
                info.workItems = wqMerge(info.workItems, workItem);
            }
            if (info.mergeOutcome == SVNStatusType.MERGED) {
                return info;
            }
        } else if (containsConflicts && dryRun) {
            info.mergeOutcome = SVNStatusType.CONFLICTED;
        } else if (copyfromText != null) {
            info.mergeOutcome = SVNStatusType.MERGED;
        } else {
            boolean special = getTranslateInfo(targetAbspath, false, false, false, true).special;
            boolean same = SVNFileUtil.compareFiles(resultTarget, (special ? detranslatedTargetAbspath : targetAbspath), null);
            info.mergeOutcome = same ? SVNStatusType.UNCHANGED : SVNStatusType.MERGED;
        }
        if (info.mergeOutcome != SVNStatusType.UNCHANGED && !dryRun) {
            SVNSkel workItem = wqBuildFileInstall(targetAbspath, resultTarget, false, false);
            info.workItems = wqMerge(info.workItems, workItem);
        }
        return info;
    }

    private boolean doTextMerge(ISvnMerger customMerger, File resultFile, File targetAbsPath, File detranslatedTargetAbspath, File leftAbspath, File rightAbspath, String targetLabel, String leftLabel, String rightLabel, SVNDiffOptions options) throws SVNException {
        ISvnMerger defaultMerger = createDefaultMerger();
        SvnMergeResult mergeResult;
        if (customMerger != null) {
            mergeResult = customMerger.mergeText(defaultMerger, resultFile, targetAbsPath, detranslatedTargetAbspath, leftAbspath, rightAbspath, targetLabel, leftLabel, rightLabel, options);
        } else {
            mergeResult = defaultMerger.mergeText(null, resultFile, targetAbsPath, detranslatedTargetAbspath, leftAbspath, rightAbspath, targetLabel, leftLabel, rightLabel, options);
        }
        if (mergeResult.getMergeOutcome() == SVNStatusType.CONFLICTED) {
            return true;
        }
        return false;
    }

    public static class ConflictMarkersInfo {

        public String rightMarker;
        public String leftMarker;
        public String targetMarker;

    }

    ConflictMarkersInfo initConflictMarkers(String targetLabel, String leftLabel, String rightLabel) {
        ConflictMarkersInfo info = new ConflictMarkersInfo();
        if (targetLabel != null) {
            info.targetMarker = String.format("<<<<<<< %s", targetLabel);
        } else {
            info.targetMarker = "<<<<<<< .working";
        }
        if (leftLabel != null) {
            info.leftMarker = String.format("||||||| %s", leftLabel);
        } else {
            info.leftMarker = "||||||| .old";
        }
        if (rightLabel != null) {
            info.rightMarker = String.format(">>>>>>> %s", rightLabel);
        } else {
            info.rightMarker = ">>>>>>> .new";
        }
        return info;
    }

    public static class PresevePreMergeFileInfo {

        public SVNSkel workItems;
        public File leftCopy;
        public File rightCopy;
        public File targetCopy;

    }

    private PresevePreMergeFileInfo preservePreMergeFiles(File leftAbspath, File rightAbspath, File targetAbspath, String leftLabel, String rightLabel, String targetLabel,
            File detranslatedTargetAbspath) throws SVNException {
        PresevePreMergeFileInfo info = new PresevePreMergeFileInfo();
        info.workItems = null;
        File dirAbspath = SVNFileUtil.getFileDir(targetAbspath);
        String targetName = SVNFileUtil.getFileName(targetAbspath);
        File tempDir = db.getWCRootTempDir(targetAbspath);
        info.leftCopy = SVNFileUtil.createUniqueFile(dirAbspath, targetName, leftLabel, false);
        info.rightCopy = SVNFileUtil.createUniqueFile(dirAbspath, targetName, rightLabel, false);
        info.targetCopy = SVNFileUtil.createUniqueFile(dirAbspath, targetName, targetLabel, false);
        File tmpLeft, tmpRight, detranslatedTargetCopy;
        if (!SVNPathUtil.isAncestor(dirAbspath.getPath(), leftAbspath.getPath())) {
            tmpLeft = openUniqueFile(tempDir, false).path;
            SVNFileUtil.copyFile(leftAbspath, tmpLeft, true);
        } else {
            tmpLeft = leftAbspath;
        }
        if (!SVNPathUtil.isAncestor(dirAbspath.getPath(), rightAbspath.getPath())) {
            tmpRight = openUniqueFile(tempDir, false).path;
            SVNFileUtil.copyFile(rightAbspath, tmpRight, true);
        } else {
            tmpRight = rightAbspath;
        }
        SVNSkel workItem;
        workItem = wqBuildFileCopyTranslated(targetAbspath, tmpLeft, info.leftCopy);
        info.workItems = wqMerge(info.workItems, workItem);
        workItem = wqBuildFileCopyTranslated(targetAbspath, tmpRight, info.rightCopy);
        info.workItems = wqMerge(info.workItems, workItem);
        try {
            detranslatedTargetCopy = getTranslatedFile(targetAbspath, targetAbspath, true, false, false, false, false);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                detranslatedTargetCopy = detranslatedTargetAbspath;
            } else {
                throw e;
            }
        }
        workItem = wqBuildFileCopyTranslated(targetAbspath, detranslatedTargetCopy, info.targetCopy);
        info.workItems = wqMerge(info.workItems, workItem);
        return info;
    }

    private MergeInfo maybeResolveConflicts(File leftAbspath, File rightAbspath, File targetAbspath, File copyfromText, String leftLabel, String rightLabel, String targetLabel,
            SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, File resultTarget, File detranslatedTarget, SVNPropertyValue mimeprop, SVNDiffOptions options,
            ISVNConflictHandler conflictResolver) throws SVNException {
        MergeInfo info = new MergeInfo();
        info.workItems = null;
        SVNConflictResult result;
        File dirAbspath = SVNFileUtil.getFileDir(targetAbspath);
        if (conflictResolver == null) {
            result = new SVNConflictResult(SVNConflictChoice.POSTPONE, null);
        } else {
            SVNConflictDescription cdesc = setupTextConflictDesc(leftAbspath, rightAbspath, targetAbspath, leftVersion, rightVersion, resultTarget, detranslatedTarget, mimeprop, false);
            result = conflictResolver.handleConflict(cdesc);
            if (result == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "Conflict callback violated API: returned no results");
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            if (result.isIsSaveMerged()) {
                info.workItems = saveMergeResult(targetAbspath, result.getMergedFile() != null ? result.getMergedFile() : resultTarget);
            }
        }
        MergeInfo evalInfo = evalConflictResolverResult(result.getConflictChoice(), dirAbspath, leftAbspath, rightAbspath, targetAbspath, copyfromText,
                result.getMergedFile() != null ? result.getMergedFile() : resultTarget, detranslatedTarget, options);
        info.mergeOutcome = evalInfo.mergeOutcome;
        SVNSkel workItem = evalInfo.workItems;
        info.workItems = wqMerge(info.workItems, workItem);
        if (result.getConflictChoice() != SVNConflictChoice.POSTPONE) {
            return info;
        }
        info.mergeOutcome = SVNStatusType.CONFLICTED;
        return info;
    }

    private SVNConflictDescription setupTextConflictDesc(File leftAbspath, File rightAbspath, File targetAbspath, SVNConflictVersion leftVersion, SVNConflictVersion rightVersion, File resultTarget,
            File detranslatedTarget, SVNPropertyValue mimeprop, boolean isBinary) {
        SVNWCConflictDescription17 cdesc = SVNWCConflictDescription17.createText(targetAbspath);
        cdesc.setBinary(false);
        cdesc.setMimeType((mimeprop != null && mimeprop.isString()) ? mimeprop.getString() : null);
        cdesc.setBaseFile(leftAbspath);
        cdesc.setTheirFile(rightAbspath);
        cdesc.setMyFile(detranslatedTarget);
        cdesc.setMergedFile(resultTarget);
        cdesc.setSrcLeftVersion(leftVersion);
        cdesc.setSrcRightVersion(rightVersion);
        return cdesc.toConflictDescription();
    }

    private SVNSkel saveMergeResult(File versionedAbspath, File source) throws SVNException {
        File dirAbspath = SVNFileUtil.getFileDir(versionedAbspath);
        String filename = SVNFileUtil.getFileName(versionedAbspath);
        File editedCopyAbspath = SVNFileUtil.createUniqueFile(dirAbspath, filename, ".edited", false);
        return wqBuildFileCopyTranslated(versionedAbspath, source, editedCopyAbspath);
    }

    private MergeInfo evalConflictResolverResult(SVNConflictChoice choice, File wriAbspath, File leftAbspath, File rightAbspath, File targetAbspath, File copyfromText, File mergedFile,
            File detranslatedTarget, SVNDiffOptions options) throws SVNException {
        File installFrom = null;
        boolean removeSource = false;
        MergeInfo info = new MergeInfo();
        info.workItems = null;
        if (choice == SVNConflictChoice.BASE) {
            installFrom = leftAbspath;
            info.mergeOutcome = SVNStatusType.MERGED;
        } else if (choice == SVNConflictChoice.THEIRS_FULL) {
            installFrom = rightAbspath;
            info.mergeOutcome = SVNStatusType.MERGED;
        } else if (choice == SVNConflictChoice.MINE_FULL) {
            info.mergeOutcome = SVNStatusType.MERGED;
            return info;
        } else if (choice == SVNConflictChoice.THEIRS_CONFLICT || choice == SVNConflictChoice.MINE_CONFLICT) {
            SVNDiffConflictChoiceStyle style = choice == SVNConflictChoice.THEIRS_CONFLICT ? SVNDiffConflictChoiceStyle.CHOOSE_LATEST : SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED;
            File tempDir = db.getWCRootTempDir(wriAbspath);
            FSMergerBySequence merger = new FSMergerBySequence(null, null, null);
            RandomAccessFile localIS = null;
            RandomAccessFile latestIS = null;
            RandomAccessFile baseIS = null;
            UniqueFileInfo uniqFile = openUniqueFile(tempDir, true);
            File chosenPath = uniqFile.path;
            OutputStream chosenStream = uniqFile.stream;
            try {
                localIS = new RandomAccessFile(detranslatedTarget, "r");
                latestIS = new RandomAccessFile(rightAbspath, "r");
                baseIS = new RandomAccessFile(leftAbspath, "r");
                QSequenceLineRAData baseData = new QSequenceLineRAFileData(baseIS);
                QSequenceLineRAData localData = new QSequenceLineRAFileData(localIS);
                QSequenceLineRAData latestData = new QSequenceLineRAFileData(latestIS);
                merger.merge(baseData, localData, latestData, options, chosenStream, style);
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
                SVNErrorManager.error(err, e, SVNLogType.WC);
            } finally {
                SVNFileUtil.closeFile(chosenStream);
                SVNFileUtil.closeFile(localIS);
                SVNFileUtil.closeFile(baseIS);
                SVNFileUtil.closeFile(latestIS);
            }
            installFrom = chosenPath;
            removeSource = true;
            info.mergeOutcome = SVNStatusType.MERGED;
        } else if (choice == SVNConflictChoice.MERGED) {
            installFrom = mergedFile;
            info.mergeOutcome = SVNStatusType.MERGED;
        } else {
            if (copyfromText != null) {
                installFrom = copyfromText;
            } else {
                return info;
            }
        }
        assert (installFrom != null);
        {
            SVNSkel workItem = wqBuildFileInstall(targetAbspath, installFrom, false, false);
            info.workItems = wqMerge(info.workItems, workItem);
            if (removeSource) {
                workItem = wqBuildFileRemove(installFrom);
                info.workItems = wqMerge(info.workItems, workItem);
            }
        }
        return info;
    }

    private MergeInfo mergeBinaryFile(File leftAbspath, File rightAbspath, File targetAbspath, String leftLabel, String rightLabel, String targetLabel, SVNConflictVersion leftVersion,
            SVNConflictVersion rightVersion, File detranslatedTargetAbspath, SVNPropertyValue mimeprop, ISVNConflictHandler conflictResolver) throws SVNException {
        assert (SVNFileUtil.isAbsolute(targetAbspath));
        MergeInfo info = new MergeInfo();
        info.workItems = null;
        File leftCopy, rightCopy;
        File conflictWrk;
        SVNSkel workItem;
        File mergeDirpath = SVNFileUtil.getFileDir(targetAbspath);
        String mergeFilename = SVNFileUtil.getFileName(targetAbspath);
        if (conflictResolver != null) {
            File installFrom = null;
            SVNConflictDescription cdesc = setupTextConflictDesc(leftAbspath, rightAbspath, targetAbspath, leftVersion, rightVersion, null, detranslatedTargetAbspath, mimeprop, true);
            SVNConflictResult result = conflictResolver.handleConflict(cdesc);
            if (result == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "Conflict callback violated API: returned no results");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (result.getConflictChoice() == SVNConflictChoice.BASE) {
                installFrom = leftAbspath;
                info.mergeOutcome = SVNStatusType.MERGED;
            } else if (result.getConflictChoice() == SVNConflictChoice.THEIRS_FULL) {
                installFrom = rightAbspath;
                info.mergeOutcome = SVNStatusType.MERGED;
            } else if (result.getConflictChoice() == SVNConflictChoice.MINE_FULL) {
                info.mergeOutcome = SVNStatusType.MERGED;
                return info;
            } else if (result.getConflictChoice() == SVNConflictChoice.MERGED) {
                if (result.getMergedFile() == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "Conflict callback violated API: returned no results");
                    SVNErrorManager.error(err, SVNLogType.WC);
                } else {
                    installFrom = result.getMergedFile();
                    info.mergeOutcome = SVNStatusType.MERGED;
                }
            } else {
            }
            if (installFrom != null) {
                info.workItems = wqBuildFileInstall(targetAbspath, installFrom, false, false);
                return info;
            }
        }
        leftCopy = SVNFileUtil.createUniqueFile(mergeDirpath, mergeFilename, leftLabel, false);
        rightCopy = SVNFileUtil.createUniqueFile(mergeDirpath, mergeFilename, rightLabel, false);
        SVNFileUtil.copyFile(leftAbspath, leftCopy, true);
        SVNFileUtil.copyFile(rightAbspath, rightCopy, true);
        if (!targetAbspath.equals(detranslatedTargetAbspath)) {
            File mineCopy = SVNFileUtil.createUniqueFile(mergeDirpath, mergeFilename, targetLabel, true);
            info.workItems = wqBuildFileMove(detranslatedTargetAbspath, mineCopy);
            conflictWrk = mineCopy;
        } else {
            conflictWrk = null;
        }
        workItem = wqBuildSetTextConflictMarkersTmp(targetAbspath, leftCopy, rightCopy, conflictWrk);
        info.workItems = wqMerge(info.workItems, workItem);
        info.mergeOutcome = SVNStatusType.CONFLICTED;
        return info;
    }

    public SVNSkel wqBuildFileMove(File srcAbspath, File dstAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(srcAbspath));
        assert (SVNFileUtil.isAbsolute(dstAbspath));
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(srcAbspath));
        if (kind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "''{0}'' not found", srcAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
//        getDb().getWCRoot(dirAbspath)
        SVNSkel workItem = SVNSkel.createEmptyList();
        workItem.prependPath(getRelativePath(dstAbspath));
        workItem.prependPath(getRelativePath(srcAbspath));
        workItem.prependString(WorkQueueOperation.FILE_MOVE.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }
    
    public SVNSkel wqBuildFileMove(File anchorPath, File srcAbspath, File dstAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(srcAbspath));
        assert (SVNFileUtil.isAbsolute(dstAbspath));
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(srcAbspath));
        if (kind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "''{0}'' not found", srcAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNSkel workItem = SVNSkel.createEmptyList();
        File anchorRelativePath = getRelativePath(anchorPath);
        workItem.prependPath(SVNFileUtil.createFilePath(anchorRelativePath, SVNWCUtils.skipAncestor(anchorPath, dstAbspath)));
        workItem.prependPath(SVNFileUtil.createFilePath(anchorRelativePath, SVNWCUtils.skipAncestor(anchorPath, srcAbspath)));
        workItem.prependString(WorkQueueOperation.FILE_MOVE.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildFileCopyTranslated(File localAbspath, File srcAbspath, File dstAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        assert (SVNFileUtil.isAbsolute(srcAbspath));
        assert (SVNFileUtil.isAbsolute(dstAbspath));
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(srcAbspath));
        if (kind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "''{0}'' not found", srcAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNSkel workItem = SVNSkel.createEmptyList();
        workItem.prependPath(getRelativePath(dstAbspath));
        workItem.prependPath(getRelativePath(srcAbspath));
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.FILE_COPY_TRANSLATED.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildSetTextConflictMarkersTmp(File localAbspath, File old, File neo, File wrk) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        SVNSkel workItem = SVNSkel.createEmptyList();
        File wcRoot = getDb().getWCRoot(localAbspath);
        String oldPath = old != null ? SVNWCUtils.getPathAsChild(wcRoot, old) : null;
        String newPath = neo != null ? SVNWCUtils.getPathAsChild(wcRoot, neo) : null;
        String wrkPath = wrk != null ? SVNWCUtils.getPathAsChild(wcRoot, wrk) : null;
        
        workItem.prependString(wrkPath != null ? wrkPath : "");
        workItem.prependString(newPath != null ? newPath : "");
        workItem.prependString(oldPath != null ? oldPath : "");
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.TMP_SET_TEXT_CONFLICT_MARKERS.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildBaseRemove(File localAbspath, boolean keepNotPresent) throws SVNException {
        SVNSkel workItem = SVNSkel.createEmptyList();
        workItem.prependString(keepNotPresent ? "1" : "0");
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.BASE_REMOVE.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }
    
    public SVNSkel wqBuildBaseRemove(File localAbspath, long notPresentRevision, SVNWCDbKind notPresentKind) throws SVNException {
        SVNSkel workItem = SVNSkel.createEmptyList();
        workItem.prependString(Integer.toString(notPresentKind.ordinal()));
        workItem.prependString(Long.toString(notPresentRevision));
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.BASE_REMOVE.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildRecordFileinfo(File localAbspath, SVNDate setTime) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        SVNSkel workItem = SVNSkel.createEmptyList();
        if (setTime != null) {
            workItem.prependString(String.format("%d", setTime.getTimeInMicros()));
        }
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.RECORD_FILEINFO.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildFileInstall(File localAbspath, File sourceAbspath, boolean useCommitTimes, boolean recordFileinfo) throws SVNException {
        SVNSkel workItem = SVNSkel.createEmptyList();
        if (sourceAbspath != null) {
            workItem.prependPath(getRelativePath(sourceAbspath));
        }
        workItem.prependString(recordFileinfo ? "1" : "0");
        workItem.prependString(useCommitTimes ? "1" : "0");
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.FILE_INSTALL.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildSyncFileFlags(File localAbspath) throws SVNException {
        SVNSkel workItem = SVNSkel.createEmptyList();
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.SYNC_FILE_FLAGS.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildFileRemove(File localAbspath) throws SVNException {
        SVNSkel workItem = SVNSkel.createEmptyList();
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.FILE_REMOVE.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildPrejInstall(File localAbspath, SVNSkel conflictSkel) throws SVNException {
        assert (conflictSkel != null);
        SVNSkel workItem = SVNSkel.createEmptyList();
        workItem.addChild(conflictSkel);
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.PREJ_INSTALL.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }

    public SVNSkel wqBuildSetPropertyConflictMarkerTemp(File localAbspath, File prejFile) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        SVNSkel workItem = SVNSkel.createEmptyList();
        File wcRoot = getDb().getWCRoot(localAbspath);
        String prejPath = null;
        if (prejFile != null) {
            prejPath = SVNWCUtils.getPathAsChild(wcRoot, prejFile);
        }
        workItem.prependString(prejPath != null ? prejPath : "");
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.TMP_SET_PROPERTY_CONFLICT_MARKER.getOpName());
        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }
    
    public SVNSkel wqBuildPostUpgrade() throws SVNException {
    	SVNSkel result = SVNSkel.createEmptyList();
    	SVNSkel workItem = SVNSkel.createEmptyList();
    	workItem.prependString(WorkQueueOperation.POSTUPGRADE.getOpName());
    	result.addChild(workItem);
    	return result;
    }
    
    public SVNSkel wqMerge(SVNSkel workItem1, SVNSkel workItem2) throws SVNException {
        if (workItem1 == null) {
            return workItem2;
        }
        if (workItem2 == null) {
            return workItem1;
        }
        if (workItem1.isAtom()) {
            if (workItem2.isAtom()) {
                SVNSkel result = SVNSkel.createEmptyList();
                result.addChild(workItem2);
                result.addChild(workItem1);
                return result;
            }
            workItem2.addChild(workItem1);
            return workItem2;
        }
        if (workItem2.isAtom()) {
            workItem1.appendChild(workItem2);
            return workItem1;
        }
        int listSize = workItem2.getListSize();
        for (int i = 0; i < listSize; i++) {
            workItem1.appendChild(workItem2.getChild(i));
        }
        return workItem1;
    }

    public void wqRun(File dirAbspath) throws SVNException {
        // SVNDebugLog.getDefaultLog().log(SVNLogType.WC,
        // String.format("work queue run: wcroot='%s'", wcRootAbspath),
        // Level.INFO);
        File wcRootAbspath = getDb().getWCRoot(dirAbspath);
        while (true) {
            checkCancelled();
            WCDbWorkQueueInfo fetchWorkQueue = db.fetchWorkQueue(dirAbspath);
            if (fetchWorkQueue.workItem == null) {
                break;
            }
            dispatchWorkItem(wcRootAbspath, fetchWorkQueue.workItem);
            db.completedWorkQueue(dirAbspath, fetchWorkQueue.id);
        }
    }

    private void dispatchWorkItem(File wcRootAbspath, SVNSkel workItem) throws SVNException {
        if (!workItem.isAtom()) {
            for (WorkQueueOperation scan : WorkQueueOperation.values()) {
                if (scan.getOpName().equals(workItem.getChild(0).getValue())) {
                    // SVNDebugLog.getDefaultLog().log(SVNLogType.WC,
                    // String.format("work queue dispatch: operation='%s'",
                    // scan.getOpName()), Level.INFO);
                    scan.getOperation().runOperation(this, wcRootAbspath, workItem);
                    return;
                }
            }
        }
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_BAD_ADM_LOG, "Unrecognized work item in the queue associated with ''{0}''", wcRootAbspath);
        SVNErrorManager.error(err, SVNLogType.WC);
    }

    public static class RunBaseRemove implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            long value  = -1;
            try {
                value = Long.parseLong(workItem.getChild(2).getValue());
            } catch (NumberFormatException nfe) {
                value = -1;
            }
            File reposRelPath = null;
            SVNURL reposRootUrl = null;
            String reposUuid = null;
            SVNWCDbKind kind = null;
            long revision = -1;
            if (workItem.getList().size() >= 4) {
                revision = value;
                kind = SVNWCDbKind.values()[Integer.parseInt(workItem.getChild(3).getValue())];
                if (revision >= 0) {
                    File dirAbsPath = SVNFileUtil.getParentFile(localAbspath);
                    WCDbRepositoryInfo info = ctx.getDb().scanBaseRepository(dirAbsPath, RepositoryInfoField.relPath, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
                    reposRelPath = SVNFileUtil.createFilePath(info.relPath, SVNFileUtil.getFileName(localAbspath));
                    reposRootUrl = info.rootUrl;
                    reposUuid = info.uuid;
                }
            } else {
                boolean keepNotPresent = value == 1;
                if (keepNotPresent) {
                    WCDbBaseInfo info = ctx.getDb().getBaseInfo(localAbspath, BaseInfoField.kind, BaseInfoField.revision, BaseInfoField.reposRelPath, BaseInfoField.reposRootUrl,
                            BaseInfoField.reposUuid); 
                    reposRelPath = info.reposRelPath;
                    reposRootUrl = info.reposRootUrl;
                    reposUuid = info.reposUuid;
                    revision = info.revision;
                    kind = info.kind;
                }
            }
            ctx.removeBaseNode(localAbspath);
            if (revision >= 0) {
                ctx.getDb().addBaseNotPresentNode(localAbspath, reposRelPath, reposRootUrl, reposUuid, revision, kind, null, null);
            }
        }
    }

    public static class RunFileInstall implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            boolean useCommitTimes = "1".equals(workItem.getChild(2).getValue());
            boolean recordFileInfo = "1".equals(workItem.getChild(3).getValue());
            File srcPath;
            if (workItem.getListSize() <= 4) {
                srcPath = ctx.getPristineContents(localAbspath, false, true).path;
            } else {
                srcPath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(4).getValue());
            }
            TranslateInfo tinfo = ctx.getTranslateInfo(localAbspath, true, true, true, true);
            SVNTranslator.translate(srcPath, localAbspath, tinfo.charset, tinfo.eolStyleInfo.eolStr, tinfo.keywords, tinfo.special, true);
            if (tinfo.special) {
                return;
            }
            
            final Structure<InstallInfo> installInfo = SvnWcDbReader.readNodeInstallInfo((SVNWCDb) ctx.getDb(), 
                    localAbspath, InstallInfo.changedDate, InstallInfo.pristineProps);            
            final SVNProperties props = installInfo.get(InstallInfo.pristineProps);
            if (props != null &&
                    (props.containsName(SVNProperty.EXECUTABLE) ||
                     props.containsName(SVNProperty.NEEDS_LOCK))) {
                ctx.syncFileFlags(localAbspath);
            }
            if (useCommitTimes) {
                final SVNDate changedDate = installInfo.get(InstallInfo.changedDate);
                if (changedDate != null) {
                    SVNFileUtil.setLastModified(localAbspath, changedDate.getTime());
                }
            }
            if (recordFileInfo) {
                ctx.getAndRecordFileInfo(localAbspath, false);
            }
        }
    }

    public static class RunFileCommit implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            File tmpFile = ctx.getTranslatedFile(localAbspath, localAbspath, false, false, false, false, true);
            TranslateInfo info = ctx.getTranslateInfo(localAbspath, false, false, false, true);
            boolean sameContents = false;
            boolean overwroteWorkFile = false;
            if ((info == null || !info.special) && !tmpFile.equals(localAbspath)) {
                sameContents = ctx.isSameContents(tmpFile, localAbspath);
            } else {
                sameContents = true;
            }
            if (!sameContents) {
                SVNFileUtil.rename(tmpFile, localAbspath);
                overwroteWorkFile = true;
            } else if (!tmpFile.equals(localAbspath)) {
                SVNFileUtil.deleteFile(tmpFile);
            }
            ctx.syncFileFlags(localAbspath);
            if (overwroteWorkFile) {
                ctx.getAndRecordFileInfo(localAbspath, false);
            } else {
                // TODO need to fix size and tsmtp.
                ctx.isTextModified(localAbspath, false);
            }
        }
    }

    public static class RunFileRemove implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            SVNFileUtil.deleteFile(localAbspath);
        }
    }

    public static class RunFileMove implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File srcRelPath = new File(workItem.getChild(1).getValue());
            File dstRelPath = new File(workItem.getChild(2).getValue());
            File srcAbspath = !SVNFileUtil.isAbsolute(srcRelPath) ? SVNFileUtil.createFilePath(wcRootAbspath, srcRelPath) : srcRelPath;
            File dstAbspath = !SVNFileUtil.isAbsolute(dstRelPath) ? SVNFileUtil.createFilePath(wcRootAbspath, dstRelPath) : dstRelPath;

            SVNFileType srcType = SVNFileType.getType(srcAbspath);
            if (srcType == SVNFileType.DIRECTORY) {
                SVNFileUtil.moveDir(srcAbspath, dstAbspath);
            } else if (srcType == SVNFileType.FILE) {
                SVNFileUtil.moveFile(srcAbspath, dstAbspath);
            } else if (srcType == SVNFileType.SYMLINK) {
                SVNFileUtil.createSymlink(dstAbspath, SVNFileUtil.getSymlinkName(srcAbspath));
            }
        }
    }

    public static class RunFileTranslate implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            File srcAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(2).getValue());
            File dstAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(3).getValue());
            TranslateInfo tinf = ctx.getTranslateInfo(localAbspath, true, true, true, true);
            SVNTranslator.copyAndTranslate(srcAbspath, dstAbspath, tinf.charset, tinf.eolStyleInfo.eolStr, tinf.keywords, tinf.special, true, true);
        }
    }

    public static class RunSyncFileFlags implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            ctx.syncFileFlags(localAbspath);
        }
    }

    public static class RunPrejInstall implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            SVNSkel conflictSkel = null;
            if (workItem.getListSize() > 2) {
                conflictSkel = workItem.getChild(2);
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            File tmpPrejfileAbspath = ctx.createPrejFile(localAbspath, conflictSkel);
            File prejfileAbspath = ctx.getPrejfileAbspath(localAbspath);
            assert (prejfileAbspath != null);
            SVNFileUtil.rename(tmpPrejfileAbspath, prejfileAbspath);
        }
    }

    public static class RunRecordFileInfo implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws NumberFormatException, SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            SVNDate setTime = null;
            if (workItem.getListSize() > 2) {
                long val = Long.parseLong(workItem.getChild(2).getValue());
                setTime = SVNWCUtils.readDate(val);
            }
            if (setTime != null) {
                if (SVNFileType.getType(localAbspath).isFile()) {
                    SVNFileUtil.setLastModified(localAbspath, setTime.getTime());
                }
            }
            ctx.getAndRecordFileInfo(localAbspath, true);
        }
    }

    public static class RunSetTextConflictMarkersTemp implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            int listSize = workItem.getListSize();

            File oldBasename;
            if (listSize > 2) {
                String value = workItem.getChild(2).getValue();
                oldBasename = (value == null || value.length() == 0) ? null : SVNFileUtil.createFilePath(value);
            }
            else {
                oldBasename = null;
            }

            File newBasename;
            if (listSize > 3) {
                String value = workItem.getChild(3).getValue();
                newBasename = (value == null || value.length() == 0) ? null : SVNFileUtil.createFilePath(value);
            }
            else {
                newBasename = null;
            }

            File wrkBasename;
            if (listSize > 4) {
                String value = workItem.getChild(4).getValue();
                wrkBasename = (value == null || value.length() == 0) ? null : SVNFileUtil.createFilePath(value);
            }
            else {
                wrkBasename = null;
            }
            ctx.getDb().opSetTextConflictMarkerFilesTemp(localAbspath, oldBasename, newBasename, wrkBasename);
        }
    }

    public static class RunSetPropertyConflictMarkerTemp implements RunWorkQueueOperation {

        public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
            File localAbspath = SVNFileUtil.createFilePath(wcRootAbspath, workItem.getChild(1).getValue());
            String prejBasename = workItem.getListSize() > 2 ? workItem.getChild(2).getValue() : null;
            ctx.getDb().opSetPropertyConflictMarkerFileTemp(localAbspath, prejBasename);
        }
    }

    public static class RunPostUpgrade implements RunWorkQueueOperation {

    	public void runOperation(SVNWCContext ctx, File wcRootAbspath, SVNSkel workItem) throws SVNException {
    		
    		try {
    			SvnOldUpgrade.wipePostUpgrade(ctx, wcRootAbspath, false);
    		} catch (SVNException ex) {
    			/* No entry, this can happen when the wq item is rerun. */
    			if (ex.getErrorMessage().getErrorCode() != SVNErrorCode.ENTRY_NOT_FOUND)
    				throw ex;
    		}
    		
    		File adminPath = SVNFileUtil.createFilePath(wcRootAbspath, SVNFileUtil.getAdminDirectoryName());
    		File entriesPath = SVNFileUtil.createFilePath(adminPath, WC_ADM_ENTRIES);
    		File formatPath = SVNFileUtil.createFilePath(adminPath, WC_ADM_FORMAT);
    		
    		/* Write the 'format' and 'entries' files.

   	     	### The order may matter for some sufficiently old clients.. but
   	     	### this code only runs during upgrade after the files had been
   	     	### removed earlier during the upgrade. */
    		
    		File tempFile = SVNFileUtil.createUniqueFile(adminPath, "svn-XXXXXX", ".tmp", false);
    		SVNFileUtil.writeToFile(tempFile, WC_NON_ENTRIES_STRING, "US-ASCII");
    		SVNFileUtil.rename(tempFile, formatPath);
            
    		tempFile = SVNFileUtil.createUniqueFile(adminPath, "svn-XXXXXX", ".tmp", false);
    		SVNFileUtil.writeToFile(tempFile, WC_NON_ENTRIES_STRING, "US-ASCII");
    		SVNFileUtil.rename(tempFile, entriesPath);
        }
    }

    public void removeBaseNode(File localAbspath) throws SVNException {
        checkCancelled();
        WCDbInfo readInfo;
        try { 
            readInfo = db.readInfo(localAbspath, InfoField.status, InfoField.kind, InfoField.haveBase, InfoField.haveWork);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                return;
            }
            throw e;
        }
        if (!readInfo.haveBase) {
            return;
        }
        SVNWCDbStatus wrkStatus = readInfo.status;
        SVNWCDbKind wrkKind = readInfo.kind;
        boolean haveBase = readInfo.haveBase;
        assert (haveBase);
        SVNWCDbStatus baseStatus;
        SVNWCDbKind baseKind;
        if (wrkStatus == SVNWCDbStatus.Normal || wrkStatus == SVNWCDbStatus.NotPresent || wrkStatus == SVNWCDbStatus.ServerExcluded) {
            baseStatus = wrkStatus;
            baseKind = wrkKind;
        } else {
            WCDbBaseInfo baseInfo = db.getBaseInfo(localAbspath, BaseInfoField.status, BaseInfoField.kind);
            baseStatus = baseInfo.status;
            baseKind = baseInfo.kind;
        }
        if (baseKind == SVNWCDbKind.Dir && (baseStatus == SVNWCDbStatus.Normal || baseStatus == SVNWCDbStatus.Incomplete)) {
            Set<String> children = db.getBaseChildren(localAbspath);

            for (String childName : children) {
                File childAbspath = SVNFileUtil.createFilePath(localAbspath, childName);
                removeBaseNode(childAbspath);
            }
        }
        if (baseStatus == SVNWCDbStatus.Normal && wrkStatus != SVNWCDbStatus.Added && wrkStatus != SVNWCDbStatus.Excluded) {
            if (wrkStatus != SVNWCDbStatus.Deleted && (baseKind == SVNWCDbKind.File || baseKind == SVNWCDbKind.Symlink)) {
                SVNFileUtil.deleteFile(localAbspath);
            } else if (baseKind == SVNWCDbKind.Dir && wrkStatus != SVNWCDbStatus.Deleted) {
                SVNFileUtil.deleteFile(localAbspath);
            }
        }
        db.removeBase(localAbspath);
    }

    public void getAndRecordFileInfo(File localAbspath, boolean ignoreError) throws SVNException {
        if (localAbspath.exists()) {
            SVNDate lastModified = new SVNDate(SVNFileUtil.getFileLastModified(localAbspath), 0);
            long length = SVNFileUtil.getFileLength(localAbspath);
            db.globalRecordFileinfo(localAbspath, length, lastModified);
        }
    }

    public void syncFileFlags(File localAbspath) throws SVNException {
        SVNFileUtil.setReadonly(localAbspath, false);
        SVNFileUtil.setExecutable(localAbspath, false);
        maybeSetReadOnly(localAbspath);
        maybeSetExecutable(localAbspath);
    }

    private boolean maybeSetReadOnly(File localAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        boolean didSet = false;
        SVNWCDbStatus status = null;
        ISVNWCDb.SVNWCDbLock lock = null;
        try {
            WCDbInfo readInfo = db.readInfo(localAbspath, InfoField.status, InfoField.lock);
            status = readInfo.status;
            lock = readInfo.lock;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
        }
        if (lock != null) {
            return didSet;
        }
        if (status == SVNWCDbStatus.Added) {
            return didSet;
        }
        String needsLock = getProperty(localAbspath, SVNProperty.NEEDS_LOCK);
        SVNProperties pristineProperties = getPristineProps(localAbspath);
        if (needsLock != null && (pristineProperties != null && pristineProperties.getStringValue(SVNProperty.NEEDS_LOCK) != null)) {
            SVNFileUtil.setReadonly(localAbspath, true);
            didSet = true;
        }
        return didSet;
    }

    private boolean maybeSetExecutable(File localAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        boolean didSet = false;
        String propval = getProperty(localAbspath, SVNProperty.EXECUTABLE);
        if (propval != null) {
            SVNFileUtil.setExecutable(localAbspath, true);
            didSet = true;
        }
        return didSet;
    }

    public File createPrejFile(File localAbspath, SVNSkel conflictSkel) throws SVNException {
        File tempDirAbspath = db.getWCRootTempDir(localAbspath);
        UniqueFileInfo openUniqueFile = openUniqueFile(tempDirAbspath, true);
        OutputStream stream = openUniqueFile.stream;
        File tempAbspath = openUniqueFile.path;
        try {
            int listSize = conflictSkel.getListSize();
            for (int i = 0; i < listSize; i++) {
                SVNSkel child = conflictSkel.getChild(i);
                appendPropConflict(stream, child);
            }
        } finally {
            SVNFileUtil.closeFile(stream);
        }
        return tempAbspath;
    }

    private void appendPropConflict(OutputStream stream, SVNSkel propSkel) throws SVNException {
        String message = messageFromSkel(propSkel);
        try {
            stream.write(message.getBytes());
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        }
    }

    private String messageFromSkel(SVNSkel skel) throws SVNException {
        String propname = skel.getChild(1).getValue();
        
        SVNPropertyValue original = maybePropValue(propname, skel.getChild(2));
        SVNPropertyValue mine = maybePropValue(propname, skel.getChild(3));
        SVNPropertyValue incoming = maybePropValue(propname, skel.getChild(4));
        SVNPropertyValue incomingBase = maybePropValue(propname, skel.getChild(5));
        
        String conflictMessage = generateConflictMessage(propname, original, mine, incoming, incomingBase);
        
        if (mine == null) {
            mine = SVNPropertyValue.create("");
        }
        if (incoming == null) {
            incoming = SVNPropertyValue.create("");
        }
        if (original == null) {
            if (incomingBase != null) {
                original = incomingBase;
            } else {
                original = SVNPropertyValue.create("");
            }
        } else if (incomingBase != null && mine.equals(original)) {
            original =  incomingBase;
        }
        
        byte[] originalBytes = SVNPropertyValue.getPropertyAsBytes(original);
        byte[] mineBytes = SVNPropertyValue.getPropertyAsBytes(mine);
        byte[] incomingBytes = SVNPropertyValue.getPropertyAsBytes(incoming);
        boolean mineIsBinary = false;
        try {
            mineIsBinary = mineBytes != null && SVNFileUtil.detectMimeType(new ByteArrayInputStream(mineBytes)) != null;
        } catch (IOException e) {
        }  
        boolean incomingIsBinary = false;
        try {
            incomingIsBinary = incomingBytes != null && SVNFileUtil.detectMimeType(new ByteArrayInputStream(incomingBytes)) != null;
        } catch (IOException e) {
        }
        boolean originalIsBinary = false;
        try {
            originalIsBinary = originalBytes != null && SVNFileUtil.detectMimeType(new ByteArrayInputStream(originalBytes)) != null;
        } catch (IOException e) {
        }

        if (!(originalIsBinary || mineIsBinary || incomingIsBinary)) {
            ConflictMarkersInfo markersInfo = initConflictMarkers("(local property value)", "", "(incoming property value)");
            String targetMarker = markersInfo.targetMarker;
            String leftMarker = markersInfo.leftMarker;
            String rightMarker = markersInfo.rightMarker;
            FSMergerBySequence merger = new FSMergerBySequence(
                    targetMarker.getBytes(),
                    CONFLICT_SEPARATOR,
                    rightMarker.getBytes(),
                    leftMarker.getBytes());
            int mergeResult = 0;
            RandomAccessFile localIS = null;
            RandomAccessFile latestIS = null;
            RandomAccessFile baseIS = null;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try {
                SVNDiffOptions diffOptions = new SVNDiffOptions();
                diffOptions.setIgnoreAllWhitespace(false);
                diffOptions.setIgnoreAmountOfWhitespace(false);
                diffOptions.setIgnoreEOLStyle(false);

                mergeResult = merger.merge(
                        new QSequenceLineRAByteData(originalBytes),
                        new QSequenceLineRAByteData(mineBytes),
                        new QSequenceLineRAByteData(incomingBytes),
                        diffOptions, result, SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST);
                result.flush();
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
                SVNErrorManager.error(err, e, SVNLogType.WC);
            } finally {
                SVNFileUtil.closeFile(result);
                SVNFileUtil.closeFile(localIS);
                SVNFileUtil.closeFile(baseIS);
                SVNFileUtil.closeFile(latestIS);
            }
            if (mergeResult == FSMergerBySequence.CONFLICTED) {
                conflictMessage += result.toString();
                return conflictMessage;
            }
        }

        if (mineBytes != null && mineBytes.length > 0) {
            conflictMessage += "Local property value:\n";
            if (mineIsBinary) {
                conflictMessage += "Cannot display: property value is binary data\n";
            } else {
                conflictMessage += SVNPropertyValue.getPropertyAsString(mine);
            }
            conflictMessage += "\n";
        }
        if (incomingBytes != null && incomingBytes.length > 0) {
            conflictMessage += "Incoming property value:\n";
            if (incomingIsBinary) {
                conflictMessage += "Cannot display: property value is binary data\n";
            } else {
                conflictMessage += SVNPropertyValue.getPropertyAsString(incoming);
            }
            conflictMessage += "\n";
        }
        
        return conflictMessage;
    }

    private SVNPropertyValue maybePropValue(String propname, SVNSkel child) throws SVNException {
        if (child.getListSize() != 1) {
            return null;
        }
        byte[] data = child.getChild(0).getData();
        return SVNPropertyValue.create(propname, data);
    }

    private String generateConflictMessage(String propname, SVNPropertyValue original, SVNPropertyValue mine, SVNPropertyValue incoming, SVNPropertyValue incomingBase) {
        if (incomingBase == null) {
            if (mine != null) {
                assert (!mine.equals(incoming));
                return String.format("Trying to add new property '%s'\nbut the property already exists.\n", propname);
            }
            return String.format("Trying to add new property '%s'\nbut the property has been locally deleted.\n", propname);
        }
        
        if (incoming == null) {
            if (original == null && mine != null) {
                return String.format("Trying to delete property '%s'\nbut the property has been locally added.\n", propname);
            }
            if (original.equals(incomingBase)) {
                if (mine != null) {
                    return String.format("Trying to delete property '%s'\nbut the property has been locally modified.\n", propname);
                }
            } else if (mine == null) {
                return String.format("Trying to delete property '%s'\nbut the property has been locally deleted and had a different value.\n", propname);
            }
            return String.format("Trying to delete property '%s'\nbut the local property value is different.\n", propname);
        }
        if (original != null && mine != null && original.equals(mine)) {
            return String.format("Trying to change property '%s'\nbut the local property value conflicts with the incoming change.\n", propname);
        }
        if (original != null && mine != null) {
            return String.format("Trying to change property '%s'\nbut the property has already been locally changed to a different value.\n", propname);
        }
        if (original != null) {
            return String.format("Trying to change property '%s'\nbut the property has been locally deleted.\n", propname);
        }
        if (mine != null) {
            return String.format("Trying to change property '%s'\nbut the property has been locally added with a different value.\n", propname);
        }
        return String.format("Trying to change property '%s'\nbut the property does not exist locally.\n", propname);
    }

    public boolean resolveConflictOnNode(File localAbsPath, boolean resolveText, boolean resolveProps, SVNConflictChoice conflictChoice) throws SVNException {
        boolean foundFile;
        File conflictOld = null;
        File conflictNew = null;
        File conflictWorking = null;
        File propRejectFile = null;
        SVNWCDbKind kind;
        List<SVNConflictDescription> conflicts;
        File conflictDirAbspath;
        boolean didResolve = false;
        kind = getDb().readKind(localAbsPath, true);
        conflicts = getDb().readConflicts(localAbsPath);
        for (SVNConflictDescription desc : conflicts) {
            if (desc.isTextConflict()) {
                conflictOld = desc.getMergeFiles().getBaseFile();
                conflictNew = desc.getMergeFiles().getRepositoryFile();
                conflictWorking = desc.getMergeFiles().getLocalFile();
            } else if (desc.isPropertyConflict()) {
                propRejectFile = desc.getMergeFiles().getRepositoryFile();
            }
        }
        if (kind == SVNWCDbKind.Dir) {
            conflictDirAbspath = localAbsPath;
        } else {
            conflictDirAbspath = SVNFileUtil.getFileDir(localAbsPath);
        }
        if (resolveText) {
            File autoResolveSrc = null;
            if (conflictChoice == SVNConflictChoice.BASE) {
                autoResolveSrc = conflictOld;
            } else if (conflictChoice == SVNConflictChoice.MINE_FULL) {
                autoResolveSrc = conflictWorking;
            } else if (conflictChoice == SVNConflictChoice.THEIRS_FULL) {
                autoResolveSrc = conflictNew;
            } else if (conflictChoice == SVNConflictChoice.MERGED) {
                autoResolveSrc = null;
            } else if (conflictChoice == SVNConflictChoice.THEIRS_CONFLICT || conflictChoice == SVNConflictChoice.MINE_CONFLICT) {
                if (conflictOld != null && conflictWorking != null && conflictNew != null) {
                    File tempDir = getDb().getWCRootTempDir(conflictDirAbspath);
                    SVNDiffConflictChoiceStyle style = conflictChoice == SVNConflictChoice.THEIRS_CONFLICT ? SVNDiffConflictChoiceStyle.CHOOSE_LATEST : SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED;
                    UniqueFileInfo openUniqueFile = openUniqueFile(tempDir, false);
                    autoResolveSrc = openUniqueFile.path;
                    if (!SVNFileUtil.isAbsolute(conflictOld)) {
                        conflictOld = SVNFileUtil.createFilePath(conflictDirAbspath, conflictOld);
                    }
                    if (!SVNFileUtil.isAbsolute(conflictWorking)) {
                        conflictWorking = SVNFileUtil.createFilePath(conflictDirAbspath, conflictWorking);
                    }
                    if (!SVNFileUtil.isAbsolute(conflictNew)) {
                        conflictNew = SVNFileUtil.createFilePath(conflictDirAbspath, conflictNew);
                    }
                    byte[] nullBytes = new byte[] {};
                    FSMergerBySequence merger = new FSMergerBySequence(nullBytes, nullBytes, nullBytes);
                    RandomAccessFile localIS = null;
                    RandomAccessFile latestIS = null;
                    RandomAccessFile baseIS = null;
                    OutputStream result = null;
                    try {
                        result = SVNFileUtil.openFileForWriting(autoResolveSrc);
                        baseIS = new RandomAccessFile(conflictOld, "r");
                        localIS = new RandomAccessFile(conflictWorking, "r");
                        latestIS = new RandomAccessFile(conflictNew, "r");
                        QSequenceLineRAData baseData = new QSequenceLineRAFileData(baseIS);
                        QSequenceLineRAData localData = new QSequenceLineRAFileData(localIS);
                        QSequenceLineRAData latestData = new QSequenceLineRAFileData(latestIS);
                        merger.merge(baseData, localData, latestData, null, result, style);
                    } catch (IOException e) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
                        SVNErrorManager.error(err, e, SVNLogType.WC);
                    } finally {
                        SVNFileUtil.closeFile(result);
                        SVNFileUtil.closeFile(localIS);
                        SVNFileUtil.closeFile(baseIS);
                        SVNFileUtil.closeFile(latestIS);
                    }
                } else {
                    autoResolveSrc = null;
                }
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "Invalid 'conflict_result' argument");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (autoResolveSrc != null) {
                SVNFileUtil.copyFile(SVNFileUtil.createFilePath(conflictDirAbspath, autoResolveSrc), localAbsPath, true);
            }
        }
        foundFile = false;
        if (resolveText) {
            foundFile = attemptDeletion(conflictDirAbspath, conflictOld);
            foundFile = attemptDeletion(conflictDirAbspath, conflictNew);
            foundFile = attemptDeletion(conflictDirAbspath, conflictWorking);
            resolveText = conflictOld != null || conflictNew != null || conflictWorking != null;
        }
        if (resolveProps) {
            if (propRejectFile != null) {
                foundFile = attemptDeletion(conflictDirAbspath, propRejectFile);
            } else {
                resolveProps = false;
            }
        }
        if (resolveText || resolveProps) {
            getDb().opMarkResolved(localAbsPath, resolveText, resolveProps, false);
            if (foundFile) {
                didResolve = true;
            }
        }
        return didResolve;
    }
    
    private void resolveOneConflict(File localAbsPath, boolean resolveText, 
    		String resolveProps, boolean resolveTree, SVNConflictChoice conflictChoice) throws SVNException {
    	
    	List<SVNConflictDescription> conflicts;
    	boolean resolved = false;
    	
    	conflicts = getDb().readConflicts(localAbsPath);
    	for (SVNConflictDescription desc : conflicts) {
    		if (desc.isTreeConflict()) {
    			if (!resolveTree) 
    				break;
    			/* For now, we only clear tree conflict information and resolve
	             * to the working state. There is no way to pick theirs-full
	             * or mine-full, etc. Throw an error if the user expects us
	             * to be smarter than we really are. */
    			if (conflictChoice != SVNConflictChoice.MERGED) {
    				SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "Tree conflicts can only be resolved to 'working' state; {0} not resolved", localAbsPath);
                    SVNErrorManager.error(err, SVNLogType.WC);
	            }
    			getDb().opSetTreeConflict(localAbsPath, null);
	            resolved = true;
	            break;
    		}
    		else if (desc.isTextConflict()) {
    			if (!resolveText) 
    				break;
    			if (resolveConflictOnNode(localAbsPath, true, false, conflictChoice))
    				resolved = true;
    			break;
    		}
    		else if (desc.isPropertyConflict()) {
    			if ("".equals(resolveProps))
    				break;
    			
    			/* ### this is bogus. resolve_conflict_on_node() does not handle
	               ### individual property resolution.  */
	            if ("".equals(resolveProps) && !resolveProps.equals(desc.getPropertyName()))
	                break; /* Skip this property conflict */

	            /* We don't have property name handling here yet :( */
	            if (resolveConflictOnNode(localAbsPath, false, true, conflictChoice))
	            	resolved = true;
	            break;
    		}
    		else {
    			break;
    		}
    	}

    	if (resolved && getEventHandler() != null) {
    		SVNEvent event = new SVNEvent(localAbsPath, null, null, -1, null, null, null, null, SVNEventAction.RESOLVED, null, null, null, null, null, null);
            getEventHandler().handleEvent(event, 0);
    	}
    }
    
    private void recursiveResolveConflict(File localAbsPath, boolean thisIsConflicted, SVNDepth depth, boolean resolveText, 
    		String resolveProps, boolean resolveTree, SVNConflictChoice conflictChoice) throws SVNException {
    	SVNHashMap visited = new SVNHashMap();
    	SVNDepth childDepth;
    	
    	checkCancelled();
    	
    	if (thisIsConflicted) {
    		resolveOneConflict(localAbsPath, resolveText, resolveProps, resolveTree, conflictChoice);
    	}
    	
    	
    	if (depth.getId() < SVNDepth.FILES.getId())
    		return;
    	
    	childDepth = (depth.getId() < SVNDepth.INFINITY.getId()) ? SVNDepth.EMPTY : depth;
    	Set<String> children = db.readChildren(localAbsPath);
    	for (String child : children) {
    		checkCancelled();
    		File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, child);
    		WCDbInfo readInfo = db.readInfo(childAbsPath, InfoField.status, InfoField.kind, InfoField.conflicted);
    		SVNWCDbStatus status = readInfo.status;
            SVNWCDbKind kind = readInfo.kind;
            boolean conflicted = readInfo.conflicted;
            if (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.ServerExcluded) {
            	continue;
            }
            visited.put(child, child);
            if (kind == SVNWCDbKind.Dir && depth.getId() < SVNDepth.IMMEDIATES.getId())
            	continue;
    		if (kind == SVNWCDbKind.Dir)
    			recursiveResolveConflict(childAbsPath, conflicted, childDepth, resolveText, resolveProps, resolveTree, conflictChoice);
    		else if (conflicted)
    			resolveOneConflict(childAbsPath, resolveText, resolveProps, resolveTree, conflictChoice);
    	}
    	
    	List<String> conflictVictims = getDb().readConflictVictims(localAbsPath);

    	for (String child : conflictVictims) {
    		if (visited.containsKey(child))
    	        continue; /* Already visited */

    	    checkCancelled();
    	      
    	    File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, child);

    	    /* We only have to resolve one level of tree conflicts. All other
    	        conflicts are resolved in the other loop */
    	    resolveOneConflict(childAbsPath, false, "", resolveTree, conflictChoice);
   	    }
    }
    
    public void resolvedConflict(File localAbsPath, SVNDepth depth, boolean resolveText, 
    		String resolveProps, boolean resolveTree, SVNConflictChoice conflictChoice) throws SVNException {
    	/* ### the underlying code does NOT support resolving individual
	       ### properties. bail out if the caller tries it.  */
	    if (resolveProps != null && !"".equals(resolveProps)) {
    		SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "Resolving a single property is not (yet) supported.");
            SVNErrorManager.error(err, SVNLogType.WC);
    	}
    	
        WCDbInfo readInfo = db.readInfo(localAbsPath, InfoField.kind, InfoField.conflicted);
        SVNWCDbKind dbKind = readInfo.kind;
        boolean conflicted = readInfo.conflicted;
            
    	/* When the implementation still used the entry walker, depth
    	  unknown was translated to infinity. */
    	if (dbKind != SVNWCDbKind.Dir)
    		depth = SVNDepth.EMPTY;
    	else if (depth == SVNDepth.UNKNOWN)
    		depth = SVNDepth.INFINITY;
    	
    	recursiveResolveConflict(localAbsPath, conflicted, depth, resolveText, resolveProps, resolveTree, conflictChoice);
    }
            
    private boolean attemptDeletion(File parentDir, File baseName) throws SVNException {
        if (baseName == null) {
            return false;
        }
        File fullPath = SVNFileUtil.createFilePath(parentDir, baseName);
        return SVNFileUtil.deleteFile(fullPath);
    }

    public int checkWC(File localAbsPath) throws SVNException {
        return checkWC(localAbsPath, false);
    }

    private int checkWC(File localAbsPath, boolean check) throws SVNException {
        int wcFormat = 0;
        try {
            wcFormat = db.getFormatTemp(localAbsPath);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_MISSING) {
                throw e;
            }
            wcFormat = 0;
            SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath));
            if (kind == SVNNodeKind.NONE) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "''{0}'' does not exist", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return 0;
            }
        }
        if (wcFormat >= WC_NG_VERSION) {
            if (check) {
                SVNNodeKind wcKind = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath));
                if (wcKind != SVNNodeKind.DIR) {
                    return 0;
                }
            }
            SVNWCDbStatus dbStatus;
            SVNWCDbKind dbKind;
            try {
                WCDbInfo readInfo = db.readInfo(localAbsPath, InfoField.status, InfoField.kind);
                dbStatus = readInfo.status;
                dbKind = readInfo.kind;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    return 0;
                }
                throw e;
            }
            if (dbKind != SVNWCDbKind.Dir) {
                return 0;
            }
            switch (dbStatus) {
                case NotPresent:
                case ServerExcluded:
                case Excluded:
                    return 0;
                default:
                    break;
            }
        }
        return wcFormat;
    }

    public void initializeWC(File localAbspath, SVNURL url, SVNURL repositoryRoot, String uuid, long revision, SVNDepth depth) throws SVNException {
        assert (isAbsolute(localAbspath));
        assert (url != null);
        assert (repositoryRoot != null);
        assert (uuid != null);
        // assert(svn_uri_is_ancestor(repos_root_url, url));

        int format = checkWC(localAbspath, true);
        File reposRelpath = SVNFileUtil.createFilePath(SVNWCUtils.isChild(repositoryRoot, url));
        if (reposRelpath == null) {
            reposRelpath = SVNFileUtil.createFilePath("");
        }
        if (format != SVNWCDb.WC_FORMAT_17) {
            initWC(localAbspath, reposRelpath, repositoryRoot, uuid, revision, depth);
            return;
        }
        WCDbInfo readInfo = db.readInfo(localAbspath, InfoField.status, InfoField.revision, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.reposUuid);
        SVNWCDbStatus status = readInfo.status;
        long dbRevision = readInfo.revision;
        File dbReposRelpath = readInfo.reposRelPath;
        SVNURL dbReposRootUrl = readInfo.reposRootUrl;
        String dbReposUuid = readInfo.reposUuid;
        if (status != SVNWCDbStatus.Deleted && status != SVNWCDbStatus.NotPresent) {
            if (dbRevision != revision) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Revision {0} doesn't match existing revision {1} in ''{2}''", new Object[] {
                        revision, dbRevision, localAbspath
                });
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (dbReposRootUrl == null) {
                if (status == SVNWCDbStatus.Added) {
                    WCDbAdditionInfo scanAddition = db.scanAddition(localAbspath, AdditionInfoField.reposRelPath, AdditionInfoField.reposRootUrl, AdditionInfoField.reposUuid);
                    dbReposRelpath = scanAddition.reposRelPath;
                    dbReposRootUrl = scanAddition.reposRootUrl;
                    dbReposUuid = scanAddition.reposUuid;
                } else {
                    WCDbRepositoryInfo scanBase = db.scanBaseRepository(localAbspath, RepositoryInfoField.relPath, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
                    dbReposRelpath = scanBase.relPath;
                    dbReposRootUrl = scanBase.rootUrl;
                    dbReposUuid = scanBase.uuid;
                }
            }
            if (!dbReposUuid.equals(uuid) || !dbReposRootUrl.equals(repositoryRoot) || !SVNWCUtils.isAncestor(dbReposRelpath, reposRelpath)) {
                NodeCopyFromInfo copyFromInfo = getNodeCopyFromInfo(localAbspath, NodeCopyFromField.rootUrl, NodeCopyFromField.reposRelPath);
                SVNURL copyfromRootUrl = copyFromInfo.rootUrl;
                File copyfromReposRelpath = copyFromInfo.reposRelPath;
                if (copyfromRootUrl == null || !copyfromRootUrl.equals(repositoryRoot) || !copyfromReposRelpath.equals(reposRelpath)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "URL ''{0}'' doesn't match existing URL ''{1}'' in ''{2}''", new Object[] {
                            url, SVNWCUtils.join(dbReposRootUrl, dbReposRelpath), localAbspath
                    });
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
        }
    }

    private void initWC(File localAbspath, File reposRelpath, SVNURL repositoryRoot, String uuid, long revNumber, SVNDepth depth) throws SVNException {
        File wcAdmDir = SVNWCUtils.admChild(localAbspath, null);
        SVNFileUtil.ensureDirectoryExists(wcAdmDir);
        SVNFileUtil.setHidden(wcAdmDir, true);
        SVNFileUtil.ensureDirectoryExists(SVNWCUtils.admChild(localAbspath, WC_ADM_PRISTINE));
        SVNFileUtil.ensureDirectoryExists(SVNWCUtils.admChild(localAbspath, WC_ADM_TMP));
        SVNFileUtil.writeToFile(SVNWCUtils.admChild(localAbspath, WC_ADM_FORMAT), "12", null);
        SVNFileUtil.writeToFile(SVNWCUtils.admChild(localAbspath, WC_ADM_ENTRIES), "12", null);
        db.init(localAbspath, reposRelpath, repositoryRoot, uuid, revNumber, depth);
    }

    public String getActualTarget(File path) throws SVNException {
        boolean wcRoot = false;
        boolean switched = false;
        SVNWCDbKind kind = null;
        try {
            CheckWCRootInfo checkWCRoot = checkWCRoot(path.getAbsoluteFile(), true);
            wcRoot = checkWCRoot.wcRoot;
            kind = checkWCRoot.kind;
            switched = checkWCRoot.switched;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND && e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                throw e;
            }
        }
        if (!(wcRoot || switched) || (kind != SVNWCDbKind.Dir)) {
            return path.getName();
        }
        return "";
    }
    
    public File getNodeReposRelPath(File localAbsPath) throws SVNException {
        WCDbInfo readInfo = getDb().readInfo(localAbsPath, InfoField.status, InfoField.reposRelPath, InfoField.haveBase);
        SVNWCDbStatus status = readInfo.status;
        File reposRelPath = readInfo.reposRelPath;
        boolean haveBase = readInfo.haveBase;
        if (reposRelPath == null) {
            if (status == SVNWCDbStatus.Added) {
                reposRelPath = getDb().scanAddition(localAbsPath, AdditionInfoField.reposRelPath).reposRelPath;
            } else if (haveBase) {
                reposRelPath = getDb().scanBaseRepository(localAbsPath, RepositoryInfoField.relPath).relPath;
            } else if (status == SVNWCDbStatus.Excluded || (!haveBase && (status == SVNWCDbStatus.Deleted))) {
                File parentAbspath = SVNFileUtil.getParentFile(localAbsPath);
                String name = SVNFileUtil.getFileName(localAbsPath);
                File parentRelpath = getNodeReposRelPath(parentAbspath);
                if (parentRelpath != null) {
                    reposRelPath = SVNFileUtil.createFilePath(parentRelpath, name);
                }
            } else {
                /* Status: obstructed, obstructed_add */
                reposRelPath = null;
            }
        }
        return reposRelPath;
    }

    public boolean isChangelistMatch(File localAbsPath, Collection<String> changelistsSet) {
        if (changelistsSet == null || changelistsSet.isEmpty()) {
            return true;
        }
        try {
            String changelist = db.readInfo(localAbsPath, InfoField.changelist).changelist;
            return changelist != null && changelistsSet.contains(changelist);
        } catch (SVNException e) {
            return false;
        }
    }

    public boolean isNodeStatusDeleted(File localAbsPath) throws SVNException {
        return getDb().readInfo(localAbsPath, InfoField.status).status == SVNWCDbStatus.Deleted;
    }

    public static class PropDiffs {

        public SVNProperties propChanges;
        public SVNProperties originalProps;
    }

    public static class ConflictInfo {
        public boolean textConflicted;
        public boolean propConflicted;
        public boolean treeConflicted;
        public File baseFile;
        public File repositoryFile;
        public File localFile;
        public File propRejectFile;
        public SVNTreeConflictDescription treeConflict;
    }

    public PropDiffs getPropDiffs(File localAbsPath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbsPath));
        PropDiffs propDiffs = new PropDiffs();
        SVNProperties baseProps = getDb().readPristineProperties(localAbsPath);
        if (baseProps == null) {
            baseProps = new SVNProperties();
        }
        propDiffs.originalProps = baseProps;
        SVNProperties actualProps = getDb().readProperties(localAbsPath);
        propDiffs.propChanges = SVNWCUtils.propDiffs(actualProps, baseProps);
        return propDiffs;
    }

    public SVNWCDbLock getNodeLock(File localAbsPath) throws SVNException {
        try {
            return getDb().getBaseInfo(localAbsPath, BaseInfoField.lock).lock;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            return null;
        }
    }

    public List<File> getNodeChildren(File dirAbsPath, boolean showHidden) throws SVNException {
        Set<String> relChildren = getDb().readChildren(dirAbsPath);
        List<File> childs = new ArrayList<File>();
        for (String child : relChildren) {
            File childAbsPath = SVNFileUtil.createFilePath(dirAbsPath, child);
            if (!showHidden) {
                boolean childIsHiden = getDb().isNodeHidden(childAbsPath);
                if (childIsHiden) {
                    continue;
                }
            }
            childs.add(childAbsPath);
        }
        return childs;
    }

    public List<File> getChildrenOfWorkingNode(File dirAbsPath, boolean showHidden) throws SVNException {
        Set<String> relChildren = getDb().getChildrenOfWorkingNode(dirAbsPath);
        List<File> childs = new ArrayList<File>();
        for (String child : relChildren) {
            File childAbsPath = SVNFileUtil.createFilePath(dirAbsPath, child);
            if (!showHidden) {
                boolean childIsHiden = getDb().isNodeHidden(childAbsPath);
                if (childIsHiden) {
                    continue;
                }
            }
            childs.add(childAbsPath);
        }
        return childs;
    }

    public SVNDepth getNodeDepth(File localAbsPath) throws SVNException {
        return getDb().readInfo(localAbsPath, InfoField.depth).depth;
    }

    public SVNSkel wqBuildFileCommit(File localAbspath, boolean propsMods) throws SVNException {
        SVNSkel workItem = SVNSkel.createEmptyList();
        workItem.prependString("0");
        workItem.prependString("0");
        workItem.prependPath(getRelativePath(localAbspath));
        workItem.prependString(WorkQueueOperation.FILE_COMMIT.getOpName());

        SVNSkel result = SVNSkel.createEmptyList();
        result.appendChild(workItem);
        return result;
    }
    
    private File getRelativePath(File localAbsPath) throws SVNException {
        if (localAbsPath == null) {
            return null;
        }
        DirParsedInfo parseDir = ((SVNWCDb) getDb()).parseDir(localAbsPath, Mode.ReadWrite);
        return parseDir.localRelPath;
    }

    public void ensureNoUnfinishedTransactions() throws SVNException {
        ((SVNWCDb) getDb()).ensureNoUnfinishedTransactions();
    }
    
    public void canonicalizeURLs(File path, SVNExternalsStore externalsStore, boolean omitDefaultPort) throws SVNException {
        DirParsedInfo parseDir = ((SVNWCDb) getDb()).parseDir(path, Mode.ReadWrite);
        SvnWcDbShared.canonicalizeURLs(parseDir.wcDbDir.getWCRoot(), true, externalsStore, omitDefaultPort);
        wqRun(path);
    }

    public void setSqliteJournalMode(SqlJetPagerJournalMode sqliteJournalMode) {
        if (this.db != null) {
            ((SVNWCDb) this.db).setJournalModel(sqliteJournalMode);
        }
    }
}
