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
package org.tmatesoft.svn.core.internal.wc;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNMethodCallLogger implements InvocationHandler {

    static Method OBJECT_TOSTRING;
    static Method OBJECT_HASHCODE;
    static Method OBJECT_EQUALS;

    static {
        try {
            OBJECT_TOSTRING = Object.class.getMethod("toString", new Class[0]);
            OBJECT_HASHCODE = Object.class.getMethod("hashCode", new Class[0]);
            OBJECT_EQUALS = Object.class.getMethod("equals", new Class[]{Object.class});
        } catch (NoSuchMethodException e) {
        }
    }

    public static Object newInstance(Object object, Class[] callSites) {
        return Proxy.newProxyInstance(object.getClass().getClassLoader(),
                object.getClass().getInterfaces(),
                new SVNMethodCallLogger(object, callSites));
    }

    private final Object myTarget;
    private final Class[] myCallSites;

    public SVNMethodCallLogger(Object target, Class[] callSites) {
        myTarget = target;
        myCallSites = callSites == null ? new Class[0] : callSites;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (OBJECT_TOSTRING.equals(method)) {
            return "Logger: " + myTarget.toString();
        }
        if (OBJECT_HASHCODE.equals(method)) {
            return new Integer(myTarget.hashCode());
        }
        if (OBJECT_EQUALS.equals(method)) {
            return new Boolean(myTarget.equals(args[0]));
        }
        
        Object result = null;
        Throwable failure = null;
        try {
            result = method.invoke(myTarget, args);
        } catch (IllegalAccessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw e;
        } catch (Throwable th) {
            failure = th;
        }

        String message = createMessage(method, args, result, failure);
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, message);
        
        if (failure != null) {
            throw failure;
        }
        return result;
    }

    private String createMessage(Method method, Object[] args, Object result, Throwable failure) {
        StringBuffer buffer = new StringBuffer();
        buffer.append('\n');
        buffer.append("Invoked: ");
        buffer.append('\n');
        buffer.append(method);
        buffer.append('\n');

        buffer.append("Arguments:");
        buffer.append('\n');
        Class[] parameters = method.getParameterTypes();
        if (args == null) {
            args = new Object[0];
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (parameters != null && parameters.length > i) {
                Class parameterClass = parameters[i];
                if (parameterClass != null) {
                    buffer.append(getShortClassName(parameterClass));
                    buffer.append(" = ");
                }
            }
            buffer.append(String.valueOf(arg));
            buffer.append('\n');
        }

        buffer.append("Call Site:");
        buffer.append('\n');
        buffer.append(findCallSite());

        buffer.append("Returned:");
        buffer.append('\n');
        buffer.append(String.valueOf(result));
        buffer.append('\n');

        if (failure != null) {
            buffer.append("Thrown:");
            buffer.append('\n');
            buffer.append(failure.getMessage());
            buffer.append('\n');
            buffer.append(generateStackTrace(failure));
            buffer.append('\n');
        }

        return buffer.toString();
    }

    private String getShortClassName(Class cls) {
        if (cls == null) {
            return "null";
        }
        int dotIdx = cls.getName().lastIndexOf(".");
        if (dotIdx >= 0 && dotIdx < cls.getName().length() - 1) {
            return cls.getName().substring(dotIdx + 1);
        }
        return cls.getName();
    }

    private String findCallSite() {
        Throwable traceProvider = new Throwable();
        traceProvider = traceProvider.fillInStackTrace();
        StackTraceElement[] stackTrace = traceProvider.getStackTrace();
        
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement stackTraceElement = stackTrace[i];

            if (myCallSites != null) {
                for (int j = 0; j < myCallSites.length; j++) {
                    Class allowedCallSite = myCallSites[j];

                    if (stackTraceElement.getClassName().equalsIgnoreCase(allowedCallSite.getName()) ||
                            stackTraceElement.getClassName().indexOf(allowedCallSite.getName()) >= 0) {
                        buffer.append(stackTraceElement.toString());
                        buffer.append('\n');
                    }
                }
            } else {
                buffer.append(stackTraceElement);
                buffer.append('\n');
            }
        }
        if (buffer.length() == 0) {
            return "[NOT DETECTED]";
        }
        return buffer.toString();
    }

    private String generateStackTrace(Throwable th) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);
        try {
            th.printStackTrace(writer);
        } finally {
            SVNFileUtil.closeFile(writer);
        }
        try {
            return new String(baos.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(baos.toByteArray());
        }
    }
}
