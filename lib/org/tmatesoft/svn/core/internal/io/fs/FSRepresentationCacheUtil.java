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
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.repcache.IFSRepresentationCacheManagerFactory;
import org.tmatesoft.svn.core.internal.wc.SVNClassLoader;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRepresentationCacheUtil {
    
    private static volatile boolean ourIsAvailable;
    
    private static final String SQLJET_DB_CLASS_NAME = "org.tmatesoft.sqljet.core.table.SqlJetDb";
    private static final String ANTLR_CLASS_NAME = "org.antlr.runtime.Token";

    private static IFSRepresentationCacheManagerFactory ourRepCacheManagerFactory;
    
    static {
        Boolean option = Boolean.valueOf(System.getProperty("svnkit.fsfs.repcache", "true"));
        if (option.booleanValue()) {
            try {
                Class antlrClazz = FSRepresentationCacheUtil.class.getClassLoader().loadClass(ANTLR_CLASS_NAME);
                if (antlrClazz == null) {
                    ourIsAvailable = false;
                } else {
                    Class clazz = FSRepresentationCacheUtil.class.getClassLoader().loadClass(SQLJET_DB_CLASS_NAME);
                    ourIsAvailable = clazz != null;
                    if (ourIsAvailable) {
                        ourRepCacheManagerFactory = SVNClassLoader.getFSRepresentationCacheManagerFactory();
                        if (ourRepCacheManagerFactory != null) {
                            ourIsAvailable = true;
                        } else {
                            ourIsAvailable = false;
                        }
                    }
                }
            } catch (Throwable e) {
                ourIsAvailable = false;
            }
        } else {
            ourIsAvailable = false;
        }
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.FSFS, "SQLJET enabled: " + ourIsAvailable);
    }
    
    public static IFSRepresentationCacheManager open(FSFS fsfs) throws SVNException {
        if (!isAvailable()) {
            return null;
        }
        if (ourRepCacheManagerFactory != null) {
            return ourRepCacheManagerFactory.openRepresentationCache(fsfs);
        }
        return null;
    }

    public static void create(File path) throws SVNException {
        if (!isAvailable()) {
            return;
        }
        if (ourRepCacheManagerFactory != null) {
            ourRepCacheManagerFactory.createRepresentationCache(path);
        }
    }
    
    private static boolean isAvailable() {
        return ourIsAvailable;
    }

}
