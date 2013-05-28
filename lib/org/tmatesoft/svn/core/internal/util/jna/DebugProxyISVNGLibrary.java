package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Pointer;

public class DebugProxyISVNGLibrary implements ISVNGLibrary {
    private final ISVNGLibrary myLibrary;
    private final ISVNDebugLog myDebugLog;

    public DebugProxyISVNGLibrary(ISVNGLibrary myLibrary, ISVNDebugLog myDebugLog) {
        this.myLibrary = myLibrary;
        this.myDebugLog = myDebugLog;
    }

    public String g_get_application_name() {
        final String s = myLibrary.g_get_application_name();
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGLibrary#g_get_application_name() = " + s, Level.INFO);
        return s;
    }

    public void g_set_application_name(String applicationName) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGLibrary#g_set_application_name() = " + applicationName, Level.INFO);
        myLibrary.g_set_application_name(applicationName);
    }

    public Pointer g_main_loop_new(Pointer context, boolean isRunning) {
        final Pointer pointer = myLibrary.g_main_loop_new(context, isRunning);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGLibrary#g_main_loop_new(" +
                DebugProxyISVNCLibrary.toStringNullable(context) + ", " + isRunning + ") = " +
                DebugProxyISVNCLibrary.toStringNullable(pointer), Level.INFO);
        return pointer;
    }

    public void g_main_loop_run(Pointer context) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGLibrary#g_main_loop_run() = " + DebugProxyISVNCLibrary.toStringNullable(context), Level.INFO);
        myLibrary.g_main_loop_run(context);
    }

    public void g_main_loop_quit(Pointer context) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGLibrary#g_main_loop_quit() = " + DebugProxyISVNCLibrary.toStringNullable(context), Level.INFO);
        myLibrary.g_main_loop_quit(context);
    }
}
