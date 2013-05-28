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

import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNSocketConnection implements Runnable {

    private Socket mySocket;
    private InetSocketAddress myAddress;
    private int myTimeout;
    private IOException myError;
    private volatile boolean myIsSocketConnected;

    public SVNSocketConnection(Socket socket, InetSocketAddress address, int timeout) {
        mySocket = socket;
        myAddress = address;
        myTimeout = timeout;
        myIsSocketConnected = false;
    }

    public IOException getError() {
        return myError;
    }

    public boolean isSocketConnected() {
        
        synchronized (this) {
            if (!myIsSocketConnected) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                }
            }
        }
        return myIsSocketConnected;
    }

    public void run() {
        try {
            mySocket.connect(myAddress, myTimeout);
        } catch (IOException e) {
            myError = e;
        } finally {
            synchronized (this) {
                myIsSocketConnected = true;
                notify();
            }
        }
    }
}
