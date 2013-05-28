package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Pointer;

public class DebugProxyISVNCLibrary implements ISVNCLibrary {
    private final ISVNCLibrary myLibrary;
    private ISVNDebugLog myDebugLog;

    public DebugProxyISVNCLibrary(ISVNCLibrary library, ISVNDebugLog debugLog) {
        myLibrary = library;
        myDebugLog = debugLog;
    }

    public int chmod(String filename, int mode) {
        int chmod = myLibrary.chmod(filename, mode);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#chmod(" + filename + ", " + mode + ") = " + chmod, Level.INFO);
        return chmod;
    }

    public static String toStringNullable(final Object o) {
        return o == null ? "null" : o.toString();
    }

    public static String isNull(final Object o) {
        return o == null ? "null" : "NOT null";
    }

    public int readlink(String filename, Pointer linkname, int linkNameSize) {
        int readlink = myLibrary.readlink(filename, linkname, linkNameSize);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#readlink(" + filename + ", " + toStringNullable(linkname) + ", " + linkNameSize + ") = " + readlink, Level.INFO);
        return readlink;
    }

    public int __lxstat64(int ver, String path, Pointer stat) {
        int i = myLibrary.__lxstat64(ver, path, stat);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#__lxstat64(" + ver + ", " + path + ", " + toStringNullable(stat) + ") = " + i, Level.INFO);
        return i;
    }

    public int lstat(String path, Pointer stat) {
        int lstat = myLibrary.lstat(path, stat);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#lstat(" + path + ", " + toStringNullable(stat) + ") = " + lstat, Level.INFO);
        return lstat;
    }

    public int _lstat(String path, Pointer stat) {
        int i = myLibrary._lstat(path, stat);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#_lstat(" + path + ", " + toStringNullable(stat) + ") = " + i, Level.INFO);
        return i;
    }

    public int __xstat64(int ver, String path, Pointer stat) {
        int i = myLibrary.__xstat64(ver, path, stat);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#__xstat64(" + ver + ", " + path + ", " + toStringNullable(stat) + ") = " + i, Level.INFO);
        return i;
    }

    public int _stat(String path, Pointer stat) {
        int i = myLibrary._stat(path, stat);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#_stat(" + path + ", " + toStringNullable(stat) + ") = " + i, Level.INFO);
        return i;
    }

    public int stat(String path, Pointer stat) {
        int stat1 = myLibrary.stat(path, stat);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#stat(" + path + ", " + toStringNullable(stat) + ") = " + stat1, Level.INFO);
        return stat1;
    }

    public int symlink(String targetPath, String linkPath) {
        int symlink = myLibrary.symlink(targetPath, linkPath);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#symlink(" + targetPath + ", " + linkPath + ") = " + symlink, Level.INFO);
        return symlink;
    }

    public int getuid() {
        int getuid = myLibrary.getuid();
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#getuid() = " + getuid, Level.INFO);
        return getuid;
    }

    public int getgid() {
        int getgid = myLibrary.getgid();
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNCLibrary#getgid() = " + getgid, Level.INFO);
        return getgid;
    }
}
