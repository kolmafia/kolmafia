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
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.wc.ISVNGnomeKeyringPasswordProvider;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNGnomeKeyring {
	
	private static final Object keyringAccessMonitor = new Object();

    private static final ISVNGnomeKeyringLibrary.GnomeKeyringOperationDoneCallback DONE_CALLBACK = new ISVNGnomeKeyringLibrary.GnomeKeyringOperationDoneCallback() {

        public void callback(int result, Pointer data) {
            if (data == null) {
                return;
            }
            final ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();
            synchronized (keyringAccessMonitor) {
                GnomeKeyringContext context = new GnomeKeyringContext(data);
                context.read();
                gLibrary.g_main_loop_quit(context.loop);
			}
        }
    };

    private static final ISVNGnomeKeyringLibrary.GnomeKeyringOperationGetKeyringInfoCallback GET_KEYRING_INFO_CALLBACK = new ISVNGnomeKeyringLibrary.GnomeKeyringOperationGetKeyringInfoCallback() {

        public void callback(int result, Pointer info, Pointer data) {
            if (data == null) {
                return;
            }
            final ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
            final ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();
            synchronized (keyringAccessMonitor) {
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
        }
    };

    private static final ISVNGnomeKeyringLibrary.GnomeKeyringOperationGetStringCallback DEFAULT_KEYRING_CALLBACK = new ISVNGnomeKeyringLibrary.GnomeKeyringOperationGetStringCallback() {

        public void callback(int result, Pointer value, Pointer data) {
            if (data == null) {
                return;
            }
            final ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();
            synchronized (keyringAccessMonitor) {
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
        final ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();
        if (gLibrary == null) {
            return;
        }
        synchronized (keyringAccessMonitor) {
            String applicationName = gLibrary.g_get_application_name();
            if (applicationName == null) {
                gLibrary.g_set_application_name("Subversion");
            }
		}
    }

    private static String getDefaultKeyringName() {
        final ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
        final ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();
        synchronized (keyringAccessMonitor) {
	
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
    }

    private static boolean checkKeyringIsLocked(String keyringName) {
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
        ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();

        GnomeKeyringContext context = new GnomeKeyringContext();
        context.keyringName = null;
        context.keyringName = null;
        context.loop = gLibrary.g_main_loop_new(null, false);
        context.write();

        synchronized (keyringAccessMonitor) {
            gnomeKeyringLibrary.gnome_keyring_get_info(keyringName, GET_KEYRING_INFO_CALLBACK, context.getPointer(), null);
            gLibrary.g_main_loop_run(context.loop);
            context.read();

            if (context.keyringInfo == null) {
                destroyKeyringContext(context);
                return false;
            }

            return gnomeKeyringLibrary.gnome_keyring_info_get_is_locked(context.keyringInfo);
		}
    }

    private static void unlockKeyring(String keyringName, char[] keyringPassword) {
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
        ISVNGLibrary gLibrary = JNALibraryLoader.getGLibrary();

        GnomeKeyringContext context = new GnomeKeyringContext();
        context.keyringInfo = null;
        context.keyringName = null;
        context.loop = gLibrary.g_main_loop_new(null, false);
        context.write();

        synchronized (keyringAccessMonitor) {
	        gnomeKeyringLibrary.gnome_keyring_get_info(keyringName, GET_KEYRING_INFO_CALLBACK, context.getPointer(), null);
	        gLibrary.g_main_loop_run(context.loop);
	        context.read();
	
	        if (context.keyringInfo == null) {
	            destroyKeyringContext(context);
	            return;
	        } else {
	            context.loop = gLibrary.g_main_loop_new(null, false);
	            context.write();
	
	            final byte[] keyringUTF8Password = SVNEncodingUtil.getBytes(keyringPassword, "UTF-8");
                Memory passwordData = null;
                try {
                    passwordData = new Memory(keyringUTF8Password.length + 1);
                    passwordData.write(0, keyringUTF8Password, 0, keyringUTF8Password.length);
                    passwordData.setByte(keyringUTF8Password.length, (byte) 0);
    	            gnomeKeyringLibrary.gnome_keyring_unlock(keyringName, passwordData.getPointer(0), DONE_CALLBACK, context.getPointer(), null);
    	            gLibrary.g_main_loop_run(context.loop);
    	            context.read();
                } finally {
                    if (passwordData != null) {
                        passwordData.clear();
                    }
                    SVNEncodingUtil.clearArray(keyringUTF8Password);
                }
	        }
	        destroyKeyringContext(context);
        }
    }

    public static char[] getPassword(String realm, String userName, boolean nonInteractive, ISVNGnomeKeyringPasswordProvider keyringPasswordProvider) throws SVNException {
        String defaultKeyring = getDefaultKeyringName();
        if (!nonInteractive) {
            if (checkKeyringIsLocked(defaultKeyring)) {
                if (keyringPasswordProvider != null) {
                    char[] keyringPassword = keyringPasswordProvider.getKeyringPassword(defaultKeyring);
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

    private static char[] getPassword(String realm, String userName) {
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
//      Ensure Gnome Keyring access is initialized via dbus.
//      if (!dbus_bus_get(DBUS_BUS_SESSION, NULL)) {
//          return FALSE;
//      }

        synchronized (keyringAccessMonitor) {
            if (!gnomeKeyringLibrary.gnome_keyring_is_available()) {
                return null;
            }
            
            getDefaultKeyringName();
            PointerByReference itemsReference = new PointerByReference();
            int result = gnomeKeyringLibrary.gnome_keyring_find_network_password_sync(userName, realm, null, null, null, null, 0, itemsReference);

            byte[]password = null;
            if (result == ISVNGnomeKeyringLibrary.GNOME_KEYRING_RESULT_OK) {
                ISVNGLibrary.GList items = new ISVNGLibrary.GList(itemsReference.getValue());
                items.read();
                if (items.data != null) {
                    ISVNGnomeKeyringLibrary.GnomeKeyringNetworkPasswordData item = new ISVNGnomeKeyringLibrary.GnomeKeyringNetworkPasswordData(items.data);
                    item.read();
                    if (item.password != null) {
                        int offset = 0;
                        while(item.password.getByte(offset) != 0 && offset < item.size()) {
                            offset++;
                        }
                        password = item.password.getByteArray(0, offset);
                    }
                    gnomeKeyringLibrary.gnome_keyring_network_password_list_free(items);
                }
            }

//          Subversion frees memory allocated for temporary defaultKeyring value.
//          Ensure we freed native memory from which we obtained corresponding String Object keyringName.
//          if (default_keyring)
//              free(default_keyring);
            try {
                return SVNEncodingUtil.getChars(password, "UTF-8");
            } finally {
                SVNEncodingUtil.clearArray(password);
            }
		}
    }

    public static boolean setPassword(String realm, String userName, char[] password, boolean nonInteractive, ISVNGnomeKeyringPasswordProvider keyringPasswordProvider) throws SVNException {
        String defaultKeyring = getDefaultKeyringName();
        if (!nonInteractive) {
            if (checkKeyringIsLocked(defaultKeyring)) {
                if (keyringPasswordProvider != null) {
                    char[] keyringPassword = keyringPasswordProvider.getKeyringPassword(defaultKeyring);
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

    private static boolean setPassword(String realm, String userName, char[] password) {
        final ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
//      Ensure Gnome Keyring access is initialized via dbus.
//      if (!dbus_bus_get(DBUS_BUS_SESSION, NULL)) {
//          return FALSE;
//      }

        synchronized (keyringAccessMonitor) {
	        if (!gnomeKeyringLibrary.gnome_keyring_is_available()) {
	            return false;
	        }
	        
	        getDefaultKeyringName();
	        IntByReference itemId = new IntByReference();

	        final byte[] utf8Password = SVNEncodingUtil.getBytes(password, "UTF-8");
            Memory passwordData = null;
            int result;
            try {
                passwordData = new Memory(utf8Password.length + 1);
                passwordData.write(0, utf8Password, 0, utf8Password.length);
                passwordData.setByte(utf8Password.length, (byte) 0);
                result = gnomeKeyringLibrary.gnome_keyring_set_network_password_sync(null, userName, realm, null, null, null, null, 0, passwordData.getPointer(0), itemId);
            } finally {
                if (passwordData != null) {
                    passwordData.clear();
                }
                SVNEncodingUtil.clearArray(utf8Password);
            }
	
	//      Subversion frees memory allocated for temporary defaultKeyring value.
	//      Ensure we freed native memory from which we obtained corresponding String Object keyringName.
	//      if (default_keyring)
	//          free(default_keyring);
	
	        return result == ISVNGnomeKeyringLibrary.GNOME_KEYRING_RESULT_OK;
        }
    }

    private static void destroyKeyringContext(GnomeKeyringContext context) {
        if (context == null) {
            return;
        }
        ISVNGnomeKeyringLibrary gnomeKeyringLibrary = JNALibraryLoader.getGnomeKeyringLibrary();
//      if (key_info - > keyring_name != NULL)
//          free((void*)key_info - > keyring_name);
//      Does JNA free native memory referenced by keyringName or not?


        synchronized (keyringAccessMonitor) {
            context.keyringName = null;
            context.write();
            if (context.keyringInfo != null) {
                gnomeKeyringLibrary.gnome_keyring_info_free(context.keyringInfo);
                context.keyringInfo = null;
            }
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
