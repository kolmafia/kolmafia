/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNFormatUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNUUIDGenerator;
import org.tmatesoft.svn.core.internal.util.jna.SVNJNAUtil;
import org.tmatesoft.svn.core.internal.util.jna.SVNOS2Util;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * @version 1.3
 * @author TMate Software Ltd., Peter Skoog
 */
public class SVNFileUtil {

    private static final String ID_COMMAND;
    private static final String LN_COMMAND;
    public static final String LS_COMMAND;
    private static final String CHMOD_COMMAND;
    private static final String ATTRIB_COMMAND;
    private static final String ENV_COMMAND;
    private static final String STAT_COMMAND;

    public static final boolean logNativeCalls;
    public static final boolean isWindows;
    public static final boolean isOS2;
    public static final boolean isOSX;
    public static final boolean isBSD;
    public static boolean isLinux;
    public static final boolean isSolaris;
    public static final boolean isOpenVMS;

    public static final boolean is32Bit;
    public static final boolean is64Bit;

    public static final int STREAM_CHUNK_SIZE = 16384;
    private static final int FILE_CREATION_ATTEMPTS_COUNT;

    public final static OutputStream DUMMY_OUT = new OutputStream() {

        public void write(int b) throws IOException {
        }
    };
    public final static InputStream DUMMY_IN = new InputStream() {

        public int read() throws IOException {
            return -1;
        }
    };

    private static boolean ourUseUnsafeCopyOnly = Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty("svnkit.no.safe.copy", System.getProperty("javasvn.no.safe.copy", "false")));
    private static boolean ourCopyOnSetWritable = Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty("svnkit.fast.setWritable", "true"));
    private static boolean ourUseNIOCopying = Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty("svnkit.nio.copy", "true"));

    private static String nativeEOLMarker;
    private static String ourGroupID;
    private static String ourUserID;
    private static File ourAppDataPath;
    private static String ourAdminDirectoryName;
    private static File ourSystemAppDataPath;

    private static Method ourSetWritableMethod;
    private static Method ourSetExecutableMethod;

    private static volatile boolean ourIsSleepForTimeStamp = true;

    public static final String BINARY_MIME_TYPE = "application/octet-stream";

    static {
        final String logNativeCallsString = System.getProperty("svnkit.log.native.calls", "false");
        logNativeCalls = logNativeCallsString == null ? false : Boolean.parseBoolean(logNativeCallsString);

        String retryCountStr = System.getProperty("svnkit.fs.win32_retry_count", "100");
        int retryCount = -1;
        try {
            retryCount = Integer.parseInt(retryCountStr);
        } catch (NumberFormatException nfe) {
            retryCount = -1;
        }
        if (retryCount < 0) {
            retryCount = 100;
        }
        FILE_CREATION_ATTEMPTS_COUNT = retryCount;

        String osName = System.getProperty("os.name");
        String osNameLC = osName == null ? null : osName.toLowerCase();

        boolean windows = osName != null && osNameLC.indexOf("windows") >= 0;
        if (!windows && osName != null) {
            windows = osNameLC.indexOf("os/2") >= 0;
            isOS2 = windows;
        } else {
            isOS2 = false;
        }

        isWindows = windows;
        isOSX = osName != null && (osNameLC.indexOf("mac") >= 0 || osNameLC.indexOf("darwin") >= 0);
        isLinux = osName != null && (osNameLC.indexOf("linux") >= 0 || osNameLC.indexOf("hp-ux") >= 0);
        isBSD = !isLinux && osName != null && osNameLC.indexOf("bsd") >= 0;
        isSolaris = !isLinux && !isBSD && osName != null && (osNameLC.indexOf("solaris") >= 0 || osNameLC.indexOf("sunos") >= 0);
        isOpenVMS = !isOSX && osName != null && osNameLC.indexOf("openvms") >= 0;

        if (!isWindows && !isOSX && !isLinux && !isBSD && !isSolaris && !isOpenVMS && !isOS2) {
            // fallback to some default.
            isLinux = true;
        }

        is32Bit = "32".equals(System.getProperty("sun.arch.data.model", "32"));
        is64Bit = "64".equals(System.getProperty("sun.arch.data.model", "64"));

        if (isOpenVMS) {
            setAdminDirectoryName("_svn");
        }
        String prefix = "svnkit.program.";

        Properties props = new Properties();
        InputStream is = SVNFileUtil.class.getResourceAsStream("/svnkit.runtime.properties");
        if (is != null) {
            try {
                props.load(is);
            } catch (IOException e) {
            } finally {
                SVNFileUtil.closeFile(is);
            }
        }

        ID_COMMAND = props.getProperty(prefix + "id", "id");
        LN_COMMAND = props.getProperty(prefix + "ln", "ln");
        LS_COMMAND = props.getProperty(prefix + "ls", "ls");
        CHMOD_COMMAND = props.getProperty(prefix + "chmod", "chmod");
        ATTRIB_COMMAND = props.getProperty(prefix + "attrib", "attrib");
        ENV_COMMAND = props.getProperty(prefix + "env", "env");
        STAT_COMMAND = props.getProperty(prefix + "stat", "stat");

        try {
            ourSetWritableMethod = File.class.getMethod("setWritable", new Class[] {
                Boolean.TYPE
            });
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }

        try {
            ourSetExecutableMethod = File.class.getMethod("setExecutable", new Class[] {
                Boolean.TYPE, Boolean.TYPE,
            });
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    public static boolean isCaseInsensitiveFS() {
        return isWindows || isOS2;
    }

    public static synchronized boolean useUnsafeCopyOnly() {
        return ourUseUnsafeCopyOnly;
    }

    public static synchronized void setUseUnsafeCopyOnly(boolean useUnsafeCopyOnly) {
        ourUseUnsafeCopyOnly = useUnsafeCopyOnly;
    }

    public static synchronized boolean useCopyOnSetWritable() {
        return ourCopyOnSetWritable;
    }

    public static synchronized void setUseCopyOnSetWritable(boolean useCopyOnSetWritable) {
        ourCopyOnSetWritable = useCopyOnSetWritable;
    }

    public static synchronized boolean useNIOCopying() {
        return ourUseNIOCopying;
    }

    public static synchronized void setUseNIOCopying(boolean useNIOCopy) {
        ourUseNIOCopying = useNIOCopy;
    }

    public static String getIdCommand() {
        return ID_COMMAND;
    }

    public static String getLnCommand() {
        return LN_COMMAND;
    }

    public static String getLsCommand() {
        return LS_COMMAND;
    }

    public static String getChmodCommand() {
        return CHMOD_COMMAND;
    }

    public static String getAttribCommand() {
        return ATTRIB_COMMAND;
    }

    public static String getEnvCommand() {
        return ENV_COMMAND;
    }

    public static String getStatCommand() {
        return STAT_COMMAND;
    }

    public static File getParentFile(File file) {
        if (file == null) {
            return null;
        }
        String path = file.getAbsolutePath();
        path = path.replace(File.separatorChar, '/');
        path = SVNPathUtil.canonicalizePath(path);
        int up = 0;
        while (path.endsWith("/..")) {
            path = SVNPathUtil.removeTail(path);
            up++;
        }
        for (int i = 0; i < up; i++) {
            path = SVNPathUtil.removeTail(path);
        }
        path = path.replace('/', File.separatorChar);
        file = new File(path);
        return file.getParentFile();
    }

    public static String readFile(File file) throws SVNException {
        InputStream is = null;
        try {
            is = openFileForReading(file, SVNLogType.WC);
            return readFile(is);
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read from file ''{0}'': {1}", new Object[] {
                    file, ioe.getMessage()
            });
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        } finally {
            closeFile(is);
        }
        return null;
    }

    public static String readFile(InputStream input) throws IOException {
        byte[] buf = new byte[STREAM_CHUNK_SIZE];
        StringBuffer result = new StringBuffer();
        int r = -1;
        while ((r = input.read(buf)) != -1) {
            if (r == 0) {
                continue;
            }
            result.append(new String(buf, 0, r, "UTF-8"));
        }
        return result.toString();
    }

    public static int readIntoBuffer(InputStream is, byte[] buff, int off, int len) throws IOException {
        int read = 0;
        while (len > 0) {
            int r = is.read(buff, off + read, len);
            if (r < 0) {
                if (read == 0) {
                    read = -1;
                }
                break;
            }

            read += r;
            len -= r;
        }
        return read;
    }

    public static String getBasePath(File file) {
        File base = SVNFileUtil.getFileDir(file);
        while (base != null) {
            if (base.isDirectory()) {
                File adminDir = new File(base, getAdminDirectoryName());
                if (adminDir.exists() && adminDir.isDirectory()) {
                    break;
                }
            }
            base = SVNFileUtil.getFileDir(base);
        }
        String path = file.getAbsolutePath();
        if (base != null) {
            path = path.substring(base.getAbsolutePath().length());
        }
        path = path.replace(File.separatorChar, '/');
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public static void createEmptyFile(File file) throws SVNException {
        boolean created;
        if (file != null && SVNFileUtil.getFileDir(file) != null && !SVNFileUtil.getFileDir(file).exists()) {
            SVNFileUtil.getFileDir(file).mkdirs();
        }

        IOException ioError = null;
        try {
            created = createNewFile(file);
        } catch (IOException ioe) {
            created = false;
            ioError = ioe;
        }
        if (!created) {
            SVNErrorMessage err = null;
            if (ioError != null) {
                err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot create new file ''{0}'': {1}", new Object[] {
                        file, ioError.getMessage()
                });
            } else {
                err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot create new file ''{0}''", file);
            }
            SVNErrorManager.error(err, ioError != null ? ioError : new Exception(), Level.FINE, SVNLogType.WC);
        }
    }

    public static boolean createNewFile(File file) throws IOException {
        if (file == null) {
            return false;
        }
        boolean created = false;
        int count = SVNFileUtil.isWindows ? FILE_CREATION_ATTEMPTS_COUNT : 1;
        long sleep = 1;
        while (!created && (count > 0)) {
            IOException ioError = null;
            try {
                created = file.createNewFile();
            } catch (IOException e) {
                ioError = e;
            }
            if (ioError != null) {
                if (count == 1) {
                    throw ioError;
                }
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ie) {
                    SVNDebugLog.getDefaultLog().log(SVNLogType.DEFAULT, ie, Level.FINEST);
                }
                if (sleep < 128) {
                    sleep = sleep * 2;
                }
            }

            if (ioError == null && !created) {
                return false;
            }
            count--;
        }
        return created;
    }

    /**
     * An internal method for ASCII bytes to write only!
     *
     * @param file
     * @param contents
     * @throws SVNException
     */
    public static void createFile(File file, String contents, String charSet) throws SVNException {
        createEmptyFile(file);
        if (contents == null || contents.length() == 0) {
            return;
        }

        OutputStream os = null;
        try {
            os = SVNFileUtil.openFileForWriting(file);
            if (charSet != null) {
                os.write(contents.getBytes(charSet));
            } else {
                os.write(contents.getBytes());
            }
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot write to file ''{0}'': {1}", new Object[] {
                    file, ioe.getMessage()
            });
            SVNErrorManager.error(err, ioe, Level.FINE, SVNLogType.DEFAULT);
        } catch (SVNException svne) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot write to file ''{0}''", file);
            SVNErrorManager.error(err, svne, Level.FINE, SVNLogType.DEFAULT);
        } finally {
            SVNFileUtil.closeFile(os);
        }
    }

    public static void writeToFile(File file, String contents, String charSet) throws SVNException {
        if (contents == null || contents.length() == 0) {
            return;
        }

        OutputStream os = null;
        try {
            os = SVNFileUtil.openFileForWriting(file);
            if (charSet != null) {
                os.write(contents.getBytes(charSet));
            } else {
                os.write(contents.getBytes());
            }
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot write to file ''{0}'': {1}", new Object[] {
                    file, ioe.getMessage()
            });
            SVNErrorManager.error(err, ioe, Level.FINE, SVNLogType.DEFAULT);
        } catch (SVNException svne) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot write to file ''{0}''", file);
            SVNErrorManager.error(err, svne, Level.FINE, SVNLogType.DEFAULT);
        } finally {
            SVNFileUtil.closeFile(os);
        }
    }

    public static void writeVersionFile(File file, int version) throws SVNException {
        if (version < 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "Version {0} is not non-negative", new Integer(version));
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }

        String contents = version + "\n";
        File tmpFile = SVNFileUtil.createUniqueFile(SVNFileUtil.getFileDir(file), SVNFileUtil.getFileName(file), ".tmp", false);
        OutputStream os = null;

        try {
            os = SVNFileUtil.openFileForWriting(tmpFile);
            os.write(contents.getBytes("US-ASCII"));
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        } finally {
            SVNFileUtil.closeFile(os);
        }
        if (isWindows) {
            setReadonly(file, false);
        }
        SVNFileUtil.rename(tmpFile, file);
        setReadonly(file, true);
    }

    public static synchronized File createUniqueFile(File parent, String name, String suffix, boolean useUUIDGenerator) throws SVNException {
        StringBuffer fileName = new StringBuffer();
        fileName.append(name);
        if (useUUIDGenerator) {
            fileName.append(".");
            fileName.append(SVNUUIDGenerator.generateUUIDString());
        }
        fileName.append(suffix);
        File file = new File(parent, fileName.toString());
        int i = 2;
        do {
            if (SVNFileType.getType(file) == SVNFileType.NONE) {
                createEmptyFile(file);
                return file;
            }
            fileName.setLength(0);
            fileName.append(name);
            fileName.append(".");
            if (useUUIDGenerator) {
                fileName.append(SVNUUIDGenerator.generateUUIDString());
            } else {
                fileName.append(i);
            }
            fileName.append(suffix);
            file = new File(parent, fileName.toString());
            i++;
        } while (i < 99999);

        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_UNIQUE_NAMES_EXHAUSTED, "Unable to make name for ''{0}''", new File(parent, name));
        SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        return null;
    }
    
    public static synchronized File createUniqueDir(File parent, String name, String suffix, boolean useUUIDGenerator) throws SVNException {
        StringBuffer fileName = new StringBuffer();
        fileName.append(name);
        if (useUUIDGenerator) {
            fileName.append(".");
            fileName.append(SVNUUIDGenerator.generateUUIDString());
        }
        fileName.append(suffix);
        File file = new File(parent, fileName.toString());
        int i = 2;
        do {
            if (SVNFileType.getType(file) == SVNFileType.NONE) {
                file.mkdir();
                return file;
            }
            fileName.setLength(0);
            fileName.append(name);
            fileName.append(".");
            if (useUUIDGenerator) {
                fileName.append(SVNUUIDGenerator.generateUUIDString());
            } else {
                fileName.append(i);
            }
            fileName.append(suffix);
            file = new File(parent, fileName.toString());
            i++;
        } while (i < 99999);
        

        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_UNIQUE_NAMES_EXHAUSTED, "Unable to make name for ''{0}''", new File(parent, name));
        SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        return null;
    }
    
    public static void moveFile(File src, File dst) throws SVNException {
    	
    	File tmpPath = SVNFileUtil.createUniqueFile(SVNFileUtil.getFileDir(dst), SVNFileUtil.getFileName(src), "tmp", false);
    	
    	try {
    		SVNFileUtil.copyFile(src, tmpPath, true);
    	}
    	catch (SVNException ex) {
    		try {
    		SVNFileUtil.deleteFile(tmpPath);
    		} catch (SVNException ex2) {}
    		throw ex;
    	}
    	
    	try {
    		SVNFileUtil.rename(tmpPath, dst);
    	}
    	catch (SVNException ex) {
    		try {
    			SVNFileUtil.deleteFile(tmpPath);
    		} catch (SVNException ex2) {}
    		throw ex;
    	}
    	
    	try {
    		SVNFileUtil.deleteFile(src);
    	}
    	catch (SVNException ex) {
    		try {
    		SVNFileUtil.deleteFile(dst);
    		} catch (SVNException ex2) {}
    		throw ex;
    	}
    }
    
    public static void moveDir(File src, File dst) throws SVNException {
    	File tmpPath = SVNFileUtil.createUniqueDir(SVNFileUtil.getFileDir(dst), SVNFileUtil.getFileName(src), "tmp", false);
    	
    	try {
    		SVNFileUtil.copyDirectory(src, tmpPath, false, null);
    	}
    	catch (SVNException ex) {
    		SVNFileUtil.deleteAll(tmpPath, true);
    		throw ex;
    	}
    	
    	try {
    		SVNFileUtil.rename(tmpPath, dst);
    	}
    	catch (SVNException ex) {
    		SVNFileUtil.deleteAll(tmpPath, true);
    		throw ex;
    	}
    	
    	try {
    		SVNFileUtil.deleteAll(src, true, null);
    	}
    	catch (SVNException ex) {
    		SVNFileUtil.deleteAll(dst, true);
    		throw ex;
    	}
    	
    	
    	
    }
    
    /*
    
      
          

         

          err = svn_io_remove_file2(from_path, FALSE, pool);
          if (! err)
            return SVN_NO_ERROR;

          svn_error_clear(svn_io_remove_file2(to_path, FALSE, pool));

          return err;

        failed_tmp:
          svn_error_clear(svn_io_remove_file2(tmp_to_path, FALSE, pool));
        }

      return err;
    */

    public static void rename(File src, File dst) throws SVNException {
        if (SVNFileType.getType(src) == SVNFileType.NONE) {
            deleteFile(dst);
            return;
        }
        //TODO directory cannot be renamed????
        /*
        if (dst.isDirectory()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot rename file ''{0}'' to ''{1}''; file ''{1}'' is a directory", new Object[] {
                    src, dst
            });
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }
        */
        boolean renamed = false;
        if (!isWindows) {
            renamed = src.renameTo(dst);
            if (!renamed && src.isFile() && !dst.exists()) {
                copyFile(src, dst, true);
                if (!dst.isFile()) {
                    copyFile(src, dst, false);
                }
                boolean deleted = deleteFile(src);
                renamed = deleted && dst.isFile();
            }
        } else {
            // check for os/2 first because on os/2
            // isWindows is also true
            if (isOS2) {
                if (SVNOS2Util.moveFile(src, dst)) {
                    renamed = true;
                }
            } else if (SVNJNAUtil.moveFile(src, dst)) {
                renamed = true;
            }
            if (!renamed) {
                boolean wasRO = dst.exists() && !dst.canWrite();
                setReadonly(src, false);
                setReadonly(dst, false);
                // use special loop on windows.
                long sleep = 1;
                for (int i = 0; i < FILE_CREATION_ATTEMPTS_COUNT; i++) {
                    dst.delete();
                    if (src.renameTo(dst)) {
                        if (wasRO && !isOpenVMS) {
                            dst.setReadOnly();
                        }
                        return;
                    }
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                    }
                    if (sleep < 128) {
                        sleep = sleep * 2;
                    }
                }
            }
        }
        if (!renamed) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot rename file ''{0}'' to ''{1}''", new Object[] {
                    src, dst
            });
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }
    }

    public static boolean setReadonly(File file, boolean readonly) {
        if (!file.exists()) {
            return false;
        }
        if (isOpenVMS) {
            // Never set readOnly for OpenVMS
            return true;
        }
        if (readonly) {
            return file.setReadOnly();
        } else if (ourSetWritableMethod != null) {
            try {
                Object result = ourSetWritableMethod.invoke(file, new Object[] {
                    Boolean.TRUE
                });
                if (Boolean.TRUE.equals(result)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        // check for os/2 first because on os/2
        // isWindows is also true
        if (isOS2) {
            if (SVNOS2Util.setWritable(file)) {
                return true;
            }
        } else if (isWindows) {
            if (SVNJNAUtil.setWritable(file)) {
                return true;
            }
        } else if (isLinux || isOSX || isBSD || SVNFileUtil.isSolaris) {
            if (SVNJNAUtil.setWritable(file)) {
                return true;
            }
        }
        try {
            SVNFileType fileType = SVNFileType.getType(file);            
            if (fileType == SVNFileType.FILE && useCopyOnSetWritable() && file.length() < 1024 * 100) {
                // faster way for small files.
                File tmp = createUniqueFile(SVNFileUtil.getFileDir(file), SVNFileUtil.getFileName(file), ".ro", true);
                copyFile(file, tmp, false);
                copyFile(tmp, file, false);
                deleteFile(tmp);
            } else {
                if (isWindows) {
                    Process p = null;
                    try {
                        p = Runtime.getRuntime().exec(ATTRIB_COMMAND + " -R \"" + file.getAbsolutePath() + "\"");
                        p.waitFor();
                    } finally {
                        if (p != null) {
                            closeFile(p.getInputStream());
                            closeFile(p.getOutputStream());
                            closeFile(p.getErrorStream());
                            p.destroy();
                        }
                    }
                } else {
                    execCommand(new String[] {
                            CHMOD_COMMAND, "ugo+w", file.getAbsolutePath()
                    });
                }
            }
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, th);
            return false;
        }
        return true;
    }

    public static void setExecutable(File file, boolean executable) {
        if (isWindows || isOpenVMS || file == null || !file.exists() || SVNFileType.getType(file) == SVNFileType.SYMLINK) {
            return;
        }
        
        if (ourSetExecutableMethod != null) {
            try {
                ourSetExecutableMethod.invoke(file, new Object[] {
                    Boolean.valueOf(executable),
                    Boolean.FALSE,
                });
                return;
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        
        if (SVNJNAUtil.setExecutable(file, executable)) {
            return;
        }
        try {
            if (file.canWrite()) {
                execCommand(new String[] {
                        CHMOD_COMMAND, executable ? "ugo+x" : "ugo-x", file.getAbsolutePath()
                });
            }
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, th);
        }
    }

    public static boolean symlinksSupported() {
        return !(isWindows || isOpenVMS) && SVNFileType.isSymlinkSupportEnabled();
    }

    public static void setSGID(File dir) {
        if (isWindows || isOpenVMS || dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }

        if (SVNJNAUtil.setSGID(dir)) {
            return;
        }

        try {
            execCommand(new String[] {
                    CHMOD_COMMAND, "g+s", dir.getAbsolutePath()
            });
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, th);
        }
    }

    public static File resolveSymlinkToFile(File file) {
        if (!symlinksSupported()) {
            return null;
        }
        File targetFile = resolveSymlink(file);
        if (targetFile == null || !targetFile.isFile()) {
            return null;
        }
        return targetFile;
    }

    public static File resolveSymlink(File file) {
        if (!symlinksSupported()) {
            return null;
        }
        File targetFile = file;
        while (SVNFileType.getType(targetFile) == SVNFileType.SYMLINK) {
            String symlinkName = getSymlinkName(targetFile);
            if (symlinkName == null) {
                return null;
            }
            if (symlinkName.startsWith("/")) {
                targetFile = new File(symlinkName);
            } else {
                targetFile = new File(SVNFileUtil.getFileDir(targetFile), symlinkName);
            }
        }
        return targetFile;
    }

    public static void copy(File src, File dst, boolean safe, boolean copyAdminDirectories) throws SVNException {
        SVNFileType srcType = SVNFileType.getType(src);
        if (srcType == SVNFileType.FILE) {
            copyFile(src, dst, safe);
        } else if (srcType == SVNFileType.DIRECTORY) {
            copyDirectory(src, dst, copyAdminDirectories, null);
        } else if (srcType == SVNFileType.SYMLINK) {
            String name = SVNFileUtil.getSymlinkName(src);
            if (name != null) {
                SVNFileUtil.createSymlink(dst, name);
            }
        }
    }

    public static void copyFile(File src, File dst, boolean safe) throws SVNException {
        copyFile(src, dst, safe, true);
    }

    public static void copyFile(File src, File dst, boolean safe, boolean keepTimestamp) throws SVNException {
        if (src == null || dst == null) {
            return;
        }
        if (src.equals(dst)) {
            return;
        }
        if (!src.exists()) {
            dst.delete();
            return;
        }
        File tmpDst = dst;
        if (SVNFileType.getType(dst) != SVNFileType.NONE) {
            if (safe && !useUnsafeCopyOnly()) {
                tmpDst = createUniqueFile(SVNFileUtil.getFileDir(dst), ".copy", ".tmp", true);
            } else {
                dst.delete();
            }
        }
        boolean executable = isExecutable(src);
        SVNFileUtil.getFileDir(dst).mkdirs();

        SVNErrorMessage error = null;
        final boolean useNIO = useNIOCopying();
        if (useNIO) {
            FileChannel srcChannel = null;
            FileChannel dstChannel = null;
            FileInputStream is = null;
            FileOutputStream os = null;

            try {
                is = createFileInputStream(src);
                srcChannel = is.getChannel();
                os = createFileOutputStream(tmpDst, false);
                dstChannel = os.getChannel();
                long totalSize = srcChannel.size();
                long toCopy = totalSize;
                while (toCopy > 0) {
                    toCopy -= dstChannel.transferFrom(srcChannel, totalSize - toCopy, toCopy);
                }
            } catch (IOException e) {
                error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot copy file ''{0}'' to ''{1}'': {2}", new Object[] {
                        src, dst, e.getLocalizedMessage()
                });
            } finally {
                if (srcChannel != null) {
                    try {
                        srcChannel.close();
                    } catch (IOException e) {
                        //
                    }
                }
                if (dstChannel != null) {
                    try {
                        dstChannel.close();
                    } catch (IOException e) {
                        //
                    }
                }
                SVNFileUtil.closeFile(is);
                SVNFileUtil.closeFile(os);
            }
        }

        if (!useNIO || error != null) {
            error = null;
            InputStream sis = null;
            OutputStream dos = null;
            try {
                sis = SVNFileUtil.openFileForReading(src, SVNLogType.WC);
                dos = SVNFileUtil.openFileForWriting(tmpDst);
                SVNTranslator.copy(sis, dos);
            } catch (IOException e) {
                error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot copy file ''{0}'' to ''{1}'': {2}", new Object[] {
                        src, dst, e.getLocalizedMessage()
                });
            } finally {
                SVNFileUtil.closeFile(dos);
                SVNFileUtil.closeFile(sis);
            }
        }
        if (error != null) {
            SVNErrorManager.error(error, Level.FINE, SVNLogType.WC);
        }
        if (safe && tmpDst != dst) {
            rename(tmpDst, dst);
        }
        if (executable) {
            setExecutable(dst, true);
        }
        if (keepTimestamp) {
            SVNFileUtil.setLastModified(dst, src.lastModified());
        }
    }
    
    public static boolean setLastModified(File file, long timestamp) {
        if (file != null && timestamp >= 0) {
            return file.setLastModified(timestamp);
        }
        return false;
    }

    public static boolean createSymlink(File link, File linkName) throws SVNException {
        if (!symlinksSupported()) {
            return false;
        }
        if (SVNFileType.getType(link) != SVNFileType.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot create symbolic link ''{0}''; file already exists", link);
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }
        String fileContents = "";
        try {
            fileContents = readSingleLine(linkName);
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        }
        if (fileContents.startsWith("link ")) {
            fileContents = fileContents.substring("link".length()).trim();
            return createSymlink(link, fileContents);
        }
        // create file using internal representation
        createFile(link, fileContents, "UTF-8");
        return true;
    }

    public static boolean createSymlink(File link, String linkName) {
        if (!symlinksSupported()) {
            return false;
        }
        if (SVNJNAUtil.createSymlink(link, linkName)) {
            return true;
        }
        try {
            execCommand(new String[] {
                    LN_COMMAND, "-s", linkName, link.getAbsolutePath()
            });
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, th);
        }
        return SVNFileType.getType(link) == SVNFileType.SYMLINK;
    }

    public static boolean detranslateSymlink(File src, File linkFile) throws SVNException {
        if (!symlinksSupported()) {
            return false;
        }
        if (SVNFileType.getType(src) != SVNFileType.SYMLINK) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot detranslate symbolic link ''{0}''; file does not exist or not a symbolic link", src);
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }
        String linkPath = getSymlinkName(src);
        if (linkPath == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot detranslate symbolic link ''{0}''; file does not exist or not a symbolic link", src);
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }
        OutputStream os = openFileForWriting(linkFile);
        try {
            os.write("link ".getBytes("UTF-8"));
            os.write(linkPath.getBytes("UTF-8"));
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        } finally {
            SVNFileUtil.closeFile(os);
        }
        return true;
    }

    public static String getSymlinkName(File link) {
        if (!symlinksSupported() || link == null) {
            return null;
        }
        String ls = null;
        ls = SVNJNAUtil.getLinkTarget(link);
        if (ls != null) {
            return ls;
        }
        try {
            ls = execCommand(new String[] {
                    LS_COMMAND, "-ld", link.getAbsolutePath()
            });
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, th);
        }
        if (ls == null || ls.lastIndexOf(" -> ") < 0) {
            return null;
        }
        int index = ls.lastIndexOf(" -> ") + " -> ".length();
        if (index <= ls.length()) {
            return ls.substring(index);
        }
        return null;
    }

    public static String computeChecksum(String line) {
        if (line == null) {
            return null;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        if (digest == null) {
            return null;
        }
        digest.update(line.getBytes());
        return toHexDigest(digest);

    }

    public static String computeChecksum(File file) throws SVNException {
        if (file == null || file.isDirectory() || !file.exists()) {
            return null;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "MD5 implementation not found: {0}", e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
            return null;
        }
        InputStream is = openFileForReading(file, SVNLogType.WC);
        byte[] buffer = new byte[1024 * 16];
        try {
            while (true) {
                int l = is.read(buffer);
                if (l < 0) {
                    break;
                } else if (l == 0) {
                    continue;
                }
                digest.update(buffer, 0, l);
            }
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        } finally {
            closeFile(is);
        }
        return toHexDigest(digest);
    }

    public static boolean compareFiles(File f1, File f2, MessageDigest digest) throws SVNException {
        if (f1 == null || f2 == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "NULL paths are supported in compareFiles method");
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
            return false;
        }
        if (f1.equals(f2)) {
            return true;
        }
        boolean equals = true;
        if (f1.length() != f2.length()) {
            if (digest == null) {
                return false;
            }
            equals = false;
        }
        InputStream is1 = openFileForReading(f1, SVNLogType.WC);
        InputStream is2 = openFileForReading(f2, SVNLogType.WC);
        try {
            while (true) {
                int b1 = is1.read();
                int b2 = is2.read();
                if (b1 != b2) {
                    if (digest == null) {
                        return false;
                    }
                    equals = false;
                }
                if (b1 < 0) {
                    break;
                }
                if (digest != null) {
                    digest.update((byte) (b1 & 0xFF));
                }
            }
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        } finally {
            closeFile(is1);
            closeFile(is2);
        }
        return equals;
    }

    public static void truncate(File file, long truncateToSize) throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = openRAFileForWriting(file, false);
            raf.setLength(truncateToSize);
        } catch (SVNException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } finally {
            closeFile(raf);
        }
    }

    public static void setHidden(File file, boolean hidden) {
        // check for os/2 first because on os/2
        // isWindows is also true
        if (isOS2 && SVNOS2Util.setHidden(file, hidden)) {
            return;
        }

        if (isWindows && SVNJNAUtil.setHidden(file)) {
            return;
        }
        if (!isWindows || file == null || !file.exists() || file.isHidden()) {
            return;
        }
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("attrib " + (hidden ? "+" : "-") + "H \"" + file.getAbsolutePath() + "\"");
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, th);
        } finally {
            if (p != null) {
                closeFile(p.getErrorStream());
                closeFile(p.getInputStream());
                closeFile(p.getOutputStream());
                p.destroy();
            }
        }
    }

    public static void deleteAll(File dir, ISVNEventHandler cancelBaton) throws SVNException {
        deleteAll(dir, true, cancelBaton);
    }

    public static void deleteAll(File dir, boolean deleteDirs) {
        try {
            deleteAll(dir, deleteDirs, null);
        } catch (SVNException e) {
            // should never happen as cancell handler is null.
        }
    }

    public static void deleteAll(File dir, boolean deleteDirs, ISVNCanceller cancelBaton) throws SVNException {
        if (dir == null) {
            return;
        }
        SVNFileType fileType = SVNFileType.getType(dir);
        File[] children = fileType == SVNFileType.DIRECTORY ? SVNFileListUtil.listFiles(dir) : null;
        if (children != null) {
            if (cancelBaton != null) {
                cancelBaton.checkCancelled();
            }
            for (int i = 0; i < children.length; i++) {
                File child = children[i];
                deleteAll(child, deleteDirs, cancelBaton);
            }
            if (cancelBaton != null) {
                cancelBaton.checkCancelled();
            }
        }
        if (fileType == SVNFileType.DIRECTORY && !deleteDirs) {
            return;
        }
        deleteFile(dir);
    }

    public static boolean deleteFile(File file) throws SVNException {
        if (file == null) {
            return true;
        }
        if (!isWindows || file.isDirectory() || !file.exists()) {
            return file.delete();
        }
        long sleep = 1;
        for (int i = 0; i < FILE_CREATION_ATTEMPTS_COUNT; i++) {
            if (file.delete() && !file.exists()) {
                return true;
            }
            if (!file.exists()) {
                return true;
            }
            setReadonly(file, false);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
            }
            if (sleep < 128) {
                sleep = sleep * 2;
            }
        }
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot delete file ''{0}''", file);
        SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        return false;
    }

    public static String toHexDigest(MessageDigest digest) {
        if (digest == null) {
            return null;
        }
        byte[] result = digest.digest();
        StringBuffer hexDigest = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            SVNFormatUtil.appendHexNumber(hexDigest, result[i]);
        }
        return hexDigest.toString();
    }

    public static String toHexDigest(byte[] digest) {
        if (digest == null) {
            return null;
        }

        StringBuffer hexDigest = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            SVNFormatUtil.appendHexNumber(hexDigest, digest[i]);
        }
        return hexDigest.toString();
    }

    public static byte[] fromHexDigest(String hexDigest) {
        if (hexDigest == null || hexDigest.length() == 0) {
            return null;
        }

        hexDigest = hexDigest.toLowerCase();

        int digestLength = hexDigest.length() / 2;

        if (digestLength == 0 || 2 * digestLength != hexDigest.length()) {
            return null;
        }

        byte[] digest = new byte[digestLength];
        for (int i = 0; i < hexDigest.length() / 2; i++) {
            if (!isHex(hexDigest.charAt(2 * i)) || !isHex(hexDigest.charAt(2 * i + 1))) {
                return null;
            }

            int hi = Character.digit(hexDigest.charAt(2 * i), 16) << 4;

            int lo = Character.digit(hexDigest.charAt(2 * i + 1), 16);
            Integer ib = new Integer(hi | lo);
            byte b = ib.byteValue();

            digest[i] = b;
        }

        return digest;
    }

    public static String getNativeEOLMarker(ISVNOptions options) {
        if (nativeEOLMarker == null) {
            nativeEOLMarker = new String(options.getNativeEOL());
        }
        return nativeEOLMarker;
    }

    public static long roundTimeStamp(long tstamp) {
        return (tstamp / 1000) * 1000;
    }

    public static void sleepForTimestamp() {
        if (!ourIsSleepForTimeStamp) {
            return;
        }
        long time = System.currentTimeMillis();
        time = 1100 - (time - (time / 1000) * 1000);
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            //
        }
    }

    public static void setSleepForTimestamp(boolean sleep) {
        ourIsSleepForTimeStamp = sleep;
    }

    public static String readLineFromStream(InputStream is, StringBuffer buffer, CharsetDecoder decoder) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int r = -1;
        while ((r = is.read()) != '\n') {
            if (r == -1) {
                String out = decode(decoder, byteBuffer.toByteArray());
                buffer.append(out);
                return null;
            }
            byteBuffer.write(r);

        }
        String out = decode(decoder, byteBuffer.toByteArray());
        buffer.append(out);
        return out;
    }

    public static String detectMimeType(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];

        int read = readIntoBuffer(is, buffer, 0, buffer.length);

        int binaryCount = 0;
        for (int i = 0; i < read; i++) {
            byte b = buffer[i];
            if (b == 0) {
                return BINARY_MIME_TYPE;
            }
            if (b < 0x07 || (b > 0x0d && b < 0x20) || b > 0x7F) {
                binaryCount++;
            }
        }
        if (read > 0 && binaryCount * 1000 / read > 850) {
            return BINARY_MIME_TYPE;
        }
        return null;
    }

    public static String detectMimeType(File file, Map<String,String> mimeTypes) throws SVNException {
        if (file == null || !file.exists()) {
            return null;
        }

        SVNFileType kind = SVNFileType.getType(file);
        if (kind != SVNFileType.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_FILENAME, "Can''t detect MIME type of non-file ''{0}''", file);
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }

        if (mimeTypes != null) {
            String name = SVNFileUtil.getFileName(file);
            String pathExt = "";
            int dotInd = name.lastIndexOf('.');
            if (dotInd != -1 && dotInd != 0 && dotInd != name.length() - 1) {
                pathExt = name.substring(dotInd + 1);
            }

            String mimeType = (String) mimeTypes.get(pathExt);
            if (mimeType != null) {
                return mimeType;
            }
        }

        InputStream is = null;
        try {
            is = openFileForReading(file, SVNLogType.WC);
            return detectMimeType(is);
        } catch (IOException e) {
            return null;
        } catch (SVNException e) {
            return null;
        } finally {
            closeFile(is);
        }
    }

    public static boolean isExecutable(File file) throws SVNException {
        if (isWindows || isOpenVMS) {
            return false;
        }
        Boolean executable = SVNJNAUtil.isExecutable(file);
        if (executable != null) {
            return executable.booleanValue();
        }
        String[] commandLine = new String[] {
                LS_COMMAND, "-ln", file.getAbsolutePath()
        };
        String line = null;
        try {
            if (file.canRead()) {
                line = execCommand(commandLine);
            }
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, th);
        }
        if (line == null || line.indexOf(' ') < 0) {
            return false;
        }
        int index = 0;

        String mod = null;
        String fuid = null;
        String fgid = null;
        for (StringTokenizer tokens = new StringTokenizer(line, " \t"); tokens.hasMoreTokens();) {
            String token = tokens.nextToken();
            if (index == 0) {
                mod = token;
            } else if (index == 2) {
                fuid = token;
            } else if (index == 3) {
                fgid = token;
            } else if (index > 3) {
                break;
            }
            index++;
        }
        if (mod == null) {
            return false;
        }
        if (getCurrentUser().equals(fuid)) {
            return mod.toLowerCase().indexOf('x') >= 0 && mod.toLowerCase().indexOf('x') < 4;
        } else if (getCurrentGroup().equals(fgid)) {
            return mod.toLowerCase().indexOf('x', 4) >= 4 && mod.toLowerCase().indexOf('x', 4) < 7;
        } else {
            return mod.toLowerCase().indexOf('x', 7) >= 7;
        }
    }

    public static File ensureDirectoryExists(File path) throws SVNException {
        SVNFileType type = SVNFileType.getType(path);
        SVNNodeKind kind = SVNFileType.getNodeKind(type);
        if (kind != SVNNodeKind.NONE && kind != SVNNodeKind.DIR) {
            SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "''{0}'' is not a directory", path);
            SVNErrorManager.error(error, SVNLogType.WC);
        } else if (kind == SVNNodeKind.NONE) {
            boolean created = path.mkdirs();
            if (!created) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Unable to make directories", path);
                SVNErrorManager.error(error, SVNLogType.WC);
            }
        }
        return path;
    }

    public static void copyDirectory(File srcDir, File dstDir, boolean copyAdminDir, ISVNEventHandler cancel) throws SVNException {
        if (!dstDir.exists()) {
            dstDir.mkdirs();
            SVNFileUtil.setLastModified(dstDir, srcDir.lastModified());
        }
        File[] files = SVNFileListUtil.listFiles(srcDir);
        for (int i = 0; files != null && i < files.length; i++) {
            File file = files[i];
            if (SVNFileUtil.getFileName(file).equals("..") || SVNFileUtil.getFileName(file).equals(".") || file.equals(dstDir)) {
                continue;
            }
            if (cancel != null) {
                cancel.checkCancelled();
            }
            if (!copyAdminDir && SVNFileUtil.getFileName(file).equals(getAdminDirectoryName())) {
                continue;
            }
            SVNFileType fileType = SVNFileType.getType(file);
            File dst = new File(dstDir, SVNFileUtil.getFileName(file));

            if (fileType == SVNFileType.FILE) {
                boolean executable = isExecutable(file);
                copyFile(file, dst, false);
                if (executable) {
                    setExecutable(dst, executable);
                }
            } else if (fileType == SVNFileType.DIRECTORY) {
                copyDirectory(file, dst, copyAdminDir, cancel);
                if (file.isHidden() || getAdminDirectoryName().equals(SVNFileUtil.getFileName(file))) {
                    setHidden(dst, true);
                }
            } else if (fileType == SVNFileType.SYMLINK) {
                String name = getSymlinkName(file);
                if (name != null) {
                    createSymlink(dst, name);
                }
            }
        }
    }

    public static OutputStream openFileForWriting(File file) throws SVNException {
        return openFileForWriting(file, false);
    }

    public static OutputStream openFileForWriting(File file, boolean append) throws SVNException {
        if (file == null) {
            return null;
        }
        if (SVNFileUtil.getFileDir(file) != null && !SVNFileUtil.getFileDir(file).exists()) {
            SVNFileUtil.getFileDir(file).mkdirs();
        }
        if (isOpenVMS && !append && file.isFile()) {
            deleteFile(file);
        } else if (file.isFile() && !file.canWrite()) {
            // force writable.
            setReadonly(file, false);
        }
        OutputStream fos = null;
        OutputStream result = null;
        try {
            fos = createFileOutputStream(file, append);
            result = new BufferedOutputStream(fos);
        } catch (IOException e) {
            closeFile(fos);
            closeFile(result);
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot write to ''{0}'': {1}", new Object[] {
                    file, e.getMessage()
            });
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        } finally {
            if (result == null) {
                closeFile(fos);
            }
        }
        return result;
    }

    public static FileOutputStream createFileOutputStream(File file, boolean append) throws IOException {
        int retryCount = SVNFileUtil.isWindows ? FILE_CREATION_ATTEMPTS_COUNT : 1;
        FileOutputStream os = null;
        long sleep = 1;
        for (int i = 0; i < retryCount; i++) {
            try {
                os = new FileOutputStream(file, append);
                break;
            } catch (IOException e) {
                SVNFileUtil.closeFile(os);
                if (i + 1 >= retryCount) {
                    throw e;
                }
                if (file.exists() && file.isFile() && file.canWrite()) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e1) {
                    }
                    if (sleep < 128) {
                        sleep = sleep * 2;
                    }
                    continue;
                }
                throw e;
            }
        }
        return os;
    }

    public static RandomAccessFile openRAFileForWriting(File file, boolean append) throws SVNException {
        if (file == null) {
            return null;
        }
        if (SVNFileUtil.getFileDir(file) != null && !SVNFileUtil.getFileDir(file).exists()) {
            SVNFileUtil.getFileDir(file).mkdirs();
        }
        RandomAccessFile raf = null;
        RandomAccessFile result = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            if (append) {
                raf.seek(raf.length());
            }
            result = raf;
        } catch (FileNotFoundException e) {
            closeFile(raf);
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Can not write to file ''{0}'': {1}", new Object[] {
                    file, e.getMessage()
            });
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        } catch (IOException ioe) {
            closeFile(raf);
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Can not set position pointer in file ''{0}'': {1}", new Object[] {
                    file, ioe.getMessage()
            });
            SVNErrorManager.error(err, ioe, Level.FINE, SVNLogType.DEFAULT);
        } finally {
            if (result == null) {
                closeFile(raf);
            }
        }
        return result;
    }

    public static InputStream openFileForReading(File file) throws SVNException {
        return openFileForReading(file, Level.FINE, SVNLogType.DEFAULT);
    }

    public static InputStream openFileForReading(File file, SVNLogType logType) throws SVNException {
        return openFileForReading(file, Level.FINE, logType);
    }

    public static InputStream openFileForReading(File file, Level logLevel, SVNLogType logType) throws SVNException {
        if (file == null) {
            return null;
        }
        InputStream fis = null;
        InputStream result = null;
        try {
            fis = createFileInputStream(file);
            result = new BufferedInputStream(fis);
        } catch (FileNotFoundException nfe) {
            closeFile(fis);
            closeFile(result);
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read from ''{0}'': {1}", new Object[] {
                    file, nfe.getMessage()
            });
            SVNErrorManager.error(err, logLevel, logType);
        } catch (IOException e) {
            closeFile(fis);
            closeFile(result);
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read from ''{0}'': {1}", new Object[] {
                    file, e.getMessage()
            });
            SVNErrorManager.error(err, e, logLevel, logType);
        } finally {
            if (result == null) {
                closeFile(fis);
            }
        }
        return result;
    }

    public static FileInputStream createFileInputStream(File file) throws IOException {
        int retryCount = SVNFileUtil.isWindows ? FILE_CREATION_ATTEMPTS_COUNT : 1;
        FileInputStream is = null;
        long sleep = 1;
        for (int i = 0; i < retryCount; i++) {
            try {
                is = new FileInputStream(file);
                break;
            } catch (IOException e) {
                SVNFileUtil.closeFile(is);
                if (i + 1 >= retryCount) {
                    throw e;
                }
                if (file.exists() && file.isFile() && file.canRead()) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e1) {
                    }
                    if (sleep < 128) {
                        sleep = sleep * 2;
                    }
                    continue;
                }
                throw e;
            }
        }
        return is;
    }

    public static RandomAccessFile openRAFileForReading(File file) throws SVNException {
        if (file == null) {
            return null;
        }
        if (!file.isFile() || !file.canRead()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read from ''{0}'': path refers to a directory or read access is denied", file);
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }
        if (!file.exists()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "File ''{0}'' does not exist", file);
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }
        try {
            return new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read from ''{0}'': {1}", new Object[] {
                    file, e.getMessage()
            });
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }
        return null;
    }

    public static void closeFile(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
            //
        }
    }

    public static void closeFile(ISVNInputFile inFile) {
        if (inFile == null) {
            return;
        }
        try {
            inFile.close();
        } catch (IOException e) {
            //
        }
    }

    public static void closeFile(OutputStream os) {
        if (os == null) {
            return;
        }
        try {
            os.close();
        } catch (IOException e) {
            //
        }
    }

    public static void closeFile(RandomAccessFile raf) {
        if (raf == null) {
            return;
        }
        try {
            raf.close();
        } catch (IOException e) {
            //
        }
    }

    public static String execCommand(String[] commandLine) throws SVNException {
        return execCommand(commandLine, false, null);
    }

    public static String execCommand(String[] commandLine, boolean waitAfterRead, ISVNReturnValueCallback callback) throws SVNException {
        return execCommand(commandLine, null, waitAfterRead, callback);
    }

    public static String execCommand(String[] commandLine, String[] env, boolean waitAfterRead, ISVNReturnValueCallback callback) throws SVNException {
        InputStream is = null;
        boolean handleOutput = callback != null && callback.isHandleProgramOutput();
        StringBuffer result = handleOutput ? null : new StringBuffer();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(commandLine, env);
            is = process.getInputStream();
            if (!waitAfterRead) {
                int rc = process.waitFor();
                if (callback != null) {
                    callback.handleReturnValue(rc);
                }
                if (rc != 0) {
                    return null;
                }
            }
            int r;
            while ((r = is.read()) >= 0) {
                char ch = (char) (r & 0xFF);
                if (handleOutput) {
                    callback.handleChar(ch);
                } else {
                    result.append(ch);
                }
            }
            if (waitAfterRead) {
                int rc = process.waitFor();
                if (callback != null) {
                    callback.handleReturnValue(rc);
                }
                if (rc != 0) {
                    return null;
                }
            }
            return handleOutput ? null : result.toString().trim();
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, e);
        } catch (InterruptedException e) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, e);
        } finally {
            closeFile(is);
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }

    public static void closeFile(Writer os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public static void closeFile(Reader is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public static String getAdminDirectoryName() {
        if (ourAdminDirectoryName == null) {
            String defaultAdminDir = ".svn";
            if (getEnvironmentVariable("SVN_ASP_DOT_NET_HACK") != null) {
                defaultAdminDir = "_svn";
            }
            ourAdminDirectoryName = System.getProperty("svnkit.admindir", System.getProperty("javasvn.admindir", defaultAdminDir));
            if (ourAdminDirectoryName == null || "".equals(ourAdminDirectoryName.trim())) {
                ourAdminDirectoryName = defaultAdminDir;
            }
        }
        return ourAdminDirectoryName;
    }

    public static void setAdminDirectoryName(String name) {
        ourAdminDirectoryName = name;
    }

    public static File getApplicationDataPath() {
        if (ourAppDataPath != null) {
            return ourAppDataPath;
        }
        String jnaAppData = SVNJNAUtil.getApplicationDataPath(false);
        if (jnaAppData != null) {
            ourAppDataPath = new File(jnaAppData);
            return ourAppDataPath;
        }

        String envAppData = getEnvironmentVariable("APPDATA");
        if (envAppData == null) {
            // no appdata for that user, fallback to system one.
            ourAppDataPath = getSystemApplicationDataPath();
        } else {
            ourAppDataPath = new File(envAppData);
        }
        return ourAppDataPath;
    }

    public static File getSystemApplicationDataPath() {
        if (ourSystemAppDataPath != null) {
            return ourSystemAppDataPath;
        }
        String jnaAppData = SVNJNAUtil.getApplicationDataPath(true);
        if (jnaAppData != null) {
            ourSystemAppDataPath = new File(jnaAppData);
            return ourSystemAppDataPath;
        }
        String envAppData = getEnvironmentVariable("ALLUSERSPROFILE");
        if (envAppData == null) {
            ourSystemAppDataPath = new File(new File("C:/Documents and Settings/All Users"), "Application Data");
        } else {
            ourSystemAppDataPath = new File(envAppData, "Application Data");
        }
        return ourSystemAppDataPath;
    }

    public static String getEnvironmentVariable(String name) {
        try {
            // pre-Java 1.5 this throws an Error. On Java 1.5 it
            // returns the environment variable
            Method getenv = System.class.getMethod("getenv", new Class[] {
                String.class
            });
            if (getenv != null) {
                Object value = getenv.invoke(null, new Object[] {
                    name
                });
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Throwable e) {
            try {
                // This means we are on 1.4. Get all variables into
                // a Properties object and get the variable from that
                return getEnvironment().getProperty(name);
            } catch (Throwable e1) {
                SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, e);
                SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, e1);
                return null;
            }
        }
        return null;
    }

    private static String ourTestEditor = null;
    private static String ourTestMergeTool = null;
    private static String ourTestFunction = null;

    public static void setTestEnvironment(String editor, String mergeTool, String function) {
        ourTestEditor = editor;
        ourTestMergeTool = mergeTool;
        ourTestFunction = function;
    }

    public static String[] getTestEnvironment() {
        return new String[] {
                ourTestEditor, ourTestMergeTool, ourTestFunction
        };
    }

    public static Properties getEnvironment() throws Throwable {
        Process p = null;
        Properties envVars = new Properties();
        Runtime r = Runtime.getRuntime();
        if (isWindows) {
            if (System.getProperty("os.name").toLowerCase().indexOf("windows 9") >= 0) {
                p = r.exec("command.com /c set");
            } else {
                p = r.exec("cmd.exe /c set");
            }
        } else {
            p = r.exec(ENV_COMMAND); // if OpenVMS ENV_COMMAND could be "mcr
                                     // gnu:[bin]env"
        }
        if (p != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx >= 0) {
                    String key = line.substring(0, idx);
                    String value = line.substring(idx + 1);
                    envVars.setProperty(key, value);
                }
            }
        }
        return envVars;
    }

    public static File createTempDirectory(String name) throws SVNException {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("svnkit" + name, ".tmp");
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot create temporary directory: {0}", e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        }
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        tmpFile.mkdirs();
        return tmpFile;
    }

    public static File createTempFile(String prefix, String suffix) throws SVNException {
        File tmpFile = null;
        try {
            if (prefix.length() < 3) {
                prefix = "svn" + prefix;
            }
            tmpFile = File.createTempFile(prefix, suffix);
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot create temporary file: {0}", e.getMessage());
            SVNErrorManager.error(err, e, Level.FINE, SVNLogType.DEFAULT);
        }
        return tmpFile;
    }

    public static File getSystemConfigurationDirectory() {
        if (isWindows) {
            return new File(getSystemApplicationDataPath(), "Subversion");
        } else if (isOpenVMS) {
            return new File("/sys$config", "subversion").getAbsoluteFile();
        }
        return new File("/etc/subversion");
    }

    public static String readSingleLine(File file) throws IOException {
        if (!file.isFile() || !file.canRead()) {
            throw new IOException("can't open file '" + file.getAbsolutePath() + "'");
        }
        BufferedReader reader = null;
        String line = null;
        InputStream is = null;
        try {
            is = createFileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            line = reader.readLine();
        } finally {
            closeFile(is);
        }
        return line;
    }

    private static String decode(CharsetDecoder decoder, byte[] in) {
        ByteBuffer inBuf = ByteBuffer.wrap(in);
        CharBuffer outBuf = CharBuffer.allocate(inBuf.capacity() * Math.round(decoder.maxCharsPerByte() + 0.5f));
        decoder.decode(inBuf, outBuf, true);
        decoder.flush(outBuf);
        decoder.reset();
        return outBuf.flip().toString();
    }

    private static String getCurrentUser() throws SVNException {
        if (isWindows || isOpenVMS) {
            return System.getProperty("user.name");
        }
        if (ourUserID == null) {
            ourUserID = execCommand(new String[] {
                    ID_COMMAND, "-u"
            });
            if (ourUserID == null) {
                ourUserID = "0";
            }
        }
        return ourUserID;
    }

    private static String getCurrentGroup() throws SVNException {
        if (isWindows || isOpenVMS) {
            return System.getProperty("user.name");
        }
        if (ourGroupID == null) {
            ourGroupID = execCommand(new String[] {
                    ID_COMMAND, "-g"
            });
            if (ourGroupID == null) {
                ourGroupID = "0";
            }
        }
        return ourGroupID;
    }

    private static boolean isHex(char ch) {
        return Character.isDigit(ch) || (Character.toUpperCase(ch) >= 'A' && Character.toUpperCase(ch) <= 'F');
    }

    public static boolean isAbsolute(File path) {
        return path != null && path.isAbsolute();
    }

    public static String getFilePath(File file) {
        if (file == null)
            return null;
        String path = file.getPath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }
        return path;
    }

    public static String getFileName(File file) {
        if (file == null)
            return null;
        return file.getName();
    }

    public static File getFileDir(File file) {
        if (file == null || "".equals(file.getPath()))
            return null;
        File parentFile = file.getParentFile();
        return parentFile != null ? parentFile : new File("");
    }

    public static File createFilePath(String path) {
        if (path == null)
            return null;
        return new File(path);
    }

    public static File createFilePath(File parent, File child) {
        if (child == null) {
            return parent;
        }
        if (isAbsolute(child)) {
            return child;
        }
        return createFilePath(parent, child.toString());
    }

    public static File createFilePath(File parent, String child) {
        if (child == null)
            return parent;
        if (parent == null)
            return createFilePath(child);
        return createFilePath(parent.toString(), child.toString());
    }

    public static File createFilePath(String parent, String child) {
        if (child == null)
            return null;
        if (parent == null || "".equals(parent))
            return createFilePath(child);
        if (SVNPathUtil.isAbsolute(child))
            return createFilePath(child);
        return new File(parent, child.toString());
    }

    public static String getFileExtension(File path) {
        if (path == null)
            return null;
        return getFileNameExtension(path.getName());
    }

    public static String getFileNameExtension(String name) {
        if (name == null)
            return null;
        int dotInd = name.lastIndexOf('.');
        if (dotInd != -1 && dotInd != 0 && dotInd != name.length() - 1) {
            return name.substring(dotInd + 1);
        }
        return null;
    }

    public static boolean compare(InputStream is, InputStream old) {
        try {
            while(true) {
                int r1 = is.read();
                int r2 = old.read();
                if (r1 != r2) {
                    return false;
                }
                if (r1 < 0) {
                    break;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static InputStream readSymlink(File link) throws SVNException {
        if (symlinksSupported()) {
            SVNFileType type = SVNFileType.getType(link);
            if (type == SVNFileType.SYMLINK) {
                StringBuffer sb = new StringBuffer();
                sb.append("link ");
                sb.append(getSymlinkName(link));
                try {
                    return new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    return new ByteArrayInputStream(sb.toString().getBytes());
                }
            }
        } 
        return openFileForReading(link);
    }

    public static long getFileLength(File file) {
        if (symlinksSupported()) {
            SVNFileType type = SVNFileType.getType(file);
            if (type == SVNFileType.SYMLINK) {
                try {
                    return getSymlinkName(file).getBytes("UTF-8").length;
                } catch (UnsupportedEncodingException e) {
                    return getSymlinkName(file).getBytes().length;
                }
            }
        }
        return file.length();
    }

    public static long getFileLastModified(File file) {
        if (symlinksSupported()) {
            SVNFileType type = SVNFileType.getType(file);
            if (type == SVNFileType.SYMLINK) {
                Long lastModified = SVNJNAUtil.getSymlinkLastModified(file);
                if (lastModified != null) {
                    return lastModified;
                }
                try {
                    String output = execCommand(new String[]{
                            STAT_COMMAND, "-c", "%Y", file.getAbsolutePath()
                    });
                    if (output != null) {
                        try {
                            return Long.parseLong(output) * 1000;
                        } catch (NumberFormatException e) {
                        }
                    }
                } catch (Throwable th) {
                    SVNDebugLog.getDefaultLog().logFinest(SVNLogType.DEFAULT, th);
                }
            }
        }
        return file.lastModified();
    }
}