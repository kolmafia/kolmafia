/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs.repcache;

import java.io.File;

import org.tmatesoft.sqljet.core.SqlJetErrorCode;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.internal.SqlJetSafetyLevel;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetRunnableWithLock;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.FSFS;
import org.tmatesoft.svn.core.internal.io.fs.FSRepresentation;
import org.tmatesoft.svn.core.internal.io.fs.IFSRepresentationCacheManager;
import org.tmatesoft.svn.core.internal.io.fs.IFSSqlJetTransaction;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRepresentationCacheManager implements IFSRepresentationCacheManager {
    
    public static final String REP_CACHE_TABLE = "rep_cache";
    private static final int REP_CACHE_DB_FORMAT =  1;
    private static final String REP_CACHE_DB_SQL =  "create table rep_cache (hash text not null primary key, " +
                                                    "                        revision integer not null, " + 
                                                    "                        offset integer not null, " + 
                                                    "                        size integer not null, " +
                                                    "                        expanded_size integer not null); ";

    private SqlJetDb myRepCacheDB;
    private ISqlJetTable myTable;
    private FSFS myFSFS;
    
    public static IFSRepresentationCacheManager openRepresentationCache(FSFS fsfs) throws SVNException {
        final FSRepresentationCacheManager cacheObj = new FSRepresentationCacheManager();
        try {
            cacheObj.myRepCacheDB = SqlJetDb.open(fsfs.getRepositoryCacheFile(), true);
            cacheObj.myRepCacheDB.setSafetyLevel(SqlJetSafetyLevel.OFF);
            
            checkFormat(cacheObj.myRepCacheDB);
            cacheObj.myTable = cacheObj.myRepCacheDB.getTable(REP_CACHE_TABLE);
        } catch (SqlJetException e) {
            SVNDebugLog.getDefaultLog().logError(SVNLogType.FSFS, e);
            return new FSEmptyRepresentationCacheManager();
            
        }
        return cacheObj;
    }
    
    public static void createRepresentationCache(File path) throws SVNException {
        SqlJetDb db = null;
        try {
            db = SqlJetDb.open(path, true);
            checkFormat(db);
        } catch (SqlJetException e) {
            SVNDebugLog.getDefaultLog().logError(SVNLogType.FSFS, e);
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (SqlJetException e) {
                    SVNDebugLog.getDefaultLog().logFine(SVNLogType.FSFS, e);
                }
            }
        }
    }

    private static void checkFormat(final SqlJetDb db) throws SqlJetException {
        db.runWithLock(new ISqlJetRunnableWithLock() {
            public Object runWithLock(SqlJetDb db) throws SqlJetException {
                int version = db.getOptions().getUserVersion();
                if (version < REP_CACHE_DB_FORMAT) {
                    db.getOptions().setAutovacuum(true);
                    db.runWriteTransaction(new ISqlJetTransaction() {
                        public Object run(SqlJetDb db) throws SqlJetException {
                            db.getOptions().setUserVersion(REP_CACHE_DB_FORMAT);
                            db.createTable(FSRepresentationCacheManager.REP_CACHE_DB_SQL);
                            return null;
                        }
                    });
                } else if (version > REP_CACHE_DB_FORMAT) {
                    throw new SqlJetException("Schema format " + version + " not recognized");   
                }
                return null;
            }
        });
    }
    
    public void insert(final FSRepresentation representation, boolean rejectDup) throws SVNException {
        if (representation.getSHA1HexDigest() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_CHECKSUM_KIND, 
                    "Only SHA1 checksums can be used as keys in the rep_cache table.\n");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        FSRepresentation oldRep = getRepresentationByHash(representation.getSHA1HexDigest());
        if (oldRep != null) {
            if (rejectDup && (oldRep.getRevision() != representation.getRevision() || oldRep.getOffset() != representation.getOffset() ||
                    oldRep.getSize() != representation.getSize() || oldRep.getExpandedSize() != representation.getExpandedSize())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Representation key for checksum ''{0}'' exists in " + 
                        "filesystem ''{1}'' with a different value ({2},{3},{4},{5}) than what we were about to store ({6},{7},{8},{9})", 
                        new Object[] { representation.getSHA1HexDigest(), myFSFS.getRepositoryRoot(), String.valueOf(oldRep.getRevision()), 
                        String.valueOf(oldRep.getOffset()), String.valueOf(oldRep.getSize()), String.valueOf(oldRep.getExpandedSize()), 
                        String.valueOf(representation.getRevision()), String.valueOf(representation.getOffset()), 
                        String.valueOf(representation.getSize()), String.valueOf(representation.getExpandedSize()) });
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            return;
        }
        
        try {
            myTable.insert(new Object[] { representation.getSHA1HexDigest(), new Long(representation.getRevision()),
                    new Long(representation.getOffset()), new Long(representation.getSize()), 
                    new Long(representation.getExpandedSize()) });
        } catch (SqlJetException e) {
            SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
        }
    }

    public void close() throws SVNException {
        if (myRepCacheDB != null) {
            try {
                myRepCacheDB.close();
            } catch (SqlJetException e) {
                SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
            } finally {
                myTable = null;
                myRepCacheDB = null;
                myFSFS = null;
            }
        }
    }
    
    public FSRepresentation getRepresentationByHash(String hash) throws SVNException {
        FSRepresentationCacheRecord cache = getByHash(hash);
        if (cache != null) {
            FSRepresentation representation = new FSRepresentation();
            representation.setExpandedSize(cache.getExpandedSize());
            representation.setOffset(cache.getOffset());
            representation.setRevision(cache.getRevision());
            representation.setSize(cache.getSize());
            representation.setSHA1HexDigest(cache.getHash());
            return representation;
        }
        return null;
    }

    private FSRepresentationCacheRecord getByHash(final String hash) throws SVNException {
        ISqlJetCursor lookup = null;
        try {
            lookup = myTable.lookup(myTable.getPrimaryKeyIndexName(), new Object[] { hash });
            if (!lookup.eof()) {
                return new FSRepresentationCacheRecord(lookup);
            }
        } catch (SqlJetException e) {
            SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
        } finally {
            if (lookup != null) {
                try {
                    lookup.close();
                } catch (SqlJetException e) {
                    SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
                }
            }
        }
        return null;
    }

    private static SVNErrorMessage convertError(SqlJetException e) {
        SVNErrorMessage err = SVNErrorMessage.create(convertErrorCode(e), e.getMessage());
        return err;
    }
    
    private static SVNErrorCode convertErrorCode(SqlJetException e) {
        SqlJetErrorCode sqlCode = e.getErrorCode();
        if (sqlCode == SqlJetErrorCode.READONLY) {
            return SVNErrorCode.SQLITE_READONLY;
        } 
        return SVNErrorCode.SQLITE_ERROR;
    }

    public void runWriteTransaction(final IFSSqlJetTransaction transaction) throws SVNException {
        if (myRepCacheDB != null) {
            try {
                myRepCacheDB.runWriteTransaction(new ISqlJetTransaction() {
                    public Object run(SqlJetDb db) throws SqlJetException {
                        try {
                            transaction.run();
                        } catch (SVNException e) {
                            throw new SqlJetException(e);
                        }
                        return null;
                    }
                });
            } catch (SqlJetException e) {
                SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
            }
        }
    }

    public void runReadTransaction(final IFSSqlJetTransaction transaction) throws SVNException {
        if (myRepCacheDB != null) {
            try {
                myRepCacheDB.runReadTransaction(new ISqlJetTransaction() {
                    public Object run(SqlJetDb db) throws SqlJetException {
                        try {
                            transaction.run();
                        } catch (SVNException e) {
                            throw new SqlJetException(e);
                        }
                        return null;
                    }
                });
            } catch (SqlJetException e) {
                SVNErrorManager.error(convertError(e), SVNLogType.FSFS);
            }
        }
    }
}