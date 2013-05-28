package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class DebugProxyISVNMacOsSecurityLibrary implements ISVNMacOsSecurityLibrary {
    private final ISVNMacOsSecurityLibrary myLibrary;
    private final ISVNDebugLog myDebugLog;

    public DebugProxyISVNMacOsSecurityLibrary(ISVNMacOsSecurityLibrary myLibrary, ISVNDebugLog myDebugLog) {
        this.myLibrary = myLibrary;
        this.myDebugLog = myDebugLog;
    }

    public int SecKeychainSetUserInteractionAllowed(boolean userInteractionAllowed) {
        final int i = myLibrary.SecKeychainSetUserInteractionAllowed(userInteractionAllowed);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNMacOsSecurityLibrary#SecKeychainSetUserInteractionAllowed(" +
                userInteractionAllowed + ") = " + i, Level.INFO);
        return i;
    }

    public int SecKeychainFindGenericPassword(Pointer keychain, int realmLength, byte[] realm, int userNameLength, byte[] userName, IntByReference passwordLengthHolder, PointerByReference passwordHolder, PointerByReference itemHolder) {
        final int i = myLibrary.SecKeychainFindGenericPassword(keychain, realmLength, realm, userNameLength, userName, passwordLengthHolder, passwordHolder, itemHolder);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNMacOsSecurityLibrary#SecKeychainFindGenericPassword(" +
                DebugProxyISVNCLibrary.toStringNullable(keychain) + ", " + realmLength +
                ", " + new String(realm, 0, realmLength) + ", " + userNameLength + ", " +
                new String(userName, 0, userNameLength) + ", " + DebugProxyISVNCLibrary.isNull(passwordLengthHolder) +
                ", " + DebugProxyISVNCLibrary.isNull(passwordHolder) + ", " + DebugProxyISVNCLibrary.isNull(itemHolder) + ") = " + i, Level.INFO);
        return i;
    }

    public int SecKeychainAddGenericPassword(Pointer keychain, int realmLength, byte[] realm, int userNameLength, byte[] userName, int pointerLength, byte[] password, Pointer item) {
        final int i = myLibrary.SecKeychainAddGenericPassword(keychain, realmLength, realm, userNameLength, userName, pointerLength, password, item);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNMacOsSecurityLibrary#SecKeychainAddGenericPassword(" +
                DebugProxyISVNCLibrary.toStringNullable(keychain) + ", " + realmLength +
                ", " + new String(realm, 0, realmLength) + ", " + userNameLength + ", " +
                new String(userName, 0, userNameLength) + ", " + pointerLength +
                ", " + DebugProxyISVNCLibrary.isNull(password) + ", " + DebugProxyISVNCLibrary.toStringNullable(item) + ") = " + i, Level.INFO);
        return i;
    }

    public int SecKeychainItemModifyAttributesAndData(Pointer item, PointerByReference attributesHolder, int passwordLength, byte[] password) {
        final int i = myLibrary.SecKeychainItemModifyAttributesAndData(item, attributesHolder, passwordLength, password);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNMacOsSecurityLibrary#SecKeychainItemModifyAttributesAndData(" +
                DebugProxyISVNCLibrary.toStringNullable(item) + ", " + DebugProxyISVNCLibrary.toStringNullable(attributesHolder) +
                ", " + passwordLength + ", " + DebugProxyISVNCLibrary.isNull(password) + ") = " + i, Level.INFO);
        return i;
    }

    public int SecKeychainItemFreeContent(Pointer attributes, Pointer data) {
        final int i = myLibrary.SecKeychainItemFreeContent(attributes, data);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNMacOsSecurityLibrary#SecKeychainItemFreeContent(" +
                DebugProxyISVNCLibrary.toStringNullable(attributes) + ", " + DebugProxyISVNCLibrary.toStringNullable(data) + ") = " + i, Level.INFO);
        return i;
    }
}
