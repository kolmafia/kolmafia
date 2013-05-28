/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.SVNConfigFile;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNWCProperties;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSFS {
    public static final String DB_DIR = "db";
    public static final String REVS_DIR = "revs";
    public static final String REPOS_FORMAT_FILE = "format";
    public static final String DB_FORMAT_FILE = "format";
    public static final String DB_LOGS_LOCK_FILE = "db-logs.lock";
    public static final String DB_LOCK_FILE = "db.lock";
    public static final String CURRENT_FILE = "current";
    public static final String UUID_FILE = "uuid";
    public static final String FS_TYPE_FILE = "fs-type";
    public static final String TXN_CURRENT_FILE = "txn-current";
    public static final String MIN_UNPACKED_REV_FILE = "min-unpacked-rev";
    public static final String TXN_CURRENT_LOCK_FILE = "txn-current-lock";
    public static final String REVISION_PROPERTIES_DIR = "revprops";
    public static final String WRITE_LOCK_FILE = "write-lock";
    public static final String LOCKS_DIR = "locks";
    public static final String DAV_DIR = "dav";
    public static final String TRANSACTIONS_DIR = "transactions";
    public static final String TRANSACTION_PROTOS_DIR = "txn-protorevs";
    public static final String NODE_ORIGINS_DIR = "node-origins";

    public static final String REP_CACHE_DB = "rep-cache.db";
    public static final String PACK_EXT = ".pack";
    public static final String PACK_KIND_PACK = "pack";
    public static final String PACK_KIND_MANIFEST = "manifest";
    public static final String ENABLE_REP_SHARING_OPTION = "enable-rep-sharing";
    public static final String REP_SHARING_SECTION = "rep-sharing";
    public static final String PATH_CONFIG = "fsfs.conf";
    public static final String TXN_PATH_EXT = ".txn";
    public static final String TXN_MERGEINFO_PATH = "mergeinfo";
    public static final String TXN_PATH_EXT_CHILDREN = ".children";
    public static final String PATH_PREFIX_NODE = "node.";
    public static final String TXN_PATH_EXT_PROPS = ".props";
    public static final String SVN_OPAQUE_LOCK_TOKEN = "opaquelocktoken:";
    public static final String TXN_PATH_REV = "rev";
    public static final String PATH_LOCK_KEY = "path";
    public static final String CHILDREN_LOCK_KEY = "children";
    public static final String TOKEN_LOCK_KEY = "token";
    public static final String OWNER_LOCK_KEY = "owner";
    public static final String IS_DAV_COMMENT_LOCK_KEY = "is_dav_comment";
    public static final String CREATION_DATE_LOCK_KEY = "creation_date";
    public static final String EXPIRATION_DATE_LOCK_KEY = "expiration_date";
    public static final String COMMENT_LOCK_KEY = "comment";
    public static final String PRE_12_COMPAT_UNNEEDED_FILE_CONTENTS =
        "This file is not used by Subversion 1.3.x or later." +
        "However, its existence is required for compatibility with" +
        "Subversion 1.2.x or earlier.";

    public static final int DIGEST_SUBDIR_LEN = 3;
    public static final int REPOSITORY_FORMAT = 5;
    public static final int REPOSITORY_FORMAT_LEGACY = 3;
    public static final int DB_FORMAT_PRE_17 = 4;
    public static final int DB_FORMAT = 5;
    public static final int DB_FORMAT_LOW = 1;
    public static final int LAYOUT_FORMAT_OPTION_MINIMAL_FORMAT = 3;
    public static final int MIN_CURRENT_TXN_FORMAT = 3;
    public static final int MIN_PROTOREVS_DIR_FORMAT = 3;
    public static final int MIN_NO_GLOBAL_IDS_FORMAT = 3;
    public static final int MIN_MERGE_INFO_FORMAT = 3;
    public static final int MIN_REP_SHARING_FORMAT = 4;
    public static final int MIN_PACKED_FORMAT = 4;
    public static final int MIN_KIND_IN_CHANGED_FORMAT = 4;
    public static final int MIN_PACKED_REVPROP_FORMAT = 5;

    //TODO: we should be able to change this via some option
    private static long DEFAULT_MAX_FILES_PER_DIRECTORY = 1000;
    private static final String DB_TYPE = "fsfs";

    public static final String REVISION_PROPERTIES_DB = "revprops.db";
    public static final String REVISION_PROPERTIES_TABLE = "revprop";
    public static final String MIN_UNPACKED_REVPROP = "min-unpacked-revprop";

    public static final boolean DB_FORMAT_PRE_17_USE_AS_DEFAULT = true;
    //public static final boolean DB_FORMAT_PRE_17_USE_AS_DEFAULT = false;

    private int myDBFormat;
    private int myReposFormat;
    private String myUUID;
    private String myFSType;
    private File myRepositoryRoot;
    private File myRevisionsRoot;
    private File myRevisionPropertiesRoot;
    private File myTransactionsRoot;
    private File myLocksRoot;
    private File myDBRoot;
    private File myWriteLockFile;
    private File myCurrentFile;
    private File myTransactionCurrentFile;
    private File myTransactionCurrentLockFile;
    private File myTransactionProtoRevsRoot;
    private File myNodeOriginsDir;
    private File myRepositoryFormatFile;
    private File myDBFormatFile;
    private File myUUIDFile;
    private File myFSTypeFile;
    private File myMinUnpackedRevFile;
    private File myRepositoryCacheFile;
    private long myMaxFilesPerDirectory;
    private long myYoungestRevisionCache;
    private long myMinUnpackedRevision;
    private SVNConfigFile myConfig;
    private IFSRepresentationCacheManager myReposCacheManager;
    private SVNSqlJetDb myRevisionProperitesDb;
    private long myMinUnpackedRevProp;
    
    private boolean myIsHooksEnabled;

    public FSFS(File repositoryRoot) {
        myRepositoryRoot = repositoryRoot;
        myMaxFilesPerDirectory = 0;
        setHooksEnabled(true);
    }
    
    public void setHooksEnabled(boolean enabled) {
        myIsHooksEnabled = enabled;
    }
    
    public boolean isHooksEnabled() {
        return myIsHooksEnabled;
    }

    public int getDBFormat() {
        return myDBFormat;
    }

    public long getMaxFilesPerDirectory() {
        return myMaxFilesPerDirectory;
    }

    public int getReposFormat() {
        return myReposFormat;
    }

    public void open() throws SVNException {
        openRoot();
        openDB();
    }

    public void close() throws SVNException {
        if (myReposCacheManager != null) {
            myReposCacheManager.close();
            myReposCacheManager = null;
        }
        if(myRevisionProperitesDb!=null) {
            myRevisionProperitesDb.close();
            myRevisionProperitesDb = null;
        }
    }

    public void openForRecovery() throws SVNException {
        openRoot();
        //create new current file
        FSWriteLock writeLock = FSWriteLock.getWriteLockForDB(this);
        synchronized (writeLock) {
            try {
                writeLock.lock();
                try {
                    SVNFileUtil.createFile(getCurrentFile(), "0 1 1\n", "US-ASCII");
                } catch (SVNException svne) {
                    //ignore errors
                }
            } finally {
                writeLock.unlock();
                FSWriteLock.release(writeLock);
            }
        }
        openDB();
    }

    public void openRoot() throws SVNException {
        // repo format /root/format
        FSFile formatFile = new FSFile(getRepositoryFormatFile());
        int format = -1;
        try {
            format = formatFile.readInt();
        } catch (NumberFormatException nfe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_VERSION_FILE_FORMAT,
                    "First line of ''{0}'' contains non-digit", formatFile.getFile());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } finally {
            formatFile.close();
        }

        if (format != REPOSITORY_FORMAT && format != REPOSITORY_FORMAT_LEGACY) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_UNSUPPORTED_VERSION,
                                  "Expected repository format ''{0}'' or " +
                                  "''{1}''; found format ''{2}''",
                                  new Object[] {new Integer(REPOSITORY_FORMAT_LEGACY),
                                                new Integer(REPOSITORY_FORMAT),
                                                new Integer(format)});
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        myReposFormat = format;

    }

    public void openDB() throws SVNException {
        int format = readDBFormat();
        FSRepositoryUtil.checkReposDBFormat(format);

        myDBFormat = format;

        // fs type /root/db/fs-type
        getFSType();

        if (myDBFormat >= MIN_PACKED_FORMAT) {
            getMinUnpackedRev();
        }

        boolean isRepSharingAllowed = true;
        SVNConfigFile config = loadConfig();
        if (config != null) {
            String optionValue = config.getPropertyValue(REP_SHARING_SECTION, ENABLE_REP_SHARING_OPTION);
            isRepSharingAllowed = DefaultSVNOptions.getBooleanValue(optionValue, true);
        }

        if (myDBFormat >= MIN_REP_SHARING_FORMAT && isRepSharingAllowed) {
            myReposCacheManager = FSRepresentationCacheUtil.open(this);
        }

        File dbCurrentFile = getCurrentFile();
        if (!(dbCurrentFile.exists() && dbCurrentFile.canRead())) {
            if (myReposCacheManager != null) {
                myReposCacheManager.close();
                myReposCacheManager = null;
            }

            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR,
                    "Can''t open file ''{0}''", dbCurrentFile);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        /* Open the revprops db. */
        if (myDBFormat >= MIN_PACKED_REVPROP_FORMAT)
          {
            updateMinUnpackedRevProp();
            myRevisionProperitesDb = SVNSqlJetDb.open(
                    getRevisionPropertiesDbPath(), SVNSqlJetDb.Mode.ReadWrite );
          }
    }

    public String getFSType() throws SVNException {
        if (myFSType == null) {
            // fs type /root/db/fs-type
            FSFile fsTypeFile = new FSFile(getFSTypeFile());
            try {
                myFSType = fsTypeFile.readLine(128);
            } finally {
                fsTypeFile.close();
            }
            if (!DB_TYPE.equals(myFSType)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_UNKNOWN_FS_TYPE,
                        "Unsupported fs type ''{0}''", myFSType);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }
        return myFSType;
    }

    public int readDBFormat() throws SVNException {
        int format = -1;
        // fs format /root/db/format
        FSFile formatFile = new FSFile(getDBFormatFile());
        try {
            format = formatFile.readInt();
            readOptions(formatFile, format);
        } catch (SVNException svne) {
            if (svne.getCause() instanceof FileNotFoundException) {
                format = DB_FORMAT_LOW;
            } else if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.STREAM_UNEXPECTED_EOF) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_VERSION_FILE_FORMAT,
                        "Can''t read first line of format file ''{0}''", formatFile.getFile());
                SVNErrorManager.error(err, SVNLogType.FSFS);
            } else {
                throw svne;
            }
        } catch (NumberFormatException nfe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_VERSION_FILE_FORMAT,
                    "Format file ''{0}'' contains an unexpected non-digit", formatFile.getFile());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } finally {
            formatFile.close();
        }
        return format;
    }

    public String getUUID() throws SVNException {
        if(myUUID == null) {
            // uuid
            FSFile formatFile = new FSFile(getUUIDFile());
            try {
                myUUID = formatFile.readLine(38);
            } finally {
                formatFile.close();
            }
        }
        return myUUID;
    }

    public File getDBRoot() {
        if (myDBRoot == null) {
            myDBRoot = new File(myRepositoryRoot, DB_DIR);
        }
        return myDBRoot;
    }

    public File getWriteLockFile() {
        if (myWriteLockFile == null) {
            myWriteLockFile = new File(getDBRoot(), WRITE_LOCK_FILE);
        }
        return myWriteLockFile;
    }

    public File getUUIDFile() {
        if (myUUIDFile == null) {
            myUUIDFile = new File(getDBRoot(), UUID_FILE);
        }
        return myUUIDFile;
    }

    public File getDBRevsDir() {
        if (myRevisionsRoot == null) {
            myRevisionsRoot = new File(getDBRoot(), REVS_DIR);
        }
        return myRevisionsRoot;
    }

    public File getDBLocksDir() {
        if (myLocksRoot == null) {
            myLocksRoot = new File(getDBRoot(), LOCKS_DIR);
        }
        return myLocksRoot;
    }

    public File getFSTypeFile() {
        if (myFSTypeFile == null) {
            myFSTypeFile = new File(getDBRoot(), FS_TYPE_FILE);
        }
        return myFSTypeFile;
    }

    public File getTransactionsParentDir(){
        if (myTransactionsRoot == null) {
            myTransactionsRoot = new File(getDBRoot(), TRANSACTIONS_DIR);
        }
        return myTransactionsRoot;
    }

    public File getRepositoryRoot(){
        return myRepositoryRoot;
    }

    public File getRevisionPropertiesRoot() {
        if (myRevisionPropertiesRoot == null) {
            myRevisionPropertiesRoot = new File(getDBRoot(), REVISION_PROPERTIES_DIR);
        }
        return myRevisionPropertiesRoot;
    }

    public File getRepositoryFormatFile(){
        if (myRepositoryFormatFile == null) {
            myRepositoryFormatFile = new File(myRepositoryRoot, REPOS_FORMAT_FILE);
        }
        return myRepositoryFormatFile;
    }

    public File getDBFormatFile() {
        if (myDBFormatFile == null) {
            myDBFormatFile = new File(getDBRoot(), DB_FORMAT_FILE);
        }
        return myDBFormatFile;
    }

    public File getNodeOriginsDir() {
        if (myNodeOriginsDir == null) {
            myNodeOriginsDir = new File(getDBRoot(), NODE_ORIGINS_DIR);
        }
        return myNodeOriginsDir;
    }

    public File getCurrentFile() {
        if(myCurrentFile == null){
            myCurrentFile = new File(getDBRoot(), CURRENT_FILE);
        }
        return myCurrentFile;
    }

    public File getRepositoryCacheFile() {
        if (myRepositoryCacheFile == null) {
            myRepositoryCacheFile = new File(getDBRoot(), REP_CACHE_DB);
        }
        return myRepositoryCacheFile;
    }

    public File getDBLogsLockFile() throws SVNException {
        File lockFile = new File(getDBRoot(), LOCKS_DIR + "/" + DB_LOGS_LOCK_FILE);
        if (!lockFile.exists()) {
            try {
                SVNFileUtil.createFile(lockFile, PRE_12_COMPAT_UNNEEDED_FILE_CONTENTS, "US-ASCII");
            } catch (SVNException svne) {
                SVNErrorMessage err = svne.getErrorMessage().wrap("Creating db logs lock file");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }
        return lockFile;
    }

    public long getDatedRevision(Date date) throws SVNException {
        long latest = getYoungestRevision();
        long top = latest;
        long bottom = 0;
        long middle;
        Date currentTime = null;

        while (bottom <= top) {
            middle = (top + bottom) / 2;
            currentTime = getRevisionTime(middle);
            if (currentTime.compareTo(date) > 0) {
                if ((middle - 1) < 0) {
                    return 0;
                }
                Date prevTime = getRevisionTime(middle - 1);
                if (prevTime.compareTo(date) < 0) {
                    return middle - 1;
                }
                top = middle - 1;
            } else if (currentTime.compareTo(date) < 0) {
                if ((middle + 1) > latest) {
                    return latest;
                }
                Date nextTime = getRevisionTime(middle + 1);
                if (nextTime.compareTo(date) > 0) {
                    return middle;
                }
                bottom = middle + 1;
            } else {
                return middle;
            }
        }
        return 0;

    }

    public long getYoungestRevision() throws SVNException {
        FSFile file = new FSFile(getCurrentFile());
        try {
            String line = file.readLine(180);
            int spaceIndex = line.indexOf(' ');
            if (spaceIndex > 0) {
                myYoungestRevisionCache = Long.parseLong(line.substring(0, spaceIndex));
            } else {
                myYoungestRevisionCache = Long.parseLong(line);
            }
            return myYoungestRevisionCache;
        } catch (NumberFormatException nfe) {
            myYoungestRevisionCache = 0;
        } finally {
            file.close();
        }
        return myYoungestRevisionCache;
    }

    public long getMinUnpackedRev() throws SVNException {
        FSFile file = new FSFile(getMinUnpackedRevFile());
        try {
            myMinUnpackedRevision = file.readLong();
            return myMinUnpackedRevision;
        } catch (NumberFormatException nfe) {
            myMinUnpackedRevision = 0;
        } finally {
            file.close();
        }
        return myMinUnpackedRevision;

    }
    public void upgrade() throws SVNException {
        FSWriteLock writeLock = FSWriteLock.getWriteLockForDB(this);
        synchronized (writeLock) {
            try {
                writeLock.lock();
                if (myDBFormat == DB_FORMAT) {
                    return;
                }
                if (myDBFormat < MIN_CURRENT_TXN_FORMAT) {
                    File txnCurrentFile = getTransactionCurrentFile();
                    SVNFileUtil.createFile(txnCurrentFile, "0\n", "US-ASCII");
                    SVNFileUtil.createEmptyFile(getTransactionCurrentLockFile());
                }
                if (myDBFormat < MIN_PROTOREVS_DIR_FORMAT) {
                    File txnProtoRevsDir = getTransactionProtoRevsDir();
                    txnProtoRevsDir.mkdirs();
                }

                if (myDBFormat < MIN_PACKED_FORMAT) {
                    SVNFileUtil.createFile(getMinUnpackedRevFile(), "0\n", "US-ASCII");
                }
                if (myDBFormat < MIN_REP_SHARING_FORMAT ) {
                    SVNFileUtil.createFile(getMinUnpackedRevFile(), "0\n", "US-ASCII");
                }

                if (myDBFormat < MIN_PACKED_REVPROP_FORMAT)
                {
                    SVNFileUtil.createFile(getMinUnpackedRevPropPath(),"0\n", "US-ASCII");
                    myRevisionProperitesDb = SVNSqlJetDb.open(
                            getRevisionPropertiesDbPath(), SVNSqlJetDb.Mode.RWCreate);
                    myRevisionProperitesDb.execStatement(SVNWCDbStatements.REVPROP_CREATE_SCHEMA);
                }

            } finally {
                writeLock.unlock();
                FSWriteLock.release(writeLock);
            }
            // force reopen to create db.
            close();
            open();
        }
    }

    protected void writeDBFormat(int format, long maxFilesPerDir, boolean overwrite) throws SVNException {
        File formatFile = getDBFormatFile();
        SVNErrorManager.assertionFailure(format >= 1 && format <= DB_FORMAT, "unexpected format " + String.valueOf(format), SVNLogType.FSFS);
        String contents = null;
        if (format >= LAYOUT_FORMAT_OPTION_MINIMAL_FORMAT) {
            if (maxFilesPerDir > 0) {
                contents = format + "\nlayout sharded " + maxFilesPerDir + "\n";
            } else {
                contents = format + "\nlayout linear";
            }
        } else {
            contents = format + "\n";
        }

        if (!overwrite) {
            SVNFileUtil.createFile(formatFile, contents, "US-ASCII");
        } else {
            File tmpFile = SVNFileUtil.createUniqueFile(formatFile.getParentFile(), formatFile.getName(), ".tmp", false);
            OutputStream os = null;
            try {
                os = SVNFileUtil.openFileForWriting(tmpFile);
                os.write(contents.getBytes("US-ASCII"));
            } catch (IOException e) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
                SVNErrorManager.error(error, SVNLogType.FSFS);
            } finally {
                SVNFileUtil.closeFile(os);
            }
            if (SVNFileUtil.isWindows) {
                SVNFileUtil.setReadonly(formatFile, false);
            }
            SVNFileUtil.rename(tmpFile, formatFile);
        }
        SVNFileUtil.setReadonly(formatFile, true);
    }

    public SVNProperties getRevisionProperties(long revision) throws SVNException {
        try{
            return readRevisionProperties(revision);
        } catch(SVNException e ) {
            if(e.getErrorMessage().getErrorCode()==SVNErrorCode.FS_NO_SUCH_REVISION &&
                    myDBFormat >= MIN_PACKED_REVPROP_FORMAT ) {
                updateMinUnpackedRevProp();
                return readRevisionProperties(revision);
            }
            throw e;
        }
    }

    private SVNProperties readRevisionProperties(long revision) throws SVNException {
        ensureRevisionsExists(revision);
        if (myDBFormat < MIN_PACKED_REVPROP_FORMAT || revision >= myMinUnpackedRevProp) {
            FSFile file = new FSFile(getRevisionPropertiesFile(revision, false));
            try {
                return file.readProperties(false, true);
            } finally {
                file.close();
            }
         }

        final SVNSqlJetStatement stmt = myRevisionProperitesDb.getStatement(SVNWCDbStatements.FSFS_GET_REVPROP);
        try{
            stmt.bindLong(1, revision);
            boolean have_row = stmt.next();
            if (!have_row) {
                SVNErrorMessage err = SVNErrorMessage.create( SVNErrorCode.FS_NO_SUCH_REVISION,
                        "No such revision ''{0}''", revision );
                SVNErrorManager.error(err, SVNLogType.FSFS);
                return null;
            }
            return stmt.getColumnProperties(SVNWCDbSchema.REVPROP__Fields.properties);
        } finally {
            stmt.reset();
        }
    }

    public FSRevisionRoot createRevisionRoot(long revision) throws SVNException {
        ensureRevisionsExists(revision);
        return new FSRevisionRoot(this, revision);
    }

    public FSTransactionRoot createTransactionRoot(FSTransactionInfo txn) throws SVNException {
        SVNProperties txnProps = getTransactionProperties(txn.getTxnId());
        int flags = 0;
        if (txnProps.getStringValue(SVNProperty.TXN_CHECK_OUT_OF_DATENESS) != null) {
            flags |= FSTransactionRoot.SVN_FS_TXN_CHECK_OUT_OF_DATENESS;
        }
        if (txnProps.getStringValue(SVNProperty.TXN_CHECK_LOCKS) != null) {
            flags |= FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS;
        }

        return new FSTransactionRoot(this, txn.getTxnId(), txn.getBaseRevision(), flags);
    }

    public FSTransactionInfo openTxn(String txnName) throws SVNException {
        SVNFileType kind = SVNFileType.getType(getTransactionDir(txnName));
        if (kind != SVNFileType.DIRECTORY) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_TRANSACTION, "No such transaction");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        FSTransactionRoot txnRoot = new FSTransactionRoot(this, txnName, -1, 0);
        FSTransactionInfo localTxn = txnRoot.getTxn();
        return new FSTransactionInfo(localTxn.getBaseRevision(), txnName);
    }

    public FSRevisionNode getRevisionNode(FSID id) throws SVNException  {
        FSFile revisionFile = null;

        if (id.isTxn()) {
            File file = new File(getTransactionDir(id.getTxnID()), PATH_PREFIX_NODE + id.getNodeID() + "." + id.getCopyID());
            revisionFile = new FSFile(file);
        } else {
            revisionFile = openAndSeekRevision(id.getRevision(), id.getOffset());
        }

        Map headers = null;
        try {
            headers = revisionFile.readHeader();
        } finally{
            revisionFile.close();
        }

        FSRevisionNode node = FSRevisionNode.fromMap(headers);
        if (node.isFreshTxnRoot()) {
            node.setFreshRootPredecessorId(node.getPredecessorId());
        }
        return node;
    }

    public Map getDirContents(FSRevisionNode revNode) throws SVNException {
        FSRepresentation txtRep = revNode.getTextRepresentation();
        if (txtRep != null && txtRep.isTxn()) {
            FSFile childrenFile = getTransactionRevisionNodeChildrenFile(revNode.getId());
            Map entries = null;
            try {
                SVNProperties rawEntries = childrenFile.readProperties(false, false);
                rawEntries.putAll(childrenFile.readProperties(true, false));

                rawEntries.removeNullValues();

                entries = parsePlainRepresentation(rawEntries, true);
            } finally {
                childrenFile.close();
            }
            return entries;
        } else if (txtRep != null) {
            FSFile revisionFile = null;

            try {
                revisionFile = openAndSeekRepresentation(txtRep);
                String repHeader = revisionFile.readLine(160);

                if(!"PLAIN".equals(repHeader)){
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed representation header");
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }

                revisionFile.resetDigest();
                SVNProperties rawEntries = revisionFile.readProperties(false, false);
                String checksum = revisionFile.digest();

                if (!checksum.equals(txtRep.getMD5HexDigest())) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT,
                            "Checksum mismatch while reading representation:\n   expected:  {0}\n     actual:  {1}",
                            new Object[] { checksum, txtRep.getMD5HexDigest() });
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }

                return parsePlainRepresentation(rawEntries, false);
            } finally {
                if(revisionFile != null){
                    revisionFile.close();
                }
            }
        }
        return new SVNHashMap();// returns an empty map, must not be null!!
    }

    public SVNProperties getProperties(FSRevisionNode revNode) throws SVNException {
        if (revNode.getPropsRepresentation() != null && revNode.getPropsRepresentation().isTxn()) {
            FSFile propsFile = null;
            try {
                propsFile = getTransactionRevisionNodePropertiesFile(revNode.getId());
                return propsFile.readProperties(false, true);
            } finally {
                if(propsFile != null){
                    propsFile.close();
                }
            }
        } else if (revNode.getPropsRepresentation() != null) {
            FSRepresentation propsRep = revNode.getPropsRepresentation();
            FSFile revisionFile = null;

            try {
                revisionFile = openAndSeekRepresentation(propsRep);
                String repHeader = revisionFile.readLine(160);

                if(!"PLAIN".equals(repHeader)){
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed representation header");
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }

                revisionFile.resetDigest();
                SVNProperties props = revisionFile.readProperties(false, true);
                String checksum = revisionFile.digest();

                if (!checksum.equals(propsRep.getMD5HexDigest())) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Checksum mismatch while reading representation:\n   expected:  {0}\n     actual:  {1}", new Object[] {
                            checksum, propsRep.getMD5HexDigest()
                    });
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
                return props;
            } finally {
                if(revisionFile != null){
                    revisionFile.close();
                }
            }
        }
        return new SVNProperties();// no properties? return an empty SVNProperties
    }

    public String[] getNextRevisionIDs() throws SVNException {
        String[] ids = new String[2];
        FSFile currentFile = new FSFile(getCurrentFile());
        String idsLine = null;

        try{
            idsLine = currentFile.readLine(80);
        }finally{
            currentFile.close();
        }

        if (idsLine == null || idsLine.length() == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Corrupt current file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        int spaceInd = idsLine.indexOf(' ');
        if (spaceInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Corrupt current file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        idsLine = idsLine.substring(spaceInd + 1);
        spaceInd = idsLine.indexOf(' ');
        if (spaceInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Corrupt current file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String nodeID = idsLine.substring(0, spaceInd);
        String copyID = idsLine.substring(spaceInd + 1);

        ids[0] = nodeID;
        ids[1] = copyID;
        return ids;
    }

    public String getAndIncrementTxnKey() throws SVNException {
        FSWriteLock writeLock = FSWriteLock.getWriteLockForCurrentTxn("_" + TXN_CURRENT_FILE, this);
        synchronized (writeLock) {
            try {
                writeLock.lock();
                File txnCurrentFile = getTransactionCurrentFile();
                FSFile reader = new FSFile(txnCurrentFile);
                String txnId = null;
                try {
                    txnId = reader.readLine(200);
                } finally {
                    reader.close();
                }

                String nextTxnId = FSRepositoryUtil.generateNextKey(txnId);

                OutputStream txnCurrentOS = null;
                File tmpFile = null;
                try {
                    tmpFile = SVNFileUtil.createUniqueFile(txnCurrentFile.getParentFile(),
                                                           TXN_CURRENT_FILE, ".tmp", false);
                    txnCurrentOS = SVNFileUtil.openFileForWriting(tmpFile);
                    nextTxnId = nextTxnId + "\n";
                    txnCurrentOS.write(nextTxnId.getBytes("UTF-8"));
                } catch (IOException ioe) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
                    SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
                } finally {
                    SVNFileUtil.closeFile(txnCurrentOS);
                }
                SVNFileUtil.rename(tmpFile, txnCurrentFile);
                return txnId;
            } finally {
                writeLock.unlock();
                FSWriteLock.release(writeLock);
            }
        }
    }

    public Map listTransactions() {
        Map result = new SVNHashMap();
        File txnsDir = getTransactionsParentDir();

        File[] entries = SVNFileListUtil.listFiles(txnsDir);
        for (int i = 0; i < entries.length; i++) {
            File entry = entries[i];
            if (entry.getName().length() <= TXN_PATH_EXT.length() || !entry.getName().endsWith(TXN_PATH_EXT)) {
                continue;
            }
            String txnName = entry.getName().substring(0, entry.getName().lastIndexOf(TXN_PATH_EXT));
            result.put(txnName, entry);
        }
        return result;
    }

    public File getNewRevisionFile(long newRevision) {
        if (myMaxFilesPerDirectory > 0 && (newRevision % myMaxFilesPerDirectory == 0)) {
            File shardDir = new File(getDBRevsDir(), String.valueOf(newRevision/myMaxFilesPerDirectory));
            shardDir.mkdirs();
        }

        File revFile = null;
        if (myMaxFilesPerDirectory > 0) {
            File shardDir = new File(getDBRevsDir(), String.valueOf(newRevision/myMaxFilesPerDirectory));
            revFile = new File(shardDir, String.valueOf(newRevision));
        } else {
            revFile = new File(getDBRevsDir(), String.valueOf(newRevision));
        }
        return revFile;
    }

    public File getNewRevisionPropertiesFile(long newRevision) {
        if (myMaxFilesPerDirectory > 0 && (newRevision % myMaxFilesPerDirectory == 0)) {
            File shardDir = new File(getRevisionPropertiesRoot(), String.valueOf(newRevision/myMaxFilesPerDirectory));
            shardDir.mkdirs();
        }

        File revPropsFile = null;
        if (myMaxFilesPerDirectory > 0) {
            File shardDir = new File(getRevisionPropertiesRoot(), String.valueOf(newRevision/myMaxFilesPerDirectory));
            revPropsFile = new File(shardDir, String.valueOf(newRevision));
        } else {
            revPropsFile = new File(getRevisionPropertiesRoot(), String.valueOf(newRevision));
        }
        return revPropsFile;
    }

    public File getTransactionDir(String txnID) {
        return new File(getTransactionsParentDir(), txnID + TXN_PATH_EXT);
    }

    public void setYoungestRevisionCache(long revision) {
        myYoungestRevisionCache = revision;
    }

    public void setUUID(String uuid) throws SVNException {
        File uniqueFile = SVNFileUtil.createUniqueFile(getDBRoot(), UUID_FILE, ".tmp", false);
        uuid += '\n';

        OutputStream uuidOS = null;
        try {
            uuidOS = SVNFileUtil.openFileForWriting(uniqueFile);
            uuidOS.write(uuid.getBytes("US-ASCII"));
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR,
                    "Error writing repository UUID to ''{0}''", getUUIDFile());
            err.setChildErrorMessage(SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage()));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(uuidOS);
        }
        SVNFileUtil.rename(uniqueFile, getUUIDFile());
    }

    public File getRevisionPropertiesFile(long revision, boolean returnMissing) throws SVNException {
        File revPropsFile = null;
        if (myMaxFilesPerDirectory > 0) {
            File shardDir = new File(getRevisionPropertiesRoot(), String.valueOf(revision/myMaxFilesPerDirectory));
            revPropsFile = new File(shardDir, String.valueOf(revision));
        } else {
            revPropsFile = new File(getRevisionPropertiesRoot(), String.valueOf(revision));
        }

        if (!revPropsFile.exists() && !returnMissing) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION, "No such revision {0}", String.valueOf(revision));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return revPropsFile;
    }

    public FSFile openAndSeekRepresentation(FSRepresentation rep) throws SVNException {
        if (!rep.isTxn()) {
            return openAndSeekRevision(rep.getRevision(), rep.getOffset());
        }
        return openAndSeekTransaction(rep);
    }

    public File getNextIDsFile(String txnID) {
        return new File(getTransactionDir(txnID), "next-ids");
    }

    public void writeNextIDs(String txnID, String nodeID, String copyID) throws SVNException {
        OutputStream nextIdsFile = null;
        try {
            nextIdsFile = SVNFileUtil.openFileForWriting(getNextIDsFile(txnID));
            String ids = nodeID + " " + copyID + "\n";
            nextIdsFile.write(ids.getBytes("UTF-8"));
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(nextIdsFile);
        }
    }

    public void changeTransactionProperties(String txnId, SVNProperties txnProperties) throws SVNException {
        for (Iterator iter = txnProperties.nameSet().iterator(); iter.hasNext();) {
            String propName = (String) iter.next();
            SVNPropertyValue propValue = txnProperties.getSVNPropertyValue(propName);
            setTransactionProperty(txnId, propName, propValue);
        }
    }

    public void setTransactionProperty(String txnID, String name, SVNPropertyValue propertyValue) throws SVNException {
        FSRepositoryUtil.validateProperty(name, propertyValue);
        SVNWCProperties revProps = new SVNWCProperties(getTransactionPropertiesFile(txnID), null);
        revProps.setPropertyValue(name, propertyValue);
    }

    public void setRevisionProperty(long revision, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        ensureRevisionsExists(revision);
        if (myDBFormat < MIN_PACKED_REVPROP_FORMAT ||
                revision >= myMinUnpackedRevProp ) {
            FSWriteLock writeLock = FSWriteLock.getWriteLockForDB(this);
            synchronized (writeLock) {
                try {
                    writeLock.lock();
                    SVNWCProperties revProps = new SVNWCProperties(getRevisionPropertiesFile(revision, false), null);
                    revProps.setPropertyValue(propertyName, propertyValue);
                } finally {
                    writeLock.unlock();
                    FSWriteLock.release(writeLock);
                }
            }
        } else {
            final SVNProperties revisionProperties = getRevisionProperties(revision);
            revisionProperties.put(propertyName, propertyValue);
            final SVNSqlJetStatement stmt = myRevisionProperitesDb.getStatement(SVNWCDbStatements.FSFS_SET_REVPROP);
            try{
                stmt.insert(new Object[] { revision, SVNSkel.createPropList(revisionProperties.asMap()).getData() } );
            } finally{
                stmt.reset();
            }
        }
    }

    public SVNProperties getTransactionProperties(String txnID) throws SVNException {
        FSFile txnPropsFile = new FSFile(getTransactionPropertiesFile(txnID));
        try {
            return txnPropsFile.readProperties(false, true);
        } finally {
            txnPropsFile.close();
        }
    }

    public File getTransactionPropertiesFile(String txnID) {
        return new File(getTransactionDir(txnID), "props");
    }

    public File getTransactionProtoRevsDir() {
        if (myTransactionProtoRevsRoot == null) {
            myTransactionProtoRevsRoot = new File(getDBRoot(), TRANSACTION_PROTOS_DIR);
        }
        return myTransactionProtoRevsRoot;
    }

    public File getTransactionProtoRevFile(String txnID) {
        if (myDBFormat >= MIN_PROTOREVS_DIR_FORMAT) {
            return new File(getTransactionProtoRevsDir(), txnID + ".rev");
        }
        return new File(getTransactionDir(txnID), "rev");
    }

    public File getTransactionProtoRevLockFile(String txnID) {
        if (myDBFormat >= MIN_PROTOREVS_DIR_FORMAT) {
            return new File(getTransactionProtoRevsDir(), txnID + ".rev-lock");
        }
        return new File(getTransactionDir(txnID), "rev-lock");
    }

    public void purgeTxn(String txnID) throws SVNException {
        SVNFileUtil.deleteAll(getTransactionDir(txnID), true);
        if (getDBFormat() >= FSFS.MIN_PROTOREVS_DIR_FORMAT) {
            SVNFileUtil.deleteFile(getTransactionProtoRevFile(txnID));
            SVNFileUtil.deleteFile(getTransactionProtoRevLockFile(txnID));
        }
    }

    public void createNewTxnNodeRevisionFromRevision(String txnID, FSRevisionNode sourceNode) throws SVNException {
        if (sourceNode.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Copying from transactions not allowed");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        FSRevisionNode revNode = FSRevisionNode.dumpRevisionNode(sourceNode);
        revNode.setPredecessorId(sourceNode.getId());
        revNode.setCount(revNode.getCount() + 1);
        revNode.setCopyFromPath(null);
        revNode.setIsFreshTxnRoot(true);
        revNode.setCopyFromRevision(SVNRepository.INVALID_REVISION);
        revNode.setId(FSID.createTxnId(sourceNode.getId().getNodeID(), sourceNode.getId().getCopyID(), txnID));
        putTxnRevisionNode(revNode.getId(), revNode);
    }

    public void putTxnRevisionNode(FSID id, FSRevisionNode revNode) throws SVNException {
        if (!id.isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Attempted to write to non-transaction");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        OutputStream revNodeFile = null;
        try {
            revNodeFile = SVNFileUtil.openFileForWriting(getTransactionRevNodeFile(id));
            writeTxnNodeRevision(revNodeFile, revNode);
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(revNodeFile);
        }
    }

    public File getTransactionRevNodeFile(FSID id) {
        return new File(getTransactionDir(id.getTxnID()), PATH_PREFIX_NODE + id.getNodeID() + "." + id.getCopyID());
    }

    public void writeTxnNodeRevision(OutputStream revNodeFile, FSRevisionNode revNode) throws IOException {
        String id = FSRevisionNode.HEADER_ID + ": " + revNode.getId() + "\n";
        revNodeFile.write(id.getBytes("UTF-8"));
        String type = FSRevisionNode.HEADER_TYPE + ": " + revNode.getType() + "\n";
        revNodeFile.write(type.getBytes("UTF-8"));

        if (revNode.getPredecessorId() != null) {
            String predId = FSRevisionNode.HEADER_PRED + ": " + revNode.getPredecessorId() + "\n";
            revNodeFile.write(predId.getBytes("UTF-8"));
        }

        String count = FSRevisionNode.HEADER_COUNT + ": " + revNode.getCount() + "\n";
        revNodeFile.write(count.getBytes("UTF-8"));

        if (revNode.getTextRepresentation() != null) {
            FSRepresentation txtRep = revNode.getTextRepresentation();
            String textRepresentation = FSRevisionNode.HEADER_TEXT + ": " + (txtRep.getTxnId() != null && revNode.getType() == SVNNodeKind.DIR ?
                    "-1" : txtRep.getStringRepresentation(myDBFormat)) + "\n";
            revNodeFile.write(textRepresentation.getBytes("UTF-8"));
        }

        if (revNode.getPropsRepresentation() != null) {
            FSRepresentation propRep = revNode.getPropsRepresentation();
            String propsRepresentation = FSRevisionNode.HEADER_PROPS + ": " + (propRep.getTxnId() != null ?
                    "-1" : propRep.getStringRepresentation(myDBFormat)) + "\n";
            revNodeFile.write(propsRepresentation.getBytes("UTF-8"));
        }

        String cpath = FSRevisionNode.HEADER_CPATH + ": " + revNode.getCreatedPath() + "\n";
        revNodeFile.write(cpath.getBytes("UTF-8"));

        if (revNode.getCopyFromPath() != null) {
            String copyFromPath = FSRevisionNode.HEADER_COPYFROM + ": " + revNode.getCopyFromRevision() + " " + revNode.getCopyFromPath() + "\n";
            revNodeFile.write(copyFromPath.getBytes("UTF-8"));
        }

        if (revNode.getCopyRootRevision() != revNode.getId().getRevision() ||
            !revNode.getCopyRootPath().equals(revNode.getCreatedPath())) {
            String copyroot = FSRevisionNode.HEADER_COPYROOT + ": " + revNode.getCopyRootRevision() + " " + revNode.getCopyRootPath() + "\n";
            revNodeFile.write(copyroot.getBytes("UTF-8"));
        }

        if (revNode.isFreshTxnRoot()) {
            String isFreshRootStr = FSRevisionNode.HEADER_IS_FRESH_TXN_ROOT + ": y\n";
            revNodeFile.write(isFreshRootStr.getBytes("UTF-8"));
        }

        if (supportsMergeInfo()) {
            if (revNode.getMergeInfoCount() > 0) {
                String mergeInfoCntStr = FSRevisionNode.HEADER_MERGE_INFO_COUNT + ": " + revNode.getMergeInfoCount() + "\n";
                revNodeFile.write(mergeInfoCntStr.getBytes("UTF-8"));
            }
            if (revNode.hasMergeInfo()) {
                String hasMergeInfoStr = FSRevisionNode.HEADER_MERGE_INFO_HERE + ": y\n";
                revNodeFile.write(hasMergeInfoStr.getBytes("UTF-8"));
            }
        }

        revNodeFile.write("\n".getBytes("UTF-8"));
    }

    public SVNLock getLock(String repositoryPath, boolean haveWriteLock, boolean throwError) throws SVNException {
        repositoryPath = SVNPathUtil.canonicalizeAbsolutePath(repositoryPath);

        SVNLock lock = fetchLockFromDigestFile(null, repositoryPath, null);

        if (lock == null) {
            if (!throwError) {
                return null;
            }
            SVNErrorManager.error(FSErrors.errorNoSuchLock(repositoryPath, this), SVNLogType.FSFS);
        }

        Date current = new Date(System.currentTimeMillis());

        if (lock.getExpirationDate() != null && current.compareTo(lock.getExpirationDate()) > 0) {
            if (haveWriteLock) {
                deleteLock(lock);
            }
            if (!throwError) {
                return null;
            }
            SVNErrorManager.error(FSErrors.errorLockExpired(lock.getID(), this), SVNLogType.FSFS);
        }
        return lock;
    }

    public void deleteLock(SVNLock lock) throws SVNException {
        String reposPath = lock.getPath();
        String childToKill = null;
        Collection children = new ArrayList();
        while (true) {
            fetchLockFromDigestFile(null, reposPath, children);
            if (childToKill != null) {
                children.remove(childToKill);
            }

            if (children.size() == 0) {
                childToKill = getDigestFromRepositoryPath(reposPath);
                File digestFile = getDigestFileFromRepositoryPath(reposPath);
                SVNFileUtil.deleteFile(digestFile);
            } else {
                writeDigestLockFile(null, children, reposPath, false);
                childToKill = null;
            }

            if ("/".equals(reposPath)) {
                break;
            }

            reposPath = SVNPathUtil.removeTail(reposPath);

            if ("".equals(reposPath)) {
                reposPath = "/";
            }
            children.clear();
        }
    }

    public void walkDigestFiles(File digestFile, ISVNLockHandler getLocksHandler, boolean haveWriteLock) throws SVNException {
        Collection children = new LinkedList();
        SVNLock lock = fetchLockFromDigestFile(digestFile, null, children);

        if (lock != null) {
            Date current = new Date(System.currentTimeMillis());
            if (lock.getExpirationDate() == null || current.compareTo(lock.getExpirationDate()) < 0) {
                getLocksHandler.handleLock(lock.getPath(), lock, null);
            } else if (haveWriteLock) {
                deleteLock(lock);
            }
        }

        if (children.isEmpty()) {
            return;
        }

        for (Iterator entries = children.iterator(); entries.hasNext();) {
            String digestName = (String) entries.next();
            File parent = new File(getDBLocksDir(), digestName.substring(0, FSFS.DIGEST_SUBDIR_LEN));
            File childDigestFile = new File(parent, digestName);
            walkDigestFiles(childDigestFile, getLocksHandler, haveWriteLock);
        }
    }

    public SVNLock getLockHelper(String repositoryPath, boolean haveWriteLock) throws SVNException {
        SVNLock lock = null;
        try {
            lock = getLock(repositoryPath, haveWriteLock, false);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NO_SUCH_LOCK || svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_LOCK_EXPIRED) {
                return null;
            }
            throw svne;
        }
        return lock;
    }

    public SVNLock fetchLockFromDigestFile(File digestFile, String repositoryPath, Collection children) throws SVNException {
        File digestLockFile = digestFile == null ? getDigestFileFromRepositoryPath(repositoryPath) : digestFile;
        SVNProperties lockProps = null;

        if (digestLockFile.exists()) {
            FSFile reader = new FSFile(digestLockFile);
            try {
                lockProps = reader.readProperties(false, true);
            } catch (SVNException svne) {
                SVNErrorMessage err = svne.getErrorMessage().wrap("Can't parse lock/entries hashfile ''{0}''", digestLockFile);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            } finally {
                reader.close();
            }
        } else {
            lockProps = new SVNProperties();
        }

        SVNLock lock = null;
        String lockPath = SVNPropertyValue.getPropertyAsString(lockProps.getSVNPropertyValue(FSFS.PATH_LOCK_KEY));
        if (lockPath != null) {
            String lockToken = SVNPropertyValue.getPropertyAsString(lockProps.getSVNPropertyValue(FSFS.TOKEN_LOCK_KEY));
            if (lockToken == null) {
                SVNErrorManager.error(FSErrors.errorCorruptLockFile(lockPath, this), SVNLogType.FSFS);
            }
            String lockOwner = SVNPropertyValue.getPropertyAsString(lockProps.getSVNPropertyValue(FSFS.OWNER_LOCK_KEY));
            if (lockOwner == null) {
                SVNErrorManager.error(FSErrors.errorCorruptLockFile(lockPath, this), SVNLogType.FSFS);
            }
            String davComment = SVNPropertyValue.getPropertyAsString(lockProps.getSVNPropertyValue(FSFS.IS_DAV_COMMENT_LOCK_KEY));
            if (davComment == null) {
                SVNErrorManager.error(FSErrors.errorCorruptLockFile(lockPath, this), SVNLogType.FSFS);
            }
            String creationTime = SVNPropertyValue.getPropertyAsString(lockProps.getSVNPropertyValue(FSFS.CREATION_DATE_LOCK_KEY));
            if (creationTime == null) {
                SVNErrorManager.error(FSErrors.errorCorruptLockFile(lockPath, this), SVNLogType.FSFS);
            }
            Date creationDate = SVNDate.parseDateString(creationTime);
            String expirationTime = SVNPropertyValue.getPropertyAsString(lockProps.getSVNPropertyValue(FSFS.EXPIRATION_DATE_LOCK_KEY));
            Date expirationDate = null;
            if (expirationTime != null) {
                expirationDate = SVNDate.parseDateString(expirationTime);
            }
            String comment = SVNPropertyValue.getPropertyAsString(lockProps.getSVNPropertyValue(FSFS.COMMENT_LOCK_KEY));
            lock = new FSLock(lockPath, lockToken, lockOwner, comment, creationDate, expirationDate, "1".equals(davComment));
        }

        String childEntries = SVNPropertyValue.getPropertyAsString(lockProps.getSVNPropertyValue(FSFS.CHILDREN_LOCK_KEY));
        if (children != null && childEntries != null) {
            String[] digests = childEntries.split("\n");
            for (int i = 0; i < digests.length; i++) {
                children.add(digests[i]);
            }
        }
        return lock;
    }

    public File getDigestFileFromRepositoryPath(String repositoryPath) throws SVNException {
        String digest = getDigestFromRepositoryPath(repositoryPath);
        File parent = new File(getDBLocksDir(), digest.substring(0, FSFS.DIGEST_SUBDIR_LEN));
        return new File(parent, digest);
    }

    public String getDigestFromRepositoryPath(String repositoryPath) throws SVNException {
        repositoryPath = SVNPathUtil.canonicalizeAbsolutePath(repositoryPath);
        MessageDigest digestFromPath = null;
        try {
            digestFromPath = MessageDigest.getInstance("MD5");
            digestFromPath.update(repositoryPath.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException nsae) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "MD5 implementation not found: {0}", nsae.getLocalizedMessage());
            SVNErrorManager.error(err, nsae, SVNLogType.FSFS);
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        }
        return SVNFileUtil.toHexDigest(digestFromPath);
    }

    public void unlockPath(String path, String token, String username, boolean breakLock, boolean enableHooks) throws SVNException {
        path = SVNPathUtil.canonicalizeAbsolutePath(path);

        String[] paths = {path};

        if (!breakLock && username == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_USER, "Cannot unlock path ''{0}'', no authenticated username available", path);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (enableHooks && isHooksEnabled()) {
            FSHooks.runPreUnlockHook(myRepositoryRoot, path, username);
        }

        FSWriteLock writeLock = FSWriteLock.getWriteLockForDB(this);
        synchronized (writeLock) {
            try {
                writeLock.lock();
                unlock(path, token, username, breakLock);
            } finally {
                writeLock.unlock();
                FSWriteLock.release(writeLock);
            }
        }

        if (enableHooks && isHooksEnabled()) {
            try {
                FSHooks.runPostUnlockHook(myRepositoryRoot, paths, username);
            } catch (SVNException svne) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_POST_UNLOCK_HOOK_FAILED, "Unlock succeeded, but post-unlock hook failed");
                err.setChildErrorMessage(svne.getErrorMessage());
                SVNErrorManager.error(err, svne, SVNLogType.FSFS);
            }
        }
    }

    public SVNLock lockPath(String path, String token, String username, String comment, Date expirationDate, long currentRevision,
            boolean stealLock, boolean isDAVComment) throws SVNException {
        path = SVNPathUtil.canonicalizeAbsolutePath(path);

        String[] paths = { path };

        if (username == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_USER,
                    "Cannot lock path ''{0}'', no authenticated username available.", path);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        String customToken = null;
        if (isHooksEnabled()) {
            customToken = FSHooks.runPreLockHook(myRepositoryRoot, path, username, comment, stealLock);
            if (customToken != null) {
                token = customToken;
            }
        }
        SVNLock lock = null;

        FSWriteLock writeLock = FSWriteLock.getWriteLockForDB(this);

        synchronized (writeLock) {
            try {
                writeLock.lock();
                lock = lock(path, token, username, comment, expirationDate, currentRevision, stealLock, isDAVComment);
            } finally {
                writeLock.unlock();
                FSWriteLock.release(writeLock);
            }
        }

        if (isHooksEnabled()) {
            try {
                FSHooks.runPostLockHook(myRepositoryRoot, paths, username);
            } catch (SVNException svne) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_POST_LOCK_HOOK_FAILED, "Lock succeeded, but post-lock hook failed");
                err.setChildErrorMessage(svne.getErrorMessage());
                SVNErrorManager.error(err, svne, SVNLogType.FSFS);
            }
        }
        return lock;
    }

    public SVNProperties compoundMetaProperties(long revision) throws SVNException {
        SVNProperties metaProperties = new SVNProperties();
        SVNProperties revProps = getRevisionProperties(revision);
        String uuid = getUUID();
        String rev = String.valueOf(revision);

        metaProperties.put(SVNProperty.LAST_AUTHOR, revProps.getStringValue(SVNRevisionProperty.AUTHOR));
        metaProperties.put(SVNProperty.COMMITTED_DATE, revProps.getStringValue(SVNRevisionProperty.DATE));

        metaProperties.put(SVNProperty.COMMITTED_REVISION, rev);
        metaProperties.put(SVNProperty.UUID, uuid);

        return metaProperties;
    }

    public long getDeletedRevision(String path, long startRev, long endRev) throws SVNException {
        if (FSRepository.isInvalidRevision(startRev)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION, "Invalid start revision {0}", new Long(startRev));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        if (FSRepository.isInvalidRevision(endRev)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION, "Invalid end revision {0}", new Long(endRev));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        if (startRev > endRev) {
            long tmpRev = endRev;
            endRev = startRev;
            startRev = tmpRev;
        }

        FSRevisionRoot startRoot = createRevisionRoot(startRev);
        FSRevisionNode startNode = null;
        try {
            startNode = startRoot.getRevisionNode(path);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                return SVNRepository.INVALID_REVISION;
            }
            throw svne;
        }
        FSID startNodeId = startNode.getId();

        FSRevisionRoot endRoot = createRevisionRoot(endRev);
        FSRevisionNode endNode = null;
        try {
            endNode = endRoot.getRevisionNode(path);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() != SVNErrorCode.FS_NOT_FOUND) {
                throw svne;
            }
        }

        if (endNode != null) {
            FSID endNodeId = endNode.getId();
            if (startNodeId.compareTo(endNodeId) != -1) {
                FSClosestCopy closestCopy = endRoot.getClosestCopy(path);
                if (closestCopy == null || closestCopy.getRevisionRoot() == null ||
                        closestCopy.getRevisionRoot().getRevision() <= startRev) {
                    return SVNRepository.INVALID_REVISION;
                }
            }
        }

        long midRev = (startRev + endRev)/2;
        while (true) {
            FSRevisionRoot root = createRevisionRoot(midRev);
            FSRevisionNode node = null;
            try {
                node = root.getRevisionNode(path);
            } catch (SVNException svne) {
                if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                    endRev = midRev;
                    midRev = (startRev + endRev)/2;
                } else {
                    throw svne;
                }
            }

            if (node != null) {
                FSID currentNodeId = node.getId();
                int nodeRelationship = startNodeId.compareTo(currentNodeId);
                FSClosestCopy closestCopy = root.getClosestCopy(path);
                if (nodeRelationship == -1 || (closestCopy != null &&
                    closestCopy.getRevisionRoot() != null &&
                    closestCopy.getRevisionRoot().getRevision() > startRev)) {
                    endRev = midRev;
                    midRev = (startRev + endRev)/2;
                } else if (endRev - midRev == 1) {
                    return endRev;
                } else {
                    startRev = midRev;
                    midRev = (startRev + endRev)/2;
                }
            }
        }
    }

    public SVNLocationEntry getPreviousLocation(String path, long revision, long[] appearedRevision) throws SVNException {
        if (appearedRevision != null && appearedRevision.length > 0) {
            appearedRevision[0] = SVNRepository.INVALID_REVISION;
        }
        FSRevisionRoot root = createRevisionRoot(revision);
        FSClosestCopy closestCopy = root.getClosestCopy(path);
        if (closestCopy == null) {
            return null;
        }

        FSRevisionRoot copyTargetRoot = closestCopy.getRevisionRoot();
        if (copyTargetRoot == null) {
            return null;
        }
        String copyTargetPath = closestCopy.getPath();
        FSRevisionNode copyFromNode = copyTargetRoot.getRevisionNode(copyTargetPath);
        String copyFromPath = copyFromNode.getCopyFromPath();
        long copyFromRevision = copyFromNode.getCopyFromRevision();
        String remainder = "";
        if (!path.equals(copyTargetPath)) {
            remainder = path.substring(copyTargetPath.length());
            if (remainder.startsWith("/")) {
                remainder = remainder.substring(1);
            }
        }
        String previousPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(copyFromPath, remainder));
        if (appearedRevision != null && appearedRevision.length > 0) {
            appearedRevision[0] = copyTargetRoot.getRevision();
        }
        return new SVNLocationEntry(copyFromRevision, previousPath);
    }

    public String getNodeOrigin(String nodeID) throws SVNException {
        SVNProperties nodeOrigins = getNodeOriginsFromFile(nodeID);
        if (nodeOrigins != null) {
            return nodeOrigins.getStringValue(nodeID);
        }
        return null;
    }

    public void setNodeOrigin(String nodeID, FSID nodeRevisionID) throws SVNException {
        File nodeOriginsDir = getNodeOriginsDir();
        ensureDirExists(nodeOriginsDir, true);
        SVNProperties nodeOrigins = getNodeOriginsFromFile(nodeID);
        if (nodeOrigins == null) {
            nodeOrigins = new SVNProperties();
        }

        String oldNodeRevID = nodeOrigins.getStringValue(nodeID);
        String nodeRevIDToStore = nodeRevisionID.toString();
        if (oldNodeRevID != null && !nodeRevIDToStore.equals(oldNodeRevID)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT,
                    "Node origin for ''{0}'' exists with a different value ({1}) than what we were about " +
                    "to store ({2})", new Object[] { nodeID, oldNodeRevID, nodeRevIDToStore });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        nodeOrigins.put(nodeID, nodeRevIDToStore);

        File nodeOriginFile = getNodeOriginFile(nodeID);
        File tmpFile = SVNFileUtil.createUniqueFile(nodeOriginFile.getParentFile(), nodeOriginFile.getName(),
                ".tmp", false);
        SVNWCProperties.setProperties(nodeOrigins, nodeOriginFile, tmpFile,
                SVNWCProperties.SVN_HASH_TERMINATOR);
    }

    public boolean supportsMergeInfo() {
        return myDBFormat >= MIN_MERGE_INFO_FORMAT;
    }

    public void readOptions(FSFile formatFile, int formatNumber) throws SVNException {
        while (true) {
            String line = null;
            try {
                line = formatFile.readLine(80);
            } catch (SVNException svne) {
                if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.STREAM_UNEXPECTED_EOF) {
                    break;
                }
            }

            if (formatNumber >= LAYOUT_FORMAT_OPTION_MINIMAL_FORMAT && line.startsWith("layout ")) {
                String optionValue = line.substring(7);
                if (optionValue.equals("linear")) {
                    myMaxFilesPerDirectory = 0;
                    continue;
                } else if (optionValue.startsWith("sharded ")) {
                    optionValue = optionValue.substring(8);
                    try {
                        myMaxFilesPerDirectory = Long.parseLong(optionValue);
                    } catch (NumberFormatException nfe) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_VERSION_FILE_FORMAT,
                                "Format file ''{0}'' contains an unexpected non-digit", formatFile.getFile());
                        SVNErrorManager.error(err, SVNLogType.FSFS);
                    }
                    continue;
                }
            }

            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_VERSION_FILE_FORMAT,
                    "''{0}'' contains invalid filesystem format option ''{1}''",
                    new Object[] {formatFile.getFile(), line});
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
    }

    public IFSRepresentationCacheManager getRepositoryCacheManager() {
        return myReposCacheManager;
    }

    public static File findRepositoryRoot(File path) {
        if (path == null) {
            path = new File("");
        }
        File rootPath = path;
        while (!isRepositoryRoot(rootPath)) {
            rootPath = rootPath.getParentFile();
            if (rootPath == null) {
                return null;
            }
        }
        return rootPath;
    }

    public static String findRepositoryRoot(String host, String path) {
        if (path == null) {
            path = "";
        }
        String testPath = host != null ? SVNPathUtil.append("\\\\" + host, path) : path;
        testPath = testPath.replaceFirst("\\|", "\\:");
        File rootPath = new File(testPath).getAbsoluteFile();
        while (!isRepositoryRoot(rootPath)) {
            if (rootPath.getParentFile() == null) {
                return null;
            }
            path = SVNPathUtil.removeTail(path);
            rootPath = rootPath.getParentFile();
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        while (path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static long getDefaultMaxFilesPerDirectory() {
        return DEFAULT_MAX_FILES_PER_DIRECTORY;
    }

    public static void setDefaultMaxFilesPerDirectory(long maxFilesPerDirectory) {
        DEFAULT_MAX_FILES_PER_DIRECTORY = maxFilesPerDirectory;
    }

    protected  boolean isPackedRevision(long revision) {
        return revision < myMinUnpackedRevision;
    }

    protected File getNodeOriginFile(String nodeID) {
        String nodeIDMinusLastChar = nodeID.length() == 1 ? "0" : nodeID.substring(0, nodeID.length() - 1);
        return new File(getNodeOriginsDir(), nodeIDMinusLastChar);
    }

    protected FSFile getTransactionRevisionPrototypeFile(String txnID) {
        File revFile = getTransactionProtoRevFile(txnID);
        return new FSFile(revFile);
    }

    protected FSFile getTransactionChangesFile(String txnID) {
        File file = new File(getTransactionDir(txnID), "changes");
        return new FSFile(file);
    }

    protected FSFile getTransactionRevisionNodeChildrenFile(FSID txnID) {
        File childrenFile = new File(getTransactionDir(txnID.getTxnID()), PATH_PREFIX_NODE + txnID.getNodeID() +
                "." + txnID.getCopyID() + TXN_PATH_EXT_CHILDREN);
        return new FSFile(childrenFile);
    }

    protected FSFile getRevisionFSFile(long revision)  throws SVNException {
        File revisionFile = getRevisionFile(revision);

        if (!revisionFile.exists()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION, "No such revision {0}", String.valueOf(revision));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return new FSFile(revisionFile);
    }

    protected FSFile getPackOrRevisionFSFile(long revision) throws SVNException {
        File file = getAbsoluteRevisionPath(revision);
        if (!file.exists()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION, "No such revision {0}", String.valueOf(revision));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return new FSFile(file);
    }

    protected File getAbsoluteRevisionPath(long revision) throws SVNException {
        if (!isPackedRevision(revision)) {
            File revFile = getRevisionFile(revision);
            if (revFile.exists()) {
                return revFile;
            }
            getMinUnpackedRev();
            if (!isPackedRevision(revision)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION, "Revision file ''{0}'' does not exist, and r{1} is not packed",
                        new Object[] { revFile, String.valueOf(revision) });
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }
        return getPackedRevPath(revision, PACK_KIND_PACK);
    }

    protected FSFile getTransactionRevisionNodePropertiesFile(FSID id) {
        File revNodePropsFile = new File(getTransactionDir(id.getTxnID()), PATH_PREFIX_NODE + id.getNodeID() + "." + id.getCopyID() + TXN_PATH_EXT_PROPS);
        return new FSFile(revNodePropsFile);
    }

    protected File getPackedRevPath(long revision, String kind) throws SVNException {
        SVNErrorManager.assertionFailure(myMaxFilesPerDirectory > 0, "max files per directory is 0 or negative: " + String.valueOf(myMaxFilesPerDirectory), SVNLogType.FSFS);
        SVNErrorManager.assertionFailure(isPackedRevision(revision), "revision " + String.valueOf(revision) + " is not packed", SVNLogType.FSFS);

        File file = new File(getDBRevsDir(), (revision/myMaxFilesPerDirectory) + PACK_EXT);
        file = new File(file, kind);
        return file;
    }

    protected File getPackDir(long revision) {
        return new File(getDBRevsDir(), revision + PACK_EXT);
    }

    protected File getPackFile(long revision) {
        return new File(getPackDir(revision), PACK_KIND_PACK);
    }

    protected File getManifestFile(long revision) {
        return new File(getPackDir(revision), PACK_KIND_MANIFEST);
    }

    protected File getRevisionFile(long revision) throws SVNException {
        SVNErrorManager.assertionFailure(!isPackedRevision(revision), "revision " + String.valueOf(revision) + " is not expected to be packed", SVNLogType.FSFS);
        File revisionFile = null;
        if (myMaxFilesPerDirectory > 0) {
            File shardDir = new File(getDBRevsDir(), String.valueOf(revision/myMaxFilesPerDirectory));
            revisionFile = new File(shardDir, String.valueOf(revision));
        } else {
            revisionFile = new File(getDBRevsDir(), String.valueOf(revision));
        }
        return revisionFile;
    }

    protected File getMinUnpackedRevFile() {
        if (myMinUnpackedRevFile == null) {
            myMinUnpackedRevFile = new File(getDBRoot(), MIN_UNPACKED_REV_FILE);
        }
        return myMinUnpackedRevFile;
    }

    protected File getTransactionCurrentFile(){
        if(myTransactionCurrentFile == null){
            myTransactionCurrentFile = new File(getDBRoot(), TXN_CURRENT_FILE);
        }
        return myTransactionCurrentFile;
    }

    protected File getTransactionCurrentLockFile(){
        if(myTransactionCurrentLockFile == null){
            myTransactionCurrentLockFile = new File(getDBRoot(), TXN_CURRENT_LOCK_FILE);
        }
        return myTransactionCurrentLockFile;
    }

    protected File getConfigFile() {
        return new File(getDBRoot(), PATH_CONFIG);
    }

    protected void writeCurrentFile(long revision, String nextNodeID, String nextCopyID) throws SVNException, IOException {
        String line = null;
        if (getDBFormat() >= FSFS.MIN_NO_GLOBAL_IDS_FORMAT) {
            line = revision + "\n";
        } else {
            line = revision + " " + nextNodeID + " " + nextCopyID + "\n";
        }

        File currentFile = getCurrentFile();
        File tmpCurrentFile = SVNFileUtil.createUniqueFile(currentFile.getParentFile(), currentFile.getName(),
                ".tmp", false);
        OutputStream currentOS = null;

        try {
            currentOS = SVNFileUtil.openFileForWriting(tmpCurrentFile);
            currentOS.write(line.getBytes("US-ASCII"));
        } finally {
            SVNFileUtil.closeFile(currentOS);
        }
        SVNFileUtil.rename(tmpCurrentFile, currentFile);
    }

    protected long getPackedOffset(long revision) throws SVNException {
        //TODO: later on introduce invoking memcache here to fetch\store the requested data
        //long shard = revision / myMaxFilesPerDirectory;
        File manifestFile = getPackedRevPath(revision, PACK_KIND_MANIFEST);
        BufferedReader reader = null;
        LinkedList manifest = new LinkedList();
        try {
            reader = new BufferedReader(new InputStreamReader(SVNFileUtil.openFileForReading(manifestFile)));
            String line = null;
            while ((line = reader.readLine()) != null) {
                Long offset = null;
                try {
                    offset = Long.valueOf(line);
                } catch (NumberFormatException nfe) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT);
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
                manifest.add(offset);
            }
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(reader);
        }

        Long revOffsetLong = (Long) manifest.get((int) (revision % myMaxFilesPerDirectory));
        SVNErrorManager.assertionFailure(revOffsetLong != null, "offset for revision " + String.valueOf(revision) + " is null", SVNLogType.FSFS);
        return revOffsetLong.longValue();
    }

    private SVNConfigFile loadConfig() {
        File confFile = getConfigFile();
        if (myDBFormat < MIN_REP_SHARING_FORMAT || !confFile.exists()) {
            return null;
        }
        myConfig = new SVNConfigFile(confFile);
        return myConfig;
    }

    private void ensureRevisionsExists(long revision) throws SVNException {
        if (FSRepository.isInvalidRevision(revision)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION,
                    "Invalid revision number ''{0}''", new Long(revision));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (revision <= myYoungestRevisionCache) {
            return;
        }

        getYoungestRevision();

        if (revision <= myYoungestRevisionCache) {
            return;
        }

        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION,
                "No such revision {0}", String.valueOf(revision));
        SVNErrorManager.error(err, SVNLogType.FSFS);
    }

    private SVNProperties getNodeOriginsFromFile(String nodeID) throws SVNException {
        File nodeOriginFile = getNodeOriginFile(nodeID);
        if (!nodeOriginFile.exists()) {
            return null;
        }
        FSFile reader = new FSFile(nodeOriginFile);
        return reader.readProperties(false, true);
    }

    private void unlock(String path, String token, String username, boolean breakLock) throws SVNException {
        SVNLock lock = getLock(path, true, true);
        if (!breakLock) {
            if (token == null || !token.equals(lock.getID())) {
                SVNErrorManager.error(FSErrors.errorNoSuchLock(lock.getPath(), this), SVNLogType.FSFS);
            }
            if (username == null || "".equals(username)) {
                SVNErrorManager.error(FSErrors.errorNoUser(this), SVNLogType.FSFS);
            }
            if (!username.equals(lock.getOwner())) {
                SVNErrorManager.error(FSErrors.errorLockOwnerMismatch(username, lock.getOwner(), this), SVNLogType.FSFS);
            }
        }
        deleteLock(lock);
    }

    private SVNLock lock(String path, String token, String username, String comment, Date expirationDate, long currentRevision,
            boolean stealLock, boolean isDAVComment) throws SVNException {
        long youngestRev = getYoungestRevision();
        FSRevisionRoot root = createRevisionRoot(youngestRev);
        SVNNodeKind kind = root.checkNodeKind(path);
        
        if (token != null) {
            if (!token.startsWith("opaquelocktoken:")) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_BAD_LOCK_TOKEN, 
                        "Lock token URI ''{0}'' has bad scheme; expected ''opaquelocktoken''", token);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            for (int i = 0; i < token.length(); i++) {
                if (token.charAt(i) > 255) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_BAD_LOCK_TOKEN, 
                            "Lock token ''{0}'' is not ASCII at byte ''{1}''", new Object[] {token, new Integer(i)});
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
            }
            if (!SVNEncodingUtil.isXMLSafe(token)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_BAD_LOCK_TOKEN, 
                        "Lock token URI ''{0}'' is not XML-safe", token);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }

        if (kind == SVNNodeKind.DIR) {
            SVNErrorManager.error(FSErrors.errorNotFile(path, this), SVNLogType.FSFS);
        } else if (kind == SVNNodeKind.NONE) {
            if (currentRevision >= 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_OUT_OF_DATE, "Path ''{0}'' doesn''t exist in HEAD revision", path);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "Path ''{0}'' doesn''t exist in HEAD revision", path);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }

        if (username == null || "".equals(username)) {
            SVNErrorManager.error(FSErrors.errorNoUser(this), SVNLogType.FSFS);
        }

        if (FSRepository.isValidRevision(currentRevision)) {
            FSRevisionNode node = root.getRevisionNode(path);
            long createdRev = node.getCreatedRevision();
            if (FSRepository.isInvalidRevision(createdRev)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_OUT_OF_DATE, "Path ''{0}'' doesn''t exist in HEAD revision", path);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            if (currentRevision < createdRev) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_OUT_OF_DATE, "Lock failed: newer version of ''{0}'' exists", path);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }

        SVNLock existingLock = getLockHelper(path, true);

        if (existingLock != null) {
            if (!stealLock) {
                SVNErrorManager.error(FSErrors.errorPathAlreadyLocked(existingLock.getPath(), existingLock.getOwner(), this), SVNLogType.FSFS);
            } else {
                deleteLock(existingLock);
            }
        }

        SVNLock lock = null;
        if (token == null) {
            token = FSRepositoryUtil.generateLockToken();
            lock = new FSLock(path, token, username, comment, new Date(System.currentTimeMillis()), expirationDate, isDAVComment);
        } else {
            lock = new FSLock(path, token, username, comment, new Date(System.currentTimeMillis()), expirationDate, isDAVComment);
        }

        setLock(lock, isDAVComment);
        return lock;
    }

    private void setLock(SVNLock lock, boolean isDAVComment) throws SVNException {
        if (lock == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "FATAL error: attempted to set a null lock");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String lastChild = "";
        String path = lock.getPath();
        Collection children = new ArrayList();
        while (true) {
            String digestFileName = getDigestFromRepositoryPath(path);
            SVNLock fetchedLock = fetchLockFromDigestFile(null, path, children);

            if (lock != null) {
                fetchedLock = lock;
                lock = null;
                lastChild = digestFileName;
            } else {
                if (!children.isEmpty() && children.contains(lastChild)) {
                    break;
                }
                children.add(lastChild);
            }

            writeDigestLockFile(fetchedLock, children, path, isDAVComment);

            if ("/".equals(path)) {
                break;
            }
            path = SVNPathUtil.removeTail(path);

            if ("".equals(path)) {
                path = "/";
            }
            children.clear();
        }
    }

    private boolean ensureDirExists(File dir, boolean create) {
        if (!dir.exists() && create) {
            return dir.mkdirs();
        } else if (!dir.exists()) {
            return false;
        }
        return true;
    }

    private void writeDigestLockFile(SVNLock lock, Collection children, String repositoryPath, boolean isDAVComment) throws SVNException {
        if (!ensureDirExists(getDBLocksDir(), true)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                    "Can''t create a directory at ''{0}''", getDBLocksDir());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        File digestLockFile = getDigestFileFromRepositoryPath(repositoryPath);
        String digest = getDigestFromRepositoryPath(repositoryPath);
        File lockDigestSubdir = new File(getDBLocksDir(), digest.substring(0, FSFS.DIGEST_SUBDIR_LEN));

        if (!ensureDirExists(lockDigestSubdir, true)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Can't create a directory at ''{0}''", lockDigestSubdir);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        SVNProperties props = new SVNProperties();

        if (lock != null) {
            props.put(FSFS.PATH_LOCK_KEY, lock.getPath());
            props.put(FSFS.OWNER_LOCK_KEY, lock.getOwner());
            props.put(FSFS.TOKEN_LOCK_KEY, lock.getID());
            String isDAVCommentValue = isDAVComment ? "1" : "0";
            props.put(FSFS.IS_DAV_COMMENT_LOCK_KEY, isDAVCommentValue);
            if (lock.getComment() != null) {
                props.put(FSFS.COMMENT_LOCK_KEY, lock.getComment());
            }
            if (lock.getCreationDate() != null) {
                props.put(FSFS.CREATION_DATE_LOCK_KEY, SVNDate.formatDate(lock.getCreationDate()));
            }
            if (lock.getExpirationDate() != null) {
                props.put(FSFS.EXPIRATION_DATE_LOCK_KEY, SVNDate.formatDate(lock.getExpirationDate()));
            }
        }
        if (children != null && children.size() > 0) {
            Object[] digests = children.toArray();
            StringBuffer value = new StringBuffer();
            for (int i = 0; i < digests.length; i++) {
                value.append(digests[i]);
                value.append('\n');
            }
            props.put(FSFS.CHILDREN_LOCK_KEY, value.toString());
        }
        try {
            SVNWCProperties.setProperties(props, digestLockFile, SVNFileUtil.createUniqueFile(digestLockFile.getParentFile(), digestLockFile.getName(), ".tmp", false), SVNWCProperties.SVN_HASH_TERMINATOR);
        } catch (SVNException svne) {
            SVNErrorMessage err = svne.getErrorMessage().wrap("Cannot write lock/entries hashfile ''{0}''", digestLockFile);
            SVNErrorManager.error(err, svne, SVNLogType.FSFS);
        }
    }

    private FSFile openAndSeekTransaction(FSRepresentation rep) {
        FSFile file = getTransactionRevisionPrototypeFile(rep.getTxnId());
        file.seek(rep.getOffset());
        return file;
    }

    private FSFile openAndSeekRevision(long revision, long offset) throws SVNException {
        ensureRevisionsExists(revision);
        FSFile file = getPackOrRevisionFSFile(revision);
        if (isPackedRevision(revision)) {
            long revOffset = getPackedOffset(revision);
            offset += revOffset;
        }
        file.seek(offset);
        return file;
    }

    private Map parsePlainRepresentation(SVNProperties entries, boolean mayContainNulls) throws SVNException {
        Map representationMap = new SVNHashMap();

        for (Iterator iterator = entries.nameSet().iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            String unparsedEntry = entries.getStringValue(name);

            if (unparsedEntry == null && mayContainNulls) {
                continue;
            }

            FSEntry nextRepEntry = parseRepEntryValue(name, unparsedEntry);
            if (nextRepEntry == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Directory entry corrupt");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            representationMap.put(name, nextRepEntry);
        }
        return representationMap;
    }

    private FSEntry parseRepEntryValue(String name, String value) {
        if (value == null) {
            return null;
        }
        int spaceInd = value.indexOf(' ');
        if (spaceInd == -1) {
            return null;
        }
        String kind = value.substring(0, spaceInd);
        String rawID = value.substring(spaceInd + 1);

        SVNNodeKind type = SVNNodeKind.parseKind(kind);
        FSID id = FSID.fromString(rawID);
        if ((type != SVNNodeKind.DIR && type != SVNNodeKind.FILE) || id == null) {
            return null;
        }
        return new FSEntry(id, type, name);
    }

    private Date getRevisionTime(long revision) throws SVNException {
        SVNProperties revisionProperties = getRevisionProperties(revision);
        String timeString = revisionProperties.getStringValue(SVNRevisionProperty.DATE);
        if (timeString == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_GENERAL, "Failed to find time on revision {0}", new Long(revision));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return SVNDate.parseDateString(timeString);
    }

    private static boolean isRepositoryRoot(File candidatePath) {
        File formatFile = new File(candidatePath, REPOS_FORMAT_FILE);
        SVNFileType fileType = SVNFileType.getType(formatFile);
        if (fileType != SVNFileType.FILE) {
            return false;
        }
        File dbFile = new File(candidatePath, DB_DIR);
        fileType = SVNFileType.getType(dbFile);
        if (fileType != SVNFileType.DIRECTORY && fileType != SVNFileType.SYMLINK) {
            return false;
        }
        return true;
    }

    public File getRevisionPropertiesDbPath() {
        return SVNFileUtil.createFilePath(getRevisionPropertiesRoot(), REVISION_PROPERTIES_DB);
    }

    public File getMinUnpackedRevPropPath() {
        return SVNFileUtil.createFilePath(getDBRoot(), MIN_UNPACKED_REVPROP);
    }

    public void updateMinUnpackedRevProp() throws SVNException {
        assert(myDBFormat >= MIN_PACKED_REVPROP_FORMAT);
        myMinUnpackedRevProp = getMinUnpackedRevProp();
    }

    public long getMinUnpackedRevProp() throws SVNException {
        FSFile file = new FSFile(getMinUnpackedRevPropPath());
        try {
            return file.readLong();
        } catch (NumberFormatException nfe) {
            return 0;
        } finally {
            file.close();
        }
    }

    public SVNSqlJetDb getRevisionProperitesDb() {
        return myRevisionProperitesDb;
    }

}
