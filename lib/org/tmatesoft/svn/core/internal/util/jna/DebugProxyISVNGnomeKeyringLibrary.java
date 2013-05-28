package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class DebugProxyISVNGnomeKeyringLibrary implements ISVNGnomeKeyringLibrary {
    private final ISVNGnomeKeyringLibrary myLibrary;
    private final ISVNDebugLog myDebugLog;

    public DebugProxyISVNGnomeKeyringLibrary(ISVNGnomeKeyringLibrary myLibrary, ISVNDebugLog myDebugLog) {
        this.myLibrary = myLibrary;
        this.myDebugLog = myDebugLog;
    }

    public boolean gnome_keyring_is_available() {
        final boolean b = myLibrary.gnome_keyring_is_available();
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_is_available() = " + b, Level.INFO);
        return b;
    }

    public void gnome_keyring_get_default_keyring(GnomeKeyringOperationGetStringCallback callback, Pointer data, Pointer destroyData) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_get_default_keyring(" +
                DebugProxyISVNCLibrary.toStringNullable(callback) + ", " + DebugProxyISVNCLibrary.toStringNullable(data) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(destroyData) + ")", Level.INFO);
        myLibrary.gnome_keyring_get_default_keyring(callback, data, destroyData);
    }

    public void gnome_keyring_get_info(String name, GnomeKeyringOperationGetKeyringInfoCallback callback, Pointer data, Pointer destroyData) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_get_info(" + name + ", " +
                DebugProxyISVNCLibrary.toStringNullable(callback) + ", " + DebugProxyISVNCLibrary.toStringNullable(data) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(destroyData) + ")", Level.INFO);
        myLibrary.gnome_keyring_get_info(name, callback, data, destroyData);
    }

    public Pointer gnome_keyring_info_copy(Pointer info) {
        final Pointer pointer = myLibrary.gnome_keyring_info_copy(info);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_info_copy(" +
                DebugProxyISVNCLibrary.toStringNullable(info) + ") = " + DebugProxyISVNCLibrary.toStringNullable(pointer), Level.INFO);
        return pointer;
    }

    public boolean gnome_keyring_info_get_is_locked(Pointer keyringInfo) {
        final boolean b = myLibrary.gnome_keyring_info_get_is_locked(keyringInfo);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_info_get_is_locked(" +
                DebugProxyISVNCLibrary.toStringNullable(keyringInfo) + ") = " + b, Level.INFO);
        return b;
    }

    public void gnome_keyring_unlock(String keyringName, String keyringPassword, GnomeKeyringOperationDoneCallback callback, Pointer data, Pointer destroyData) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_unlock(" + keyringName + ", " +
                DebugProxyISVNCLibrary.isNull(keyringPassword) + ", " + DebugProxyISVNCLibrary.toStringNullable(callback) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(data) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(destroyData) + ")", Level.INFO);
        myLibrary.gnome_keyring_unlock(keyringName, keyringPassword, callback, data, destroyData);
    }

    public int gnome_keyring_set_network_password_sync(String keychain, String userName, String domain, String server, String object, String protocol, String authType, int port, String password, IntByReference itemId) {
        final int i = myLibrary.gnome_keyring_set_network_password_sync(keychain, userName, domain, server, object, protocol, authType, port, password, itemId);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_set_network_password_sync(" +
                keychain + ", " + userName + ", " + domain + ", " + server + ", " + object + ", " + protocol +
                ", " + authType + ", " + port + ", " + DebugProxyISVNCLibrary.isNull(password) + ", " +
                DebugProxyISVNCLibrary.toStringNullable(itemId) + ") = " + i, Level.INFO);
        return i;
    }

    public int gnome_keyring_find_network_password_sync(String userName, String domain, String server, String object, String protocol, String authType, int port, PointerByReference items) {
        final int i = myLibrary.gnome_keyring_find_network_password_sync(userName, domain, server, object, protocol, authType, port, items);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_find_network_password_sync(" +
                userName + ", " + domain + ", " + server + ", " + object + ", " + protocol +
                ", " + authType + ", " + port + ", " + DebugProxyISVNCLibrary.toStringNullable(items) + ") = " + i, Level.INFO);
        return i;
    }

    public void gnome_keyring_network_password_list_free(ISVNGLibrary.GList items) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_network_password_list_free(" +
                DebugProxyISVNCLibrary.toStringNullable(items) + ")", Level.INFO);
        myLibrary.gnome_keyring_network_password_list_free(items);
    }

    public void gnome_keyring_info_free(Pointer info) {
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNGnomeKeyringLibrary#gnome_keyring_info_free(" +
                DebugProxyISVNCLibrary.toStringNullable(info) + ")", Level.INFO);
        myLibrary.gnome_keyring_info_free(info);
    }
}
