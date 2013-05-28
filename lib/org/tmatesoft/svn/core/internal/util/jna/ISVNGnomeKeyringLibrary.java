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

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public interface ISVNGnomeKeyringLibrary extends Library {

    int GNOME_KEYRING_RESULT_OK = 0;
    int GNOME_KEYRING_RESULT_DENIED = 1;
    int GNOME_KEYRING_RESULT_NO_KEYRING_DAEMON = 2;
    int GNOME_KEYRING_RESULT_ALREADY_UNLOCKED = 3;
    int GNOME_KEYRING_RESULT_NO_SUCH_KEYRING = 4;
    int GNOME_KEYRING_RESULT_BAD_ARGUMENTS = 5;
    int GNOME_KEYRING_RESULT_IO_ERROR = 6;
    int GNOME_KEYRING_RESULT_CANCELLED = 7;
    int GNOME_KEYRING_RESULT_KEYRING_ALREADY_EXISTS = 8;
    int GNOME_KEYRING_RESULT_NO_MATCH = 9;

    interface GnomeKeyringOperationDoneCallback extends Callback {

        void callback(int result, Pointer data);
    }

    interface GnomeKeyringOperationGetKeyringInfoCallback extends Callback {

        void callback(int result, Pointer info, Pointer data);
    }

    interface GnomeKeyringOperationGetStringCallback extends Callback {

        void callback(int result, Pointer string, Pointer data);
    }

    boolean gnome_keyring_is_available();

    void gnome_keyring_get_default_keyring(GnomeKeyringOperationGetStringCallback callback,
                                           Pointer data,
                                           Pointer destroyData);

    void gnome_keyring_get_info(String name,
                                GnomeKeyringOperationGetKeyringInfoCallback callback,
                                Pointer data,
                                Pointer destroyData);

    Pointer gnome_keyring_info_copy(Pointer info);

    boolean gnome_keyring_info_get_is_locked(Pointer keyringInfo);

    void gnome_keyring_unlock(String keyringName,
                              String keyringPassword,
                              GnomeKeyringOperationDoneCallback callback,
                              Pointer data,
                              Pointer destroyData);

    int gnome_keyring_set_network_password_sync(String keychain,
                                                String userName,
                                                String domain,
                                                String server,
                                                String object,
                                                String protocol,
                                                String authType,
                                                int port,
                                                String password,
                                                IntByReference itemId);

    int gnome_keyring_find_network_password_sync(String userName,
                                                 String domain,
                                                 String server,
                                                 String object,
                                                 String protocol,
                                                 String authType,
                                                 int port,
                                                 PointerByReference items);

    void gnome_keyring_network_password_list_free(ISVNGLibrary.GList items);

    void gnome_keyring_info_free(Pointer info);

//  typedef struct {
//      char*keyring;
//      guint32 item_id;
//
//      char*protocol;
//      char*server;
//      char*object;
//      char*authtype;
//      guint32 port;
//
//      char*user;
//      char*domain;
//      char*password;
//  } GnomeKeyringNetworkPasswordData;

    class GnomeKeyringNetworkPasswordData extends Structure {

        public GnomeKeyringNetworkPasswordData(Pointer p) {
            super(p);
        }

        public Pointer keyring;
        public int itemId;

        public Pointer protocol;
        public Pointer server;
        public Pointer object;
        public Pointer authType;
        public int port;

        public Pointer user;
        public Pointer domain;
        public Pointer password;

        protected List<String> getFieldOrder() {
            return Arrays.asList("keyring", "itemId", "protocol", "server", "object", "authType", "port", "user", "domain", "password");
        }
    }
}
