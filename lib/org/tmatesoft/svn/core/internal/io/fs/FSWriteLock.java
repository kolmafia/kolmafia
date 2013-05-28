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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSWriteLock {

    private static final Map ourThreadDBLocksCache = new SVNHashMap();
    private static final Map ourThreadRepositoryLocksCache = new SVNHashMap();
    private static final Map ourThreadLogLocksCache = new SVNHashMap();

    private static final int DB_LOCK = 1;
    private static final int LOGS_LOCK = 2;
    private static final int REPOS_LOCK = 3;
    
    private File myLockFile;
    private RandomAccessFile myLockRAFile;
    private FileLock myLock;
    private String myToken;
    private int myLockType;
    private boolean myIsShared;
    private int myReferencesCount = 0;

    private FSWriteLock(String token, File lockFile, int lockType, boolean shared) {
        myToken = token;
        myLockFile = lockFile;
        myLockType = lockType;
        myIsShared = shared;
    }

    public static synchronized FSWriteLock getWriteLockForDB(FSFS owner) throws SVNException {
        String uuid = owner.getUUID();
        FSWriteLock lock = (FSWriteLock) ourThreadDBLocksCache.get(uuid);
        if (lock == null) {
            lock = new FSWriteLock(uuid, owner.getWriteLockFile(), DB_LOCK, false);
            ourThreadDBLocksCache.put(uuid, lock);
        }
        lock.myReferencesCount++;
        return lock;
    }

    public static synchronized FSWriteLock getWriteLockForCurrentTxn(String token, FSFS owner) throws SVNException {
        if (token == null || token.length() == 0){
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.FS_NO_LOCK_TOKEN, "Incorrect lock token for current transaction"), SVNLogType.FSFS);
        }
        String uuid = owner.getUUID() + token;
        FSWriteLock lock = (FSWriteLock) ourThreadDBLocksCache.get(uuid);
        if (lock == null) {
            lock = new FSWriteLock(uuid, owner.getTransactionCurrentLockFile(), DB_LOCK, false);
            ourThreadDBLocksCache.put(uuid, lock);
        }
        lock.myReferencesCount++;
        return lock;
    }

    public static synchronized FSWriteLock getWriteLockForTxn(String txnID, FSFS owner) throws SVNException {
        if (txnID == null || txnID.length() == 0){
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.FS_NO_LOCK_TOKEN, "Incorrect txn id while locking"), SVNLogType.FSFS);
        }
        String uuid = owner.getUUID() + txnID;
        FSWriteLock lock = (FSWriteLock) ourThreadDBLocksCache.get(uuid);
        if (lock == null) {
            lock = new FSWriteLock(uuid, owner.getTransactionProtoRevLockFile(txnID), DB_LOCK, false);
            ourThreadDBLocksCache.put(uuid, lock);
        }
        lock.myReferencesCount++;
        return lock;
    }

    public static synchronized FSWriteLock getDBLogsLock(FSFS owner, boolean exclusive) throws SVNException {
        String uuid = owner.getUUID();
        FSWriteLock lock = (FSWriteLock) ourThreadLogLocksCache.get(uuid);
        if (lock == null) {
            lock = new FSWriteLock(uuid, owner.getDBLogsLockFile(), LOGS_LOCK, !exclusive);
            ourThreadLogLocksCache.put(uuid, lock);
        }
        lock.myReferencesCount++;
        return lock;
    }

    public synchronized void lock() throws SVNException {
        boolean errorOccured = false;
        Exception childError = null;
        if (myLock != null) {
            errorOccured = true;
        }
        try {
            SVNFileType type = SVNFileType.getType(myLockFile);
            if (type == SVNFileType.UNKNOWN || type == SVNFileType.NONE) {
                SVNFileUtil.createEmptyFile(myLockFile);
            }
            myLockRAFile = new RandomAccessFile(myLockFile, "rw");
            myLock = myLockRAFile.getChannel().lock(0L, Long.MAX_VALUE, myIsShared);
        } catch (IOException ioe) {
            unlock();
            errorOccured = true;
            childError = ioe;
        }
        if (errorOccured) {
            String msg = childError == null ? "file already locked" : childError.getMessage();
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR,
                    "Can''t get exclusive lock on file ''{0}'': {1}", new Object[]{ myLockFile, msg });
            SVNErrorManager.error(err, childError, SVNLogType.FSFS);
        }
    }

    public static synchronized void release(FSWriteLock lock) {
        if (lock == null) {
            return;
        }
        if ((--lock.myReferencesCount) == 0) {
            if (lock.myLockType == DB_LOCK) {
                ourThreadDBLocksCache.remove(lock.myToken);
            } else if (lock.myLockType == REPOS_LOCK) {
                ourThreadRepositoryLocksCache.remove(lock.myToken);
            } else if (lock.myLockType == LOGS_LOCK) {
                ourThreadLogLocksCache.remove(lock.myToken);
            }
        }
    }

    public synchronized void unlock() throws SVNException {
        if (myLock != null) {
            try {
                myLock.release();
            } catch (IOException ioex) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, 
                        "Unexpected error while releasing file lock on ''{0}''", myLockFile);
                SVNErrorManager.error(error, ioex, SVNLogType.FSFS);
            }
            myLock = null;
        }
        SVNFileUtil.closeFile(myLockRAFile);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{[");
        buffer.append("File:");
        buffer.append(myLockFile);
        buffer.append("][");
        buffer.append("RefCount:");
        buffer.append(myReferencesCount);
        buffer.append("][");
        buffer.append("Token:");
        buffer.append(myToken);
        buffer.append("]}");
        return buffer.toString();
    }
}
