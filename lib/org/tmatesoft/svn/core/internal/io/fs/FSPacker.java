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
import java.io.InputStream;
import java.io.OutputStream;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.revprop.SVNFSFSPackedRevProps;
import org.tmatesoft.svn.core.internal.io.fs.revprop.SVNFSFSPackedRevPropsManifest;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.admin.ISVNAdminEventHandler;
import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc.admin.SVNAdminEventAction;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSPacker {

    private ISVNCanceller myCanceller;
    private ISVNAdminEventHandler myNotifyHandler;

    public FSPacker(ISVNAdminEventHandler notifyHandler) {
        myCanceller = notifyHandler == null ? ISVNCanceller.NULL : notifyHandler;
        myNotifyHandler = notifyHandler;
    }

    public void pack(FSFS fsfs) throws SVNException {
        FSWriteLock writeLock = FSWriteLock.getWriteLockForDB(fsfs);
        synchronized (writeLock) {
            try {
                writeLock.lock();
                packImpl(fsfs);
            } finally {
                writeLock.unlock();
                FSWriteLock.release(writeLock);
            }
        }
    }

    private void packImpl(FSFS fsfs) throws SVNException {
        int format = fsfs.getDBFormat();
        if (format < FSFS.MIN_PACKED_FORMAT) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_UNSUPPORTED_FORMAT, "FS format too old to pack, please upgrade.");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        long maxFilesPerDirectory = fsfs.getMaxFilesPerDirectory();
        if (maxFilesPerDirectory <= 0) {
            return;
        }

        long minUnpackedRev = fsfs.getMinUnpackedRev();
        long youngestRev = fsfs.getYoungestRevision();
        long completedShards = (youngestRev + 1) / maxFilesPerDirectory;
        long minUnpackedRevProp = 0;
        boolean packRevisionProperties = fsfs.getDBFormat() >= FSFS.MIN_PACKED_REVPROP_FORMAT;
        if (packRevisionProperties)
        {
            minUnpackedRevProp =fsfs.getMinUnpackedRevProp();
        }

        if (minUnpackedRev == (completedShards * maxFilesPerDirectory) &&
           minUnpackedRevProp == (completedShards * maxFilesPerDirectory)
        ) {
            return;
        }

        for (long i = minUnpackedRev / maxFilesPerDirectory; i < completedShards; i++) {
            myCanceller.checkCancelled();
            packShard(fsfs, i, packRevisionProperties);
        }
    }

    private void packShard(FSFS fsfs, long shard, boolean packRevisionProperties) throws SVNException {
        File revShardPath = new File(fsfs.getDBRevsDir(), String.valueOf(shard));
        File revpropShardPath = new File(fsfs.getRevisionPropertiesRoot(), String.valueOf(shard));
        packRevShard(fsfs, shard, revShardPath);

        if (packRevisionProperties) {
            myCanceller.checkCancelled();
            packRevPropShard(fsfs, shard, revpropShardPath, (long)(0.9 * fsfs.getRevPropPackSize()));
        }


        File finalPath = fsfs.getMinUnpackedRevFile();
        File tmpFile = SVNFileUtil.createUniqueFile(fsfs.getDBRoot(), "tempfile", ".tmp", false);
        String line = String.valueOf((shard + 1) * fsfs.getMaxFilesPerDirectory()) + '\n';
        SVNFileUtil.writeToFile(tmpFile, line, "UTF-8");
        SVNFileUtil.rename(tmpFile, finalPath);
        SVNFileUtil.deleteAll(revShardPath, true, myCanceller);
        if (packRevisionProperties) {
            deleteRevPropShard(revpropShardPath, shard, fsfs.getMaxFilesPerDirectory());
        }

        firePackEvent(shard, false);
    }

    private void deleteRevPropShard(File revpropShardPath, long shard, long maxFilesPerDirectory) throws SVNException {
        if (shard == 0) {
            for (int i = 1; i < maxFilesPerDirectory; i++) {
                if (myCanceller != null) {
                    myCanceller.checkCancelled();
                }
                final File path = new File(revpropShardPath, String.valueOf(i));
                SVNFileUtil.deleteFile(path);
            }
        } else {
            SVNFileUtil.deleteAll(revpropShardPath, true, myCanceller);
        }
    }

    private void packRevShard(FSFS fsfs, long shard, File shardPath) throws SVNException {
        File packDir = fsfs.getPackDir(shard);
        File packFile = fsfs.getPackFile(shard);
        File manifestFile = fsfs.getManifestFile(shard);

        firePackEvent(shard, true);

        SVNFileUtil.deleteAll(packDir, false, myCanceller);

        long startRev = shard * fsfs.getMaxFilesPerDirectory();
        long endRev = (shard + 1) * fsfs.getMaxFilesPerDirectory() - 1;
        long nextOffset = 0;
        OutputStream packFileOS = null;
        OutputStream manifestFileOS = null;
        try {
            packFileOS = SVNFileUtil.openFileForWriting(packFile);
            manifestFileOS = SVNFileUtil.openFileForWriting(manifestFile);
            for (long rev = startRev; rev <= endRev; rev++) {
                File path = new File(shardPath, String.valueOf(rev));
                String line = String.valueOf(nextOffset) + '\n';
                manifestFileOS.write(line.getBytes("UTF-8"));
                nextOffset += path.length();
                InputStream revIS = null;
                try {
                    revIS = SVNFileUtil.openFileForReading(path);
                    FSRepositoryUtil.copy(revIS, packFileOS, myCanceller);
                } finally {
                    SVNFileUtil.closeFile(revIS);
                }
            }
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(packFileOS);
            SVNFileUtil.closeFile(manifestFileOS);
        }

    }

    private void firePackEvent(long shard, boolean start) throws SVNException {
        if (myNotifyHandler != null) {
            SVNAdminEvent event = new SVNAdminEvent(start ? SVNAdminEventAction.PACK_START : SVNAdminEventAction.PACK_END, shard);
            myNotifyHandler.handleAdminEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private void packRevPropShard(FSFS fsfs, long shard, File shardPath, long maxPackSize) throws SVNException {
        File packPath = new File(fsfs.getRevisionPropertiesRoot(), String.valueOf(shard) + FSFS.PACK_EXT);

        firePackEvent(shard, true);

        long startRev = shard * fsfs.getMaxFilesPerDirectory();
        long endRev = (shard + 1) * fsfs.getMaxFilesPerDirectory() - 1;
        if (startRev == 0) {
            startRev++;
        }

        long totalSize = 2 * SVNFSFSPackedRevProps.INT64_BUFFER_SIZE;
        boolean packIsEmpty = true;
        String packName = null;

        final SVNFSFSPackedRevPropsManifest.Builder manifestBuilder = new SVNFSFSPackedRevPropsManifest.Builder();

        for (long rev = startRev; rev <= endRev; rev++) {
            final File path = new File(shardPath, String.valueOf(rev));
            final long size = path.length();

            if (!packIsEmpty && totalSize + SVNFSFSPackedRevProps.INT64_BUFFER_SIZE + size > maxPackSize) {
                copyRevProps(packName, packPath, shardPath, startRev, rev-1, fsfs.isCompressPackedRevprops());
                totalSize = 2 * SVNFSFSPackedRevProps.INT64_BUFFER_SIZE;
                startRev = rev;
                packIsEmpty = true;
            }

            if (packIsEmpty) {
                packName = rev + ".0";
            }
            manifestBuilder.addPackName(packName);
            packIsEmpty = false;
            totalSize += SVNFSFSPackedRevProps.INT64_BUFFER_SIZE + size;
        }

        if (!packIsEmpty) {
            copyRevProps(packName, packPath, shardPath, startRev, endRev /*=rev - 1*/, fsfs.isCompressPackedRevprops());
        }

        final SVNFSFSPackedRevPropsManifest manifest = manifestBuilder.build();
        SVNFileUtil.writeToFile(new File(packPath, FSFS.MANIFEST_FILE), manifest.asString(), "UTF-8");

   }

    private void copyRevProps(String packName, File packPath, File shardPath, long startRev, long endRev, boolean compressPackedRevprops) throws SVNException {
        final SVNFSFSPackedRevProps.Builder packedRevPropsBuilder = new SVNFSFSPackedRevProps.Builder();
        packedRevPropsBuilder.setFirstRevision(startRev);

        for (long rev = startRev; rev <= endRev; rev++) {
            final File revPropFile = new File(shardPath, String.valueOf(rev));
            final byte[] content = SVNFileUtil.readFully(revPropFile);

            packedRevPropsBuilder.addByteArrayEntry(content);
        }

        final SVNFSFSPackedRevProps packedRevProps = packedRevPropsBuilder.build();
        final File packFile = new File(packPath, packName);
        packedRevProps.writeToFile(packFile, compressPackedRevprops);
    }

}
