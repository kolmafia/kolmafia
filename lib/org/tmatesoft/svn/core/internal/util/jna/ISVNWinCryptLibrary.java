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

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
interface ISVNWinCryptLibrary extends StdCallLibrary {

    public static class DATA_BLOB extends Structure {
        
        public DATA_BLOB(byte[] bytes) {
            if (bytes != null) {
                int allocationSize = Math.max(1, bytes.length);
                cbData = new Memory(allocationSize);
                cbData.write(0, bytes, 0, bytes.length);
                cbSize = new NativeLong(bytes.length);
            } else {
                cbSize = new NativeLong(0);
                cbData = Pointer.NULL;
            }
        }
        
        public NativeLong cbSize;
        public Pointer cbData;
        
        protected List<String> getFieldOrder() {
            return Arrays.asList("cbSize", "cbData");
        }
    }
    
    public boolean CryptProtectData(Pointer dataIn, 
            WString description, 
            Pointer entropy, 
            Pointer reserved, 
            Pointer struct,
            NativeLong flags,
            Pointer out);

    public boolean CryptUnprotectData(Pointer dataIn, 
            Pointer description, 
            Pointer entropy, 
            Pointer reserved, 
            Pointer struct,
            NativeLong flags,
            Pointer out);
}
