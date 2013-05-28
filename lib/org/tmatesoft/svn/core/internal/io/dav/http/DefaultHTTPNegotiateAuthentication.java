package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNBase64;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DefaultHTTPNegotiateAuthentication extends HTTPNegotiateAuthentication {
    
    private static final String NEGOTIATE_TYPE_PROPERTY = "svnkit.negotiate.type"; 
    private static final String NEGOTIATE_TYPE_SPNEGO = "spnego"; 
    private static final String NEGOTIATE_TYPE_KERBEROS = "krb"; 

    private static Map<String, Oid> ourOids = new HashMap<String, Oid>();
    
    static {
        try {
            ourOids.put(NEGOTIATE_TYPE_KERBEROS, new Oid("1.2.840.113554.1.2.2") );
        } catch (GSSException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
        }
        try {
            ourOids.put(NEGOTIATE_TYPE_SPNEGO, new Oid("1.3.6.1.5.5.2") );
        } catch (GSSException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
        }
    }
    
    private static Oid getDefaultOID() {
        String defaultOid = System.getProperty(NEGOTIATE_TYPE_PROPERTY, NEGOTIATE_TYPE_KERBEROS);
        if (defaultOid == null || "".equals(defaultOid)) {
            defaultOid = NEGOTIATE_TYPE_KERBEROS;
        }
        Oid oid = (Oid) ourOids.get(defaultOid);
        if (oid != null) {
            return oid;
        }
        return (Oid) ourOids.get(NEGOTIATE_TYPE_KERBEROS);
    }

    private class SVNKitCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    ((NameCallback)callbacks[i]).setName(getUserName());
                } else if (callbacks[i] instanceof PasswordCallback) {
                    ((PasswordCallback)callbacks[i]).setPassword(getPassword() == null ? null : getPassword().toCharArray());
                }
            }
        }

    }

    private static volatile Boolean ourIsNegotiateSupported;

    private GSSManager myGSSManager = GSSManager.getInstance();
    private GSSContext myGSSContext;
    private Oid mySpnegoOid;
    private Subject mySubject;
    
    public DefaultHTTPNegotiateAuthentication(DefaultHTTPNegotiateAuthentication prevAuth) {
        if (prevAuth != null) {
            mySubject = prevAuth.mySubject;
        }
    }
    
    public DefaultHTTPNegotiateAuthentication() {
        this(null);
    }

    public static synchronized boolean isSupported() {
        if (ourIsNegotiateSupported == null) {
            Oid spnegoOid = getDefaultOID();
            Oid[] supportedOids = GSSManager.getInstance().getMechs();
            for (int i = 0; i < supportedOids.length; i++) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: supported OID: " + supportedOids[i]);
            }
            ourIsNegotiateSupported = Boolean.valueOf(Arrays.asList(GSSManager.getInstance().getMechs()).contains(spnegoOid));
        }
        return ourIsNegotiateSupported.booleanValue();
    }
    
    private byte[] myToken;
    private int myTokenLength;

    public void respondTo(String challenge) {
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: respond to, challenge: " + challenge);
        if (challenge == null) {
            myToken = new byte[0];
            myTokenLength = 0;
        } else {
            myToken = new byte[(challenge.length() * 3 + 3) / 4];
            myTokenLength = SVNBase64.base64ToByteArray(new StringBuffer(challenge), myToken);
        }
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: respond to, token length: " + myTokenLength);
    }
    
    private void initializeSubject() {
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: initialize subject");
        if (mySubject != null) {
            return;
        }
        
        try {
            LoginContext ctx = new LoginContext("com.sun.security.jgss.krb5.initiate", new SVNKitCallbackHandler());
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: initialize subject, login context: " + ctx);
            ctx.login();
            mySubject = ctx.getSubject();
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: initialize subject, subject: " + mySubject);
        } catch (LoginException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
//            SecurityException rethrown = new SecurityException();
//            rethrown.initCause(e);
//            throw rethrown;
        }
    }

    private void initializeContext() throws GSSException {
        if (mySpnegoOid == null) {
            mySpnegoOid = getDefaultOID();
        }
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: initialize context, OID: " + mySpnegoOid);
        GSSCredential credentials = myGSSManager.createCredential(GSSCredential.INITIATE_ONLY);
        GSSName serverName = myGSSManager.createName(getServerPrincipalName(), GSSName.NT_HOSTBASED_SERVICE);
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: initialize context, server name: " + serverName);
        myGSSContext = myGSSManager.createContext(serverName, mySpnegoOid, credentials, GSSContext.DEFAULT_LIFETIME);
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: initialize context, GSS Context: " + myGSSContext);
    }

    public String authenticate() throws SVNException {
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate: isStarted:" + isStarted());
        if (!isStarted()) {
            initializeSubject();
        }

        final PrivilegedExceptionAction<String> action = new PrivilegedExceptionAction<String>() {
            public String run() throws SVNException {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate action: isStarted: " + isStarted());
                if (!isStarted()) {
                    try {
                        initializeContext();
                        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate action: context initializaed");
                    } catch (GSSException gsse) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Negotiate authentication failed: ''{0}''", gsse.getMajorString());
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                        return null;
                    }
                }
        
                byte[] outtoken;
        
                try {
                    myGSSContext.requestCredDeleg(true);
                    outtoken = myGSSContext.initSecContext(myToken, 0, myTokenLength);
                    SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate action: out token: " + outtoken);
                    if (outtoken != null) {
                        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate action: out token: " + SVNBase64.byteArrayToBase64(outtoken));
                    }
                } catch (GSSException gsse) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Negotiate authentication failed: ''{0}''", gsse.getMajorString());
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                    return null;
                }
        
                if (myToken != null) {
                    return "Negotiate " + SVNBase64.byteArrayToBase64(outtoken);
                }
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate action: myToken is null");
                return null;
            }
        };
        
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate: subject:" + mySubject);
        if (mySubject != null) {
            try {
                String result = (String) Subject.doAs(mySubject, action);
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate: result:" + result);
                return result;
            } catch (PrivilegedActionException e) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
                Throwable cause = e.getCause();
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, cause);
                if (cause instanceof SVNException) {
                    throw (SVNException)cause;
                }
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, e), SVNLogType.NETWORK);
            }
        }
        
        try {
            String result = action.run();
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: authenticate: result (2):" + result);
            return  result;
        } catch (Exception cause) {
            if (cause instanceof SVNException) {
                throw (SVNException) cause;
            }
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, cause), SVNLogType.NETWORK);
        }
        return null;
    }

    public boolean isStarted() {
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: isStarted: " + myGSSContext);
        return myGSSContext != null;
    }
    
    public boolean needsLogin() {
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: needsLogin");
        initializeSubject();
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "NEGOTIATE: needsLogin, mySubject: " + mySubject);
        return mySubject == null;
    }
    
}
