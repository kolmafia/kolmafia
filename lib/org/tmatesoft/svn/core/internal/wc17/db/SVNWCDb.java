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
package org.tmatesoft.svn.core.internal.wc17.db;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.internal.SqlJetPagerJournalMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetTransaction;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNTreeConflictUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
import org.tmatesoft.svn.core.internal.wc17.SVNExternalsStore;
import org.tmatesoft.svn.core.internal.wc17.SVNTreeConflictEditor;
import org.tmatesoft.svn.core.internal.wc17.SVNWCConflictDescription17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCConflictDescription17.ConflictKind;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbAdditionInfo.AdditionInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbDeletionInfo.DeletionInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDbRoot.WCLock;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.PristineInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbConflicts.ConflictInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbConflicts.TextConflictInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbConflicts.TreeConflictInfo;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbCreateSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.DELETE_LIST__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.PRISTINE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.REPOSITORY__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.WC_LOCK__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSelectCommittableExternalsImmediatelyBelow;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSelectDeletionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSelectMinMaxRevisions;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSelectMovedForDelete;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSelectMovedFromForDelete;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSelectOpDepthMovedPair;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNMergeFileSet;
import org.tmatesoft.svn.core.wc.SVNOperation;
import org.tmatesoft.svn.core.wc.SVNPropertyConflictDescription;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNTextConflictDescription;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared.begingReadTransaction;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared.commitTransaction;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared.doesNodeExists;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared.getDepthInfo;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared.getMovedFromInfo;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnBlob;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnBoolean;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnChecksum;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnDepth;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnInt64;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnKind;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnPath;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnPresence;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnProperties;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnRevNum;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnText;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getKindText;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getPresenceText;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getTranslatedSize;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.hasColumnProperties;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.isColumnNull;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.parseDepth;

/**
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDb implements ISVNWCDb {

    public static final int FORMAT_FROM_SDB = -1;
    public static final long UNKNOWN_WC_ID = -1;
    static final long INVALID_REPOS_ID = -1;


    public static boolean isAbsolute(File localAbsPath) {
        return localAbsPath != null && localAbsPath.isAbsolute();
    }

    public static <E extends Enum<E>> EnumSet<E> getInfoFields(Class<E> clazz, E... fields) {
        final EnumSet<E> set = EnumSet.noneOf(clazz);
        for (E f : fields) {
            set.add(f);
        }
        return set;
    }

    private ISVNOptions config;
    private boolean autoUpgrade;
    private boolean enforceEmptyWQ;
    private Map<String, SVNWCDbDir> dirData;
    private SqlJetPagerJournalMode journalMode;
    private boolean temporaryDbInMemory;
    private boolean isAllowWC17Access;

    public SVNWCDb() {
    }

    public void setWC17SupportEnabled(boolean allowed) {
        this.isAllowWC17Access = allowed;
    }

    public boolean isWC17AccessEnabled() {
        return isAllowWC17Access;
    }

    public void open(final SVNWCDbOpenMode mode, final ISVNOptions config, final boolean autoUpgrade, final boolean enforceEmptyWQ) {
        this.config = config;
        this.autoUpgrade = autoUpgrade;
        this.enforceEmptyWQ = enforceEmptyWQ;
        this.dirData = new HashMap<String, SVNWCDbDir>();
    }

    public void setJournalModel(SqlJetPagerJournalMode journalMode) {
        this.journalMode = journalMode;
    }

    public void setTemporaryDbInMemory(boolean temporaryDbInMemory) {
        this.temporaryDbInMemory = temporaryDbInMemory;
    }

    public void close() {
        final Set<SVNWCDbRoot> roots = new HashSet<SVNWCDbRoot>();
        /* Collect all the unique WCROOT structures, and empty out DIR_DATA. */
        if (dirData != null) {
            for (Map.Entry<String, SVNWCDbDir> entry : dirData.entrySet()) {
                final SVNWCDbDir pdh = entry.getValue();
                if (pdh.getWCRoot() != null && pdh.getWCRoot().getSDb() != null) {
                    roots.add(pdh.getWCRoot());
                }
            }
            dirData.clear();
        }
        /* Run the cleanup for each WCROOT. */
        closeManyWCRoots(roots);
    }

    public void ensureNoUnfinishedTransactions() throws SVNException {
        final Set<SVNWCDbRoot> roots = new HashSet<SVNWCDbRoot>();
        /* Collect all the unique WCROOT structures, and empty out DIR_DATA. */
        if (dirData != null) {
            for (Map.Entry<String, SVNWCDbDir> entry : dirData.entrySet()) {
                final SVNWCDbDir pdh = entry.getValue();
                if (pdh.getWCRoot() != null && pdh.getWCRoot().getSDb() != null) {
                    roots.add(pdh.getWCRoot());
                }
            }
        }
        for (final SVNWCDbRoot wcRoot : roots) {
            wcRoot.ensureNoUnfinishedTransactions();
        }
    }

    private void closeManyWCRoots(final Set<SVNWCDbRoot> roots) {
        for (final SVNWCDbRoot wcRoot : roots) {
            try {
                wcRoot.close();
            } catch (SVNException e) {
                // TODO SVNException closeManyWCRoots()
            }
        }
    }

    public ISVNOptions getConfig() {
        return config;
    }

    public void init(File localAbsPath, File reposRelPath, SVNURL reposRootUrl, String reposUuid, long initialRev, SVNDepth depth, int workingCopyFormat) throws SVNException {

        assert (SVNFileUtil.isAbsolute(localAbsPath));
        assert (reposRelPath != null);
        assert (depth == SVNDepth.EMPTY || depth == SVNDepth.FILES || depth == SVNDepth.IMMEDIATES || depth == SVNDepth.INFINITY);

        /* ### REPOS_ROOT_URL and REPOS_UUID may be NULL. ... more doc: tbd */

        /* Create the SDB and insert the basic rows. */
        CreateDbInfo createDb = createDb(localAbsPath, reposRootUrl, reposUuid, SDB_FILE, workingCopyFormat, false);

        /* Begin construction of the PDH. */
        SVNWCDbDir pdh = new SVNWCDbDir(localAbsPath);

        /* Create the WCROOT for this directory. */
        pdh.setWCRoot(new SVNWCDbRoot(this, localAbsPath, createDb.sDb, createDb.wcId, FORMAT_FROM_SDB, false, true, false));

        /* The PDH is complete. Stash it into DB. */
        dirData.put(localAbsPath.getAbsolutePath(), pdh);

        InsertBase ibb = new InsertBase();

        if (initialRev > 0) {
            ibb.status = SVNWCDbStatus.Incomplete;
        } else {
            ibb.status = SVNWCDbStatus.Normal;
        }

        ibb.kind = SVNWCDbKind.Dir;
        ibb.reposId = createDb.reposId;
        ibb.reposRelpath = reposRelPath;
        ibb.revision = initialRev;

        /* ### what about the children? */
        ibb.children = null;
        ibb.depth = depth;

        ibb.wcId = createDb.wcId;
        ibb.wcRoot = pdh.getWCRoot();
        ibb.localRelpath = SVNFileUtil.createFilePath("");
        /* ### no children, conflicts, or work items to install in a txn... */

        createDb.sDb.runTransaction(ibb);
    }

    private static class CreateDbInfo {

        public SVNSqlJetDb sDb;
        public long reposId;
        public long wcId;
    }

    private CreateDbInfo createDb(File dirAbsPath, SVNURL reposRootUrl, String reposUuid, String sdbFileName, int workingCopyFormat,
            boolean isUpgrade) throws SVNException {

        CreateDbInfo info = new CreateDbInfo();

        info.sDb = openDb(dirAbsPath, sdbFileName, SVNSqlJetDb.Mode.RWCreate, journalMode, temporaryDbInMemory);

        /* Create the database's schema. */
        SVNWCDbCreateSchema createSchema = new SVNWCDbCreateSchema(info.sDb, SVNWCDbCreateSchema.MAIN_DB_STATEMENTS, workingCopyFormat, isUpgrade);
        try {
            createSchema.exec();
        } finally {
            createSchema.reset();
        }

        /* Insert the repository. */
        info.reposId = createReposId(info.sDb, reposRootUrl, reposUuid);

        /* Insert the wcroot. */
        /* ### Right now, this just assumes wc metadata is being stored locally. */
        final SVNSqlJetStatement statement = info.sDb.getStatement(SVNWCDbStatements.INSERT_WCROOT);
        try {
            info.wcId = statement.done();
            return info;
        } finally {
            statement.reset();
        }
    }

    /**
     * For a given REPOS_ROOT_URL/REPOS_UUID pair, return the existing REPOS_ID
     * value. If one does not exist, then create a new one.
     *
     * @throws SVNException
     */
    public long createReposId(SVNSqlJetDb sDb, SVNURL reposRootUrl, String reposUuid) throws SVNException {

        final SVNSqlJetStatement getStmt = sDb.getStatement(SVNWCDbStatements.SELECT_REPOSITORY);
        try {
            getStmt.bindf("s", reposRootUrl);
            boolean haveRow = getStmt.next();
            if (haveRow) {
                return getColumnInt64(getStmt, SVNWCDbSchema.WCROOT__Fields.id);
            }
        } finally {
            getStmt.reset();
        }

        /*
         * NOTE: strictly speaking, there is a race condition between the above
         * query and the insertion below. We're simply going to ignore that, as
         * it means two processes are *modifying* the working copy at the same
         * time, *and* new repositores are becoming visible. This is rare
         * enough, let alone the miniscule chance of hitting this race
         * condition. Further, simply failing out will leave the database in a
         * consistent state, and the user can just re-run the failed operation.
         */

        final SVNSqlJetStatement insertStmt = sDb.getStatement(SVNWCDbStatements.INSERT_REPOSITORY);
        try {
            insertStmt.bindf("ss", reposRootUrl, reposUuid);
            return insertStmt.done();
        } finally {
            insertStmt.reset();
        }
    }

    public static void addWorkItems(SVNSqlJetDb sDb, SVNSkel skel) throws SVNException {
        /* Maybe there are no work items to insert. */
        if (skel == null) {
            return;
        }

        /* Is the list a single work item? Or a list of work items? */
        if (skel.first().isAtom()) {
            addSingleWorkItem(sDb, skel);
        } else {
            /* SKEL is a list-of-lists, aka list of work items. */
            for (int i = 0; i < skel.getListSize(); i++) {
                addSingleWorkItem(sDb, skel.getChild(i));
            }
        }

    }

    private static void addSingleWorkItem(SVNSqlJetDb sDb, SVNSkel workItem) throws SVNException {
        final byte[] serialized = workItem.unparse();
        final SVNSqlJetStatement stmt = sDb.getStatement(SVNWCDbStatements.INSERT_WORK_ITEM);
        try {
            stmt.bindBlob(1, serialized);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public Map<File, File> getExternalsDefinedBelow(File localAbsPath) throws SVNException {
        Map<File, File> externals = new TreeMap<File, File>(Collections.reverseOrder());

        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_EXTERNALS_DEFINED);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            while(stmt.next()) {
                localRelpath = SVNFileUtil.createFilePath(getColumnText(stmt, SVNWCDbSchema.EXTERNALS__Fields.local_relpath));
                File defLocalRelpath = SVNFileUtil.createFilePath(getColumnText(stmt, SVNWCDbSchema.EXTERNALS__Fields.def_local_relpath));
                externals.put(SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), localRelpath),
                        SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), defLocalRelpath));
            }
        } finally {
            stmt.reset();
        }
        return externals;
    }

    public void gatherExternalDefinitions(File localAbsPath, SVNExternalsStore externals) throws SVNException {
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_EXTERNAL_PROPERTIES);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            while(stmt.next()) {
                SVNProperties properties = getColumnProperties(stmt, NODES__Fields.properties);
                if (properties == null) {
                    continue;
                }
                String externalProperty = properties.getStringValue(SVNProperty.EXTERNALS);
                if (externalProperty == null) {
                    continue;
                }

                File nodeRelPath = SVNFileUtil.createFilePath(getColumnText(stmt, NODES__Fields.local_relpath));
                File nodeAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), nodeRelPath);
                externals.addExternal(nodeAbsPath, null, externalProperty);
                String depthWord = getColumnText(stmt, NODES__Fields.depth);
                externals.addDepth(nodeAbsPath, parseDepth(depthWord));
            }
        } finally {
            stmt.reset();
        }
    }

    public void addBaseExcludedNode(File localAbsPath, File reposRelPath, SVNURL reposRootUrl, String reposUuid, long revision, SVNWCDbKind kind, SVNWCDbStatus status, SVNSkel conflict,
            SVNSkel workItems) throws SVNException {
        assert (status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded);
        addExcludedOrNotPresentNode(localAbsPath, reposRelPath, reposRootUrl, reposUuid, revision, kind, status, conflict, workItems);
    }

    public void addBaseDirectory(File localAbsPath, File reposRelPath, SVNURL reposRootUrl, String reposUuid, long revision, SVNProperties props, long changedRev, SVNDate changedDate, String changedAuthor,
            List<File> children, SVNDepth depth, SVNProperties davCache, SVNSkel conflict, boolean updateActualProps, SVNProperties actualProps,
            Map<String, SVNProperties> iprops, SVNSkel workItems) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbsPath));
        assert (reposRelPath != null);
        assert (reposUuid != null);
        assert (SVNRevision.isValidRevisionNumber(revision));
        assert (props != null);
        assert (SVNRevision.isValidRevisionNumber(changedRev));

        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        InsertBase ibb = new InsertBase();
        ibb.reposRootURL = reposRootUrl;
        ibb.reposUUID = reposUuid;

        ibb.status = SVNWCDbStatus.Normal;
        ibb.kind = SVNWCDbKind.Dir;
        ibb.reposRelpath = reposRelPath;
        ibb.revision = revision;

        ibb.props = props;
        ibb.changedRev = changedRev;
        ibb.changedDate = changedDate;
        ibb.changedAuthor = changedAuthor;

        ibb.children = children;
        ibb.depth = depth;

        ibb.davCache = davCache;
        ibb.conflict = conflict;
        ibb.workItems = workItems;

        if (updateActualProps) {
            ibb.updateActualProps = true;
            ibb.actualProps = actualProps;
        }

        ibb.iprops = iprops;
        ibb.localRelpath = localRelpath;
        ibb.wcId = pdh.getWCRoot().getWcId();
        ibb.wcRoot = pdh.getWCRoot();

        pdh.getWCRoot().getSDb().runTransaction(ibb);
        pdh.flushEntries(localAbsPath);
    }

    private class Delete implements SVNSqlJetTransaction {

        public SVNWCDbRoot root;
        public File localRelPath;
        public long deleteDepth;
        public ISVNEventHandler eventHandler;
        public File movedToRelPath;
        public boolean deleteDirExternals;
        public SVNSkel workItems;
        public SVNSkel conflict;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            int deleteOpDepth = SVNWCUtils.relpathDepth(localRelPath);

            SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
            try {
                stmt.bindf("is", root.getWcId(), localRelPath);
                boolean haveRow = stmt.next();
                if (!haveRow) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", SVNFileUtil.createFilePath(root.getAbsPath(), localRelPath));
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
                int workingOpDepth = (int) stmt.getColumnLong(NODES__Fields.op_depth);
                SVNWCDbStatus status = SvnWcDbStatementUtil.getColumnPresence(stmt, NODES__Fields.presence);
                SVNWCDbKind kind = SvnWcDbStatementUtil.getColumnKind(stmt, NODES__Fields.kind);

                boolean opRoot;
                boolean addWork = false;
                int keepOpDepth = 0;
                if (workingOpDepth < deleteOpDepth) {
                    opRoot = false;
                    addWork = true;
                    keepOpDepth = workingOpDepth;
                } else {
                    opRoot = true;

                    haveRow = stmt.next();
                    if (haveRow) {
                        int belowOpDepth = (int) stmt.getColumnLong(NODES__Fields.op_depth);
                        SVNWCDbStatus belowStatus = SvnWcDbStatementUtil.getColumnPresence(stmt, NODES__Fields.presence);

                        if (belowStatus != SVNWCDbStatus.NotPresent && belowStatus != SVNWCDbStatus.BaseDeleted) {
                            addWork = true;
                            keepOpDepth = belowOpDepth;
                        } else {
                            keepOpDepth = 0;
                        }
                    } else {
                        keepOpDepth = -1;
                    }
                }

                stmt.reset();

                if (workingOpDepth != 0) {
                    status = convertToWorkingStatus(status);
                }

                if (status == SVNWCDbStatus.Deleted || status == SVNWCDbStatus.NotPresent) {
                    return;
                }

                if (status == SVNWCDbStatus.Normal && kind == SVNWCDbKind.Dir) {
                    stmt.reset();
                    stmt = root.getSDb().getStatement(SVNWCDbStatements.HAS_SERVER_EXCLUDED_DESCENDANTS);
                    stmt.bindf("is", root.getWcId(), localRelPath);
                    haveRow = stmt.next();
                    if (haveRow) {
                        String absentPath = stmt.getColumnString(NODES__Fields.local_relpath);

                        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot delete ''{0}'' as ''{1}'' is excluded by server", SVNFileUtil.createFilePath(root.getAbsPath(), localRelPath), SVNFileUtil.createFilePath(root.getAbsPath(), absentPath));
                        SVNErrorManager.error(errorMessage, SVNLogType.WC);
                    }
                } else if (status == SVNWCDbStatus.ServerExcluded) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot delete ''{0}'' as it is excluded by server", SVNFileUtil.createFilePath(root.getAbsPath(), localRelPath));
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                } else if (status == SVNWCDbStatus.Excluded) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot delete ''{0}'' as it is excluded", SVNFileUtil.createFilePath(root.getAbsPath(), localRelPath));
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }

                List<MovedNode> movedNodes = null;
                if (movedToRelPath != null) {
                    movedNodes = new ArrayList<MovedNode>();

                    int movedOpDepth = 0;
                    File movedFromRelPath = null;
                    if (status == SVNWCDbStatus.Added) {
                        Structure<StructureFields.AdditionInfo> additionInfoStructure = SvnWcDbShared.scanAddition(root, localRelPath, StructureFields.AdditionInfo.status, StructureFields.AdditionInfo.movedFromRelPath, StructureFields.AdditionInfo.movedFromOpDepth);
                        status = additionInfoStructure.get(StructureFields.AdditionInfo.status);
                        movedFromRelPath = additionInfoStructure.get(StructureFields.AdditionInfo.movedFromRelPath);
                        movedOpDepth = (int) additionInfoStructure.lng(StructureFields.AdditionInfo.movedFromOpDepth);
                    }

                    if (opRoot && movedFromRelPath != null) {
                        File part = SVNFileUtil.skipAncestor(localRelPath, movedFromRelPath);
                        MovedNode movedNode = new MovedNode();
                        if (part == null) {
                            movedNode.localRelPath = movedFromRelPath;
                        } else {
                            movedNode.localRelPath = SVNFileUtil.createFilePath(movedToRelPath, part);
                        }
                        movedNode.opDepth = movedOpDepth;
                        movedNode.movedToRelPath = movedToRelPath;
                        movedNode.movedFromDepth = -1;
                        movedNodes.add(movedNode);
                    } else if (!opRoot && (status == SVNWCDbStatus.Normal || status == SVNWCDbStatus.Copied || status == SVNWCDbStatus.MovedHere)) {
                        MovedNode movedNode = new MovedNode();
                        movedNode.localRelPath = localRelPath;
                        movedNode.opDepth = deleteOpDepth;
                        movedNode.movedToRelPath = movedToRelPath;
                        movedNode.movedFromDepth = -1;
                        movedNodes.add(movedNode);
                    }

                    stmt.reset();
                    stmt = root.getSDb().getStatement(SVNWCDbStatements.UPDATE_MOVED_TO_DESCENDANTS);
                    stmt.bindf("iss", root.getWcId(), localRelPath, movedToRelPath);
                    stmt.done();
                }
                stmt.reset();
                stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_MOVED_FOR_DELETE);
                stmt.bindf("isi", root.getWcId(), localRelPath, deleteOpDepth);
                haveRow = stmt.next();
                while (haveRow) {
                    File childRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.local_relpath));
                    File mvToRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.moved_to));
                    int childOpDepth = (int) stmt.getColumnLong(NODES__Fields.op_depth);
                    int movedFromDepth = -1;
                    boolean fixUp = false;

                    if (movedToRelPath == null && SVNFileUtil.skipAncestor(localRelPath, mvToRelPath) == null) {
                        int movedHereDepth = ((SVNWCDbSelectMovedForDelete) stmt).getMovedHereDepth();
                        if (movedHereDepth >= deleteOpDepth) {
                            fixUp = true;
                            movedFromDepth = movedHereDepth;
                        } else {
                            fixUp = true;
                            childOpDepth = deleteOpDepth;
                        }
                    } else if (movedToRelPath != null) {
                        if (deleteOpDepth == childOpDepth) {
                            fixUp = true;
                        } else if (childOpDepth >= deleteOpDepth && SVNFileUtil.skipAncestor(localRelPath, mvToRelPath) == null) {
                            childRelPath = SVNFileUtil.skipAncestor(localRelPath, childRelPath);
                            if (childRelPath != null) {
                                childRelPath = SVNFileUtil.createFilePath(movedToRelPath, childRelPath);
                                if (childOpDepth > deleteOpDepth && SVNFileUtil.skipAncestor(localRelPath, childRelPath) != null) {
                                    childOpDepth = deleteOpDepth;
                                } else {
                                    childOpDepth = SVNWCUtils.relpathDepth(childRelPath);
                                }
                                fixUp = true;
                            }
                        }
                    }

                    if (fixUp) {
                        MovedNode mn = new MovedNode();
                        mn.localRelPath = childRelPath;
                        mn.movedToRelPath = mvToRelPath;
                        mn.opDepth = childOpDepth;
                        mn.movedFromDepth = movedFromDepth;

                        if (movedNodes == null) {
                            movedNodes = new ArrayList<MovedNode>();
                        }
                        movedNodes.add(mn);
                    }

                    haveRow = stmt.next();
                }
                stmt.reset();

                if (movedNodes != null) {
                    for (Iterator<MovedNode> iterator = movedNodes.iterator(); iterator.hasNext(); ) {
                        final MovedNode movedNode = iterator.next();
                        if (movedNode.movedFromDepth > 0) {
                            ResolveMovedFrom resolveMovedFrom = resolveMovedFrom(root, localRelPath, movedNode.localRelPath, movedNode.movedFromDepth);
                            movedNode.localRelPath = resolveMovedFrom.movedFromRelPath;
                            movedNode.opDepth = resolveMovedFrom.movedFromOpDepth;
                            if (movedNode.localRelPath == null) {
                                iterator.remove();
                            }
                        }
                    }
                }

                if (movedToRelPath == null) {
                    stmt.reset();
                    stmt = root.getSDb().getStatement(SVNWCDbStatements.CLEAR_MOVED_TO_DESCENDANTS);
                    stmt.bindf("is", root.getWcId(), localRelPath);
                    stmt.done();

                    if (opRoot) {
                        stmt.reset();
                        stmt = root.getSDb().getStatement(SVNWCDbStatements.CLEAR_MOVED_TO_FROM_DEST);
                        stmt.bindf("is", root.getWcId(), localRelPath);
                        stmt.done();
                    }
                }

                stmt = new SVNWCDbCreateSchema(root.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.DELETE_LIST, -1, false);
                try {
                    stmt.done();
                } finally {
                    stmt.reset();
                }


                if (eventHandler != null) {
                    stmt.reset();
                    stmt = root.getSDb().getStatement(SVNWCDbStatements.INSERT_DELETE_LIST);
                    stmt.bindf("isi", root.getWcId(), localRelPath, workingOpDepth);
                    stmt.done();
                }

                stmt.reset();
                stmt = root.getSDb().getStatement(SVNWCDbStatements.DELETE_NODES_ABOVE_DEPTH_RECURSIVE);
                stmt.bindf("isi", root.getWcId(), localRelPath, deleteOpDepth);
                stmt.done();

                stmt.reset();
                stmt = root.getSDb().getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_LEAVING_CHANGELIST_RECURSIVE);
                stmt.bindf("is", root.getWcId(), localRelPath);
                stmt.done();

                stmt.reset();
                stmt = root.getSDb().getStatement(SVNWCDbStatements.CLEAR_ACTUAL_NODE_LEAVING_CHANGELIST_RECURSIVE);
                stmt.bindf("is", root.getWcId(), localRelPath);
                stmt.done();

                stmt.reset();
                stmt = root.getSDb().getStatement(SVNWCDbStatements.DELETE_WC_LOCK_ORPHAN_RECURSIVE);
                stmt.bindf("is", root.getWcId(), localRelPath);
                stmt.done();

                if (addWork) {
                    stmt.reset();
                    stmt = root.getSDb().getStatement(SVNWCDbStatements.INSERT_DELETE_FROM_NODE_RECURSIVE);
                    stmt.bindf("isii", root.getWcId(), localRelPath, keepOpDepth, deleteOpDepth);
                    stmt.done();
                }

                if (movedNodes != null) {
                    for (MovedNode movedNode : movedNodes) {
                        deleteUpdateMovedTo(root, movedNode.localRelPath, movedNode.opDepth, movedNode.movedToRelPath);
                    }
                }

                stmt.reset();
                stmt = root.getSDb().getStatement(SVNWCDbStatements.DELETE_FILE_EXTERNALS);
                stmt.bindf("is", root.getWcId(), localRelPath);
                stmt.done();

                stmt.reset();
                stmt = root.getSDb().getStatement(deleteDirExternals ? SVNWCDbStatements.DELETE_EXTERNAL_REGISTRATIONS : SVNWCDbStatements.DELETE_FILE_EXTERNAL_REGISTRATIONS);
                stmt.bindf("is", root.getWcId(), localRelPath);
                stmt.done();

                addWorkItems(root.getSDb(), workItems);
                if (conflict != null) {
                    markConflictInternal(root, localRelPath, conflict);
                }
            } finally {
                stmt.reset();
            }
        }
    }

    private void deleteUpdateMovedTo(SVNWCDbRoot wcRoot, File childMovedFromRelPath, int opDepth, File newMovedToRelPath) throws SVNException {
        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_MOVED_TO_RELPATH);
        try {
            stmt.bindf("isis", wcRoot.getWcId(), childMovedFromRelPath, opDepth, newMovedToRelPath);
            long rowsAffected = stmt.done();
            assert(rowsAffected == 1);
        } finally {
            stmt.reset();
        }
    }

    private ResolveMovedFrom resolveMovedFrom(SVNWCDbRoot root, File rootRelPath, File localRelPath, int opDepth) throws SVNException {
        ResolveMovedFrom resolveMovedFrom = new ResolveMovedFrom();
        String suffix = "";
        while (SVNWCUtils.relpathDepth(localRelPath) > opDepth) {
            suffix = SVNPathUtil.append(suffix, SVNFileUtil.getFileName(localRelPath));
        }
        File fromRelPath;
        int moveFromDepth;
        SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_MOVED_FROM_FOR_DELETE);
        try {
            stmt.bindf("is", root.getWcId(), localRelPath);
            boolean haveRow = stmt.next();
            if (!haveRow) {
                return resolveMovedFrom;
            }
            fromRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.local_relpath));
            int fromOpDepth = (int) stmt.getColumnLong(NODES__Fields.op_depth);
            moveFromDepth = (int) ((SVNWCDbSelectMovedFromForDelete)stmt).getMovedHereOpDepth();

            if (SVNFileUtil.skipAncestor(rootRelPath, fromRelPath) == null) {
                resolveMovedFrom.movedFromRelPath = SVNFileUtil.createFilePath(fromRelPath, suffix);
                resolveMovedFrom.movedFromOpDepth = fromOpDepth;
                return resolveMovedFrom;
            } else if (moveFromDepth == 0) {
                return resolveMovedFrom;
            }
        } finally {
            stmt.reset();
        }
        return resolveMovedFrom(root, rootRelPath, SVNFileUtil.createFilePath(fromRelPath, suffix), moveFromDepth);
    }

    private static class ResolveMovedFrom {
        File movedFromRelPath;
        int movedFromOpDepth;

        private ResolveMovedFrom() {
            movedFromOpDepth = -1;
        }
    }

    private static class MovedNode {
        public File localRelPath;
        public File movedToRelPath;
        public int opDepth;
        public int movedFromDepth;
    }

    private class InsertLock implements SVNSqlJetTransaction {

        public File localAbsPath;
        public SVNWCDbLock lock;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {

            final WCDbBaseInfo baseInfo = getBaseInfo(localAbsPath, BaseInfoField.reposRelPath, BaseInfoField.reposId);

            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.INSERT_LOCK);
            try {
                stmt.bindf("issssi",
                        baseInfo.reposId,
                        baseInfo.reposRelPath,
                        lock.token,
                        lock.owner != null ? lock.owner : null,
                        lock.comment != null ? lock.comment : null,
                        lock.date != null ? lock.date : null);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }

    }


    public class InsertBase implements SVNSqlJetTransaction {

        public Map<String, SVNProperties> iprops;
        public SVNWCDbStatus status;
        public SVNWCDbKind kind;
        public long reposId = INVALID_REPOS_ID;
        public File reposRelpath;
        public long revision = INVALID_REVNUM;

        public SVNURL reposRootURL;
        public String reposUUID;

        public SVNProperties props;
        public long changedRev = INVALID_REVNUM;
        public SVNDate changedDate;
        public String changedAuthor;
        public SVNProperties davCache;

        public List<File> children;
        public SVNDepth depth = SVNDepth.INFINITY;

        public SvnChecksum checksum;

        public File target;

        public boolean fileExternal;

        public SVNSkel conflict;

        public boolean updateActualProps;
        public SVNProperties actualProps;

       public boolean insertBaseDeleted;
       public boolean keepRecordedInfo;
        public boolean deleteWorking;

        public SVNSkel workItems;

        public long wcId;
        public File localRelpath;

        public SVNWCDbRoot wcRoot;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            long recordedSize = INVALID_FILESIZE;
            long recordedModTime = 0;
            File parentRelpath = SVNFileUtil.getFileDir(localRelpath);

            if (reposId == INVALID_REPOS_ID) {
                reposId = createReposId(db, reposRootURL, reposUUID);
            }

            if (keepRecordedInfo) {
                SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_BASE_NODE);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    boolean haveRow = stmt.next();
                    if (haveRow) {
                        recordedSize = stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.translated_size);
                        recordedModTime = stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.last_mod_time);
                    }
                } finally {
                    stmt.reset();
                }
            }


            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.INSERT_NODE);
            try {
                stmt.bindf("isisisrtstrisnnnnns",
                        wcId,
                        localRelpath,
                        0,
                        parentRelpath,
                        reposId,
                        reposRelpath,
                        revision,
                        getPresenceText(status),
                        (kind == SVNWCDbKind.Dir) ? SVNDepth.asString(depth) : null,
                        getKindText(kind),
                        changedRev,
                        changedDate,
                        changedAuthor,
                        (kind == SVNWCDbKind.Symlink) ? target : null);

                if (kind == SVNWCDbKind.File) {
                    stmt.bindChecksum(14, checksum);
                    if (recordedSize != INVALID_FILESIZE) {
                        stmt.bindLong(16, recordedSize);
                        stmt.bindLong(17, recordedModTime);
                    } else {
                        stmt.bindNull(16);
                        stmt.bindNull(17);
                    }
                } else {
                    stmt.bindNull(14);
                    stmt.bindNull(16);
                    stmt.bindNull(17);
                }

                stmt.bindProperties(15, props);
                if (davCache != null) {
                    stmt.bindProperties(18, davCache);
                } else {
                    stmt.bindNull(18);
                }
                if (fileExternal) {
                    stmt.bindString(20, "1");
                } else {
                    stmt.bindNull(20);
                }
                stmt.bindIProperties(23, iprops);
                stmt.done();
            } finally {
                stmt.reset();
            }

            if (updateActualProps) {
                SVNProperties baseProps = props;
                SVNProperties newActualProps = actualProps;
                if (baseProps != null && newActualProps != null && baseProps.size() == newActualProps.size()) {
                    SVNProperties diff = newActualProps.compareTo(baseProps);
                    if (diff.size() == 0) {
                        newActualProps = null;
                    }
                }
                setActualProperties(db, wcId, localRelpath, newActualProps);
            }

            if (kind == SVNWCDbKind.Dir && children != null) {
                insertIncompleteChildren(db, wcId, localRelpath, reposId, reposRelpath, revision, children, 0);
            }

            if (parentRelpath != null) {
                if (localRelpath != null && (status == SVNWCDbStatus.Normal || status == SVNWCDbStatus.Incomplete) && !fileExternal) {
                    extendParentDelete(db, wcId, localRelpath, kind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, 0);
                } else if (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded) {
                    retractParentDelete(db, wcId, localRelpath, 0);
                }
            }

            if (deleteWorking) {
                stmt = db.getStatement(SVNWCDbStatements.DELETE_WORKING_NODE);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            if (insertBaseDeleted) {
                stmt = db.getStatement(SVNWCDbStatements.INSERT_DELETE_FROM_BASE);
                try {
                    stmt.bindf("isi", wcId, localRelpath, SVNWCUtils.relpathDepth(localRelpath));
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            addWorkItems(db, workItems);

            if (conflict != null) {
                markConflictInternal(wcRoot, localRelpath, conflict);
            }
        }

    }

    public class InsertWorking implements SVNSqlJetTransaction {

        public SVNWCDbStatus status;
        public SVNWCDbKind kind;
        public File reposRelpath;

        public SVNProperties props;
        public long changedRev = INVALID_REVNUM;
        public SVNDate changedDate;
        public String changedAuthor;

        public List<File> children;
        public SVNDepth depth = SVNDepth.INFINITY;

        public SvnChecksum checksum;

        public File target;

        public SVNSkel workItems;

        public SVNWCDbRoot wcRoot;
        public File localRelpath;
        public File originalReposRelPath;
        public long originalReposId;
        public long originalRevision;

        public long opDepth;
        public long notPresentOpDepth;

        public SVNSkel conflict;
        public boolean movedHere;

        public boolean updateActualProps;
        public SVNProperties newActualProps;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            long wcId = wcRoot.getWcId();
            File parentRelpath = SVNFileUtil.getFileDir(localRelpath);

            File movedToRelPath = null;
            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_MOVED_TO);
            try {
                stmt.bindf("isi", wcId, localRelpath, opDepth);
                boolean haveRow = stmt.next();
                if (haveRow) {
                    movedToRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.moved_to));
                }
            } finally {
                stmt.reset();
            }

            stmt = db.getStatement(SVNWCDbStatements.INSERT_NODE);
            try {
                stmt.bindf("isisnnntstrisnnnnnsns",
                        wcId,
                        localRelpath,
                        opDepth,
                        parentRelpath,
                        getPresenceText(status),
                        (kind == SVNWCDbKind.Dir) ? SVNDepth.asString(depth) : null,
                        getKindText(kind),
                        changedRev,
                        changedDate,
                        changedAuthor,
                        (kind == SVNWCDbKind.Symlink) ? target : null,
                        movedToRelPath);

                if (movedHere) {
                    stmt.bindLong(8, 1);
                }

                if (kind == SVNWCDbKind.File) {
                    stmt.bindChecksum(14, checksum);
                }
                if (originalReposRelPath != null) {
                    stmt.bindLong(5, originalReposId);
                    stmt.bindString(6, SVNFileUtil.getFilePath(originalReposRelPath));
                    stmt.bindLong(7, originalRevision);
                }

                assert (status == SVNWCDbStatus.Normal || status == SVNWCDbStatus.Incomplete || props == null);

                stmt.bindProperties(15, props);
                stmt.done();
            } finally {
                stmt.reset();
            }

            if (kind == SVNWCDbKind.Dir && children != null) {
                insertIncompleteChildren(db, wcId, localRelpath, -1, null, originalRevision, children, opDepth);
            }

            if (updateActualProps) {
                SVNProperties baseProps = props;
                SVNProperties newActualProps = this.newActualProps;

                if (baseProps != null &&
                        newActualProps != null &&
                        (baseProps.size() == newActualProps.size())) {
                    SVNProperties diffs = newActualProps.compareTo(baseProps);
                    if (diffs.size() == 0) {
                        newActualProps = null;
                    }
                }

                setActualProperties(db, wcId, localRelpath, newActualProps);
            }

            // update changelists
            if (kind == SVNWCDbKind.Dir) {
                stmt = db.getStatement(SVNWCDbStatements.UPDATE_ACTUAL_CLEAR_CHANGELIST);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
                stmt = db.getStatement(SVNWCDbStatements.DELETE_ACTUAL_EMPTY);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            if (notPresentOpDepth > 0 && notPresentOpDepth < opDepth) {
                stmt = db.getStatement(SVNWCDbStatements.INSERT_NODE);
                try {
                    stmt.bindf("isisisrtnt",
                            wcId,
                            localRelpath,
                            notPresentOpDepth,
                            parentRelpath,
                            originalReposId,
                            originalReposRelPath,
                            originalRevision,
                            getPresenceText(SVNWCDbStatus.NotPresent),
                            getKindText(kind));
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            addWorkItems(db, workItems);

            if (conflict != null) {
                markConflictInternal(wcRoot, localRelpath, conflict);
            }
        }
    }

    public void insertIncompleteChildren(SVNSqlJetDb db, long wcId, File localRelpath, long reposId, File reposPath, long revision, List<File> children, long opDepth) throws SVNException {
        assert reposPath != null || opDepth > 0;
        assert ((reposId != -1) == (reposPath != null));

        Map movedToRelPaths = new HashMap();

        SVNSqlJetStatement stmt;

        if (opDepth > 0) {
            for (int i = children.size() - 1; i >= 0; i--) {
                File child = children.get(i);
                String name = SVNFileUtil.getFileName(child);

                stmt = db.getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);
                try {
                    stmt.bindf("is", wcId, SVNFileUtil.createFilePath(localRelpath, name));
                    boolean haveRow = stmt.next();
                    if (haveRow && !stmt.isColumnNull(NODES__Fields.moved_to)) {
                        movedToRelPaths.put(name, stmt.getColumnString(NODES__Fields.moved_to));
                    }
                } finally {
                    stmt.reset();
                }
            }
        }

        stmt = db.getStatement(SVNWCDbStatements.INSERT_NODE);
        for (int i = children.size() - 1; i >= 0; i--) {
            File child = children.get(i);
            String name = SVNFileUtil.getFileName(child);

            try {
                stmt.bindf("isisnnrsnsnnnnnnnnnnsn", wcId, SVNFileUtil.createFilePath(localRelpath, name), opDepth, localRelpath, revision,
                        SvnWcDbStatementUtil.getPresenceText(SVNWCDbStatus.Incomplete),
                        SvnWcDbStatementUtil.getKindText(SVNWCDbKind.Unknown), movedToRelPaths.get(name));
                if (reposId != -1) {
                    stmt.bindLong(5, reposId);
                    stmt.bindString(6, SVNFileUtil.getFilePath(SVNFileUtil.createFilePath(reposPath, name)));
                }

                stmt.done();
            } finally {
                stmt.reset();
            }
        }

//        for (File name : children) {
//            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.INSERT_NODE);
//            try {
//                stmt.bindf("isisnnrsns", wcId, SVNFileUtil.createFilePath(localRelpath, name), opDepth, localRelpath, revision, "incomplete", "unknown");
//                stmt.done();
//            } finally {
//                stmt.reset();
//            }
//        }
    }

    public void extendParentDelete(SVNSqlJetDb db, long wcId, File localRelPath, SVNNodeKind kind, int opDepth) throws SVNException {
        long parentOpDepth = 0;
        SVNSqlJetStatement stmt;
        File parentRelPath = SVNFileUtil.getFileDir(localRelPath);

        stmt = db.getStatement(SVNWCDbStatements.SELECT_LOWEST_WORKING_NODE);
        try {
            stmt.bindf("isi", wcId, parentRelPath, opDepth);
            boolean haveRow = stmt.next();

            if (haveRow) {
                parentOpDepth = stmt.getColumnLong(NODES__Fields.op_depth);
            }
            stmt.reset();
            if (haveRow) {
                long existingOpDepth = 0;
                stmt.bindf("isi", wcId, localRelPath, opDepth);
                haveRow = stmt.next();
                if (haveRow) {
                    existingOpDepth = stmt.getColumnLong(NODES__Fields.op_depth);
                }
                stmt.reset();
                if (!haveRow || parentOpDepth < existingOpDepth) {
                    stmt = db.getStatement(SVNWCDbStatements.INSTALL_WORKING_NODE_FOR_DELETE);
                    stmt.bindf("isist", wcId, localRelPath, parentOpDepth, parentRelPath, kind);
                    stmt.done();
                }
            }
        } finally {
            stmt.reset();
        }
    }

    public void retractParentDelete(SVNSqlJetDb db, long wcId, File localRelPath, int opDepth) throws SVNException {
        SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_LOWEST_WORKING_NODE);
        try {
            stmt.bindf("isi", wcId, localRelPath, opDepth);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public void addBaseFile(File localAbspath, File reposRelpath, SVNURL reposRootUrl, String reposUuid, long revision, SVNProperties props, long changedRev, SVNDate changedDate,
            String changedAuthor, SvnChecksum checksum, SVNProperties davCache, boolean deleteWorking, boolean updateActualProps, SVNProperties actualProps,
            boolean keepRecordedInfo, boolean insertBaseDeleted, Map<String, SVNProperties> iprops, SVNSkel conflict, SVNSkel workItems) throws SVNException {

        assert (SVNFileUtil.isAbsolute(localAbspath));
        assert (reposRelpath != null);
        assert (reposUuid != null);
        assert (SVNRevision.isValidRevisionNumber(revision));
        assert (props != null);
        assert (SVNRevision.isValidRevisionNumber(changedRev));
        assert (checksum != null);

        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);


        InsertBase ibb = new InsertBase();
        ibb.reposRootURL = reposRootUrl;
        ibb.reposUUID = reposUuid;

        ibb.status = SVNWCDbStatus.Normal;
        ibb.kind = SVNWCDbKind.File;
        ibb.reposRelpath = reposRelpath;
        ibb.revision = revision;

        ibb.props = props;
        ibb.changedRev = changedRev;
        ibb.changedDate = changedDate;
        ibb.changedAuthor = changedAuthor;

        ibb.checksum = checksum;

        ibb.davCache = davCache;
        ibb.conflict = conflict;
        ibb.workItems = workItems;
        ibb.iprops = iprops;

        if (updateActualProps) {
            ibb.updateActualProps = true;
            ibb.actualProps = actualProps;
        }
        ibb.keepRecordedInfo = keepRecordedInfo;
        ibb.insertBaseDeleted = insertBaseDeleted;
        ibb.deleteWorking = deleteWorking;

        ibb.localRelpath = localRelpath;
        ibb.wcId = pdh.getWCRoot().getWcId();
        ibb.wcRoot = pdh.getWCRoot();

        pdh.getWCRoot().getSDb().runTransaction(ibb);
        pdh.flushEntries(localAbspath);
    }

    public void addBaseSymlink(File localAbsPath, File reposRelPath, SVNURL reposRootUrl, String reposUuid, long revision, SVNProperties props, long changedRev, SVNDate changedDate,
            String changedAuthor, File target, SVNProperties davCache, SVNSkel conflict, boolean updateActualProps, SVNProperties acutalProps, SVNSkel workItems) throws SVNException {

        assert (SVNFileUtil.isAbsolute(localAbsPath));
        assert (reposRelPath != null);
        assert (reposUuid != null);
        assert (SVNRevision.isValidRevisionNumber(revision));
        assert (props != null);
        assert (SVNRevision.isValidRevisionNumber(changedRev));
        assert (target != null);

        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        InsertBase ibb = new InsertBase();
        ibb.reposUUID = reposUuid;
        ibb.reposRootURL = reposRootUrl;

        ibb.status = SVNWCDbStatus.Normal;
        ibb.kind = SVNWCDbKind.Symlink;
        ibb.reposRelpath = reposRelPath;
        ibb.revision = revision;

        ibb.props = props;
        ibb.changedRev = changedRev;
        ibb.changedDate = changedDate;
        ibb.changedAuthor = changedAuthor;

        ibb.target = target;

        ibb.davCache = davCache;
        ibb.conflict = conflict;
        ibb.workItems = workItems;

        if (updateActualProps) {
            ibb.updateActualProps = true;
            ibb.actualProps = acutalProps;
        }

        ibb.wcId = pdh.getWCRoot().getWcId();
        ibb.wcRoot = pdh.getWCRoot();
        ibb.localRelpath = localRelpath;
        pdh.getWCRoot().getSDb().runTransaction(ibb);
        pdh.flushEntries(localAbsPath);
    }

    public void addLock(File localAbsPath, SVNWCDbLock lock) throws SVNException {
    	assert (isAbsolute(localAbsPath));
    	assert (lock != null);

        final DirParsedInfo dir = parseDir(localAbsPath, Mode.ReadOnly);
        final SVNWCDbDir pdh = dir.wcDbDir;

        verifyDirUsable(pdh);

        InsertLock ilb = new InsertLock();
        ilb.localAbsPath = localAbsPath;
        ilb.lock = lock;
        pdh.getWCRoot().getSDb().runTransaction(ilb);

    	pdh.flushEntries(localAbsPath);
    }

    public void addWorkQueue(File wcRootAbsPath, SVNSkel workItem) throws SVNException {
        assert (SVNFileUtil.isAbsolute(wcRootAbsPath));
        if (workItem == null) {
            return;
        }
        DirParsedInfo parseDir = parseDir(wcRootAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);
        addWorkItems(pdh.getWCRoot().getSDb(), workItem);
    }

    public boolean checkPristine(File wcRootAbsPath, SvnChecksum checksum) throws SVNException {
        assert (SVNFileUtil.isAbsolute(wcRootAbsPath));
        assert (checksum != null);
        if (checksum.getKind() != SvnChecksum.Kind.sha1) {
            //i.e. checksum has kind "md5"
            checksum = getPristineSHA1(wcRootAbsPath, checksum);
        }
        assert (checksum.getKind() == SvnChecksum.Kind.sha1);
        DirParsedInfo parseDir = parseDir(wcRootAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);
        return SvnWcDbPristines.checkPristine(pdh.getWCRoot(), checksum);
    }

    public void completedWorkQueue(File wcRootAbsPath, long id) throws SVNException {
        assert (SVNFileUtil.isAbsolute(wcRootAbsPath));
        assert (id != 0);
        DirParsedInfo parseDir = parseDir(wcRootAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.DELETE_WORK_ITEM);
        try {
            stmt.bindLong(1, id);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public WCDbWorkQueueInfo fetchWorkQueue(File wcRootAbsPath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(wcRootAbsPath));
        WCDbWorkQueueInfo info = new WCDbWorkQueueInfo();
        DirParsedInfo parseDir = parseDir(wcRootAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_WORK_ITEM);
        try {
            boolean haveRow = stmt.next();
            if (!haveRow) {
                info.id = 0;
                info.workItem = null;
                return info;
            }
            info.id = stmt.getColumnLong(SVNWCDbSchema.WORK_QUEUE__Fields.id);
            info.workItem = SVNSkel.parse(stmt.getColumnBlob(SVNWCDbSchema.WORK_QUEUE__Fields.work));
            return info;
        } finally {
            stmt.reset();
        }
    }

    public File fromRelPath(File wriAbsPath, File localRelPath) throws SVNException {
        DirParsedInfo parsed = parseDir(wriAbsPath, Mode.ReadOnly);
        File wcRootAbsPath = parsed.wcDbDir.getWCRoot().getAbsPath();
        return SVNFileUtil.createFilePath(wcRootAbsPath, localRelPath);
    }

    public Set<String> getBaseChildren(File localAbsPath) throws SVNException {
        return gatherChildren(localAbsPath, true, false);
    }

    public Set<String> getWorkingChildren(File localAbsPath) throws SVNException {
        return gatherChildren(localAbsPath, false, true);
    }

    public SVNProperties getBaseDavCache(File localAbsPath) throws SVNException {
        SVNSqlJetStatement stmt = getStatementForPath(localAbsPath, SVNWCDbStatements.SELECT_BASE_DAV_CACHE);
        try {
            boolean haveRow = stmt.next();
            if (!haveRow) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            return stmt.getColumnProperties(SVNWCDbSchema.NODES__Fields.dav_cache);
        } finally {
            stmt.reset();
        }
    }

    public void clearDavCacheRecursive(File localAbsPath) throws SVNException {
    	assert (isAbsolute(localAbsPath));
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        verifyDirUsable(parsed.wcDbDir);
        final SVNWCDbRoot root = parsed.wcDbDir.getWCRoot();
        SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.CLEAR_BASE_NODE_RECURSIVE_DAV_CACHE);
        try {
            stmt.bindf("is", root.getWcId(), SVNFileUtil.getFilePath(parsed.localRelPath));
            stmt.done();
        } finally {
            stmt.reset();
        }
    }


    public WCDbBaseInfo getBaseInfo(File localAbsPath, BaseInfoField... fields) throws SVNException {
        assert (isAbsolute(localAbsPath));
        final DirParsedInfo dir = parseDir(localAbsPath, Mode.ReadOnly);
        final SVNWCDbDir pdh = dir.wcDbDir;
        final File localRelPath = dir.localRelPath;

        verifyDirUsable(pdh);

        return getBaseInfo(dir.wcDbDir.getWCRoot(), localRelPath, fields);
    }

    public WCDbBaseInfo getBaseInfo(SVNWCDbRoot root, File localRelPath, BaseInfoField... fields) throws SVNException {

        final EnumSet<BaseInfoField> f = getInfoFields(BaseInfoField.class, fields);
        WCDbBaseInfo info = new WCDbBaseInfo();

        boolean have_row;

        SVNSqlJetStatement stmt = root.getSDb().getStatement(f.contains(BaseInfoField.lock) ? SVNWCDbStatements.SELECT_BASE_NODE_WITH_LOCK : SVNWCDbStatements.SELECT_BASE_NODE);
        try {
            stmt.bindf("is", root.getWcId(), SVNFileUtil.getFilePath(localRelPath));
            have_row = stmt.next();

            if (have_row) {
                SVNWCDbKind node_kind = getColumnKind(stmt, NODES__Fields.kind);

                if (f.contains(BaseInfoField.kind)) {
                    info.kind = node_kind;
                }
                if (f.contains(BaseInfoField.status)) {
                    info.status = getColumnPresence(stmt);
                }
                if (f.contains(BaseInfoField.revision)) {
                    info.revision = getColumnRevNum(stmt, SVNWCDbSchema.NODES__Fields.revision);
                }
                if (f.contains(BaseInfoField.reposRelPath)) {
                    info.reposRelPath = SVNFileUtil.createFilePath(getColumnText(stmt, SVNWCDbSchema.NODES__Fields.repos_path));
                }
                if (f.contains(BaseInfoField.lock)) {
                    final SVNSqlJetStatement lockStmt = stmt.getJoinedStatement(SVNWCDbSchema.LOCK);
                    if (isColumnNull(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_token)) {
                        info.lock = null;
                    } else {
                        info.lock = new SVNWCDbLock();
                        info.lock.token = getColumnText(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_token);
                        if (!isColumnNull(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_owner))
                            info.lock.owner = getColumnText(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_owner);
                        if (!isColumnNull(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_comment))
                            info.lock.comment = getColumnText(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_comment);
                        if (!isColumnNull(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_date))
                            info.lock.date = SVNWCUtils.readDate(getColumnInt64(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_date));
                    }
                }
                info.reposId = getColumnInt64(stmt, NODES__Fields.repos_id);

                if (f.contains(BaseInfoField.reposRootUrl) || f.contains(BaseInfoField.reposUuid)) {
                    /* Fetch repository information via REPOS_ID. */
                    if (isColumnNull(stmt, SVNWCDbSchema.NODES__Fields.repos_id)) {
                        if (f.contains(BaseInfoField.reposRootUrl))
                            info.reposRootUrl = null;
                        if (f.contains(BaseInfoField.reposUuid))
                            info.reposUuid = null;
                    } else {
                        final ReposInfo reposInfo = fetchReposInfo(root.getSDb(), getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.repos_id));
                        info.reposRootUrl = SVNURL.parseURIEncoded(reposInfo.reposRootUrl);
                        info.reposUuid = reposInfo.reposUuid;
                    }
                }
                if (f.contains(BaseInfoField.changedRev)) {
                    info.changedRev = getColumnRevNum(stmt, SVNWCDbSchema.NODES__Fields.changed_revision);
                }
                if (f.contains(BaseInfoField.changedDate)) {
                    info.changedDate = SVNWCUtils.readDate(getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.changed_date));
                }
                if (f.contains(BaseInfoField.changedAuthor)) {
                    /* Result may be NULL. */
                    info.changedAuthor = getColumnText(stmt, SVNWCDbSchema.NODES__Fields.changed_author);
                }
                if (f.contains(BaseInfoField.lastModTime)) {
                    info.lastModTime = SVNWCUtils.readDate(getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.last_mod_time));
                }
                if (f.contains(BaseInfoField.depth)) {
                    if (node_kind != SVNWCDbKind.Dir) {
                        info.depth = SVNDepth.UNKNOWN;
                    } else {
                        String depth_str = getColumnText(stmt, SVNWCDbSchema.NODES__Fields.depth);

                        if (depth_str == null)
                            info.depth = SVNDepth.UNKNOWN;
                        else
                            info.depth = parseDepth(depth_str);
                    }
                }
                if (f.contains(BaseInfoField.checksum)) {
                    if (node_kind != SVNWCDbKind.File) {
                        info.checksum = null;
                    } else {
                        try {
                            info.checksum = getColumnChecksum(stmt, SVNWCDbSchema.NODES__Fields.checksum);
                        } catch (SVNException e) {
                            SVNErrorMessage err = SVNErrorMessage.create(e.getErrorMessage().getErrorCode(), "The node ''{0}'' has a corrupt checksum value.", root.getAbsPath(localRelPath));
                            SVNErrorManager.error(err, SVNLogType.WC);
                        }
                    }
                }
                if (f.contains(BaseInfoField.translatedSize)) {
                    info.translatedSize = getTranslatedSize(stmt, SVNWCDbSchema.NODES__Fields.translated_size);
                }
                if (f.contains(BaseInfoField.target)) {
                    if (node_kind != SVNWCDbKind.Symlink)
                        info.target = null;
                    else
                        info.target = SVNFileUtil.createFilePath(getColumnText(stmt, SVNWCDbSchema.NODES__Fields.symlink_target));
                }
                if (f.contains(BaseInfoField.hadProps)) {
                    info.hadProps = !stmt.isColumnNull(NODES__Fields.properties) && stmt.getColumnBlob(NODES__Fields.properties).length > 2;
                }
                if (f.contains(BaseInfoField.props)) {
                    SVNWCDbStatus nodeStatus = getColumnPresence(stmt);
                    SVNProperties properties;
                    if (nodeStatus == SVNWCDbStatus.Normal || nodeStatus == SVNWCDbStatus.Incomplete) {
                        properties = stmt.getColumnProperties(NODES__Fields.properties);
                        if (properties == null) {
                            properties = new SVNProperties();
                        }
                    } else {
                        assert stmt.isColumnNull(NODES__Fields.properties);
                        properties = null;
                    }
                    info.props = properties;
                }
                if (f.contains(BaseInfoField.updateRoot)) {
                    info.updateRoot = getColumnBoolean(stmt, SVNWCDbSchema.NODES__Fields.file_external);
                }
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", root.getAbsPath(localRelPath));
                SVNErrorManager.error(err, SVNLogType.WC);
            }

        } finally {
            stmt.reset();
        }

        return info;

    }

    public SVNProperties getBaseProps(File localAbsPath) throws SVNException {
        SVNSqlJetStatement stmt = getStatementForPath(localAbsPath, SVNWCDbStatements.SELECT_BASE_PROPS);
        try {
            boolean have_row = stmt.next();
            if (!have_row) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}''  was not found.", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            SVNProperties props = getColumnProperties(stmt, NODES__Fields.properties);
            if (props == null) {
                /*
                 * ### is this a DB constraint violation? the column "probably"
                 * should ### never be null.
                 */
                return new SVNProperties();
            }
            return props;
        } finally {
            stmt.reset();
        }
    }

    public int getFormatTemp(File localDirAbsPath) throws SVNException {
        assert (isAbsolute(localDirAbsPath));
        SVNWCDbDir pdh = getOrCreateDir(localDirAbsPath, false);
        if (pdh == null || pdh.getWCRoot() == null) {
            try {
                final DirParsedInfo parsed = parseDir(localDirAbsPath, Mode.ReadOnly);
                pdh = parsed.wcDbDir;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                    throw e;
                }
                if (pdh != null) {
                    pdh.setWCRoot(null);
                }
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_MISSING, "Path ''{0}'' is not a working copy", localDirAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            assert (pdh.getWCRoot() != null);
        }
        assert (pdh.getWCRoot().getFormat() >= 1);
        return pdh.getWCRoot().getFormat();
    }

    public SvnChecksum getPristineMD5(File wcRootAbsPath, SvnChecksum sha1Checksum) throws SVNException {
        assert (isAbsolute(wcRootAbsPath));
        assert (sha1Checksum != null);
        assert (sha1Checksum.getKind() == SvnChecksum.Kind.sha1);

        final DirParsedInfo parsed = parseDir(wcRootAbsPath, Mode.ReadOnly);

        final SVNWCDbDir pdh = parsed.wcDbDir;

        verifyDirUsable(pdh);

        final SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_PRISTINE_MD5_CHECKSUM);

        try {
            stmt.bindChecksum(1, sha1Checksum);
            boolean have_row = stmt.next();
            if (!have_row) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_DB_ERROR, "The pristine text with checksum ''{0}'' not found", sha1Checksum.toString());
                SVNErrorManager.error(err, SVNLogType.WC);
                return null;
            }
            final SvnChecksum md5Checksum = getColumnChecksum(stmt, PRISTINE__Fields.md5_checksum);
            assert (md5Checksum.getKind() == SvnChecksum.Kind.md5);
            return md5Checksum;
        } finally {
            stmt.reset();
        }
    }

    public File getPristinePath(File wcRootAbsPath, SvnChecksum checksum) throws SVNException {
        assert (SVNFileUtil.isAbsolute(wcRootAbsPath));
        assert (checksum != null);
        if (checksum.getKind() != SvnChecksum.Kind.sha1) {
            //i.e. checksum has kind "md5"
            checksum = getPristineSHA1(wcRootAbsPath, checksum);
        }
        assert (checksum.getKind() == SvnChecksum.Kind.sha1);
        final DirParsedInfo parsed = parseDir(wcRootAbsPath, Mode.ReadOnly);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        return SvnWcDbPristines.getPristinePath(pdh.getWCRoot(), checksum);
    }

    public SvnChecksum getPristineSHA1(File wcRootAbsPath, SvnChecksum md5Checksum) throws SVNException {
        assert (isAbsolute(wcRootAbsPath));
        assert (md5Checksum.getKind() == SvnChecksum.Kind.md5);

        final DirParsedInfo parsed = parseDir(wcRootAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        return SvnWcDbPristines.getPristineSHA1(pdh.getWCRoot(), md5Checksum);
    }

    public File getPristineTempDir(File wcRootAbsPath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(wcRootAbsPath));
        final DirParsedInfo parsed = parseDir(wcRootAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        return SvnWcDbPristines.getPristineTempDir(pdh.getWCRoot(), wcRootAbsPath);
    }

    public void globalRecordFileinfo(File localAbspath, long translatedSize, long lastModTime) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        final DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        RecordFileinfo rb = new RecordFileinfo();
        rb.wcRoot = pdh.getWCRoot();
        rb.localRelpath = parsed.localRelPath;
        rb.translatedSize = translatedSize;
        rb.lastModTime = lastModTime;
        pdh.getWCRoot().getSDb().runTransaction(rb);
        pdh.flushEntries(localAbspath);
    }

    private class RecordFileinfo implements SVNSqlJetTransaction {

        public long lastModTime;
        public long translatedSize;
        public File localRelpath;
        public SVNWCDbRoot wcRoot;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            try {
                db.beginTransaction(SqlJetTransactionMode.WRITE);

                final ISqlJetTable table = db.getDb().getTable(SVNWCDbSchema.NODES.name());
                ISqlJetCursor c = table.lookup(null, wcRoot.getWcId(), SVNFileUtil.getFilePath(localRelpath));
                c = c.reverse();
                if (!c.eof()) {
                    final Map<String, Object> updateValues = new HashMap<String, Object>();
                    updateValues.put(SVNWCDbSchema.NODES__Fields.translated_size.toString(), translatedSize);
                    updateValues.put(SVNWCDbSchema.NODES__Fields.last_mod_time.toString(), lastModTime);
                    c.updateByFieldNames(updateValues);
                }
                c.close();
                db.commit();
            } catch (SqlJetException e) {
                db.rollback();
                throw e;
            } catch (SVNException e) {
                db.rollback();
                throw e;
            }
        }
    }

    public void installPristine(File tempfileAbspath, SvnChecksum sha1Checksum, SvnChecksum md5Checksum) throws SVNException {
        assert (SVNFileUtil.isAbsolute(tempfileAbspath));
        File wriAbspath = SVNFileUtil.getParentFile(tempfileAbspath);
        final DirParsedInfo parsed = parseDir(wriAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        SvnWcDbPristines.installPristine(pdh.getWCRoot(), tempfileAbspath, sha1Checksum, md5Checksum);
    }

    public boolean isNodeHidden(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));

        /*
         * This uses an optimisation that first reads the working node and then
         * may read the base node. It could call svn_wc__db_read_info but that
         * would always read both nodes.
         */
        final DirParsedInfo parsedInfo = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsedInfo.wcDbDir;
        File localRelPath = parsedInfo.localRelPath;

        verifyDirUsable(pdh);

        /* First check the working node. */
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), SVNFileUtil.getFilePath(localRelPath));
            boolean have_row = stmt.next();
            if (have_row) {
                /*
                 * Note: this can ONLY be an add/copy-here/move-here. It is not
                 * possible to delete a "hidden" node.
                 */
                SVNWCDbStatus work_status = getColumnPresence(stmt);
                return (work_status == SVNWCDbStatus.Excluded);
            }
        } finally {
            stmt.reset();
        }

        /* Now check the BASE node's status. */
        final WCDbBaseInfo baseInfo = getBaseInfo(localAbsPath, BaseInfoField.status);
        SVNWCDbStatus base_status = baseInfo.status;
        return (base_status == SVNWCDbStatus.ServerExcluded || base_status == SVNWCDbStatus.NotPresent || base_status == SVNWCDbStatus.Excluded);
    }

    public static class DirParsedInfo {

        public SVNWCDbDir wcDbDir;
        public File localRelPath;
    }

    public DirParsedInfo parseDir(File localAbsPath, Mode sMode) throws SVNException {
        return parseDir(localAbsPath, sMode, false, false);
    }

    public DirParsedInfo parseDir(File localAbspath, Mode sMode, boolean isDetectWCGeneration, boolean isAdditionMode) throws SVNException {
        DirParsedInfo info = new DirParsedInfo();
        String buildRelPath;
        File localDirAbspath;
        File originalAbspath = localAbspath;
        SVNNodeKind kind;
        SVNWCDbDir probeRoot;
        SVNWCDbDir foundRoot = null;
        boolean movedUpwards = false;
        boolean alwaysCheck = false;
        boolean isSymlink;
        sMode = Mode.ReadWrite;
        SVNSqlJetDb sDb = null;
        int wc_format = 0;
        boolean isOldFormat = false;

        probeRoot = dirData.get(localAbspath.getAbsolutePath());
        SVNFileType fileType = null;
        if (probeRoot != null) {
            fileType = SVNFileType.getType(localAbspath);
            if (!isAdditionMode || fileType != SVNFileType.SYMLINK) {
                info.wcDbDir = probeRoot;
                info.localRelPath = probeRoot.computeRelPath();
                return info;
            }
        }

        if (fileType == null) {
            fileType = SVNFileType.getType(localAbspath);
        }
        isSymlink = fileType == SVNFileType.SYMLINK;
        kind = fileType == SVNFileType.DIRECTORY || (isSymlink && localAbspath.isDirectory()) ?
                SVNNodeKind.DIR : SVNFileType.getNodeKind(fileType);
        if (kind != SVNNodeKind.DIR || isSymlink) {
            buildRelPath = SVNFileUtil.getFileName(localAbspath);
            localDirAbspath = SVNFileUtil.getParentFile(localAbspath);

            if (localDirAbspath == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY, "''{0}'' is not a working copy", originalAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            probeRoot = dirData.get(localDirAbspath.getAbsolutePath());
            if (probeRoot != null) {
                if (!isSymlink || isAdditionMode) {
                    info.wcDbDir = probeRoot;
                    info.localRelPath = SVNFileUtil.createFilePath(info.wcDbDir.computeRelPath(), buildRelPath);
                    return info;
                } else {
                    //otherwise we shouldn't just use parent, but follow the symlink instead
                    foundRoot = probeRoot;
                }
            }

            if (kind == SVNNodeKind.NONE) {
                alwaysCheck = true;
            }

            localAbspath = localDirAbspath;
        } else {
            buildRelPath = "";
            localDirAbspath = localAbspath;
        }


        do { //workaround to emulate "goto"
            if (foundRoot == null) {
                while (true) {
                    try {
                        sDb = openDb(localAbspath, SDB_FILE, sMode, journalMode, temporaryDbInMemory);
                        break;
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.SQLITE_ERROR && !e.isEnoent()) {
                            throw e;
                        }
                    }

                    if (!movedUpwards || alwaysCheck || isDetectWCGeneration) {
                        wc_format = getOldVersion(localAbspath);
                        if (wc_format != 0) {
                            break;
                        }
                    }

                    if (SVNFileUtil.getParentFile(localAbspath) == null) { //if is root
                        if (isSymlink && !isAdditionMode) { //if we add a symlink, we never follow it
                            localAbspath = originalAbspath;
                            SVNNodeKind resolvedKind = SVNFileType.getNodeKind(SVNFileType.getType(SVNFileUtil.resolveSymlink(localAbspath)));

                            if (resolvedKind == SVNNodeKind.DIR) {
                                foundRoot = dirData.get(localAbspath.getAbsolutePath());
                                if (foundRoot != null) {
                                    break;
                                }

                                kind = SVNNodeKind.DIR;
                                isSymlink = false;
                                movedUpwards = false;
                                localDirAbspath = localAbspath;
                                buildRelPath = "";

                                continue;
                            }
                        }

                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY, "''{0}'' is not a working copy", originalAbspath);
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }

                    localAbspath = SVNFileUtil.getParentFile(localAbspath);
                    movedUpwards = true;

                    foundRoot = dirData.get(localAbspath.getAbsolutePath());
                    if (foundRoot != null) {
                        break;
                    }
                }
            }

            if (foundRoot != null) {
                info.wcDbDir = foundRoot;
            } else if (wc_format == 0) {
                long wcId = UNKNOWN_WC_ID;

                try {
                    wcId = fetchWCId(sDb);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_CORRUPT) {
                        SVNErrorMessage err = e.getErrorMessage().wrap("Missing a row in WCROOT for ''{0}''.", originalAbspath);
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }
                }

                info.wcDbDir = new SVNWCDbDir(localAbspath);
                final boolean allowWc17Access = isWC17AccessEnabled() || isDetectWCGeneration;
                info.wcDbDir.setWCRoot(new SVNWCDbRoot(this, localAbspath, sDb, wcId, FORMAT_FROM_SDB, autoUpgrade, !allowWc17Access, enforceEmptyWQ));

            } else {
                info.wcDbDir = new SVNWCDbDir(localAbspath);
                info.wcDbDir.setWCRoot(new SVNWCDbRoot(this, localAbspath, null, UNKNOWN_WC_ID, wc_format, autoUpgrade, false, enforceEmptyWQ));

                isOldFormat = true;
            }

            String dirRelPath = SVNPathUtil.getRelativePath(info.wcDbDir.getWCRoot().getAbsPath().getAbsolutePath(), localDirAbspath.getAbsolutePath());
            info.localRelPath = SVNFileUtil.createFilePath(dirRelPath, buildRelPath);

            if (isSymlink && !isAdditionMode) {

                SVNWCDbStatus status;
                boolean conflicted;
                boolean retryIfDir = false;
                if (isOldFormat) {
                    SVNAdminArea area = null;
                    try {
                        area = SVNWCAccess.newInstance(null).open(localDirAbspath, false, false, 0);
                        retryIfDir = area.getEntry(SVNFileUtil.getFileName(originalAbspath), false) == null;
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND &&
                                e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_UPGRADE_REQUIRED &&
                                e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_DIRECTORY &&
                                e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                            throw e;
                        }
                        retryIfDir = true;
                    } finally {
                        if (area != null) {
                            area.close();
                        }
                    }
                } else {
                    try {
                        WCDbInfo wcDbInfo = readInfo(info.wcDbDir.getWCRoot(), info.localRelPath, InfoField.status, InfoField.conflicted);
                        status = wcDbInfo.status;
                        conflicted = wcDbInfo.conflicted;
                        retryIfDir = (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded) && !conflicted;
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND &&
                                e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_UPGRADE_REQUIRED &&
                                e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                            throw e;
                        }

                        retryIfDir = true;
                    }
                }

                if (retryIfDir) {
                    SVNNodeKind resolvedKind = SVNFileType.getNodeKind(SVNFileType.getType(SVNFileUtil.resolveSymlink(localAbspath)));
                    if (resolvedKind == SVNNodeKind.DIR) {
                        localAbspath = originalAbspath;

                        //goto emulation using do{}while() + continue
                        kind = SVNNodeKind.DIR;
                        isSymlink = false;
                        movedUpwards = false;
                        localDirAbspath = localAbspath;
                        buildRelPath = "";
                        foundRoot = null;
                        wc_format = 0; //reset wc_format

                        continue;
                    }
                }
            }
            break;
        } while (true);

        if (!isAdditionMode || !isSymlink) { //we shouldn't put the resulting root to the cache
            SVNWCDbDir wcDbDir = new SVNWCDbDir(localDirAbspath);
            wcDbDir.setWCRoot(info.wcDbDir.getWCRoot());
            dirData.put(wcDbDir.getLocalAbsPath().getAbsolutePath(), wcDbDir);

            if (!movedUpwards) {
                return info;
            }

            File scanAbspath = localDirAbspath;

            do {
                File parentDir = SVNFileUtil.getParentFile(scanAbspath);
                SVNWCDbDir parentRoot;

                parentRoot = dirData.get(parentDir.getAbsolutePath());

                if (parentRoot == null) {
                    SVNWCDbDir parentWcDbDir = new SVNWCDbDir(parentDir);
                    parentWcDbDir.setWCRoot(info.wcDbDir.getWCRoot());
                    dirData.put(parentWcDbDir.getLocalAbsPath().getAbsolutePath(), parentWcDbDir);
                }

                scanAbspath = parentDir;
            } while (!localAbspath.equals(scanAbspath));
        }
        return info;
    }

    private int getOldVersion(File localAbsPath) {
        if (localAbsPath == null) {
            return 0;
        }
        try {
            int formatVersion = 0;
            File adminDir = new File(localAbsPath, SVNFileUtil.getAdminDirectoryName());
            File entriesFile = new File(adminDir, "entries");
            if (!entriesFile.exists()) {
                return 0;
            }

            try {
                formatVersion = readFormatVersion(entriesFile);
                return formatVersion;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.BAD_VERSION_FILE_FORMAT &&
                        (e.getErrorMessage().getChildErrorMessage() == null || e.getErrorMessage().getChildErrorMessage().getErrorCode() != SVNErrorCode.BAD_VERSION_FILE_FORMAT)) {
                    throw e;
                }
            }
            File formatFile = new File(adminDir, "format");
            if (formatFile.exists()) {
                formatVersion = readFormatVersion(formatFile);
            }
            return formatVersion;
        } catch (SVNException e) {
            return 0;
        }
    }

    private int readFormatVersion(File path) throws SVNException {
        int formatVersion = -1;
        BufferedReader reader = null;
        String line = null;
        try {
            reader = new BufferedReader(new InputStreamReader(SVNFileUtil.openFileForReading(path, Level.FINEST, SVNLogType.WC), "UTF-8"));
            line = reader.readLine();
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read entries file ''{0}'': {1}", new Object[] {
                    path, e.getLocalizedMessage()
            });
            SVNErrorManager.error(err, e, SVNLogType.WC);
        } catch (SVNException svne) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "''{0}'' is not a working copy", path);
            err.setChildErrorMessage(svne.getErrorMessage());
            SVNErrorManager.error(err, svne, Level.FINEST, SVNLogType.WC);
        } finally {
            SVNFileUtil.closeFile(reader);
        }
        if (line == null || line.length() == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.STREAM_UNEXPECTED_EOF, "Reading ''{0}''", path);
            SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "''{0}'' is not a working copy", path);
            err1.setChildErrorMessage(err);
            SVNErrorManager.error(err1, Level.FINEST, SVNLogType.WC);
        }
        try {
            formatVersion = Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_VERSION_FILE_FORMAT, "First line of ''{0}'' contains non-digit", path);
            SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "''{0}'' is not a working copy", path);
            err1.setChildErrorMessage(err);
            SVNErrorManager.error(err1, Level.FINEST, SVNLogType.WC);
        }
        return formatVersion;
    }

    public boolean isWCLocked(File localAbspath) throws SVNException {
        return isWCLocked(localAbspath, 0);
    }

    private boolean isWCLocked(File localAbspath, long recurseDepth) throws SVNException {
        final SVNSqlJetStatement stmt = getStatementForPath(localAbspath, SVNWCDbStatements.SELECT_WC_LOCK);
        try {
            boolean have_row = stmt.next();
            if (have_row) {
                long locked_levels = getColumnInt64(stmt, WC_LOCK__Fields.locked_levels);
                /*
                 * The directory in question is considered locked if we find a
                 * lock with depth -1 or the depth of the lock is greater than
                 * or equal to the depth we've recursed.
                 */
                return (locked_levels == -1 || locked_levels >= recurseDepth);
            }
        } finally {
            stmt.reset();
        }
        final File parentFile = SVNFileUtil.getParentFile(localAbspath);
        if (parentFile == null) {
            return false;
        }
        try {
            return isWCLocked(parentFile, recurseDepth + 1);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_WORKING_COPY) {
                return false;
            }
        }
        return false;
    }

    private boolean isWCLocked(SVNWCDbRoot root, File localRelpath, long recurseDepth) throws SVNException {
        final SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_ANCESTORS_WC_LOCKS);
        final int pathDepth = SVNWCUtils.relpathDepth(localRelpath);
        stmt.bindf("is", root.getWcId(), localRelpath);
        try {
            while(stmt.next()) {
                File lockedPath = getColumnPath(stmt, WC_LOCK__Fields.local_dir_relpath);
                if (SVNWCUtils.isAncestor(lockedPath, localRelpath)) {
                    long locked_levels = getColumnInt64(stmt, WC_LOCK__Fields.locked_levels);
                    int lockedPathDepth = SVNWCUtils.relpathDepth(lockedPath);
                    return (locked_levels == -1 || locked_levels + lockedPathDepth >= pathDepth);
                }
            }
        } finally {
            stmt.reset();
        }
        return false;
    }

    public SVNSqlJetDb getSDb(File localAbsPath) throws SVNException {
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);

        return pdh.getWCRoot().getSDb();
    }

    public void opAddDirectory(File localAbsPath, SVNProperties props, SVNSkel workItems) throws SVNException {
        File dirAbsPath = SVNFileUtil.getParentFile(localAbsPath);
        String name = SVNFileUtil.getFileName(localAbsPath);

        DirParsedInfo parseDir = parseDir(dirAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = SVNFileUtil.createFilePath(parseDir.localRelPath, name);
        verifyDirUsable(pdh);

        InsertWorking ibw = new InsertWorking();
        ibw.status = SVNWCDbStatus.Normal;
        ibw.kind = SVNWCDbKind.Dir;
        ibw.opDepth = SVNWCUtils.relpathDepth(localRelpath);
        ibw.localRelpath = localRelpath;
        ibw.wcRoot = pdh.getWCRoot();
        if (props != null && props.size() > 0) {
            ibw.updateActualProps = true;
            ibw.newActualProps = props;
        }
        ibw.workItems = workItems;

        pdh.getWCRoot().getSDb().runTransaction(ibw);
        pdh.flushEntries(localAbsPath);
    }

    public void opAddFile(File localAbsPath, SVNProperties props, SVNSkel workItems) throws SVNException {
        File dirAbsPath = SVNFileUtil.getParentFile(localAbsPath);
        String name = SVNFileUtil.getFileName(localAbsPath);

        DirParsedInfo parseDir = parseDir(dirAbsPath, Mode.ReadWrite, false, true);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = SVNFileUtil.createFilePath(parseDir.localRelPath, name);
        verifyDirUsable(pdh);

        InsertWorking ibw = new InsertWorking();
        ibw.status = SVNWCDbStatus.Normal;
        ibw.kind = SVNWCDbKind.File;
        ibw.opDepth = SVNWCUtils.relpathDepth(localRelpath);
        ibw.localRelpath = localRelpath;
        ibw.wcRoot = pdh.getWCRoot();
        if (props != null && props.size() > 0) {
            ibw.updateActualProps = true;
            ibw.newActualProps = props;
        }
        ibw.workItems = workItems;

        pdh.getWCRoot().getSDb().runTransaction(ibw);
        pdh.flushEntries(localAbsPath);
    }

    public void opAddSymlink(File localAbsPath, File target, SVNProperties props, SVNSkel workItems) throws SVNException {
        assert isAbsolute(localAbsPath);
        assert target != null;

        File dirAbsPath = SVNFileUtil.getParentFile(localAbsPath);
        String name = SVNFileUtil.getFileName(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        File localRelPath = SVNFileUtil.createFilePath(parsed.localRelPath, name);

        InsertWorking iw = new InsertWorking();
        iw.status = SVNWCDbStatus.Normal;
        iw.kind = SVNWCDbKind.Symlink;
        iw.opDepth = SVNWCUtils.relpathDepth(localRelPath);
        if (props != null && props.size() > 0) {
            iw.updateActualProps = true;
            iw.newActualProps = props;
        }
        iw.target = target;
        iw.workItems = workItems;

        pdh.getWCRoot().getSDb().runTransaction(iw);
        pdh.flushEntries(localAbsPath);
    }

    public void opCopy(File srcAbsPath, File dstAbsPath, File dstOpRootAbsPath, boolean isMove, SVNSkel workItems) throws SVNException {
        assert isAbsolute(srcAbsPath);
        assert isAbsolute(dstAbsPath);
        assert isAbsolute(dstOpRootAbsPath);

        DirParsedInfo parseSrcDir = parseDir(srcAbsPath, Mode.ReadWrite);
        SVNWCDbDir srcPdh = parseSrcDir.wcDbDir;
        File localSrcRelpath = parseSrcDir.localRelPath;
        verifyDirUsable(srcPdh);

        DirParsedInfo parseDstDir = parseDir(dstAbsPath, Mode.ReadWrite);
        SVNWCDbDir dstPdh = parseDstDir.wcDbDir;
        File localDstRelpath = parseDstDir.localRelPath;
        verifyDirUsable(dstPdh);

        File dstOpRootRelPath = SVNFileUtil.skipAncestor(dstPdh.getWCRoot().getAbsPath(), dstOpRootAbsPath);

        SvnWcDbCopy.copy(srcPdh, localSrcRelpath, dstPdh, localDstRelpath, dstOpRootRelPath, isMove, workItems);
    }

    public void opCopyShadowedLayer(File srcAbsPath, File dstAbsPath, boolean isMove) throws SVNException {
        DirParsedInfo parseSrcDir = parseDir(srcAbsPath, Mode.ReadWrite);
        SVNWCDbDir srcPdh = parseSrcDir.wcDbDir;
        File localSrcRelpath = parseSrcDir.localRelPath;
        verifyDirUsable(srcPdh);

        DirParsedInfo parseDstDir = parseDir(dstAbsPath, Mode.ReadWrite);
        SVNWCDbDir dstPdh = parseDstDir.wcDbDir;
        File localDstRelpath = parseDstDir.localRelPath;
        verifyDirUsable(dstPdh);

        SvnWcDbCopy.copyShadowedLayer(srcPdh, localSrcRelpath, dstPdh, localDstRelpath, isMove);
    }

    public void opCopyDir(File localAbsPath, SVNProperties props, long changedRev, SVNDate changedDate, String changedAuthor, File originalReposRelPath, SVNURL originalRootUrl, String originalUuid,
            long originalRevision, List<File> children, boolean isMove, SVNDepth depth, SVNSkel conflict, SVNSkel workItems) throws SVNException {
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        SvnWcDbCopy.copyDir(pdh, localRelpath,
                props, changedRev, changedDate, changedAuthor, originalReposRelPath, originalRootUrl,
                originalUuid, originalRevision, children, isMove, depth, conflict, workItems);
    }

    public void opCopyFile(File localAbsPath, SVNProperties props, long changedRev, SVNDate changedDate, String changedAuthor, File originalReposRelPath, SVNURL originalRootUrl, String originalUuid,
            long originalRevision, SvnChecksum checksum, boolean updateActualProps, SVNProperties newActualProps, SVNSkel conflict, SVNSkel workItems) throws SVNException {
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        SvnWcDbCopy.copyFile(pdh, localRelpath,
                props, changedRev, changedDate, changedAuthor, originalReposRelPath, originalRootUrl,
                originalUuid, originalRevision, checksum, updateActualProps, newActualProps, conflict, workItems);
    }

    public void opDelete(File localAbsPath, File movedToAbsPath, boolean deleteDirExternals, SVNSkel conflict, SVNSkel workItems, ISVNEventHandler eventHandler) throws SVNException {
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        File movedToRelPath;
        if (movedToAbsPath != null) {
            DirParsedInfo parsedMovedTo = parseDir(movedToAbsPath, Mode.ReadOnly);
            verifyDirUsable(parsedMovedTo.wcDbDir);
            if (!pdh.getWCRoot().getAbsPath().equals(parsedMovedTo.wcDbDir.getWCRoot().getAbsPath())) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                        "Cannot move ''{0}'' to ''{1}'' because they are not in the same working copy", localAbsPath, movedToAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            movedToRelPath = parsedMovedTo.localRelPath;
        } else {
            movedToRelPath = null;
        }

        Delete deleteTxn = new Delete();
        deleteTxn.root = pdh.getWCRoot();
        deleteTxn.localRelPath = localRelpath;
        deleteTxn.deleteDepth = SVNWCUtils.relpathDepth(localRelpath);
        deleteTxn.eventHandler = eventHandler;
        deleteTxn.movedToRelPath = movedToRelPath;
        deleteTxn.deleteDirExternals = deleteDirExternals;
        deleteTxn.conflict = conflict;
        deleteTxn.workItems = workItems;

        pdh.flushEntries(localAbsPath);
        pdh.getWCRoot().getSDb().beginTransaction(SqlJetTransactionMode.WRITE);
        try {
            try {
                deleteTxn.transaction(pdh.getWCRoot().getSDb());
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
            }
            if (eventHandler != null && pdh.getWCRoot().getSDb().getTemporaryDb().hasTable(SVNWCDbSchema.DELETE_LIST.toString())) {
                SVNSqlJetStatement selectDeleteList = new SVNSqlJetSelectStatement(pdh.getWCRoot().getSDb().getTemporaryDb(), SVNWCDbSchema.DELETE_LIST);
                try {
                    while(selectDeleteList.next()) {
                        File path = getColumnPath(selectDeleteList, DELETE_LIST__Fields.local_relpath);
                        path = pdh.getWCRoot().getAbsPath(path);
                        eventHandler.handleEvent(SVNEventFactory.createSVNEvent(path, SVNNodeKind.NONE, null, -1, SVNEventAction.DELETE,
                                SVNEventAction.DELETE, null, null, 1, 1), -1);
                    }
                } finally {
                    selectDeleteList.reset();
                }
                SVNSqlJetStatement dropList = new SVNWCDbCreateSchema(pdh.getWCRoot().getSDb().getTemporaryDb(), SVNWCDbCreateSchema.DROP_DELETE_LIST, -1, false);
                try {
                    dropList.done();
                } finally {
                    dropList.reset();
                }
            }
        } catch (SVNException e) {
            pdh.getWCRoot().getSDb().rollback();
            throw e;
        } finally {
            pdh.getWCRoot().getSDb().commit();
        }
    }

    public void opMarkResolved(File localAbspath, boolean resolvedText, boolean resolvedProps, boolean resolvedTree, SVNSkel workItems) throws SVNException {
        assert (isAbsolute(localAbspath));

        final DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        SVNSkel conflicts;

        SVNWCDbRoot wcRoot = pdh.getWCRoot();
        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelpath);
            boolean haveRow = stmt.next();
            if (!haveRow) {
                stmt.reset();
                stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
                stmt.bindf("is", wcRoot.getWcId(), localRelpath);
                haveRow = stmt.next();
                if (haveRow) {
                    return;
                }
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND,
                        "The node '{0}' was not found.", SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelpath));
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            if (wcRoot.getFormat() == ISVNWCDb.WC_FORMAT_17) {
                String conflictOld = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_old);
                String conflictWorking = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_working);
                String conflictNew = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_working);
                String propReject = stmt.getColumnString(ACTUAL_NODE__Fields.prop_reject);
                byte[] treeConflictData = stmt.getColumnBlob(ACTUAL_NODE__Fields.tree_conflict_data);

                conflicts = SvnWcDbConflicts.convertToConflictSkel(wcRoot.getAbsPath(), wcRoot.getDb(), SVNFileUtil.getFilePath(localRelpath), conflictOld, conflictWorking, conflictNew, propReject, treeConflictData);
            } else {
                conflicts = SVNSkel.parse(stmt.getColumnBlob(ACTUAL_NODE__Fields.conflict_data));
            }

        } finally {
            stmt.reset();
        }

        boolean resolvedAll = SvnWcDbConflicts.conflictSkelResolve(conflicts, this, wcRoot.getAbsPath(), resolvedText, resolvedProps ? "" : null, resolvedTree);

        long updatedRows = 0;
        if (wcRoot.getFormat() == ISVNWCDb.WC_FORMAT_17) {
            updatedRows = updateActualConflict17(wcRoot, localRelpath, conflicts, resolvedAll);
        } else {
            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_CONFLICT);
            try {
                stmt.bindf("is", wcRoot.getWcId(), localRelpath);
                if (!resolvedAll) {
                    stmt.bindBlob(3, conflicts.unparse());
                }
                updatedRows = stmt.done();
            } finally {
                stmt.reset();
            }
        }

        if (updatedRows > 0) {
            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.DELETE_ACTUAL_EMPTY);
            try {
                stmt.bindf("is", wcRoot.getWcId(), localRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }

        addWorkItems(wcRoot.getSDb(), workItems);
    }

    private long updateActualConflict17(SVNWCDbRoot wcRoot, File localRelpath, SVNSkel conflicts, boolean resolvedAll) throws SVNException {
        File wcRootAbsPath = wcRoot.getAbsPath();
        byte[] treeConflictData = null;
        File conflictOldRelPath = null;
        File conflictNewRelPath = null;
        File conflictWorkingRelPath = null;
        File prejRelPath = null;

        if (!resolvedAll && conflicts != null) {
            List<SVNWCConflictDescription17> conflictDescriptions = SvnWcDbConflicts.convertFromSkel(wcRoot.getDb(), SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelpath), false, conflicts);

            File conflictOldAbsPath = null;
            File conflictNewAbsPath = null;
            File conflictWorkingAbsPath = null;

            File prejAbsPath = null;

            for (SVNWCConflictDescription17 conflictDescription17 : conflictDescriptions) {
                SVNConflictDescription conflictDescription = conflictDescription17.toConflictDescription();

                if (conflictDescription instanceof SVNTextConflictDescription) {
                    SVNTextConflictDescription textConflictDescription = (SVNTextConflictDescription) conflictDescription;
                    SVNMergeFileSet mergeFiles = textConflictDescription.getMergeFiles();

                    conflictOldAbsPath = mergeFiles.getBaseFile();
                    conflictWorkingAbsPath = mergeFiles.getLocalFile();
                    conflictNewAbsPath = mergeFiles.getRepositoryFile();

                } else if (conflictDescription instanceof SVNPropertyConflictDescription) {
                    SVNPropertyConflictDescription propertyConflictDescription = (SVNPropertyConflictDescription) conflictDescription;

                    SVNMergeFileSet mergeFiles = propertyConflictDescription.getMergeFiles();
                    prejAbsPath = mergeFiles.getRepositoryFile();

                } else if (conflictDescription instanceof SVNTreeConflictDescription) {
                    SVNTreeConflictDescription treeConflictDescription = (SVNTreeConflictDescription) conflictDescription;

                    treeConflictData = SVNTreeConflictUtil.getSingleTreeConflictRawData(treeConflictDescription);
                }
            }

            conflictOldRelPath = conflictOldAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, conflictOldAbsPath);
            conflictNewRelPath = conflictNewAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, conflictNewAbsPath);
            conflictWorkingRelPath = conflictWorkingAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, conflictWorkingAbsPath);
            prejRelPath = prejAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, prejAbsPath);
        }

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_CONFLICT_DATA_17);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelpath);
            if (!resolvedAll) {
                stmt.bindString(3, SVNFileUtil.getFilePath(conflictOldRelPath));
                stmt.bindString(4, SVNFileUtil.getFilePath(conflictNewRelPath));
                stmt.bindString(5, SVNFileUtil.getFilePath(conflictWorkingRelPath));
                stmt.bindString(6, SVNFileUtil.getFilePath(prejRelPath));
                stmt.bindBlob(7, treeConflictData);
            }
            return stmt.done();
        } finally {
            stmt.reset();
        }

    }

    public void opMarkConflict(File localAbspath, SVNSkel conflictSkel, SVNSkel workItems) throws SVNException {
        assert (isAbsolute(localAbspath));
        final DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelPath = parsed.localRelPath;
        verifyDirUsable(pdh);
        markConflictInternal(pdh.getWCRoot(), localRelPath, conflictSkel);
        if (workItems != null) {
            addWorkItems(pdh.getWCRoot().getSDb(), workItems);
        }
        flushEntries(pdh.getWCRoot(), localRelPath, SVNDepth.EMPTY);
    }

    public void markConflictInternal(SVNWCDbRoot wcRoot, File localRelPath, SVNSkel conflictSkel) throws SVNException {
        final boolean isComplete = SvnWcDbConflicts.isConflictSkelComplete(conflictSkel);
        assert isComplete;

        if (wcRoot.getFormat() == ISVNWCDb.WC_FORMAT_17) {
            markConflictInternal17(wcRoot, localRelPath, conflictSkel);
            return;
        }

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
        boolean gotRow;
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            gotRow = stmt.next();
        } finally {
            stmt.reset();
        }

        stmt = null;
        try {
            if (gotRow) {
                stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_CONFLICT_DATA);
                stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            } else {
                stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_ACTUAL_CONFLICT_DATA);
                stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                if (localRelPath.getPath().length() > 0) {
                   stmt.bindString(4, SVNPathUtil.removeTail(SVNFileUtil.getFilePath(localRelPath)));
                }
            }
            final byte[] conflictData = conflictSkel.unparse();
            stmt.bindBlob(3, conflictData);
            stmt.exec();
        } finally {
            if (stmt != null) {
                stmt.reset();
            }
        }
    }

    private void markConflictInternal17(SVNWCDbRoot wcRoot, File localRelPath, SVNSkel conflictSkel) throws SVNException {
        File wcRootAbsPath = wcRoot.getAbsPath();
        File localAbsPath = SVNFileUtil.createFilePath(wcRootAbsPath, localRelPath);

        Structure<ConflictInfo> conflictInfoStructure = SvnWcDbConflicts.readConflictInfo(conflictSkel);
        SVNOperation operation = conflictInfoStructure.get(ConflictInfo.conflictOperation);
        List<SVNConflictVersion> locations = conflictInfoStructure.get(ConflictInfo.locations);

        File conflictOldRelPath = null;
        File conflictNewRelPath = null;
        File conflictWorkingRelPath = null;
        File prejRelPath = null;
        byte[] treeConflictRawData = null;

        if (conflictInfoStructure.is(ConflictInfo.textConflicted)) {
            Structure<TextConflictInfo> textConflictInfoStructure = SvnWcDbConflicts.readTextConflict(wcRoot.getDb(), localAbsPath, conflictSkel);
            File mineAbsPath = textConflictInfoStructure.get(TextConflictInfo.mineAbsPath);
            File theirOldAbsPath = textConflictInfoStructure.get(TextConflictInfo.theirOldAbsPath);
            File theirAbsPath = textConflictInfoStructure.get(TextConflictInfo.theirAbsPath);

            conflictOldRelPath = theirOldAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, theirOldAbsPath);
            conflictNewRelPath = theirAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, theirAbsPath);
            conflictWorkingRelPath = mineAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, mineAbsPath);
        }
        if (conflictInfoStructure.is(ConflictInfo.propConflicted)) {
            Structure<SvnWcDbConflicts.PropertyConflictInfo> propertyConflictInfoStructure = SvnWcDbConflicts.readPropertyConflict(wcRoot.getDb(), localAbsPath, conflictSkel);
            File markerAbsPath = propertyConflictInfoStructure.get(SvnWcDbConflicts.PropertyConflictInfo.markerAbspath);

            prejRelPath = markerAbsPath == null ? null : SVNFileUtil.skipAncestor(wcRootAbsPath, markerAbsPath);
        }
        if (conflictInfoStructure.is(ConflictInfo.treeConflicted)) {
            Structure<TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(wcRoot.getDb(), localAbsPath, conflictSkel);
            SVNConflictReason reason = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.localChange);
            SVNConflictAction action = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.incomingChange);

            SVNConflictVersion sourceLeftVersion = locations.get(0);
            SVNConflictVersion sourceRightVersion = locations.get(1);
            SVNNodeKind nodeKind = SVNNodeKind.UNKNOWN;
            if (sourceRightVersion != null) {
                nodeKind = sourceRightVersion.getKind();
            } else if (sourceLeftVersion != null) {
                nodeKind = sourceLeftVersion.getKind();
            }
            SVNTreeConflictDescription treeConflictDescription = new SVNTreeConflictDescription(localAbsPath, nodeKind, action, reason, operation, sourceLeftVersion, sourceRightVersion);
            treeConflictRawData = SVNTreeConflictUtil.getSingleTreeConflictRawData(treeConflictDescription);
        }

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
        boolean gotRow;
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            gotRow = stmt.next();
            stmt.reset();

            if (gotRow) {
                stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_CONFLICT_DATA_17);
                stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            } else {
                stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.INSERT_ACTUAL_CONFLICT_DATA_17);
                stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                if (SVNFileUtil.getFilePath(localRelPath).length() > 0) {
                    stmt.bindString(8, SVNPathUtil.removeTail(SVNFileUtil.getFilePath(localRelPath)));
                }
            }
            stmt.bindString(3, SVNFileUtil.getFilePath(conflictOldRelPath));
            stmt.bindString(4, SVNFileUtil.getFilePath(conflictNewRelPath));
            stmt.bindString(5, SVNFileUtil.getFilePath(conflictWorkingRelPath));
            stmt.bindString(6, SVNFileUtil.getFilePath(prejRelPath));
            stmt.bindBlob(7, treeConflictRawData);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    private void flushEntries(SVNWCDbRoot wcRoot, File localAbsPath, SVNDepth depth) {
        final Map<String, SVNWCDbDir> cache = wcRoot.getDb().dirData;
        if (cache.size() == 0) {
            return;
        }
        cache.remove(localAbsPath.getAbsolutePath());
        if (depth.compareTo(SVNDepth.EMPTY) > 0) {
            for (Iterator<Entry<String, SVNWCDbDir>> iterator = cache.entrySet().iterator(); iterator.hasNext(); ) {
                final Entry<String, SVNWCDbDir> entry = iterator.next();
                final String itemAbsPath = entry.getKey();

                if (depth == SVNDepth.FILES || depth == SVNDepth.IMMEDIATES) {
                    iterator.remove();
                } else if (depth == SVNDepth.INFINITY && SVNPathUtil.isAncestor(localAbsPath.getAbsolutePath(), itemAbsPath)) {
                    iterator.remove();
                }
            }
        }

        final File parentAbsPath = SVNFileUtil.getFileDir(localAbsPath);
        cache.remove(parentAbsPath.getAbsolutePath());
    }

    public Map<String, SVNTreeConflictDescription> opReadAllTreeConflicts(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);
        return readAllTreeConflicts(pdh, localRelpath);
    }

    private Map<String, SVNTreeConflictDescription> readAllTreeConflicts(SVNWCDbDir pdh, File localRelpath) throws SVNException {
        if (pdh.getWCRoot().getFormat() == ISVNWCDb.WC_FORMAT_17) {
            return readAllTreeConflicts17(pdh, localRelpath);
        }
        Map<String, SVNTreeConflictDescription> treeConflicts = new HashMap<String, SVNTreeConflictDescription>();
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_CONFLICT);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            boolean haveRow = stmt.next();
            while (haveRow) {
                final File childRelpath = SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath));
                final String childBaseName = SVNFileUtil.getFileName(childRelpath);
                final byte[] conflictData = stmt.getColumnBlob(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data);
                final SVNSkel skel = SVNSkel.parse(conflictData);
                final Structure<ConflictInfo> conflictInfo = SvnWcDbConflicts.readConflictInfo(skel);

                if (conflictInfo != null && conflictInfo.is(ConflictInfo.treeConflicted)) {
                    final List<SVNConflictVersion> locations = conflictInfo.get(ConflictInfo.locations);
                    SVNConflictVersion leftVersion = null;
                    SVNConflictVersion rightVersion = null;
                    if (locations != null && locations.size() > 0) {
                        leftVersion = locations.get(0);
                    }
                    if (locations != null && locations.size() > 1) {
                        rightVersion = locations.get(1);
                    }

                    final File childAbsPath = pdh.getWCRoot().getAbsPath(childRelpath);
                    final Structure<TreeConflictInfo> treeConflictInfo = SvnWcDbConflicts.readTreeConflict(this, childAbsPath, skel);
                    final SVNNodeKind tcKind;
                    if (leftVersion != null) {
                        tcKind = leftVersion.getKind();
                    } else if (rightVersion != null) {
                        tcKind = rightVersion.getKind();
                    } else {
                        tcKind = SVNNodeKind.FILE;
                    }
                    final SVNTreeConflictDescription treeConflict = new SVNTreeConflictDescription(
                            childAbsPath, tcKind,
                            treeConflictInfo.<SVNConflictAction>get(TreeConflictInfo.incomingChange),
                            treeConflictInfo.<SVNConflictReason>get(TreeConflictInfo.localChange),
                            conflictInfo.<SVNOperation>get(ConflictInfo.conflictOperation),
                            leftVersion,
                            rightVersion);
                    treeConflicts.put(childBaseName, treeConflict);
                }
                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }
        return treeConflicts;
    }

    private Map<String, SVNTreeConflictDescription> readAllTreeConflicts17(SVNWCDbDir pdh, File localRelpath) throws SVNException {
        Map<String, SVNTreeConflictDescription> treeConflicts = new HashMap<String, SVNTreeConflictDescription>();
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_CONFLICT_17);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            boolean haveRow = stmt.next();
            while (haveRow) {
                final File childRelpath = SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath));
                final String childBaseName = SVNFileUtil.getFileName(childRelpath);
                final byte[] conflictData = stmt.getColumnBlob(ACTUAL_NODE__Fields.tree_conflict_data);
                final SVNSkel skel = SVNSkel.parse(conflictData);

                SVNTreeConflictDescription treeConflictDescription = SVNTreeConflictUtil.readSingleTreeConflict(skel, SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), localRelpath));
                treeConflicts.put(childBaseName, treeConflictDescription);

                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }
        return treeConflicts;
    }

    public SVNTreeConflictDescription opReadTreeConflict(File localAbsPath) throws SVNException {
        final List<SVNConflictDescription> conflicts = readConflicts(localAbsPath);
        if (conflicts == null || conflicts.isEmpty()) {
            return null;
        }
        for (SVNConflictDescription conflictDescription : conflicts) {
            if (conflictDescription.isTreeConflict()) {
                return (SVNTreeConflictDescription) conflictDescription;
            }
        }
        return null;
    }

    public void opMakeCopy(File localAbspath, SVNSkel conflicts, SVNSkel workItems) throws SVNException {
        assert isAbsolute(localAbspath);

        DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        File localRelpath = toRelPath(localAbspath);
        SVNSqlJetDb sdb = pdh.getWCRoot().getSDb();
        sdb.beginTransaction(SqlJetTransactionMode.WRITE);
        SVNSqlJetStatement stmt = null;
        try {
            stmt = sdb.getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            boolean haveRow = stmt.next();

            if (haveRow) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS,
                        "Modification of '{{0}}' already exists", localAbspath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            catchCopyOfServerExcluded(pdh.getWCRoot(), localRelpath);

            MakeCopy makeCopy = new MakeCopy();
            makeCopy.pdh = pdh;
            makeCopy.localAbspath = localAbspath;
            makeCopy.localRelpath = localRelpath;
            makeCopy.opDepth = SVNWCUtils.relpathDepth(localRelpath);
            makeCopy.conflicts = conflicts;
            makeCopy.workItems = workItems;
            sdb.runTransaction(makeCopy);

        } finally {
            if (stmt != null) {
                stmt.reset();
            }
            sdb.commit();
        }
    }

    public void opRevert(File localAbspath, SVNDepth depth) throws SVNException {
        final DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        SVNSqlJetDb sdb = pdh.getWCRoot().getSDb();
        sdb.beginTransaction(SqlJetTransactionMode.WRITE);

        try {
            SVNSqlJetStatement stmt = new SVNWCDbCreateSchema(sdb.getTemporaryDb(), SVNWCDbCreateSchema.REVERT_LIST, -1, false);
            try {
                stmt.done();
            } finally {
                stmt.reset();
            }

            if (depth == SVNDepth.INFINITY) {
                SvnWcDbRevert.revertRecursive(pdh.getWCRoot(), parsed.localRelPath);
            } else if (depth == SVNDepth.EMPTY) {
                SvnWcDbRevert.revert(pdh.getWCRoot(), parsed.localRelPath);
            }
        } catch (SVNException e) {
            sdb.rollback();
            throw e;
        } finally {
            sdb.commit();
        }
        pdh.flushEntries(localAbspath);
    }

    public void opSetChangelist(File localAbspath, String changelistName, String[] changeLists, SVNDepth depth, ISVNEventHandler eventHandler) throws SVNException {
    	assert (isAbsolute(localAbspath));
        DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        pdh.flushEntries(localAbspath);

        SvnWcDbChangelist.setChangelist(pdh.getWCRoot(), parsed.localRelPath, changelistName, changeLists, depth, eventHandler);
    }


    public void opSetProps(File localAbsPath, SVNProperties props, SVNSkel conflict, boolean clearRecordedInfo, SVNSkel workItems) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbsPath));
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        SetProperties spb = new SetProperties();
        spb.props = props;
        spb.pdh = pdh;
        spb.conflict = conflict;
        spb.workItems = workItems;
        spb.localRelpath = parsed.localRelPath;
        spb.clearRecordedInfo = clearRecordedInfo;

        pdh.getWCRoot().getSDb().runTransaction(spb);
    }

    private class SetProperties implements SVNSqlJetTransaction {

        SVNProperties props;
        SVNWCDbDir pdh;
        File localRelpath;
        SVNSkel conflict;
        SVNSkel workItems;
        boolean clearRecordedInfo;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNProperties pristineProps = SvnWcDbProperties.readPristineProperties(pdh.getWCRoot(), localRelpath);
            if (props != null && pristineProps != null) {
                SVNProperties propDiffs = SVNWCUtils.propDiffs(props, pristineProps);
                if (propDiffs.isEmpty()) {
                    props = null;
                }
            }
            setActualProperties(db, pdh.getWCRoot().getWcId(), localRelpath, props);
            if (clearRecordedInfo) {
                RecordFileinfo rfi = new RecordFileinfo();
                rfi.lastModTime = 0;
                rfi.translatedSize = -1;
                rfi.localRelpath = localRelpath;
                rfi.wcRoot = pdh.getWCRoot();
                rfi.transaction(db);
            }
            addWorkItems(db, workItems);
            if (conflict != null) {
                markConflictInternal(pdh.getWCRoot(), localRelpath, conflict);
            }
        }

    };

    public void setActualProperties(SVNSqlJetDb db, long wcId, File localRelpath, SVNProperties props) throws SVNException {
        long affectedRows;
        SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.UPDATE_ACTUAL_PROPS);
        try {
            stmt.bindf("is", wcId, localRelpath);
            stmt.bindProperties(3, props);
            affectedRows = stmt.done();
        } finally {
            stmt.reset();
        }
        if (affectedRows == 1 || props == null) {
            return;
        }
        stmt = db.getStatement(SVNWCDbStatements.INSERT_ACTUAL_PROPS);
        try{
            stmt.bindf("is", wcId, localRelpath);
            if (localRelpath != null && !"".equals(SVNFileUtil.getFilePath(localRelpath))) {
                stmt.bindString(3, SVNFileUtil.getFilePath(SVNFileUtil.getFileDir(localRelpath)));
            } else {
                stmt.bindNull(3);
            }
            stmt.bindProperties(4, props);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public void opSetTreeConflict(File localAbspath, SVNTreeConflictDescription treeConflict) throws SVNException {
        assert (isAbsolute(localAbspath));
        SVNSkel conflictSkel = SvnWcDbConflicts.treeConflictDescriptionToSkel(this, localAbspath, treeConflict);
        opMarkConflict(localAbspath, conflictSkel, null);
    }

    private class SetTreeConflict implements SVNSqlJetTransaction {

        public File localRelpath;
        public long wcId;
        public File parentRelpath;
        public SVNTreeConflictDescription treeConflict;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            boolean haveRow;
            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
            try {
                stmt.bindf("is", wcId, localRelpath);
                haveRow = stmt.next();
            } finally {
                stmt.reset();
            }
            String treeConflictData;
            if (treeConflict != null) {
                treeConflictData = SVNTreeConflictUtil.getSingleTreeConflictData(treeConflict);
            } else {
                treeConflictData = null;
            }
            try {
                if (haveRow) {
                    stmt = db.getStatement(SVNWCDbStatements.UPDATE_ACTUAL_TREE_CONFLICTS);
                } else {
                    stmt = db.getStatement(SVNWCDbStatements.INSERT_ACTUAL_TREE_CONFLICTS);
                }
                stmt.bindf("iss", wcId, localRelpath, treeConflictData);
                if (!haveRow) {
                    stmt.bindString(4, SVNFileUtil.getFilePath(parentRelpath));
                } else {
                    stmt.bindNull(4);
                }
                stmt.done();
            } finally {
                if (stmt != null) {
                    stmt.reset();
                }
            }
            if (treeConflictData == null) {
                stmt = db.getStatement(SVNWCDbStatements.DELETE_ACTUAL_EMPTY);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }
        }
    }

    public Set<String> readChildren(File localAbsPath) throws SVNException {
        return gatherChildren(localAbsPath, false, false);
    }

    public Set<String> getChildrenOfWorkingNode(File localAbsPath) throws SVNException {
        final DirParsedInfo wcInfo = obtainWcRoot(localAbsPath);
        final File localRelPath = wcInfo.localRelPath;

        final long wcId = wcInfo.wcDbDir.getWCRoot().getWcId();
        final SVNSqlJetDb sDb = wcInfo.wcDbDir.getWCRoot().getSDb();

        final Set<String> names = new TreeSet<String>();

        final SVNSqlJetStatement work_stmt = sDb.getStatement(SVNWCDbStatements.SELECT_WORKING_CHILDREN);
        work_stmt.bindf("is", wcId, SVNFileUtil.getFilePath(localRelPath));
        addChildren(names, work_stmt);//resets statement

        return names;
    }

    public void readChildren(File localAbsPath, Map<String, SVNWCDbInfo> children, Set<String> conflicts) throws SVNException {
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelPath = parsed.localRelPath;

        verifyDirUsable(pdh);

        readChildren(pdh.getWCRoot(), localRelPath, children, conflicts);

    }

    public void readChildren(SVNWCDbRoot root, File localRelPath, Map<String, SVNWCDbInfo> children, Set<String> conflicts) throws SVNException {
        GatherChildren gather = new GatherChildren();
        gather.dirRelPath = localRelPath;
        gather.wcRoot = root;

        gather.nodes = children;
        gather.conflicts = conflicts;

        root.getSDb().runTransaction(gather, SqlJetTransactionMode.READ_ONLY);
    }

    private class GatherChildren implements SVNSqlJetTransaction {

        Map<String, SVNWCDbInfo> nodes;
        Set<String> conflicts;

        File dirRelPath;
        SVNWCDbRoot wcRoot;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            WCDbRepositoryInfo reposInfo = new WCDbRepositoryInfo();

            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_NODE_CHILDREN_INFO);
            try {
                stmt.bindf("is", wcRoot.getWcId(), dirRelPath);
                boolean haveRow = stmt.next();

                while(haveRow) {
                    File childRelPath = SVNFileUtil.createFilePath(getColumnText(stmt, SVNWCDbSchema.NODES__Fields.local_relpath));
                    String name = SVNFileUtil.getFileName(childRelPath);
                    GatheredChildItem childItem = (GatheredChildItem) nodes.get(name);
                    boolean newChild = false;
                    if (childItem == null) {
                        newChild = true;
                        childItem = new GatheredChildItem();
                    }
                    long opDepth = getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.op_depth);

                    if (newChild || opDepth > childItem.opDepth) {
                        childItem.opDepth = opDepth;
                        childItem.kind = getColumnKind(stmt, SVNWCDbSchema.NODES__Fields.kind);
                        childItem.status = getColumnPresence(stmt);
                        if (opDepth != 0) {
                            childItem.incomplete = childItem.status == SVNWCDbStatus.Incomplete;
                            childItem.status = getWorkingStatus(childItem.status);
                        }
                        if (opDepth != 0) {
                            childItem.revnum = INVALID_REVNUM;
                            childItem.reposRelpath = null;
                        } else {
                            childItem.revnum = getColumnRevNum(stmt, SVNWCDbSchema.NODES__Fields.revision);
                            childItem.reposRelpath = SVNFileUtil.createFilePath(getColumnText(stmt, SVNWCDbSchema.NODES__Fields.repos_path));
                        }
                        if (opDepth != 0 || isColumnNull(stmt, SVNWCDbSchema.NODES__Fields.repos_id)) {
                            childItem.reposRootUrl = null;
                            childItem.reposUuid = null;
                        } else {
                            long reposId = getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.repos_id);
                            if (reposInfo.rootUrl == null) {
                                fetchReposInfo(reposInfo, db, reposId);
                            }
                            childItem.reposRootUrl = reposInfo.rootUrl;
                            childItem.reposUuid = reposInfo.uuid;
                        }
                        childItem.changedRev = getColumnRevNum(stmt, SVNWCDbSchema.NODES__Fields.changed_revision);
                        childItem.changedDate = SVNWCUtils.readDate(getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.changed_date));
                        childItem.changedAuthor = getColumnText(stmt, SVNWCDbSchema.NODES__Fields.changed_author);
                        if (childItem.kind != SVNWCDbKind.Dir) {
                            childItem.depth = SVNDepth.UNKNOWN;
                        } else {
                            childItem.depth = getColumnDepth(stmt, SVNWCDbSchema.NODES__Fields.depth);
                            if (newChild) {
                                childItem.locked = isWCLocked(wcRoot, childRelPath, 0);
                            }
                        }
                        childItem.recordedModTime = getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.last_mod_time);
                        childItem.recordedSize = getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.translated_size);
                        childItem.hasChecksum = !isColumnNull(stmt, SVNWCDbSchema.NODES__Fields.checksum);
                        childItem.copied = opDepth > 0 && !isColumnNull(stmt, NODES__Fields.repos_path);
                        childItem.hadProps = !isColumnNull(stmt, SVNWCDbSchema.NODES__Fields.properties) && getColumnBlob(stmt, SVNWCDbSchema.NODES__Fields.properties).length > 2;

                        if (childItem.hadProps) {
                            SVNProperties properties = getColumnProperties(stmt, SVNWCDbSchema.NODES__Fields.properties);
                            childItem.special = properties.getSVNPropertyValue(SVNProperty.SPECIAL) != null;
                        }

                        if (opDepth == 0) {
                            childItem.opRoot = false;
                        } else {
                            childItem.opRoot = opDepth == SVNWCUtils.relpathDepth(childRelPath);
                        }
                        childItem.format = db.getDb().getOptions().getUserVersion();
                        nodes.put(name, childItem);
                    }
                    if (opDepth == 0) {
                        childItem.haveBase = true;
                        SVNSqlJetStatement lockStmt = stmt.getJoinedStatement(SVNWCDbSchema.LOCK);
                        if (lockStmt != null && !lockStmt.eof()) {
                            childItem.lock = new SVNWCDbLock();
                            childItem.lock.token = getColumnText(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_token);
                            if (!isColumnNull(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_owner))
                                childItem.lock.owner = getColumnText(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_owner);
                            if (!isColumnNull(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_comment))
                                childItem.lock.comment = getColumnText(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_comment);
                            if (!isColumnNull(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_date))
                                childItem.lock.date = SVNWCUtils.readDate(getColumnInt64(lockStmt, SVNWCDbSchema.LOCK__Fields.lock_date));
                        }
                        childItem.fileExternal = stmt.getColumnBoolean(NODES__Fields.file_external);
                    } else {
                        final File movedToRelpath = getColumnPath(stmt, SVNWCDbSchema.NODES__Fields.moved_to);
                        if (movedToRelpath != null) {
                            childItem.movedToAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), movedToRelpath);
                        }
                        childItem.movedHere = getColumnBoolean(stmt, SVNWCDbSchema.NODES__Fields.moved_here);
                        childItem.layersCount++;
                        childItem.haveMoreWork = childItem.layersCount > 1;
                    }
                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }
            if (wcRoot.getFormat() == ISVNWCDb.WC_FORMAT_17) {
                stmt = db.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_INFO_17);
            } else {
                stmt = db.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_INFO);
            }
            try {
                stmt.bindf("is", wcRoot.getWcId(), dirRelPath);
                while(stmt.next()) {
                    File childRelPath = SVNFileUtil.createFilePath(getColumnText(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath));
                    String name = SVNFileUtil.getFileName(childRelPath);

                    GatheredChildItem childItem = (GatheredChildItem) nodes.get(name);
                    if (childItem == null) {
                        childItem = new GatheredChildItem();
                        childItem.status = SVNWCDbStatus.NotPresent;
                    }
                    childItem.changelist = getColumnText(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.changelist);
                    childItem.propsMod = !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.properties);
                    if (childItem.propsMod) {
                        SVNProperties properties = getColumnProperties(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.properties);
                        childItem.special = properties.getSVNPropertyValue(SVNProperty.SPECIAL) != null;
                    }

                    childItem.conflicted = !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data) || /* data */
                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) || /* old */
                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new) || /* new */
                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working) || /* working */
                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject) || /* prop_reject */
                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data) /* tree_conflict_data */;
                    if (childItem.conflicted) {
                        conflicts.add(name);
                    }
                }
            } finally {
                stmt.reset();
            }
        }

    }

    private static class GatheredChildItem extends SVNWCDbInfo {
        public int layersCount;
        public long opDepth;
    }

    private Set<String> gatherChildren(File localAbsPath, boolean baseOnly, boolean workOnly) throws SVNException {
        final DirParsedInfo wcInfo = obtainWcRoot(localAbsPath);
        final File localRelPath = wcInfo.localRelPath;

        final long wcId = wcInfo.wcDbDir.getWCRoot().getWcId();
        final SVNSqlJetDb sDb = wcInfo.wcDbDir.getWCRoot().getSDb();

        final Set<String> names = new TreeSet<String>();

        if (!workOnly) {
            final SVNSqlJetStatement base_stmt = sDb.getStatement(SVNWCDbStatements.SELECT_BASE_NODE_CHILDREN);
            base_stmt.bindf("is", wcId, localRelPath);
            addChildren(names, base_stmt);//resets statement
        }

        if (!baseOnly) {
            final SVNSqlJetStatement work_stmt = sDb.getStatement(SVNWCDbStatements.SELECT_WORKING_NODE_CHILDREN);
            work_stmt.bindf("is", wcId, localRelPath);
            addChildren(names, work_stmt);//resets statement
        }

        return names;
    }

    public Map<String, WCDbBaseInfo> getBaseChildrenMap(File localAbsPath, boolean fetchLocks) throws SVNException {
        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        return getBaseChildrenMap(parsed.wcDbDir.getWCRoot(), parsed.localRelPath, fetchLocks);
    }

    public Map<String, WCDbBaseInfo> getBaseChildrenMap(SVNWCDbRoot wcRoot, File localRelPath, boolean fetchLocks) throws SVNException {
        final long wcId = wcRoot.getWcId();
        final SVNSqlJetDb sDb = wcRoot.getSDb();

        final Map<String, WCDbBaseInfo> children = new TreeMap<String, WCDbBaseInfo>();
        final SVNSqlJetSelectStatement baseStmt = (SVNSqlJetSelectStatement) sDb.getStatement(SVNWCDbStatements.SELECT_BASE_CHILDREN_INFO);

        baseStmt.bindf("is", wcId, localRelPath);
        Map<String, Object> row = null;
        try {
            while(baseStmt.next()) {
                WCDbBaseInfo child = new WCDbBaseInfo();
                row = baseStmt.getRowValues2(row);

                child.updateRoot = row.get(SVNWCDbSchema.NODES__Fields.file_external.toString()) != null;
                child.status = SvnWcDbStatementUtil.parsePresence((String) row.get(SVNWCDbSchema.NODES__Fields.presence.toString()));
                child.revision = (Long) row.get(SVNWCDbSchema.NODES__Fields.revision.toString()) ;
                final String path = (String) row.get(SVNWCDbSchema.NODES__Fields.repos_path.toString());
                child.reposRelPath = path != null ? new File(path) : null;
                child.depth = SvnWcDbStatementUtil.parseDepth((String) row.get(SVNWCDbSchema.NODES__Fields.depth.toString()));
                child.kind = SvnWcDbStatementUtil.parseKind((String) row.get(SVNWCDbSchema.NODES__Fields.kind.toString()));

                if (fetchLocks) {
                    final SVNSqlJetStatement lockStmt = fetchLocks ? sDb.getStatement(SVNWCDbStatements.SELECT_LOCK) : null;
                    try {
                        child.lock = null;
                        lockStmt.bindf("is",
                                row.get(SVNWCDbSchema.NODES__Fields.repos_id.toString()),
                                row.get(SVNWCDbSchema.NODES__Fields.repos_path.toString()));
                        if (lockStmt.next()) {
                            child.lock = SvnWcDbStatementUtil.getLockFromColumns(lockStmt,
                                    SVNWCDbSchema.LOCK__Fields.lock_token,
                                    SVNWCDbSchema.LOCK__Fields.lock_owner,
                                    SVNWCDbSchema.LOCK__Fields.lock_comment,
                                    SVNWCDbSchema.LOCK__Fields.lock_date);
                        }
                    } finally {
                        lockStmt.reset();
                    }
                }
                final String child_relpath = (String) row.get(SVNWCDbSchema.NODES__Fields.local_relpath.toString());
                children.put(SVNPathUtil.tail(child_relpath), child);
            }
        } finally {
            baseStmt.reset();
        }

        return children;
    }

    private void addChildren(Set<String> children, SVNSqlJetStatement stmt) throws SVNException {
        try {
            while (stmt.next()) {
                String child_relpath = getColumnText(stmt, SVNWCDbSchema.NODES__Fields.local_relpath);
                String name = SVNFileUtil.getFileName(SVNFileUtil.createFilePath(child_relpath));
                children.add(name);
            }
        } finally {
            stmt.reset();
        }
    }

    public List<String> readConflictVictims(File localAbsPath) throws SVNException {
        final DirParsedInfo wcInfo = obtainWcRoot(localAbsPath);
        final File localRelPath = wcInfo.localRelPath;
        final long wcId = wcInfo.wcDbDir.getWCRoot().getWcId();
        final SVNSqlJetDb sDb = wcInfo.wcDbDir.getWCRoot().getSDb();

        SVNSqlJetStatement stmt;

        List<String> victims = new ArrayList<String>();

        /*
         * ### This will be much easier once we have all conflicts in one field
         * of actual
         */

        Set<String> found = new HashSet<String>();

        /* First look for text and property conflicts in ACTUAL */
        if (wcInfo.wcDbDir.getWCRoot().getFormat() == ISVNWCDb.WC_FORMAT_17) {
            stmt = sDb.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CONFLICT_VICTIMS_17);
        } else {
            stmt = sDb.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CONFLICT_VICTIMS);
        }
        try {
            stmt.bindf("is", wcId, SVNFileUtil.getFilePath(localRelPath));
            while (stmt.next()) {
                String child_relpath = getColumnText(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath);
                String child_name = SVNFileUtil.getFileName(SVNFileUtil.createFilePath(child_relpath));
                found.add(child_name);
            }
        } finally {
            stmt.reset();
        }

        victims.addAll(found);
        return victims;
    }

    public List<SVNConflictDescription> readConflicts(File localAbsPath) throws SVNException {
        final List<SVNWCConflictDescription17> conflicts = readConflicts(localAbsPath, false);
        final List<SVNConflictDescription> translated = new ArrayList<SVNConflictDescription>();
        for(SVNWCConflictDescription17 description : conflicts) {

            final SVNMergeFileSet mergeFiles = new SVNMergeFileSet(null, null,
                    description.getBaseFile(),
                    description.getMyFile(),
                    localAbsPath.getAbsolutePath(),
                    description.getTheirFile(),
                    description.getMergedFile(),
                    null,
                    description.getMimeType());

            if (description.getKind() == ConflictKind.PROPERTY) {
                translated.add(new SVNPropertyConflictDescription(mergeFiles, description.getNodeKind(), description.getPropertyName(), description.getAction(), description.getReason()));
            } else if (description.getKind() == ConflictKind.TREE) {
                translated.add(new SVNTreeConflictDescription(localAbsPath, description.getNodeKind(), description.getAction(), description.getReason(), description.getOperation(), description.getSrcLeftVersion(), description.getSrcRightVersion()));
            } else if (description.getKind() == ConflictKind.TEXT) {
                translated.add(new SVNTextConflictDescription(mergeFiles, description.getNodeKind(), description.getAction(), description.getReason()));
            }
        }
        return translated;
    }

    public List<SVNWCConflictDescription17> readConflicts(File localAbsPath, boolean createTempFiles) throws SVNException {

        /* The parent should be a working copy directory. */
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parseDir.wcDbDir;
//        final File localRelPath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        if (pdh.getWCRoot().getFormat() == ISVNWCDb.WC_FORMAT_17) {
            return readConflicts17(pdh.getWCRoot(), parseDir.localRelPath, createTempFiles);
        }

        /*
         * ### This will be much easier once we have all conflicts in one field
         * of actual.
         */
        final SVNSkel conflictSkel = SvnWcDbConflicts.readConflict(pdh.getWCRoot().getDb(), localAbsPath);
        return SvnWcDbConflicts.convertFromSkel(this, localAbsPath, createTempFiles, conflictSkel);
    }

    private List<SVNWCConflictDescription17> readConflicts17(SVNWCDbRoot wcRoot, File localRelPath, boolean createTempFiles) throws SVNException {
        final File localAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelPath);
        final List<SVNWCConflictDescription17> conflicts = new ArrayList<SVNWCConflictDescription17>();

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_CONFLICT_DETAILS);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            while (stmt.next()) {
                final String conflictOld = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_old);
                final String conflictWorking = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_working);
                final String conflictNew = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_new);
                final String propReject = stmt.getColumnString(ACTUAL_NODE__Fields.prop_reject);
                final byte[] treeConflictData = stmt.getColumnBlob(ACTUAL_NODE__Fields.tree_conflict_data);

                if (conflictOld != null || conflictWorking != null || conflictNew != null) {
                    final File conflictNewAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictNew);
                    final File conflictOldAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictOld);
                    final File conflictWorkingAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictWorking);

                    final SVNWCConflictDescription17 conflictDescription = SVNWCConflictDescription17.createText(localAbsPath);
                    conflictDescription.setTheirFile(conflictNewAbsPath);
                    conflictDescription.setBaseFile(conflictOldAbsPath);
                    conflictDescription.setMyFile(conflictWorkingAbsPath);
                    conflicts.add(conflictDescription);
                }

                if (propReject != null) {
                    final File propRejectAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), propReject);

                    final SVNWCConflictDescription17 conflictDescription = SVNWCConflictDescription17.createProp(localAbsPath, SVNNodeKind.UNKNOWN, "");
                    conflictDescription.setTheirFile(propRejectAbsPath);
                    conflicts.add(conflictDescription);
                }


                if (treeConflictData != null) {
                    final SVNSkel tcSkel = SVNSkel.parse(treeConflictData);
                    final SVNTreeConflictDescription tcDesc = SVNTreeConflictUtil.readSingleTreeConflict(tcSkel, localAbsPath);

                    final SVNWCConflictDescription17 conflictDescription = SVNWCConflictDescription17.createTree(localAbsPath, tcDesc.getNodeKind(), tcDesc.getOperation(), tcDesc.getSourceLeftVersion(), tcDesc.getSourceRightVersion());
                    conflictDescription.setAction(tcDesc.getConflictAction());
                    conflictDescription.setReason(tcDesc.getConflictReason());
                    if (tcDesc.getMergeFiles() != null) {
                        conflictDescription.setMyFile(tcDesc.getMergeFiles().getLocalFile());
                    }
                    conflicts.add(conflictDescription);
                }
            }

            return conflicts;
        } finally {
            stmt.reset();
        }
    }

    public SVNSkel readConflict(File localAbsPath) throws SVNException {
        DirParsedInfo pdh = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir wcDbDir = pdh.wcDbDir;
        verifyDirUsable(wcDbDir);
        File localRelPath = pdh.localRelPath;
        return readConflictInternal(wcDbDir.getWCRoot(), localRelPath);
    }

    public SVNSkel readConflictInternal(SVNWCDbRoot wcRoot, File localRelPath) throws SVNException {
        if (wcRoot.getFormat() == ISVNWCDb.WC_FORMAT_17) {
            return readConflictInternal17(wcRoot, localRelPath);
        }
        SVNSqlJetStatement statement = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
        try {
            statement.bindf("is", wcRoot.getWcId(), localRelPath);
            boolean haveRow = statement.next();
            if (!haveRow) {
                SVNSqlJetStatement selectNodeInfoStatement = null;
                try {
                    selectNodeInfoStatement = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
                    selectNodeInfoStatement.bindf("is", wcRoot.getWcId(), localRelPath);
                    haveRow = selectNodeInfoStatement.next();
                    if (haveRow) {
                        return null;
                    }
                } catch (SVNException e) {
                    //ignore
                } finally {
                    if (selectNodeInfoStatement != null) {
                        selectNodeInfoStatement.reset();
                    }
                }

                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND,
                        "The node '{{0}}' was not found.", localRelPath); //TODO: transform path
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            final byte[] conflictData = statement.getColumnBlob(ACTUAL_NODE__Fields.conflict_data);
            if (conflictData != null) {
                return SVNSkel.parse(conflictData);
            } else {
                return null;
            }

        } finally {
            statement.reset();
        }
    }

    private SVNSkel readConflictInternal17(SVNWCDbRoot wcRoot, File localRelPath) throws SVNException {
        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            if (!stmt.next()) {
                return null;
            }
            String conflictOld = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_old);
            String conflictWorking = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_working);
            String conflictNew = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_new);
            String propReject = stmt.getColumnString(ACTUAL_NODE__Fields.prop_reject);
            byte[] treeConflictData = stmt.getColumnBlob(ACTUAL_NODE__Fields.tree_conflict_data);

            return SvnWcDbConflicts.convertToConflictSkel(wcRoot.getAbsPath(), wcRoot.getDb(), SVNFileUtil.getFilePath(localRelPath), conflictOld, conflictWorking, conflictNew, propReject, treeConflictData);
        } finally {
            stmt.reset();
        }
    }

    public WCDbInfo readInfo(File localAbsPath, InfoField... fields) throws SVNException {
        final DirParsedInfo wcInfo = obtainWcRoot(localAbsPath);
        final File localRelPath = wcInfo.localRelPath;
        final SVNSqlJetDb sDb = wcInfo.wcDbDir.getWCRoot().getSDb();

        final WCDbInfo info = readInfo(wcInfo.wcDbDir.getWCRoot(), localRelPath, fields);

        final EnumSet<InfoField> f = getInfoFields(InfoField.class, fields);
        if (f.contains(InfoField.reposRootUrl) || f.contains(InfoField.reposUuid)) {
            ReposInfo reposInfo = fetchReposInfo(sDb, info.reposId);
            if (reposInfo.reposRootUrl != null) {
                info.reposRootUrl = f.contains(InfoField.reposRootUrl) ? SVNURL.parseURIEncoded(reposInfo.reposRootUrl) : null;
            }
            info.reposUuid = f.contains(InfoField.reposUuid) ? reposInfo.reposUuid : null;
        }
        if (f.contains(InfoField.originalRootUrl) || f.contains(InfoField.originalUuid)) {
            ReposInfo reposInfo = fetchReposInfo(sDb, info.originalReposId);
            if (reposInfo.reposRootUrl != null) {
                info.originalRootUrl = f.contains(InfoField.originalRootUrl) ? SVNURL.parseURIEncoded(reposInfo.reposRootUrl) : null;
            }
            info.originalUuid = f.contains(InfoField.originalUuid) ? reposInfo.reposUuid : null;
        }
        return info;
    }

    public Structure<NodeInfo> readInfo(File localAbsPath, NodeInfo... fields) throws SVNException {
        return readInfo(localAbsPath, false, fields);
    }

    public Structure<NodeInfo> readInfo(File localAbsPath, boolean isAdditionMode, NodeInfo... fields) throws SVNException {
        final DirParsedInfo wcInfo = obtainWcRoot(localAbsPath, isAdditionMode);
        final File localRelPath = wcInfo.localRelPath;
        final SVNSqlJetDb sDb = wcInfo.wcDbDir.getWCRoot().getSDb();

        if (fields != null) {
            //if reposRootUrl or reposUuid or originalRootUrl or originalUuid are specified, we need to know corresponding reposId
            Structure<NodeInfo> fieldsInfo = Structure.obtain(NodeInfo.class, fields);
            final boolean needsReposId = (fieldsInfo.hasField(NodeInfo.reposRootUrl) || fieldsInfo.hasField(NodeInfo.reposUuid)) && !fieldsInfo.hasField(NodeInfo.reposId);
            final boolean needsOriginalReposId = (fieldsInfo.hasField(NodeInfo.originalRootUrl) || fieldsInfo.hasField(NodeInfo.originalUuid)) && !fieldsInfo.hasField(NodeInfo.originalReposId);

            if (needsReposId || needsOriginalReposId) {
                final int modifiedFieldsCount = fields.length + (needsReposId ? 1 : 0) + (needsOriginalReposId ? 1 : 0);
                final NodeInfo[] modifiedFields = new NodeInfo[modifiedFieldsCount];
                System.arraycopy(fields, 0, modifiedFields, 0, fields.length);
                int index = fields.length;
                if (needsReposId) {
                    modifiedFields[index] = NodeInfo.reposId;
                    index++;
                }
                if (needsOriginalReposId) {
                    modifiedFields[index] = NodeInfo.originalReposId;
                    index++;
                }
                fields = modifiedFields;
            }
        }

        Structure<NodeInfo> info = SvnWcDbShared.readInfo(wcInfo.wcDbDir.getWCRoot(), localRelPath, fields);

        if (info.hasField(NodeInfo.reposRootUrl) || info.hasField(NodeInfo.reposUuid)) {
            Structure<RepositoryInfo> reposInfo = fetchRepositoryInfo(sDb, info.lng(NodeInfo.reposId));
            reposInfo.from(RepositoryInfo.reposRootUrl, RepositoryInfo.reposUuid).into(info, NodeInfo.reposRootUrl, NodeInfo.reposUuid);
            reposInfo.release();
        }
        if (info.hasField(NodeInfo.originalRootUrl) || info.hasField(NodeInfo.originalUuid)) {
            Structure<RepositoryInfo> reposInfo = fetchRepositoryInfo(sDb, info.lng(NodeInfo.originalReposId));
            reposInfo.from(RepositoryInfo.reposRootUrl, RepositoryInfo.reposUuid).into(info, NodeInfo.originalRootUrl, NodeInfo.originalUuid);
            reposInfo.release();
        }
        return info;
    }

    public long readOpDepth(SVNWCDbRoot root, File localRelPath) throws SVNException {
        SVNSqlJetStatement stmt = null;
        try {
            stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
            stmt.bindf("is", root.getWcId(), localRelPath);
            if (stmt.next()) {
                return getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.op_depth);
            }
        } finally {
            try {
                if (stmt != null) {
                    stmt.reset();
                }
            } catch (SVNException e) {}
        }
        return 0;
    }

    public WCDbInfo readInfoBelowWorking(File localAbsPath) throws SVNException {
        final DirParsedInfo wcInfo = obtainWcRoot(localAbsPath);
        final File localRelPath = wcInfo.localRelPath;
        return readInfoBelowWorking(wcInfo.wcDbDir.getWCRoot(), localRelPath, -1);
    }

    public WCDbInfo readInfoBelowWorking(SVNWCDbRoot wcRoot, File localRelPath, int belowOpDepth) throws SVNException {

        WCDbInfo info = new WCDbInfo();
        SVNSqlJetStatement stmt = null;
        boolean haveRow;
        try {
            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            haveRow = stmt.next();

            if (belowOpDepth >= 0) {
                while(haveRow && getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.op_depth) > belowOpDepth) {
                    haveRow = stmt.next();
                }
            }
            if (haveRow) {
                haveRow = stmt.next();
                if (haveRow) {
                    info.status = getColumnPresence(stmt);
                }
                while (haveRow) {
                    if (getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.op_depth) > 0) {
                        info.haveWork = true;
                    } else {
                        info.haveBase = true;
                    }
                    haveRow = stmt.next();
                }
            }
        } finally {
            try {
                if (stmt != null) {
                    stmt.reset();
                }
            } catch (SVNException e) {}
        }
        if (info.haveWork) {
            info.status = getWorkingStatus(info.status);
        }
        return info;
    }

    public WCDbInfo readInfo(SVNWCDbRoot wcRoot, File localRelPath, InfoField... fields) throws SVNException {

        WCDbInfo info = new WCDbInfo();

        final EnumSet<InfoField> f = getInfoFields(InfoField.class, fields);
        SVNSqlJetStatement stmtInfo = null;
        SVNSqlJetStatement stmtActual = null;

        try {
            stmtInfo = wcRoot.getSDb().getStatement(f.contains(InfoField.lock) ? SVNWCDbStatements.SELECT_NODE_INFO_WITH_LOCK : SVNWCDbStatements.SELECT_NODE_INFO);
            stmtInfo.bindf("is", wcRoot.getWcId(), localRelPath);
            boolean haveInfo = stmtInfo.next();
            boolean haveActual = false;

            if (f.contains(InfoField.changelist) || f.contains(InfoField.conflicted) || f.contains(InfoField.propsMod)) {
                stmtActual = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
                stmtActual.bindf("is", wcRoot.getWcId(), localRelPath);
                haveActual = stmtActual.next();
            }

            if (haveInfo) {
                long opDepth = getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.op_depth);
                SVNWCDbKind nodeKind = getColumnKind(stmtInfo, NODES__Fields.kind);
                if (f.contains(InfoField.status)) {
                    info.status = getColumnPresence(stmtInfo);
                    if (opDepth != 0) {
                        info.status = getWorkingStatus(info.status);
                    }
                }
                if (f.contains(InfoField.kind)) {
                    info.kind = nodeKind;
                }
                info.reposId = opDepth != 0 ? INVALID_REPOS_ID : getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.repos_id);
                if (f.contains(InfoField.revision)) {
                    info.revision = opDepth != 0 ? INVALID_REVNUM : getColumnRevNum(stmtInfo, SVNWCDbSchema.NODES__Fields.revision);
                }
                if (f.contains(InfoField.reposRelPath)) {
                    info.reposRelPath = opDepth != 0 ? null : SVNFileUtil.createFilePath(getColumnText(stmtInfo, SVNWCDbSchema.NODES__Fields.repos_path));
                }
                if (f.contains(InfoField.changedDate)) {
                    info.changedDate = isColumnNull(stmtInfo, SVNWCDbSchema.NODES__Fields.changed_date) ? null : SVNWCUtils.readDate(getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.changed_date));
                }
                if (f.contains(InfoField.changedRev)) {
                    info.changedRev = getColumnRevNum(stmtInfo, SVNWCDbSchema.NODES__Fields.changed_revision);
                }
                if (f.contains(InfoField.changedAuthor)) {
                    info.changedAuthor = getColumnText(stmtInfo, SVNWCDbSchema.NODES__Fields.changed_author);
                }
                if (f.contains(InfoField.lastModTime)) {
                    info.lastModTime = getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.last_mod_time);
                }
                if (f.contains(InfoField.depth)) {
                    if (nodeKind != SVNWCDbKind.Dir) {
                        info.depth = SVNDepth.UNKNOWN;
                    } else {
                        info.depth = getColumnDepth(stmtInfo, SVNWCDbSchema.NODES__Fields.depth);
                    }
                }
                if (f.contains(InfoField.checksum)) {
                    if (nodeKind != SVNWCDbKind.File) {
                        info.checksum = null;
                    } else {
                        try {
                            info.checksum = getColumnChecksum(stmtInfo, SVNWCDbSchema.NODES__Fields.checksum);
                        } catch (SVNException e) {
                            SVNErrorMessage err = SVNErrorMessage.create(e.getErrorMessage().getErrorCode(), "The node ''{0}'' has a corrupt checksum value.", wcRoot.getAbsPath(localRelPath));
                            SVNErrorManager.error(err, SVNLogType.WC);
                        }
                    }
                }
                if (f.contains(InfoField.translatedSize)) {
                    info.translatedSize = getTranslatedSize(stmtInfo, SVNWCDbSchema.NODES__Fields.translated_size);
                }
                if (f.contains(InfoField.target)) {
                    info.target = SVNFileUtil.createFilePath(getColumnText(stmtInfo, SVNWCDbSchema.NODES__Fields.symlink_target));
                }
                if (f.contains(InfoField.changelist) && haveActual) {
                    info.changelist = getColumnText(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.changelist);
                }
                info.originalReposId = opDepth == 0 ? INVALID_REPOS_ID : getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.repos_id);
                if (f.contains(InfoField.originalRevision)) {
                    info.originalRevision = opDepth == 0 ? INVALID_REVNUM : getColumnRevNum(stmtInfo, SVNWCDbSchema.NODES__Fields.revision);
                }
                if (f.contains(InfoField.originalReposRelpath)) {
                    info.originalReposRelpath = opDepth == 0 ? null : SVNFileUtil.createFilePath(getColumnText(stmtInfo, SVNWCDbSchema.NODES__Fields.repos_path));
                }
                if (f.contains(InfoField.propsMod) && haveActual) {
                    info.propsMod = !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.properties);
                }
                if (f.contains(InfoField.hadProps)) {
                    byte[] props = getColumnBlob(stmtInfo, SVNWCDbSchema.NODES__Fields.properties);
                    info.hadProps = props != null && props.length > 2;
                }
                if (f.contains(InfoField.conflicted) && haveActual) {
                    info.conflicted =
                        !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data) || /* data */
                        !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) || /* old */
                        !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new) || /* new */
                        !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working) || /* working */
                        !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject) || /* prop_reject */
                        !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data) /* tree_conflict_data */;
                }
                if (f.contains(InfoField.lock) && opDepth == 0) {
                    final SVNSqlJetStatement stmtBaseLock = stmtInfo.getJoinedStatement(SVNWCDbSchema.LOCK.toString());
                    if (!isColumnNull(stmtBaseLock, SVNWCDbSchema.LOCK__Fields.lock_token)) {
                        info.lock = new SVNWCDbLock();
                        info.lock.token = getColumnText(stmtBaseLock, SVNWCDbSchema.LOCK__Fields.lock_token);
                        if (!isColumnNull(stmtBaseLock, SVNWCDbSchema.LOCK__Fields.lock_owner)) {
                            info.lock.owner = getColumnText(stmtBaseLock, SVNWCDbSchema.LOCK__Fields.lock_owner);
                        }
                        if (!isColumnNull(stmtBaseLock, SVNWCDbSchema.LOCK__Fields.lock_comment)) {
                            info.lock.comment = getColumnText(stmtBaseLock, SVNWCDbSchema.LOCK__Fields.lock_comment);
                        }
                        if (!isColumnNull(stmtBaseLock, SVNWCDbSchema.LOCK__Fields.lock_date)) {
                            info.lock.date = SVNWCUtils.readDate(getColumnInt64(stmtBaseLock, SVNWCDbSchema.LOCK__Fields.lock_date));
                        }
                    }
                }
                if (f.contains(InfoField.haveWork)) {
                    info.haveWork = opDepth != 0;
                }
                if (f.contains(InfoField.opRoot)) {
                    info.opRoot = opDepth > 0 && opDepth == SVNWCUtils.relpathDepth(localRelPath);
                }
                if (f.contains(InfoField.movedHere)) {
                    info.movedHere = getColumnBoolean(stmtInfo, NODES__Fields.moved_here);
                }
                if (f.contains(InfoField.movedTo)) {
                    final File relativePath = getColumnPath(stmtInfo, NODES__Fields.moved_to);
                    if (relativePath != null) {
                        info.movedToAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), relativePath);
                    }
                }
                if (f.contains(InfoField.haveBase) || f.contains(InfoField.haveWork)) {
                    while(opDepth != 0) {
                        haveInfo = stmtInfo.next();
                        if (!haveInfo) {
                            break;
                        }
                        opDepth = getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.op_depth);
                        if (f.contains(InfoField.haveMoreWork)) {
                            if (opDepth > 0) {
                                info.haveMoreWork = true;
                            }
                            if (!f.contains(InfoField.haveBase)) {
                                break;
                            }
                        }
                    }
                    if (f.contains(InfoField.haveBase)) {
                        info.haveBase = opDepth == 0;
                    }
                }
            } else if (haveActual) {
                if (isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, "Corrupt data for ''{0}''", wcRoot.getAbsPath(localRelPath));
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                assert (f.contains(InfoField.conflicted));
                if (f.contains(InfoField.status)) {
                    info.status = SVNWCDbStatus.Normal;
                }
                if (f.contains(InfoField.kind)) {
                    info.kind = SVNWCDbKind.Unknown;
                }
                if (f.contains(InfoField.revision)) {
                    info.revision = INVALID_REVNUM;
                }
                info.reposId = INVALID_REPOS_ID;
                if (f.contains(InfoField.changedRev)) {
                    info.changedRev = INVALID_REVNUM;
                }
                if (f.contains(InfoField.depth)) {
                    info.depth = SVNDepth.UNKNOWN;
                }
                if (f.contains(InfoField.originalReposId)) {
                    info.originalReposId = INVALID_REPOS_ID;
                }
                if (f.contains(InfoField.originalRevision)) {
                    info.originalRevision = INVALID_REVNUM;
                }
                if (f.contains(InfoField.changelist)) {
                    info.changelist = stmtActual.getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist);
                }
                if (f.contains(InfoField.originalRevision)) {
                    info.originalRevision = INVALID_REVNUM;
                }
                if (f.contains(InfoField.conflicted)) {
                    info.conflicted = true;
                }
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", wcRoot.getAbsPath(localRelPath));
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } finally {
            try {
                if (stmtInfo != null) {
                    stmtInfo.reset();
                }
            } catch (SVNException e) {}
            try {
                if (stmtActual != null) {
                    stmtActual.reset();
                }
            } catch (SVNException e) {}
        }

        return info;
    }

    public static SVNWCDbStatus getWorkingStatus(SVNWCDbStatus status) {
        if (status == SVNWCDbStatus.Excluded) {
            return status;
        } else if (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.BaseDeleted) {
            return SVNWCDbStatus.Deleted;
        }
        return SVNWCDbStatus.Added;
    }

    public SVNWCDbKind readKind(File localAbsPath, boolean allowMissing) throws SVNException {
        try {
            final WCDbInfo info = readInfo(localAbsPath, InfoField.kind);
            return info.kind;
        } catch (SVNException e) {
            if (allowMissing && e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                return SVNWCDbKind.Unknown;
            }
            throw e;
        }
    }

    public SVNNodeKind readKind(File localAbsPath, boolean allowMissing, boolean showDeleted, boolean showHidden) throws SVNException {
        assert isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        SVNWCDbRoot wcRoot = pdh.getWCRoot();
        File localRelPath = parsed.localRelPath;

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            boolean haveInfo = stmt.next();

            if (!haveInfo) {
                if (allowMissing) {
                    return SVNNodeKind.UNKNOWN;
                } else {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelPath));
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            }

            if (!(showDeleted && showHidden)) {
                int opDepth = (int) stmt.getColumnLong(NODES__Fields.op_depth);
                boolean reportNone = false;
                SVNWCDbStatus status = SvnWcDbStatementUtil.getColumnPresence(stmt, NODES__Fields.presence);

                if (opDepth > 0) {
                    status = convertToWorkingStatus(status);
                }

                switch (status) {
                    case NotPresent:
                        if (! (showHidden && showDeleted)) {
                            reportNone = true;
                        }
                        break;
                    case Excluded:
                    case ServerExcluded:
                        if (!showHidden) {
                            reportNone = true;
                        }
                        break;
                    case Deleted:
                        if (!showDeleted) {
                            reportNone = true;
                        }
                        break;
                    default:
                        break;
                }

                if (reportNone) {
                    return SVNNodeKind.NONE;
                }
            }

            return SvnWcDbStatementUtil.getColumnKind(stmt, NODES__Fields.kind) == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;

        } finally {
            stmt.reset();
        }
    }

    private SVNWCDbStatus convertToWorkingStatus(SVNWCDbStatus status) {
        SVNWCDbStatus workStatus = status;
        assert workStatus == SVNWCDbStatus.Normal ||
                workStatus == SVNWCDbStatus.NotPresent ||
                workStatus == SVNWCDbStatus.BaseDeleted ||
                workStatus == SVNWCDbStatus.Incomplete ||
                workStatus == SVNWCDbStatus.Excluded;
        if (workStatus == SVNWCDbStatus.Excluded) {
            return SVNWCDbStatus.Excluded;
        } else if (workStatus == SVNWCDbStatus.NotPresent || workStatus == SVNWCDbStatus.BaseDeleted) {
            return SVNWCDbStatus.Deleted;
        } else {
            return SVNWCDbStatus.Added;
        }
    }

    public InputStream readPristine(File wcRootAbsPath, SvnChecksum checksum) throws SVNException {
        assert (isAbsolute(wcRootAbsPath));
        assert (checksum != null);

        /*
         * ### Transitional: accept MD-5 and look up the SHA-1. Return an error
         * if the pristine text is not in the store.
         */
        if (checksum.getKind() != SvnChecksum.Kind.sha1) {
            //i.e. checksum has kind "md5"
            checksum = getPristineSHA1(wcRootAbsPath, checksum);
        }
        assert (checksum.getKind() == SvnChecksum.Kind.sha1);

        final DirParsedInfo parsed = parseDir(wcRootAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        /* ### should we look in the PRISTINE table for anything? */

        return SvnWcDbPristines.readPristine(pdh.getWCRoot(), wcRootAbsPath, checksum);

    }

    public SVNProperties readPristineProperties(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));
        final DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelPath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        return SvnWcDbProperties.readPristineProperties(pdh.getWCRoot(), localRelPath);
    }

    public void readPropertiesRecursively(File localAbsPath, SVNDepth depth, boolean baseProperties, boolean pristineProperties, Collection<String> changelists,
            ISvnObjectReceiver<SVNProperties> receiver) throws SVNException {
        assert (isAbsolute(localAbsPath));

        final DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelPath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        SvnWcDbProperties.readPropertiesRecursively(pdh.getWCRoot(), localRelPath, depth, baseProperties, pristineProperties, changelists, receiver);
    }

    public SVNProperties readProperties(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));
        final DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelPath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        try {
            begingReadTransaction(pdh.getWCRoot());
            return SvnWcDbProperties.readProperties(pdh.getWCRoot(), localRelPath);
        } finally {
            commitTransaction(pdh.getWCRoot());
        }
    }

    private SVNSqlJetStatement getStatementForPath(File localAbsPath, SVNWCDbStatements statementIndex) throws SVNException {
        assert (isAbsolute(localAbsPath));
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        verifyDirUsable(parsed.wcDbDir);
        final SVNWCDbRoot wcRoot = parsed.wcDbDir.getWCRoot();
        final SVNSqlJetStatement statement = wcRoot.getSDb().getStatement(statementIndex);
        statement.bindf("is", wcRoot.getWcId(), SVNFileUtil.getFilePath(parsed.localRelPath));
        return statement;
    }

    public void removeBase(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        BaseRemove brb = new BaseRemove();
        brb.localRelpath = localRelpath;
        brb.wcId = pdh.getWCRoot().getWcId();
        brb.root = pdh.getWCRoot();
        pdh.getWCRoot().getSDb().runTransaction(brb);
        pdh.flushEntries(localAbsPath);
    }

    public void removeBase(File localAbsPath, boolean keepAsWorking, boolean queueDeletes, boolean removeLocks, long notPresentRevision, SVNSkel conflict, SVNSkel workItems) throws SVNException {
        assert (isAbsolute(localAbsPath));
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        BaseRemove brb = new BaseRemove();
        brb.localRelpath = localRelpath;
        brb.wcId = pdh.getWCRoot().getWcId();
        brb.root = pdh.getWCRoot();
        brb.keepAsWorking = keepAsWorking;
        brb.queueDeletes = queueDeletes;
        brb.removeLocks = removeLocks;
        brb.notPresentRevision = notPresentRevision;
        brb.conflict = conflict;
        brb.workItems = workItems;
        pdh.getWCRoot().getSDb().runTransaction(brb);
        pdh.flushEntries(localAbsPath);
    }

    public class BaseRemove implements SVNSqlJetTransaction {

        public SVNWCDbRoot root;
        public long wcId;
        public File localRelpath;

        public boolean keepAsWorking;
        private boolean queueDeletes;
        private boolean removeLocks;

        public SVNSkel conflict;
        public SVNSkel workItems;
        public long notPresentRevision;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            boolean keepWorking;

            WCDbBaseInfo baseInfo = getBaseInfo(root, localRelpath, BaseInfoField.status, BaseInfoField.kind, BaseInfoField.reposRelPath, BaseInfoField.reposId);
            if (baseInfo.status == SVNWCDbStatus.Normal && keepAsWorking) {
                opMakeCopy(SVNFileUtil.createFilePath(root.getAbsPath(), localRelpath), null, null);
                keepWorking = true;
            } else {
                SVNSqlJetStatement selectWorkingNodeStatement = db.getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);
                try {
                    selectWorkingNodeStatement.bindf("is", wcId, localRelpath);
                    keepWorking = selectWorkingNodeStatement.next();
                } finally {
                    selectWorkingNodeStatement.reset();
                }
            }

            if (removeLocks) {
                SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_LOCK_RECURSIVELY);
                try {
                    stmt.bindf("is", baseInfo.reposId, baseInfo.reposRelPath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            if (!keepWorking && queueDeletes && (baseInfo.status == SVNWCDbStatus.Normal || baseInfo.status == SVNWCDbStatus.Incomplete)) {
                File localAbsPath = SVNFileUtil.createFilePath(root.getAbsPath(), localRelpath);

                SVNSkel workItem;
                if (baseInfo.kind == SVNWCDbKind.Dir) {
                    SVNSqlJetStatement selectWorkingNodeStatement = db.getStatement(SVNWCDbStatements.SELECT_BASE_PRESENT);
                    try {
                        selectWorkingNodeStatement.bindf("is", wcId, localRelpath);
                        while (selectWorkingNodeStatement.next()) {
                            String nodeRelpath = selectWorkingNodeStatement.getColumnString(NODES__Fields.local_relpath);
                            SVNWCDbKind nodeKind = SvnWcDbStatementUtil.parseKind(selectWorkingNodeStatement.getColumnString(NODES__Fields.kind));

                            File nodeAbsPath = SVNFileUtil.createFilePath(root.getAbsPath(), nodeRelpath);

                            if (nodeKind == SVNWCDbKind.Dir) {
                                workItem = SVNWCContext.wqBuildDirRemove(SVNWCDb.this, root.getAbsPath(), nodeAbsPath, false);
                            } else {
                                workItem = SVNWCContext.wqBuildFileRemove(SVNWCDb.this, root.getAbsPath(), nodeAbsPath);
                            }
                            addWorkItems(root.getSDb(), workItem);

                        }
                    } finally {
                        selectWorkingNodeStatement.reset();
                    }

                    workItem = SVNWCContext.wqBuildDirRemove(SVNWCDb.this, root.getAbsPath(), localAbsPath, false);
                } else {
                    workItem = SVNWCContext.wqBuildFileRemove(SVNWCDb.this, root.getAbsPath(), localAbsPath);
                }

                addWorkItems(root.getSDb(), workItem);
            }

            if (!keepWorking) {
                SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_RECURSIVE);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            } else if (!keepAsWorking) {
                SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_ACTUAL_FOR_BASE_RECURSIVE);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            if (conflict != null) {
                SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_MOVED_OUTSIDE);
                try {
                    stmt.bindf("isi", wcId, localRelpath, SVNWCUtils.relpathDepth(localRelpath));
                    boolean haveRow = stmt.next();

                    while (haveRow) {
                        File childRelpath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.local_relpath));
                        clearMovedHere(childRelpath, root);
                        haveRow = stmt.next();
                    }
                } finally {
                    stmt.reset();
                }
            }

            if (keepWorking) {
                SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_WORKING_BASE_DELETE);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            } else {
                SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_WORKING_RECURSIVE);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_BASE_RECURSIVE);
            try {
                stmt.bindf("is", wcId, localRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }

            stmt = db.getStatement(SVNWCDbStatements.DELETE_BASE_NODE);
            try {
                stmt.bindf("is", wcId, localRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }

            retractParentDelete(db);

            if (!keepWorking) {
                stmt = db.getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE);
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            if (SVNRevision.isValidRevisionNumber(notPresentRevision)) {
                InsertBase ibb = new InsertBase();
                ibb.reposId = baseInfo.reposId;
                ibb.status = SVNWCDbStatus.NotPresent;
                ibb.kind = baseInfo.kind;
                ibb.reposRelpath = baseInfo.reposRelPath;
                ibb.revision = notPresentRevision;

                ibb.children = null;
                ibb.depth = SVNDepth.UNKNOWN;
                ibb.checksum = null;
                ibb.target = null;

                ibb.localRelpath = localRelpath;
                ibb.wcRoot = root;
                ibb.wcId = wcId;

                ibb.transaction(db);
            }

            addWorkItems(root.getSDb(), workItems);
            if (conflict != null) {
                markConflictInternal(root, localRelpath, conflict);
            }
        }

        private void retractParentDelete(SVNSqlJetDb db) throws SVNException {
            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_LOWEST_WORKING_NODE);
            try {
                stmt.bindf("is", wcId, localRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }

        private void clearMovedHere(File srcRelpath, SVNWCDbRoot wcRoot) throws SVNException {
            SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_MOVED_TO);
            File dstRelpath;
            try {
                stmt.bindf("isi", wcRoot.getWcId(), srcRelpath, SVNWCUtils.relpathDepth(srcRelpath));
                stmt.next();
                dstRelpath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.moved_to));
            } finally {
                stmt.reset();
            }

            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.CLEAR_MOVED_HERE_RECURSIVE);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), dstRelpath, SVNWCUtils.relpathDepth(dstRelpath));
                stmt.done();
            } finally {
                stmt.reset();
            }
        }
    }

    public void removeLock(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        WCDbRepositoryInfo reposInfo = new WCDbRepositoryInfo();
        long reposId = scanUpwardsForRepos(reposInfo, pdh.getWCRoot(), localRelpath);
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.DELETE_LOCK);
        try {
            stmt.bindf("is", reposId, reposInfo.relPath);
            stmt.done();
        } finally {
            stmt.reset();
        }
        pdh.flushEntries(localAbsPath);
    }

    public void removePristine(File wcRootAbsPath, SvnChecksum checksum) throws SVNException {
        assert (isAbsolute(wcRootAbsPath));
        assert (checksum != null);
        if (checksum.getKind() != SvnChecksum.Kind.sha1) {
            //i.e. checksum has kind "md5"
            checksum = getPristineSHA1(wcRootAbsPath, checksum);
        }
        assert (checksum.getKind() == SvnChecksum.Kind.sha1);
        DirParsedInfo parseDir = parseDir(wcRootAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);

        SvnWcDbPristines.removePristine(pdh.getWCRoot(), checksum);
    }

    public WCDbAdditionInfo scanAddition(File localAbsPath, AdditionInfoField... fields) throws SVNException {
        assert (isAbsolute(localAbsPath));
        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        EnumSet<AdditionInfoField> f = getInfoFields(AdditionInfoField.class, fields);
        File buildRelpath = SVNFileUtil.createFilePath("");

        WCDbAdditionInfo additionInfo = new WCDbAdditionInfo();
        additionInfo.originalRevision = INVALID_REVNUM;
        long originalReposId = INVALID_REPOS_ID;
        additionInfo.movedFromRelPath = null;
        additionInfo.movedFromOpRootRelPath = null;
        additionInfo.movedFromOpDepth = 0;


        File currentRelpath = localRelpath;

        boolean haveRow;
        SVNWCDbStatus presence;
        File reposPrefixPath = SVNFileUtil.createFilePath("");
        int i;

        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);

        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            haveRow = stmt.next();

            if (!haveRow) {
                stmt.reset();
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            presence = getColumnPresence(stmt);

            if (presence != SVNWCDbStatus.Normal) {
                stmt.reset();
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Expected node ''{0}'' to be added.", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            if (f.contains(AdditionInfoField.originalRevision)) {
                additionInfo.originalRevision = getColumnRevNum(stmt, NODES__Fields.revision);
            }

            if (f.contains(AdditionInfoField.status)) {
                additionInfo.status = SVNWCDbStatus.Added;
            }

            long opDepth = stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);

            for (i = SVNWCUtils.relpathDepth(localRelpath); i > opDepth; --i) {
                reposPrefixPath = SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(SVNFileUtil.getFileName(currentRelpath)), reposPrefixPath);
                currentRelpath = SVNFileUtil.getFileDir(currentRelpath);
            }

            if (f.contains(AdditionInfoField.opRootAbsPath)) {
                additionInfo.opRootAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), currentRelpath);
            }

            if (f.contains(AdditionInfoField.originalReposRelPath) || f.contains(AdditionInfoField.originalReposId) || f.contains(AdditionInfoField.originalRootUrl) || f.contains(AdditionInfoField.originalUuid) || (f.contains(AdditionInfoField.originalRevision) && additionInfo.originalRevision == INVALID_REVNUM) ||
                    f.contains(AdditionInfoField.status) || f.contains(AdditionInfoField.movedFromRelPath) || f.contains(AdditionInfoField.movedFromOpRootRelPath)) {
                if (!localRelpath.equals(currentRelpath)) {
                    stmt.reset();
                    stmt.bindf("is", pdh.getWCRoot().getWcId(), currentRelpath);
                    haveRow = stmt.next();
                    if (!haveRow) {
                        stmt.reset();
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.",
                                SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), currentRelpath));
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }

                    if (f.contains(AdditionInfoField.originalRevision) && additionInfo.originalRevision == INVALID_REVNUM)
                        additionInfo.originalRevision = getColumnRevNum(stmt, NODES__Fields.revision);
                }

                /*
                 * current_relpath / current_abspath as well as the record in
                 * stmt contain the data of the op_root
                 */
                if (f.contains(AdditionInfoField.originalReposRelPath)) {
                    additionInfo.originalReposRelPath = getColumnPath(stmt, NODES__Fields.repos_path);
                }

                if (!isColumnNull(stmt, NODES__Fields.repos_id) && (f.contains(AdditionInfoField.status) || f.contains(AdditionInfoField.originalReposId) || f.contains(AdditionInfoField.movedFromRelPath) || f.contains(AdditionInfoField.movedFromOpRootRelPath))) {
                    if (f.contains(AdditionInfoField.originalReposId)) {
                        originalReposId = getColumnInt64(stmt, NODES__Fields.repos_id);
                        ReposInfo reposInfo = fetchReposInfo(pdh.getWCRoot().getSDb(), originalReposId);
                        additionInfo.originalRootUrl = SVNURL.parseURIEncoded(reposInfo.reposRootUrl);
                        additionInfo.originalUuid = reposInfo.reposUuid;
                        additionInfo.originalReposId = originalReposId;
                    }

                    boolean movedHere = getColumnBoolean(stmt, NODES__Fields.moved_here);

                    if (f.contains(AdditionInfoField.status)) {
                        if (getColumnBoolean(stmt, NODES__Fields.moved_here)) {
                            additionInfo.status = SVNWCDbStatus.MovedHere;
                        } else {
                            additionInfo.status = SVNWCDbStatus.Copied;
                        }
                    }

                    if (movedHere && (f.contains(AdditionInfoField.movedFromRelPath) || f.contains(AdditionInfoField.movedFromOpRootRelPath))) {
                        Structure<StructureFields.MovedFromInfo> movedFromInfo = getMovedFromInfo(pdh.getWCRoot(), currentRelpath, localRelpath);
                        currentRelpath = movedFromInfo.get(StructureFields.MovedFromInfo.movedFromOpRootRelPath);
                        additionInfo.movedFromOpRootRelPath = currentRelpath;
                        additionInfo.movedFromRelPath = movedFromInfo.get(StructureFields.MovedFromInfo.movedFromRelPath);
                        additionInfo.movedFromOpDepth = (int) movedFromInfo.lng(StructureFields.MovedFromInfo.opDepth);
                    }
                }
            }

            while (true) {
                stmt.reset();
                reposPrefixPath = SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(SVNFileUtil.getFileName(currentRelpath)), reposPrefixPath);
                currentRelpath = SVNFileUtil.getFileDir(currentRelpath);
                stmt.bindf("is", pdh.getWCRoot().getWcId(), currentRelpath);
                haveRow = stmt.next();
                if (!haveRow) {
                    break;
                }
                opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                for (i = SVNWCUtils.relpathDepth(currentRelpath); i > opDepth; i--) {
                    reposPrefixPath = SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(SVNFileUtil.getFileName(currentRelpath)), reposPrefixPath);
                    currentRelpath = SVNFileUtil.getFileDir(currentRelpath);
                }
            }

        } finally {
            stmt.reset();
        }

        buildRelpath = reposPrefixPath;

        if (f.contains(AdditionInfoField.originalRootUrl) || f.contains(AdditionInfoField.originalUuid)) {
            originalReposId = additionInfo.originalReposId;
        }

        if (f.contains(AdditionInfoField.reposRelPath) || f.contains(AdditionInfoField.reposRootUrl) || f.contains(AdditionInfoField.reposUuid)) {
            WCDbRepositoryInfo rInfo = new WCDbRepositoryInfo();
            long reposId = scanUpwardsForRepos(rInfo, pdh.getWCRoot(), currentRelpath);
            if (f.contains(AdditionInfoField.reposRelPath)) {
                additionInfo.reposRelPath = SVNFileUtil.createFilePath(rInfo.relPath, buildRelpath);
            }
            if (reposId != INVALID_REPOS_ID && f.contains(AdditionInfoField.reposRootUrl) || f.contains(AdditionInfoField.reposUuid)) {
                ReposInfo reposInfo = fetchReposInfo(pdh.getWCRoot().getSDb(), reposId);
                if (reposInfo.reposRootUrl != null) {
                    additionInfo.reposRootUrl = SVNURL.parseURIEncoded(reposInfo.reposRootUrl);
                }
                additionInfo.reposUuid = reposInfo.reposUuid;
            }
        }

        if (originalReposId != INVALID_REPOS_ID && f.contains(AdditionInfoField.originalRootUrl) || f.contains(AdditionInfoField.originalUuid)) {
            ReposInfo reposInfo = fetchReposInfo(pdh.getWCRoot().getSDb(), originalReposId);
            if (reposInfo.reposRootUrl != null) {
                additionInfo.originalRootUrl = SVNURL.parseURIEncoded(reposInfo.reposRootUrl);
            }
            additionInfo.originalUuid = reposInfo.reposUuid;
        }

        return additionInfo;
    }

    public WCDbRepositoryInfo scanBaseRepository(File localAbsPath, RepositoryInfoField... fields) throws SVNException {
        assert (isAbsolute(localAbsPath));
        final EnumSet<RepositoryInfoField> f = getInfoFields(RepositoryInfoField.class, fields);
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        WCDbBaseInfo baseInfo = getBaseInfo(localAbsPath, BaseInfoField.reposId, BaseInfoField.reposRelPath);
        final WCDbRepositoryInfo reposInfo = new WCDbRepositoryInfo();
        reposInfo.relPath = baseInfo.reposRelPath;
        if (f.contains(RepositoryInfoField.rootUrl) || f.contains(RepositoryInfoField.uuid)) {
            fetchReposInfo(reposInfo, pdh.getWCRoot().getSDb(), baseInfo.reposId);
        }
        return reposInfo;
    }

    /**
     * Scan from LOCAL_RELPATH upwards through parent nodes until we find a
     * parent that has values in the 'repos_id' and 'repos_relpath' columns.
     * Return that information in REPOS_ID and REPOS_RELPATH (either may be
     * NULL). Use LOCAL_ABSPATH for diagnostics
     */
    private static long scanUpwardsForRepos(WCDbRepositoryInfo reposInfo, SVNWCDbRoot wcroot, File localRelPath) throws SVNException {
        assert (wcroot.getSDb() != null && wcroot.getWcId() != UNKNOWN_WC_ID);
        assert (reposInfo != null);
        SVNSqlJetStatement stmt = wcroot.getSDb().getStatement(SVNWCDbStatements.SELECT_BASE_NODE);
        try {
            stmt.bindf("is", wcroot.getWcId(), localRelPath);
            boolean haveRow = stmt.next();
            if (!haveRow) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", SVNFileUtil.createFilePath(wcroot.getAbsPath(), localRelPath));
                SVNErrorManager.error(err, SVNLogType.WC);
                return 0;
            }
            assert (!stmt.isColumnNull(SVNWCDbSchema.NODES__Fields.repos_id));
            assert (!stmt.isColumnNull(SVNWCDbSchema.NODES__Fields.repos_path));
            reposInfo.relPath = SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.NODES__Fields.repos_path));
            return stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.repos_id);
        } finally {
            stmt.reset();
        }
    }

    private static void fetchReposInfo(WCDbRepositoryInfo reposInfo, SVNSqlJetDb sdb, long reposId) throws SVNException {
        final SVNSqlJetStatement stmt = sdb.getStatement(SVNWCDbStatements.SELECT_REPOSITORY_BY_ID);
        try {
            stmt.bindf("i", reposId);
            boolean have_row = stmt.next();
            if (!have_row) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, "No REPOSITORY table entry for id ''{0}''", reposId);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            reposInfo.rootUrl = !isColumnNull(stmt, REPOSITORY__Fields.root) ? SVNURL.parseURIEncoded(getColumnText(stmt, REPOSITORY__Fields.root)) : null;
            reposInfo.uuid = getColumnText(stmt, REPOSITORY__Fields.uuid);
        } finally {
            stmt.reset();
        }
    }

    public WCDbDeletionInfo scanDeletion(File localAbsPath, DeletionInfoField... fields) throws SVNException {
        assert (isAbsolute(localAbsPath));

        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File currentRelPath = parsed.localRelPath;
        verifyDirUsable(pdh);

        final EnumSet<DeletionInfoField> f = getInfoFields(DeletionInfoField.class, fields);

        /* Initialize all the OUT parameters. */
        final WCDbDeletionInfo deletionInfo = new WCDbDeletionInfo();


        boolean scan = f.contains(DeletionInfoField.movedToOpRootAbsPath) || f.contains(DeletionInfoField.movedToAbsPath);
        SVNWCDbSelectDeletionInfo stmt = (SVNWCDbSelectDeletionInfo) pdh.getWCRoot().getSDb().getStatement(scan ? SVNWCDbStatements.SELECT_DELETION_INFO_SCAN : SVNWCDbStatements.SELECT_DELETION_INFO);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), parsed.localRelPath);
            boolean haveRow = stmt.next();
            if (!haveRow) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", localAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            SVNWCDbStatus workPresence = SvnWcDbStatementUtil.getColumnPresence(stmt, NODES__Fields.presence);
            boolean haveBase = !stmt.getInternalStatement().isColumnNull(NODES__Fields.presence);
            if (workPresence != SVNWCDbStatus.NotPresent && workPresence != SVNWCDbStatus.BaseDeleted) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Expected node ''{0}'' to be deleted.", localAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            long opDepth = stmt.getColumnLong(NODES__Fields.op_depth);
            if (workPresence == SVNWCDbStatus.NotPresent && f.contains(DeletionInfoField.baseDelAbsPath)) {
                deletionInfo.workDelAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), currentRelPath);
                if (!scan && !f.contains(DeletionInfoField.baseDelAbsPath)) {
                    return deletionInfo;
                }
            }

            while (true) {
                int currentDepth = SVNWCUtils.relpathDepth(currentRelPath);

                while (true) {
                    if (scan) {
                        MovedTo movedTo = getMovedTo(scan, stmt, currentRelPath, pdh.getWCRoot(), parsed.localRelPath);
                        scan = movedTo.scan;
                        deletionInfo.movedToAbsPath = movedTo.movedToRelPath == null ? null : SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), movedTo.movedToRelPath);
                        deletionInfo.movedToOpRootAbsPath = movedTo.movedToOpRootRelPath == null ? null : SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), movedTo.movedToOpRootRelPath);

                        if (!scan && !f.contains(DeletionInfoField.baseDelAbsPath) && !f.contains(DeletionInfoField.workDelAbsPath)) {
                            return deletionInfo;
                        }
                    }

                    if (currentDepth <= opDepth) {
                        break;
                    }

                    currentRelPath = SVNFileUtil.getFileDir(currentRelPath);
                    currentDepth--;

                    if (scan || currentDepth == opDepth) {
                        stmt.reset();
                        stmt.bindf("is", pdh.getWCRoot().getWcId(), currentRelPath);
                        haveRow = stmt.next();
                        assert haveRow;
                        haveBase = !stmt.getInternalStatement().isColumnNull(NODES__Fields.presence);
                    }
                }
                stmt.reset();

                assert SVNFileUtil.getFilePath(currentRelPath).length() != 0;
                File parentRelPath = SVNFileUtil.getFileDir(currentRelPath);
                stmt.bindf("is", pdh.getWCRoot().getWcId(), parentRelPath);
                haveRow = stmt.next();
                if (!haveRow) {
                    if (haveBase && f.contains(DeletionInfoField.baseDelAbsPath)) {
                        deletionInfo.baseDelAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), currentRelPath);
                    }
                    break;
                }

                if (f.contains(DeletionInfoField.workDelAbsPath) && deletionInfo.workDelAbsPath == null) {
                    deletionInfo.workDelAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), currentRelPath);
                    if (!scan && !f.contains(DeletionInfoField.baseDelAbsPath)) {
                        break;
                    }
                }

                currentRelPath = parentRelPath;
                opDepth = stmt.getColumnLong(NODES__Fields.op_depth);
                haveBase = !stmt.getInternalStatement().isColumnNull(NODES__Fields.presence);
            }
        } finally {
            stmt.reset();
        }

        return deletionInfo;
    }

    public MovedTo getMovedTo(boolean scan, SVNWCDbSelectDeletionInfo stmt, File currentRelPath, SVNWCDbRoot wcRoot, File localRelPath) throws SVNException {
        MovedTo movedTo = new MovedTo();
        movedTo.scan = scan;

        File movedToRelPath = SvnWcDbStatementUtil.getColumnPath(stmt.getJoinedStatement(SVNWCDbSchema.NODES), NODES__Fields.moved_to);
        if (movedToRelPath != null) {
            File movedToOpRootRelPath = movedToRelPath;
            if (!localRelPath.equals(currentRelPath)) {
                File movedChildRelPath = SVNFileUtil.skipAncestor(currentRelPath, localRelPath);
                assert movedChildRelPath != null && SVNFileUtil.getFilePath(movedChildRelPath).length() > 0;
                movedToRelPath = SVNFileUtil.createFilePath(movedToOpRootRelPath, movedChildRelPath);
            }
            if (movedToOpRootRelPath != null) {
                movedTo.movedToOpRootRelPath = movedToOpRootRelPath;
            }
            if (movedToRelPath != null) {
                movedTo.movedToRelPath = movedToRelPath;
            }
            movedTo.scan = false;
        }
        return movedTo;
    }

    private static class MovedTo {
        public File movedToRelPath;
        public File movedToOpRootRelPath;
        public boolean scan;
    }

    public SVNWCDbDir navigateToParent(SVNWCDbDir childPdh, Mode sMode) throws SVNException {
        SVNWCDbDir parentPdh = childPdh.getParent();
        if (parentPdh != null && parentPdh.getWCRoot() != null)
            return parentPdh;
        File parentAbsPath = SVNFileUtil.getParentFile(childPdh.getLocalAbsPath());
        /* Make sure we don't see the root as its own parent */
        assert (parentAbsPath != null);
        parentPdh = parseDir(parentAbsPath, sMode).wcDbDir;
        verifyDirUsable(parentPdh);
        childPdh.setParent(parentPdh);
        return parentPdh;
    }

    public void setBaseDavCache(File localAbsPath, SVNProperties props) throws SVNException {
        assert (isAbsolute(localAbsPath));

        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        SvnWcDbShared.begingWriteTransaction(pdh.getWCRoot());
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.UPDATE_BASE_NODE_DAV_CACHE);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), parsed.localRelPath);
            stmt.bindProperties(3, props);
            stmt.exec();
        } catch (SVNException e) {
            SvnWcDbShared.rollbackTransaction(pdh.getWCRoot());
            throw e;
        } finally {
            stmt.reset();
            SvnWcDbShared.commitTransaction(pdh.getWCRoot());
        }
    }

    public File toRelPath(File wriAbsPath, File localAbsPath) throws SVNException {
        assert isAbsolute(localAbsPath);
        DirParsedInfo parsed = parseDir(wriAbsPath, Mode.ReadOnly);
        File rootAbsPath = parsed.wcDbDir.getWCRoot().getAbsPath();
        return SVNPathUtil.isAncestor(rootAbsPath.getPath(), localAbsPath.getPath()) ?
                SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(rootAbsPath.getPath(), localAbsPath.getPath())) :
                localAbsPath;
    }

    public File toRelPath(File localAbsPath) throws SVNException {
        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        return parsed.localRelPath;
    }

    public String getFileExternalTemp(File path) throws SVNException {
        final SVNSqlJetStatement stmt = getStatementForPath(path, SVNWCDbStatements.SELECT_FILE_EXTERNAL);
        try {
            boolean have_row = stmt.next();
            /*
             * ### file externals are pretty bogus right now. they have just a
             * ### WORKING_NODE for a while, eventually settling into just a
             * BASE_NODE. ### until we get all that fixed, let's just not worry
             * about raising ### an error, and just say it isn't a file
             * external.
             */
            if (!have_row)
                return null;
            /* see below: *serialized_file_external = ... */
            return getColumnText(stmt, NODES__Fields.file_external);
        } finally {
            stmt.reset();
        }
    }

    public void cleanupPristine(File localAbsPath) throws SVNException {
    	assert (isAbsolute(localAbsPath));

        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        SvnWcDbPristines.cleanupPristine(pdh.getWCRoot(), localAbsPath);
    }

    private long fetchWCId(SVNSqlJetDb sDb) throws SVNException {
        /*
         * ### cheat. we know there is just one WORKING_COPY row, and it has a
         * ### NULL value for local_abspath.
         */
        final SVNSqlJetStatement stmt = sDb.getStatement(SVNWCDbStatements.SELECT_WCROOT_NULL);
        try {
            final boolean have_row = stmt.next();
            if (!have_row) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, "Missing a row in WCROOT.");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            // assert (!stmt.isColumnNull("id"));
            return getColumnInt64(stmt, SVNWCDbSchema.WCROOT__Fields.id);
        } finally {
            stmt.reset();
        }
    }

    public static class ReposInfo {

        public String reposRootUrl;
        public String reposUuid;
    }

    public ReposInfo fetchReposInfo(SVNSqlJetDb sDb, long repos_id) throws SVNException {

        ReposInfo info = new ReposInfo();
        if (repos_id == INVALID_REPOS_ID) {
            return info;
        }

        SVNSqlJetStatement stmt;
        boolean have_row;

        stmt = sDb.getStatement(SVNWCDbStatements.SELECT_REPOSITORY_BY_ID);
        try {
            stmt.bindf("i", repos_id);
            have_row = stmt.next();
            if (!have_row) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, "No REPOSITORY table entry for id ''{0}''", repos_id);
                SVNErrorManager.error(err, SVNLogType.WC);
                return info;
            }

            info.reposRootUrl = getColumnText(stmt, SVNWCDbSchema.REPOSITORY__Fields.root);
            info.reposUuid = getColumnText(stmt, SVNWCDbSchema.REPOSITORY__Fields.uuid);

        } finally {
            stmt.reset();
        }
        return info;
    }

    public Structure<RepositoryInfo> fetchRepositoryInfo(SVNSqlJetDb sDb, long repos_id) throws SVNException {
        Structure<RepositoryInfo> info = Structure.obtain(RepositoryInfo.class);
        if (repos_id == INVALID_REPOS_ID) {
            return info;
        }

        SVNSqlJetStatement stmt;
        boolean have_row;

        stmt = sDb.getStatement(SVNWCDbStatements.SELECT_REPOSITORY_BY_ID);
        try {
            stmt.bindf("i", repos_id);
            have_row = stmt.next();
            if (!have_row) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, "No REPOSITORY table entry for id ''{0}''", repos_id);
                SVNErrorManager.error(err, SVNLogType.WC);
                return info;
            }

            info.set(RepositoryInfo.reposRootUrl, SVNURL.parseURIEncoded(getColumnText(stmt, SVNWCDbSchema.REPOSITORY__Fields.root)));
            info.set(RepositoryInfo.reposUuid, getColumnText(stmt, SVNWCDbSchema.REPOSITORY__Fields.uuid));
        } finally {
            stmt.reset();
        }

        return info;
    }

    private static SVNSqlJetDb openDb(File dirAbsPath, String sdbFileName, Mode sMode, SqlJetPagerJournalMode journalMode, boolean temporaryDbInMemory) throws SVNException {
        if (dirAbsPath == null || sdbFileName == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return SVNSqlJetDb.open(SVNWCUtils.admChild(dirAbsPath, sdbFileName), sMode, journalMode, temporaryDbInMemory);
    }

    protected static void verifyDirUsable(SVNWCDbDir pdh) throws SVNException {
        if (!SVNWCDbDir.isUsable(pdh)) {
            if (pdh != null && pdh.getWCRoot() != null && pdh.getWCRoot().getFormat() < ISVNWCDb.WC_FORMAT_17) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_UNSUPPORTED_FORMAT);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            assert (false);
        }
    }

    public SVNSqlJetDb borrowDbTemp(File localDirAbsPath, SVNWCDbOpenMode mode) throws SVNException {
        assert (isAbsolute(localDirAbsPath));
        final Mode smode = mode == SVNWCDbOpenMode.ReadOnly ? Mode.ReadOnly : Mode.ReadWrite;
        final DirParsedInfo parsed = parseDir(localDirAbsPath, smode);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        return pdh.getWCRoot().getSDb();
    }

    public boolean isWCRoot(File localAbspath) throws SVNException {
        return isWCRoot(localAbspath, false);
    }
    public boolean isWCRoot(File localAbspath, boolean isAdditionMode) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite, false, isAdditionMode);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelPath = parsed.localRelPath;
        verifyDirUsable(pdh);
        if (localRelPath != null && !localRelPath.getPath().equals("")) {
            /* Node is a file, or has a parent directory within the same wcroot */
            return false;
        }
        return true;
    }

    public void opStartDirectoryUpdateTemp(File localAbspath, File newReposRelpath, long newRevision) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        assert (SVNRevision.isValidRevisionNumber(newRevision));
        // SVN_ERR_ASSERT(svn_relpath_is_canonical(new_repos_relpath,
        // scratch_pool));
        DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelPath = parsed.localRelPath;
        verifyDirUsable(pdh);
        final StartDirectoryUpdate du = new StartDirectoryUpdate();
        du.wcId = pdh.getWCRoot().getWcId();
        du.newRevision = newRevision;
        du.newReposRelpath = newReposRelpath;
        du.localRelpath = localRelPath;
        pdh.getWCRoot().getSDb().runTransaction(du);
        pdh.flushEntries(localAbspath);
    }

    private class StartDirectoryUpdate implements SVNSqlJetTransaction {

        public long wcId;
        public File localRelpath;
        public long newRevision;
        public File newReposRelpath;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.UPDATE_BASE_NODE_PRESENCE_REVNUM_AND_REPOS_PATH);
            try {
                stmt.bindf("istis", wcId, localRelpath, getPresenceText(SVNWCDbStatus.Incomplete), newRevision, newReposRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }

    }

    public void opMakeCopyTemp(File localAbspath, boolean removeBase) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelPath = parsed.localRelPath;
        verifyDirUsable(pdh);
        boolean haveRow = false;
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelPath);
            haveRow = stmt.next();
        } finally {
            stmt.reset();
        }
        if (haveRow) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Modification of ''{0}'' already exists", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        catchCopyOfAbsent(pdh, localRelPath);
        final MakeCopy mcb = new MakeCopy();
        mcb.pdh = pdh;
        mcb.localRelpath = localRelPath;
        mcb.localAbspath = localAbspath;
        mcb.opDepth = SVNWCUtils.relpathDepth(localRelPath);
        pdh.getWCRoot().getSDb().runTransaction(mcb);
        pdh.flushEntries(localAbspath);
    }

    private void catchCopyOfAbsent(SVNWCDbDir pdh, File localRelPath) throws SVNException {
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.HAS_SERVER_EXCLUDED_NODES);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelPath);
            if (stmt.next()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.AUTHZ_UNREADABLE,
                        "Cannot copy ''{0}'', excluded by server", getColumnPath(stmt, NODES__Fields.local_relpath));
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } finally {
            stmt.reset();
        }
    }

    private void catchCopyOfServerExcluded(SVNWCDbRoot wcRoot, File localRelpath) throws SVNException {
        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.HAS_SERVER_EXCLUDED_DESCENDANTS);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelpath);
            boolean haveRow = stmt.next();
            if (haveRow) {
                File serverExcludedRelpath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.local_relpath));
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.AUTHZ_UNREADABLE,
                        "Cannot copy '{{0}}' excluded by server",
                        SVNFileUtil.createFilePath(wcRoot.getAbsPath(), serverExcludedRelpath));
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        } finally {
            stmt.reset();
        }
    }

    private class MakeCopy implements SVNSqlJetTransaction {

        File localAbspath;
        SVNWCDbDir pdh;
        File localRelpath;
        long opDepth;
        SVNSkel conflicts;
        SVNSkel workItems;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNSqlJetStatement stmt;
            boolean haveRow;
            boolean removeWorking = false;
            boolean addWorkingBaseDeleted = false;
            stmt = db.getStatement(SVNWCDbStatements.SELECT_LOWEST_WORKING_NODE);
            stmt.bindf("isi", pdh.getWCRoot().getWcId(), localRelpath, 0);
            try {
                haveRow = stmt.next();
                if (haveRow) {
                    SVNWCDbStatus workingStatus = getColumnPresence(stmt);
                    long workingOpDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                    assert (workingStatus == SVNWCDbStatus.Normal || workingStatus == SVNWCDbStatus.BaseDeleted || workingStatus == SVNWCDbStatus.NotPresent || workingStatus == SVNWCDbStatus.Incomplete);
                    if (workingOpDepth <= opDepth) {
                        addWorkingBaseDeleted = true;
                        if (workingStatus == SVNWCDbStatus.BaseDeleted) {
                            removeWorking = true;
                        }
                    }
                }
            } finally {
                stmt.reset();
            }
            if (removeWorking) {
                stmt = db.getStatement(SVNWCDbStatements.DELETE_LOWEST_WORKING_NODE);
                try {
                    stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }
            if (addWorkingBaseDeleted) {
                stmt = db.getStatement(SVNWCDbStatements.INSERT_DELETE_FROM_BASE);
                try {
                    stmt.bindf("isi", pdh.getWCRoot().getWcId(), localRelpath, opDepth);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            } else {
                stmt = db.getStatement(SVNWCDbStatements.INSERT_WORKING_NODE_FROM_BASE_COPY);
                try {
                    stmt.bindf("isi", pdh.getWCRoot().getWcId(), localRelpath, opDepth);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            final List<String> children = gatherRepoChildren(pdh.getWCRoot(), localRelpath, 0);
            for (String name : children) {
                MakeCopy cbt = new MakeCopy();
                cbt.localAbspath = SVNFileUtil.createFilePath(localAbspath, name);
                DirParsedInfo parseDir = parseDir(cbt.localAbspath, Mode.ReadWrite);
                cbt.pdh = parseDir.wcDbDir;
                cbt.localRelpath = parseDir.localRelPath;
                verifyDirUsable(cbt.pdh);
                cbt.opDepth = opDepth;
                cbt.transaction(db);
            }

            pdh.flushEntries(localAbspath);

            if (conflicts != null) {
                markConflictInternal(pdh.getWCRoot(), localRelpath, conflicts);
            }

            addWorkItems(pdh.getWCRoot().getSDb(), workItems);
        }

    };

    public List<String> gatherRepoChildren(SVNWCDbRoot root, File localRelpath, long opDepth) throws SVNException {
        final List<String> children = new ArrayList<String>();
        final SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_OP_DEPTH_CHILDREN);
        try {
            stmt.bindf("isi", root.getWcId(), localRelpath, opDepth);
            boolean haveRow = stmt.next();
            while (haveRow) {
                String childRelpath = SVNFileUtil.getFileName(SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath)));
                children.add(childRelpath);
                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }
        return children;
    }

    public long fetchReposId(SVNSqlJetDb db, SVNURL reposRootUrl, String reposUuid) throws SVNException {
        SVNSqlJetStatement getStmt = db.getStatement(SVNWCDbStatements.SELECT_REPOSITORY);
        try {
            getStmt.bindf("s", reposRootUrl);
            getStmt.nextRow();
            return getStmt.getColumnLong(SVNWCDbSchema.REPOSITORY__Fields.id);
        } finally {
            getStmt.reset();
        }
    }

    public void opSetNewDirToIncompleteTemp(File localAbspath, File reposRelpath, SVNURL reposRootURL, String reposUuid, long revision, SVNDepth depth,
                                            boolean insertBaseDeleted, boolean deleteWorking, SVNSkel conflict, SVNSkel workItems) throws SVNException {

        assert (SVNFileUtil.isAbsolute(localAbspath));
        assert (SVNRevision.isValidRevisionNumber(revision));
        assert (reposRelpath != null && reposRootURL != null && reposUuid != null);
        DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        final InsertBase insertBase = new InsertBase();
        insertBase.reposRootURL = reposRootURL;
        insertBase.reposUUID = reposUuid;
        insertBase.status = SVNWCDbStatus.Incomplete;
        insertBase.kind = SVNWCDbKind.Dir;
        insertBase.reposRelpath = reposRelpath;
        insertBase.revision = revision;
        insertBase.depth = depth;

        insertBase.localRelpath = parsed.localRelPath;
        insertBase.wcId = pdh.getWCRoot().getWcId();
        insertBase.wcRoot = pdh.getWCRoot();

        insertBase.insertBaseDeleted = insertBaseDeleted;
        insertBase.deleteWorking = deleteWorking;
        insertBase.conflict = conflict;
        insertBase.workItems = workItems;

        pdh.getWCRoot().getSDb().runTransaction(insertBase);
        pdh.flushEntries(localAbspath);
    }

    public void opBumpRevisionPostUpdate(File localAbsPath, SVNDepth depth, File newReposRelPath, SVNURL newReposRootURL, String newReposUUID,
            long newRevision, Collection<File> excludedPaths, Map<File, Map<String, SVNProperties>> inheritableProperties, ISVNEventHandler eventHandler) throws SVNException {
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);
        File localRelPath = parseDir.localRelPath;
        if (excludedPaths != null && excludedPaths.contains(localRelPath)) {
            return;
        }
        if (depth == SVNDepth.UNKNOWN) {
            depth = SVNDepth.INFINITY;
        }
        BumpRevisionPostUpdate brb = new BumpRevisionPostUpdate();

        brb.depth = depth;
        brb.newReposRelPath = newReposRelPath;
        brb.newReposRootURL = newReposRootURL;
        brb.newReposUUID = newReposUUID;
        brb.newRevision = newRevision;

        brb.localRelPath = localRelPath;
        brb.wcRoot = pdh.getWCRoot().getAbsPath();
        brb.exludedRelPaths = excludedPaths;
        brb.dbWcRoot = pdh.getWCRoot();
        brb.iprops = inheritableProperties;
        brb.eventHandler = eventHandler;

        pdh.getWCRoot().getSDb().runTransaction(brb);
        pdh.flushEntries(localAbsPath);
    }

    private class BumpRevisionPostUpdate implements SVNSqlJetTransaction {

        public Map<File, Map<String, SVNProperties>> iprops;
        private SVNDepth depth;
        private File newReposRelPath;
        private SVNURL newReposRootURL;
        private String newReposUUID;
        private long newRevision;
        private Collection<File> exludedRelPaths;

        private File localRelPath;
        private File wcRoot;
        private SVNWCDbRoot dbWcRoot;
        private ISVNEventHandler eventHandler;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNWCDbStatus status = null;
            WCDbBaseInfo baseInfo = null;
            try {
                baseInfo = getBaseInfo(dbWcRoot, localRelPath,
                        BaseInfoField.status, BaseInfoField.kind, BaseInfoField.reposRelPath, BaseInfoField.updateRoot,
                        BaseInfoField.revision);
                status = baseInfo.status;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    return;
                }
                throw e;
            }
            switch (status) {
            case NotPresent:
            case ServerExcluded:
            case Excluded:
                return;
            default:
                break;
            }

            long reposId = INVALID_REPOS_ID;
            if (newReposRootURL != null) {
                reposId = createReposId(db, newReposRootURL, newReposUUID);
            }
            bumpNodeRevision(dbWcRoot, wcRoot, localRelPath, reposId, newReposRelPath, newRevision, depth, exludedRelPaths, true, false);

            bumpMovedAway(dbWcRoot, localRelPath, depth, dbWcRoot.getDb());
            updateMoveListNotify(dbWcRoot, -1, -1, eventHandler);
        }

        private void bumpMovedAway(SVNWCDbRoot wcRoot, File localRelPath, SVNDepth depth, SVNWCDb db) throws SVNException {
            SVNSqlJetStatement createUpdateMoveList = new SVNWCDbCreateSchema(wcRoot.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.CREATE_UPDATE_MOVE_LIST, -1, false);
            try {
                createUpdateMoveList.done();
            } finally {
                createUpdateMoveList.reset();
            }

            BaseMovedTo baseMovedTo = db.opDepthMovedTo(0, wcRoot, localRelPath);
            File moveDstOpRootRelPath = baseMovedTo.moveDstOpRootRelPath;
            File moveSrcRootRelPath = baseMovedTo.moveSrcRootRelPath;
            File moveSrcOpRootRelPath = baseMovedTo.moveSrcOpRootRelPath;

            if (moveSrcRootRelPath != null) {
                if (!moveSrcRootRelPath.equals(localRelPath)) {
                    bumpMarkTreeConflict(wcRoot, moveSrcRootRelPath, moveSrcOpRootRelPath, moveDstOpRootRelPath);
                    return;
                }
            }

            Set<File> srcDone = new HashSet<File>();
            bumpMovedAway(wcRoot, localRelPath, 0, srcDone, depth, db);
        }

        private void bumpMovedAway(SVNWCDbRoot wcRoot, File localRelPath, int opDepth, Set<File> srcDone, SVNDepth depth, ISVNWCDb db) throws SVNException {
            SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_MOVED_PAIR3);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), localRelPath, opDepth);
                boolean haveRow = stmt.next();
                while (haveRow) {
                    int srcOpDepth = (int) stmt.getColumnLong(NODES__Fields.op_depth);
                    SVNDepth srcDepth = depth;

                    File srcRelPath = SvnWcDbStatementUtil.getColumnPath(stmt, NODES__Fields.local_relpath);
                    File dstRelPath = SvnWcDbStatementUtil.getColumnPath(stmt, NODES__Fields.moved_to);

                    if (depth != SVNDepth.INFINITY) {
                        boolean skipThisSrc = false;

                        if (!srcRelPath.equals(localRelPath)) {
                            if (depth == SVNDepth.EMPTY) {
                                skipThisSrc = true;
                            } else if (depth == SVNDepth.FILES && (SvnWcDbStatementUtil.getColumnKind(stmt, NODES__Fields.kind) != SVNWCDbKind.File)) {
                                skipThisSrc = true;
                            } else if (depth == SVNDepth.IMMEDIATES) {
                                if (!SVNFileUtil.getFileDir(srcRelPath).equals(localRelPath)) {
                                    skipThisSrc = true;
                                }
                                srcDepth = SVNDepth.EMPTY;
                            } else {
                                SVNErrorManager.assertionFailure(false, null, SVNLogType.WC);
                            }
                        }

                        if (skipThisSrc) {
                            haveRow = stmt.next();
                            continue;
                        }
                    }

                    SVNSqlJetStatement stmt2 = wcRoot.getSDb().getStatement(SVNWCDbStatements.HAS_LAYER_BETWEEN);
                    try {
                        stmt2.bindf("isii", wcRoot.getWcId(), localRelPath, opDepth, srcOpDepth);
                        haveRow = stmt2.next();
                    } finally {
                        stmt2.reset();
                    }

                    if (!haveRow) {
                        File srcRootRelPath = srcRelPath;
                        boolean canBump;
                        if (opDepth == 0) {
                            canBump = depthSufficientToBump(srcRelPath, wcRoot, srcDepth);
                        } else {
                            canBump = true;
                        }
                        if (!canBump) {
                            bumpMarkTreeConflict(wcRoot, srcRelPath, srcRootRelPath, dstRelPath);
                            haveRow = stmt.next();
                        }
                        while (SVNWCUtils.relpathDepth(srcRootRelPath) > srcOpDepth) {
                            srcRootRelPath = SVNFileUtil.getFileDir(srcRootRelPath);
                        }
                        if (srcDone.contains(srcRelPath)) {
                            srcDone.add(srcRelPath);
                            SVNSkel conflict = SvnWcDbConflicts.readConflictInternal(wcRoot, localRelPath);
                            if (conflict == null) {
                                replaceMovedLayer(wcRoot, srcRelPath, dstRelPath, opDepth);
                                bumpMovedAway(wcRoot, dstRelPath, SVNWCUtils.relpathDepth(dstRelPath), srcDone, depth, db);
                            }
                        }
                    }

                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }
        }

        private boolean depthSufficientToBump(File localRelPath, SVNWCDbRoot wcRoot, SVNDepth depth) throws SVNException {
            if (depth == SVNDepth.INFINITY) {
                return true;
            } else if (depth == SVNDepth.EMPTY) {
                SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_OP_DEPTH_CHILDREN);
                try {
                    stmt.bindf("isi", wcRoot.getWcId(), localRelPath, 0);
                    return !stmt.next();
                } finally {
                    stmt.reset();
                }
            } else if (depth == SVNDepth.FILES) {
                SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_HAS_NON_FILE_CHILDREN);
                try {
                    stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                    return !stmt.next();
                } finally {
                    stmt.reset();
                }
            } else if (depth == SVNDepth.IMMEDIATES) {
                SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_HAS_GRANDCHILDREN);
                try {
                    stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                    return !stmt.next();
                } finally {
                    stmt.reset();
                }
            } else {
                SVNErrorManager.assertionFailure(false, null, SVNLogType.WC);
            }
            return false;
        }

        private void bumpMarkTreeConflict(SVNWCDbRoot wcRoot, File moveSrcRootRelPath, File moveSrcOpRootRelPath, File moveDstOpRootRelPath) throws SVNException {
            WCDbBaseInfo baseInfo = getBaseInfo(wcRoot, moveSrcOpRootRelPath);
            long reposId = baseInfo.reposId;
            File newReposRelPath = baseInfo.reposRelPath;
            long newRevision = baseInfo.revision;
            SVNNodeKind newKind = baseInfo.kind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;

            ReposInfo reposInfo = fetchReposInfo(wcRoot.getSDb(), reposId);
            SVNURL reposRootUrl = SVNURL.parseURIEncoded(reposInfo.reposRootUrl);
            String reposUuid = reposInfo.reposUuid;

            Structure<NodeInfo> depthInfo = getDepthInfo(wcRoot, moveDstOpRootRelPath, SVNWCUtils.relpathDepth(moveDstOpRootRelPath));
            SVNNodeKind oldKind = depthInfo.get(NodeInfo.kind) == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;
            long oldRevision = depthInfo.lng(NodeInfo.revision);
            File oldReposRelPath = depthInfo.get(NodeInfo.reposRelPath);
            SVNConflictVersion oldVersion = new SVNConflictVersion(reposRootUrl, SVNFileUtil.getFilePath(oldReposRelPath), oldRevision, oldKind);
            SVNConflictVersion newVersion = new SVNConflictVersion(reposRootUrl, SVNFileUtil.getFilePath(newReposRelPath), newRevision, newKind);

            markTreeConflict(moveSrcRootRelPath, wcRoot, oldVersion, newVersion, moveDstOpRootRelPath, SVNOperation.UPDATE,
                    oldKind, newKind, oldReposRelPath, SVNConflictReason.MOVED_AWAY, SVNConflictAction.EDIT, moveSrcOpRootRelPath);
        }

        private void bumpNodeRevision(SVNWCDbRoot root, File wcRoot, File localRelPath, long reposId, File newReposRelPath, long newRevision,
                SVNDepth depth, Collection<File> exludedRelPaths, boolean isRoot, boolean skipWhenDir) throws SVNException {
            if (exludedRelPaths != null && exludedRelPaths.contains(localRelPath)) {
                return;
            }
            final WCDbBaseInfo baseInfo = getBaseInfo(root, localRelPath,
                    BaseInfoField.status, BaseInfoField.kind, BaseInfoField.revision, BaseInfoField.reposId, BaseInfoField.updateRoot,
                    BaseInfoField.reposRelPath);
            if (baseInfo.updateRoot && baseInfo.kind == SVNWCDbKind.File && !isRoot) {
                return;
            }
            if (skipWhenDir && baseInfo.kind == SVNWCDbKind.Dir) {
                return;
            }

            if (!isRoot && (baseInfo.status == SVNWCDbStatus.NotPresent ||
                    (baseInfo.status == SVNWCDbStatus.ServerExcluded && baseInfo.revision != newRevision))) {
                removeBase(SVNFileUtil.createFilePath(wcRoot, localRelPath));
                return;
            }
            boolean setReposRelPath = false;
            if (newReposRelPath != null && !baseInfo.reposRelPath.equals(newReposRelPath)) {
                setReposRelPath = true;
            }
            final Map<String, SVNProperties> nodeIprops = iprops != null ? iprops.get(root.getAbsPath(localRelPath)) : null;
            if (nodeIprops != null || setReposRelPath || (newRevision >= 0 && newRevision != baseInfo.revision)) {
                try {
                    opSetRevAndReposRelpath(root, localRelPath, nodeIprops, newRevision, setReposRelPath, newReposRelPath, newReposRootURL, newReposUUID);
                } catch (Throwable th) {
                    th.printStackTrace();
                }

            }

            if (depth.compareTo(SVNDepth.EMPTY) <= 0 ||
                    baseInfo.kind != SVNWCDbKind.Dir ||
                    baseInfo.status == SVNWCDbStatus.ServerExcluded ||
                    baseInfo.status == SVNWCDbStatus.Excluded ||
                    baseInfo.status == SVNWCDbStatus.NotPresent) {
                return;
            }
            SVNDepth depthBelowHere = depth;
            if (depth == SVNDepth.IMMEDIATES || depth == SVNDepth.FILES) {
                depthBelowHere = SVNDepth.EMPTY;
            }

            List<String> children = gatherRepoChildren(root, localRelPath, 0);
            for (String child : children) {
                File childReposRelPath = null;
                File childLocalRelPath = SVNFileUtil.createFilePath(localRelPath, child);
                if (newReposRelPath != null) {
                    childReposRelPath = SVNFileUtil.createFilePath(newReposRelPath, child);
                }
                bumpNodeRevision(root, wcRoot, childLocalRelPath, reposId, childReposRelPath, newRevision, depthBelowHere, exludedRelPaths, false, depth.compareTo(SVNDepth.IMMEDIATES) < 0);
            }
        }
    }

    public File getWCRootTempDir(File localAbspath) throws SVNException {
        assert (isAbsolute(localAbspath));
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);
        return SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), SVNFileUtil.getAdminDirectoryName()), WCROOT_TEMPDIR_RELPATH);
    }


    public void opRemoveWorkingTemp(File localAbspath) throws SVNException {
        assert (isAbsolute(localAbspath));
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        pdh.flushEntries(localAbspath);
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.DELETE_WORKING_NODE);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public void opSetBaseIncompleteTemp(File localDirAbspath, boolean incomplete) throws SVNException {
        SVNWCDbStatus baseStatus = getBaseInfo(localDirAbspath, BaseInfoField.status).status;
        assert (baseStatus == SVNWCDbStatus.Normal || baseStatus == SVNWCDbStatus.Incomplete);

        long affectedNodeRows;
        SVNSqlJetStatement stmt = getStatementForPath(localDirAbspath, SVNWCDbStatements.UPDATE_NODE_BASE_PRESENCE);
        try {
            stmt.bindString(3, incomplete ? "incomplete" : "normal");
            affectedNodeRows = stmt.done();
        } finally {
            stmt.reset();
        }
        long affectedRows = affectedNodeRows;
        if (affectedRows > 0) {
            SVNWCDbDir pdh = getOrCreateDir(localDirAbspath, false);
            if (pdh != null) {
                pdh.flushEntries(localDirAbspath);
            }
        }
    }

    private SVNWCDbDir getOrCreateDir(File localDirAbspath, boolean createAllowed) {
        SVNWCDbDir pdh = dirData.get(localDirAbspath.getAbsolutePath());
        if (pdh == null && createAllowed) {
            pdh = new SVNWCDbDir(localDirAbspath);
            dirData.put(localDirAbspath.getAbsolutePath(), pdh);
        }
        return pdh;
    }

    public void opSetDirDepthTemp(File localAbspath, SVNDepth depth) throws SVNException {
        assert (isAbsolute(localAbspath));
        assert (depth.getId() >= SVNDepth.EMPTY.getId() && depth.getId() <= SVNDepth.INFINITY.getId());
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        updateDepthValues(localAbspath, pdh, localRelpath, depth);
    }

    private void updateDepthValues(File localAbspath, SVNWCDbDir pdh, File localRelpath, SVNDepth depth) throws SVNException {
        pdh.flushEntries(localAbspath);
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.UPDATE_NODE_BASE_DEPTH);
        long affectedRows;
        try {
            stmt.bindf("iss", pdh.getWCRoot().getWcId(), localRelpath, SVNDepth.asString(depth));
            affectedRows = stmt.done();
        } finally {
            stmt.reset();
        }
        if (affectedRows == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' os not a committed directory", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    public void opRemoveEntryTemp(File localAbspath) throws SVNException {
        assert (isAbsolute(localAbspath));
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        pdh.flushEntries(localAbspath);
        SVNSqlJetDb sdb = pdh.getWCRoot().getSDb();
        long wcId = pdh.getWCRoot().getWcId();
        SVNSqlJetStatement stmt = sdb.getStatement(SVNWCDbStatements.DELETE_NODES);
        try {
            stmt.bindf("is", wcId, localRelpath);
            stmt.done();
        } finally {
            stmt.reset();
        }
        stmt = sdb.getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_WITHOUT_CONFLICT);
        try {
            stmt.bindf("is", wcId, localRelpath);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public void opSetRevAndReposRelpathTemp(File localAbspath, long revision, boolean setReposRelpath, final File reposRelpath, SVNURL reposRootUrl, String reposUuid) throws SVNException {
        assert (isAbsolute(localAbspath));
        assert (SVNRevision.isValidRevisionNumber(revision) || setReposRelpath);
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);
        SetRevRelpath baton = new SetRevRelpath();
        baton.pdh = pdh;
        baton.localRelpath = parseDir.localRelPath;
        baton.rev = revision;
        baton.setReposRelpath = setReposRelpath;
        baton.reposRelpath = reposRelpath;
        baton.reposRootUrl = reposRootUrl;
        baton.reposUuid = reposUuid;
        pdh.flushEntries(localAbspath);
        pdh.getWCRoot().getSDb().runTransaction(baton);
    }

    private void opSetRevAndReposRelpath(SVNWCDbRoot wcRoot, File localRelpath, Map<String, SVNProperties> nodeIprops, long revision, boolean setReposRelpath, final File reposRelpath, SVNURL reposRootUrl, String reposUuid) throws SVNException {
        SVNSqlJetStatement stmt = null;
        if (SVNRevision.isValidRevisionNumber(revision)) {
            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_BASE_REVISION);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), localRelpath, revision);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }

        if (setReposRelpath) {
            final long reposId = createReposId(wcRoot.getSDb(), reposRootUrl, reposUuid);
            stmt = new SVNSqlJetUpdateStatement(wcRoot.getSDb(), SVNWCDbSchema.NODES) {
                @Override
                public Map<String, Object> getUpdateValues() throws SVNException {
                    Map<String, Object> values = new HashMap<String, Object>();
                    values.put(NODES__Fields.repos_id.toString(), reposId);
                    values.put(NODES__Fields.repos_path.toString(), SVNFileUtil.getFilePath(reposRelpath));
                    return values;
                }
            };
            try {
                stmt.bindf("isi", wcRoot.getWcId(), localRelpath, 0);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }
        try {
            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_IPROPS);
            stmt.bindf("isb", wcRoot.getWcId(), localRelpath, nodeIprops != null ? SVNSkel.createInheritedProperties(nodeIprops).unparse() : null);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    private class SetRevRelpath implements SVNSqlJetTransaction {

        public SVNWCDbDir pdh;
        public File localRelpath;
        public long rev;
        public boolean setReposRelpath;
        public File reposRelpath;
        public SVNURL reposRootUrl;
        public String reposUuid;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNSqlJetStatement stmt;
            if (SVNRevision.isValidRevisionNumber(rev)) {
                stmt = db.getStatement(SVNWCDbStatements.UPDATE_BASE_REVISION);
                try {
                    stmt.bindf("isi", pdh.getWCRoot().getWcId(), localRelpath, rev);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }
            if (setReposRelpath) {
                final long reposId = createReposId(pdh.getWCRoot().getSDb(), reposRootUrl, reposUuid);
                stmt = new SVNSqlJetUpdateStatement(pdh.getWCRoot().getSDb(), SVNWCDbSchema.NODES) {
                    @Override
                    public Map<String, Object> getUpdateValues() throws SVNException {
                        Map<String, Object> values = new HashMap<String, Object>();
                        values.put(NODES__Fields.repos_id.toString(), reposId);
                        values.put(NODES__Fields.repos_path.toString(), SVNFileUtil.getFilePath(reposRelpath));
                        return values;
                    }
                };
                try {
                    stmt.bindf("isi", pdh.getWCRoot().getWcId(), localRelpath, 0);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }
        }
    };

    public void obtainWCLock(File localAbspath, int levelsToLock, boolean stealLock) throws SVNException {
        assert (isAbsolute(localAbspath));
        assert (levelsToLock >= -1);
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        WCLockObtain baton = new WCLockObtain();
        baton.pdh = pdh;
        baton.localRelpath = localRelpath;
        if (!stealLock) {
            SVNWCDbRoot wcroot = pdh.getWCRoot();
            int depth = SVNWCUtils.relpathDepth(localRelpath);
            for (WCLock lock : wcroot.getOwnedLocks()) {
                if (SVNWCUtils.isAncestor(lock.localRelpath, localRelpath) && (lock.levels == -1 || (lock.levels + SVNWCUtils.relpathDepth(lock.localRelpath)) >= depth)) {
                    File lockAbspath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), lock.localRelpath);
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_LOCKED, "''{0}'' is already locked via ''{1}''", new Object[] {
                            localAbspath, lockAbspath
                    });
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }

            WCDbWorkQueueInfo wq = fetchWorkQueue(pdh.getWCRoot().getAbsPath());
            if (wq.workItem != null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_LOCKED, "There are unfinished work items in ''{0}''; run ''svn cleanup'' first.", pdh.getWCRoot().getAbsPath());
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        baton.stealLock = stealLock;
        baton.levelsToLock = levelsToLock;
        pdh.getWCRoot().getSDb().runTransaction(baton);
    }

    private class WCLockObtain implements SVNSqlJetTransaction {

        SVNWCDbDir pdh;
        File localRelpath;
        int levelsToLock;
        boolean stealLock;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNWCDbRoot wcroot = pdh.getWCRoot();
            if (localRelpath != null && !"".equals(SVNFileUtil.getFilePath(localRelpath))) {
                boolean nodeExists = doesNodeExists(wcroot, localRelpath);
                if (!nodeExists) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.",
                            wcroot.getAbsPath(localRelpath));
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            int lockDepth = SVNWCUtils.relpathDepth(localRelpath);
            int maxDepth = lockDepth + levelsToLock;
            File lockRelpath;
            SVNSqlJetStatement stmt = wcroot.getSDb().getStatement(SVNWCDbStatements.FIND_WC_LOCK);
            try {
                stmt.bindf("is", wcroot.getWcId(), localRelpath);
                boolean gotRow = stmt.next();
                while (gotRow) {
                    lockRelpath = SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.WC_LOCK__Fields.local_dir_relpath));
                    if (levelsToLock >= 0 && SVNWCUtils.relpathDepth(lockRelpath) > maxDepth) {
                        gotRow = stmt.next();
                        continue;
                    }
                    File lockAbspath = SVNFileUtil.createFilePath(wcroot.getAbsPath(), lockRelpath);
                    boolean ownLock = isWCLockOwns(lockAbspath, true);
                    if (!ownLock && !stealLock) {
                        SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.WC_LOCKED, "''{0}'' is already locked.", SVNFileUtil.createFilePath(wcroot.getAbsPath(), lockRelpath));
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_LOCKED, "Working copy ''{0}'' locked", SVNFileUtil.createFilePath(wcroot.getAbsPath(), localRelpath));
                        err.setChildErrorMessage(err1);
                        SVNErrorManager.error(err, SVNLogType.WC);
                    } else if (!ownLock) {
                        stealWCLock(wcroot, lockRelpath);
                    }
                    gotRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }
            if (stealLock) {
                stealWCLock(wcroot, localRelpath);
            }
            stmt = wcroot.getSDb().getStatement(SVNWCDbStatements.SELECT_WC_LOCK);
            try {
                lockRelpath = localRelpath;
                while (true) {
                    stmt.bindf("is", wcroot.getWcId(), lockRelpath);
                    boolean gotRow = stmt.next();
                    if (gotRow) {
                        long levels = stmt.getColumnLong(SVNWCDbSchema.WC_LOCK__Fields.locked_levels);
                        if (levels >= 0) {
                            levels += SVNWCUtils.relpathDepth(lockRelpath);
                        }
                        stmt.reset();
                        if (levels == -1 || levels >= lockDepth) {
                            SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.WC_LOCKED, "''{0}'' is already locked.", SVNFileUtil.createFilePath(wcroot.getAbsPath(), lockRelpath));
                            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_LOCKED, "Working copy ''{0}'' locked", SVNFileUtil.createFilePath(wcroot.getAbsPath(), localRelpath));
                            err.setChildErrorMessage(err1);
                            SVNErrorManager.error(err, SVNLogType.WC);
                        }
                        break;
                    }
                    stmt.reset();
                    if (lockRelpath == null) {
                        break;
                    }
                    lockRelpath = SVNFileUtil.getFileDir(lockRelpath);
                }
            } finally {
                stmt.reset();
            }
            stmt = wcroot.getSDb().getStatement(SVNWCDbStatements.INSERT_WC_LOCK);
            stmt.bindf("isi", wcroot.getWcId(), localRelpath, levelsToLock);
            try {
                stmt.done();
            } catch (SVNException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_LOCKED, "Working copy ''{0}'' locked", SVNFileUtil.createFilePath(wcroot.getAbsPath(), localRelpath));
                SVNErrorManager.error(err, SVNLogType.WC);
            } finally {
                stmt.reset();
            }
            WCLock lock = new WCLock();
            lock.localRelpath = localRelpath;
            lock.levels = levelsToLock;
            wcroot.getOwnedLocks().add(lock);
        }

    };

    private void stealWCLock(SVNWCDbRoot wcroot, File localRelpath) throws SVNException {
        SVNSqlJetStatement stmt = wcroot.getSDb().getStatement(SVNWCDbStatements.DELETE_WC_LOCK);
        try {
            stmt.bindf("is", wcroot.getWcId(), localRelpath);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public void releaseWCLock(File localAbspath) throws SVNException {
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        WCLock foundLock = null;
        List<WCLock> ownedLocks = pdh.getWCRoot().getOwnedLocks();
        for (WCLock lock : ownedLocks) {
            if (lock.localRelpath.equals(localRelpath)) {
                foundLock = lock;
                break;
            }
        }
        if (foundLock == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_LOCKED, "Working copy not locked at ''{0}''", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        ownedLocks.remove(foundLock);
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.DELETE_WC_LOCK);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public File getWCRoot(File wcRootAbspath) throws SVNException {
        DirParsedInfo parseDir = parseDir(wcRootAbspath, Mode.ReadOnly);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        if (pdh.getWCRoot() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY, "The node ''{0}'' is not in the working copy", wcRootAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return pdh.getWCRoot().getAbsPath();
    }

    public void forgetDirectoryTemp(File localDirAbspath) throws SVNException {
        Set<SVNWCDbRoot> roots = new HashSet<SVNWCDbRoot>();
        for (Iterator<Entry<String, SVNWCDbDir>> i = dirData.entrySet().iterator(); i.hasNext();) {
            Entry<String, SVNWCDbDir> entry = i.next();
            SVNWCDbDir pdh = entry.getValue();
            if (!SVNWCUtils.isAncestor(localDirAbspath, pdh.getLocalAbsPath())) {
                continue;
            }
            try {
                releaseWCLock(pdh.getLocalAbsPath());
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY && e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_LOCKED) {
                    throw e;
                }
            }
            i.remove();
            if (pdh.getWCRoot() != null && pdh.getWCRoot().getSDb() != null && SVNWCUtils.isAncestor(localDirAbspath, pdh.getWCRoot().getAbsPath())) {
                roots.add(pdh.getWCRoot());
            }
        }
        closeManyWCRoots(roots);
    }

    public boolean isWCLockOwns(File localAbspath, boolean exact) throws SVNException {
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        if (pdh.getWCRoot() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY, "The node ''{0}'' was not found.", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        verifyDirUsable(pdh);
        boolean ownLock = false;
        List<WCLock> ownedLocks = pdh.getWCRoot().getOwnedLocks();
        int lockLevel = SVNWCUtils.relpathDepth(localRelpath);
        if (exact)
            for (WCLock lock : ownedLocks) {
                if (lock.localRelpath.equals(localRelpath)) {
                    ownLock = true;
                    return ownLock;
                }
            }
        else
            for (WCLock lock : ownedLocks) {
                if (SVNWCUtils.isAncestor(lock.localRelpath, localRelpath) && (lock.levels == -1 || ((SVNWCUtils.relpathDepth(lock.localRelpath) + lock.levels) >= lockLevel))) {
                    ownLock = true;
                    return ownLock;
                }
            }
        return ownLock;
    }

    public void opSetTextConflictMarkerFilesTemp(File localAbspath, File oldBasename, File newBasename, File wrkBasename) throws SVNException {
        assert (isAbsolute(localAbspath));
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        boolean gotRow = false;
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            gotRow = stmt.next();
        } finally {
            stmt.reset();
        }
        try {
            if (gotRow) {
                stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_TEXT_CONFLICTS);
            } else if (oldBasename == null && newBasename == null && wrkBasename == null) {
                return;
            } else {
                stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.INSERT_ACTUAL_TEXT_CONFLICTS);
                stmt.bindString(6, SVNFileUtil.getFilePath(SVNFileUtil.getFileDir(localRelpath)));
            }
            stmt.bindf("issss", pdh.getWCRoot().getWcId(), localRelpath, oldBasename, newBasename, wrkBasename);
            stmt.done();
        } finally {
            if (stmt != null) {
                stmt.reset();
            }
        }
    }

    public void addBaseNotPresentNode(File localAbspath, File reposRelPath, SVNURL reposRootUrl, String reposUuid, long revision, SVNWCDbKind kind, SVNSkel conflict, SVNSkel workItems)
            throws SVNException {
        addExcludedOrNotPresentNode(localAbspath, reposRelPath, reposRootUrl, reposUuid, revision, kind, SVNWCDbStatus.NotPresent, conflict, workItems);
    }

    public void opSetPropertyConflictMarkerFileTemp(File localAbspath, String prejBasename) throws SVNException {
        assert (isAbsolute(localAbspath));
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        boolean gotRow = false;
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            gotRow = stmt.next();
        } finally {
            stmt.reset();
        }
        try {
            if (gotRow) {
                stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.UPDATE_ACTUAL_PROPERTY_CONFLICTS);
            } else if (prejBasename == null) {
                return;
            } else {
                stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.INSERT_ACTUAL_PROPERTY_CONFLICTS);
                if (localRelpath != null && !"".equals(SVNFileUtil.getFilePath(localRelpath))) {
                    stmt.bindString(4, SVNFileUtil.getFilePath(SVNFileUtil.getFileDir(localRelpath)));
                } else {
                    stmt.bindNull(4);
                }
            }
            stmt.bindf("iss", pdh.getWCRoot().getWcId(), localRelpath, prejBasename);
            stmt.done();
        } finally {
            if (stmt != null) {
                stmt.reset();
            }
        }
    }

    private void addExcludedOrNotPresentNode(File localAbspath, File reposRelpath, SVNURL reposRootUrl, String reposUuid, long revision, SVNWCDbKind kind, SVNWCDbStatus status, SVNSkel conflict,
            SVNSkel workItems) throws SVNException {
        assert (isAbsolute(localAbspath));
        assert (reposRelpath != null);
        // SVN_ERR_ASSERT(svn_uri_is_absolute(repos_root_url));
        assert (reposUuid != null);
        assert (SVNRevision.isValidRevisionNumber(revision));
        assert (status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.NotPresent);

        DirParsedInfo parseDir = parseDir(SVNFileUtil.getParentFile(localAbspath), Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = SVNFileUtil.createFilePath(parseDir.localRelPath, SVNFileUtil.getFileName(localAbspath));
        verifyDirUsable(pdh);

        InsertBase ibb = new InsertBase();
        ibb.status = status;
        ibb.kind = kind;
        ibb.reposRelpath = reposRelpath;
        ibb.revision = revision;
        ibb.children = null;
        ibb.depth = SVNDepth.UNKNOWN;
        ibb.checksum = null;
        ibb.target = null;
        ibb.conflict = conflict;
        ibb.workItems = workItems;
        ibb.reposRootURL = reposRootUrl;
        ibb.reposUUID = reposUuid;

        ibb.wcId = pdh.getWCRoot().getWcId();
        ibb.wcRoot = pdh.getWCRoot();
        ibb.localRelpath = localRelpath;
        pdh.getWCRoot().getSDb().runTransaction(ibb);
        pdh.flushEntries(localAbspath);
    }

    public void globalCommit(File localAbspath, long newRevision, long changedRevision, SVNDate changedDate, String changedAuthor, SvnChecksum newChecksum, List<File> newChildren,
            SVNProperties newDavCache, boolean keepChangelist, boolean noUnlock, SVNSkel workItems) throws SVNException {
        assert (isAbsolute(localAbspath));
        assert (SVNRevision.isValidRevisionNumber(newRevision));
        assert (newChecksum == null || newChildren == null);
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);
        Commit cb = new Commit();
        cb.pdh = pdh;
        cb.localRelpath = localRelpath;
        cb.newRevision = newRevision;
        cb.changedRev = changedRevision;
        cb.changedDate = changedDate;
        cb.changedAuthor = changedAuthor;
        cb.newChecksum = newChecksum;
        cb.newChildren = newChildren;
        cb.newDavCache = newDavCache;
        cb.keepChangelist = keepChangelist;
        cb.noUnlock = noUnlock;
        cb.workItems = workItems;
        pdh.getWCRoot().getSDb().runTransaction(cb);
        pdh.flushEntries(localAbspath);
    }

    private static class ReposInfo2 {

        public long reposId;
        public File reposRelPath;
    }

    private ReposInfo2 determineReposInfo(SVNWCDbDir pdh, File localRelpath) throws SVNException {
        ReposInfo2 info = new ReposInfo2();
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_BASE_NODE);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            boolean haveRow = stmt.next();
            if (haveRow) {
                assert (!stmt.isColumnNull(SVNWCDbSchema.NODES__Fields.repos_id));
                assert (!stmt.isColumnNull(SVNWCDbSchema.NODES__Fields.repos_path));
                info.reposId = stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.repos_id);
                info.reposRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.NODES__Fields.repos_path));
                return info;
            }
        } finally {
            stmt.reset();
        }
        File localParentRelpath = SVNFileUtil.getFileDir(localRelpath);
        String name = SVNFileUtil.getFileName(localRelpath);
        WCDbRepositoryInfo reposInfo = new WCDbRepositoryInfo();
        info.reposId = scanUpwardsForRepos(reposInfo, pdh.getWCRoot(), localParentRelpath);
        File reposParentRelpath = reposInfo.relPath;
        info.reposRelPath = SVNFileUtil.createFilePath(reposParentRelpath, name);
        return info;
    }

    public class Commit implements SVNSqlJetTransaction {

        public SVNWCDbDir pdh;
        public File localRelpath;
        public long newRevision;
        public long changedRev;
        public SVNDate changedDate;
        public String changedAuthor;
        public SvnChecksum newChecksum;
        public List<File> newChildren;
        public SVNProperties newDavCache;
        public boolean keepChangelist;
        public boolean noUnlock;
        public SVNSkel workItems;
        public long reposId;
        public File reposRelPath;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {

            SVNSqlJetStatement stmtAct = null;
            SVNSqlJetStatement stmtInfo = null;
            boolean haveAct;
            byte[] propBlob = null;
            String changelist = null;
            File parentRelpath;
            SVNWCDbStatus newPresence;
            String newDepthStr = null;
            SVNSqlJetStatement stmt;
            boolean fileExternal = false;
            long opDepth;
            SVNWCDbKind newKind;
            SVNWCDbStatus oldPresence = null;

            ReposInfo2 reposInfo = determineReposInfo(pdh, localRelpath);
            reposId = reposInfo.reposId;
            reposRelPath = reposInfo.reposRelPath;

            SVNSqlJetDb sdb = pdh.getWCRoot().getSDb();
            long wcId = pdh.getWCRoot().getWcId();

            try {
                stmtInfo = sdb.getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
                stmtInfo.bindf("is", wcId, localRelpath);
                stmtInfo.next();

                stmtAct = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
                stmtAct.bindf("is", wcId, localRelpath);
                haveAct = stmtAct.next();

                opDepth = getColumnInt64(stmtInfo, NODES__Fields.op_depth);
                newKind = getColumnKind(stmtInfo, NODES__Fields.kind);
                if (newKind == SVNWCDbKind.Dir) {
                    newDepthStr = getColumnText(stmtInfo, NODES__Fields.depth);
                }
                if (haveAct) {
                    propBlob = getColumnBlob(stmtAct, ACTUAL_NODE__Fields.properties);
                }
                if (propBlob == null) {
                    propBlob = getColumnBlob(stmtInfo, NODES__Fields.properties);
                }
                if (keepChangelist && haveAct) {
                    changelist = getColumnText(stmtAct, ACTUAL_NODE__Fields.changelist);
                }
                oldPresence = getColumnPresence(stmtInfo);
            } finally {
                if (stmtInfo != null) {
                    stmtInfo.reset();
                }
                if (stmtAct != null)  {
                    stmtAct.reset();
                }
            }

            if (opDepth > 0) {
                stmt = sdb.getStatement(SVNWCDbStatements.DELETE_ALL_LAYERS);
                long affectedRows;
                try {
                    stmt.bindf("is", wcId, localRelpath);
                    affectedRows = stmt.done();
                } finally {
                    stmt.reset();
                }
                if (affectedRows > 1) {
                    stmt = sdb.getStatement(SVNWCDbStatements.DELETE_SHADOWED_RECURSIVE);
                    try {
                        stmt.bindf("isi", wcId, localRelpath, opDepth);
                        stmt.done();
                    } finally {
                        stmt.reset();
                    }
                }
                commitDescendant(localRelpath, reposRelPath, opDepth, newRevision);
            }

            if (localRelpath == null || "".equals(SVNFileUtil.getFilePath(localRelpath))) {
                parentRelpath = null;
            } else {
                parentRelpath = SVNFileUtil.getFileDir(localRelpath);
            }

            newPresence = oldPresence == SVNWCDbStatus.Incomplete ? SVNWCDbStatus.Incomplete : SVNWCDbStatus.Normal;

            stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.APPLY_CHANGES_TO_BASE_NODE);
            try {
                stmt.bindf("issisrtstrisnbnn",
                        pdh.getWCRoot().getWcId(), localRelpath,
                        parentRelpath, reposId,
                        reposRelPath, newRevision,
                        getPresenceText(newPresence),
                        newDepthStr,
                        getKindText(newKind),
                        changedRev, changedDate, changedAuthor,
                        propBlob);
                stmt.bindChecksum(13, newChecksum);
                stmt.bindProperties(15, newDavCache);
                if (fileExternal) {
                    stmt.bindString(17, "1");
                } else {
                    stmt.bindNull(17);
                }
                stmt.done();
            } finally {
                stmt.reset();
            }

            if (haveAct) {
                if (keepChangelist && changelist != null) {
                    stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.RESET_ACTUAL_WITH_CHANGELIST);
                    try {
                        stmt.bindf("isss", pdh.getWCRoot().getWcId(), localRelpath, SVNFileUtil.getFileDir(localRelpath), changelist);
                        stmt.done();
                    } finally {
                        stmt.reset();
                    }
                } else {
                    stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE);
                    try {
                        stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
                        stmt.done();
                    } finally {
                        stmt.reset();
                    }
                }
            }
            if (!noUnlock) {
                SVNSqlJetStatement lockStmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.DELETE_LOCK);
                try {
                    lockStmt.bindf("is", reposId, reposRelPath);
                    lockStmt.done();
                } finally {
                    lockStmt.reset();
                }
            }

            addWorkItems(pdh.getWCRoot().getSDb(), workItems);
        }

        private void commitDescendant(File parentLocalRelPath, File parentReposRelPath, long opDepth, long revision) throws SVNException {
            List<String> children = gatherRepoChildren(pdh.getWCRoot(), parentLocalRelPath, opDepth);
            for (String name : children) {
                SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.COMMIT_DESCENDANT_TO_BASE);
                try {
                    File childLocalRelPath = SVNFileUtil.createFilePath(parentLocalRelPath, name);
                    File childReposRelPath = SVNFileUtil.createFilePath(parentReposRelPath, name);
                    stmt.bindf("isiisr", pdh.getWCRoot().getWcId(), childLocalRelPath, opDepth, reposId, childReposRelPath, revision);
                    stmt.done();

                    commitDescendant(childLocalRelPath, childReposRelPath, opDepth, revision);
                } finally {
                    stmt.reset();
                }
            }
        }

    }

    public Structure<PristineInfo> readPristineInfo(File localAbspath) throws SVNException {
        final DirParsedInfo wcInfo = obtainWcRoot(localAbspath);
        SVNWCDbDir dir = wcInfo.wcDbDir;
        SVNSqlJetStatement stmt = dir.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
        File relativePath = wcInfo.localRelPath;
        Structure<PristineInfo> result = Structure.obtain(PristineInfo.class);
        try {
            stmt.bindf("is", dir.getWCRoot().getWcId(), relativePath);
            if (!stmt.next()) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.",
                        localAbspath), SVNLogType.WC);
            }

            long opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
            SVNWCDbStatus status = getColumnPresence(stmt);

            if (opDepth > 0 && status == SVNWCDbStatus.BaseDeleted) {
                boolean hasNext = stmt.next();
                assert hasNext;
                opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                status = getColumnPresence(stmt);
            }
            SVNWCDbKind kind = getColumnKind(stmt, NODES__Fields.kind);
            result.set(PristineInfo.kind, kind);
            result.set(PristineInfo.changed_date, SVNWCUtils.readDate(getColumnInt64(stmt, NODES__Fields.changed_date)));
            result.set(PristineInfo.changed_author, getColumnText(stmt, NODES__Fields.changed_author));
            result.set(PristineInfo.changed_rev, getColumnInt64(stmt, NODES__Fields.changed_revision));

            if (opDepth > 0) {
                result.set(PristineInfo.status, getWorkingStatus(status));
            } else {
                result.set(PristineInfo.status, status);
            }
            if (kind != SVNWCDbKind.Dir) {
                result.set(PristineInfo.depth, SVNDepth.UNKNOWN);
            } else {
                String depthStr = getColumnText(stmt, NODES__Fields.depth);
                if (depthStr == null) {
                    result.set(PristineInfo.depth, SVNDepth.UNKNOWN);
                } else {
                    result.set(PristineInfo.depth, SVNDepth.fromString(depthStr));
                }
            }
            if (kind == SVNWCDbKind.File) {
                SvnChecksum checksum = getColumnChecksum(stmt, NODES__Fields.checksum);
                result.set(PristineInfo.checksum, checksum);
            } else if (kind == SVNWCDbKind.Symlink) {
                result.set(PristineInfo.target, getColumnText(stmt, NODES__Fields.symlink_target));
            }
            result.set(PristineInfo.hadProps, hasColumnProperties(stmt, NODES__Fields.properties));

            SVNProperties props;
            if (status == SVNWCDbStatus.Normal || status == SVNWCDbStatus.Incomplete) {
                props = SvnWcDbStatementUtil.getColumnProperties(stmt, NODES__Fields.properties);
                if (props == null) {
                    props = new SVNProperties();
                }
            } else {
                assert stmt.isColumnNull(NODES__Fields.properties);
                props = null;
            }
            result.set(PristineInfo.props, props);
        } finally {
            stmt.reset();
        }

        return result;
    }

    public DirParsedInfo obtainWcRoot(File localAbspath) throws SVNException {
        return obtainWcRoot(localAbspath, false);
    }

    public DirParsedInfo obtainWcRoot(File localAbspath, boolean isAdditionMode) throws SVNException {
        assert (isAbsolute(localAbspath));

        final DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadOnly, false, isAdditionMode);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);

        return parseDir;
    }

    public void registerExternal(File definingAbsPath, File localAbsPath, SVNNodeKind kind, SVNURL reposRootUrl, String reposUuid, File reposRelPath, long operationalRevision, long revision) throws SVNException {
        SvnWcDbExternals.addExternalDir(this, localAbsPath, definingAbsPath, reposRootUrl, reposUuid, definingAbsPath, reposRelPath, operationalRevision, revision, null);
    }

    public void opRemoveNode(File localAbspath, long notPresentRevision, SVNWCDbKind notPresentKind) throws SVNException {
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        pdh.getWCRoot().getSDb().beginTransaction(SqlJetTransactionMode.WRITE);
        try {
            long reposId = -1;
            File reposRelPath = null;
            if (notPresentRevision >= 0) {
                WCDbBaseInfo baseInfo = getBaseInfo(pdh.getWCRoot(), localRelpath, BaseInfoField.reposRelPath, BaseInfoField.reposId);
                reposId = baseInfo.reposId;
                reposRelPath = baseInfo.reposRelPath;
            }

            SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.DELETE_NODES_RECURSIVE);
            try {
                stmt.bindf("isi", pdh.getWCRoot().getWcId(), localRelpath , 0);
                stmt.done();
            } finally {
                stmt.reset();
            }

            stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_RECURSIVE);
            try {
                stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }

            if (notPresentRevision >= 0) {
                InsertBase ib = new InsertBase();
                ib.reposId = reposId;
                ib.reposRelpath = reposRelPath;
                ib.status = SVNWCDbStatus.NotPresent;
                ib.kind = notPresentKind;
                ib.revision = notPresentRevision;

                ib.localRelpath = localRelpath;
                ib.wcId = pdh.getWCRoot().getWcId();
                ib.wcRoot = pdh.getWCRoot();

                pdh.getWCRoot().getSDb().runTransaction(ib);
            }
        } catch (SVNException e) {
            pdh.getWCRoot().getSDb().rollback();
            throw e;
        } finally {
            pdh.getWCRoot().getSDb().commit();
        }
        pdh.flushEntries(localAbspath);
    }

    public void upgradeBegin(File localAbspath, SVNWCDbUpgradeData upgradeData, SVNURL repositoryRootUrl, String repositoryUUID, int targetWorkingCopyFormat) throws SVNException {
    	CreateDbInfo dbInfo =  createDb(localAbspath, repositoryRootUrl, repositoryUUID, SDB_FILE, targetWorkingCopyFormat, true);
    	upgradeData.repositoryId = dbInfo.reposId;
    	upgradeData.workingCopyId = dbInfo.wcId;

    	SVNWCDbDir pdh = new SVNWCDbDir(localAbspath);
    	SVNWCDbRoot root = new SVNWCDbRoot(this, localAbspath, dbInfo.sDb, dbInfo.wcId, FORMAT_FROM_SDB, true, false, false);
        pdh.setWCRoot(root);
        upgradeData.root = root;
        dirData.put(upgradeData.rootAbsPath.getAbsolutePath(), pdh);
    }

    public class CheckReplace implements SVNSqlJetTransaction {

        public long wcId;
        public File localRelpath;

        public boolean replaceRoot;
        public boolean baseReplace;
        public boolean replace;

        public CheckReplace(long wcId, File localRelpath) {
            this.wcId = wcId;
            this.localRelpath = localRelpath;
        }

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            long replacedOpDepth;
            SVNWCDbStatus replacedStatus;
            boolean haveRow;
            long parentOpDepth;

            final SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
            try {
                stmt.bindf("is", wcId, localRelpath.getPath());
                haveRow = stmt.next();
                if (!haveRow) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND,
                            "The node ''{0}'' was not found.", localRelpath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }

                {
                    SVNWCDbStatus status = getColumnPresence(stmt);

                    if (status != SVNWCDbStatus.Normal) {
                        return;
                    }
                }
                haveRow = stmt.next();
                if (!haveRow) {
                    return;
                }

                replacedStatus = getColumnPresence(stmt);
                if (replacedStatus != SVNWCDbStatus.NotPresent
                        && replacedStatus != SVNWCDbStatus.Excluded
                        && replacedStatus != SVNWCDbStatus.ServerExcluded
                        && replacedStatus != SVNWCDbStatus.Deleted) {
                    replace = true;
                }

                replacedOpDepth = stmt.getColumnLong(NODES__Fields.op_depth);

                //if we need base replace {

                long opDepth = stmt.getColumnLong(NODES__Fields.op_depth);

                while (opDepth != 0 && haveRow) {
                    haveRow = stmt.next();
                    if (haveRow) {
                        opDepth = stmt.getColumnLong(NODES__Fields.op_depth);
                    }
                }

                if (haveRow && opDepth == 0) {
                    SVNWCDbStatus baseStatus = getColumnPresence(stmt);
                    baseReplace = baseStatus != SVNWCDbStatus.NotPresent;
                }

                // }

            } finally {
                stmt.reset();
            }

            if (replacedStatus != SVNWCDbStatus.BaseDeleted) {
                try {
                    stmt.bindf("is", wcId, SVNFileUtil.getFileDir(localRelpath));

                    stmt.nextRow();

                    parentOpDepth = stmt.getColumnLong(NODES__Fields.op_depth);

                    if (parentOpDepth >= replacedOpDepth) {
                        replaceRoot = parentOpDepth == replacedOpDepth;
                        return;
                    }

                    haveRow = stmt.next();

                    if (haveRow) {
                        parentOpDepth = stmt.getColumnLong(NODES__Fields.op_depth);
                    }
                } finally {
                    stmt.reset();
                }

                if (!haveRow) {
                    replaceRoot = true;
                } else if (parentOpDepth < replacedOpDepth) {
                    replaceRoot = true;
                }
            }
        }
    }

    public SVNWCDbNodeCheckReplaceData nodeCheckReplace(File localAbspath) throws SVNException {
        assert SVNFileUtil.isAbsolute(localAbspath);
        DirParsedInfo pdh = parseDir(localAbspath, Mode.ReadOnly);
        verifyDirUsable(pdh.wcDbDir);

        if (pdh.localRelPath == null || pdh.localRelPath.getPath() == null || pdh.localRelPath.getPath().length() == 0) {
            return SVNWCDbNodeCheckReplaceData.NO_REPLACE;
        }

        final CheckReplace checkReplace = new CheckReplace(pdh.wcDbDir.getWCRoot().getWcId(), pdh.localRelPath);
        checkReplace.replace = false;
        checkReplace.replaceRoot = false;
        checkReplace.baseReplace = false;

        pdh.wcDbDir.getWCRoot().getSDb().runTransaction(checkReplace, SqlJetTransactionMode.READ_ONLY);

        return new SVNWCDbNodeCheckReplaceData(checkReplace.replaceRoot, checkReplace.replace, checkReplace.baseReplace);
    }

    public SVNWCDbBaseMovedToData baseMovedTo(File localAbsPath) throws SVNException {
        assert isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        BaseMovedTo bmt = new BaseMovedTo();
        bmt.wcRoot = pdh.getWCRoot();
        bmt.localRelPath = parsed.localRelPath;
        bmt.opDepth = 0;
        pdh.getWCRoot().getSDb().runTransaction(bmt);

        SVNWCDbBaseMovedToData baseMovedToData = new SVNWCDbBaseMovedToData();
        if (bmt.moveDstRelPath != null) {
            baseMovedToData.moveDstAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), bmt.moveDstRelPath);
        }
        if (bmt.moveDstOpRootRelPath != null) {
            baseMovedToData.moveDstOpRootAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), bmt.moveDstOpRootRelPath);
        }
        if (bmt.moveSrcRootRelPath != null) {
            baseMovedToData.moveSrcRootAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), bmt.moveSrcRootRelPath);
        }
        if (bmt.moveSrcOpRootRelPath != null) {
            baseMovedToData.moveSrcOpRootAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), bmt.moveSrcOpRootRelPath);
        }
        return baseMovedToData;
    }

    public static class BaseMovedTo implements SVNSqlJetTransaction {

        public SVNWCDbRoot wcRoot;
        public File localRelPath;
        public long opDepth;

        public File moveDstOpRootRelPath;
        public File moveDstRelPath;
        public File moveSrcRootRelPath;
        public File moveSrcOpRootRelPath;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            File moveDstOpRootRelPath = null;
            File moveDstRelPath = null;
            File moveSrcRootRelPath = null;
            File moveSrcOpRootRelPath = null;
            long deleteOpDepth = 0;
            File relPath = localRelPath;

            boolean haveRow;
            do {
                SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_LOWEST_WORKING_NODE);
                try {
                    stmt.bindf("isi", wcRoot.getWcId(), localRelPath, opDepth);
                    haveRow = stmt.next();
                    if (haveRow) {
                        deleteOpDepth = stmt.getColumnLong(NODES__Fields.op_depth);
                        moveDstOpRootRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.moved_to));
                        if (moveDstOpRootRelPath != null) {
                            moveSrcRootRelPath = relPath;
                        }
                    }
                    if (moveDstOpRootRelPath == null) {
                        relPath = SVNFileUtil.getParentFile(relPath);
                    }

                } finally {
                    stmt.reset();
                }
            } while (moveDstOpRootRelPath == null && haveRow && deleteOpDepth <= SVNWCUtils.relpathDepth(relPath));


            if (moveDstOpRootRelPath != null) {
                moveDstRelPath = SVNFileUtil.createFilePath(moveDstOpRootRelPath, SVNWCUtils.skipAncestor(relPath, localRelPath));

                while (deleteOpDepth < SVNWCUtils.relpathDepth(relPath)) {
                    relPath = SVNFileUtil.getParentFile(relPath);
                }
                moveSrcOpRootRelPath = relPath;
            }

            this.moveSrcRootRelPath = moveSrcRootRelPath;
            this.moveSrcOpRootRelPath = moveSrcOpRootRelPath;
            this.moveDstRelPath = moveDstRelPath;
            this.moveDstOpRootRelPath = moveDstOpRootRelPath;
        }
    }

    public NodeInstallInfo readNodeInstallInfo(File localAbsPath, File wriAbsPath) throws SVNException {
        assert isAbsolute(localAbsPath);

        if (wriAbsPath == null) {
            wriAbsPath = localAbsPath;
        }

        DirParsedInfo parsed = parseDir(wriAbsPath, Mode.ReadOnly);
        verifyDirUsable(parsed.wcDbDir);

        NodeInstallInfo nodeInstallInfo = new NodeInstallInfo();

        File localRelPath = parsed.localRelPath;

        SVNWCDbRoot wcRoot = parsed.wcDbDir.getWCRoot();
        File wcRootAbsPath = wcRoot.getAbsPath();

        if (!localAbsPath.equals(wriAbsPath)) {
            if (!SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(wcRootAbsPath), SVNFileUtil.getFilePath(localAbsPath))) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node '{{0}}' is not in working copy '{{1}}'",
                        localAbsPath, wcRootAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            localRelPath = SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(SVNFileUtil.getFilePath(wcRootAbsPath),
                    SVNFileUtil.getFilePath(localAbsPath)));
        }

        nodeInstallInfo.wcRoot = wcRoot;

        boolean haveRow;

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            haveRow = stmt.next();

            if (haveRow) {
                nodeInstallInfo.checksum = SvnWcDbStatementUtil.getColumnChecksum(stmt, NODES__Fields.checksum);
                nodeInstallInfo.properties = SvnWcDbStatementUtil.getColumnProperties(stmt, NODES__Fields.properties);
                nodeInstallInfo.changedDate = SvnWcDbStatementUtil.getColumnDate(stmt, NODES__Fields.changed_date);
            } else {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node '{0}' is not installable", localAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

        } finally {
            stmt.reset();
        }

        return nodeInstallInfo;
    }

    public void resolveBreakMovedAway(File localAbsPath, ISVNEventHandler eventHandler) throws SVNException {
        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelPath = parsed.localRelPath;
        verifyDirUsable(pdh);

        ResolveBreakMovedAway resolveBreakMovedAway = new ResolveBreakMovedAway();
        resolveBreakMovedAway.wcRoot = parsed.wcDbDir.getWCRoot();
        resolveBreakMovedAway.localRelPath = localRelPath;
        pdh.getWCRoot().getSDb().runTransaction(resolveBreakMovedAway);

        if (eventHandler != null) {
            eventHandler.handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.UNKNOWN, null, -1,
                    SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE, SVNStatusType.LOCK_UNKNOWN, SVNEventAction.MOVE_BROKEN, SVNEventAction.MOVE_BROKEN,null, null), ISVNEventHandler.UNKNOWN);
        }
    }

    protected static class ResolveBreakMovedAway implements SVNSqlJetTransaction {

        public SVNWCDbRoot wcRoot;
        public File localRelPath;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            BaseMovedTo movedTo = wcRoot.getDb().opDepthMovedTo(SVNWCUtils.relpathDepth(localRelPath) - 1, wcRoot, localRelPath);

            BreakMove breakMove = new BreakMove();
            breakMove.wcRoot = wcRoot;
            breakMove.srcRelPath = localRelPath;
            breakMove.srcOpDepth = SVNWCUtils.relpathDepth(movedTo.moveSrcOpRootRelPath);
            breakMove.dstRelPath = movedTo.moveDstOpRootRelPath;
            breakMove.dstOpDepth = SVNWCUtils.relpathDepth(movedTo.moveDstOpRootRelPath);
            breakMove.transaction(db);
        }
    }

    private static class BreakMove implements SVNSqlJetTransaction {

        public SVNWCDbRoot wcRoot;
        public File srcRelPath;
        public long srcOpDepth;
        public File dstRelPath;
        public long dstOpDepth;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNSqlJetStatement stmt;

            stmt = db.getStatement(SVNWCDbStatements.CLEAR_MOVE_TO_RELPATH);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), srcRelPath, srcOpDepth);
                stmt.done();
            } finally {
                stmt.reset();
            }

            stmt = db.getStatement(SVNWCDbStatements.UPDATE_OP_DEPTH_RECURSIVE);
            try {
                stmt.bindf("isii", wcRoot.getWcId(), dstRelPath, dstOpDepth, dstOpDepth);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }
    }

    public BaseMovedTo opDepthMovedTo(long opDepth, SVNWCDbRoot wcRoot, File localRelPath) throws SVNException {
        BaseMovedTo movedTo = new BaseMovedTo();

        File relPath = localRelPath;

        boolean haveRow;
        long deleteOpDepth = 0;
        do {
            SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_LOWEST_WORKING_NODE);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), relPath, opDepth);
                haveRow = stmt.next();

                if (haveRow) {
                    deleteOpDepth = stmt.getColumnLong(NODES__Fields.op_depth);
                    movedTo.moveDstOpRootRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.moved_to));

                    if (movedTo.moveDstOpRootRelPath != null) {
                        movedTo.moveSrcRootRelPath = relPath;
                    }
                }
            } finally {
                stmt.reset();
            }

            if (movedTo.moveDstOpRootRelPath == null) {
                relPath = SVNFileUtil.getFileDir(relPath);
            }
        } while (movedTo.moveDstOpRootRelPath == null && haveRow && deleteOpDepth <= SVNWCUtils.relpathDepth(relPath));

        if (movedTo.moveDstOpRootRelPath != null) {
            movedTo.moveDstRelPath = SVNFileUtil.createFilePath(movedTo.moveDstOpRootRelPath, SVNPathUtil.getRelativePath(SVNFileUtil.getFilePath(relPath), SVNFileUtil.getFilePath(localRelPath)));

            while (deleteOpDepth < SVNWCUtils.relpathDepth(relPath)) {
                relPath = SVNFileUtil.getFileDir(relPath);
            }
            movedTo.moveSrcOpRootRelPath = relPath;
        }
        return movedTo;
    }

    private class ResolveDeleteRaiseMovedAway implements SVNSqlJetTransaction {

        public SVNWCDbDir pdh;
        public File localRelPath;
        public SVNOperation operation;
        public SVNConflictAction action;
        public SVNConflictVersion oldVersion;
        public SVNConflictVersion newVersion;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNWCDbRoot wcRoot = pdh.getWCRoot();
            long opDepth = SVNWCUtils.relpathDepth(localRelPath);

            SVNSqlJetStatement stmt;

            stmt = new SVNWCDbCreateSchema(db.getTemporaryDb(), SVNWCDbCreateSchema.CREATE_UPDATE_MOVE_LIST, -1, false);
            try {
                stmt.done();
            } finally {
                stmt.reset();
            }

            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_OP_DEPTH_MOVED_PAIR);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), localRelPath, opDepth);
                boolean haveRow = stmt.next();

                while (haveRow) {
                    File movedRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.local_relpath));
                    File moveRootDstRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.moved_to));
                    File movedDstReposRelPath = SVNFileUtil.createFilePath(((SVNWCDbSelectOpDepthMovedPair)stmt).getReposPath());

                    markTreeConflict(movedRelPath, pdh.getWCRoot(), oldVersion, newVersion, moveRootDstRelPath, operation, SVNNodeKind.DIR, SVNNodeKind.DIR,
                            movedDstReposRelPath, SVNConflictReason.MOVED_AWAY, action, localRelPath);

                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }
        }
    }

    public void resolveDeleteRaiseMovedAway(File localAbsPath, ISVNEventHandler eventHandler) throws SVNException {
        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelPath = parsed.localRelPath;

        verifyDirUsable(pdh);

        TcInfo tcInfo = getTcInfo(localAbsPath);
        SVNTreeConflictDescription treeConflictDescription = tcInfo.treeConflictDescription;
        SVNOperation operation = treeConflictDescription.getOperation();
        SVNConflictReason reason = treeConflictDescription.getConflictReason();
        SVNConflictAction action = treeConflictDescription.getConflictAction();
        SVNConflictVersion oldVersion = treeConflictDescription.getSourceLeftVersion();
        SVNConflictVersion newVersion = treeConflictDescription.getSourceRightVersion();

        ResolveDeleteRaiseMovedAway resolveDeleteRaiseMovedAway = new ResolveDeleteRaiseMovedAway();
        resolveDeleteRaiseMovedAway.pdh = pdh;
        resolveDeleteRaiseMovedAway.localRelPath = localRelPath;
        resolveDeleteRaiseMovedAway.operation = operation;
        resolveDeleteRaiseMovedAway.action = action;
        resolveDeleteRaiseMovedAway.oldVersion = oldVersion;
        resolveDeleteRaiseMovedAway.newVersion = newVersion;

        pdh.getWCRoot().getSDb().runTransaction(resolveDeleteRaiseMovedAway);

        updateMoveListNotify(pdh.getWCRoot(), oldVersion.getPegRevision(), newVersion != null ? newVersion.getPegRevision() : SVNRepository.INVALID_REVISION, eventHandler);
    }

    private void markTreeConflict(File localRelPath, SVNWCDbRoot wcRoot,
                                  SVNConflictVersion oldVersion, SVNConflictVersion newVersion,
                                  File moveRootDstRelPath, SVNOperation operation,
                                  SVNNodeKind oldKind, SVNNodeKind newKind,
                                  File oldReposRelPath,
                                  SVNConflictReason reason, SVNConflictAction action,
                                  File moveSrcOpRootRelPath) throws SVNException {
        File moveSrcOpRootAbsPath = moveSrcOpRootRelPath != null ? SVNFileUtil.createFilePath(wcRoot.getAbsPath(), moveSrcOpRootRelPath) : null;
        File oldReposRelPathPart = oldReposRelPath != null ? SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(oldVersion.getPath(), SVNFileUtil.getFilePath(oldReposRelPath))) : null;
        File newReposRelPath = oldReposRelPathPart != null ? SVNFileUtil.createFilePath(newVersion.getPath(), SVNFileUtil.getFilePath(oldReposRelPathPart)) : null;

        if (newReposRelPath == null) {
            newReposRelPath = SVNFileUtil.createFilePath(newVersion.getPath(),
                    SVNPathUtil.getRelativePath(SVNFileUtil.getFilePath(moveRootDstRelPath), SVNFileUtil.getFilePath(localRelPath)));
        }

        SVNSkel conflict;
        try {
            conflict = readConflictInternal(wcRoot, localRelPath);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            } else {
                conflict = null;
            }
        }

        if (conflict != null) {
            Structure<ConflictInfo> conflictInfoStructure = SvnWcDbConflicts.readConflictInfo(conflict);
            SVNOperation conflictOperation = conflictInfoStructure.get(ConflictInfo.conflictOperation);
            boolean treeConflicted = conflictInfoStructure.is(ConflictInfo.treeConflicted);

            if (conflictOperation != SVNOperation.UPDATE && conflictOperation != SVNOperation.SWITCH) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "'{0}' already in conflict", localRelPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            if (treeConflicted) {
                Structure<TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(this, wcRoot.getAbsPath(), conflict);
                SVNConflictReason existingReason = treeConflictInfoStructure.get(TreeConflictInfo.localChange);
                SVNConflictAction existingAction = treeConflictInfoStructure.get(TreeConflictInfo.incomingChange);
                File existingAbsPath = treeConflictInfoStructure.get(TreeConflictInfo.moveSrcOpRootAbsPath);

                if (reason != existingReason || action != existingAction || ((reason == SVNConflictReason.MOVED_AWAY) &&
                        !moveSrcOpRootAbsPath.equals(SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(SVNFileUtil.getFilePath(wcRoot.getAbsPath()), SVNFileUtil.getFilePath(existingAbsPath)))))) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "'{0}' already in conflict");
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }

                return;
            }
        } else {
            conflict = SvnWcDbConflicts.createConflictSkel();
        }

        SvnWcDbConflicts.addTreeConflict(conflict, this, SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelPath), reason, action, moveSrcOpRootAbsPath);

        SVNConflictVersion conflictOldVersion;
        if (reason != SVNConflictReason.UNVERSIONED) {
            conflictOldVersion = new SVNConflictVersion(oldVersion.getRepositoryRoot(), SVNFileUtil.getFilePath(oldReposRelPath), oldVersion.getPegRevision(), oldKind);
        } else {
            conflictOldVersion = null;
        }

        SVNConflictVersion conflictNewVersion = new SVNConflictVersion(newVersion.getRepositoryRoot(), SVNFileUtil.getFilePath(newReposRelPath), newVersion.getPegRevision(), newKind);

        if (operation == SVNOperation.UPDATE) {
            SvnWcDbConflicts.conflictSkelOpUpdate(conflict, conflictOldVersion, conflictNewVersion);
        } else {
            assert operation == SVNOperation.SWITCH;

            SvnWcDbConflicts.conflictSkelOpSwitch(conflict, conflictOldVersion, conflictNewVersion);
        }

        markConflictInternal(wcRoot, localRelPath, conflict);

        updateMoveListAdd(wcRoot, localRelPath, SVNEventAction.TREE_CONFLICT, newKind, SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE);
    }

    public void updateMoveListNotify(SVNWCDbRoot wcRoot, long oldRevision, long newRevision, ISVNEventHandler eventHandler) throws SVNException {
        SVNSqlJetStatement stmt;

        if (eventHandler != null) {
            stmt = wcRoot.getSDb().getTemporaryDb().getStatement(SVNWCDbStatements.SELECT_UPDATE_MOVE_LIST);
            try {
                boolean haveRow = stmt.next();
                while (haveRow) {
                    File localRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.local_relpath));
                    SVNEventAction eventAction = SvnWcDbStatementUtil.getColumnEventAction(stmt, SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.action);
                    SVNNodeKind kind = SvnWcDbStatementUtil.getColumnNodeKind(stmt, SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.kind);
                    SVNStatusType contentStatus = SvnWcDbStatementUtil.getColumnStatusType(stmt, SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.content_state);
                    SVNStatusType propStatus = SvnWcDbStatementUtil.getColumnStatusType(stmt, SVNWCDbSchema.UPDATE_MOVE_LIST__Fields.prop_state);

                    SVNEvent event = SVNEventFactory.createSVNEvent(SVNFileUtil.createFilePath(wcRoot.getAbsPath(), SVNFileUtil.getFilePath(localRelPath)),
                            kind, null, newRevision, contentStatus, propStatus, SVNStatusType.UNKNOWN, eventAction, eventAction, null, null);
                    event.setPreviousRevision(oldRevision);
                    eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }
        }

        if (wcRoot.getSDb().getTemporaryDb().hasTable(SVNWCDbSchema.UPDATE_MOVE_LIST.name())) {
            stmt = new SVNWCDbCreateSchema(wcRoot.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.FINALIZE_UPDATE_MOVE, -1, false);
            try {
                stmt.done();
            } finally {
                stmt.reset();
            }
        }
    }

    private TcInfo getTcInfo(File srcAbsPath) throws SVNException {
        SVNSkel conflictSkel = readConflict(srcAbsPath);
        if (conflictSkel == null) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE,
                    "'{0}' is not in conflict", srcAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        Structure<ConflictInfo> conflictInfoStructure = SvnWcDbConflicts.readConflictInfo(conflictSkel);
        boolean treeConflicted = conflictInfoStructure.is(ConflictInfo.treeConflicted);
        final List<SVNConflictVersion> locations = conflictInfoStructure.get(ConflictInfo.locations);
        SVNOperation operation = conflictInfoStructure.get(ConflictInfo.conflictOperation);

        if ((operation != SVNOperation.UPDATE && operation != SVNOperation.SWITCH) || !treeConflicted) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "'{0}' is not a tree-conflict victim", srcAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        SVNConflictVersion oldVersion = null;
        SVNConflictVersion newVersion = null;

        if (locations != null) {
            assert locations.size() >= 2;

            oldVersion = locations.get(0);
            newVersion = locations.get(1);
        }

        Structure<TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(this, srcAbsPath, conflictSkel);
        SVNConflictReason localChange = treeConflictInfoStructure.get(TreeConflictInfo.localChange);
        SVNConflictAction incomingChange = treeConflictInfoStructure.get(TreeConflictInfo.incomingChange);

        TcInfo tcInfo = new TcInfo();
        tcInfo.moveSrcOpRootAbsPath = treeConflictInfoStructure.get(TreeConflictInfo.moveSrcOpRootAbsPath); //TODO: unused!
        tcInfo.treeConflictDescription = new SVNTreeConflictDescription(srcAbsPath, SVNNodeKind.UNKNOWN, incomingChange, localChange, operation, oldVersion, newVersion);
        return tcInfo;
    }

    private static class TcInfo {
        public SVNTreeConflictDescription treeConflictDescription;
        public File moveSrcOpRootAbsPath;
    }

    public static void updateMoveListAdd(SVNWCDbRoot wcRoot, File localRelPath, SVNEventAction eventAction, SVNNodeKind kind, SVNStatusType contentState, SVNStatusType propState) throws SVNException {
        SVNSqlJetStatement stmt = wcRoot.getSDb().getTemporaryDb().getStatement(SVNWCDbStatements.INSERT_UPDATE_MOVE_LIST);
        try {
            stmt.bindf("siiii", localRelPath, eventAction.getID(), kind.getID(), contentState.getID(), propState.getID());
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public void updateMovedAwayConflictVictim(File victimAbsPath, ISVNEventHandler eventHandler) throws SVNException {
        TcInfo tcInfo = getTcInfo(victimAbsPath);
        SVNTreeConflictDescription conflictDescription = tcInfo.treeConflictDescription;
        File moveSrcOpRootAbsPath = tcInfo.moveSrcOpRootAbsPath;

        if (moveSrcOpRootAbsPath != null) {
            SVNWCContext.writeCheck(this, moveSrcOpRootAbsPath);

            DirParsedInfo parsed = parseDir(victimAbsPath, Mode.ReadOnly);
            SVNWCDbDir pdh = parsed.wcDbDir;
            File localRelPath = parsed.localRelPath;

            verifyDirUsable(pdh);

            File moveSrcOpRootRelPath = SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(SVNFileUtil.getFilePath(pdh.getWCRoot().getAbsPath()), SVNFileUtil.getFilePath(moveSrcOpRootAbsPath)));

            UpdateMovedAwayConflictVictim updateMovedAwayConflictVictim = new UpdateMovedAwayConflictVictim();
            updateMovedAwayConflictVictim.wcRoot = pdh.getWCRoot();
            updateMovedAwayConflictVictim.victimRelPath = localRelPath;
            updateMovedAwayConflictVictim.localChange = conflictDescription.getConflictReason();
            updateMovedAwayConflictVictim.incomingChange = conflictDescription.getConflictAction();
            updateMovedAwayConflictVictim.moveSrcOpRootRelPath = moveSrcOpRootRelPath;
            updateMovedAwayConflictVictim.oldVersion = conflictDescription.getSourceLeftVersion();
            updateMovedAwayConflictVictim.newVersion = conflictDescription.getSourceRightVersion();
            updateMovedAwayConflictVictim.operation = conflictDescription.getOperation();

            pdh.getWCRoot().getSDb().runTransaction(updateMovedAwayConflictVictim);

            updateMoveListNotify(pdh.getWCRoot(),
                    conflictDescription.getSourceLeftVersion().getPegRevision(), conflictDescription.getSourceRightVersion().getPegRevision(),
                    eventHandler);

            if (eventHandler != null) {
                SVNEvent event = SVNEventFactory.createSVNEvent(SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), localRelPath), SVNNodeKind.NONE, null,
                        conflictDescription.getSourceRightVersion().getPegRevision(), SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE,
                        SVNStatusType.LOCK_UNKNOWN, SVNEventAction.UPDATE_COMPLETED, SVNEventAction.UPDATE_COMPLETED, null, null);
                eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
            }
        }
    }

    private class UpdateMovedAwayConflictVictim implements SVNSqlJetTransaction {
        public SVNWCDbRoot wcRoot;
        public File victimRelPath;
        public SVNOperation operation;
        public SVNConflictReason localChange;
        public SVNConflictAction incomingChange;
        public File moveSrcOpRootRelPath;
        public SVNConflictVersion oldVersion;
        public SVNConflictVersion newVersion;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            BaseMovedTo movedTo = opDepthMovedTo(SVNWCUtils.relpathDepth(moveSrcOpRootRelPath) - 1, wcRoot, victimRelPath);
            File moveRootDstRelPath = movedTo.moveDstOpRootRelPath;

            if (moveRootDstRelPath == null) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE,
                        "The node '{0}' has not been moved away", SVNFileUtil.createFilePath(wcRoot.getAbsPath(), victimRelPath));
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            File moveRootDstAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), moveRootDstRelPath);
            SVNWCContext.writeCheck(SVNWCDb.this, moveRootDstAbsPath);

            SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_HIGHEST_WORKING_NODE);
            int srcOpDepth = 0;
            boolean haveRow;
            try {
                stmt.bindf("isi", wcRoot.getWcId(), moveSrcOpRootRelPath, SVNWCUtils.relpathDepth(moveSrcOpRootRelPath));
                haveRow = stmt.next();
                if (haveRow) {
                    srcOpDepth = (int) stmt.getColumnLong(NODES__Fields.op_depth);
                }
            } finally {
                stmt.reset();
            }

            if (!haveRow) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "'{0}' is not deleted",
                        SVNFileUtil.createFilePath(wcRoot.getAbsPath(), victimRelPath));
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            if (srcOpDepth == 0) {
                suitableForMove(wcRoot, victimRelPath);
            }

            stmt = new SVNWCDbCreateSchema(db.getTemporaryDb(), SVNWCDbCreateSchema.CREATE_UPDATE_MOVE_LIST, -1, false);
            try {
                stmt.done();
            } finally {
                stmt.reset();
            }

            SVNTreeConflictEditor treeConflictEditor = new SVNTreeConflictEditor(wcRoot.getDb(), operation, oldVersion, newVersion, wcRoot, moveRootDstRelPath);

            wcRoot.getDb().driveTreeConflictEditor(treeConflictEditor, victimRelPath, moveRootDstRelPath, srcOpDepth, operation, localChange, incomingChange, oldVersion, newVersion, wcRoot);
        }
    }

    private void driveTreeConflictEditor(SVNTreeConflictEditor treeConflictEditor,
                                         File srcRelPath, File dstRelPath,
                                         int srcOpDepth, SVNOperation operation,
                                         SVNConflictReason localChange, SVNConflictAction incomingChange,
                                         SVNConflictVersion oldVersion, SVNConflictVersion newVersion,
                                         SVNWCDbRoot wcRoot) throws SVNException {
        if (operation == SVNOperation.UPDATE && operation == SVNOperation.SWITCH) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE,
                    "Cannot auto-resolve tree-conflict on '{0}'", SVNFileUtil.createFilePath(wcRoot.getAbsPath(), srcRelPath));
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        updateMovedAwayNode(treeConflictEditor, srcRelPath, dstRelPath, srcOpDepth,
                dstRelPath, oldVersion.getPegRevision(), wcRoot);
        replaceMovedLayer(wcRoot, srcRelPath, dstRelPath, srcOpDepth);
        treeConflictEditor.complete();
    }

    private void updateMovedAwayNode(SVNTreeConflictEditor treeConflictEditor,
                                     File srcRelPath, File dstRelPath,
                                     int srcOpDepth,
                                     File moveRootDstRelPath, long moveRootDstRevision,
                                     SVNWCDbRoot wcRoot) throws SVNException {
        int dstOpDepth = SVNWCUtils.relpathDepth(moveRootDstRelPath);

        UpdateMovedAwayNodeInfo srcInfo = getInfo(wcRoot, srcRelPath, srcOpDepth);
        SVNProperties srcProps = srcInfo.props;
        SvnChecksum srcChecksum = srcInfo.checksum;
        List<String> srcChildren = srcInfo.children;
        SVNNodeKind srcKind = srcInfo.kind;

        UpdateMovedAwayNodeInfo dstInfo = getInfo(wcRoot, dstRelPath, dstOpDepth);
        SVNProperties dstProps = dstInfo.props;
        SvnChecksum dstChecksum = dstInfo.checksum;
        List<String> dstChildren = dstInfo.children;
        SVNNodeKind dstKind = dstInfo.kind;

        if (srcKind == SVNNodeKind.NONE || (dstKind != SVNNodeKind.NONE && srcKind != dstKind)) {
            treeConflictEditor.delete(SVNFileUtil.getFilePath(dstRelPath), moveRootDstRevision);
        }

        if (srcKind != SVNNodeKind.NONE && srcKind != dstKind) {
            if (srcKind == SVNNodeKind.FILE) { //TODO: check for symlink ?
                InputStream inputStream = SvnWcDbPristines.readPristine(wcRoot, wcRoot.getAbsPath(), srcChecksum);
                treeConflictEditor.addFile(SVNFileUtil.getFilePath(dstRelPath), srcChecksum, inputStream, srcProps, moveRootDstRevision);
            } else if (srcKind == SVNNodeKind.DIR) {
                treeConflictEditor.addDir(SVNFileUtil.getFilePath(dstRelPath), srcChildren, srcProps, moveRootDstRevision);
            }
        } else if (srcKind != SVNNodeKind.NONE) {
            boolean match = propsMatch(srcProps, dstProps);
            SVNProperties props = match ? null : srcProps;

            if (srcKind == SVNNodeKind.FILE) { //TODO: check for symlink ?
                if (SvnChecksum.match(srcChecksum, dstChecksum)) {
                    srcChecksum = null;
                }
                InputStream inputStream;
                if (srcChecksum != null) {
                    inputStream = SvnWcDbPristines.readPristine(wcRoot, wcRoot.getAbsPath(), srcChecksum);
                } else {
                    inputStream = null;
                }
                if (props != null || srcChecksum != null) {
                    treeConflictEditor.alterFile(SVNFileUtil.getFilePath(dstRelPath), moveRootDstRevision, props, srcChecksum, inputStream);
                }

            } else if (srcKind == SVNNodeKind.DIR) {
                List<String> children = childrenMatch(srcChildren, dstChildren) ? null : srcChildren;

                if (props != null || children != null) {
                    treeConflictEditor.alterDir(SVNFileUtil.getFilePath(dstRelPath), moveRootDstRevision, children, props);
                }
            }
        }

        if (srcKind == SVNNodeKind.DIR) {
            int i,j;
            i = j = 0;

            while (i < srcChildren.size() || j < dstChildren.size()) {
                String childName;
                boolean srcOnly = false;
                boolean dstOnly = false;

                if (i >= srcChildren.size()) {
                    dstOnly = true;
                    childName = dstChildren.get(j);
                } else if (j >= dstChildren.size()) {
                    srcOnly = true;
                    childName = srcChildren.get(i);
                } else {
                    String srcName = srcChildren.get(i);
                    String dstName = dstChildren.get(j);

                    int cmp = srcName.compareTo(dstName);

                    if (cmp > 0) {
                        dstOnly = true;
                    } else if (cmp < 0) {
                        srcOnly = true;
                    }

                    childName = dstOnly ? dstName : srcName;
                }

                File srcChildRelPath = SVNFileUtil.createFilePath(srcRelPath, childName);
                File dstChildRelPath = SVNFileUtil.createFilePath(dstRelPath, childName);

                updateMovedAwayNode(treeConflictEditor, srcChildRelPath, dstChildRelPath, srcOpDepth, moveRootDstRelPath, moveRootDstRevision, wcRoot);

                if (!dstOnly) {
                    i++;
                }
                if (!srcOnly) {
                    j++;
                }
            }
        }
    }

    private boolean propsMatch(SVNProperties srcProps, SVNProperties dstProps) {
        if (srcProps == null && dstProps == null) {
            return true;
        } else if (srcProps != null || dstProps != null) {
            return false;
        } else {
            SVNProperties diff = dstProps.compareTo(srcProps);
            return diff != null && diff.size() > 0 ? false : true;
        }
    }

    private boolean childrenMatch(List<String> srcChildren, List<String> dstChildren) {
        if (srcChildren.size() != dstChildren.size()) {
            return false;
        }

        for (int i = 0; i < srcChildren.size(); i++) {
            String srcChild = srcChildren.get(i);
            String dstChild = dstChildren.get(i);

            if (!srcChild.equals(dstChild)) {
                return false;
            }
        }
        return true;
    }

    private void replaceMovedLayer(SVNWCDbRoot wcRoot, File srcRelPath, File dstRelPath, int srcOpDepth) throws SVNException {
        int dstOpDepth = SVNWCUtils.relpathDepth(dstRelPath);

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_LOCAL_RELPATH_OP_DEPTH);
        try {
            stmt.bindf("isi", wcRoot.getWcId(), srcRelPath, srcOpDepth);
            boolean haveRow = stmt.next();

            while (haveRow) {
                File srcCpRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.local_relpath));
                File dstCpRelPath = SVNFileUtil.createFilePath(dstRelPath, SVNFileUtil.skipAncestor(srcRelPath, srcCpRelPath));

                SVNSqlJetStatement stmt2 = wcRoot.getSDb().getStatement(SVNWCDbStatements.COPY_MOVE_NODE);
                try {
                    stmt2.bindf("isisis", wcRoot.getWcId(),
                            srcCpRelPath, srcOpDepth,
                            dstCpRelPath, dstOpDepth,
                            SVNFileUtil.getFileDir(dstCpRelPath));
                    stmt2.done();
                } finally {
                    stmt2.reset();
                }


                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }
    }

    private UpdateMovedAwayNodeInfo getInfo(SVNWCDbRoot wcRoot, File localRelPath, int opDepth) throws SVNException {
        UpdateMovedAwayNodeInfo updateMovedAwayNodeInfo = new UpdateMovedAwayNodeInfo();

        try {
            Structure<NodeInfo> depthInfo = SvnWcDbShared.getDepthInfo(wcRoot, localRelPath, opDepth, NodeInfo.kind, NodeInfo.checksum, NodeInfo.propsMod);
            updateMovedAwayNodeInfo.kind = depthInfo.get(NodeInfo.kind) == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;
            updateMovedAwayNodeInfo.checksum = depthInfo.get(NodeInfo.checksum);
            updateMovedAwayNodeInfo.props = depthInfo.get(NodeInfo.propsMod);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                updateMovedAwayNodeInfo.kind = SVNNodeKind.NONE;
            } else {
                throw e;
            }
        }

        Set<String> children = wcRoot.getDb().getChildrenOpDepth(wcRoot, localRelPath, opDepth).keySet();
        updateMovedAwayNodeInfo.children = new ArrayList<String>(children);
        Collections.sort(updateMovedAwayNodeInfo.children);
        return updateMovedAwayNodeInfo;
    }

    private static class UpdateMovedAwayNodeInfo {
        public SVNProperties props;
        public SvnChecksum checksum;
        public List<String> children;
        public SVNNodeKind kind;
    }

    private void suitableForMove(SVNWCDbRoot wcRoot, File localRelPath) throws SVNException {
        long revision = -1;
        File reposRelPath = null;
        boolean haveRow;

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_BASE_NODE);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            haveRow = stmt.next();
            if (haveRow) {
                revision = stmt.getColumnLong(NODES__Fields.revision);
                reposRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.repos_path));
            }
        } finally {
            stmt.reset();
        }

        if (!haveRow) {
            return;
        }

        stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_REPOS_PATH_REVISION);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            haveRow = stmt.next();

            while (haveRow) {
                long nodeRevision = stmt.getColumnLong(NODES__Fields.revision);
                File relPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.local_relpath));

                relPath = SVNFileUtil.skipAncestor(localRelPath, relPath);
                relPath = SVNFileUtil.createFilePath(reposRelPath, relPath);

                if (revision != nodeRevision) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE,
                            "Cannot apply update because move source '{0}' is a mixed-revision working copy",
                            SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelPath));
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }

                if (!relPath.equals(SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.repos_path)))) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE,
                            "Cannot apply update because move source '{0}' is a switched subtree",
                            SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelPath));
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }

                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }
    }

    public void wqAdd(File wriAbsPath, SVNSkel workItem) throws SVNException {
        assert isAbsolute(wriAbsPath);

        if (workItem == null) {
            return;
        }

        DirParsedInfo parsed = parseDir(wriAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        addWorkItems(pdh.getWCRoot().getSDb(), workItem);
    }

    public Map<String, SVNWCDbKind> getChildrenOpDepth(SVNWCDbRoot wcRoot, File localRelPath, int opDepth) throws SVNException {
        Map<String, SVNWCDbKind> relPathToKind = new HashMap<String, SVNWCDbKind>();

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_OP_DEPTH_CHILDREN);
        try {
            stmt.bindf("isi", wcRoot.getWcId(), localRelPath, opDepth);
            boolean haveRow = stmt.next();

            while (haveRow) {
                File childRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.local_relpath));
                SVNWCDbKind childKind = SvnWcDbStatementUtil.getColumnKind(stmt, NODES__Fields.kind);

                relPathToKind.put(SVNFileUtil.getFileName(childRelPath), childKind);

                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }

        return relPathToKind;
    }

    public SwitchedInfo isSwitched(File localAbsPath) throws SVNException {
        assert isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        File localRelPath = parsed.localRelPath;

        SwitchedInfo switchedInfo = new SwitchedInfo();
        switchedInfo.isSwitched = false;
        if (localRelPath.getPath().length() == 0) {
            switchedInfo.isWcRoot = true;
            switchedInfo.kind = SVNWCDbKind.Dir;
            return switchedInfo;
        }
        switchedInfo.isWcRoot = false;

        IsSwitched is = new IsSwitched();
        is.wcRoot = pdh.getWCRoot();
        is.localRelPath = localRelPath;

        pdh.getWCRoot().getSDb().runTransaction(is);

        switchedInfo.isSwitched = is.isSwitched;
        switchedInfo.kind = is.kind;
        return switchedInfo;
    }

    private class IsSwitched implements SVNSqlJetTransaction {
        private SVNWCDbRoot wcRoot;
        private File localRelPath;

        private boolean isSwitched;
        private SVNWCDbKind kind;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            Structure<NodeInfo> nodeInfoStructure = SvnWcDbShared.readInfo(wcRoot, localRelPath, NodeInfo.status, NodeInfo.kind,
                    NodeInfo.reposRelPath, NodeInfo.reposId);
            SVNWCDbStatus status = nodeInfoStructure.get(NodeInfo.status);
            kind = nodeInfoStructure.get(NodeInfo.kind);
            File reposRelPath = nodeInfoStructure.get(NodeInfo.reposRelPath);
            long reposId = nodeInfoStructure.lng(NodeInfo.reposId);

            if (status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded || status == SVNWCDbStatus.NotPresent) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node '{0}' was not found.", SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelPath));
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            } else if (reposRelPath == null) {
                isSwitched = false;
                return;
            }

            File parentLocalRelPath = SVNFileUtil.getFileDir(localRelPath);
            String name = SVNFileUtil.getFileName(localRelPath);

            WCDbBaseInfo baseInfo = getBaseInfo(wcRoot, parentLocalRelPath, BaseInfoField.reposRelPath, BaseInfoField.reposId);
            File parentReposRelPath = baseInfo.reposRelPath;
            long parentReposId = baseInfo.reposId;

            if (reposId != parentReposId) {
                isSwitched = true;
            } else {
                File expectedPath = SVNFileUtil.createFilePath(parentReposRelPath, name);
                isSwitched = !expectedPath.equals(reposRelPath);
            }
        }
    }

    public List<File> getConflictMarkerFiles(File localAbsPath) throws SVNException {
        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        if (pdh.getWCRoot().getFormat() == ISVNWCDb.WC_FORMAT_17) {
            GetConflictMarkerFiles17 conflictMarkerFiles = new GetConflictMarkerFiles17();
            conflictMarkerFiles.wcRoot = pdh.getWCRoot();
            conflictMarkerFiles.localRelPath = parsed.localRelPath;
            pdh.getWCRoot().getSDb().runTransaction(conflictMarkerFiles);
            return conflictMarkerFiles.markerFiles;
        } else {
            GetConflictMarkerFiles conflictMarkerFiles = new GetConflictMarkerFiles();
            conflictMarkerFiles.wcRoot = pdh.getWCRoot();
            conflictMarkerFiles.localRelPath = parsed.localRelPath;
            pdh.getWCRoot().getSDb().runTransaction(conflictMarkerFiles);
            return conflictMarkerFiles.markerFiles;
        }
    }

    private static class GetConflictMarkerFiles implements SVNSqlJetTransaction {
        public SVNWCDbRoot wcRoot;
        public File localRelPath;

        public List<File> markerFiles;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            markerFiles = new ArrayList<File>();

            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
            try {
                stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                boolean haveRow = stmt.next();
                if (haveRow && !stmt.isColumnNull(ACTUAL_NODE__Fields.conflict_data)) {
                    byte[] conflictData = stmt.getColumnBlob(ACTUAL_NODE__Fields.conflict_data);
                    SVNSkel conflicts = SVNSkel.parse(conflictData);

                    List<File> markers = SvnWcDbConflicts.readConflictMarkers(wcRoot.getDb(), wcRoot.getAbsPath(), conflicts);
                    if (markers != null) {
                        markerFiles.addAll(markers);
                    }
                }
            } finally {
                stmt.reset();
            }

            stmt = db.getStatement(SVNWCDbStatements.SELECT_CONFLICT_VICTIMS);
            try {
                stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                boolean haveRow = stmt.next();

                while (haveRow) {
                    byte[] conflictData = stmt.getColumnBlob(ACTUAL_NODE__Fields.conflict_data);
                    if (conflictData != null) {
                        SVNSkel conflicts = SVNSkel.parse(conflictData);
                        List<File> markers = SvnWcDbConflicts.readConflictMarkers(wcRoot.getDb(), wcRoot.getAbsPath(), conflicts);
                        if (markers != null) {
                            markerFiles.addAll(markers);
                        }
                    }
                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }

            if (markerFiles.size() == 0) {
                markerFiles = null;
            }
        }
    }

    private static class GetConflictMarkerFiles17 implements SVNSqlJetTransaction {
        public SVNWCDbRoot wcRoot;
        public File localRelPath;

        public List<File> markerFiles;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            markerFiles = new ArrayList<File>();

            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
            try {
                stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                boolean haveRow = stmt.next();
                if (haveRow && (
                        !stmt.isColumnNull(ACTUAL_NODE__Fields.conflict_old) ||
                        !stmt.isColumnNull(ACTUAL_NODE__Fields.conflict_new) ||
                        !stmt.isColumnNull(ACTUAL_NODE__Fields.conflict_working)) ||
                        !stmt.isColumnNull(ACTUAL_NODE__Fields.prop_reject)) {
                    String conflictOldRelPath = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_old);
                    String conflictNewRelPath = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_new);
                    String conflictWorkingRelPath = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_working);
                    String prejRelPath = stmt.getColumnString(ACTUAL_NODE__Fields.prop_reject);

                    File conflictOldAbsPath = conflictOldRelPath == null ? null : SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictOldRelPath);
                    File conflictNewAbsPath = conflictNewRelPath == null ? null : SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictNewRelPath);
                    File conflictWorkingAbsPath = conflictWorkingRelPath == null ? null : SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictWorkingRelPath);
                    File prejAbsPath = prejRelPath == null ? null : SVNFileUtil.createFilePath(wcRoot.getAbsPath(), prejRelPath);

                    if (conflictOldAbsPath != null) {
                        markerFiles.add(conflictOldAbsPath);
                    }
                    if (conflictNewAbsPath != null) {
                        markerFiles.add(conflictNewAbsPath);
                    }
                    if (conflictWorkingAbsPath != null) {
                        markerFiles.add(conflictWorkingAbsPath);
                    }
                    if (prejAbsPath != null) {
                        markerFiles.add(prejAbsPath);
                    }
                }
            } finally {
                stmt.reset();
            }

            stmt = db.getStatement(SVNWCDbStatements.SELECT_CONFLICT_VICTIMS_17);
            try {
                stmt.bindf("is", wcRoot.getWcId(), localRelPath);
                boolean haveRow = stmt.next();

                while (haveRow) {
                    String conflictOldRelPath = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_old);
                    String conflictNewRelPath = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_new);
                    String conflictWorkingRelPath = stmt.getColumnString(ACTUAL_NODE__Fields.conflict_working);
                    String prejRelPath = stmt.getColumnString(ACTUAL_NODE__Fields.prop_reject);

                    File conflictOldAbsPath = conflictOldRelPath == null ? null : SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictOldRelPath);
                    File conflictNewAbsPath = conflictNewRelPath == null ? null : SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictNewRelPath);
                    File conflictWorkingAbsPath = conflictWorkingRelPath == null ? null : SVNFileUtil.createFilePath(wcRoot.getAbsPath(), conflictWorkingRelPath);
                    File prejAbsPath = prejRelPath == null ? null : SVNFileUtil.createFilePath(wcRoot.getAbsPath(), prejRelPath);

                    if (conflictOldAbsPath != null) {
                        markerFiles.add(conflictOldAbsPath);
                    }
                    if (conflictNewAbsPath != null) {
                        markerFiles.add(conflictNewAbsPath);
                    }
                    if (conflictWorkingAbsPath != null) {
                        markerFiles.add(conflictWorkingAbsPath);
                    }
                    if (prejAbsPath != null) {
                        markerFiles.add(prejAbsPath);
                    }
                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }

            if (markerFiles.size() == 0) {
                markerFiles = null;
            }
        }
    }

    public long[] minMaxRevisions(File localAbsPath, boolean committed) throws SVNException {
        assert isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        return getMinMaxRevisions(pdh.getWCRoot(), parsed.localRelPath, committed);
    }

    private long[] getMinMaxRevisions(SVNWCDbRoot wcRoot, File localRelPath, boolean committed) throws SVNException {
        long minRevision, maxRevision;
        SVNWCDbSelectMinMaxRevisions stmt = (SVNWCDbSelectMinMaxRevisions) wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_MIN_MAX_REVISIONS);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            stmt.next();
            if (committed) {
                minRevision = stmt.getMinChangedRevision();
                maxRevision = stmt.getMaxChangedRevision();
            } else {
                minRevision = stmt.getMinRevision();
                maxRevision = stmt.getMaxRevision();
            }
        } finally {
            stmt.reset();
        }
        return new long[] {minRevision, maxRevision};
    }

    public boolean opHandleMoveBack(File localAbsPath, File movedFromAbsPath, SVNSkel workItems) throws SVNException {
        assert isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;

        verifyDirUsable(pdh);

        SVNWCDbRoot wcRoot = pdh.getWCRoot();

        File movedFromRelPath = SVNFileUtil.skipAncestor(wcRoot.getAbsPath(), movedFromAbsPath);
        if ("".equals(parsed.localRelPath) || movedFromRelPath == null) {
            addWorkItems(wcRoot.getSDb(), workItems);
            return false;
        }

        HandleMoveBack handleMoveBack = new HandleMoveBack();
        handleMoveBack.wcRoot = wcRoot;
        handleMoveBack.localRelPath = parsed.localRelPath;
        handleMoveBack.movedFromRelPath = movedFromRelPath;
        handleMoveBack.workItems = workItems;

        wcRoot.getSDb().runTransaction(handleMoveBack);

        return handleMoveBack.movedBack;
    }

    private static class HandleMoveBack implements SVNSqlJetTransaction {
        public SVNWCDbRoot wcRoot;
        public File localRelPath;
        public File movedFromRelPath;
        public SVNSkel workItems;

        public boolean movedBack;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            movedBack = false;
            boolean different = false;

            addWorkItems(db, workItems);

            Structure<NodeInfo> nodeInfoStructure = SvnWcDbShared.readInfo(wcRoot, localRelPath, NodeInfo.status, NodeInfo.opRoot, NodeInfo.haveMoreWork);
            SVNWCDbStatus status = nodeInfoStructure.get(NodeInfo.status);
            boolean opRoot = nodeInfoStructure.is(NodeInfo.opRoot);
            boolean haveMoreWork = nodeInfoStructure.is(NodeInfo.haveMoreWork);

            if (status != SVNWCDbStatus.Added || !opRoot) {
                movedBack = false;
                return;
            }

            int fromOpDepth;
            if (haveMoreWork) {
                fromOpDepth = opDepthOf(wcRoot, localRelPath);
            } else {
                fromOpDepth = 0;
            }

            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_MOVED_BACK);
            try {
                stmt.bindf("isii", wcRoot.getWcId(), localRelPath, fromOpDepth, SVNWCUtils.relpathDepth(localRelPath));
                boolean haveRow = stmt.next();
                assert haveRow;

                boolean movedHere = stmt.getColumnBoolean(NODES__Fields.moved_here);
                File movedTo = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.moved_to));

                if (!movedHere || movedTo == null || !movedTo.equals(movedFromRelPath)) {
                    different = true;
                    haveRow = false;
                }

                while (haveRow) {
                    SVNWCDbStatus upperStatus = SvnWcDbStatementUtil.getColumnPresence(stmt, NODES__Fields.presence);
                    if (stmt.getJoinedStatement(SVNWCDbSchema.NODES).isColumnNull(NODES__Fields.presence)) {
                        if (upperStatus != SVNWCDbStatus.NotPresent) {
                            different = true;
                            break;
                        }
                        continue;
                    }

                    SVNWCDbStatus lowerStatus = SvnWcDbStatementUtil.getColumnPresence(stmt.getJoinedStatement(SVNWCDbSchema.NODES), NODES__Fields.presence);
                    if (upperStatus != lowerStatus) {
                        different = true;
                        break;
                    }

                    if (upperStatus == SVNWCDbStatus.NotPresent || upperStatus == SVNWCDbStatus.Excluded) {
                        haveRow = stmt.next();
                        continue;
                    } else if (upperStatus != SVNWCDbStatus.Normal) {
                        different = true;
                        break;
                    }

                    File upperReposRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.repos_path));
                    File lowerReposRelPath = SVNFileUtil.createFilePath(stmt.getJoinedStatement(SVNWCDbSchema.NODES).getColumnString(NODES__Fields.repos_path));

                    if (upperReposRelPath == null || !upperReposRelPath.equals(lowerReposRelPath)) {
                        different = true;
                        break;
                    }

                    long upperRevision = stmt.getColumnLong(NODES__Fields.revision);
                    long lowerRevision = stmt.getJoinedStatement(SVNWCDbSchema.NODES).getColumnLong(NODES__Fields.revision);

                    if (upperRevision != lowerRevision) {
                        different = true;
                        break;
                    }

                    int upperReposId = (int) stmt.getColumnLong(NODES__Fields.repos_id);
                    int lowerReposId = (int) stmt.getJoinedStatement(SVNWCDbSchema.NODES).getColumnLong(NODES__Fields.repos_id);

                    if (upperReposId != lowerReposId) {
                        different = true;
                        break;
                    }
                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }

            if (!different) {
                stmt = db.getStatement(SVNWCDbStatements.DELETE_MOVED_BACK);
                try {
                    stmt.bindf("isi", wcRoot.getWcId(), localRelPath, SVNWCUtils.relpathDepth(localRelPath));
                    stmt.done();
                } finally {
                    stmt.reset();
                }

                movedBack = true;
            }
        }
    }

    private static int opDepthOf(SVNWCDbRoot wcRoot, File localRelPath) throws SVNException {
        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
        try {
            stmt.bindf("is", wcRoot.getWcId(), localRelPath);
            boolean haveRow = stmt.next();
            assert haveRow;
            return (int)stmt.getColumnLong(NODES__Fields.op_depth);
        } finally {
            stmt.reset();
        }
    }

    public File requiredLockForResolve(File localAbsPath) throws SVNException {
        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        RequiredLockForResolve requiredLockForResolve = new RequiredLockForResolve();
        requiredLockForResolve.wcRoot = pdh.getWCRoot();
        requiredLockForResolve.localRelPath = parsed.localRelPath;

        pdh.getWCRoot().getSDb().runTransaction(requiredLockForResolve);

        return SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), requiredLockForResolve.requiredRelPath);
    }

    private static class RequiredLockForResolve implements SVNSqlJetTransaction {
        public SVNWCDbRoot wcRoot;
        public File localRelPath;

        public File requiredRelPath;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            requiredRelPath = localRelPath;

            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.SELECT_MOVED_OUTSIDE);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), localRelPath, 0);
                boolean haveRow = stmt.next();
                while (haveRow) {
                    File moveDstRelPath = SVNFileUtil.createFilePath(stmt.getColumnString(NODES__Fields.moved_to));
                    requiredRelPath = SVNFileUtil.createFilePath(SVNPathUtil.getCommonPathAncestor(SVNFileUtil.getFilePath(requiredRelPath), SVNFileUtil.getFilePath(moveDstRelPath)));
                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }
        }
    }

    public Map<SVNURL, String> getNodeLockTokensRecursive(File localAbsPath) throws SVNException {
        assert isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        long lastReposId = -1;
        ReposInfo reposInfo = null;
        Map<SVNURL, String> lockTokens = new HashMap<SVNURL, String>();

        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_BASE_NODE_LOCK_TOKENS_RECURSIVE);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), parsed.localRelPath);
            boolean haveRow = stmt.next();
            while (haveRow) {
                long childReposId = stmt.getColumnLong(NODES__Fields.repos_id);
                File childRelPath = SvnWcDbStatementUtil.getColumnPath(stmt, NODES__Fields.repos_path);
                String lockToken = stmt.getJoinedStatement(SVNWCDbSchema.LOCK).getColumnString(SVNWCDbSchema.LOCK__Fields.lock_token);
                if (childReposId != lastReposId) {
                    reposInfo = fetchReposInfo(pdh.getWCRoot().getSDb(), childReposId);
                    lastReposId = childReposId;
                }
                assert reposInfo.reposRootUrl != null;
                lockTokens.put(SVNURL.parseURIEncoded(reposInfo.reposRootUrl).appendPath(SVNFileUtil.getFilePath(childRelPath), false),
                        lockToken);
                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }

        return lockTokens;
    }

    public List<SVNWCContext.CommittableExternalInfo> committableExternalsBelow(File localAbsPath, boolean immediatesOnly) throws SVNException {
        List<SVNWCContext.CommittableExternalInfo> result = null;
        SVNWCContext.CommittableExternalInfo info;

        assert isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);

        File localRelPath = parsed.localRelPath;

        SVNWCDbSelectCommittableExternalsImmediatelyBelow stmt = (SVNWCDbSelectCommittableExternalsImmediatelyBelow) pdh.getWCRoot().getSDb().getStatement(immediatesOnly ? SVNWCDbStatements.SELECT_COMMITTABLE_EXTERNALS_IMMEDIATELY_BELOW : SVNWCDbStatements.SELECT_COMMITTABLE_EXTERNALS_BELOW);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelPath);
            boolean haveRow = stmt.next();
            if (haveRow) {
                result = new ArrayList<SVNWCContext.CommittableExternalInfo>();
            }
            while (haveRow) {
                info = new SVNWCContext.CommittableExternalInfo();
                localRelPath = SvnWcDbStatementUtil.getColumnPath(stmt, SVNWCDbSchema.EXTERNALS__Fields.local_relpath);
                info.localAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), localRelPath);
                SVNWCDbKind dbKind = SvnWcDbStatementUtil.getColumnKind(stmt, SVNWCDbSchema.EXTERNALS__Fields.kind);
                info.kind = dbKind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;
                info.reposRelPath = SvnWcDbStatementUtil.getColumnPath(stmt, SVNWCDbSchema.EXTERNALS__Fields.def_repos_relpath);
                info.reposRootUrl = SVNURL.parseURIEncoded(SvnWcDbStatementUtil.getColumnText(stmt.getInternalStatement1(), REPOSITORY__Fields.root));
                result.add(info);
                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }
        return result;
    }

    public Moved scanMoved(File localAbsPath) throws SVNException {
        assert isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        File localRelPath = parsed.localRelPath;

        Structure<StructureFields.AdditionInfo> additionInfoStructure = SvnWcDbShared.scanAddition(pdh.getWCRoot(), localRelPath, StructureFields.AdditionInfo.status,
                StructureFields.AdditionInfo.opRootRelPath, StructureFields.AdditionInfo.movedFromRelPath,
                StructureFields.AdditionInfo.movedFromOpRootRelPath, StructureFields.AdditionInfo.movedFromOpDepth);
        SVNWCDbStatus status = additionInfoStructure.get(StructureFields.AdditionInfo.status);
        File opRootRelPath = additionInfoStructure.get(StructureFields.AdditionInfo.opRootRelPath);
        File movedFromRelPath = additionInfoStructure.get(StructureFields.AdditionInfo.movedFromRelPath);
        File movedFromOpRootRelPath = additionInfoStructure.get(StructureFields.AdditionInfo.movedFromOpRootRelPath);
        long movedFromOpDepth = additionInfoStructure.lng(StructureFields.AdditionInfo.movedFromOpDepth);

        if (status != SVNWCDbStatus.MovedHere) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Path ''{0}'' was not moved here", SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), localRelPath));
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        Moved moved = new Moved();
        moved.opRootAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), opRootRelPath);
        moved.movedFromAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), movedFromRelPath);
        moved.opRootMovedFromAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), movedFromOpRootRelPath);

        File tmp = movedFromOpRootRelPath;

        assert movedFromOpDepth >= 0;

        while (SVNWCUtils.relpathDepth(tmp) > movedFromOpDepth) {
            tmp = SVNFileUtil.getFileDir(tmp);
        }

        moved.movedFromDeleteAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), tmp);
        return moved;
    }

    public void dropRoot(File localAbsPath) throws SVNException {
        SVNWCDbDir rootDir = dirData.get(localAbsPath);
        if (rootDir == null) {
            return;
        }
        if (!rootDir.getWCRoot().getAbsPath().equals(localAbsPath)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY, "''{0}'' is not a working copy root", localAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        for (Iterator<Entry<String, SVNWCDbDir>> iterator = dirData.entrySet().iterator(); iterator.hasNext(); ) {
            final Entry<String, SVNWCDbDir> entry = iterator.next();
            SVNWCDbDir wcDir = entry.getValue();
            if (rootDir == wcDir) {
                iterator.remove();
            }
        }
        rootDir.getWCRoot().close();
    }

    public void upgradeInsertExternal(File localAbsPath, SVNNodeKind kind, File parentAbsPath, File defLocalAbsPath, File reposRelPath, SVNURL reposRootUrl, String reposUuid, long defPegRevision, long defRevision) throws SVNException {
        assert SVNFileUtil.isAbsolute(localAbsPath);

        DirParsedInfo parsed = parseDir(defLocalAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File defLocalRelPath = parsed.localRelPath;

        verifyDirUsable(pdh);

        long reposId = -1;
        boolean haveRow = false;

        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_REPOSITORY);
        try {
            stmt.bindf("s", reposRootUrl);
            haveRow = stmt.next();
            if (haveRow) {
                reposId = stmt.getColumnLong(REPOSITORY__Fields.id);
            }
        } finally {
            stmt.reset();
        }
        if (!haveRow) {
            createReposId(pdh.getWCRoot().getSDb(), reposRootUrl, reposUuid);
        }
        stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.INSERT_EXTERNAL);
        try {
            stmt.bindf("isssssis",
                    pdh.getWCRoot().getWcId(),
                    SVNFileUtil.skipAncestor(pdh.getWCRoot().getAbsPath(), localAbsPath),
                    SVNFileUtil.skipAncestor(pdh.getWCRoot().getAbsPath(), parentAbsPath),
                    SvnWcDbStatementUtil.getPresenceText(SVNWCDbStatus.Normal),
                    SvnWcDbStatementUtil.getKindText(kind == SVNNodeKind.DIR ? SVNWCDbKind.Dir : SVNWCDbKind.File),
                    defLocalRelPath,
                    reposId,
                    reposRelPath);

            if (SVNRevision.isValidRevisionNumber(defPegRevision)) {
                stmt.bindLong(9, defPegRevision);
            }
            if (SVNRevision.isValidRevisionNumber(defRevision)) {
                stmt.bindLong(10, defRevision);
            }
            stmt.done();
        } finally {
            stmt.reset();
        }
    }
}
