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
package org.tmatesoft.svn.core.internal.io.svn.sasl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.io.svn.SVNAuthenticator;
import org.tmatesoft.svn.core.internal.io.svn.SVNConnection;
import org.tmatesoft.svn.core.internal.io.svn.SVNPlainAuthenticator;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryImpl;
import org.tmatesoft.svn.core.internal.util.SVNBase64;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNSaslAuthenticator extends SVNAuthenticator {

    private SaslClient myClient;
    private ISVNAuthenticationManager myAuthenticationManager;
    private SVNAuthentication myAuthentication;

    public SVNSaslAuthenticator(SVNConnection connection) throws SVNException {
        super(connection);
    }

    public SVNAuthentication authenticate(List mechs, String realm, SVNRepositoryImpl repository) throws SVNException {
        boolean failed = true;
        setLastError(null);
        myAuthenticationManager = repository.getAuthenticationManager();
        myAuthentication = null;
        boolean isAnonymous = false;
        
        if (mechs.contains("EXTERNAL") && repository.getExternalUserName() != null) {
            mechs = new ArrayList();
            mechs.add("EXTERNAL");
        } else { 
            for (Iterator mech = mechs.iterator(); mech.hasNext();) {
                String m = (String) mech.next();
                if ("ANONYMOUS".equals(m) || "EXTERNAL".equals(m) || "PLAIN".equals(m)) {
                    mechs = new ArrayList();
                    isAnonymous = "ANONYMOUS".equals(m); 
                    mechs.add(m);
                    break;
                }
            }
        }
        dispose();
        try {
            myClient = createSaslClient(mechs, realm, repository, repository.getLocation());
            while(true) {
                if (myClient == null) {
                    return new SVNPlainAuthenticator(getConnection()).authenticate(mechs, realm, repository);
                }
                try {
                    if (tryAuthentication(repository, getMechanismName(myClient, isAnonymous))) {
                        if (myAuthenticationManager != null && myAuthentication != null) {
                            String realmName = getFullRealmName(repository.getLocation(), realm);
                            BasicAuthenticationManager.acknowledgeAuthentication(true, myAuthentication.getKind(), realmName, null, myAuthentication, repository.getLocation(), myAuthenticationManager);
                        }
                        failed = false;
                        setLastError(null);
                        setEncryption(repository);
                        break;
                    }
                    // some sort of authentication error.
                } catch (SaslException e) {
                    // it may be plain replaced with anonymous.
                    String mechName = getMechanismName(myClient, isAnonymous);
                    mechs.remove(mechName);
                } 
                if (myAuthenticationManager != null) {
                    SVNErrorMessage error = getLastError();
                    if (error == null) {
                        error = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED);
                        setLastError(error);
                    }
                    if (myAuthentication != null) {
                        String realmName = getFullRealmName(repository.getLocation(), realm);
                        BasicAuthenticationManager.acknowledgeAuthentication(false, myAuthentication.getKind(), realmName, getLastError(), myAuthentication, repository.getLocation(), myAuthenticationManager);
                    } else {
                        // automatically generated authentication, do not try this mech again, will lead to the same error.
                        // 
                        mechs.remove(getMechanismName(myClient, isAnonymous));
                    }
                }
                dispose();
                if (mechs.isEmpty()) {
                    failed = true;
                    break;
                }
                myClient = createSaslClient(mechs, realm, repository, repository.getLocation());
            }
        } finally {
            if (failed) {
                dispose();
            }
        }
        if (getLastError() != null) {
            SVNErrorManager.error(getLastError(), SVNLogType.NETWORK);
        }

        return myAuthentication;
    }
    
    public void dispose() {
        if (myClient != null) {
            try {
                myClient.dispose();
            } catch (SaslException e) {
                //
            }
        }
    }
    
    protected boolean tryAuthentication(SVNRepositoryImpl repos, String mechName) throws SaslException, SVNException {
        String initialChallenge = null;
        boolean expectChallenge = !("ANONYMOUS".equals(mechName) || "EXTERNAL".equals(mechName) || "PLAIN".equals(mechName));
        if ("EXTERNAL".equals(mechName) && repos.getExternalUserName() != null) {
            initialChallenge = "";
        } else if (myClient.hasInitialResponse()) {
            // compute initial response
            byte[] initialResponse = null;
            initialResponse = myClient.evaluateChallenge(new byte[0]);
            if (initialResponse == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Unexpected initial response received from {0}", mechName);
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            initialChallenge = toBase64(initialResponse);
        }
        if (initialChallenge != null) {
            getConnection().write("(w(s))", new Object[] {mechName, initialChallenge});
        } else {
            getConnection().write("(w())", new Object[] {mechName});
        }

        // read response (challenge)
        String status = SVNAuthenticator.STEP;

        while(SVNAuthenticator.STEP.equals(status)) {
            List items = getConnection().readTuple("w(?s)", true);
            status = (String) items.get(0);
            if (SVNAuthenticator.FAILURE.equals(status)) {
                String msg = (String) (items.size() > 1 ? items.get(1) : ""); 
                setLastError(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, msg));
                return false;
            }
            String challenge = (String) (items.size() > 1 ? items.get(1) : null); 
            if (challenge == null && ("CRAM-MD5".equals(mechName) || "GSSAPI".equals(mechName)) && SVNAuthenticator.SUCCESS.equals(status)) {
                challenge = "";
            }
            if ((!SVNAuthenticator.STEP.equals(status) && !SVNAuthenticator.SUCCESS.equals(status)) || 
                    (challenge == null && expectChallenge)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Unexpected server response to authentication");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            byte[] challengeBytes = "CRAM-MD5".equals(mechName) ? challenge.getBytes() : fromBase64(challenge);
            byte[] response = null;
            if (!myClient.isComplete()) {
                response = myClient.evaluateChallenge(challengeBytes);
            }
            if (SVNAuthenticator.SUCCESS.equals(status)) {
                return true;
            }
            if (response == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Unexpected response received from {0}", mechName);
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            if (response.length > 0) {
                String responseStr = "CRAM-MD5".equals(mechName) ? new String(response) : toBase64(response);
                getConnection().write("s", new Object[] {responseStr});
            } else {
                getConnection().write("s", new Object[] {""});
            }
        }
        return true;
        
    }
    
    protected void setEncryption(SVNRepositoryImpl repository) {
        if (getConnection().isEncrypted()) {
            dispose();
            return;
        }
        String qop = (String) myClient.getNegotiatedProperty(Sasl.QOP);
        String buffSizeStr = (String) myClient.getNegotiatedProperty(Sasl.MAX_BUFFER);
        String sendSizeStr = (String) myClient.getNegotiatedProperty(Sasl.RAW_SEND_SIZE);
        
        if ("auth-int".equals(qop) || "auth-conf".equals(qop)) {
            int outBuffSize = 1000;
            int inBuffSize = 1000;
            if (sendSizeStr != null) {
                try {
                    outBuffSize = Integer.parseInt(sendSizeStr);
                } catch (NumberFormatException nfe) {
                    outBuffSize = 1000;
                }
            }
            if (buffSizeStr != null) {
                try {
                    inBuffSize = Integer.parseInt(buffSizeStr);
                } catch (NumberFormatException nfe) {
                    inBuffSize = 1000;
                }
            }
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.NETWORK, "SASL read buffer size: " + inBuffSize);
            SVNDebugLog.getDefaultLog().logFinest(SVNLogType.NETWORK, "SASL write buffer size: " + outBuffSize);
            try {
                getPlainOutputStream().flush();
            } catch (IOException e) {
                //
            }
            OutputStream os = new SaslOutputStream(myClient, outBuffSize, getPlainOutputStream());
            os = repository.getDebugLog().createLogStream(SVNLogType.NETWORK, os);
            setOutputStream(os);
            InputStream is = new SaslInputStream(myClient, inBuffSize, getPlainInputStream());
            is = repository.getDebugLog().createLogStream(SVNLogType.NETWORK, is);
            setInputStream(is);
            getConnection().setEncrypted(this);
        } else {
            dispose();
        }
    }
    
    protected SaslClient createSaslClient(List mechs, String realm, SVNRepositoryImpl repos, SVNURL location) throws SVNException {
        Map props = new SVNHashMap();
        props.put(Sasl.QOP, "auth-conf,auth-int,auth");
        props.put(Sasl.MAX_BUFFER, "8192");
        props.put(Sasl.RAW_SEND_SIZE, "8192");
        props.put(Sasl.POLICY_NOPLAINTEXT, "false");
        props.put(Sasl.REUSE, "false");
        props.put(Sasl.POLICY_NOANONYMOUS, "true");
        
        String[] mechsArray = (String[]) mechs.toArray(new String[mechs.size()]);
        SaslClient client = null;
        for (int i = 0; i < mechsArray.length; i++) {
            String mech = mechsArray[i];
            try {
                if ("ANONYMOUS".equals(mech) || "EXTERNAL".equals(mech) || "PLAIN".equals(mech)) {
                    props.put(Sasl.POLICY_NOANONYMOUS, "false");
                }
                SaslClientFactory clientFactory = getSaslClientFactory(mech, props);
                if (clientFactory == null) {
                    continue;
                }
                SVNAuthentication auth = null;
                if ("ANONYMOUS".equals(mech)) {
                    auth = new SVNPasswordAuthentication("", "", false, location, false);
                } else if ("EXTERNAL".equals(mech)) {
                    String name = repos.getExternalUserName();
                    if (name == null) {
                        name = "";
                    }
                    auth = new SVNPasswordAuthentication(name, "", false, location, false);
                } else {                
                    if (myAuthenticationManager == null) {
                        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Authentication required for ''{0}''", realm),
                                SVNLogType.NETWORK);
                    }
                    String realmName = getFullRealmName(location, realm);
                    if (myAuthentication != null) {
                        myAuthentication = myAuthenticationManager.getNextAuthentication(ISVNAuthenticationManager.PASSWORD, realmName, location);
                    } else {
                        myAuthentication = myAuthenticationManager.getFirstAuthentication(ISVNAuthenticationManager.PASSWORD, realmName, location);
                    }
                    if (myAuthentication == null) {
                        if (getLastError() != null) {
                            SVNErrorManager.error(getLastError(), SVNLogType.NETWORK);
                        }
                        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Authentication required for ''{0}''", realm),
                                SVNLogType.NETWORK);
                    }
                    auth = myAuthentication;
                }
                client = clientFactory.createSaslClient(new String[] {"ANONYMOUS".equals(mech) ? "PLAIN" : mech}, null, "svn", location.getHost(), props, new SVNCallbackHandler(realm, auth));
                if (client != null) {
                    break;
                }
                myAuthentication = null;
            } catch (SaslException e) {
                // remove mech from the list and try next
                // so next time we wouldn't even try this mech next time.
                mechs.remove(mechsArray[i]);
                myAuthentication = null;
            }
        }
        return client;
    }
    
    private static String getFullRealmName(SVNURL location, String realm) {
        if (location == null || realm == null) {
            return realm;
        } 
        return "<" + location.getProtocol() + "://" + location.getHost() + ":" + location.getPort() + "> " + realm;
    }
    
    private static String toBase64(byte[] src) {
        return SVNBase64.byteArrayToBase64(src);
    }
    
    private static byte[] fromBase64(String src) {
        if (src == null) {
            return new byte[0];
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < src.length(); i++) {
            char ch = src.charAt(i);
            if (!Character.isWhitespace(ch) && ch != '\n' && ch != '\r') {
                bos.write((byte) ch & 0xFF);
            }                    
        }
        byte[] cbytes = new byte[src.length()];
        try {
            src = new String(bos.toByteArray(), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            //
        }
        int clength = SVNBase64.base64ToByteArray(new StringBuffer(src), cbytes);
        byte[] result = new byte[clength];
        // strip trailing -1s.
        for(int i = clength - 1; i>=0; i--) {
            if (i == -1) {
                clength--;
            }
        }
        System.arraycopy(cbytes, 0, result, 0, clength);
        return result;
    }
    
    private static String getMechanismName(SaslClient client, boolean isAnonymous) {
        if (client == null) {
            return null;
        }
        String name = client.getMechanismName();
        if ("PLAIN".equals(name) && isAnonymous) {
            name = "ANONYMOUS";
        }
        return name;
    }
    
    private static SaslClientFactory getSaslClientFactory(String mechName, Map props) {
        if (mechName == null) {
            return null;
        }
        if ("ANONYMOUS".equals(mechName)) {
            mechName = "PLAIN";
        }
        for(Enumeration factories = Sasl.getSaslClientFactories(); factories.hasMoreElements();) {
            SaslClientFactory factory = (SaslClientFactory) factories.nextElement();
            String[] mechs = factory.getMechanismNames(props);
            for (int i = 0; mechs != null && i < mechs.length; i++) {
                if (mechName.endsWith(mechs[i])) {
                    return factory; 
                }
            }
        }
        return null;
    }

    private static class SVNCallbackHandler implements CallbackHandler {
        
        private String myRealm;
        private SVNAuthentication myAuthentication;
        
        public SVNCallbackHandler(String realm, SVNAuthentication auth) {
            myRealm = realm;
            myAuthentication = auth;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                Callback callback = callbacks[i];
                if (callback instanceof NameCallback) {
                    String userName = myAuthentication.getUserName();
                    ((NameCallback) callback).setName(userName != null ? userName : "");
                } else if (callback instanceof PasswordCallback) {
                    String password = ((SVNPasswordAuthentication) myAuthentication).getPassword();
                    ((PasswordCallback) callback).setPassword(password != null ? password.toCharArray() : new char[0]);
                } else if (callback instanceof RealmCallback) {
                    ((RealmCallback) callback).setText(myRealm);
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        }
    }
}
