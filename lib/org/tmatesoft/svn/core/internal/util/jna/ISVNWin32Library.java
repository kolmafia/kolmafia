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

import com.sun.jna.IntegerType;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public interface ISVNWin32Library extends StdCallLibrary {

    class DWORD extends IntegerType {
        
        private static final long serialVersionUID = 1L;
        
        public DWORD() { this(0); }
        public DWORD(long value) { super(4, value); } 
    }
    
    class HRESULT extends NativeLong {

        private static final long serialVersionUID = 1L;
    }

    DWORD SHGFP_TYPE_CURRENT = new DWORD(0);         
    DWORD SHGFP_TYPE_DEFAULT = new DWORD(1);

    int CSIDL_APPDATA = 0x001a;
    int CSIDL_COMMON_APPDATA = 0x0023;

    HRESULT SHGetFolderPathW(Pointer hwndOwner, int nFolder, Pointer hToken, DWORD dwFlags, char[] pszPath);

}
