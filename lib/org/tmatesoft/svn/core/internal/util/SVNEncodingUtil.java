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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNEncodingUtil {

    public static String uriEncode(String src) {
        StringBuffer sb = null;
        byte[] bytes;
        try {
            bytes = src.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            bytes = src.getBytes();
        }
        for (int i = 0; i < bytes.length; i++) {
            int index = bytes[i] & 0xFF;
            if (uri_char_validity[index] > 0) {
                if (sb != null) {
                    sb.append((char) bytes[i]);
                }
                continue;
            }
            if (sb == null) {
                sb = new StringBuffer();
                try {
                    sb.append(new String(bytes, 0, i, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    sb.append(new String(bytes, 0, i));
                }
            }
            sb.append("%");

            sb.append(Character.toUpperCase(Character.forDigit((index & 0xF0) >> 4, 16)));
            sb.append(Character.toUpperCase(Character.forDigit(index & 0x0F, 16)));
        }
        return sb == null ? src : sb.toString();
    }
    
    public static String autoURIEncode(String src) {
        StringBuffer sb = null;
        byte[] bytes;
        try {
            bytes = src.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            bytes = src.getBytes();
        }
        for (int i = 0; i < bytes.length; i++) {
            int index = bytes[i] & 0xFF;
            if (uri_char_validity[index] > 0) {
                if (sb != null) {
                    sb.append((char) bytes[i]);
                }
                continue;
            } else if (index == '%' && i + 2 < bytes.length && isHexDigit((char) bytes[i + 1]) && isHexDigit((char) bytes[i + 2])) {
                if (sb != null) {
                    sb.append((char) bytes[i]);
                }
                // digits will be processed fine.
                continue;
            }
            if (sb == null) {
                sb = new StringBuffer();
                try {
                    sb.append(new String(bytes, 0, i, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    sb.append(new String(bytes, 0, i));
                }
            }
            sb.append("%");

            sb.append(Character.toUpperCase(Character.forDigit((index & 0xF0) >> 4, 16)));
            sb.append(Character.toUpperCase(Character.forDigit(index & 0x0F, 16)));
        }
        return sb == null ? src : sb.toString();
    }
    
    public static void assertURISafe(String path) throws SVNException {
        path = path == null ? "" : path;
        byte[] bytes;
        try {
            bytes = path.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_URL, "path ''{0}'' could not be encoded as UTF-8", path);
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
            return;
        }
        if (bytes == null || bytes.length != path.length()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_URL, "path ''{0}'' doesn not look like URI-encoded path", path);
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }
        for (int i = 0; i < bytes.length; i++) {
            if (uri_char_validity[bytes[i]] <= 0 && bytes[i] != '%') {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_URL, "path ''{0}'' doesn not look like URI-encoded path; character ''{1}'' is URI unsafe", new Object[] {path, ((char) bytes[i]) + ""});
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
        }
        return;
    }

    public static String uriDecode(String src) {
        // this is string in ASCII-US encoding.
        boolean query = false;
        boolean decoded = false;
        int length = src.length();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        for(int i = 0; i < length; i++) {
            byte ch = (byte) src.charAt(i);
            if (ch == '?') {
                query = true;
            } else if (ch == '+' && query) {
                ch = ' ';
            } else if (ch == '%' && i + 2 < length &&
                       isHexDigit(src.charAt(i + 1)) &&
                       isHexDigit(src.charAt(i + 2))) {
                ch = (byte) (hexValue(src.charAt(i + 1))*0x10 + hexValue(src.charAt(i + 2)));
                decoded = true;
                i += 2;
            } else {
                // if character is not URI-safe try to encode it.
            }
            bos.write(ch);
        }
        if (!decoded) {
            return src;
        }
        try {
            return new String(bos.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        return src;
    }

    public static String xmlEncodeCDATA(String src) {
        return xmlEncodeCDATA(src, false);
    }

    public static String xmlEncodeCDATA(String src, boolean escapeQuotes) {
        StringBuffer sb = null;
        for(int i = 0; i < src.length(); i++) {
            char ch = src.charAt(i);
            switch (ch) {
                case '&':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&amp;");
                    break;
                case '<':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&lt;");
                    break;
                case '>':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&gt;");
                    break;
                case '\r':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&#13;");
                    break;
                case '\"':
                    if (escapeQuotes) {
                        if (sb == null) {
                            sb = createStringBuffer(src, i);
                        }
                        sb.append("&quot;");
                        break;
                    }
                default:
                    if (sb != null) {
                        sb.append(ch);
                    }
            }
        }
        return sb != null ? sb.toString() : src;
    }

    public static String xmlEncodeAttr(String src) {
        StringBuffer sb = new StringBuffer(src.length());
        for(int i = 0; i < src.length(); i++) {
            char ch = src.charAt(i);
            switch (ch) {
                case '&':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&amp;");
                    break;
                case '<':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&lt;");
                    break;
                case '>':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&gt;");
                    break;
                case '\'':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&apos;");
                    break;
                case '\"':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&quot;");
                    break;
                case '\r':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&#13;");
                    break;
                case '\n':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&#10;");
                    break;
                case '\t':
                    if (sb == null) {
                        sb = createStringBuffer(src, i);
                    }
                    sb.append("&#9;");
                    break;
                default:
                    if (sb != null) {
                        sb.append(ch);
                    }
            }
        }
        return sb != null ? sb.toString() : src;
    }

    public static boolean isXMLSafe(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 0x20 && ch != 0x0A && ch != 0x0D && ch != 0x09 && ch != 0x08) {
                return false;
            }
        }
        return true;
    }

    private static final Map XML_UNESCAPE_MAP = new SVNHashMap();

    static {
        XML_UNESCAPE_MAP.put("&amp;", "&");
        XML_UNESCAPE_MAP.put("&lt;", "<");
        XML_UNESCAPE_MAP.put("&gt;", ">");
        XML_UNESCAPE_MAP.put("&quot;", "\"");
        XML_UNESCAPE_MAP.put("&apos;", "'");
        XML_UNESCAPE_MAP.put("&#13;", "\r");
        XML_UNESCAPE_MAP.put("&#10;", "\n");
        XML_UNESCAPE_MAP.put("&#9;", "\t");
    }

    public static String xmlDecode(String value) {
        StringBuffer result = new StringBuffer(value.length());
        int l = value.length();
        for (int i = 0; i < l; i++) {
            char ch = value.charAt(i);
            if (ch == '&') {
                String replacement = null;
                for (int j = i + 1; j < i + 6 && j < l; j++) {
                    if (value.charAt(j) == ';' && j - i > 1) {
                        String escape = value.substring(i, j + 1); // full
                        replacement = (String) XML_UNESCAPE_MAP.get(escape);
                        if (replacement != null) {
                            result.append(replacement);
                            i = j;
                        }
                        break;
                    }
                }
                if (replacement != null) {
                    continue;
                }
            }
            result.append(ch);
        }
        return result.toString();
    }
    
    public static String fuzzyEscape(String str) {
        char[] chars = str.toCharArray();
        StringBuffer result = createStringBuffer(str, 0);
        for (int i = 0; i < chars.length; i++) {
            if (!isASCIIControlChar(chars[i]) || chars[i] == '\r'
                    || chars[i] == '\n' || chars[i] == '\t') {
                result.append(chars[i]);
            } else {
                result.append("?\\");
                int code = chars[i] & 0xFF;
                if (code < 100) {
                    result.append('0');
                }
                result.append(code);
            }
        }
        return result.toString();
    }

    public static boolean isHexDigit(char ch) {
        return Character.isDigit(ch) ||
               (Character.toUpperCase(ch) >= 'A' && Character.toUpperCase(ch) <= 'F');
    }

    public static boolean isASCIIControlChar(char ch) {
        return (ch >= 0x00 && ch <= 0x1f) || ch == 0x7f;
    }

    private static int hexValue(char ch) {
        if (Character.isDigit(ch)) {
            return ch - '0';
        }
        ch = Character.toUpperCase(ch);
        return (ch - 'A') + 0x0A;
    }

    private static StringBuffer createStringBuffer(String src, int length) {
        StringBuffer sb = new StringBuffer(src.length());
        sb.append(src.toCharArray(), 0, length);
        return sb;
    }

    private static final byte[] uri_char_validity = new byte[] {
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
        0, 1, 0, 0, 1, 0, 1, 1,   1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,   1, 1, 1, 0, 0, 1, 0, 0,

        /* 64 */
        1, 1, 1, 1, 1, 1, 1, 1,   1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,   1, 1, 1, 0, 0, 0, 0, 1,
        0, 1, 1, 1, 1, 1, 1, 1,   1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,   1, 1, 1, 0, 0, 0, 1, 0,

        /* 128 */
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,

        /* 192 */
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0, 0, 0, 0, 0,
    };
}
