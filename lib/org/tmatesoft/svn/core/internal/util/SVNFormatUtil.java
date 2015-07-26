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
package org.tmatesoft.svn.core.internal.util;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNFormatUtil {

    public static String formatString(String str, int chars, boolean left) {
        return formatString(str, chars, left, true);
    }

    public static String formatString(String str, int chars, boolean left, boolean cut) {
        if (str.length() > chars) {
            return cut ? str.substring(0, chars) : str;
        }
        StringBuffer formatted = new StringBuffer();
        if (left) {
            formatted.append(str);
        }
        for(int i = 0; i < chars - str.length(); i++) {
            formatted.append(' ');
        }
        if (!left) {
            formatted.append(str);
        }
        return formatted.toString();
    }

    public static String getHexNumberFromByte(byte b) {
        int lo = b & 0xf;
        int hi = (b >> 4) & 0xf;
        return Integer.toHexString(hi) + Integer.toHexString(lo);
    }

    public static void appendHexNumber(StringBuffer target, byte b) {
        int lo = b & 0xf;
        int hi = (b >> 4) & 0xf;
        target.append(HEX[hi]);
        target.append(HEX[lo]);
    }

    public static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == 0x0b || c == '\f' || c == '\r';
    }

    public static String collapseSpaces(String s) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (!isSpace(c)) {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }
    
    private static char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

}
