package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Pointer;

public class DebugProxyISVNWin32Library implements ISVNWin32Library {
    private final ISVNWin32Library myLibrary;
    private final ISVNDebugLog myDebugLog;

    public DebugProxyISVNWin32Library(ISVNWin32Library myLibrary, ISVNDebugLog myDebugLog) {
        this.myLibrary = myLibrary;
        this.myDebugLog = myDebugLog;
    }

    public HRESULT SHGetFolderPathW(Pointer hwndOwner, int nFolder, Pointer hToken, DWORD dwFlags, char[] pszPath) {
        final HRESULT hresult = myLibrary.SHGetFolderPathW(hwndOwner, nFolder, hToken, dwFlags, pszPath);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNWin32Library#SHGetFolderPathW(" +
                DebugProxyISVNCLibrary.toStringNullable(hwndOwner) + ", " +
                DebugProxyISVNCLibrary.toStringNullable(nFolder) + ", " +
                DebugProxyISVNCLibrary.toStringNullable(hToken)+ ", " +
                DebugProxyISVNCLibrary.toStringNullable(dwFlags) + ", " +
                DebugProxyISVNCLibrary.toStringNullable(pszPath) + ") = " + DebugProxyISVNCLibrary.toStringNullable(hresult), Level.INFO);
        return hresult;
    }
}
