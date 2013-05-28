/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.util.jna;

import java.util.Arrays;
import java.util.List;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.ISVNGnomeKeyringPasswordProvider;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNGnomeKeyring {

    private static final ISVNGnomeKeyringLibrary.GnomeKeyringOperationDoneCallback DONE_CALLBACK = new ISVNGnomeKeyringLibrary.GnomeKeyringOperationDoneCallback() {

        public void callback(int result, Pointer data) {
            if (data == null) {
                return;
            }
            ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();
            
            GnomeKeyringContext context = new GnomeKeyringContext(data);
            context.read();
            gLibrary.g_main_loop_quit(context.loop);
        }
    };

    private static final ISVNGnomeKeyringLibrary.GnomeKeyringOperationGetKeyringInfoCallback GET_KEYRING_INFO_CALLBACK = new ISVNGnomeKeyringLibrary.GnomeKeyringOperationGetKeyringInfoCallback() {

        public void callback(int result, Pointer info, Pointer data) {
            if (data == null) {
                return;
            }
            ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
            ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();
            
            GnomeKeyringContext context = new GnomeKeyringContext(data);
            context.read();
            if (result == ISVNGnomeKeyringLibrary.GNOME_KEYRING_RESULT_OK && info != null) {
                context.keyringInfo = gnomeKeyringLibrary.gnome_keyring_info_copy(info);
                context.write();
            } else {
                if (context.keyringInfo != null) {
                    gnomeKeyringLibrary.gnome_keyring_info_free(context.keyringInfo);
                }
                context.keyringInfo = null;
                context.write();
            }
            gLibrary.g_main_loop_quit(context.loop);
        }
    };

    private static final ISVNGnomeKeyringLibrary.GnomeKeyringOperationGetStringCallback DEFAULT_KEYRING_CALLBACK = new ISVNGnomeKeyringLibrary.GnomeKeyringOperationGetStringCallback() {

        public void callback(int result, Pointer value, Pointer data) {
            if (data == null) {
                return;
            }
            ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();

            GnomeKeyringContext context = new GnomeKeyringContext(data);
            context.read();
            if (result == ISVNGnomeKeyringLibrary.GNOME_KEYRING_RESULT_OK && value != null) {
                String stringValue = value.getString(0);
                context.keyringName = stringValue;
                context.write();
            } else {
//              if (key_info - > keyring_name != NULL)
//                  free((void*)key_info - > keyring_name);
//              Does JNA free native memory referenced by keyringName or not?
                context.keyringName = null;
                context.write();
            }
            gLibrary.g_main_loop_quit(context.loop);
        }
    };

    public static boolean isEnabled() {
        boolean gnomeSupported = SVNFileUtil.isOSX || SVNFileUtil.isLinux || SVNFileUtil.isBSD || SVNFileUtil.isSolaris;

        String gnomeKeyringOption = System.getProperty("svnkit.library.gnome-keyring.enabled", "true");
        boolean gnomeKeyringEnabled = Boolean.TRUE.toString().equalsIgnoreCase(gnomeKeyringOption);

        boolean librariesLoaded = JNALibraryLoader.getGnomeKeyringLibrary() != null && JNALibraryLoader.getGLibrary() != null;

        final boolean enabled = gnomeSupported && gnomeKeyringEnabled && librariesLoaded;
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, enabled ? "Gnome Keyring enabled" : "Gnome Keyring disabled");
        return enabled;
    }

    public static void initialize() {
        ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();
        if (gLibrary == null) {
            return;
        }
        String applicationName = gLibrary.g_get_application_name();
        if (applicationName == null) {
            gLibrary.g_set_application_name("Subversion");
        }
    }

    private static String getDefaultKeyringName() {
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
        ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();

        GnomeKeyringContext context = new GnomeKeyringContext();
        context.keyringInfo = null;
        context.keyringName = null;
        context.loop = gLibrary.g_main_loop_new(null, false);
        context.write();

        gnomeKeyringLibrary.gnome_keyring_get_default_keyring(DEFAULT_KEYRING_CALLBACK, context.getPointer(), null);
        gLibrary.g_main_loop_run(context.loop);
        context.read();

        if (context.keyringName == null) {
            destroyKeyringContext(context);
            return null;
        }

        String defaultKeyringName = context.keyringName;
        destroyKeyringContext(context);
        return defaultKeyringName;
    }

    private static boolean checkKeyringIsLocked(String keyringName) {
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
        ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();

        GnomeKeyringContext context = new GnomeKeyringContext();
        context.keyringName = null;
        context.keyringName = null;
        context.loop = gLibrary.g_main_loop_new(null, false);
        context.write();

        gnomeKeyringLibrary.gnome_keyring_get_info(keyringName, GET_KEYRING_INFO_CALLBACK, context.getPointer(), null);
        gLibrary.g_main_loop_run(context.loop);
        context.read();

        if (context.keyringInfo == null) {
            destroyKeyringContext(context);
            return false;
        }

        return gnomeKeyringLibrary.gnome_keyring_info_get_is_locked(context.keyringInfo);
    }

    private static void unlockKeyring(String keyringName, String keyringPassword) {
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
        ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();

        GnomeKeyringContext context = new GnomeKeyringContext();
        context.keyringInfo = null;
        context.keyringName = null;
        context.loop = gLibrary.g_main_loop_new(null, false);
        context.write();

        gnomeKeyringLibrary.gnome_keyring_get_info(keyringName, GET_KEYRING_INFO_CALLBACK, context.getPointer(), null);
        gLibrary.g_main_loop_run(context.loop);
        context.read();

        if (context.keyringInfo == null) {
            destroyKeyringContext(context);
            return;
        } else {
            context.loop = gLibrary.g_main_loop_new(null, false);
            context.write();

            gnomeKeyringLibrary.gnome_keyring_unlock(keyringName, keyringPassword, DONE_CALLBACK, context.getPointer(), null);
            gLibrary.g_main_loop_run(context.loop);
            context.read();
        }
        destroyKeyringContext(context);
    }

    public static String getPassword(String realm, String userName, boolean nonInteractive, ISVNGnomeKeyringPasswordProvider keyringPasswordProvider) throws SVNException {
        String defaultKeyring = getDefaultKeyringName();
        if (!nonInteractive) {
            if (checkKeyringIsLocked(defaultKeyring)) {
                if (keyringPasswordProvider != null) {
                    String keyringPassword = keyringPasswordProvider.getKeyringPassword(defaultKeyring);
                    unlockKeyring(defaultKeyring, keyringPassword);
                }
            }
        }
        if (checkKeyringIsLocked(defaultKeyring)) {
            SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE,
                    "GNOME Keyring is locked and we are non-interactive");
            SVNErrorManager.error(error, SVNLogType.CLIENT);
            return null;
        } else {
            return getPassword(realm, userName);
        }
    }

    private static String getPassword(String realm, String userName) {
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();

//      Ensure Gnome Keyring access is initialized via dbus.
//      if (!dbus_bus_get(DBUS_BUS_SESSION, NULL)) {
//          return FALSE;
//      }

        if (!gnomeKeyringLibrary.gnome_keyring_is_available()) {
            return null;
        }
        
        getDefaultKeyringName();
        PointerByReference itemsReference = new PointerByReference();
        int result = gnomeKeyringLibrary.gnome_keyring_find_network_password_sync(userName, realm, null, null, null, null, 0, itemsReference);

        String password = null;
        if (result == ISVNGnomeKeyringLibrary.GNOME_KEYRING_RESULT_OK) {
            ISVNGLibrary.GList items = new ISVNGLibrary.GList(itemsReference.getValue());
            items.read();
            if (items.data != null) {
                ISVNGnomeKeyringLibrary.GnomeKeyringNetworkPasswordData item = new ISVNGnomeKeyringLibrary.GnomeKeyringNetworkPasswordData(items.data);
                item.read();
                if (item.password != null) {
                    password = item.password.getString(0);
                }
                gnomeKeyringLibrary.gnome_keyring_network_password_list_free(items);
            }
        }

//      Subversion frees memory allocated for temporary defaultKeyring value.
//      Ensure we freed native memory from which we obtained corresponding String Object keyringName.
//      if (default_keyring)
//          free(default_keyring);
        
        return password;
    }

    public static boolean setPassword(String realm, String userName, String password, boolean nonInteractive, ISVNGnomeKeyringPasswordProvider keyringPasswordProvider) throws SVNException {
        String defaultKeyring = getDefaultKeyringName();
        if (!nonInteractive) {
            if (checkKeyringIsLocked(defaultKeyring)) {
                if (keyringPasswordProvider != null) {
                    String keyringPassword = keyringPasswordProvider.getKeyringPassword(defaultKeyring);
                    unlockKeyring(defaultKeyring, keyringPassword);
                }
            }
        }
        if (checkKeyringIsLocked(defaultKeyring)) {
            SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE,
                    "GNOME Keyring is locked and we are non-interactive");
            SVNErrorManager.error(error, SVNLogType.CLIENT);
            return false;
        } else {
            return setPassword(realm, userName, password);
        }
    }

    private static boolean setPassword(String realm, String userName, String password) {
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();

//      Ensure Gnome Keyring access is initialized via dbus.
//      if (!dbus_bus_get(DBUS_BUS_SESSION, NULL)) {
//          return FALSE;
//      }

        if (!gnomeKeyringLibrary.gnome_keyring_is_available()) {
            return false;
        }
        
        getDefaultKeyringName();
        IntByReference itemId = new IntByReference();
        int result = gnomeKeyringLibrary.gnome_keyring_set_network_password_sync(null, userName, realm, null, null, null, null, 0, password, itemId);

//      Subversion frees memory allocated for temporary defaultKeyring value.
//      Ensure we freed native memory from which we obtained corresponding String Object keyringName.
//      if (default_keyring)
//          free(default_keyring);

        return result == ISVNGnomeKeyringLibrary.GNOME_KEYRING_RESULT_OK;
    }

    private static void destroyKeyringContext(GnomeKeyringContext context) {
        if (context == null) {
            return;
        }
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
//      if (key_info - > keyring_name != NULL)
//          free((void*)key_info - > keyring_name);
//      Does JNA free native memory referenced by keyringName or not?

        context.keyringName = null;
        context.write();

        if (context.keyringInfo != null) {
            gnomeKeyringLibrary.gnome_keyring_info_free(context.keyringInfo);
            context.keyringInfo = null;
        }
    }

//  struct gnome_keyring_baton
//  {
//      const char*keyring_name;
//      GnomeKeyringInfo * info;
//      GMainLoop * loop;
//  }
    public static class GnomeKeyringContext extends Structure {

        public GnomeKeyringContext() {
        }

        public GnomeKeyringContext(Pointer p) {
            super(p);
        }

        public String keyringName;
        public Pointer keyringInfo;
        public Pointer loop;
        
        protected List<String> getFieldOrder() {
            return Arrays.asList("keyringName", "keyringInfo", "loop");
        }
    }
}
