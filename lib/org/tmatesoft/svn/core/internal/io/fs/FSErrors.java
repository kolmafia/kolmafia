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

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNNodeKind;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSErrors {

    public static SVNErrorMessage errorDanglingId(FSID id, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_ID_NOT_FOUND, "Reference to non-existent node ''{0}'' in filesystem ''{1}''", new Object[] {
                id, fsDir
        });
        return err;
    }

    public static SVNErrorMessage errorTxnNotMutable(String txnId, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_TRANSACTION_NOT_MUTABLE, "Cannot modify transaction named ''{0}'' in filesystem ''{1}''", new Object[] {
                txnId, fsDir
        });
        return err;
    }

    public static SVNErrorMessage errorNotMutable(long revision, String path, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "File is not mutable: filesystem ''{0}'', revision {1}, path ''{2}''", new Object[] {
                fsDir, new Long(revision), path
        });
        return err;
    }

    public static SVNErrorMessage errorNotFound(FSRoot root, String path) {
        SVNErrorMessage err;
        if (root instanceof FSTransactionRoot) {
            FSTransactionRoot txnRoot = (FSTransactionRoot) root;
            err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "File not found: transaction ''{0}'', path ''{1}''", new Object[] {
                    txnRoot.getTxnID(), path
            });
        } else {
            FSRevisionRoot revRoot = (FSRevisionRoot) root;
            err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "File not found: revision {0}, path ''{1}''", new Object[] {
                    new Long(revRoot.getRevision()), path
            });
        }
        return err;
    }

    public static SVNErrorMessage errorNotDirectory(String path, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_DIRECTORY, "''{0}'' is not a directory in filesystem ''{1}''", new Object[] {
                path, fsDir
        });
        return err;
    }

    public static SVNErrorMessage errorCorruptLockFile(String path, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Corrupt lockfile for path ''{0}'' in filesystem ''{1}''", new Object[] {
                path, fsDir
        });
        return err;
    }

    public static SVNErrorMessage errorOutOfDate(String path, SVNNodeKind kind) {
        if ("/".equals(path)) {
            path = "";
        }
        
        SVNErrorMessage err = null;
        if (kind == SVNNodeKind.DIR) {
            err = SVNErrorMessage.create(SVNErrorCode.FS_TXN_OUT_OF_DATE, 
                    "Directory ''{0}'' is out of date", path); 
        } else {
            err = SVNErrorMessage.create(SVNErrorCode.FS_TXN_OUT_OF_DATE, 
                    "File ''{0}'' is out of date", path);
        }
        return err;
    }

    public static SVNErrorMessage errorAlreadyExists(FSRoot root, String path, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = null;
        if (root instanceof FSTransactionRoot) {
            FSTransactionRoot txnRoot = (FSTransactionRoot) root;
            err = SVNErrorMessage.create(SVNErrorCode.FS_ALREADY_EXISTS, "File already exists: filesystem ''{0}'', transaction ''{1}'', path ''{2}''", new Object[] {
                    fsDir, txnRoot.getTxnID(), path
            });
        } else {
            FSRevisionRoot revRoot = (FSRevisionRoot) root;
            err = SVNErrorMessage.create(SVNErrorCode.FS_ALREADY_EXISTS, "File already exists: filesystem ''{0}'', revision {1}, path ''{2}''", new Object[] {
                    fsDir, new Long(revRoot.getRevision()), path
            });
        }
        return err;
    }

    public static SVNErrorMessage errorNotTxn() {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_TXN_ROOT, "Root object must be a transaction root");
        return err;
    }

    public static SVNErrorMessage errorConflict(String path, StringBuffer conflictPath) {
        if (conflictPath != null) {
            conflictPath.delete(0, conflictPath.length());
            conflictPath.append(path);
        }
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CONFLICT, "Conflict at ''{0}''", path);
        return err;
    }

    public static SVNErrorMessage errorNoSuchLock(String path, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_LOCK, "No lock on path ''{0}'' in filesystem ''{1}''", new Object[] {
                path, fsDir
        });
        return err;
    }

    public static SVNErrorMessage errorLockExpired(String lockToken, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_LOCK_EXPIRED, "Lock has expired:  lock-token ''{0}'' in filesystem ''{1}''", new Object[] {
                lockToken, fsDir
        });
        return err;
    }

    public static SVNErrorMessage errorNoUser(FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_USER, "No username is currently associated with filesystem ''{0}''", fsDir);
        return err;
    }

    public static SVNErrorMessage errorLockOwnerMismatch(String username, String lockOwner, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_LOCK_OWNER_MISMATCH, "User ''{0}'' is trying to use a lock owned by ''{1}'' in filesystem ''{2}''", new Object[] {
                username, lockOwner, fsDir
        });
        return err;
    }

    public static SVNErrorMessage errorNotFile(String path, FSFS owner) {
        File fsDir = owner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FILE, "''{0}'' is not a file in filesystem ''{1}''", new Object[] {
                path, fsDir
        });
        return err;
    }

    public static SVNErrorMessage errorPathAlreadyLocked(String path, String owner, FSFS fsfsOwner) {
        File fsDir = fsfsOwner.getDBRoot();
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_PATH_ALREADY_LOCKED, "Path ''{0}'' is already locked by user ''{1}'' in filesystem ''{2}''", new Object[] {
                path, owner, fsDir
        });
        return err;
    }

    public static boolean isLockError(SVNErrorMessage err) {
        if (err == null) {
            return false;
        }
        SVNErrorCode errCode = err.getErrorCode();
        return errCode == SVNErrorCode.FS_PATH_ALREADY_LOCKED 
                || errCode == SVNErrorCode.FS_NOT_FOUND
                || errCode == SVNErrorCode.FS_BAD_LOCK_TOKEN
                || errCode == SVNErrorCode.FS_OUT_OF_DATE;
    }

    public static boolean isUnlockError(SVNErrorMessage err) {
        if (err == null) {
            return false;
        }
        SVNErrorCode errCode = err.getErrorCode();
        return errCode == SVNErrorCode.FS_PATH_NOT_LOCKED || errCode == SVNErrorCode.FS_BAD_LOCK_TOKEN || errCode == SVNErrorCode.FS_LOCK_OWNER_MISMATCH || errCode == SVNErrorCode.FS_NO_SUCH_LOCK
                || errCode == SVNErrorCode.RA_NOT_LOCKED || errCode == SVNErrorCode.FS_LOCK_EXPIRED;
    }

}
