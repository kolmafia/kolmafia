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

import java.io.UnsupportedEncodingException;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
class SVNMacOsKeychain {

    private static final int ERR_SEC_ITEM_NOT_FOUND = -25300;

    static boolean isEnabled() {
        return SVNFileUtil.isOSX && JNALibraryLoader.getMacOsSecurityLibrary() != null;
    }

    public static synchronized boolean setPassword(String realm, String userName, String password, boolean nonInteractive) throws SVNException {
        final ISVNMacOsSecurityLibrary library = JNALibraryLoader.getMacOsSecurityLibrary();
        if (library == null) {
            return false;
        }
        if (realm == null) {
            return false;
        }

        if (nonInteractive) {
            library.SecKeychainSetUserInteractionAllowed(false);
        }

        try {
            byte[] rawRealm = realm.getBytes("UTF-8");
            byte[] rawUserName = userName == null ? null : userName.getBytes("UTF-8");
            int rawUserNameLength = userName == null ? 0 : rawUserName.length;
            byte[] rawPassword = password.getBytes("UTF-8");

            PointerByReference itemHolder = new PointerByReference();
            int status = library.SecKeychainFindGenericPassword(null, rawRealm.length, rawRealm,
                    rawUserNameLength, rawUserName, null, null, itemHolder);

            if (status == ERR_SEC_ITEM_NOT_FOUND) {
                status = library.SecKeychainAddGenericPassword(null, rawRealm.length, rawRealm,
                        rawUserNameLength, rawUserName, rawPassword.length, rawPassword, null);
            } else {
                Pointer item = itemHolder.getValue();
                try {
                    status = library.SecKeychainItemModifyAttributesAndData(item, null, rawPassword.length, rawPassword);
                } finally {
                    release(item);
                }
            }

            return status == 0;
        } catch (UnsupportedEncodingException e) {
            SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(error, e, SVNLogType.DEFAULT);
        } finally {
            if (nonInteractive) {
                library.SecKeychainSetUserInteractionAllowed(true);
            }
        }
        
        return false;
    }

    public static synchronized String getPassword(String realm, String userName, boolean nonInteractive) throws SVNException {
        ISVNMacOsSecurityLibrary library = JNALibraryLoader.getMacOsSecurityLibrary();
        if (library == null) {
            return null;
        }

        if (realm == null) {
            return null;
        }

        if (nonInteractive) {
            library.SecKeychainSetUserInteractionAllowed(false);
        }

        try {
            byte[] rawRealm = realm.getBytes("UTF-8");
            byte[] rawUserName = userName == null ? null : userName.getBytes("UTF-8");
            int rawUserNameLength = userName == null ? 0 : rawUserName.length;
            IntByReference passwordLengthHolder = new IntByReference();
            PointerByReference passwordHolder = new PointerByReference();

            int status = library.SecKeychainFindGenericPassword(null, rawRealm.length, rawRealm, rawUserNameLength, rawUserName,
                    passwordLengthHolder, passwordHolder, null);

            if (status != 0) {
                return null;
            }

            Pointer passwordPointer = passwordHolder.getValue();
            if (passwordPointer == null) {
                return null;
            }

            int passwordLength = passwordLengthHolder.getValue();
            byte[] rawPassword = passwordPointer.getByteArray(0, passwordLength);

            String password;
            try {
                password = new String(rawPassword, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(error, e, SVNLogType.DEFAULT);
                password = null;
            } finally {
                library.SecKeychainItemFreeContent(null, passwordPointer);
            }

            return password;
        } catch (UnsupportedEncodingException e) {
            SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(error, e, SVNLogType.DEFAULT);
            return null;
        } finally {
            if (nonInteractive) {
                library.SecKeychainSetUserInteractionAllowed(true);
            }
        }
    }

    private static void release(Pointer pointer) {
        if (pointer != null) {
            ISVNMacOsCFLibrary library = JNALibraryLoader.getMacOsCFLibrary();
            if (library != null) {
                library.CFRelease(pointer);
            }
        }
    }
}
