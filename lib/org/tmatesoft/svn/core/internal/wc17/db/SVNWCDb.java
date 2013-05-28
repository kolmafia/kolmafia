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
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
import org.tmatesoft.svn.core.internal.wc17.SVNExternalsStore;
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
import org.tmatesoft.svn.core.internal.wc17.db.statement.*;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.*;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared.*;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.*;

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
    
    public SVNWCDb() {
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

    public void init(File localAbsPath, File reposRelPath, SVNURL reposRootUrl, String reposUuid, long initialRev, SVNDepth depth) throws SVNException {

        assert (SVNFileUtil.isAbsolute(localAbsPath));
        assert (reposRelPath != null);
        assert (depth == SVNDepth.EMPTY || depth == SVNDepth.FILES || depth == SVNDepth.IMMEDIATES || depth == SVNDepth.INFINITY);

        /* ### REPOS_ROOT_URL and REPOS_UUID may be NULL. ... more doc: tbd */

        /* Create the SDB and insert the basic rows. */
        CreateDbInfo createDb = createDb(localAbsPath, reposRootUrl, reposUuid, SDB_FILE);

        /* Begin construction of the PDH. */
        SVNWCDbDir pdh = new SVNWCDbDir(localAbsPath);

        /* Create the WCROOT for this directory. */
        pdh.setWCRoot(new SVNWCDbRoot(this, localAbsPath, createDb.sDb, createDb.wcId, FORMAT_FROM_SDB, false, false));

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
        ibb.localRelpath = SVNFileUtil.createFilePath("");
        /* ### no children, conflicts, or work items to install in a txn... */

        createDb.sDb.runTransaction(ibb);
    }

    private static class CreateDbInfo {

        public SVNSqlJetDb sDb;
        public long reposId;
        public long wcId;
    }

    private CreateDbInfo createDb(File dirAbsPath, SVNURL reposRootUrl, String reposUuid, String sdbFileName) throws SVNException {

        CreateDbInfo info = new CreateDbInfo();

        info.sDb = openDb(dirAbsPath, sdbFileName, SVNSqlJetDb.Mode.RWCreate, journalMode);

        /* Create the database's schema. */
        info.sDb.execStatement(SVNWCDbStatements.CREATE_SCHEMA);

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

    private void addWorkItems(SVNSqlJetDb sDb, SVNSkel skel) throws SVNException {
        /* Maybe there are no work items to insert. */
        if (skel == null) {
            return;
        }

        /* Is the list a single work item? Or a list of work items? */
        if (skel.isAtom()) {
            addSingleWorkItem(sDb, skel);
        } else {
            /* SKEL is a list-of-lists, aka list of work items. */
            for (int i = 0; i < skel.getListSize(); i++) {
                addSingleWorkItem(sDb, skel.getChild(i));
            }
        }

    }

    private void addSingleWorkItem(SVNSqlJetDb sDb, SVNSkel workItem) throws SVNException {
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
            List<File> children, SVNDepth depth, SVNProperties davCache, SVNSkel conflict, boolean updateActualProps, SVNProperties actualProps, SVNSkel workItems) throws SVNException {
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
        
        ibb.localRelpath = localRelpath;
        ibb.wcId = pdh.getWCRoot().getWcId();
            
        pdh.getWCRoot().getSDb().runTransaction(ibb);
        pdh.flushEntries(localAbsPath);
    }
    
    private class Delete implements SVNSqlJetTransaction {
        
        public SVNWCDbRoot root;
        public File localRelPath;
        public long deleteDepth;
        public ISVNEventHandler eventHandler;
        
        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            WCDbInfo info = readInfo(root, localRelPath, InfoField.status, InfoField.opRoot);
            SVNWCDbStatus status = info.status;
            if (status == SVNWCDbStatus.Deleted || status == SVNWCDbStatus.NotPresent) {
                return;
            }
            SVNSqlJetStatement stmt = root.getSDb().getStatement(SVNWCDbStatements.HAS_SERVER_EXCLUDED_NODES);
            try {
                stmt.bindf("is", root.getWcId(), localRelPath);                
                if (stmt.next()) {
                    File absentPath = getColumnPath(stmt, SVNWCDbSchema.NODES__Fields.local_relpath);
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS,
                            "Cannot delete ''{0}'' as ''{1}'' is excluded by server", localRelPath, absentPath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            } finally {
                stmt.reset();
            }
            boolean addWork = false;
            boolean refetchDepth = false;
            long selectDepth;

            
            if (info.opRoot) {
                WCDbInfo infoBelow = readInfoBelowWorking(root, localRelPath, -1);
                if ((infoBelow.haveBase || infoBelow.haveWork) 
                        && infoBelow.status != SVNWCDbStatus.NotPresent 
                        && infoBelow.status != SVNWCDbStatus.Deleted) {
                    addWork = true;
                    refetchDepth = true;
                }
                selectDepth = SVNWCUtils.relpathDepth(localRelPath);
            } else {
                addWork = true;
                selectDepth = readOpDepth(root, localRelPath);
            } 
            // collect files to delete.
            stmt = new SVNWCDbCreateSchema(root.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.DELETE_LIST, -1);
            try {
                stmt.done();
            } finally {
                stmt.reset();
            }
            
            if (eventHandler != null) {
                stmt = new SVNWCDbInsertDeleteList(root.getSDb());
                try {
                    stmt.bindf("isi", root.getWcId(), localRelPath, selectDepth);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }

            SVNSqlJetStatement deleteStmt = root.getSDb().getStatement(SVNWCDbStatements.DELETE_NODES_RECURSIVE);
            try {
                deleteStmt.bindf("isi", root.getWcId(), localRelPath, deleteDepth);
                deleteStmt.done();
            } finally {
               deleteStmt.reset();
            }
            
            if (refetchDepth) {
                selectDepth = readOpDepth(root, localRelPath);
            }
            deleteStmt = root.getSDb().getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_LEAVING_CHANGELIST_RECURSIVE);
            try {
                deleteStmt.bindf("is", root.getWcId(), localRelPath);
                deleteStmt.done();
            } finally {
                deleteStmt.reset();
            }

            deleteStmt = root.getSDb().getStatement(SVNWCDbStatements.CLEAR_ACTUAL_NODE_LEAVING_CHANGELIST_RECURSIVE);
            try {
                deleteStmt.bindf("is", root.getWcId(), localRelPath);
                deleteStmt.done();
            } finally {
                deleteStmt.reset();
            }
            
            deleteStmt = root.getSDb().getStatement(SVNWCDbStatements.DELETE_WC_LOCK_ORPHAN_RECURSIVE);
            try {
                deleteStmt.bindf("is", root.getWcId(), localRelPath);
                deleteStmt.done();
            } finally {
                deleteStmt.reset();
            }
            
            if (addWork) {
                SVNSqlJetStatement insertStmt = root.getSDb().getStatement(SVNWCDbStatements.INSERT_DELETE_FROM_NODE_RECURSIVE);
                try {
                    insertStmt.bindf("isii", root.getWcId(), localRelPath, selectDepth, deleteDepth);
                    insertStmt.done();
                } finally {
                    insertStmt.reset();
                }
            }
        }
        
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
       
        public SVNSkel workItems;

        public long wcId;
        public File localRelpath;
        
        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            assert (conflict == null);
            
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
                insertIncompleteChildren(db, wcId, localRelpath, revision, children, 0);
            }

            if (parentRelpath != null) {
                if (localRelpath != null && (status == SVNWCDbStatus.Normal || status == SVNWCDbStatus.Incomplete) && !fileExternal) {
                    extendParentDelete(db, wcId, localRelpath);
                } else if (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded) {
                    retractParentDelete(db, wcId, localRelpath);
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

        public long wcId;
        public File localRelpath;
        public File originalReposRelPath;
        public long originalReposId;
        public long originalRevision;
        
        public long opDepth;
        public long notPresentOpDepth;
        
        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            File parentRelpath = SVNFileUtil.getFileDir(localRelpath);
            
            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.INSERT_NODE);
            try {
                stmt.bindf("isisnnntstrisnnnnns",
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
                        (kind == SVNWCDbKind.Symlink) ? target : null);

                if (kind == SVNWCDbKind.File) {
                    stmt.bindChecksum(14, checksum);
                }
                if (originalReposRelPath != null) {
                    stmt.bindLong(5, originalReposId);
                    stmt.bindString(6, SVNFileUtil.getFilePath(originalReposRelPath));
                    stmt.bindLong(7, originalRevision);
                }
                stmt.bindProperties(15, props);
                stmt.done();
            } finally {
                stmt.reset();
            }
            
            if (kind == SVNWCDbKind.Dir && children != null) {
                insertIncompleteChildren(db, wcId, localRelpath, originalRevision, children, opDepth);
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
            
            addWorkItems(db, workItems);
            
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
        }

    }

    public void insertIncompleteChildren(SVNSqlJetDb db, long wcId, File localRelpath, long revision, List<File> children, long opDepth) throws SVNException {
        for (File name : children) {
            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.INSERT_NODE);
            try {
                stmt.bindf("isisnnrsns", wcId, SVNFileUtil.createFilePath(localRelpath, name), opDepth, localRelpath, revision, "incomplete", "unknown");
                stmt.done();
            } finally {
                stmt.reset();
            }
        }
    }
    
    public void retractParentDelete(SVNSqlJetDb db, long wcId, File localRelPath) throws SVNException {
        SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_LOWEST_WORKING_NODE);
        try {
            stmt.bindf("is", wcId, localRelPath);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public void extendParentDelete(SVNSqlJetDb db, long wcId, File localRelpath) throws SVNException {
        assert (localRelpath != null);
        boolean haveRow;
        SVNSqlJetStatement stmt;
        long parentOpDepth = 0;
        File parentRelpath = SVNFileUtil.getFileDir(localRelpath);
        assert (parentRelpath != null);
        stmt = db.getStatement(SVNWCDbStatements.SELECT_LOWEST_WORKING_NODE);
        try {
            stmt.bindf("is", wcId, parentRelpath);
            haveRow = stmt.next();
            if (haveRow) {
                parentOpDepth = stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
            }
        } finally {
            stmt.reset();
        }
        if (haveRow) {
            long opDepth = 0;
            try {
                stmt.bindf("is", wcId, localRelpath);
                haveRow = stmt.next();
                if (haveRow) {
                    opDepth = stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
                }
            } finally {
                stmt.reset();
            }
            if (!haveRow || parentOpDepth < opDepth) {
                stmt = db.getStatement(SVNWCDbStatements.INSTALL_WORKING_NODE_FOR_DELETE);
                try {
                    stmt.bindf("isit", wcId, localRelpath, parentOpDepth, getPresenceText(SVNWCDbStatus.BaseDeleted));
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }
        }
    }

    public void addBaseFile(File localAbspath, File reposRelpath, SVNURL reposRootUrl, String reposUuid, long revision, SVNProperties props, long changedRev, SVNDate changedDate,
            String changedAuthor, SvnChecksum checksum, SVNProperties davCache, SVNSkel conflict, boolean updateActualProps, SVNProperties actualProps,
            boolean keepRecordedInfo, boolean insertBaseDeleted, SVNSkel workItems) throws SVNException {

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
        
        if (updateActualProps) {
            ibb.updateActualProps = true;
            ibb.actualProps = actualProps;
        }
        ibb.keepRecordedInfo = keepRecordedInfo;
        ibb.insertBaseDeleted = insertBaseDeleted;

        ibb.localRelpath = localRelpath;
        ibb.wcId = pdh.getWCRoot().getWcId();
        
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

    public File fromRelPath(File wcRootAbsPath, File localRelPath) throws SVNException {
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

    public void globalRecordFileinfo(File localAbspath, long translatedSize, SVNDate lastModTime) throws SVNException {
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

        public SVNDate lastModTime;
        public long translatedSize;
        public File localRelpath;
        public SVNWCDbRoot wcRoot;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.UPDATE_NODE_FILEINFO);
            try {
                stmt.bindf("isii", wcRoot.getWcId(), localRelpath, translatedSize, lastModTime);
                long affectedRows = stmt.done();
                assert (affectedRows == 1);
            } finally {
                stmt.reset();
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
                        sDb = openDb(localAbspath, SDB_FILE, sMode, journalMode);
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
                info.wcDbDir.setWCRoot(new SVNWCDbRoot(this, localAbspath, sDb, wcId, FORMAT_FROM_SDB, autoUpgrade, enforceEmptyWQ));
    
            } else {
                info.wcDbDir = new SVNWCDbDir(localAbspath);
                info.wcDbDir.setWCRoot(new SVNWCDbRoot(this, localAbspath, null, UNKNOWN_WC_ID, wc_format, autoUpgrade, enforceEmptyWQ));
    
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
            if (entriesFile.exists()) {
                formatVersion = readFormatVersion(entriesFile);
            } else {
                File formatFile = new File(adminDir, "format");
                if (formatFile.exists()) {
                    formatVersion = readFormatVersion(formatFile);
                }
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

    public void opAddDirectory(File localAbsPath, SVNSkel workItems) throws SVNException {
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        InsertWorking ibw = new InsertWorking();
        ibw.status = SVNWCDbStatus.Normal;
        ibw.kind = SVNWCDbKind.Dir;
        ibw.opDepth = SVNWCUtils.relpathDepth(localRelpath);
        ibw.workItems = workItems;
        ibw.localRelpath = localRelpath;
        ibw.wcId = pdh.getWCRoot().getWcId();
        
        pdh.getWCRoot().getSDb().runTransaction(ibw);
        pdh.flushEntries(localAbsPath);
    }

    public void opAddFile(File localAbsPath, SVNSkel workItems) throws SVNException {
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadWrite, false, true);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelpath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        InsertWorking ibw = new InsertWorking();
        ibw.status = SVNWCDbStatus.Normal;
        ibw.kind = SVNWCDbKind.File;
        ibw.opDepth = SVNWCUtils.relpathDepth(localRelpath);
        ibw.workItems = workItems;
        ibw.localRelpath = localRelpath;
        ibw.wcId = pdh.getWCRoot().getWcId();

        pdh.getWCRoot().getSDb().runTransaction(ibw);
        pdh.flushEntries(localAbsPath);
    }

    public void opCopy(File srcAbsPath, File dstAbsPath, SVNSkel workItems) throws SVNException {
        DirParsedInfo parseSrcDir = parseDir(srcAbsPath, Mode.ReadWrite);
        SVNWCDbDir srcPdh = parseSrcDir.wcDbDir;
        File localSrcRelpath = parseSrcDir.localRelPath;
        verifyDirUsable(srcPdh);

        DirParsedInfo parseDstDir = parseDir(dstAbsPath, Mode.ReadWrite);
        SVNWCDbDir dstPdh = parseDstDir.wcDbDir;
        File localDstRelpath = parseDstDir.localRelPath;
        verifyDirUsable(dstPdh);
        
        SvnWcDbCopy.copy(srcPdh, localSrcRelpath, dstPdh, localDstRelpath, workItems);
    }

    public void opCopyShadowedLayer(File srcAbsPath, File dstAbsPath) throws SVNException {
        DirParsedInfo parseSrcDir = parseDir(srcAbsPath, Mode.ReadWrite);
        SVNWCDbDir srcPdh = parseSrcDir.wcDbDir;
        File localSrcRelpath = parseSrcDir.localRelPath;
        verifyDirUsable(srcPdh);

        DirParsedInfo parseDstDir = parseDir(dstAbsPath, Mode.ReadWrite);
        SVNWCDbDir dstPdh = parseDstDir.wcDbDir;
        File localDstRelpath = parseDstDir.localRelPath;
        verifyDirUsable(dstPdh);
        
        SvnWcDbCopy.copyShadowedLayer(srcPdh, localSrcRelpath, dstPdh, localDstRelpath);
    }

    public void opCopyDir(File localAbsPath, SVNProperties props, long changedRev, SVNDate changedDate, String changedAuthor, File originalReposRelPath, SVNURL originalRootUrl, String originalUuid,
            long originalRevision, List<File> children, SVNDepth depth, SVNSkel conflict, SVNSkel workItems) throws SVNException {
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        SvnWcDbCopy.copyDir(pdh, localRelpath, 
                props, changedRev, changedDate, changedAuthor, originalReposRelPath, originalRootUrl, 
                originalUuid, originalRevision, children, depth, conflict, workItems);
    }

    public void opCopyFile(File localAbsPath, SVNProperties props, long changedRev, SVNDate changedDate, String changedAuthor, File originalReposRelPath, SVNURL originalRootUrl, String originalUuid,
            long originalRevision, SvnChecksum checksum, SVNSkel conflict, SVNSkel workItems) throws SVNException {
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        SvnWcDbCopy.copyFile(pdh, localRelpath, 
                props, changedRev, changedDate, changedAuthor, originalReposRelPath, originalRootUrl, 
                originalUuid, originalRevision, checksum, conflict, workItems);
    }

    public void opDelete(File localAbsPath, ISVNEventHandler notifyHandler) throws SVNException {
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);
        
        Delete deleteTxn = new Delete();
        deleteTxn.root = pdh.getWCRoot();
        deleteTxn.localRelPath = localRelpath;
        deleteTxn.deleteDepth = SVNWCUtils.relpathDepth(localRelpath);
        deleteTxn.eventHandler = notifyHandler;
        
        pdh.flushEntries(localAbsPath);
        pdh.getWCRoot().getSDb().beginTransaction(SqlJetTransactionMode.WRITE);
        try {
            try {
                deleteTxn.transaction(pdh.getWCRoot().getSDb());
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
            }
            if (notifyHandler != null && pdh.getWCRoot().getSDb().getTemporaryDb().hasTable(SVNWCDbSchema.DELETE_LIST.toString())) {
                SVNSqlJetStatement selectDeleteList = new SVNSqlJetSelectStatement(pdh.getWCRoot().getSDb().getTemporaryDb(), SVNWCDbSchema.DELETE_LIST);
                try {
                    while(selectDeleteList.next()) {
                        File path = getColumnPath(selectDeleteList, DELETE_LIST__Fields.local_relpath);
                        path = pdh.getWCRoot().getAbsPath(path);
                        notifyHandler.handleEvent(SVNEventFactory.createSVNEvent(path, SVNNodeKind.NONE, null, -1, SVNEventAction.DELETE, 
                                SVNEventAction.DELETE, null, null, 1, 1), -1);
                    }
                } finally {
                    selectDeleteList.reset();
                }
                SVNSqlJetStatement dropList = new SVNWCDbCreateSchema(pdh.getWCRoot().getSDb().getTemporaryDb(), SVNWCDbCreateSchema.DROP_DELETE_LIST, -1);
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

    public void opMarkResolved(File localAbspath, boolean resolvedText, boolean resolvedProps, boolean resolvedTree) throws SVNException {
        assert (isAbsolute(localAbspath));
        assert (!resolvedTree);

        final DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        if (resolvedText) {
            SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.CLEAR_TEXT_CONFLICT);
            try {
                stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }
        if (resolvedProps) {
            SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.CLEAR_PROPS_CONFLICT);
            try {
                stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }

        pdh.flushEntries(localAbspath);

        return;
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
        Map<String, SVNTreeConflictDescription> treeConflicts = new HashMap<String, SVNTreeConflictDescription>();
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_TREE_CONFLICT);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            boolean haveRow = stmt.next();
            while (haveRow) {
                File childRelpath = SVNFileUtil.createFilePath(stmt.getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath));
                String childBaseName = SVNFileUtil.getFileName(childRelpath);
                byte[] conflictData = stmt.getColumnBlob(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data);
                SVNSkel skel = SVNSkel.parse(conflictData);
                SVNTreeConflictDescription treeConflict = SVNTreeConflictUtil.readSingleTreeConflict(skel, pdh.getWCRoot().getAbsPath());
                treeConflicts.put(childBaseName, treeConflict);
                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }
        return treeConflicts;
    }

    public SVNTreeConflictDescription opReadTreeConflict(File localAbsPath) throws SVNException {
        assert (isAbsolute(localAbsPath));
        final DirParsedInfo parsed = parseDir(localAbsPath, Mode.ReadWrite);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        verifyDirUsable(pdh);
        return readTreeConflict(pdh, localRelpath);
    }

    private SVNTreeConflictDescription readTreeConflict(SVNWCDbDir pdh, File localRelpath) throws SVNException {
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_TREE_CONFLICT);
        try {
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
            boolean haveRow = stmt.next();
            if (!haveRow) {
                return null;
            }
            byte[] conflictData = stmt.getColumnBlob(SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data);
            SVNSkel skel = SVNSkel.parse(conflictData);
            return SVNTreeConflictUtil.readSingleTreeConflict(skel, pdh.getWCRoot().getAbsPath());
        } finally {
            stmt.reset();
        }
    }

    public void opRevert(File localAbspath, SVNDepth depth) throws SVNException {        
        final DirParsedInfo parsed = parseDir(localAbspath, Mode.ReadWrite);
        SVNWCDbDir pdh = parsed.wcDbDir;
        verifyDirUsable(pdh);
        
        SVNSqlJetDb sdb = pdh.getWCRoot().getSDb();
        sdb.beginTransaction(SqlJetTransactionMode.WRITE);
            
        try {
            SVNSqlJetStatement stmt = new SVNWCDbCreateSchema(sdb.getTemporaryDb(), SVNWCDbCreateSchema.REVERT_LIST, -1);
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
            assert (conflict == null);
            addWorkItems(db, workItems);
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
                rfi.lastModTime = SVNDate.NULL;
                rfi.translatedSize = -1;
                rfi.localRelpath = localRelpath;
                rfi.wcRoot = pdh.getWCRoot();
                rfi.transaction(db);
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
        SetTreeConflict stb = new SetTreeConflict();
        stb.treeConflict = treeConflict;
        DirParsedInfo parseDir = parseDir(localAbspath, Mode.ReadWrite);
        stb.localRelpath = parseDir.localRelPath;
        SVNWCDbDir pdh = parseDir.wcDbDir;
        verifyDirUsable(pdh);
        stb.wcId = pdh.getWCRoot().getWcId();
        stb.parentRelpath = SVNFileUtil.getFileDir(stb.localRelpath);
        pdh.getWCRoot().getSDb().runTransaction(stb);
        pdh.flushEntries(localAbspath);
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
                    } else {
                        childItem.layersCount++;
                        childItem.haveMoreWork = childItem.layersCount > 1;
                    }
                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }
            stmt = db.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_INFO);
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
                    childItem.conflicted = !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) ||
                                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new) ||
                                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working) ||
                                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject) ||
                                            !isColumnNull(stmt, SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data);
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
    public Map<String, WCDbBaseInfo> getBaseChildrenMap(SVNWCDbRoot wcRoot, File localRelPath, boolean fetchLocks) throws SVNException {
        final long wcId = wcRoot.getWcId();
        final SVNSqlJetDb sDb = wcRoot.getSDb();

        final Map<String, WCDbBaseInfo> children = new TreeMap<String, WCDbBaseInfo>();
        final SVNSqlJetSelectStatement baseStmt = (SVNSqlJetSelectStatement) sDb.getStatement(SVNWCDbStatements.SELECT_BASE_NODE_CHILDREN);

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
        stmt = sDb.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CONFLICT_VICTIMS);
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
        List<SVNConflictDescription> conflicts = new ArrayList<SVNConflictDescription>();

        /* The parent should be a working copy directory. */
        DirParsedInfo parseDir = parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parseDir.wcDbDir;
        File localRelPath = parseDir.localRelPath;
        verifyDirUsable(pdh);

        /*
         * ### This will be much easier once we have all conflicts in one field
         * of actual.
         */

        /* First look for text and property conflicts in ACTUAL */
        SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_CONFLICT_DETAILS);
        try {

            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelPath);

            boolean have_row = stmt.next();

            if (have_row) {
                /* ### Store in description! */
                String prop_reject = getColumnText(stmt, ACTUAL_NODE__Fields.prop_reject);
                if (prop_reject != null) {
                    final File reposFile = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), prop_reject);
                    final SVNMergeFileSet mergeFiles = new SVNMergeFileSet(null, null, null, localAbsPath, null, reposFile, null, null, null);
                    final SVNPropertyConflictDescription desc = new SVNPropertyConflictDescription(mergeFiles, SVNNodeKind.UNKNOWN, "", null, null);
                    conflicts.add(desc);
                }

                String conflict_old = getColumnText(stmt, ACTUAL_NODE__Fields.conflict_old);
                if ("".equals(conflict_old)) {
                    conflict_old = null;
                }
                String conflict_new = getColumnText(stmt, ACTUAL_NODE__Fields.conflict_new);
                if ("".equals(conflict_new)) {
                    conflict_new = null;
                }
                String conflict_working = getColumnText(stmt, ACTUAL_NODE__Fields.conflict_working);
                if ("".equals(conflict_working)) {
                    conflict_working = null;
                }

                if (conflict_old != null || conflict_new != null || conflict_working != null) {
                    File baseFile = conflict_old != null ? SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), conflict_old) : null;
                    File theirFile = conflict_new != null ? SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), conflict_new) : null;
                    File myFile = conflict_working != null ? SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), conflict_working) : null;
                    File mergedFile = new File(SVNFileUtil.getFileName(localAbsPath));
                    SVNMergeFileSet mergeFiles = new SVNMergeFileSet(null, null, baseFile, myFile, null, theirFile, mergedFile, null, null);
                    SVNTextConflictDescription desc = new SVNTextConflictDescription(mergeFiles, SVNNodeKind.UNKNOWN, null, null);
                    conflicts.add(desc);
                }

                byte[] conflict_data = getColumnBlob(stmt, ACTUAL_NODE__Fields.tree_conflict_data);
                if (conflict_data != null) {
                    SVNSkel skel = SVNSkel.parse(conflict_data);
                    SVNTreeConflictDescription desc = SVNTreeConflictUtil.readSingleTreeConflict(skel, SVNFileUtil.getParentFile(localAbsPath));
                    conflicts.add(desc);
                }

            }
        } finally {
            stmt.reset();
        }

        return conflicts;
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
                    info.changedDate = SVNWCUtils.readDate(getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.changed_date));
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
                    info.conflicted = !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) || /* old */
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
                if (isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data)) {
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
        pdh.getWCRoot().getSDb().runTransaction(brb);
        pdh.flushEntries(localAbsPath);
    }

    private class BaseRemove implements SVNSqlJetTransaction {

        public long wcId;
        public File localRelpath;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            boolean haveRow = false;
            SVNSqlJetStatement stmt = db.getStatement(SVNWCDbStatements.DELETE_BASE_NODE);
            try {
                stmt.bindf("is", wcId, localRelpath);
                stmt.done();
            } finally {
                stmt.reset();
            }
            retractParentDelete(db);
            stmt = db.getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);
            try {
                stmt.bindf("is", wcId, localRelpath);
                haveRow = stmt.next();
            } finally {
                stmt.reset();
            }
            if (!haveRow) {
                stmt = db.getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_WITHOUT_CONFLICT);
                try{
                    stmt.bindf("is", wcId, localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
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

            if (f.contains(AdditionInfoField.originalReposRelPath) || f.contains(AdditionInfoField.originalRootUrl) || f.contains(AdditionInfoField.originalUuid)
                    || (f.contains(AdditionInfoField.originalRevision) && additionInfo.originalRevision == INVALID_REVNUM) || f.contains(AdditionInfoField.status)) {
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

                if (!isColumnNull(stmt, NODES__Fields.repos_id) && (f.contains(AdditionInfoField.status) || f.contains(AdditionInfoField.originalRootUrl) || f.contains(AdditionInfoField.originalUuid))) {
                    if (f.contains(AdditionInfoField.originalRootUrl) || f.contains(AdditionInfoField.originalUuid)) {
                        originalReposId = getColumnInt64(stmt, NODES__Fields.repos_id);
                        ReposInfo reposInfo = fetchReposInfo(pdh.getWCRoot().getSDb(), originalReposId);
                        additionInfo.originalRootUrl = SVNURL.parseURIEncoded(reposInfo.reposRootUrl);
                        additionInfo.originalUuid = reposInfo.reposUuid;
                    }
                    if (f.contains(AdditionInfoField.status)) {
                        if (getColumnBoolean(stmt, NODES__Fields.moved_here)) {
                            additionInfo.status = SVNWCDbStatus.MovedHere;
                        } else {
                            additionInfo.status = SVNWCDbStatus.Copied;
                        }
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
        File current_relpath = parsed.localRelPath;
        verifyDirUsable(pdh);

        final EnumSet<DeletionInfoField> f = getInfoFields(DeletionInfoField.class, fields);

        /* Initialize all the OUT parameters. */
        final WCDbDeletionInfo deletionInfo = new WCDbDeletionInfo();

        File current_abspath = localAbsPath;
        File child_abspath = null;
        boolean child_has_base = false;
        boolean found_moved_to = false;

        long opDepth = 0, localOpDepth = 0;

        /*
         * Initialize to something that won't denote an important parent/child
         * transition.
         */

        SVNWCDbStatus child_presence = SVNWCDbStatus.BaseDeleted;

        while (true) {
            SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_DELETION_INFO);

            try {
                stmt.bindf("is", pdh.getWCRoot().getWcId(), SVNFileUtil.getFilePath(current_relpath));

                boolean have_row = stmt.next();

                if (!have_row) {
                    /* There better be a row for the starting node! */
                    if (current_abspath == localAbsPath) {
                        stmt.reset();
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", localAbsPath);
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }

                    /* There are no values, so go ahead and reset the stmt now. */
                    stmt.reset();

                    /*
                     * No row means no WORKING node at this path, which means we
                     * just fell off the top of the WORKING tree.
                     *
                     * If the child was not-present this implies the root of the
                     * (added) WORKING subtree was deleted. This can occur
                     * during post-commit processing when the copied parent that
                     * was in the WORKING tree has been moved to the BASE tree.
                     */
                    if (f.contains(DeletionInfoField.workDelAbsPath) && child_presence == SVNWCDbStatus.NotPresent && deletionInfo.workDelAbsPath == null) {
                        deletionInfo.workDelAbsPath = child_abspath;
                    }

                    /*
                     * If the child did not have a BASE node associated with it,
                     * then we're looking at a deletion that occurred within an
                     * added tree. There is no root of a deleted/replaced BASE
                     * tree.
                     *
                     * If the child was base-deleted, then the whole tree is a
                     * simple (explicit) deletion of the BASE tree.
                     *
                     * If the child was normal, then it is the root of a
                     * replacement, which means an (implicit) deletion of the
                     * BASE tree.
                     *
                     * In both cases, set the root of the operation (if we have
                     * not already set it as part of a moved-away).
                     */
                    if (f.contains(DeletionInfoField.baseDelAbsPath) && child_has_base && deletionInfo.baseDelAbsPath == null) {
                        deletionInfo.baseDelAbsPath = child_abspath;
                    }

                    /*
                     * We found whatever roots we needed. This BASE node and its
                     * ancestors are unchanged, so we're done.
                     */
                    break;
                }

                /*
                 * We need the presence of the WORKING node. Note that legal
                 * values are: normal, not-present, base-deleted.
                 */
                SVNWCDbStatus work_presence = getColumnPresence(stmt);

                /* The starting node should be deleted. */
                if (current_abspath.equals(localAbsPath) && work_presence != SVNWCDbStatus.NotPresent && work_presence != SVNWCDbStatus.BaseDeleted) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Expected node ''{0}'' to be deleted.", localAbsPath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                assert (work_presence == SVNWCDbStatus.Normal ||
                        work_presence == SVNWCDbStatus.Incomplete ||
                        work_presence == SVNWCDbStatus.NotPresent || 
                        work_presence == SVNWCDbStatus.BaseDeleted);

                SVNSqlJetStatement baseStmt = stmt.getJoinedStatement(SVNWCDbSelectDeletionInfo.NODES_BASE);
                try {
                    boolean have_base = baseStmt != null && baseStmt.next() && !isColumnNull(baseStmt, SVNWCDbSchema.NODES__Fields.presence);

                    if (have_base) {
                        SVNWCDbStatus base_presence = getColumnPresence(baseStmt);

                        /* Only "normal" and "not-present" are allowed. */
                        assert (base_presence == SVNWCDbStatus.Normal || base_presence == SVNWCDbStatus.NotPresent

                        /*
                         * ### there are cases where the BASE node is ### marked
                         * as incomplete. we should treat this ### as a "normal"
                         * node for the purposes of ### this function. we really
                         * should not allow ### it, but this situation occurs
                         * within the ### following tests: ### switch_tests 31
                         * ### update_tests 46 ### update_tests 53
                         */
                        || base_presence == SVNWCDbStatus.Incomplete);

                        /* ### see above comment */
                        if (base_presence == SVNWCDbStatus.Incomplete)
                            base_presence = SVNWCDbStatus.Normal;

                        /*
                         * If a BASE node is marked as not-present, then we'll
                         * ignore it within this function. That status is simply
                         * a bookkeeping gimmick, not a real node that may have
                         * been deleted.
                         */

                        /*
                         * If we're looking at a present BASE node, *and* there
                         * is a WORKING node (present or deleted), then a
                         * replacement has occurred here or in an ancestor.
                         */
                        if (f.contains(DeletionInfoField.baseReplaced) && base_presence == SVNWCDbStatus.Normal && work_presence != SVNWCDbStatus.BaseDeleted) {
                            deletionInfo.baseReplaced = true;
                        }
                    }

                    /* Only grab the nearest ancestor. */
                    if (!found_moved_to && (f.contains(DeletionInfoField.movedToAbsPath) || f.contains(DeletionInfoField.baseDelAbsPath)) && !isColumnNull(stmt, SVNWCDbSchema.NODES__Fields.moved_to)) {
                        /* There better be a BASE_NODE (that was moved-away). */
                        assert (have_base);

                        found_moved_to = true;

                        /* This makes things easy. It's the BASE_DEL_ABSPATH! */
                        if (f.contains(DeletionInfoField.baseDelAbsPath))
                            deletionInfo.baseDelAbsPath = current_abspath;

                        if (f.contains(DeletionInfoField.movedToAbsPath))
                            deletionInfo.movedToAbsPath = SVNFileUtil.createFilePath(pdh.getWCRoot().getAbsPath(), getColumnText(stmt, SVNWCDbSchema.NODES__Fields.moved_to));
                    }

                    opDepth = stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
                    if (current_abspath.equals(localAbsPath)) {
                        localOpDepth = opDepth;
                    }

                    if (f.contains(DeletionInfoField.workDelAbsPath) && deletionInfo.workDelAbsPath == null && ((opDepth < localOpDepth && opDepth > 0) || child_presence == SVNWCDbStatus.NotPresent)) {
                        deletionInfo.workDelAbsPath = child_abspath;
                    }

                    /*
                     * Move to the parent node. Remember the information about
                     * this node for our parent to use.
                     */
                    child_abspath = current_abspath;
                    child_presence = work_presence;
                    child_has_base = have_base;
                    if (current_abspath.equals(pdh.getLocalAbsPath())) {
                        /*
                         * The current node is a directory, so move to the
                         * parent dir.
                         */
                        pdh = navigateToParent(pdh, Mode.ReadOnly);
                    }
                    current_abspath = pdh.getLocalAbsPath();
                    current_relpath = pdh.computeRelPath();
                } finally {
                    baseStmt.reset();
                }
            } finally {
                stmt.reset();
            }

        }

        return deletionInfo;

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

    private static SVNSqlJetDb openDb(File dirAbsPath, String sdbFileName, Mode sMode, SqlJetPagerJournalMode journalMode) throws SVNException {
        if (dirAbsPath == null || sdbFileName == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return SVNSqlJetDb.open(SVNWCUtils.admChild(dirAbsPath, sdbFileName), sMode, journalMode);
    }

    private static void verifyDirUsable(SVNWCDbDir pdh) throws SVNException {
        if (!SVNWCDbDir.isUsable(pdh)) {
            if (pdh != null && pdh.getWCRoot() != null && pdh.getWCRoot().getFormat() != ISVNWCDb.WC_FORMAT_17) {
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

    private class MakeCopy implements SVNSqlJetTransaction {

        File localAbspath;
        SVNWCDbDir pdh;
        File localRelpath;
        long opDepth;

        public void transaction(SVNSqlJetDb db) throws SqlJetException, SVNException {
            SVNSqlJetStatement stmt;
            boolean haveRow;
            boolean removeWorking = false;
            boolean addWorkingBaseDeleted = false;
            stmt = db.getStatement(SVNWCDbStatements.SELECT_LOWEST_WORKING_NODE);
            stmt.bindf("is", pdh.getWCRoot().getWcId(), localRelpath);
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
            
            final List<String> children = gatherRepoChildren(pdh, localRelpath, 0);
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
        }

    };

    public List<String> gatherRepoChildren(SVNWCDbDir pdh, File localRelpath, long opDepth) throws SVNException {
        final List<String> children = new ArrayList<String>();
        final SVNSqlJetStatement stmt = pdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_OP_DEPTH_CHILDREN);
        try {
            stmt.bindf("isi", pdh.getWCRoot().getWcId(), localRelpath, opDepth);
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

    public void opSetNewDirToIncompleteTemp(File localAbspath, File reposRelpath, SVNURL reposRootURL, String reposUuid, long revision, SVNDepth depth) throws SVNException {

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
        
        pdh.getWCRoot().getSDb().runTransaction(insertBase);
        pdh.flushEntries(localAbspath);
    }
    
    public void opBumpRevisionPostUpdate(File localAbsPath, SVNDepth depth, File newReposRelPath, SVNURL newReposRootURL, String newReposUUID,
            long newRevision, Collection<File> excludedPaths) throws SVNException {
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
        
        pdh.getWCRoot().getSDb().runTransaction(brb);
        pdh.flushEntries(localAbsPath);
    }
    
    private class BumpRevisionPostUpdate implements SVNSqlJetTransaction {
        
        private SVNDepth depth;
        private File newReposRelPath;
        private SVNURL newReposRootURL;
        private String newReposUUID;
        private long newRevision;
        private Collection<File> exludedRelPaths;
        
        private File localRelPath;
        private File wcRoot;
        private SVNWCDbRoot dbWcRoot;

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
            bumpNodeRevision(dbWcRoot, wcRoot, localRelPath, baseInfo, reposId, newReposRelPath, newRevision, depth, exludedRelPaths, true, false);
        }

        private void bumpNodeRevision(SVNWCDbRoot root, File wcRoot, File localRelPath, WCDbBaseInfo baseInfo, long reposId, File newReposRelPath, long newRevision,
                SVNDepth depth, Collection<File> exludedRelPaths, boolean isRoot, boolean skipWhenDir) throws SVNException {
            if (exludedRelPaths != null && exludedRelPaths.contains(localRelPath)) {
                return;
            }
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
            if (setReposRelPath || (newRevision >= 0 && newRevision != baseInfo.revision)) {
                opSetRevAndReposRelpath(root, localRelPath, newRevision, setReposRelPath, newReposRelPath, newReposRootURL, newReposUUID);
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
            
            Map<String, WCDbBaseInfo> children = getBaseChildrenMap(root, localRelPath, false);
            for (String child : children.keySet()) {
                File childReposRelPath = null;
                File childLocalRelPath = SVNFileUtil.createFilePath(localRelPath, child);                
                if (newReposRelPath != null) {
                    childReposRelPath = SVNFileUtil.createFilePath(newReposRelPath, child);
                }
                bumpNodeRevision(root, wcRoot, childLocalRelPath, children.get(child), reposId, childReposRelPath, newRevision, depthBelowHere, exludedRelPaths, false, depth.compareTo(SVNDepth.IMMEDIATES) < 0);               
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

    private void opSetRevAndReposRelpath(SVNWCDbRoot wcRoot, File localRelpath, long revision, boolean setReposRelpath, final File reposRelpath, SVNURL reposRootUrl, String reposUuid) throws SVNException {
        assert (SVNRevision.isValidRevisionNumber(revision) || setReposRelpath);
        SVNSqlJetStatement stmt;
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
            List<String> children = gatherRepoChildren(pdh, parentLocalRelPath, opDepth);
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
          
                try {
                    ib.transaction(pdh.getWCRoot().getSDb());
                } catch (SqlJetException e) {
                    SVNSqlJetDb.createSqlJetError(e);
                }
            }
        } catch (SVNException e) {
            pdh.getWCRoot().getSDb().rollback();
            throw e;
        } finally {
            pdh.getWCRoot().getSDb().commit();
        }
        pdh.flushEntries(localAbspath);
    }
    
    public void upgradeBegin(File localAbspath, SVNWCDbUpgradeData upgradeData, SVNURL repositoryRootUrl, String repositoryUUID) throws SVNException {
    	CreateDbInfo dbInfo =  createDb(localAbspath, repositoryRootUrl, repositoryUUID, SDB_FILE);
    	upgradeData.repositoryId = dbInfo.reposId;
    	upgradeData.workingCopyId = dbInfo.wcId;
    	    	
    	SVNWCDbDir pdh = new SVNWCDbDir(localAbspath);
    	SVNWCDbRoot root = new SVNWCDbRoot(this, localAbspath, dbInfo.sDb, dbInfo.wcId, FORMAT_FROM_SDB, false, false);     	
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
}
