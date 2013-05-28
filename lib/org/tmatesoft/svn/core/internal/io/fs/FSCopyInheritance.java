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
package org.tmatesoft.svn.core.internal.io.fs;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSCopyInheritance {

    // Copy id inheritance style
    public static final int COPY_ID_INHERIT_UNKNOWN = 0;
    public static final int COPY_ID_INHERIT_SELF = 1;
    public static final int COPY_ID_INHERIT_PARENT = 2;
    public static final int COPY_ID_INHERIT_NEW = 3;

    private int myStyle;
    private String myCopySourcePath;

    public FSCopyInheritance(int style, String path) {
        myStyle = style;
        myCopySourcePath = path;
    }

    public String getCopySourcePath() {
        return myCopySourcePath;
    }

    public int getStyle() {
        return myStyle;
    }

    public void setCopySourcePath(String copySourcePath) {
        myCopySourcePath = copySourcePath;
    }

    public void setStyle(int style) {
        myStyle = style;
    }

}
