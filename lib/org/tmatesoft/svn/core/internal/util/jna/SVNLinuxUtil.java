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

import com.sun.jna.Memory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNLinuxUtil {

    private static Memory ourSharedMemory;
    private static final boolean ourIsDashStat = Boolean.getBoolean("svnkit.jna.dash_stat");

    static {
        try {
            ourSharedMemory = new Memory(1024);
        } catch (Throwable th) {
            ourSharedMemory = null;
        }
    }

    public static SVNFileType getFileType(File file) {
        if (file == null || ourSharedMemory == null) {
            return null;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            ISVNCLibrary cLibrary = JNALibraryLoader.getCLibrary();
            if (cLibrary == null) {
                return null;
            }
            synchronized (ourSharedMemory) {
                ourSharedMemory.clear();
                int rc;
                synchronized (cLibrary) {
                    if (ourIsDashStat && SVNFileUtil.isBSD) {
                        rc = cLibrary._lstat(path, ourSharedMemory);
                    } else {
                        rc = SVNFileUtil.isOSX || SVNFileUtil.isBSD || SVNFileUtil.isSolaris ?
                            cLibrary.lstat(path, ourSharedMemory) :
                            cLibrary.__lxstat64(0, path, ourSharedMemory);
                    }
                }
                if (rc < 0) {
                    if (file.exists() || file.isDirectory() || file.isFile()) {
                        return null;
                    }
                    return SVNFileType.NONE;
                }
                int mode = SVNFileUtil.isOSX || SVNFileUtil.isBSD || SVNFileUtil.isSolaris ?
                        ourSharedMemory.getShort(getFileModeOffset()) : ourSharedMemory.getInt(getFileModeOffset());
                int type = mode & 0170000;
                if (type == 0120000) {
                    return SVNFileType.SYMLINK;
                } else if (type == 0040000) {
                    return SVNFileType.DIRECTORY;
                } else if (type == 0100000) {
                    return SVNFileType.FILE;
                } else {
                    if (file.exists() || file.isDirectory() || file.isFile()) {
                        return null;
                    }
                    return SVNFileType.NONE;
                }
            }
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, th);
            //
        }
        return null;
    }

    public static Boolean isExecutable(File file) {
        if (file == null || ourSharedMemory == null) {
            return null;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            ISVNCLibrary cLibrary = JNALibraryLoader.getCLibrary();
            if (cLibrary == null) {
                return null;
            }
            synchronized (ourSharedMemory) {
                ourSharedMemory.clear();
                int rc;
                synchronized (cLibrary) {
                    if (ourIsDashStat && SVNFileUtil.isBSD) {
                        rc = cLibrary._lstat(path, ourSharedMemory);
                    } else {
                        rc = SVNFileUtil.isOSX || SVNFileUtil.isBSD || SVNFileUtil.isSolaris ?
                            cLibrary.lstat(path, ourSharedMemory) :
                            cLibrary.__lxstat64(0, path, ourSharedMemory);
                    }
                }
                if (rc < 0) {
                    return null;
                }

                int mode = ourSharedMemory.getInt(getFileModeOffset());
                int fuid = ourSharedMemory.getInt(getFileUserIDOffset());
                int fgid = ourSharedMemory.getInt(getFileGroupIDOffset());

                int access = mode & 0777;
                int mask = 0111;
                if (JNALibraryLoader.getUID() == fuid) {
                    mask = 0100; // check user
                } else if (JNALibraryLoader.getGID() == fgid) {
                    mask = 0010; // check group
                } else {
                    mask = 0001; // check other.
                }
                return Boolean.valueOf((access & mask) != 0);
            }
        } catch (Throwable th) {
            //
        }
        return null;
    }

    public static String getLinkTarget(File file) {
        if (file == null || ourSharedMemory == null) {
            return null;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            ISVNCLibrary cLibrary = JNALibraryLoader.getCLibrary();
            if (cLibrary == null) {
                return null;
            }
            synchronized (ourSharedMemory) {
                ourSharedMemory.clear();
                int rc;
                synchronized (cLibrary) {
                    rc = cLibrary.readlink(path, ourSharedMemory, 1024);
                }
                if (rc <= 0) {
                    return null;
                }
                byte[] buffer = new byte[rc];
                ourSharedMemory.read(0, buffer, 0, rc);
                // intentionally read in system default encoding.
                return new String(buffer, 0, rc);
            }
        } catch (Throwable th) {
            //
        }
        return null;
    }


    public static Long getSymlinkLastModified(File file) {
        if (file == null || ourSharedMemory == null) {
            return null;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            ISVNCLibrary cLibrary = JNALibraryLoader.getCLibrary();
            if (cLibrary == null) {
                return null;
            }
            synchronized (ourSharedMemory) {
                ourSharedMemory.clear();
                int rc;
                synchronized (cLibrary) {
                    if (ourIsDashStat && SVNFileUtil.isBSD) {
                        rc = cLibrary._lstat(path, ourSharedMemory);
                    } else {
                        rc = SVNFileUtil.isOSX || SVNFileUtil.isBSD || SVNFileUtil.isSolaris ?
                            cLibrary.lstat(path, ourSharedMemory) :
                            cLibrary.__lxstat64(0, path, ourSharedMemory);
                    }
                }
                if (rc < 0) {
                    return null;
                }

                return ourSharedMemory.getLong(getFileLastModifiedOffset()) * 1000;
            }
        } catch (Throwable th) {
            //
        }
        return null;
    }

    public static boolean setExecutable(File file, boolean set) {
        if (file == null || ourSharedMemory == null) {
            return false;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            ISVNCLibrary cLibrary = JNALibraryLoader.getCLibrary();
            if (cLibrary == null) {
                return false;
            }
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, "Calling JNA.setExecutable");
            synchronized (ourSharedMemory) {
                ourSharedMemory.clear();
                int rc;
                synchronized (cLibrary) {
                    if (ourIsDashStat && SVNFileUtil.isBSD) {
                        rc = cLibrary._lstat(path, ourSharedMemory);
                    } else {
                        rc = SVNFileUtil.isOSX || SVNFileUtil.isBSD || SVNFileUtil.isSolaris ?
                            cLibrary.lstat(path, ourSharedMemory) :
                            cLibrary.__lxstat64(0, path, ourSharedMemory);
                    }
                }
                if (rc < 0) {
                    return false;
                }

                int mode = ourSharedMemory.getInt(getFileModeOffset());
                int access = mode & 0777;
                int perms = access;
                if (set) {
                    if ((access & 0400) != 0)
                        perms |= 0100;
                    if ((access & 0040) != 0)
                        perms |= 0010;
                    if ((access & 0004) != 0)
                        perms |= 0001;
                } else {
                    if ((access & 0400) != 0)
                        perms &= ~0100;
                    if ((access & 0040) != 0)
                        perms &= ~0010;
                    if ((access & 0004) != 0)
                        perms &= ~0001;
                }
                synchronized (cLibrary) {
                    rc = cLibrary.chmod(path, perms);
                }
                return rc < 0 ? false : true;
            }
        } catch (Throwable th) {
            //
        }
        return false;
    }

    public static boolean setWritable(File file) {
        if (file == null || ourSharedMemory == null) {
            return false;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            ISVNCLibrary cLibrary = JNALibraryLoader.getCLibrary();
            if (cLibrary == null) {
                return false;
            }
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, "Calling JNA.setWritable");
            synchronized (ourSharedMemory) {
                ourSharedMemory.clear();
                int rc;
                synchronized (cLibrary) {
                    if (ourIsDashStat && SVNFileUtil.isBSD) {
                        rc = cLibrary._lstat(path, ourSharedMemory);
                    } else {
                        rc = SVNFileUtil.isOSX || SVNFileUtil.isBSD || SVNFileUtil.isSolaris ?
                            cLibrary.lstat(path, ourSharedMemory) :
                            cLibrary.__lxstat64(0, path, ourSharedMemory);
                    }
                }
                if (rc < 0) {
                    return false;
                }

                int mode = ourSharedMemory.getInt(getFileModeOffset());
                int access = mode & 0777;
                int mask = 0;
                if ((access & 0400) != 0) {
                    mask |= 0200;
                }
                if ((access & 0040) != 0) {
                    mask |= 0020;
                }
                if ((access & 0004) != 0) {
                    mask |= 0002;
                }
                if (mask == 0) {
                    return false;
                }
                synchronized (cLibrary) {
                    rc = cLibrary.chmod(path, mask | access);
                }
                return rc < 0 ? false : true;
            }
        } catch (Throwable th) {
            //
        }
        return false;
    }

    public static boolean setSGID(File file) {
        if (file == null || ourSharedMemory == null) {
            return false;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            ISVNCLibrary cLibrary = JNALibraryLoader.getCLibrary();
            if (cLibrary == null) {
                return false;
            }
            synchronized (ourSharedMemory) {
                ourSharedMemory.clear();
                int rc;
                synchronized (cLibrary) {
                    if (ourIsDashStat && SVNFileUtil.isBSD) {
                        rc = cLibrary._stat(path, ourSharedMemory);
                    } else {
                        rc = SVNFileUtil.isOSX || SVNFileUtil.isBSD || SVNFileUtil.isSolaris ?
                            cLibrary.stat(path, ourSharedMemory) :
                            cLibrary.__xstat64(0, path, ourSharedMemory);
                    }
                }
                if (rc < 0) {
                    return false;
                }

                int mode = ourSharedMemory.getInt(getFileModeOffset());
                int access = mode & 07777;
                int mask = 02000;
                if ((access & mask) != 0) {
                    return false;
                }
                synchronized (cLibrary) {
                    rc = cLibrary.chmod(path, mask | access);
                }
                return rc < 0 ? false : true;
            }
        } catch (Throwable th) {
            //
        }
        return false;
    }

    public static boolean createSymlink(File file, String linkName) {
        if (file == null || linkName == null || ourSharedMemory == null) {
            return false;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        try {
            ISVNCLibrary cLibrary = JNALibraryLoader.getCLibrary();
            if (cLibrary == null) {
                return false;
            }
            int rc;
            synchronized (cLibrary) {
                rc = cLibrary.symlink(linkName, path);
            }
            return rc < 0 ? false : true;
        } catch (Throwable th) {
            //
        }
        return false;
    }

    private static int getFileModeOffset() {
        if (SVNFileUtil.isLinux && SVNFileUtil.is64Bit) {
            return 24;
        }
        if (SVNFileUtil.isLinux && SVNFileUtil.is32Bit) {
            return 16;
        }
        if (SVNFileUtil.isOSX) {
            return 8;
        }
        if (SVNFileUtil.isSolaris && SVNFileUtil.is64Bit) {
            return 16;
        }
        if (SVNFileUtil.isSolaris && SVNFileUtil.is32Bit) {
            return 20;
        }
        if (SVNFileUtil.isBSD) {
            return 8;
        }
        return 16;
    }

    private static int getFileUserIDOffset() {
        int modeOffset = getFileModeOffset();
        if (SVNFileUtil.isLinux && SVNFileUtil.is64Bit) {
            return modeOffset + 4;
        }
        if (SVNFileUtil.isLinux && SVNFileUtil.is32Bit) {
            return modeOffset + 8;
        }
        if (SVNFileUtil.isOSX) {
            return modeOffset + 4;
        }
        if (SVNFileUtil.isSolaris) {
            return modeOffset + 8;
        }
        if (SVNFileUtil.isBSD) {
            return modeOffset + 4;
        }

        return modeOffset + 8;
    }

    private static int getFileGroupIDOffset() {
        return getFileUserIDOffset() + 4;
    }

    private static int getFileLastModifiedOffset() {
        if (SVNFileUtil.isLinux && SVNFileUtil.is64Bit) {
            return 88;
        }
        if (SVNFileUtil.isLinux && SVNFileUtil.is32Bit) {
            return 64;
        }
        if (SVNFileUtil.isBSD) {
            return 32;
        }
        if (SVNFileUtil.isSolaris) {
            //64bit
            return 64;
        }
        if (SVNFileUtil.isOSX) {
            return 48;
        }
        return 88;
    }
    
}