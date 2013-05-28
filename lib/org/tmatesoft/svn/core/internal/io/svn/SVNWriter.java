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
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNWriter {

    private SVNWriter() {
    }

    public static void write(OutputStream os, String templateStr, Object[] src) throws SVNException {
        StringBuffer template = new StringBuffer(templateStr.length());
        for (int i = 0; i < templateStr.length(); i++) {
            char ch = templateStr.charAt(i);
            if (!Character.isWhitespace(ch)) {
                template.append(ch);
            }
        }
        int offset = 0;
        try {
            for (int i = 0; i < template.length(); i++) {
                char ch = template.charAt(i);
                if (ch == '(' || ch == ')') {
                    os.write((byte) ch);
                    os.write(' ');
                    continue;
                }
                Object item = src[offset++];
                if (item == null) {
                    if (ch == '*' || ch == '?') {
                        i++;
                    }
                    continue;
                }
                if (item instanceof Date) {
                    item = SVNDate.formatDate((Date) item, true);
                }
                if (ch == 'i') {

                    InputStream is = ((SVNDataSource) item).getInputStream();
                    long length = ((SVNDataSource) item).lenght();

                    os.write(Long.toString(length).getBytes("UTF-8"));
                    os.write(':');
                    byte[] buffer = new byte[Math.min(2048, (int) length)];
                    while (true) {
                        int read = is.read(buffer);
                        if (read > 0) {
                            os.write(buffer, 0, read);
                        } else if (read < 0) {
                            break;
                        }
                    }
                }
                if (ch == 'b') {
                    byte[] bytes = (byte[]) item;
                    os.write(Integer.toString(bytes.length).getBytes("UTF-8"));
                    os.write(':');
                    os.write(bytes);
                } else if (ch == 'n') {
                    os.write(item.toString().getBytes("UTF-8"));
                } else if (ch == 'w') {
                    os.write(item.toString().getBytes("UTF-8"));
                } else if (ch == 's') {
                    os.write(Integer.toString(
                            item.toString().getBytes("UTF-8").length).getBytes(
                            "UTF-8"));
                    os.write(':');
                    os.write(item.toString().getBytes("UTF-8"));
                } else if (ch == '*') {
                    ch = template.charAt(i + 1);
                    if (item instanceof Object[]) {
                        Object[] list = (Object[]) item;
                        for (int j = 0; j < list.length; j++) {
                            if (ch == 's') {
                                os.write(Integer
                                        .toString(
                                                list[j].toString().getBytes(
                                                        "UTF-8").length)
                                        .getBytes("UTF-8"));
                                os.write(':');
                                os.write(list[j].toString().getBytes("UTF-8"));
                            } else if (ch == 'w') {
                                os.write(list[j].toString().getBytes("UTF-8"));
                            }
                            os.write(' ');
                        }
                    } else if (item instanceof long[] && ch == 'n') {
                        long[] list = (long[]) item;
                        for (int j = 0; j < list.length; j++) {
                            os.write(Long.toString(list[j]).getBytes("UTF-8"));
                            os.write(' ');
                        }
                    } else if (item instanceof Map && ch == 'l') {
                        Map map = (Map) item;
                        for (Iterator paths = map.keySet().iterator(); paths.hasNext();) {
                            String path = (String) paths.next();
                            String token = (String) map.get(path);
                            os.write('(');
                            os.write(' ');
                            os.write(Integer.toString(path.getBytes("UTF-8").length).getBytes("UTF-8"));
                            os.write(':');
                            os.write(path.getBytes("UTF-8"));
                            os.write(' ');
                            os.write(Integer.toString(token.getBytes("UTF-8").length).getBytes("UTF-8"));
                            os.write(':');
                            os.write(token.getBytes("UTF-8"));
                            os.write(' ');
                            os.write(')');
                            os.write(' ');
                        }
                    } else if (item instanceof SVNProperties && ch == 'l') {
                        SVNProperties props = (SVNProperties) item;
                        for (Iterator iterator = props.nameSet().iterator(); iterator.hasNext();) {
                            String name = (String) iterator.next();
                            SVNPropertyValue value = props.getSVNPropertyValue(name);
                            os.write('(');
                            os.write(' ');
                            os.write(Integer.toString(name.getBytes("UTF-8").length).getBytes("UTF-8"));
                            os.write(':');
                            os.write(name.getBytes("UTF-8"));
                            os.write(' ');
                            byte[] bytes = SVNPropertyValue.getPropertyAsBytes(value);
                            os.write(Integer.toString(bytes.length).getBytes("UTF-8"));
                            os.write(':');
                            os.write(bytes);
                            os.write(' ');
                            os.write(')');
                            os.write(' ');
                        }
                    }
                    i++;
                }
                os.write(' ');
            }
        } catch (IOException e) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, e.getMessage()), e, SVNLogType.NETWORK);
        } 
    }
}
