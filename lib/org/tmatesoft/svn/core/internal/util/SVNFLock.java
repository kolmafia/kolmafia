package org.tmatesoft.svn.core.internal.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.jna.SVNJNAUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SVNFLock {

    /**
     * Obtains flock using JNA or 'flock' utility
     * @param file file to lock
     * @param exclusive true for exclusive locking, false for shared locking
     * @return null if the system doesn't support flock, lock object otherwise
     * @throws SVNException is thrown if the system supports flock but flock is not obtained
     */
    public static SVNFLock obtain(File file, boolean exclusive) throws SVNException {
        if (!SVNFileUtil.isLinux && !SVNFileUtil.isBSD && !SVNFileUtil.isOSX) {
            //flock is supported not on all systems
            return null;
        }
        //try JNA:
        final int fd = SVNJNAUtil.flock(file, exclusive);
        if (fd >= 0) {
            return new SVNFLock(file, exclusive, fd, null);
        }

        //Now try command line
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(
                    shouldUsePerlCommand() ?
                            getPerlCommand(file, exclusive) :
                            getFlockCommand(file, exclusive));

            //once the process starts, it writes 'x' to stdout
            //then it waits for a new line from stdin and exits

            final Process process = processBuilder.start();
            final InputStream inputStream = process.getInputStream();
            final int read = inputStream.read();
            if (read == 'x') {
                return new SVNFLock(file, exclusive, -1, process);
            } else {
                final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Unable to lock ''{0}'', exclusive={1}", file, exclusive);
                SVNErrorManager.error(errorMessage, SVNLogType.DEFAULT);
            }
        } catch (IOException e) {
            final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.DEFAULT);
        }
        return null;
    }

    private final File file;
    private final boolean exclusive;
    private int fd;
    private Process flockProcess;

    private SVNFLock(File file, boolean exclusive, int fd, Process flockProcess) {
        this.file = file;
        this.exclusive = exclusive;
        this.fd = fd;
        this.flockProcess = flockProcess;
    }

    public File getFile() {
        return file;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean isValid() {
        return fd >= 0 || flockProcess != null;
    }

    public void release() {
        if (isJNALock()) {
            if (SVNJNAUtil.unflock(fd)) {
                fd = -1;
            }
        } else if (flockProcess != null) {
            final OutputStream outputStream = flockProcess.getOutputStream();
            try {
                //the process is waiting for a new line, then exits; let's finish it:
                outputStream.write('\n');
                outputStream.close();
                final int rc = flockProcess.waitFor();
                if (rc != 0) {
                    SVNDebugLog.getDefaultLog().log(SVNLogType.DEFAULT, "Couldn't unlock \"" + file.getAbsolutePath() + "\", rc=" + rc, Level.FINEST);
                }
                flockProcess = null;
            } catch (IOException e) {
                SVNDebugLog.getDefaultLog().log(SVNLogType.DEFAULT, e, Level.FINEST);
            } catch (InterruptedException e) {
                SVNDebugLog.getDefaultLog().log(SVNLogType.DEFAULT, e, Level.FINEST);
            }
        }
    }

    @Override
    public String toString() {
        return "SVNFLock{" +
                "file=" + file +
                ", exclusive=" + exclusive +
                ", valid=" + isValid() +
                ", using " + ((isJNALock()) ? "JNA" :
                    (shouldUsePerlCommand() ? "'perl' command" : "'flock' utility")) +
                '}';
    }

    private boolean isJNALock() {
        return fd >= 0;
    }

    private static boolean shouldUsePerlCommand() {
        return SVNFileUtil.isOSX;
    }

    private static List<String> getFlockCommand(File file, boolean exclusive) {
        return Arrays.<String>asList("flock",
                exclusive ? "--exclusive" : "--shared",
                file.getAbsolutePath(),
                "--command",
                "echo x && read -N 1");
    }

    private static List<String> getPerlCommand(File file, boolean exclusive) {
        return Arrays.<String>asList(
                "perl",
                "-MFcntl=:flock",
                "-e",
                "$|=1;" + //autoflush
                "$f=shift;" + //put filename to $f
                "open(FH, \"+>>\",$f)||die($!);" + // open the file
                String.format("flock(FH,%s);", exclusive ? "LOCK_EX" : "LOCK_SH") +
                "print \"x\";" +
                "readline(STDIN);" +
                "flock(FH,LOCK_UN);",
                file.getAbsolutePath());
    }
}
