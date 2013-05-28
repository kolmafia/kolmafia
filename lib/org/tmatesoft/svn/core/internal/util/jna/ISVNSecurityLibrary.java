/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
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

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;


/**
 * @version 1.1.2
 * @author  TMate Software Ltd.
 */
public interface ISVNSecurityLibrary extends StdCallLibrary {
    public static int SEC_WINNT_AUTH_IDENTITY_ANSI = 0x1;
    public static int SEC_WINNT_AUTH_IDENTITY_UNICODE = 0x2;
    public static int SECPKG_CRED_OUTBOUND = 0x00000002;
    public static int SECURITY_NATIVE_DREP = 0x00000010;
    public static int SECBUFFER_TOKEN = 2;
    public static int SEC_I_CONTINUE_NEEDED = 0x00090312;
    public static int SEC_I_COMPLETE_NEEDED = 0x00090313;
    public static int SEC_I_COMPLETE_AND_CONTINUE = 0x00090314;
    
    public static class SecHandle extends Structure {
        public NativeLong dwLower;
        public NativeLong dwUpper;
        
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwLower", "dwUpper");
        }
    }
    
    public static class TimeStamp extends Structure {
        public NativeLong LowPart;
        public NativeLong HighPart;

        protected List<String> getFieldOrder() {
            return Arrays.asList("LowPart", "HighPart");
        }
    }
    
    public static class SecBufferDesc extends Structure {
        public NativeLong ulVersion;
        public NativeLong cBuffers;
        public Pointer pBuffers;

        protected List<String> getFieldOrder() {
            return Arrays.asList("ulVersion", "cBuffers", "pBuffers");
        }

    }
    
    public static class SecBuffer extends Structure {
        public NativeLong cbBuffer;
        public NativeLong BufferType;
        public Pointer pvBuffer;

        protected List<String> getFieldOrder() {
            return Arrays.asList("cbBuffer", "BufferType", "pvBuffer");
        }
    }
    
    public static class SEC_WINNT_AUTH_IDENTITY extends Structure {
        public WString User;
        public NativeLong UserLength;
        public WString Domain;
        public NativeLong DomainLength;
        public WString Password;
        public NativeLong PasswordLength;
        public NativeLong Flags;

        protected List<String> getFieldOrder() {
            return Arrays.asList("User", "UserLength", "Domain", "DomainLength", "Password", "PasswordLength", "Flags");
        }
    }
    
    public int FreeCredentialsHandle(Pointer phCredential);
    
    public int AcquireCredentialsHandleW(WString pszPrincipal, WString pszPackage, 
            NativeLong fCredentialUse, Pointer pvLogonID, Pointer pAuthData, Pointer pGetKeyFn, 
            Pointer pvGetKeyArgument, Pointer phCredential, Pointer ptsExpiry);

    public int FreeContextBuffer(Pointer pvContextBuffer);
    
    public int InitializeSecurityContextW(Pointer phCredential, Pointer phContext, 
            WString pszTargetName, NativeLong fContextReq, NativeLong Reserved1, 
            NativeLong TargetDataRep, Pointer pInput, NativeLong Reserved2, Pointer phNewContext,
            Pointer pOutput, Pointer pfContextAttr, Pointer ptsExpiry);
    
    public int CompleteAuthToken(Pointer phContext, Pointer pToken);
    
    public int DeleteSecurityContext(Pointer phContext);
}
