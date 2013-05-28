package org.tmatesoft.svn.core.internal.util.jna;
import com.sun.jna.Library;
import com.sun.jna.Pointer;


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

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
interface ISVNCLibrary extends Library {
    
    public int chmod(String filename, int mode);
    
    public int readlink(String filename, Pointer linkname, int linkNameSize);

    public int __lxstat64(int ver, String path, Pointer stat);

    public int lstat(String path, Pointer stat);

    public int _lstat(String path, Pointer stat);

    public int __xstat64(int ver, String path, Pointer stat);

    public int _stat(String path, Pointer stat);

    public int stat(String path, Pointer stat);

    public int symlink(String targetPath, String linkPath);
    
    public int getuid();
    
    public int getgid();
    
}
