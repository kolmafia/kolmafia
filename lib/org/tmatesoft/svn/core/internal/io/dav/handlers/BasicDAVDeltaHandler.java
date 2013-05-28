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

package org.tmatesoft.svn.core.internal.io.dav.handlers;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaReader;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNBase64;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;

import org.xml.sax.SAXException;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public abstract class BasicDAVDeltaHandler extends BasicDAVHandler {

    protected static final DAVElement TX_DELTA = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "txdelta");

    private boolean myIsDeltaProcessing;
    private SVNDeltaReader myDeltaReader;
    private StringBuffer myDeltaOutputStream;

    protected void setDeltaProcessing(boolean processing) throws SVNException {
        myIsDeltaProcessing = processing;

        if (!myIsDeltaProcessing) {
            myDeltaReader.reset(getCurrentPath(), getDeltaConsumer());
            getDeltaConsumer().textDeltaEnd(getCurrentPath());
        } else {
            myDeltaOutputStream.delete(0, myDeltaOutputStream.length());
        }
    }

    protected void init() {
        myDeltaReader = new SVNDeltaReader();
        myDeltaOutputStream = new StringBuffer();
        super.init();
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (myIsDeltaProcessing) {
            int offset = start;

            for (int i = start; i < start + length; i++) {
                if (ch[i] == '\r' || ch[i] == '\n') {
                    myDeltaOutputStream.append(ch, offset, i - offset);
                    offset = i + 1;
                    if (i + 1 < (start + length) && ch[i + 1] == '\n') {
                        offset++;
                        i++;
                    }
                }
            }
            if (offset < start + length) {
                myDeltaOutputStream.append(ch, offset, start + length - offset);
            }
            // decode (those dividable by 4) 
            int stored = myDeltaOutputStream.length();
            if (stored < 4) {
                return;
            }
            int segmentsCount = stored / 4;
            int remains = stored - (segmentsCount * 4);

            StringBuffer toDecode = new StringBuffer();
            toDecode.append(myDeltaOutputStream);
            toDecode.delete(myDeltaOutputStream.length() - remains, myDeltaOutputStream.length());

            int index = 0;
            while (index < toDecode.length() && Character.isWhitespace(toDecode.charAt(index))) {
                index++;
            }
            if (index > 0) {
                toDecode = toDecode.delete(0, index);
            }
            index = toDecode.length() - 1;
            while (index >= 0 && Character.isWhitespace(toDecode.charAt(index))) {
                toDecode.delete(index, toDecode.length());
                index--;
            }
            byte[] buffer = allocateBuffer(toDecode.length());
            try {
                int decodedLength = SVNBase64.base64ToByteArray(toDecode, buffer);
                myDeltaReader.nextWindow(buffer, 0, decodedLength, getCurrentPath(), getDeltaConsumer());
            } catch (IllegalArgumentException e) {
                throw new SAXException(e);
            } catch (SVNException e) {
                throw new SAXException(e);
            }
            myDeltaOutputStream.delete(0, toDecode.length());
        } else {
            super.characters(ch, start, length);
        }
    }

    protected abstract String getCurrentPath();

    protected abstract ISVNDeltaConsumer getDeltaConsumer();
}
