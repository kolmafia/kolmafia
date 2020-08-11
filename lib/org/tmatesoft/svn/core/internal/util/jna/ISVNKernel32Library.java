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

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

import java.util.Arrays;
import java.util.List;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
interface ISVNKernel32Library extends StdCallLibrary {
    
    public static class OSVERSIONINFO extends Structure {
        public WinDef.DWORD dwOSVersionInfoSize;
        public WinDef.DWORD dwMajorVersion;
        public WinDef.DWORD dwMinorVersion;
        public WinDef.DWORD dwBuildNumber;
        public WinDef.DWORD dwPlatformId;
        public char[] szCSDVersion; 
        
        public OSVERSIONINFO() {
            dwMajorVersion = new WinDef.DWORD(0);
            dwMinorVersion = new WinDef.DWORD(0);
            dwBuildNumber = new WinDef.DWORD(0);
            dwPlatformId = new WinDef.DWORD(0);
            szCSDVersion = new char[128];
            for (int i = 0; i < szCSDVersion.length; i++) {
                szCSDVersion[i] = 0;
            }
            dwOSVersionInfoSize = new WinDef.DWORD(size());
        }
        
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwOSVersionInfoSize", "dwMajorVersion", "dwMinorVersion", "dwBuildNumber", "dwPlatformId", "szCSDVersion");
        }

    }

    public long FILE_ATTRIBUTE_READONLY = 0x01;
    public long FILE_ATTRIBUTE_HIDDEN   = 0x02;
    public long FILE_ATTRIBUTE_NORMAL   = 0x80;
    
    public int VER_PLATFORM_WIN32_WINDOWS = 1;
    public int VER_PLATFORM_WIN32_NT = 2;
    
    public Pointer LocalFree(Pointer ptr);
    
    public int SetFileAttributesW(WString path, NativeLong attrs);

    public int MoveFileW(WString src, WString dst);

    public int MoveFileExW(WString src, WString dst, NativeLong flags);
    
    public int GetVersionExW(Pointer pInfo);
    
    public int GetLastError();
}
