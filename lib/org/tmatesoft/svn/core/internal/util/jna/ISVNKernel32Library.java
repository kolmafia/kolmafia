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
 * @version 1.3
 * @author  TMate Software Ltd.
 */
interface ISVNKernel32Library extends StdCallLibrary {
    
    public static class OSVERSIONINFO extends Structure {
        public NativeLong dwOSVersionInfoSize;
        public NativeLong dwMajorVersion;
        public NativeLong dwMinorVersion;
        public NativeLong dwBuildNumber;
        public NativeLong dwPlatformId;
        public char[] szCSDVersion; 
        
        public OSVERSIONINFO() {
            dwMajorVersion = new NativeLong(0);
            dwMinorVersion = new NativeLong(0);
            dwBuildNumber = new NativeLong(0);
            dwPlatformId = new NativeLong(0);
            szCSDVersion = new char[128];
            for (int i = 0; i < szCSDVersion.length; i++) {
                szCSDVersion[i] = 0;
            }
            dwOSVersionInfoSize = new NativeLong(this.size());
        }
        
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwMajorVersion", 
                    "dwMinorVersion", 
                    "dwBuildNumber", 
                    "dwPlatformId", 
                    "szCSDVersion", 
                    "dwOSVersionInfoSize");
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
}
