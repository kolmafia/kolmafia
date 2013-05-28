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
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSHotCopier {

    public void runHotCopy(FSFS srcOwner, File dstPath) throws SVNException {
        FSWriteLock dbLogsLock = FSWriteLock.getDBLogsLock(srcOwner, false);
        File srcPath = srcOwner.getRepositoryRoot();
        synchronized (dbLogsLock) {
            try {
                dbLogsLock.lock();
                createRepositoryLayout(srcPath, dstPath);
                File dstReposLocksDir = new File(dstPath, FSFS.LOCKS_DIR);
                try {
                    createReposDir(dstReposLocksDir);
                } catch (SVNException svne) {
                    SVNErrorMessage err = svne.getErrorMessage().wrap("Creating lock dir");
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
                createDBLock(dstReposLocksDir);
                createDBLogsLock(dstReposLocksDir);
                File dstDBDir = new File(dstPath, FSFS.DB_DIR);
                dstDBDir.mkdirs();
                SVNFileUtil.setSGID(dstDBDir);
                FSFS dstOwner = new FSFS(dstPath);
                String fsType = srcOwner.getFSType();
                hotCopy(srcOwner, dstOwner);
                writeFSType(dstOwner, fsType);
                SVNFileUtil.writeVersionFile(new File(dstPath, FSFS.REPOS_FORMAT_FILE),
                        srcOwner.getReposFormat());
            } finally {
                dbLogsLock.unlock();
                FSWriteLock.release(dbLogsLock);
            }
        }
    }

    private void writeFSType(FSFS dstOwner, String fsType) throws SVNException {
        OutputStream fsTypeStream = null;
        try {
            fsTypeStream = SVNFileUtil.openFileForWriting(dstOwner.getFSTypeFile());
            fsType += '\n';
            fsTypeStream.write(fsType.getBytes("US-ASCII"));
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(fsTypeStream);
        }
    }

    private void createRepositoryLayout(File srcPath, File dstPath) throws SVNException {
        File[] children = srcPath.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            String childName = child.getName();
            if (childName.equals(FSFS.DB_DIR) || childName.equals(FSFS.LOCKS_DIR) ||
                    childName.equals(FSFS.REPOS_FORMAT_FILE)) {
                continue;
            }

            File dstChildPath = new File(dstPath, childName);
            if (child.isDirectory()) {
                createReposDir(dstChildPath);
                createRepositoryLayout(child, dstChildPath);
            } else if (child.isFile()) {
                SVNFileUtil.copyFile(child, dstChildPath, true);
            }
        }
    }

    private void createReposDir(File dir) throws SVNException {
        if (dir.exists()) {
            File[] dstChildren = dir.listFiles();
            if (dstChildren.length > 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.DIR_NOT_EMPTY,
                        "''{0}'' exists and is non-empty", dir);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        } else {
            dir.mkdirs();
        }
    }

    private void createDBLock(File dstPath) throws SVNException {
        try {
            SVNFileUtil.createFile(new File(dstPath, FSFS.DB_LOCK_FILE), FSFS.PRE_12_COMPAT_UNNEEDED_FILE_CONTENTS,
            "US-ASCII");
        } catch (SVNException svne) {
            SVNErrorMessage err = svne.getErrorMessage().wrap("Creating db lock file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
    }

    private void createDBLogsLock(File dstPath) throws SVNException {
        try {
            SVNFileUtil.createFile(new File(dstPath, FSFS.DB_LOGS_LOCK_FILE), FSFS.PRE_12_COMPAT_UNNEEDED_FILE_CONTENTS,
            "US-ASCII");
        } catch (SVNException svne) {
            SVNErrorMessage err = svne.getErrorMessage().wrap("Creating db logs lock file");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
    }

    private void hotCopy(FSFS srcOwner, FSFS dstOwner) throws SVNException {
        int format = srcOwner.readDBFormat();
        FSRepositoryUtil.checkReposDBFormat(format);
        SVNFileUtil.copyFile(srcOwner.getCurrentFile(), dstOwner.getCurrentFile(), true);
        SVNFileUtil.copyFile(srcOwner.getUUIDFile(), dstOwner.getUUIDFile(), true);

        long minUnpackedRevision = 0;
        if (format >= FSFS.MIN_PACKED_FORMAT) {
            SVNFileUtil.copyFile(srcOwner.getMinUnpackedRevFile(), dstOwner.getMinUnpackedRevFile(), true);
            minUnpackedRevision = srcOwner.getMinUnpackedRev();
        }
        long youngestRev = dstOwner.getYoungestRevision();

        File dstRevsDir = dstOwner.getDBRevsDir();
        dstRevsDir.mkdirs();

        long maxFilesPerDirectory = srcOwner.getMaxFilesPerDirectory();
        long rev = 0;
        for (; rev < minUnpackedRevision; rev += srcOwner.getMaxFilesPerDirectory()) {
            long packedShard = rev / maxFilesPerDirectory;
            SVNFileUtil.copyDirectory(srcOwner.getPackDir(packedShard), dstOwner.getPackDir(packedShard), false, null);
        }

        SVNErrorManager.assertionFailure(rev == minUnpackedRevision, "expected minimal unpacked revision " + String.valueOf(minUnpackedRevision) + ", but real revision is " +
                String.valueOf(rev), SVNLogType.FSFS);

        for (; rev <= youngestRev; rev++) {
            File dstDir = dstRevsDir;
            if (maxFilesPerDirectory > 0) {
                String shard = String.valueOf(rev / maxFilesPerDirectory);
                dstDir = new File(dstRevsDir, shard);
            }
            SVNFileUtil.copyFile(srcOwner.getRevisionFile(rev), new File(dstDir, String.valueOf(rev)), true);
        }

        long min_unpacked_revprop = 0;
        /* Copy the min unpacked revprop file, and read its value. */
        if (format >= FSFS.MIN_PACKED_REVPROP_FORMAT)
          {
            min_unpacked_revprop = srcOwner.getMinUnpackedRevProp();
            SVNFileUtil.copyFile(srcOwner.getMinUnpackedRevPropPath(), dstOwner.getMinUnpackedRevPropPath(),true);
            final File srcRevPropDb = srcOwner.getRevisionPropertiesDbPath();
            final File dstRevPropDb = dstOwner.getRevisionPropertiesDbPath();
            final SVNSqlJetDb revPropDb = SVNSqlJetDb.open(
                    srcOwner.getRevisionPropertiesDbPath(), SVNSqlJetDb.Mode.ReadWrite);
            try{
                SVNException e = (SVNException) revPropDb.getDb().runReadTransaction(new ISqlJetTransaction() {
                        public Object run(SqlJetDb db) throws SqlJetException {
                            try {
                                SVNFileUtil.copyFile(srcRevPropDb,
                                        dstRevPropDb,true);
                            } catch (SVNException e) {
                                return e;
                            }
                            return null;
                        }
                    });
                if(e!=null){
                    throw e;
                }
            } catch (SqlJetException e) {
                SVNErrorMessage err = SVNErrorMessage.create( SVNErrorCode.SQLITE_ERROR, e );
                SVNErrorManager.error(err, SVNLogType.FSFS);
            } finally {
                revPropDb.close();
            }
          }

        File dstRevPropsDir = dstOwner.getRevisionPropertiesRoot();
        for (rev = min_unpacked_revprop; rev <= youngestRev; rev++) {
            File dstDir = dstRevPropsDir;
            if (maxFilesPerDirectory > 0) {
                String shard = String.valueOf(rev / maxFilesPerDirectory);
                dstDir = new File(dstRevPropsDir, shard);
            }
            SVNFileUtil.copyFile(srcOwner.getRevisionPropertiesFile(rev, false), new File(dstDir, String.valueOf(rev)),
                    true);
        }

        dstOwner.getTransactionsParentDir().mkdirs();
        if (format >= FSFS.MIN_PROTOREVS_DIR_FORMAT) {
            dstOwner.getTransactionProtoRevsDir().mkdirs();
        }

        File srcLocksDir = srcOwner.getDBLocksDir();
        if (srcLocksDir.exists()) {
            SVNFileUtil.copyDirectory(srcLocksDir, dstOwner.getDBLocksDir(), false, null);
        }

        File srcNodeOriginsDir = srcOwner.getNodeOriginsDir();
        if (srcNodeOriginsDir.exists()) {
            SVNFileUtil.copyDirectory(srcNodeOriginsDir, dstOwner.getNodeOriginsDir(), false, null);
        }

        if (format >= FSFS.MIN_CURRENT_TXN_FORMAT) {
            SVNFileUtil.copyFile(srcOwner.getTransactionCurrentFile(), dstOwner.getTransactionCurrentFile(), true);
        }
        dstOwner.writeDBFormat(format, maxFilesPerDirectory, false);
    }
}
