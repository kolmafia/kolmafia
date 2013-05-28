package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

public class DebugProxyISVNWinCryptLibrary implements ISVNWinCryptLibrary {
    private final ISVNWinCryptLibrary myLibrary;
    private final ISVNDebugLog myDebugLog;

    public DebugProxyISVNWinCryptLibrary(ISVNWinCryptLibrary myLibrary, ISVNDebugLog myDebugLog) {
        this.myLibrary = myLibrary;
        this.myDebugLog = myDebugLog;
    }

    public boolean CryptProtectData(Pointer dataIn, WString description, Pointer entropy, Pointer reserved, Pointer struct, NativeLong flags, Pointer out) {
        boolean b = myLibrary.CryptProtectData(dataIn, description, entropy, reserved, struct, flags, out);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNWinCryptLibrary#CryptProtectData( flags =" +
                ", " + DebugProxyISVNCLibrary.toStringNullable(flags) + ") = " + b, Level.INFO);
        return b;
    }

    public boolean CryptUnprotectData(Pointer dataIn, Pointer description, Pointer entropy, Pointer reserved, Pointer struct, NativeLong flags, Pointer out) {
        boolean b = myLibrary.CryptUnprotectData(dataIn, description, entropy, reserved, struct, flags, out);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNWinCryptLibrary#CryptUnprotectData( flags =" +
                ", " + DebugProxyISVNCLibrary.toStringNullable(flags) + ") = " + b, Level.INFO);
        return b;
    }
}
