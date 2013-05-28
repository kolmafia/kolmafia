package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

public class DebugProxyISVNKernel32Library implements ISVNKernel32Library {
    private final ISVNKernel32Library myLibrary;
    private final ISVNDebugLog myDebugLog;

    public DebugProxyISVNKernel32Library(ISVNKernel32Library myLibrary, ISVNDebugLog myDebugLog) {
        this.myLibrary = myLibrary;
        this.myDebugLog = myDebugLog;
    }

    public Pointer LocalFree(Pointer ptr) {
        final Pointer pointer = myLibrary.LocalFree(ptr);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNKernel32Library#LocalFree(" +
                DebugProxyISVNCLibrary.toStringNullable(ptr) + ") = " + DebugProxyISVNCLibrary.toStringNullable(pointer), Level.INFO);
        return pointer;
    }

    public int SetFileAttributesW(WString path, NativeLong attrs) {
        int i = myLibrary.SetFileAttributesW(path, attrs);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNKernel32Library#SetFileAttributesW(" +
                DebugProxyISVNCLibrary.toStringNullable(path) + ", " + DebugProxyISVNCLibrary.toStringNullable(attrs) +
                ") = " + i, Level.INFO);
        return i;
    }

    public int MoveFileW(WString src, WString dst) {
        int i = myLibrary.MoveFileW(src, dst);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNKernel32Library#MoveFileW(" +
                DebugProxyISVNCLibrary.toStringNullable(src) + ", " + DebugProxyISVNCLibrary.toStringNullable(dst) +
                ") = " + i, Level.INFO);
        return i;
    }

    public int MoveFileExW(WString src, WString dst, NativeLong flags) {
        int i = myLibrary.MoveFileExW(src, dst, flags);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNKernel32Library#MoveFileExW(" +
                DebugProxyISVNCLibrary.toStringNullable(src) + ", " + DebugProxyISVNCLibrary.toStringNullable(dst) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(flags) + ") = " + i, Level.INFO);
        return i;
    }

    public int GetVersionExW(Pointer pInfo) {
        int i = myLibrary.GetVersionExW(pInfo);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNKernel32Library#GetVersionExW(" +
                DebugProxyISVNCLibrary.toStringNullable(pInfo) + ") = " + i, Level.INFO);
        return i;
    }
}
