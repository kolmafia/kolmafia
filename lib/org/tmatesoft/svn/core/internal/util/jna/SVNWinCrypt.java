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

import java.io.UnsupportedEncodingException;

import org.tmatesoft.svn.core.internal.util.SVNBase64;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

import com.sun.jna.NativeLong;
import com.sun.jna.WString;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class SVNWinCrypt {

    public static String decrypt(String encryptedData) {
        if (encryptedData == null) {
            return null;
        }
        ISVNWinCryptLibrary library = JNALibraryLoader.getWinCryptLibrary();
        if (library == null) {
            return null;
        }
        byte[] buffer = new byte[encryptedData.length()];
        StringBuffer sb = SVNBase64.normalizeBase64(new StringBuffer(encryptedData));
        int decodedBytes = SVNBase64.base64ToByteArray(sb, buffer);
        byte[] decodedBuffer = new byte[decodedBytes];
        System.arraycopy(buffer, 0, decodedBuffer, 0, decodedBytes);
        
        ISVNWinCryptLibrary.DATA_BLOB dataIn = null; 
        ISVNWinCryptLibrary.DATA_BLOB dataOut = null;
        try {
            dataIn = new ISVNWinCryptLibrary.DATA_BLOB(decodedBuffer);
            dataIn.write();
            dataOut = new ISVNWinCryptLibrary.DATA_BLOB(null);
            dataOut.write();
            synchronized (library) {
                boolean ok = library.CryptUnprotectData(
                        dataIn.getPointer(), 
                        null, null, null, null, new NativeLong(1), 
                        dataOut.getPointer());
                if (!ok) {
                    return encryptedData;
                }
                dataOut.read();
            }
            if (dataOut.cbSize == null || dataOut.cbSize.intValue() < 0) {
                return null;
            }
            byte[] decryptedData = new byte[dataOut.cbSize.intValue()];
            dataOut.cbData.read(0, decryptedData, 0, decryptedData.length);
            return new String(decryptedData, 0, decryptedData.length, "UTF-8");
        } catch (Throwable th) {
            return null;
        } finally {
            ISVNKernel32Library kernel = JNALibraryLoader.getKernelLibrary();
            if (kernel != null) {
                try {
                    synchronized (kernel) {
                        if (dataOut != null) {
                            kernel.LocalFree(dataOut.cbData); 
                        }
                    }
                } catch (Throwable th) {
                    //
                }
            }
        }
    }
    
    public static String encrypt(String rawData) {
        if (rawData == null) {
            return null;
        }
        ISVNWinCryptLibrary library = JNALibraryLoader.getWinCryptLibrary();
        if (library == null) {
            return rawData;
        }
        ISVNWinCryptLibrary.DATA_BLOB dataIn = null;
        ISVNWinCryptLibrary.DATA_BLOB dataOut = null;
        try {
            try {
                dataIn = new ISVNWinCryptLibrary.DATA_BLOB(rawData.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return rawData;
            }
            dataIn.write();
            dataOut = new ISVNWinCryptLibrary.DATA_BLOB(null);
            dataOut.write();
            
            synchronized (library) {
                boolean ok = library.CryptProtectData(
                        dataIn.getPointer(), 
                        new WString("auth_svn.simple.wincrypt"), 
                        null, null, null, new NativeLong(1), 
                        dataOut.getPointer());
                if (!ok) {
                    return rawData;
                }
                dataOut.read();
            }
            if (dataOut.cbSize == null || dataOut.cbSize.intValue() <= 0) {
                return rawData;
            }
            byte[] encryptedData = new byte[dataOut.cbSize.intValue()];
            dataOut.cbData.read(0, encryptedData, 0, encryptedData.length);
            return SVNBase64.byteArrayToBase64(encryptedData);
        } catch (Throwable th) {
            return rawData;
        } finally {
            ISVNKernel32Library kernel = JNALibraryLoader.getKernelLibrary();
            if (kernel != null) {
                try {
                    synchronized (kernel) {
                        if (dataOut != null) {
                            kernel.LocalFree(dataOut.cbData); 
                        }
                    }
                } catch (Throwable th) {
                }
            }
        }
    }

    public synchronized static boolean isEnabled() {
        return SVNFileUtil.isWindows && JNALibraryLoader.getWinCryptLibrary() != null;
    }
}