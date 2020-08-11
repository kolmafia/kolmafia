package org.tmatesoft.svn.core.internal.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SVNDoubleLock {

    private static boolean ourUseDoubleLock = Boolean.valueOf(System.getProperty("svnkit.useDoubleLock", Boolean.valueOf(SVNFileUtil.isLinux).toString()));

    public static SVNDoubleLock obtain(File file, boolean exclusive) throws SVNException {
        //FLOCK
        final SVNFLock flock = ourUseDoubleLock ? obtainFlockIfNeeded(file, exclusive) : null;

        //POSIX lock
        RandomAccessFile randomAccessFile = null;
        FileLock posixLock = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            posixLock = randomAccessFile.getChannel().lock(0, Long.MAX_VALUE, !exclusive);
        } catch (IOException e) {
            if (flock != null) {
                flock.release();
            }
            final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.DEFAULT);
        }
        return new SVNDoubleLock(flock, randomAccessFile, posixLock, file, exclusive);
    }

    private SVNFLock flock; //null if flocks are not supported or cause problems on the platform
    private RandomAccessFile randomAccessFile;
    private FileLock posixLock;

    //fields for toString() method:
    private final File file;
    private final boolean exclusive;
    private boolean valid;

    private SVNDoubleLock(SVNFLock flock, RandomAccessFile randomAccessFile, FileLock posixLock, File file, boolean exclusive) {
        this.flock = flock;
        this.randomAccessFile = randomAccessFile;
        this.posixLock = posixLock;
        this.file = file;
        this.exclusive = exclusive;
        this.valid = true;
    }

    public void release() {
        //release locks in the reverse order: POSIX lock first, FLOCK second
        try {
            if (posixLock != null) {
                posixLock.release();
                posixLock = null;
            }
            if (randomAccessFile != null) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
            if (flock != null) {
                flock.release();
                flock = null;
            }
            valid = false;
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().log(SVNLogType.DEFAULT, e, Level.INFO);
        }
    }

    private static SVNFLock obtainFlockIfNeeded(File file, boolean exclusive) throws SVNException {
        //create a test file in the same directory to check filesystem locking capabilities
        final File uniqueFile = SVNFileUtil.createUniqueFile(file.getParentFile(),
                file.getName(), ".trylock", true);
        try {
            final boolean doubleLockingNeeded = isDoubleLockingNeeded(uniqueFile, exclusive);

            //if FLOCK blocks POSIX lock, double locking is not needed
            return doubleLockingNeeded ? SVNFLock.obtain(file, exclusive) : null;
        } finally {
            SVNFileUtil.deleteFile(uniqueFile);
        }
    }

    private static boolean isDoubleLockingNeeded(File file, boolean exclusive) throws SVNException {
        final SVNFLock flock = SVNFLock.obtain(file, exclusive);
        if (flock == null) {
            //can't obtain FLOCK at all, FLOCKs are probably not supported,
            //double locking is not needed
            return false;
        }
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            FileLock posixLock = randomAccessFile.getChannel().tryLock(0, Long.MAX_VALUE, !exclusive);
            //if FLOCK and POSIX lock can be obtained at the same time
            //double locking is needed
            final boolean doubleLockingNeeded = (posixLock != null);
            if (posixLock != null) {
                posixLock.release();
            }
            return doubleLockingNeeded;
        } catch (IOException e) {
            final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            return false;
        } finally {
            if (randomAccessFile != null) {
                SVNFileUtil.closeFile(randomAccessFile);
            }
            flock.release();
        }
    }

    public String toString() {
        if (!valid) {
            return "SVNDoubleLock{file=" + file + ", valid = false, exclusive=" + exclusive + "}";
        } else {
            return "SVNDoubleLock{" +
                    ((flock != null) ? ("flock=" + flock) : "flock is not supported") +
                    ", posixLock=" + posixLock +
                    '}';
        }
    }
}
