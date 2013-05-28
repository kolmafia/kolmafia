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
package org.tmatesoft.svn.core.internal.io.svn;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNItem {
    public static final int WORD = 0;
    public static final int BYTES = 1;
    public static final int LIST = 2;
    public static final int NUMBER = 3;

    private int myKind;

    private long myNumber = -1;
    private String myWord;  // success
    private byte[] myLine; // 3:abc
    private Collection myItems;

    public int getKind() {
        return myKind;
    }

    public void setKind(int kind) {
        myKind = kind;
    }

    public long getNumber() {
        return myNumber;
    }

    public void setNumber(long number) {
        myNumber = number;
    }

    public String getWord() {
        return myWord;
    }

    public void setWord(String word) {
        myWord = word;
    }

    public byte[] getBytes() {
        return myLine;
    }

    public void setLine(byte[] line) {
        myLine = line;
    }

    public Collection getItems() {
        return myItems;
    }

    public void setItems(Collection items) {
        myItems = items;
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        if (myKind == WORD) {
            result.append("W").append(myWord);
        } else if (myKind == BYTES) {
            result.append("S").append(myLine.length).append(":");
            try {
                result.append(new String(myLine, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                result.append(myLine);
            }
            result.append(" ");
        } else if (myKind == NUMBER) {
            result.append("N").append(myNumber);
        } else if (myKind == LIST) {
            result.append("L(");
            for (Iterator elemenets = myItems.iterator(); elemenets.hasNext();) {
                SVNItem item = (SVNItem) elemenets.next();
                result.append(item.toString());
                result.append(" ");
            }
            result.append(") ");
        }
        return result.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof String) {
            if (myKind == WORD) {
                return myWord.equals(o);
            } else if (myKind == BYTES) {
                return myLine.equals(o);
            }
            return false;
        } else if (o instanceof byte[]) {
            if (myKind == WORD) {
                return myWord.getBytes().equals(o);
            } else if (myKind == BYTES) {
                return myLine.equals(o);
            }
            return false;
        } else if (o instanceof Long) {
            long value = ((Long) o).longValue();
            return myKind == NUMBER && myNumber == value;
        } else if (o instanceof Integer) {
            long value = ((Integer) o).longValue();
            return myKind == NUMBER && myNumber == value;
        } else if (o instanceof Collection) {
            return myKind == LIST && myItems.equals(o);
        }
        return super.equals(o);
    }
}
