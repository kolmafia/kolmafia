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

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNBase64;
import org.tmatesoft.svn.core.internal.util.jna.ISVNSecurityLibrary.SEC_WINNT_AUTH_IDENTITY;
import org.tmatesoft.svn.core.internal.util.jna.ISVNSecurityLibrary.SecBuffer;
import org.tmatesoft.svn.core.internal.util.jna.ISVNSecurityLibrary.SecBufferDesc;
import org.tmatesoft.svn.core.internal.util.jna.ISVNSecurityLibrary.SecHandle;
import org.tmatesoft.svn.core.internal.util.jna.ISVNSecurityLibrary.TimeStamp;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.WString;


/**
 * @version 1.1.2
 * @author  TMate Software Ltd.
 */
public class SVNWinSecurity {

    public static boolean isNativeLibraryAvailable() {
        return JNALibraryLoader.getSecurityLibrary() != null;
    }
    
    public static SVNNTSecurityParameters getSecurityParams(String userName, String password, 
            String ntDomain) {
        SecHandle crdHandle = getCredentialsHandle(userName, password, ntDomain);
        if (crdHandle == null) {
            return null;
        }

        SVNNTSecurityParameters secParams = new SVNNTSecurityParameters();
        secParams.myUsername = userName;
        secParams.myPassword = password;
        secParams.myNTDomain = ntDomain;
        secParams.myState = 0;
        secParams.myCrdHandle = crdHandle;
        return secParams;
    }

    public static String getAuthHeader(String token, SVNNTSecurityParameters params) throws SVNException {
        byte[] input = null;
        if (token != null) {
            StringBuffer tokenBuffer = new StringBuffer(token);
            byte[] tmp = new byte[tokenBuffer.length()];
            StringBuffer sb = SVNBase64.normalizeBase64(tokenBuffer);
            int resultLength = SVNBase64.base64ToByteArray(sb, tmp);
            input = new byte[resultLength];
            System.arraycopy(tmp, 0, input, 0, resultLength);
        }
            
        byte[] nextTokenBytes = getNextToken(params, input);
        if (nextTokenBytes == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, 
                    "Internal authentication error");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }
        return SVNBase64.byteArrayToBase64(nextTokenBytes);
    }

    private static byte[] getNextToken(SVNNTSecurityParameters params, byte[] lastToken) {
        ISVNSecurityLibrary library = JNALibraryLoader.getSecurityLibrary();
        if (library == null) {
            return null;
        }

        SecHandle newContext = null;
        SecHandle pContext = params.myCtxHandle;
        if (pContext == null) {
            newContext = new SecHandle();
            newContext.dwLower = new NativeLong(0);
            newContext.dwUpper = new NativeLong(0);
            newContext.write();
            params.myCtxHandle = newContext;
        } else {
            newContext = pContext;
        }
        
        final int bufferSize = 8192;
        SecBuffer outSecBuffer = new SecBuffer();
        outSecBuffer.cbBuffer = new NativeLong(bufferSize);
        outSecBuffer.BufferType = new NativeLong(ISVNSecurityLibrary.SECBUFFER_TOKEN);
        outSecBuffer.pvBuffer = new Memory(bufferSize);
        outSecBuffer.write();
        
        SecBufferDesc outBufferDescription = new SecBufferDesc();
        outBufferDescription.ulVersion = new NativeLong(0);
        outBufferDescription.cBuffers = new NativeLong(1);
        outBufferDescription.pBuffers = outSecBuffer.getPointer();

        outBufferDescription.write();
        
        SecBufferDesc inBufferDescription = null;
        SecBuffer inSecBuffer = null;
        if (lastToken != null) {
            inSecBuffer = new SecBuffer();
            inSecBuffer.cbBuffer = new NativeLong(lastToken.length);
            inSecBuffer.BufferType = new NativeLong(ISVNSecurityLibrary.SECBUFFER_TOKEN);
            inSecBuffer.pvBuffer = new Memory(lastToken.length);
            inSecBuffer.pvBuffer.write(0, lastToken, 0, lastToken.length);
            inSecBuffer.write();
                
            inBufferDescription = new SecBufferDesc();
            inBufferDescription.ulVersion = new NativeLong(0);
            inBufferDescription.cBuffers = new NativeLong(1);
            inBufferDescription.pBuffers = inSecBuffer.getPointer();
            inBufferDescription.write();
        }
        
        Pointer contextAttributes = new Memory(NativeLong.SIZE);
        TimeStamp ltime = new TimeStamp();
        ltime.HighPart = new NativeLong(0);
        ltime.LowPart = new NativeLong(0);
        ltime.write();
        
        int securityStatus = library.InitializeSecurityContextW(params.myCrdHandle.getPointer(), 
                pContext != null ? pContext.getPointer() : Pointer.NULL, 
                        null, 
                        new NativeLong(0), 
                        new NativeLong(0), 
                        new NativeLong(ISVNSecurityLibrary.SECURITY_NATIVE_DREP), 
                        lastToken != null ? inBufferDescription.getPointer() : Pointer.NULL, 
                        new NativeLong(0), 
                        newContext.getPointer(), 
                        outBufferDescription.getPointer(), 
                        contextAttributes, 
                        ltime.getPointer());

        if (securityStatus < 0) {
            endSequence(params);
            return null;
        }

        newContext.read();
        params.myCtxHandle.read();
        
        if (securityStatus == ISVNSecurityLibrary.SEC_I_COMPLETE_NEEDED || 
                securityStatus == ISVNSecurityLibrary.SEC_I_COMPLETE_AND_CONTINUE) {
            outBufferDescription.read();
            securityStatus = library.CompleteAuthToken(params.myCtxHandle.getPointer(), 
                    outBufferDescription.getPointer());
            
            if (securityStatus < 0) {
                endSequence(params);
                return null;
            }
        }
        
        byte[] result = null;
        outBufferDescription.read();
        outSecBuffer.read();
        boolean sequenceIsEnded = false;
        if (outSecBuffer.cbBuffer.intValue() > 0) {
            result = outSecBuffer.pvBuffer.getByteArray(0, outSecBuffer.cbBuffer.intValue());
            if (lastToken != null) {
                endSequence(params);
                sequenceIsEnded = true;
            }
        }
        
        if (securityStatus != ISVNSecurityLibrary.SEC_I_CONTINUE_NEEDED && 
                securityStatus == ISVNSecurityLibrary.SEC_I_COMPLETE_AND_CONTINUE) {
            if (!sequenceIsEnded) {
                endSequence(params);
            }
        }
        return result;
    }

    private static SecHandle getCredentialsHandle(String user, String password, String domain) {
        ISVNSecurityLibrary library = JNALibraryLoader.getSecurityLibrary();
        if (library == null) {
            return null;
        }

        SEC_WINNT_AUTH_IDENTITY authIdentity = null; 
        if (user != null || password != null || domain != null) {
            authIdentity = new SEC_WINNT_AUTH_IDENTITY();
            if (user != null) {
                authIdentity.User = new WString(user);
                authIdentity.UserLength = new NativeLong(user.length());
            }
            
            if (password != null) {
                authIdentity.Password = new WString(password);
                authIdentity.PasswordLength = new NativeLong(password.length());
            }
            
            if (domain != null) {
                authIdentity.Domain = new WString(domain);
                authIdentity.DomainLength = new NativeLong(domain.length());
            }
            authIdentity.Flags = new NativeLong(ISVNSecurityLibrary.SEC_WINNT_AUTH_IDENTITY_UNICODE);
            authIdentity.write();
        }
        
        SecHandle pCred = new SecHandle();
        pCred.dwLower = new NativeLong(0);
        pCred.dwUpper = new NativeLong(0);
        pCred.write();

        ISVNSecurityLibrary.TimeStamp ltime = new ISVNSecurityLibrary.TimeStamp();
        ltime.HighPart = new NativeLong(0);
        ltime.LowPart = new NativeLong(0);
        ltime.write();
        
        int securityStatus = library.AcquireCredentialsHandleW(null, new WString("NTLM"), 
                new NativeLong(ISVNSecurityLibrary.SECPKG_CRED_OUTBOUND), Pointer.NULL, 
                authIdentity != null ? authIdentity.getPointer() : Pointer.NULL, 
                        Pointer.NULL, Pointer.NULL, pCred.getPointer(), ltime.getPointer());
        if (securityStatus == 0) {
            pCred.read();
            return pCred;
        }
        return null;
    }

    private static void endSequence(SVNNTSecurityParameters params) {
        ISVNSecurityLibrary library = JNALibraryLoader.getSecurityLibrary();
        if (library == null) {
            return;
        }

        if (params.myCrdHandle != null) {
            library.FreeCredentialsHandle(params.myCrdHandle.getPointer());
            params.myCrdHandle = null;
        }

        if (params.myCtxHandle != null) {
            library.DeleteSecurityContext(params.myCtxHandle.getPointer());
            params.myCtxHandle = null;
        }
    }
    
    public static class SVNNTSecurityParameters {
        public String myUsername;
        public String myPassword;
        public String myNTDomain;
        public int myState;
        public SecHandle myCrdHandle;
        public SecHandle myCtxHandle;
    }

}
