/*
 * $Id: OpenBrowserAction.java,v 1.4 2008/12/05 14:34:56 kschaefe Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.action;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 *
 * @author joshy
 */
public class OpenBrowserAction extends AbstractAction {
    
    private static Logger log = Logger.getLogger(OpenBrowserAction.class.getName());

    private URL url;
    /** Creates a new instance of OpenBrowserAction */
    public OpenBrowserAction() {
    }
    
    public OpenBrowserAction(String url) throws MalformedURLException {
        this.setUrl(new URL(url));
    }
    public OpenBrowserAction(URL url) {
        this.setUrl(url);
    }
    
    public URL getUrl() {
        return url;
    }
    
    public void setUrl(URL url) {
        this.url = url;
    }
    
    
    /* code to actuall open the browser on all platforms */
    private static final String osName = System.getProperty("os.name" ).toLowerCase();
    
    private static final boolean linux = osName.startsWith("linux");
    private static final boolean macosx = osName.startsWith("mac os x");
    private static final boolean win95 = osName.equals("windows 95");
    private static final boolean winAny = osName.startsWith("windows");
    private static final boolean DEBUG = true;
    
    public void actionPerformed(ActionEvent e) {
        try {
            if(isApplet()) {
                log.fine("trying applet version");
                openApplet(url);
                log.fine("succeeded applet version");
            } else if(isWebStart()) {
                log.fine("trying applet version");
                openWebstart(url);
                log.fine("succeeded applet version");
            } else if (macosx) {
                log.fine("trying Mac OS X version");
                openMacOSX(url);
                log.fine("succeeded mac os x version");
            } else if (linux) {
                log.fine("trying linux version");
                openLinux(url);
                log.fine("succeeded linux version");
            } else if (win95) {
                log.fine("trying win95 version");
                openWin95(url);
                log.fine("succeeded win95 version");
            } else if (winAny) {
                log.fine("trying windows any version");
                openWindows(url);
                log.fine("succeeded windows any version");
            } else {
                log.fine("trying other version");
                openOther(url);
                log.fine("succeeded other version");
            }
        } catch (Throwable thr) {
            handleError(thr);
        }
    }
    
    protected void handleError(Throwable thr) {
        System.err.println(thr.getMessage());
        thr.printStackTrace(System.err);
    }
    
    private static void openMacOSX(URL url) throws IOException {
        try {
            log.fine("trying mac version using FileManager class");
            // Mac OS X can handle file types itself (thank goodness!)
            Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
            Method openURL = fileMgr.getDeclaredMethod("openURL",
                    new Class[] {String.class});
            openURL.invoke(null, new Object[] {url.toString()});
        } catch (Throwable thr) {
            log .fine("trying 'open' command version");
            thr.printStackTrace();
            // if that fails for some reason, try the 'open' commandline command
            Runtime.getRuntime().exec(new String[] {"open", url.toString()});
        }
    }
    
    private void openLinux(URL url) throws IOException, InterruptedException, Exception {
        try {
            // try the xdg service first. part of free desktop?
            Runtime.getRuntime().exec(new String[] {"./xdg-open", url.toString()});
        } catch (Throwable thr) {
            
            // if xdg fails
            // look for one of the browsers
            String[] browsers = { // list of html browsers
                "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
            String browser = null;
            for (int count = 0; count < browsers.length && browser == null; count++) {
                if (Runtime.getRuntime().exec(
                        new String[] {"which", browsers[count]}).waitFor() == 0) {
                    browser = browsers[count];
                }
            }
            
            if (browser == null) { // got no browser, bummer
                throw new Exception("Could not find web browser");
            } else {// got a browser, run it
                Runtime.getRuntime().exec(new String[] {browser, url.toString()});
            }
        }
    }
    
    private void openWin95(URL url) throws IOException {
        Runtime.getRuntime().exec(new String[] {"command.com", "/C", "start", url.toString()});
    }
    
    private void openWindows(URL url) throws IOException {
        Runtime.getRuntime().exec(new String[] {"cmd.exe", "/C", "start", url.toString()});
    }
    
    private void openOther(URL url) throws IOException {
        Runtime.getRuntime().exec(new String[] {"open", url.toString()});
    }
    
    private boolean isWebStart() {
        try {
            Class<?> svcManager = Class.forName("jnlp.ServiceManager");
            Method lookup = svcManager.getDeclaredMethod("lookup",
                    new Class[] {String.class});
            lookup.invoke(null, new Object[] {"jnlp.BasicService"});
            return true;
        } catch (Throwable thr) {
            return false;
        }
    }
    
    private void openWebstart(URL url) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class<?> svcManager = Class.forName("javax.jnlp.ServiceManager");
        Method lookup = svcManager.getDeclaredMethod("lookup",
                new Class[] {String.class});
        Object basicSvcInst = lookup.invoke(null, new Object[] {"javax.jnlp.BasicService"});
        Class<?> basicSvc = Class.forName("javax.jnlp.BasicService");
        Method showDocument = basicSvc.getDeclaredMethod("showDocument",
                new Class[] {URL.class});
        showDocument.invoke(basicSvcInst, new Object[] {url});
    }

    private boolean isApplet() {
        return false;
    }

    private void openApplet(URL url) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public static void main(String ... args) throws MalformedURLException {
        //System.out.println("trying to open a URL on the current platform");
        Action action = new OpenBrowserAction("http://sun.com/");
        action.actionPerformed(null);
    }
}
