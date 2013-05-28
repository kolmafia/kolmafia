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
package org.tmatesoft.svn.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class Version {

    private static String PROPERTIES_PATH = "/svnkit.build.properties";

    private static final String SHORT_VERSION_STRING_PROPERTY = "svnkit.version";
    private static final String VERSION_STRING_PROPERTY = "svnkit.version.string";
    private static final String VERSION_MAJOR_PROPERTY = "svnkit.version.major";
    private static final String VERSION_MINOR_PROPERTY = "svnkit.version.minor";
    private static final String VERSION_MICRO_PROPERTY = "svnkit.version.micro";
    private static final String VERSION_REVISION_PROPERTY = "svnkit.version.revision";
    private static final String SVN_VERSION_PROPERTY = "svnkit.svn.version";  
    
    private static final String VERSION_STRING_DEFAULT = "SVN/1.7.9 SVNKit/1.7.9 (http://svnkit.com/) rSNAPSHOT";
    
    private static final String VERSION_MAJOR_DEFAULT = "1";
    private static final String VERSION_MINOR_DEFAULT = "7";
    private static final String VERSION_MICRO_DEFAULT = "9";
    private static final String VERSION_REVISION_DEFAULT = "SNAPSHOT";
    private static final String SVN_VERSION_DEFAULT = "1.7.9";
    private static String ourUserAgent;

    private static Properties ourProperties;
    
    static {
        ourUserAgent = System.getProperty("svnkit.http.userAgent");
    }

    public static String getVersionString() {
        loadProperties();
        return ourProperties.getProperty(VERSION_STRING_PROPERTY, VERSION_STRING_DEFAULT);
    }
    
    public static String getShortVersionString() {
        loadProperties();
        return ourProperties.getProperty(SHORT_VERSION_STRING_PROPERTY, VERSION_STRING_DEFAULT);
    }

    public static String getSVNVersion() {
        loadProperties();
        return ourProperties.getProperty(SVN_VERSION_PROPERTY, SVN_VERSION_DEFAULT);
    }
    
    public static void setUserAgent(String userAgent) {
        synchronized (Version.class) {
            ourUserAgent = userAgent;
        }
    }

    public static String getUserAgent() {
        if (ourUserAgent != null) {
            return ourUserAgent;
        }
        return getVersionString();
    }

    public static int getMajorVersion() {
        loadProperties();
        try {
            return Integer.parseInt(ourProperties.getProperty(
                    VERSION_MAJOR_PROPERTY, VERSION_MAJOR_DEFAULT));
        } catch (NumberFormatException nfe) {
            //
        }
        return Integer.parseInt(VERSION_MAJOR_DEFAULT);
    }

    public static int getMinorVersion() {
        loadProperties();
        try {
            return Integer.parseInt(ourProperties.getProperty(
                    VERSION_MINOR_PROPERTY, VERSION_MINOR_DEFAULT));
        } catch (NumberFormatException nfe) {
            //
        }
        return Integer.parseInt(VERSION_MINOR_DEFAULT);
    }

    public static int getMicroVersion() {
        loadProperties();
        try {
            return Integer.parseInt(ourProperties.getProperty(
                    VERSION_MICRO_PROPERTY, VERSION_MICRO_DEFAULT));
        } catch (NumberFormatException nfe) {
            //
        }
        return Integer.parseInt(VERSION_MICRO_DEFAULT);
    }

    /**
     * @deprecated use getRevisionString instead
     */
    @Deprecated    
    public static long getRevisionNumber() {
        loadProperties();
        String revisionProperty = ourProperties.getProperty(VERSION_REVISION_PROPERTY, VERSION_REVISION_DEFAULT);
        try {
            return Long.parseLong(revisionProperty);
        } catch (NumberFormatException nfe) {
            // try to fetch revision in other way.
            if (revisionProperty.lastIndexOf('.') > 0) {
                revisionProperty = revisionProperty.substring(revisionProperty.lastIndexOf('.') + 1);
            }
            if (revisionProperty.indexOf('r') >= 0) {
                int start = revisionProperty.indexOf('r') + 1;
                final StringBuffer revValue = new StringBuffer();
                while(start < revisionProperty.length()) {
                    char ch = revisionProperty.charAt(start);
                    start++;
                    if (!Character.isDigit(ch)) {
                        break;
                    }
                    revValue.append(ch);
                }
                if (revValue.length() > 0) {
                    return Long.parseLong(revValue.toString());
                }
            }
        }
        try {
            return Long.parseLong(VERSION_REVISION_DEFAULT);
        } catch (NumberFormatException nfe) {
            //
        }
        return -1;
    }

    public static String getRevisionString() {
        loadProperties();
        return ourProperties.getProperty(VERSION_REVISION_PROPERTY, VERSION_REVISION_DEFAULT);
    }

    private static void loadProperties() {
        synchronized (Version.class) {
            if (ourProperties != null) {
                return;
            }
            ourProperties = new Properties();
            InputStream is = Version.class.getResourceAsStream(PROPERTIES_PATH);
            if (is == null) {
                return;
            }
            try {
                ourProperties.load(is);
            } catch (IOException e) {
                //
            } finally {
                SVNFileUtil.closeFile(is);
            }
        }
    }
}
