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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class XMLReader extends Reader {
    
    public static final char COLON_REPLACEMENT = '\u3007'; // ideografic char.
    
    private Reader mySource;
    private boolean myIsEscaping;
    private int myColonCount;
    private boolean myIsClosed;

    public XMLReader(InputStream is) throws UnsupportedEncodingException {
        mySource = new InputStreamReader(is, "UTF-8");
    }

    public int read(char[] b, int off, int len) throws IOException {
        int read = mySource.read(b, off, len);
        for(int i = 0; i < read; i++) {
            char ch = b[off + i];
            if (ch < 0x20 && ch != '\r' &&
                    ch != '\n' && ch != '\t') {
                b[off + i] = ' ';
                continue;
            } else if (ch == 0xffff || ch == 0xfffe) {
                b[off + i] = ' ';
                continue;
            }
            if (myIsEscaping) {
                if (ch == ':') {
                    myColonCount++;
                    if (myColonCount > 1) {
                        b[off + i] = COLON_REPLACEMENT;
                    }
                } else if (Character.isWhitespace(ch) || ch == '>') {
                    myIsEscaping = false;
                }
            } else if (!myIsEscaping && ch == '<') {
                myIsEscaping = true;
                myColonCount = 0;
            } 
        }
        myIsClosed = read < 0;
        return read;
    }
    
    public boolean isClosed() {
        return myIsClosed;
    }

    public void close() throws IOException {
    }

}
