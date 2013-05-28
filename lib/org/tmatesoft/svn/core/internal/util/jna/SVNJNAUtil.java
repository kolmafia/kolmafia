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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.ISVNGnomeKeyringPasswordProvider;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNJNAUtil {
    
    private static boolean ourIsJNAEnabled;
    private static boolean ourIsJNAPresent;
    private static final String JNA_CLASS_NAME = "com.sun.jna.Library";
    
    static {
        try {
            ClassLoader loader = SVNJNAUtil.class.getClassLoader();
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            if (loader != null && loader.loadClass(JNA_CLASS_NAME) != null) {
                ourIsJNAPresent = true;
            }
        } catch (ClassNotFoundException e) {
            ourIsJNAPresent = false;
        }
        String jnaEnabledProperty = System.getProperty("svnkit.useJNA", "true");
        ourIsJNAEnabled = Boolean.valueOf(jnaEnabledProperty).booleanValue();
        
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.WC, "JNA present/enabled: " + ourIsJNAPresent + "/" + ourIsJNAEnabled);
    }
    
    public static void setJNAEnabled(boolean enabled) {
        synchronized (SVNJNAUtil.class) {
            ourIsJNAEnabled = enabled;
        }
    }
    
    public static boolean isJNAPresent() {
        synchronized (SVNJNAUtil.class) {
            return ourIsJNAPresent && ourIsJNAEnabled;
        }
    }

    // linux.
    
    public static SVNFileType getFileType(File file) {
        if (isJNAPresent()) {
            return SVNLinuxUtil.getFileType(file);
        }
        return null;
    }

    public static Boolean isExecutable(File file) {
        if (isJNAPresent()) {
            return SVNLinuxUtil.isExecutable(file);
        }
        return null;
    }

    public static String getLinkTarget(File file) {
        if (isJNAPresent()) {
            return SVNLinuxUtil.getLinkTarget(file);
        }
        return null;
    }

    public static boolean setExecutable(File file, boolean set) {
        if (isJNAPresent()) {
            return SVNLinuxUtil.setExecutable(file, set);
        }
        return false;
    }

    public static boolean setSGID(File file) {
        if (isJNAPresent()) {
            return SVNLinuxUtil.setSGID(file);
        }
        return false;
    }

    public static boolean createSymlink(File file, String linkName) {
        if (isJNAPresent()) {
            return SVNLinuxUtil.createSymlink(file, linkName);
        }
        return false;
    }

    public static Long getSymlinkLastModified(File file) {
        if (isJNAPresent()) {
            return SVNLinuxUtil.getSymlinkLastModified(file);
        }
        return null;
    }

    // linux and win32.
    public static boolean setWritable(File file) {
        if (isJNAPresent()) {
            return SVNFileUtil.isWindows ?
                    SVNWin32Util.setWritable(file) :
                    SVNLinuxUtil.setWritable(file);
        }
        return false;
    }

    // win32
    public static boolean setHidden(File file) {
        if (isJNAPresent()) {
            return SVNWin32Util.setHidden(file);
        }
        return false;
    }

    public static boolean moveFile(File src, File dst) {
        if (isJNAPresent()) {
            return SVNWin32Util.moveFile(src, dst);
        }
        return false;
    }
    
    public static String decrypt(String encryptedData) {
        if (isJNAPresent()) {
            return SVNWinCrypt.decrypt(encryptedData);
        }
        return null;
    }
    
    public static String encrypt(String rawData) {
        if (isJNAPresent()) {
            return SVNWinCrypt.encrypt(rawData);
        }
        return null;
    }

    public static boolean addPasswordToMacOsKeychain(String realm, String userName, String password, boolean nonInteractive) throws SVNException {
        if (isJNAPresent()) {
            return SVNMacOsKeychain.setPassword(realm, userName, password, nonInteractive);
        }
        return false;
    }

    public static String getPasswordFromMacOsKeychain(String realm, String userName, boolean nonInteractive) throws SVNException {
        if (isJNAPresent()) {
            return SVNMacOsKeychain.getPassword(realm, userName, nonInteractive);
        }
        return null;
    }

    public static boolean addPasswordToGnomeKeyring(String realm, String userName, String password, boolean nonInteractive, ISVNGnomeKeyringPasswordProvider keyringPasswordProvider) throws SVNException {
        if (isJNAPresent()) {
            return SVNGnomeKeyring.setPassword(realm, userName, password, nonInteractive, keyringPasswordProvider);
        }
        return false;
    }

    public static String getPasswordFromGnomeKeyring(String realm, String userName, boolean nonInteractive, ISVNGnomeKeyringPasswordProvider keyringPasswordProvider) throws SVNException {
        if (isJNAPresent()) {
            return SVNGnomeKeyring.getPassword(realm, userName, nonInteractive, keyringPasswordProvider);
        }
        return null;
    }

    public synchronized static boolean isWinCryptEnabled() {
        return isJNAPresent() && SVNWinCrypt.isEnabled();
    }

    public synchronized static boolean isMacOsKeychainEnabled() {
        return isJNAPresent() && SVNMacOsKeychain.isEnabled();
    }

    public synchronized static boolean isGnomeKeyringEnabled() {
        return isJNAPresent() && SVNGnomeKeyring.isEnabled();
    }
    
    public static String getApplicationDataPath(boolean common) {
        if (isJNAPresent()) {
            return SVNWin32Util.getApplicationDataPath(common);
        }
        return null;
    }
}
