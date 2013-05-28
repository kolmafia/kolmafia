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

import java.io.File;

import org.tmatesoft.svn.core.internal.util.jna.ISVNWin32Library.HRESULT;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.WString;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class SVNWin32Util {
    
    public static boolean setWritable(File file) {
        if (file == null) {
            return false;
        }
        ISVNKernel32Library library = JNALibraryLoader.getKernelLibrary();
        if (library == null) {
            // use ugly way.
            return false;
        }
        synchronized (library) {
            try {
                int rc = library.SetFileAttributesW(new WString(file.getAbsolutePath()), new NativeLong(ISVNKernel32Library.FILE_ATTRIBUTE_NORMAL));
                return rc != 0;
            } catch (Throwable th) {
            }
        }
        return false;
    }

    public static boolean setHidden(File file) {
        if (file == null) {
            return false;
        }
        ISVNKernel32Library library = JNALibraryLoader.getKernelLibrary();
        if (library == null) {
            // use ugly way.
            return false;
        }
        synchronized (library) {
            try {
                int rc = library.SetFileAttributesW(new WString(file.getAbsolutePath()), 
                        new NativeLong(ISVNKernel32Library.FILE_ATTRIBUTE_HIDDEN));
                return rc != 0;
            } catch (Throwable th) {
            }
            return false;
        }
    }

    public static boolean moveFile(File src, File dst) {
        if (src == null || dst == null) {
            return false;
        }
        ISVNKernel32Library library = JNALibraryLoader.getKernelLibrary();
        if (library == null) {
            // use ugly way.
            return false;
        }
        if (dst.isFile() && !dst.canWrite()) {
            SVNFileUtil.setReadonly(dst, false);
            SVNFileUtil.setReadonly(src, true);
        }
        synchronized (library) {
            try {
                int rc = library.MoveFileExW(new WString(src.getAbsoluteFile().getAbsolutePath()), new WString(dst.getAbsoluteFile().getAbsolutePath()), new NativeLong(3));
                return rc != 0;
            } catch (Throwable th) {
            }
        }
        return false;
    }
    
    public static String getApplicationDataPath(boolean common) {
        ISVNWin32Library library = JNALibraryLoader.getWin32Library(); 
        if (library == null) {
            return null;
        }
        final char[] commonAppDataPath = new char[1024];
        int type = common ? ISVNWin32Library.CSIDL_COMMON_APPDATA : ISVNWin32Library.CSIDL_APPDATA;
        HRESULT result = library.SHGetFolderPathW(Pointer.NULL, type, Pointer.NULL, ISVNWin32Library.SHGFP_TYPE_CURRENT, commonAppDataPath);
        if (result == null || result.longValue() != 0) {
            return null;
        }
        int length = commonAppDataPath.length;
        for (int i = 0; i < commonAppDataPath.length; i++) {
            if (commonAppDataPath[i] == '\0') {
                length = i;
                break;
            }
        }
        String path = new String(commonAppDataPath, 0, length);
        path = path.replace(File.separatorChar, '/');
        return path;
        
    }

}
