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
package org.tmatesoft.svn.core.internal.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNSkel {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    public static final char TYPE_NOTHING = 0;
    public static final char TYPE_SPACE = 1;
    public static final char TYPE_DIGIT = 2;
    public static final char TYPE_PAREN = 3;
    public static final char TYPE_NAME = 4;

    private static final char[] TYPES_TABLE = new char[]{
            0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0,

            /* 64 */
            0, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 0, 3, 0, 0,
            0, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 0, 0, 0, 0,

            /* 128 */
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

            /* 192 */
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    public static char getType(byte b) {
        return TYPES_TABLE[b & 0xFF];
    }

    public static SVNSkel parse(byte[] data) throws SVNException {
        if (data == null) {
            return null;
        }
        return parse(data, 0, data.length);
    }

    public static SVNSkel parse(byte[] data, int offset, int length) throws SVNException {
        if (data == null || length == 0 || offset + length > data.length) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        return parse(buffer);
    }

    public static SVNSkel parse(ByteBuffer buffer) throws SVNException {
        if (buffer == null || !buffer.hasRemaining()) {
            return null;
        }
        byte cur = buffer.get(buffer.position());
        if (cur == '(') {
            return parseList(buffer);
        }
        if (getType(cur) == TYPE_NAME) {
            return parseImplicitAtom(buffer);
        }
        return parseExplicitAtom(buffer);
    }

    public static SVNSkel parseList(ByteBuffer buffer) throws SVNException {
        if (buffer == null || !buffer.hasRemaining()) {
            return null;
        }
        if (buffer.get() != '(') {
            return null;
        }
        if (!buffer.hasRemaining()) {
            return null;
        }
        SVNSkel list = createEmptyList();
        while (true) {
            byte cur = 0;
            while (buffer.hasRemaining()) {
                cur = buffer.get();
                if (getType(cur) != TYPE_SPACE) {
                    break;
                }
            }
            if (cur == ')') {
                break;
            }
            buffer = unread(buffer, 1);
            SVNSkel element = parse(buffer);
            if (element == null) {
                return null;
            }
            list.appendChild(element);
        }
        return list;
    }

    public static SVNSkel parseImplicitAtom(ByteBuffer buffer) {
        if (buffer == null || !buffer.hasRemaining()) {
            return null;
        }
        if (getType(buffer.get(buffer.position())) != TYPE_NAME) {
            return null;
        }
        int start = buffer.position();
        while (buffer.hasRemaining()) {
            byte cur = buffer.get();
            if (getType(cur) == TYPE_SPACE || getType(cur) == TYPE_PAREN) {
                buffer = unread(buffer, 1);
                break;
            }
        }
        return createAtom(buffer.array(), buffer.arrayOffset() + start, buffer.position() - start);
    }

    public static SVNSkel parseExplicitAtom(ByteBuffer buffer) {
        if (buffer == null || !buffer.hasRemaining()) {
            return null;
        }
        int size = parseSize(buffer, buffer.remaining());
        if (size < 0) {
            return null;
        }
        if (!buffer.hasRemaining() || getType(buffer.get()) != TYPE_SPACE) {
            return null;
        }
        int start = buffer.arrayOffset() + buffer.position();
        buffer.position(buffer.position() + size);
        return createAtom(buffer.array(), start, size);
    }

    private static SVNSkel createAtom(SVNPropertyValue propertyValue) {
        if (propertyValue != null && propertyValue.getString() != null) {
            return createAtom(propertyValue.getString());
        } else if (propertyValue != null && propertyValue.getBytes() != null) {
            return createAtom(propertyValue.getBytes());
        } else {
            return createAtom("");
        }
    }

    public static SVNSkel createAtom(String str) {
        if (str == null) {
            return null;
        }
        byte[] data;
        try {
            data = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            data = str.getBytes();
        }
        return new SVNSkel(data);
    }

    public static SVNSkel createAtom(byte[] data) {
        if (data == null) {
            return null;
        }
        return createAtom(data, 0, data.length);
    }

    public static SVNSkel createAtom(byte[] data, int offset, int length) {
        if (data == null) {
            return null;
        }
        byte[] raw = new byte[length];
        System.arraycopy(data, offset, raw, 0, length);
        return new SVNSkel(raw);
    }

    public static SVNSkel createEmptyList() {
        return new SVNSkel();
    }

    public static SVNSkel createPropList(Map<String, SVNPropertyValue> props) throws SVNException {
        SVNSkel list = createEmptyList();
        if (props == null) {
            return list;
        }
        for(String propertyName : props.keySet()) {
            SVNSkel name = createAtom(propertyName);
            SVNPropertyValue pv = props.get(propertyName);
            SVNSkel value = createAtom(pv);

            list.addChild(value);
            list.addChild(name);
            
        }
        if (!list.isValidPropList()) {
            error("proplist");
        }
        return list;
    }

    final private byte[] myRawData;
    final private List myList;

    protected SVNSkel(byte[] data) {
        myRawData = data;
        myList = null;
    }

    protected SVNSkel() {
        myRawData = null;
        myList = new ArrayList();
    }

    public boolean isAtom() {
        return myList == null;
    }

    public byte[] getData() {
        return myRawData;
    }

    public List getList() {
        return Collections.unmodifiableList(myList);
    }

    public SVNSkel getChild(int i) throws SVNException {
        if (isAtom()) {
            SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.FS_MALFORMED_SKEL, "Unable to get a child from atom");
            SVNErrorManager.error(error, SVNLogType.DEFAULT);
        }
        return (SVNSkel) myList.get(i);
    }

    public void appendChild(SVNSkel child) throws SVNException {
        if (isAtom()) {
            SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.FS_MALFORMED_SKEL, "Unable to add a child to atom");
            SVNErrorManager.error(error, SVNLogType.DEFAULT);
        }
        myList.add(child);
    }

    public void addChild(SVNSkel child) throws SVNException {
        if (isAtom()) {
            SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.FS_MALFORMED_SKEL, "Unable to add a child to atom");
            SVNErrorManager.error(error, SVNLogType.DEFAULT);
        }
        myList.add(0, child);
    }

    public void prependString(String str) throws SVNException {
        SVNSkel skel = SVNSkel.createAtom(str);
        addChild(skel);
    }

    public void prependPropertyValue(SVNPropertyValue propertyValue) throws SVNException{
        SVNSkel skel = SVNSkel.createAtom(propertyValue);
        addChild(skel);
    }

    public void prependPath(File path) throws SVNException {        
        String str = path != null ? SVNFileUtil.getFilePath(path) : "";
        prependString(str);
    }

    public int getListSize() {
        if (isAtom()) {
            return -1;
        }
        return myList.size();
    }

    public String getValue() {
        if (isAtom()) {
            String str;
            try {
                str = new String(getData(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                str = new String(getData());
            }
            return str;
        }
        return null;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (isAtom()) {
            buffer.append("[");
            buffer.append(getValue());
            buffer.append("]");
        } else {
            buffer.append("(");
            for (Iterator iterator = myList.iterator(); iterator.hasNext();) {
                SVNSkel element = (SVNSkel) iterator.next();
                buffer.append(element.toString());
            }
            buffer.append(")");
        }
        return buffer.toString();
    }

    public boolean contentEquals(String str) {
        if (!isAtom()) {
            return false;
        }
        String value = getValue();
        return value.equals(str);
    }

    public boolean containsAtomsOnly() {
        if (isAtom()) {
            return false;
        }
        for (Iterator iterator = myList.iterator(); iterator.hasNext();) {
            SVNSkel element = (SVNSkel) iterator.next();
            if (!element.isAtom()) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidPropList() {
        int length = getListSize();
        if (length >= 0 && (length & 1) == 0) {
            return containsAtomsOnly();
        }
        return false;
    }

    public Map parsePropList() throws SVNException {
        if (!isValidPropList()) {
            error("proplist");
        }
        Map props = new SVNHashMap();
        for (Iterator iterator = myList.iterator(); iterator.hasNext();) {
// We always have name - value pair since list length is even
            SVNSkel nameElement = (SVNSkel) iterator.next();
            SVNSkel valueElement = (SVNSkel) iterator.next();
            String name = nameElement.getValue();
            byte[] value = valueElement.isAtom() ? valueElement.getData() : null;
            props.put(name, value);
        }
        return props;
    }

    public byte[] unparse() throws SVNException {
        int approxSize = estimateUnparsedSize();
        ByteBuffer buffer = ByteBuffer.allocate(approxSize);
        buffer = writeTo(buffer);
        buffer.flip();
        byte[] raw = new byte[buffer.limit() - buffer.arrayOffset()];
        System.arraycopy(buffer.array(), buffer.arrayOffset(), raw, 0, buffer.limit());
        return raw;
    }

    public ByteBuffer writeTo(ByteBuffer buffer) throws SVNException {
        if (isAtom()) {
            byte[] data = getData();
            if (useImplicit()) {
                buffer = allocate(buffer, data.length);
                buffer = buffer.put(data);
            } else {
                byte[] sizeBytes = getSizeBytes(data.length);
                if (sizeBytes == null) {
                    SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.FS_MALFORMED_SKEL, "Unable to write size bytes to buffer");
                    SVNErrorManager.error(error, SVNLogType.DEFAULT);
                }
                buffer = allocate(buffer, sizeBytes.length + 1 + data.length);
                buffer.put(sizeBytes);

                try {
                    buffer.put(" ".getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    buffer.put(" ".getBytes());
                }
                buffer.put(data);
            }
        } else {
            buffer = allocate(buffer, 1);
            try {
                buffer.put("(".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                buffer.put("(".getBytes());
            }
            for (Iterator iterator = myList.iterator(); iterator.hasNext();) {
                SVNSkel element = (SVNSkel) iterator.next();
                buffer = element.writeTo(buffer);
                if (iterator.hasNext()) {
                    buffer = allocate(buffer, 1);
                    try {
                        buffer.put(" ".getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        buffer.put(" ".getBytes());
                    }
                }
            }
            buffer = allocate(buffer, 1);
            try {
                buffer.put(")".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                buffer.put(")".getBytes());
            }
        }
        return buffer;
    }

    private int estimateUnparsedSize() {
        if (isAtom()) {
            byte[] data = getData();
            if (data.length < 100) {
// length bytes + whitespace
                return data.length + 3;
            }
            return data.length + 30;
        }
        int total = 2;
        for (Iterator iterator = myList.iterator(); iterator.hasNext();) {
            SVNSkel element = (SVNSkel) iterator.next();
            total += element.estimateUnparsedSize();
// space between a pair of elements
            total++;
        }
        return total;
    }

    private boolean useImplicit() {
        byte[] data = getData();
        if (data.length == 0 || data.length >= 100) {
            return false;
        }
        if (getType(data[0]) != TYPE_NAME) {
            return false;
        }
        for (int i = 0; i < data.length; i++) {
            byte cur = data[i];
            if (getType(cur) == TYPE_SPACE || getType(cur) == TYPE_PAREN) {
                return false;
            }
        }
        return true;
    }

    private static ByteBuffer allocate(ByteBuffer buffer, int capacity) {
        if (buffer == null) {
            capacity = Math.max(capacity * 3 / 2, DEFAULT_BUFFER_SIZE);
            return ByteBuffer.allocate(capacity);
        }
        if (capacity > buffer.remaining()) {
            ByteBuffer expandedBuffer = ByteBuffer.allocate((buffer.position() + capacity) * 3 / 2);
            buffer.flip();
            expandedBuffer.put(buffer);
            return expandedBuffer;
        }
        return buffer;
    }

    private static ByteBuffer unread(ByteBuffer buffer, int length) {
        buffer.position(buffer.position() - length);
        return buffer;
    }

    private static int parseSize(ByteBuffer buffer, int limit) {
        limit = limit < 0 ? Integer.MAX_VALUE : limit;
        final int maxPrefix = limit / 10;
        final int maxDigit = limit % 10;
        int value = 0;
        int start = buffer.position();
        while (buffer.hasRemaining()) {
            byte cur = buffer.get();
            if ('0' <= cur && cur <= '9') {
                int digit = cur - '0';
                if (value > maxPrefix || (value == maxPrefix && digit > maxDigit)) {
                    return -1;
                }
                value = (value * 10) + digit;
            } else {
                buffer = unread(buffer, 1);
                break;
            }
        }
        if (start == buffer.position()) {
            return -1;
        }
        return value;
    }

    private static int writeSizeBytes(int value, byte[] data) {
        int i = 0;
        do {
            if (i >= data.length) {
                return -1;
            }
            data[i] = (byte) ((value % 10) + '0');
            value = value / 10;
            i++;
        } while (value > 0);

        for (int left = 0, right = i - 1; left < right; left++, right--) {
            byte tmp = data[left];
            data[left] = data[right];
            data[right] = tmp;
        }
        return i;
    }

    private static byte[] getSizeBytes(final int value) {
        int tmp = value;
        int length = 0;
        do {
            tmp = tmp / 10;
            length++;
        } while (tmp > 0);

        byte[] data = new byte[length];
        int count = writeSizeBytes(value, data);
        if (count < 0) {
            return null;
        }
        if (count < data.length) {
            byte[] result = new byte[count];
            System.arraycopy(data, 0, result, 0, count);
            return result;
        }
        return data;
    }

    private static void error(String type) throws SVNException {
        SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.FS_MALFORMED_SKEL, "Malformed{0}{1} skeleton", new Object[]{type == null ? "" : " ",
                type == null ? "" : type});
        SVNErrorManager.error(error, SVNLogType.DEFAULT);
    }
}