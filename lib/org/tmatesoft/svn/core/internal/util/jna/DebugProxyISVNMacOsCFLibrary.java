package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Pointer;

public class DebugProxyISVNMacOsCFLibrary implements ISVNMacOsCFLibrary {
    private final ISVNMacOsCFLibrary myLibrary;
    private final ISVNDebugLog myDebugLog;

    public DebugProxyISVNMacOsCFLibrary(ISVNMacOsCFLibrary myLibrary, ISVNDebugLog myDebugLog) {
        this.myLibrary = myLibrary;
        this.myDebugLog = myDebugLog;
    }

    public void CFRelease(Pointer pointer) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNMacOsCFLibrary#CFRelease(" + DebugProxyISVNCLibrary.toStringNullable(pointer) + ")", Level.INFO);
        myLibrary.CFRelease(pointer);
    }
}
