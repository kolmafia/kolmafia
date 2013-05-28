package org.tmatesoft.svn.core.internal.util.jna;

import java.util.logging.Level;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

public class DebugProxyISVNSecurityLibrary implements ISVNSecurityLibrary {
    private final ISVNSecurityLibrary myLibrary;
    private final ISVNDebugLog myDebugLog;

    public DebugProxyISVNSecurityLibrary(ISVNSecurityLibrary myLibrary, ISVNDebugLog myDebugLog) {
        this.myLibrary = myLibrary;
        this.myDebugLog = myDebugLog;
    }

    public int FreeCredentialsHandle(Pointer phCredential) {
        int i = myLibrary.FreeCredentialsHandle(phCredential);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNSecurityLibrary#FreeCredentialsHandle(" +
                DebugProxyISVNCLibrary.toStringNullable(phCredential) + ") = " + i, Level.INFO);
        return i;
    }

    public int AcquireCredentialsHandleW(WString pszPrincipal, WString pszPackage, NativeLong fCredentialUse,
                                         Pointer pvLogonID, Pointer pAuthData, Pointer pGetKeyFn,
                                         Pointer pvGetKeyArgument, Pointer phCredential, Pointer ptsExpiry) {
        final int i = myLibrary.AcquireCredentialsHandleW(pszPrincipal, pszPackage, fCredentialUse, pvLogonID, pAuthData, pGetKeyFn, pvGetKeyArgument, phCredential, ptsExpiry);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNSecurityLibrary#AcquireCredentialsHandleW(" +
                DebugProxyISVNCLibrary.toStringNullable(phCredential) + ") = " + i, Level.INFO);
        return i;
    }

    public int FreeContextBuffer(Pointer pvContextBuffer) {
        return myLibrary.FreeContextBuffer(pvContextBuffer);
    }

    public int InitializeSecurityContextW(Pointer phCredential, Pointer phContext, WString pszTargetName, NativeLong fContextReq, NativeLong Reserved1, NativeLong TargetDataRep, Pointer pInput, NativeLong Reserved2, Pointer phNewContext, Pointer pOutput, Pointer pfContextAttr, Pointer ptsExpiry) {
        final int i = myLibrary.InitializeSecurityContextW(phCredential, phContext, pszTargetName, fContextReq, Reserved1, TargetDataRep, pInput, Reserved2, phNewContext, pOutput, pfContextAttr, ptsExpiry);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNSecurityLibrary#InitializeSecurityContextW(" +
                DebugProxyISVNCLibrary.toStringNullable(phCredential) + ", " + DebugProxyISVNCLibrary.toStringNullable(phContext) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(pszTargetName) + ", " +
                DebugProxyISVNCLibrary.toStringNullable(fContextReq) + ", " + DebugProxyISVNCLibrary.toStringNullable(Reserved1) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(TargetDataRep) + ", " + DebugProxyISVNCLibrary.toStringNullable(pInput) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(Reserved2) + ", " + DebugProxyISVNCLibrary.toStringNullable(phNewContext) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(pOutput) + ", " + DebugProxyISVNCLibrary.toStringNullable(pfContextAttr) +
                ", " + DebugProxyISVNCLibrary.toStringNullable(ptsExpiry) + ") = " + i, Level.INFO);
        return i;
    }

    public int CompleteAuthToken(Pointer phContext, Pointer pToken) {
        final int i = myLibrary.CompleteAuthToken(phContext, pToken);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNSecurityLibrary#CompleteAuthToken(" +
                DebugProxyISVNCLibrary.toStringNullable(phContext) + ", " +
                DebugProxyISVNCLibrary.toStringNullable(pToken) + ") = " + i, Level.INFO);
        return i;
    }

    public int DeleteSecurityContext(Pointer phContext) {
        final int i = myLibrary.DeleteSecurityContext(phContext);
        myDebugLog.log(SVNLogType.NATIVE_CALL, "CALLED ISVNSecurityLibrary#DeleteSecurityContext(" +
                DebugProxyISVNCLibrary.toStringNullable(phContext) + ") = " + i, Level.INFO);
        return i;
    }
}
