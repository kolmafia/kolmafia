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
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class HTTPParser {
    
    private static ThreadLocal<byte[]> ourLocalReadBuffer = new ThreadLocal<byte[]>() {
        protected byte[] initialValue() {
            return new byte[8192];
        }
    };
    
    public static HTTPStatus parseStatus(InputStream is, String charset) throws IOException, ParseException {
        String line = null;
        int limit = 100;
        do {
            if (limit < 0) {
                line = null;
                break;
            }
            line = readLine(is, charset);
            limit--;
            // check if line represents HTTP status line.
        } while (line != null && (line.length() == 0 || line.trim().length() == 0 || !HTTPStatus.isHTTPStatusLine(line)));
        
        if (line == null) {
            throw new EOFException("can not read HTTP status line");
        }
        return HTTPStatus.createHTTPStatus(line);
    }
    
    public static String readLine(InputStream is, String charset) throws IOException {
        int length = readPlainLine(is);
        if (length <= 0) {
            return null;
        }
        byte[] buffer = ourLocalReadBuffer.get();
        if (length > 0 && buffer[length - 1] == '\n') {
            length--;
            if (length > 0 && buffer[length - 1] == '\r') {
                length--;
            }
        }
        return new String(buffer, 0, length, charset);
    }
    
    private static int readPlainLine(InputStream is) throws IOException {
        int ch;
        int i = 0;
        byte[] buffer = ourLocalReadBuffer.get();
        while (i < buffer.length && (ch = is.read()) >= 0) {
            buffer[i] = (byte) (ch & 0xFF);
            if (ch == '\n') {
                break;
            }
            i++;
        }
        return i;
    }

    public static StringBuffer getCanonicalPath(String path, StringBuffer target) {
        target = target == null ? new StringBuffer() : target;
        if (path.startsWith("http:") || path.startsWith("https:")) {
            target.append(path);
            return target;
        }
    
        int end = path.length() - 1;
        for(int i = 0; i <= end; i++) {
            char ch = path.charAt(i);
            switch (ch) {
            case '/':
                if (i == end && i != 0) {
                    // skip trailing slash
                    break;
                } else if (i > 0 && path.charAt(i - 1) == '/') {
                    // skip duplicated slashes
                    break;
                }
            default:
                target.append(ch);
            }
        }
        return target;
    
    }
}