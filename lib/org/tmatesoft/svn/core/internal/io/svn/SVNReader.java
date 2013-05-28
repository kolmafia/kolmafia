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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNReader {

    private static final String DEAFAULT_ERROR_TEMPLATE = "nssn";
    private static final String DEFAULT_TEMPLATE = "wl";
    private static final String UTF8_CHARSET_STRING = "UTF-8";

    public static Date getDate(List items, int index) {
        String str = getString(items, index);
        return SVNDate.parseDate(str);
    }

    public static long getLong(List items, int index) {
        if (items == null || index >= items.size()) {
            return -1;
        }
        Object item = items.get(index);
        if (item instanceof Long) {
            return ((Long) item).longValue();
        }
        return -1;
    }

    public static boolean getBoolean(List items, int index) {
        if (items == null || index >= items.size()) {
            return false;
        }
        Object item = items.get(index);
        if (item instanceof String) {
            return Boolean.valueOf((String) item).booleanValue();
        }
        return false;
    }

    public static List getList(List items, int index) {
        if (items == null || index >= items.size()) {
            return Collections.EMPTY_LIST;
        }
        Object item = items.get(index);
        if (item instanceof List) {
            List list = (List) item;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof SVNItem) {
                    SVNItem svnItem = (SVNItem) list.get(i);
                    if (svnItem.getKind() == SVNItem.BYTES) {
                        list.set(i, svnItem.getBytes());
                    } else if (svnItem.getKind() == SVNItem.WORD) {
                        list.set(i, svnItem.getWord());
                    } else if (svnItem.getKind() == SVNItem.NUMBER) {
                        list.set(i, new Long(svnItem.getNumber()));
                    }
                }
            }
            return list;
        }
        return Collections.EMPTY_LIST;
    }

    public static SVNProperties getProperties(List items, int index, SVNProperties properties) throws SVNException {
        properties = properties == null ? new SVNProperties() : properties;

        if (items == null || index >= items.size()) {
            return properties;
        }
        if (!(items.get(index) instanceof List)) {
            return properties;
        }

        List props = getItemList(items, index);
        for (Iterator prop = props.iterator(); prop.hasNext();) {
            SVNItem item = (SVNItem) prop.next();
            if (item.getKind() != SVNItem.LIST) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Proplist element not a list");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            List propItems = parseTuple("sb", item.getItems(), null);
            properties.put(getString(propItems, 0), getBytes(propItems, 1));
        }
        return properties;
    }

    public static SVNProperties getPropertyDiffs(List items, int index, SVNProperties diffs) throws SVNException {
        if (items == null || index >= items.size()) {
            return diffs;
        }
        if (!(items.get(index) instanceof List)) {
            return diffs;
        }
        diffs = diffs == null ? new SVNProperties() : diffs;
        items = getList(items, index);
        for (Iterator iterator = items.iterator(); iterator.hasNext();) {
            SVNItem item = (SVNItem) iterator.next();
            if (item.getKind() != SVNItem.LIST) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Prop diffs element not a list");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            List values = parseTuple("s(?b)", item.getItems(), null);
            diffs.put(getString(values, 0), getBytes(values, 1));
        }
        return diffs;
    }

    public static SVNLock getLock(Collection items) throws SVNException {
        if (items == null || items.isEmpty()) {
            return null;
        }
        List values = parseTuple("sss(?s)s(?s)", items, null);
        if (values.isEmpty()) {
            return null;
        }
        String path = SVNPathUtil.canonicalizePath(getString(values, 0));
        String token = getString(values, 1);
        String owner = getString(values, 2);
        String comment = getString(values, 3);
        Date creationDate = getDate(values, 4);
        Date expirationDate = null;
        if (values.size() >= 6 && values.get(5) != null) {
            expirationDate = getDate(values, 5);
        }
        return new SVNLock(path, token, owner, comment, creationDate, expirationDate);
    }

    public static String getString(List items, int index) {
        if (items == null || index >= items.size()) {
            return null;
        }
        Object item = items.get(index);
        if (item instanceof byte[]) {
            try {
                return new String((byte[]) item, UTF8_CHARSET_STRING);
            } catch (IOException e) {
                return null;
            }
        } else if (item instanceof String) {
            return (String) item;
        } else if (item instanceof Long) {
            return item.toString();
        }
        return null;
    }

    public static byte[] getBytes(List items, int index) {
        if (items == null || index >= items.size()) {
            return null;
        }
        Object item = items.get(index);
        if (item instanceof byte[]) {
            return (byte[]) item;
        } else if (item instanceof String) {
            try {
                return ((String) item).getBytes(UTF8_CHARSET_STRING);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private static List getItemList(List items, int index){
        if (items == null || index >= items.size()) {
            return Collections.EMPTY_LIST;
        }
        if (items.get(index) instanceof List){
            return (List) items.get(index);
        }
        return Collections.EMPTY_LIST;
    }

    public static boolean hasValue(List items, int index, Object value) {
        if (items == null || index >= items.size()) {
            return false;
        }
        if (items.get(index) instanceof List) {
            // look in list.
            for (Iterator iter = ((List) items.get(index)).iterator(); iter.hasNext();) {
                Object element = iter.next();
                if (element.equals(value)) {
                    return true;
                }
            }
        } else {
            if (items.get(index) == null) {
                return value == null;
            }
            if (items.get(index) instanceof byte[] && value instanceof String) {
                try {
                    items.set(index, new String((byte[]) items.get(index), UTF8_CHARSET_STRING));
                } catch (IOException e) {
                    return false;
                }
            }
            return items.get(index).equals(value);
        }
        return false;
    }

    public static SVNItem readItem(InputStream is) throws SVNException {
        char ch = skipWhiteSpace(is);
        return readItem(is, null, ch);
    }

    public static List parse(InputStream is, String template, List values) throws SVNException {
        List readItems = readTuple(is, DEFAULT_TEMPLATE);
        String word = getString(readItems, 0);
        List list = getItemList(readItems, 1);

        if ("success".equals(word)) {
            return parseTuple(template, list, values);
        } else if ("failure".equals(word)) {
            handleFailureStatus(list);
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Unknown status ''{0}'' in command response", word);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return null;
    }

    public static void handleFailureStatus(List list) throws SVNException {
        if (list.size() == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Empty error list");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        SVNErrorMessage topError = getErrorMessage((SVNItem) list.get(list.size() - 1));
        SVNErrorMessage parentError = topError;
        for (int i = list.size() - 2; i >= 0; i--) {
            SVNItem item = (SVNItem) list.get(i);
            SVNErrorMessage error = getErrorMessage(item);
            parentError.setChildErrorMessage(error);
            parentError = error;
        }
        SVNErrorManager.error(topError, SVNLogType.NETWORK);
    }

    private static SVNErrorMessage getErrorMessage(SVNItem item) throws SVNException {
        if (item.getKind() != SVNItem.LIST) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Malformed error list");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        List errorItems = parseTuple(DEAFAULT_ERROR_TEMPLATE, item.getItems(), null);
        int code = ((Long) errorItems.get(0)).intValue();
        SVNErrorCode errorCode = SVNErrorCode.getErrorCode(code);
        String errorMessage = getString(errorItems, 1);
        errorMessage = errorMessage == null ? "" : errorMessage;
        //errorItems contains 2 items more (file and line), no sense to use them.
        return SVNErrorMessage.create(errorCode, errorMessage);
    }

    public static List readTuple(InputStream is, String template) throws SVNException {
        char ch = skipWhiteSpace(is);
        SVNItem item = readItem(is, null, ch);
        if (item.getKind() != SVNItem.LIST) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return parseTuple(template, item.getItems(), null);
    }

    public static List parseTuple(String template, Collection items, List values) throws SVNException {
        values = values == null ? new ArrayList() : values;
        parseTuple(template, 0, items, values);
        return values;
    }

    // ? - skip char, put default value if there's no any chars next to this one.
    // n, r - Long from SVNItem.NUMBER
    // s - String from SVNItem.BYTES (String created from byte[] using UTF-8) or from SVNItem.WORD
    // b - byte[] from SVNItem.BYTES
    // w - String from SVNItem.WORD
    // l - list of SVNItems from SVNItem.LIST
    // (, ) - values of parsing part of template from this '(' to accroding ')' will be added to current values
    private static int parseTuple(String template, int index, Collection items, List values) throws SVNException {
        values = values == null ? new ArrayList() : values;
        for (Iterator iterator = items.iterator(); iterator.hasNext() && index < template.length(); index++) {
            SVNItem item = (SVNItem) iterator.next();
            char ch = template.charAt(index);
            if (ch == '?') {
                index++;
                ch = template.charAt(index);
            }

            if ((ch == 'n' || ch == 'r') && item.getKind() == SVNItem.NUMBER) {
                values.add(new Long(item.getNumber()));
            } else if (ch == 's' && item.getKind() == SVNItem.BYTES) {
                try {
                    values.add(new String(item.getBytes(), UTF8_CHARSET_STRING));
                } catch (IOException e) {
                    values.add(item.getBytes());
                }
            } else if (ch == 's' && item.getKind() == SVNItem.WORD){
                values.add(item.getWord());                
            } else if (ch == 'b' && item.getKind() == SVNItem.BYTES) {
                values.add(item.getBytes());
            } else if (ch == 'w' && item.getKind() == SVNItem.WORD) {
                values.add(item.getWord());
            } else if (ch == 'l' && item.getKind() == SVNItem.LIST) {
                values.add(item.getItems());
            } else if (ch == '(' && item.getKind() == SVNItem.LIST) {
                index++;
                index = parseTuple(template, index, item.getItems(), values);
            } else if (ch == ')') {
                index++;
                return index;
            } else {
                break;
            }
        }
        if (index < template.length() && template.charAt(index) == '?') {
            int nestingLevel = 0;
            while (index < template.length()) {
                switch (template.charAt(index)) {
                    case'?':
                        break;
                    case'r':
                    case'n':
                        values.add(new Long(SVNRepository.INVALID_REVISION));
                        break;
                    case's':
                    case'w':
                    case'b':
                        values.add(null);
                        break;
                    case'l':
                        values.add(Collections.EMPTY_LIST);
                        break;
                    case'(':
                        nestingLevel++;
                        break;
                    case')':
                        nestingLevel--;
                        if (nestingLevel < 0) {
                            return index;
                        }
                        break;
                    default:
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                index++;
            }
        }
        if (index == (template.length() - 1) && template.charAt(index) != ')') {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return index;
    }

    private static SVNItem readItem(InputStream is, SVNItem item, char ch) throws SVNException {
        if (item == null) {
            item = new SVNItem();
        }
        if (Character.isDigit(ch)) {
            long value = Character.digit(ch, 10);
            long previousValue;
            while (true) {
                previousValue = value;
                ch = readChar(is);
                if (Character.isDigit(ch)) {
                    value = value * 10 + Character.digit(ch, 10);
                    if (previousValue != value / 10) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Number is larger than maximum");
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                    continue;
                }
                break;
            }
            if (ch == ':') {
                // string.
                byte[] buffer = new byte[(int) value];
                try {
                    int toRead = (int) value;
                    while (toRead > 0) {
                        int r = is.read(buffer, buffer.length - toRead, toRead);
                        if (r < 0) {
                            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
                            SVNErrorManager.error(err, SVNLogType.NETWORK);
                        }
                        toRead -= r;
                    }
                } catch (IOException e) {
                    SVNDebugLog.getDefaultLog().logFinest(SVNLogType.NETWORK, e);
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                item.setKind(SVNItem.BYTES);
                item.setLine(buffer);

                ch = readChar(is);
            } else {
                // number.
                item.setKind(SVNItem.NUMBER);
                item.setNumber(value);
            }
        } else if (Character.isLetter(ch)) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(ch);
            while (true) {
                ch = readChar(is);
                if (Character.isLetterOrDigit(ch) || ch == '-') {
                    buffer.append(ch);
                    continue;
                }
                break;
            }
            item.setKind(SVNItem.WORD);
            item.setWord(buffer.toString());
        } else if (ch == '(') {
            item.setKind(SVNItem.LIST);
            item.setItems(new ArrayList());
            while (true) {
                ch = skipWhiteSpace(is);
                if (ch == ')') {
                    break;
                }
                SVNItem child = new SVNItem();
                item.getItems().add(child);
                readItem(is, child, ch);
            }
            ch = readChar(is);
        }
        if (!Character.isWhitespace(ch)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return item;
    }

    private static char readChar(InputStream is) throws SVNException {
        int r = 0;
        try {
            r = is.read();
            if (r < 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.NETWORK, e);
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return (char) (r & 0xFF);
    }

    private static char skipWhiteSpace(InputStream is) throws SVNException {
        while (true) {
            char ch = readChar(is);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            return ch;
        }
    }
}
