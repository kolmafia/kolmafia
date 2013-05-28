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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DefaultSVNDebugFormatter extends Formatter {
    
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    
    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        String message = formatMessage(record);
        sb.append("[");
        Date date = new Date(record.getMillis());
        
        synchronized (DATE_FORMAT) {
            sb.append(DATE_FORMAT.format(date));
        }
        sb.append("] ");
        sb.append(message);
        sb.append("\n");
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception e) {
            }
        }
        return sb.toString();
    }
}
