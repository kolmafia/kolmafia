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

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
interface ISVNMacOsSecurityLibrary extends Library {

    int SecKeychainSetUserInteractionAllowed(boolean userInteractionAllowed);

    int SecKeychainFindGenericPassword(Pointer keychain, int realmLength, byte[] realm,
                                       int userNameLength, byte[] userName, IntByReference passwordLengthHolder,
                                       PointerByReference passwordHolder, PointerByReference itemHolder);

    int SecKeychainAddGenericPassword(Pointer keychain, int realmLength, byte[] realm, int userNameLength,
                                      byte[] userName, int pointerLength, byte[] password, Pointer item);

    int SecKeychainItemModifyAttributesAndData(Pointer item, PointerByReference attributesHolder, int passwordLength, byte[] password);

    int SecKeychainItemFreeContent(Pointer attributes, Pointer data);
}
